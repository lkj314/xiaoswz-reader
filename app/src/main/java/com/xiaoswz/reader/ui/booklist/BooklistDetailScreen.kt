package com.xiaoswz.reader.ui.booklist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.xiaoswz.reader.data.api.BooklistItemDto
import com.xiaoswz.reader.data.api.BOOK_SOURCE_MAIN
import com.xiaoswz.reader.data.booklist.BooklistRepository
import com.xiaoswz.reader.data.settings.AppSettingsRepository
import com.xiaoswz.reader.ui.booklist.EditBooklistSheet
import com.xiaoswz.reader.ui.components.AppTopBar
import com.xiaoswz.reader.ui.components.ReportSheet
import com.xiaoswz.reader.ui.components.MetaButton
import com.xiaoswz.reader.ui.theme.GlassTokens
import kotlinx.coroutines.launch
import com.xiaoswz.reader.ui.theme.MetaIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BooklistDetailScreen(
    booklistId: String,
    onBack: () -> Unit,
    onUserClick: (String) -> Unit,
    onBookClick: (String) -> Unit,
    viewModel: BooklistDetailViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var showReport by remember { mutableStateOf(false) }
    var showPicker by remember { mutableStateOf(false) }
    var editBooklist by remember { mutableStateOf(false) }
    var deleteConfirm by remember { mutableStateOf(false) }
    var editItem by remember { mutableStateOf<BooklistItemDto?>(null) }
    var justShared by remember { mutableStateOf(false) }
    var justCollected by remember { mutableStateOf(false) }

    val isOwner = state.detail?.owner?.id == state.accountId

    val settingsRepo = remember { AppSettingsRepository(context.applicationContext) }
    LaunchedEffect(booklistId) {
        viewModel.load(booklistId)
        settingsRepo.accountIdFlow.collect { viewModel.setAccountId(it) }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "书单详情",
                onBack = onBack,
                showLogo = false,
                actions = {
                    if (isOwner) {
                        IconButton(onClick = { editBooklist = true }) {
                            Icon(MetaIcons.Edit, contentDescription = "编辑书单", tint = GlassTokens.Label)
                        }
                        IconButton(onClick = { deleteConfirm = true }) {
                            Icon(MetaIcons.Delete, contentDescription = "删除书单", tint = GlassTokens.SystemRed)
                        }
                    } else {
                        IconButton(onClick = { showReport = true }) {
                            Icon(MetaIcons.Warning, contentDescription = "举报", tint = GlassTokens.Label)
                        }
                    }
                },
            )
        },
        containerColor = Color.Transparent,
    ) { padding ->
        if (state.isLoading && state.detail == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GlassTokens.SystemBlue)
            }
            return@Scaffold
        }

        val detail = state.detail
        if (detail == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.error ?: "书单不存在或已删除", color = GlassTokens.SecondaryLabel)
                    Spacer(Modifier.height(12.dp))
                    MetaButton(text = "返回", onClick = onBack)
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(88.dp)
                            .clip(RoundedCornerShape(GlassTokens.RadiusLG))
                            .background(GlassTokens.GlassFill),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (!detail.coverUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = detail.coverUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(GlassTokens.RadiusLG)),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Icon(MetaIcons.MenuBook, null, tint = GlassTokens.TertiaryLabel, modifier = Modifier.size(40.dp))
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(detail.title, style = MaterialTheme.typography.titleLarge, color = GlassTokens.Label, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.clickable { onUserClick(detail.owner.id) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(MetaIcons.AccountCircle, null, tint = GlassTokens.SecondaryLabel, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(detail.owner.displayName ?: "读者", style = MaterialTheme.typography.labelMedium, color = GlassTokens.SecondaryLabel)
                            if (detail.isOfficial) {
                                Spacer(Modifier.width(8.dp))
                                Text("官方", style = MaterialTheme.typography.labelSmall, color = Color.White, modifier = Modifier
                                    .clip(RoundedCornerShape(GlassTokens.RadiusPill))
                                    .background(GlassTokens.SystemBlue)
                                    .padding(horizontal = 8.dp, vertical = 2.dp))
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(MetaIcons.Star, null, tint = GlassTokens.SystemBlue, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("${detail.collectCount} 收藏", style = MaterialTheme.typography.labelSmall, color = GlassTokens.TertiaryLabel)
                            Spacer(Modifier.width(12.dp))
                            Text("${detail.items.size} 本", style = MaterialTheme.typography.labelSmall, color = GlassTokens.TertiaryLabel)
                        }
                    }
                }
            }

            if (!detail.description.isNullOrBlank()) {
                item {
                    Text(detail.description, style = MaterialTheme.typography.bodyMedium, color = GlassTokens.SecondaryLabel, lineHeight = 20.sp)
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // 收藏 / 取消收藏
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(GlassTokens.RadiusMD))
                            .background(if (detail.collected) GlassTokens.SystemBlue else GlassTokens.GlassFillStrong)
                            .clickable {
                                scope.launch {
                                    viewModel.toggleCollect(booklistId) { res ->
                                        res.onSuccess { justCollected = true }
                                            .onFailure { e ->
                                                android.widget.Toast.makeText(context, e.message ?: "操作失败", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                }
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                MetaIcons.Star,
                                null,
                                tint = if (detail.collected) Color.White else GlassTokens.SystemBlue,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (detail.collected) "已收藏" else "收藏",
                                color = if (detail.collected) Color.White else GlassTokens.Label,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                    // 分享到社区
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(GlassTokens.RadiusMD))
                            .background(GlassTokens.GlassFillStrong)
                            .clickable {
                                scope.launch {
                                    viewModel.shareToCommunity(booklistId, detail.title, detail.description) { res ->
                                        res.onSuccess {
                                            justShared = true
                                            android.widget.Toast.makeText(context, "已分享到社区", android.widget.Toast.LENGTH_SHORT).show()
                                        }.onFailure { e ->
                                            android.widget.Toast.makeText(context, e.message ?: "分享失败（需登录）", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(MetaIcons.Share, null, tint = GlassTokens.Label, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("分享", color = GlassTokens.Label, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
                if (justCollected || justShared) Spacer(Modifier.height(2.dp))
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("书单内容（${detail.items.size}）", style = MaterialTheme.typography.titleMedium, color = GlassTokens.Label, fontWeight = FontWeight.SemiBold)
                    androidx.compose.material3.AssistChip(
                        onClick = { showPicker = true },
                        label = { Text("添加书籍") },
                        leadingIcon = { Icon(MetaIcons.Add, null, modifier = Modifier.size(18.dp)) },
                    )
                }
            }

            if (detail.items.isEmpty()) {
                item {
                    Text("这本书单还没有收录书籍", style = MaterialTheme.typography.bodyMedium, color = GlassTokens.SecondaryLabel)
                }
            } else {
                itemsIndexed(detail.items, key = { _, it -> it.id }) { index, item ->
                    BooklistItemRow(
                        item = item,
                        isOwner = isOwner,
                        canMoveUp = index > 0,
                        canMoveDown = index < detail.items.lastIndex,
                        onClick = { onBookClick(item.bookId) },
                        onEditNote = { editItem = item },
                        onMoveUp = {
                            val above = detail.items[index - 1]
                            scope.launch {
                                viewModel.updateItem(booklistId, item.id, position = above.position)
                                viewModel.updateItem(booklistId, above.id, position = item.position)
                            }
                        },
                        onMoveDown = {
                            val below = detail.items[index + 1]
                            scope.launch {
                                viewModel.updateItem(booklistId, item.id, position = below.position)
                                viewModel.updateItem(booklistId, below.id, position = item.position)
                            }
                        },
                        onDelete = {
                            scope.launch {
                                viewModel.deleteItem(booklistId, item.id) { res ->
                                    res.onFailure { e ->
                                        android.widget.Toast.makeText(context, e.message ?: "删除失败", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                    )
                }
            }
        }
    }

    if (showPicker) {
        BookPickerSheet(
            onDismiss = { showPicker = false },
            onPicked = { book ->
                val slug = book.slug ?: return@BookPickerSheet
                scope.launch {
                    BooklistRepository.addItem(
                        id = booklistId,
                        bookSourceId = BOOK_SOURCE_MAIN,
                        bookId = slug,
                        title = book.title,
                        author = book.displayAuthor,
                        coverUrl = book.coverImage,
                        note = null,
                        bookUid = book.uid,
                    ).onSuccess {
                        viewModel.load(booklistId)
                        android.widget.Toast.makeText(context, "已加入书单", android.widget.Toast.LENGTH_SHORT).show()
                        showPicker = false
                    }.onFailure { e ->
                        android.widget.Toast.makeText(context, e.message ?: "添加失败", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            },
        )
    }

    if (showReport) {
        ReportSheet(
            title = "举报书单",
            onDismiss = { showReport = false },
            onSubmit = { reason ->
                viewModel.report(booklistId, reason) { res ->
                    showReport = false
                    res.onSuccess {
                        android.widget.Toast.makeText(context, "举报已提交，感谢反馈", android.widget.Toast.LENGTH_SHORT).show()
                    }.onFailure { e ->
                        android.widget.Toast.makeText(context, e.message ?: "举报失败", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            },
        )
    }

    // ── 编辑书单（owner）──
    if (editBooklist) {
        val detail = state.detail
        if (detail != null) {
            EditBooklistSheet(
                initialTitle = detail.title,
                initialDescription = detail.description ?: "",
                initialCover = detail.coverUrl ?: "",
                onDismiss = { editBooklist = false },
                onSave = { title, desc, cover ->
                    viewModel.editBooklist(booklistId, title, desc, cover) { res ->
                        res.onSuccess { editBooklist = false }
                            .onFailure { e ->
                                android.widget.Toast.makeText(context, e.message ?: "保存失败", android.widget.Toast.LENGTH_SHORT).show()
                            }
                    }
                },
            )
        }
    }

    // ── 删除书单二次确认（owner）──
    if (deleteConfirm) {
        val detail = state.detail
        if (detail != null) {
            AlertDialog(
                onDismissRequest = { deleteConfirm = false },
                confirmButton = {
                    TextButton(onClick = {
                        deleteConfirm = false
                        viewModel.deleteBooklist(booklistId) { res ->
                            res.onSuccess { onBack() }
                                .onFailure { e ->
                                    android.widget.Toast.makeText(context, e.message ?: "删除失败", android.widget.Toast.LENGTH_SHORT).show()
                                }
                        }
                    }) { Text("删除", color = GlassTokens.SystemRed) }
                },
                dismissButton = { TextButton(onClick = { deleteConfirm = false }) { Text("取消") } },
                title = { Text("删除书单", color = GlassTokens.Label) },
                text = { Text("确定要删除「${detail.title}」吗？删除后该书单对其他用户不可见。", color = GlassTokens.SecondaryLabel) },
                containerColor = GlassTokens.GroupedBackground,
            )
        }
    }

    // ── 编辑书单项备注（owner）──
    editItem?.let { item ->
        EditItemNoteSheet(
            initialNote = item.note ?: "",
            onDismiss = { editItem = null },
            onSave = { note ->
                viewModel.updateItem(booklistId, item.id, note = note.ifBlank { null }) { res ->
                    res.onSuccess { editItem = null }
                        .onFailure { e ->
                            android.widget.Toast.makeText(context, e.message ?: "保存失败", android.widget.Toast.LENGTH_SHORT).show()
                        }
                }
            },
        )
    }
}

@Composable
private fun BooklistItemRow(
    item: BooklistItemDto,
    isOwner: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onClick: () -> Unit,
    onEditNote: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GlassTokens.RadiusLG))
            .background(GlassTokens.GlassFillStrong)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(GlassTokens.RadiusMD)).background(GlassTokens.GlassFill),
            contentAlignment = Alignment.Center,
        ) {
            if (!item.coverUrl.isNullOrEmpty()) {
                AsyncImage(model = item.coverUrl, contentDescription = null, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(GlassTokens.RadiusMD)), contentScale = ContentScale.Crop)
            } else {
                Icon(MetaIcons.MenuBook, null, tint = GlassTokens.TertiaryLabel, modifier = Modifier.size(28.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.title ?: "未知书目", style = MaterialTheme.typography.bodyLarge, color = GlassTokens.Label, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            item.author?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = GlassTokens.SecondaryLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            item.bookUid?.let {
                Text("书号 $it", style = MaterialTheme.typography.labelSmall, color = GlassTokens.SystemBlue, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            item.note?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = GlassTokens.TertiaryLabel, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        if (isOwner) {
            IconButton(onClick = onEditNote, modifier = Modifier.size(32.dp)) {
                Icon(MetaIcons.Edit, contentDescription = "编辑备注", tint = GlassTokens.SecondaryLabel, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onMoveUp, enabled = canMoveUp, modifier = Modifier.size(32.dp)) {
                Icon(MetaIcons.KeyboardArrowUp, contentDescription = "上移", tint = if (canMoveUp) GlassTokens.SecondaryLabel else GlassTokens.TertiaryLabel, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onMoveDown, enabled = canMoveDown, modifier = Modifier.size(32.dp)) {
                Icon(MetaIcons.KeyboardArrowDown, contentDescription = "下移", tint = if (canMoveDown) GlassTokens.SecondaryLabel else GlassTokens.TertiaryLabel, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(MetaIcons.Delete, contentDescription = "从书单移除", tint = GlassTokens.SystemRed, modifier = Modifier.size(16.dp))
            }
        } else {
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(MetaIcons.Delete, contentDescription = "从书单移除", tint = GlassTokens.SecondaryLabel, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditItemNoteSheet(
    initialNote: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var note by remember { mutableStateOf(initialNote) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp).imePadding()) {
            Text("编辑推荐语", style = MaterialTheme.typography.titleLarge, color = GlassTokens.Label, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { if (it.length <= 300) note = it },
                label = { Text("为什么推荐这本书？（可选）") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
            )
            Spacer(Modifier.height(18.dp))
            androidx.compose.material3.Button(
                onClick = { onSave(note) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(GlassTokens.RadiusMD),
            ) {
                Text("保存")
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
