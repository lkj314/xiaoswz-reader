package com.xiaoswz.reader.ui.reader

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoswz.reader.data.BookRepository
import com.xiaoswz.reader.data.annotation.ANNOTATION_TYPE_HIGHLIGHT
import com.xiaoswz.reader.data.annotation.AnnotationEntity
import com.xiaoswz.reader.data.annotation.AnnotationRepository
import com.xiaoswz.reader.data.api.SegmentCommentItem
import com.xiaoswz.reader.data.api.SegmentCommentListResponse
import com.xiaoswz.reader.data.backend.BackendRepository
import com.xiaoswz.reader.data.model.ChapterDto
import com.xiaoswz.reader.data.settings.ReaderSettings
import com.xiaoswz.reader.data.settings.ReaderSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 连续滚动（无缝阅读）模式下，已加载进同一条滚动流的单个章节块。
 * content 已是预处理（缩进/段距）后的正文，屏幕直接渲染。
 */
data class ChapterBlock(
    val id: String,
    val title: String,
    val index: Int,
    val content: String,
    val rawContent: String = "",
    val fromOffline: Boolean = false,
)

/** 书内全文搜索结果项：章内字符偏移相对「预处理后正文」 */
data class SearchMatch(
    val chapterId: String,
    val chapterTitle: String,
    val offset: Int,
    val snippet: String,
)

data class ReaderUiState(
    val bookSlug: String = "",
    val currentChapterId: String = "",
    val chapterTitle: String = "",
    val bookName: String = "",
    val rawContent: String = "",
    val toc: List<ChapterDto> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val menuVisible: Boolean = false,
    val settingsVisible: Boolean = false,
    val settings: ReaderSettings = ReaderSettings(),
    /** 当前章节内容是否来自离线文件缓存（断网可读） */
    val isOffline: Boolean = false,
    // ── 连续滚动（无缝阅读）状态 ──
    /** 是否处于连续滚动渲染模式（continuousScroll && 滚动模式） */
    val isContinuous: Boolean = false,
    /** 已加载进同一条滚动流的连续章节窗口 */
    val chapterBlocks: List<ChapterBlock> = emptyList(),
    /** 末尾正在 append 下一章 */
    val blockAppendLoading: Boolean = false,
    /** 顶部正在 prepend 上一章 */
    val blockPrependLoading: Boolean = false,
    /** 请求屏幕滚动到指定章节块（TOC 跳转 / 上一章下一章按钮） */
    val pendingScrollToChapterId: String? = null,
    /** 本书全部标注（高亮 + 书签），本地文件为权威，云端仅做跨设备合并 */
    val annotations: List<AnnotationEntity> = emptyList(),
    /** 段评：按章节 id 分组的段评列表（正文不入库，仅锚定到段落 + 段内偏移） */
    val segmentCommentsByChapter: Map<String, List<SegmentCommentItem>> = emptyMap(),
) {
    val currentIndex: Int get() = toc.indexOfFirst { it.id == currentChapterId }.let { if (it < 0) 0 else it }
    val totalChapters: Int get() = toc.size
    val prevChapterId: String? get() = toc.getOrNull(toc.indexOfFirst { it.id == currentChapterId } - 1)?.id
    val nextChapterId: String? get() = toc.getOrNull(toc.indexOfFirst { it.id == currentChapterId } + 1)?.id
    val hasPrevChapter: Boolean get() = prevChapterId != null
    val hasNextChapter: Boolean get() = nextChapterId != null
}

class ReaderViewModel(application: Application) : AndroidViewModel(application) {

    // repository 在类体内初始化，确保构造函数只剩单一 Application 参数，
    // 否则 AndroidViewModelFactory.getConstructor(Application.class) 反射失败 →
    // "Cannot create an instance of ReaderViewModel" 闪退（点开章节必崩）。
    private val repository = BookRepository()
    private val settingsRepo = ReaderSettingsRepository(application)

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    /** 当前书 slug（从状态流读取，供标注持久化按书寻址） */
    private val bookSlug: String get() = _uiState.value.bookSlug

    init {
        // 订阅设置流，实时应用到阅读器
        viewModelScope.launch {
            settingsRepo.settingsFlow.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
    }

    /** 入口：加载书籍目录 + 指定章节 */
    fun load(bookSlug: String, chapterId: String) {
        // 标注按书加载（本地即时 + 云端合并，失败静默）
        viewModelScope.launch { loadAnnotations(bookSlug) }
        if (_uiState.value.bookSlug == bookSlug && _uiState.value.toc.isNotEmpty()) {
            // 同一本书已就绪，直接以当前模式切章
            if (_uiState.value.currentChapterId != chapterId) openChapter(chapterId)
            return
        }
        _uiState.update { it.copy(bookSlug = bookSlug, isLoading = true, error = null) }
        viewModelScope.launch {
            repository.getBookDetail(bookSlug)
                .onSuccess { detail ->
                    _uiState.update {
                        it.copy(
                            toc = detail.chapters.orEmpty(),
                            bookName = detail.name.orEmpty(),
                        )
                    }
                    openChapter(chapterId)
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message ?: "目录加载失败")
                    }
                }
        }
    }

    /** 按当前阅读模式分派：连续滚动 → 多章窗口；否则 → 单章 */
    private fun openChapter(chapterId: String) {
        val s = _uiState.value
        val continuous = s.settings.continuousScroll && s.settings.pageMode == ReaderSettings.MODE_SCROLL
        if (continuous) openContinuousChapter(chapterId) else openSingleChapter(chapterId)
    }

    /** 设置（模式）切换后，若当前内容形态与新模式不匹配则重新打开当前章 */
    fun reopen() {
        val s = _uiState.value
        if (s.currentChapterId.isBlank()) return
        val cont = s.settings.continuousScroll && s.settings.pageMode == ReaderSettings.MODE_SCROLL
        if (cont && s.chapterBlocks.isEmpty() && !s.isLoading) {
            openContinuousChapter(s.currentChapterId)
        } else if (!cont && s.rawContent.isEmpty() && !s.isLoading) {
            openSingleChapter(s.currentChapterId)
        }
    }

    // ── 单章模式（翻页 / 单章滚动）：整体替换当前章 ──
    private fun openSingleChapter(chapterId: String) {
        _uiState.update {
            it.copy(
                isLoading = true,
                error = null,
                isOffline = false,
                isContinuous = false,
                chapterBlocks = emptyList(),
            )
        }
        viewModelScope.launch {
            repository.getChapterContent(chapterId)
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentChapterId = chapterId,
                            chapterTitle = result.data.title.orEmpty(),
                            rawContent = result.data.content.orEmpty(),
                            bookName = it.bookName.ifBlank { result.data.bookName.orEmpty() },
                            isOffline = result.fromOfflineCache,
                            menuVisible = false,
                            settingsVisible = false,
                        )
                    }
                    prefetchAround(result.data.id ?: chapterId)
                    loadSegmentComments(result.data.id ?: chapterId)
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message ?: "章节加载失败")
                    }
                }
        }
    }

    // ── 连续滚动模式：把章节载入「连续窗口」，并立刻把下一章接上 ──
    private fun openContinuousChapter(chapterId: String) {
        _uiState.update {
            it.copy(
                isLoading = true,
                error = null,
                isOffline = false,
                isContinuous = true,
                chapterBlocks = emptyList(),
                pendingScrollToChapterId = null,
            )
        }
        viewModelScope.launch {
            loadBlock(chapterId)?.let { block ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentChapterId = chapterId,
                        chapterTitle = block.title,
                        chapterBlocks = listOf(block),
                        isOffline = block.fromOffline,
                        menuVisible = false,
                        settingsVisible = false,
                    )
                }
                prefetchAround(chapterId)
                loadSegmentComments(chapterId)
                // 立刻把下一章 append 进同一条滚动流，滚动即无缝
                appendNextChapter()
            } ?: _uiState.update {
                it.copy(isLoading = false, error = "章节加载失败")
            }
        }
    }

    /** 拉取并预处理单个章节块（命中文件缓存则瞬时，无感） */
    private suspend fun loadBlock(chapterId: String): ChapterBlock? {
        val s = _uiState.value
        return repository.getChapterContent(chapterId).fold(
            onSuccess = { res ->
                val raw = res.data.content.orEmpty()
                val processed = preprocessContent(
                    raw,
                    s.settings.indentFirstLine,
                    s.settings.paraSpacing,
                )
                val idx = s.toc.indexOfFirst { it.id == chapterId }
                ChapterBlock(
                    id = chapterId,
                    title = res.data.title.orEmpty(),
                    index = idx,
                    content = processed,
                    rawContent = raw,
                    fromOffline = res.fromOfflineCache,
                )
            },
            onFailure = { null },
        )
    }

    /** 滚动到末块时调用：把下一章 append 进连续窗口（缓存命中即瞬时） */
    fun appendNextChapter() {
        val s = _uiState.value
        if (s.isLoading || s.blockAppendLoading || s.chapterBlocks.isEmpty()) return
        val last = s.chapterBlocks.last()
        val idx = s.toc.indexOfFirst { it.id == last.id }
        val nextDto = s.toc.getOrNull(idx + 1) ?: return // 已到末章
        if (s.chapterBlocks.any { it.id == nextDto.id }) return // 该章已在窗口
        _uiState.update { it.copy(blockAppendLoading = true) }
        viewModelScope.launch {
            val nid = nextDto.id ?: return@launch
            val blk = loadBlock(nid)
            _uiState.update { st ->
                if (blk != null) {
                    st.copy(chapterBlocks = st.chapterBlocks + blk, blockAppendLoading = false)
                } else {
                    st.copy(blockAppendLoading = false) // 失败静默，下次滚动到底再试
                }
            }
            if (blk != null) loadSegmentComments(nid)
        }
    }

    /** 向上滚动到首块时调用：把上一章 prepend 进连续窗口 */
    fun prependPrevChapter() {
        val s = _uiState.value
        if (s.isLoading || s.blockPrependLoading || s.chapterBlocks.isEmpty()) return
        val first = s.chapterBlocks.first()
        val idx = s.toc.indexOfFirst { it.id == first.id }
        val prevDto = s.toc.getOrNull(idx - 1) ?: return // 已到首章
        if (s.chapterBlocks.any { it.id == prevDto.id }) return
        _uiState.update { it.copy(blockPrependLoading = true) }
        viewModelScope.launch {
            val pid = prevDto.id ?: return@launch
            val blk = loadBlock(pid)
            _uiState.update { st ->
                if (blk != null) {
                    st.copy(chapterBlocks = listOf(blk) + st.chapterBlocks, blockPrependLoading = false)
                } else {
                    st.copy(blockPrependLoading = false)
                }
            }
            if (blk != null) loadSegmentComments(pid)
        }
    }

    /** 滚动时由屏幕上报当前顶部可见章，用于进度保存与预读推进 */
    fun setCurrentChapter(chapterId: String) {
        if (_uiState.value.currentChapterId == chapterId) return
        _uiState.update { it.copy(currentChapterId = chapterId) }
        prefetchAround(chapterId)
    }

    fun requestScrollToChapter(id: String) {
        _uiState.update { it.copy(pendingScrollToChapterId = id) }
    }

    fun clearPendingScroll() {
        _uiState.update { it.copy(pendingScrollToChapterId = null) }
    }

    fun goToChapter(chapterId: String) {
        val s = _uiState.value
        if (s.isContinuous && s.chapterBlocks.any { it.id == chapterId }) {
            requestScrollToChapter(chapterId)
        } else {
            openChapter(chapterId)
        }
    }

    /** 翻到下一章；连续模式下平滑滚动到下一章块（而非整屏替换） */
    fun nextChapter(): Boolean {
        val s = _uiState.value
        if (s.isContinuous) {
            val idx = s.chapterBlocks.indexOfFirst { it.id == s.currentChapterId }
            val target = s.chapterBlocks.getOrNull(idx + 1)?.id ?: s.nextChapterId
            if (target != null) {
                if (s.chapterBlocks.any { it.id == target }) {
                    requestScrollToChapter(target)
                } else {
                    openChapter(target) // 该章尚未载入窗口，重建窗口
                }
                return true
            }
            return false
        }
        return s.nextChapterId?.let { openChapter(it); true } ?: false
    }

    /** 翻到上一章；连续模式下平滑滚动到上一章块 */
    fun prevChapter(): Boolean {
        val s = _uiState.value
        if (s.isContinuous) {
            val idx = s.chapterBlocks.indexOfFirst { it.id == s.currentChapterId }
            val target = s.chapterBlocks.getOrNull(idx - 1)?.id ?: s.prevChapterId
            if (target != null) {
                if (s.chapterBlocks.any { it.id == target }) {
                    requestScrollToChapter(target)
                } else {
                    openChapter(target)
                }
                return true
            }
            return false
        }
        return s.prevChapterId?.let { openChapter(it); true } ?: false
    }

    fun retry() {
        val s = _uiState.value
        if (s.toc.isEmpty()) {
            load(s.bookSlug, s.currentChapterId)
        } else if (s.isContinuous && s.chapterBlocks.isEmpty()) {
            openContinuousChapter(s.currentChapterId)
        } else {
            openSingleChapter(s.currentChapterId)
        }
    }

    fun toggleMenu() {
        _uiState.update { it.copy(menuVisible = !it.menuVisible, settingsVisible = false) }
    }

    fun hideMenu() {
        _uiState.update { it.copy(menuVisible = false, settingsVisible = false) }
    }

    fun showSettings() {
        _uiState.update { it.copy(settingsVisible = true) }
    }

    fun hideSettings() {
        _uiState.update { it.copy(settingsVisible = false) }
    }

    fun updateSettings(transform: (ReaderSettings) -> ReaderSettings) {
        viewModelScope.launch {
            settingsRepo.update(transform)
        }
    }

    /** 读章后后台预取周围的章节（下 N + 上 1），命中文件缓存则跳过 */
    private fun prefetchAround(currentChapterId: String) {
        val state = _uiState.value
        // 仅 WiFi 预读取：`prefetchWifiOnly` 开启且当前非 WiFi 时跳过，避免消耗移动流量
        if (state.settings.prefetchWifiOnly && !com.xiaoswz.reader.util.NetworkUtils.isWifi(
                getApplication<android.app.Application>().applicationContext,
            )
        ) return
        val toc = state.toc
        if (toc.isEmpty()) return
        val idx = toc.indexOfFirst { it.id == currentChapterId }.takeIf { it >= 0 } ?: return
        val ids = mutableListOf<String>()
        for (i in 1..state.settings.prefetchNext) {
            toc.getOrNull(idx + i)?.id?.let { ids.add(it) }
        }
        for (i in 1..state.settings.prefetchPrev) {
            toc.getOrNull(idx - i)?.id?.let { ids.add(it) }
        }
        ids.forEach { id ->
            viewModelScope.launch {
                repository.prefetchChapter(id)
            }
        }
    }

    // ── 模块 A：标注 / 书签 / 书内搜索 ──

    /** 加载本书全部标注：本地文件为权威，云端合并补充（需登录；失败静默）。 */
    private suspend fun loadAnnotations(bookId: String) {
        val ctx = getApplication<Application>().applicationContext
        runCatching {
            val list = AnnotationRepository.sync(ctx, bookId)
            _uiState.update { it.copy(annotations = list.filter { a -> !a.deleted }) }
        }
    }

    /** 某章的高亮（用于连续滚动渲染背景）。 */
    fun highlightsForChapter(chapterId: String): List<AnnotationEntity> =
        _uiState.value.annotations.filter {
            it.type == ANNOTATION_TYPE_HIGHLIGHT && it.chapterId == chapterId
        }

    fun addHighlight(chapterId: String, start: Int, end: Int, quoted: String?, color: Int): AnnotationEntity {
        val ann = AnnotationRepository.createHighlight(bookSlug, chapterId, start, end, quoted, color)
        commitAnnotation(ann)
        return ann
    }

    fun updateAnnotationNote(clientId: String, note: String) {
        val ctx = getApplication<Application>().applicationContext
        val list = _uiState.value.annotations.toMutableList()
        val idx = list.indexOfFirst { it.clientId == clientId }
        if (idx < 0) return
        val updated = list[idx].copy(note = note, updatedAt = System.currentTimeMillis())
        list[idx] = updated
        AnnotationRepository.persist(ctx, bookSlug, list)
        _uiState.update { it.copy(annotations = list) }
        viewModelScope.launch { AnnotationRepository.pushOne(ctx, updated) }
    }

    fun deleteAnnotation(clientId: String) {
        val ctx = getApplication<Application>().applicationContext
        val list = _uiState.value.annotations.toMutableList()
        val idx = list.indexOfFirst { it.clientId == clientId }
        if (idx < 0) return
        val tombstone = list[idx].copy(deleted = true, updatedAt = System.currentTimeMillis())
        list[idx] = tombstone
        AnnotationRepository.persist(ctx, bookSlug, list)
        _uiState.update { it.copy(annotations = list.filter { a -> !a.deleted }) }
        viewModelScope.launch { AnnotationRepository.pushOne(ctx, tombstone) }
    }

    private fun commitAnnotation(ann: AnnotationEntity) {
        val ctx = getApplication<Application>().applicationContext
        val list = _uiState.value.annotations.toMutableList().apply { add(ann) }
        AnnotationRepository.persist(ctx, bookSlug, list)
        _uiState.update { it.copy(annotations = list) }
        viewModelScope.launch { AnnotationRepository.pushOne(ctx, ann) }
    }

    // ── 模块 B：章评 / 段评（v0.15）──
    // 正文由书源实时抓取，不入库；评论仅锚定到章节 + 段内位置 + 引用快照。

    /** 加载某章段评（一次性拉取，失败静默，绝不阻断阅读）。 */
    fun loadSegmentComments(chapterId: String) {
        val s = _uiState.value
        if (s.bookSlug.isBlank() || chapterId.isBlank()) return
        viewModelScope.launch {
            BackendRepository.getSegmentComments(s.bookSlug, chapterId)
                .onSuccess { resp ->
                    _uiState.update { st ->
                        st.copy(segmentCommentsByChapter = st.segmentCommentsByChapter + (chapterId to resp.comments))
                    }
                }
                .onFailure { /* 静默降级：段评加载失败不影响阅读 */ }
        }
    }

    /** 发段评（登录）。成功后刷新该章段评。失败由调用方 Toast 提示。 */
    fun postSegmentComment(
        chapterId: String,
        paragraphIndex: Int,
        startOffset: Int?,
        endOffset: Int?,
        quotedText: String,
        content: String,
        parentId: String? = null,
    ) {
        val s = _uiState.value
        if (s.bookSlug.isBlank()) return
        viewModelScope.launch {
            BackendRepository.postSegmentComment(
                s.bookSlug, chapterId, content, paragraphIndex, startOffset, endOffset, quotedText, parentId,
            ).onSuccess { loadSegmentComments(chapterId) }
        }
    }

    /** 段评点赞（乐观本地 + 后台自增，不计重）。 */
    fun likeSegmentComment(chapterId: String, id: String) {
        val list = _uiState.value.segmentCommentsByChapter[chapterId] ?: return
        val updated = list.map { if (it.id == id) it.copy(likeCount = it.likeCount + 1) else it }
        _uiState.update { st ->
            st.copy(segmentCommentsByChapter = st.segmentCommentsByChapter + (chapterId to updated))
        }
        viewModelScope.launch { BackendRepository.likeComment(id) }
    }

    /** 段评举报（基础风控，自增 reportCount，审核后台预留）。 */
    fun reportSegmentComment(id: String) {
        viewModelScope.launch { BackendRepository.reportComment(id) }
    }

    /** 当前章预处理后的正文（供听书 TTS 朗读）。连续模式取当前章块；单章模式预处理 rawContent。 */
    fun currentChapterProcessedText(): String {
        val s = _uiState.value
        return if (s.isContinuous) {
            s.chapterBlocks.firstOrNull { it.id == s.currentChapterId }?.content ?: ""
        } else {
            preprocessContent(
                s.rawContent,
                s.settings.indentFirstLine,
                s.settings.paraSpacing,
            )
        }
    }

    /** 书内全文搜索：遍历目录逐章抓取（命中缓存则瞬时），返回章内偏移匹配。 */
    suspend fun searchBook(query: String): List<SearchMatch> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val s = _uiState.value
        val toc = s.toc
        if (toc.isEmpty()) return emptyList()
        val lower = q.lowercase()
        val matches = mutableListOf<SearchMatch>()
        for (dto in toc) {
            val cid = dto.id ?: continue
            val content = repository.getChapterContent(cid)
                .fold({ it.data.content.orEmpty() }, { "" })
            if (content.isEmpty()) continue
            val processed = preprocessContent(content, s.settings.indentFirstLine, s.settings.paraSpacing)
            var from = 0
            while (true) {
                val idx = processed.lowercase().indexOf(lower, from)
                if (idx < 0) break
                val snippetStart = maxOf(0, idx - 12)
                val snippetEnd = minOf(processed.length, idx + q.length + 12)
                matches.add(
                    SearchMatch(
                        chapterId = cid,
                        chapterTitle = dto.name ?: "",
                        offset = idx,
                        snippet = processed.substring(snippetStart, snippetEnd),
                    ),
                )
                from = idx + q.length
            }
        }
        return matches
    }
}
