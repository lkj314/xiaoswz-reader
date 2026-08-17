package com.xiaoswz.reader.ui

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.xiaoswz.reader.data.settings.AppSettingsRepository
import com.xiaoswz.reader.data.settings.AppThemeMode
import com.xiaoswz.reader.data.bookshelf.BookshelfRepository
import com.xiaoswz.reader.data.api.ApiClient
import com.xiaoswz.reader.data.api.BackendClient
import com.xiaoswz.reader.data.sync.SyncRepository
import com.xiaoswz.reader.data.auth.AuthRepository
import com.xiaoswz.reader.ui.bookstore.HomeScreen
import com.xiaoswz.reader.ui.bookstore.BookLibraryScreen
import com.xiaoswz.reader.ui.community.CommunityScreen
import com.xiaoswz.reader.ui.booklist.BooklistsScreen
import com.xiaoswz.reader.ui.booklist.BooklistDetailScreen
import com.xiaoswz.reader.ui.detail.BookDetailScreen
import com.xiaoswz.reader.ui.detail.CharacterDetailScreen
import com.xiaoswz.reader.ui.creator.CreatorHubScreen
import com.xiaoswz.reader.ui.creator.CharacterAdminScreen
import com.xiaoswz.reader.ui.creator.BookAdminScreen
import com.xiaoswz.reader.ui.creator.AnnouncementAdminScreen
import com.xiaoswz.reader.ui.reader.ReaderScreen
import com.xiaoswz.reader.ui.settings.SettingsScreen
import com.xiaoswz.reader.ui.settings.UserCenterScreen
import com.xiaoswz.reader.ui.settings.AccountScreen
import com.xiaoswz.reader.ui.bookshelf.BookshelfScreen
import com.xiaoswz.reader.ui.profile.UserProfileScreen
import com.xiaoswz.reader.ui.profile.ReadingStatsScreen
import com.xiaoswz.reader.ui.plugin.PluginPlazaScreen
import com.xiaoswz.reader.ui.theme.SurfReaderTheme
import com.xiaoswz.reader.ui.components.WhaleBackground
import com.xiaoswz.reader.ui.theme.GlassTokens
import com.xiaoswz.reader.R
import kotlinx.coroutines.delay
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape

object Routes {
    const val HOME = "home"
    const val BOOKLIBRARY = "booklibrary"
    const val BOOKSHELF = "bookshelf"
    const val COMMUNITY = "community"
    const val BOOKLISTS = "booklists"
    const val SETTINGS = "settings"
    const val ACCOUNT = "account"
    const val DETAIL = "detail/{slug}"
    const val READER = "reader/{bookSlug}/{chapterId}"
    const val CHARACTER = "character/{characterId}" // 0.14.0 角色互动：角色详情
    const val BOOKLIST_DETAIL = "booklist/{id}"
    const val USER_PROFILE = "user/{id}"
    const val READING_STATS = "reading-stats"
    const val USER_CENTER = "user-center"
    const val PLUGIN_PLAZA = "plugin-plaza" // 0.12.0 创意工坊 · 插件广场
    const val CREATOR = "creator" // 0.14.1 创作者中心：App 内嵌管理模块（仅 admin）
    const val CHARACTER_ADMIN = "character-admin/{src}/{bookId}" // 角色录入（指定书籍）
    const val BOOK_ADMIN = "book-admin" // 书籍元数据编辑
    const val ANNOUNCEMENT_ADMIN = "announcement-admin" // 公告管理

    // slug 可能含中文，必须 URL 编码后再拼路由
    fun detail(slug: String) = "detail/${Uri.encode(slug)}"
    fun reader(bookSlug: String, chapterId: String) =
        "reader/${Uri.encode(bookSlug)}/${Uri.encode(chapterId)}"
    fun character(characterId: String) = "character/${Uri.encode(characterId)}"
    fun characterAdmin(src: String, bookId: String) =
        "character-admin/${Uri.encode(src)}/${Uri.encode(bookId)}"
    fun bookAdmin() = "book-admin"
    fun announcementAdmin() = "announcement-admin"
    fun booklist(id: String) = "booklist/$id"
    fun user(id: String) = "user/$id"
}

/** 顶层目标（显示底部导航栏），详情/阅读器为覆盖式全屏，不显示底栏 */
private val TopLevelRoutes = setOf(
    Routes.HOME,
    Routes.BOOKSHELF,
    Routes.COMMUNITY,
    Routes.BOOKLISTS,
    Routes.PLUGIN_PLAZA,
    Routes.SETTINGS,
)

private data class BottomTab(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

private val BottomTabs = listOf(
    BottomTab(Routes.HOME, "首页", Icons.Default.Home),
    BottomTab(Routes.BOOKSHELF, "书架", Icons.Default.MenuBook),
    BottomTab(Routes.COMMUNITY, "书友圈", Icons.Default.Forum),
    BottomTab(Routes.BOOKLISTS, "书单", Icons.Default.MenuBook),
    BottomTab(Routes.PLUGIN_PLAZA, "工坊", Icons.Default.Extension),
    BottomTab(Routes.SETTINGS, "设置", Icons.Default.Settings),
)

// 全应用页面转场：横向滑动 + 淡入（书城→详情→阅读不再硬切）
private val EnterTransitionX: androidx.compose.animation.EnterTransition =
    androidx.compose.animation.slideInHorizontally(
        initialOffsetX = { it },
        animationSpec = tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing),
    ) + fadeIn(animationSpec = tween(300))

private val ExitTransitionX: androidx.compose.animation.ExitTransition =
    androidx.compose.animation.slideOutHorizontally(
        targetOffsetX = { -it },
        animationSpec = tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing),
    ) + fadeOut(animationSpec = tween(300))

private val PopEnterTransitionX: androidx.compose.animation.EnterTransition =
    androidx.compose.animation.slideInHorizontally(
        initialOffsetX = { -it },
        animationSpec = tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing),
    ) + fadeIn(animationSpec = tween(300))

private val PopExitTransitionX: androidx.compose.animation.ExitTransition =
    androidx.compose.animation.slideOutHorizontally(
        targetOffsetX = { it },
        animationSpec = tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing),
    ) + fadeOut(animationSpec = tween(300))

/** 底部导航切换：回到起始目的地之上再跳转，避免堆叠 */
private fun NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val appSettings = remember { AppSettingsRepository(context.applicationContext) }
    // 启动即恢复书架：清空过大的 data: 封面（防 CursorWindow 溢出崩溃），并联网按 slug 重新拉取封面写回
    LaunchedEffect(Unit) {
        val shelf = BookshelfRepository(context.applicationContext)
        shelf.repairCovers { slug -> ApiClient.api.getBookDetail(bookId = slug).coverUrl }
        // 先把 DataStore 里残留的 vercel.app 旧后端地址迁移到默认局域网地址（0.6.5）
        appSettings.migrateBackendUrlIfNeeded()
        // 无条件注入后端身份（设备头 + 地址），保证投票/评分/评论等互动调用有身份，
        // 与云同步解耦（同步可能被节流跳过，但互动调用不应等同步）。
        BackendClient.setDeviceId(appSettings.getDeviceId())
        BackendClient.setBaseUrl(appSettings.getBackendBaseUrl())
        // 恢复登录态（Bearer 令牌注入）+ 刷新角色 / 禁言状态；
        // 失败静默，绝不阻塞本地阅读。
        AuthRepository.applyStoredToken()
        AuthRepository.refreshSession()
        // 启动节流云同步（离线优先，不阻塞 UI；后端未启动也不影响本地使用）
        SyncRepository(context.applicationContext).syncIfNeeded()
    }
    val themeMode by appSettings.themeModeFlow.collectAsState(initial = AppThemeMode.SYSTEM)
    // v2.1 改为浅色玻璃默认：SYSTEM 跟随改为强制浅色；用户仍可在设置中选择「深色」
    val darkTheme = when (themeMode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        else -> false
    }
    var showSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { delay(1100); showSplash = false }

    SurfReaderTheme(darkTheme = darkTheme) {
        AnimatedVisibility(
            visible = showSplash,
            exit = fadeOut(animationSpec = tween(300)),
        ) { SplashScreen() }
        AnimatedVisibility(
            visible = !showSplash,
            enter = fadeIn(animationSpec = tween(500)),
        ) { AppShell() }
    }
}

@Composable
private fun AppShell() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in TopLevelRoutes
    // 阅读器保留自身独立主题背景，不叠加全局深海图
    val showGlobalBg = currentRoute != Routes.READER

    Box(modifier = Modifier.fillMaxSize()) {
        if (showGlobalBg) {
            WhaleBackground {}
        }
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                AnimatedVisibility(visible = showBottomBar) {
                    NavigationBar {
                        BottomTabs.forEach { tab ->
                            NavigationBarItem(
                                selected = currentRoute == tab.route,
                                onClick = { navController.navigateTopLevel(tab.route) },
                                icon = { Icon(tab.icon, contentDescription = null) },
                                label = { Text(tab.label) },
                            )
                        }
                    }
                }
            },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                composable(Routes.HOME) {
                    HomeScreen(
                        onBookClick = { slug -> navController.navigate(Routes.detail(slug)) },
                        onBrowseLibrary = { navController.navigate(Routes.BOOKLIBRARY) },
                    )
                }

                composable(Routes.BOOKLIBRARY) {
                    BookLibraryScreen(
                        onBookClick = { slug -> navController.navigate(Routes.detail(slug)) },
                    )
                }

                composable(Routes.BOOKSHELF) {
                    BookshelfScreen(
                        onBookClick = { slug, chapterId ->
                            navController.navigate(Routes.reader(slug, chapterId))
                        },
                    )
                }

                composable(Routes.COMMUNITY) {
                    CommunityScreen(
                        onAccountClick = { navController.navigate(Routes.ACCOUNT) },
                        onUserClick = { id -> navController.navigate(Routes.user(id)) },
                        onBooklistClick = { id -> navController.navigate(Routes.booklist(id)) },
                        onReadingStats = { navController.navigate(Routes.READING_STATS) },
                    )
                }

                composable(Routes.BOOKLISTS) {
                    BooklistsScreen(
                        onBack = { navController.popBackStack() },
                        onBooklistClick = { id -> navController.navigate(Routes.booklist(id)) },
                        onAccountClick = { navController.navigate(Routes.ACCOUNT) },
                    )
                }

                composable(
                    route = Routes.BOOKLIST_DETAIL,
                    arguments = listOf(navArgument("id") { type = NavType.StringType }),
                    enterTransition = { EnterTransitionX },
                    exitTransition = { ExitTransitionX },
                    popEnterTransition = { PopEnterTransitionX },
                    popExitTransition = { PopExitTransitionX },
                ) { entry ->
                    val id = entry.arguments?.getString("id").orEmpty()
                    BooklistDetailScreen(
                        booklistId = id,
                        onBack = { navController.popBackStack() },
                        onUserClick = { uid -> navController.navigate(Routes.user(uid)) },
                        onBookClick = { slug -> navController.navigate(Routes.detail(slug)) },
                    )
                }

                composable(
                    route = Routes.USER_PROFILE,
                    arguments = listOf(navArgument("id") { type = NavType.StringType }),
                    enterTransition = { EnterTransitionX },
                    exitTransition = { ExitTransitionX },
                    popEnterTransition = { PopEnterTransitionX },
                    popExitTransition = { PopExitTransitionX },
                ) { entry ->
                    val id = entry.arguments?.getString("id").orEmpty()
                    UserProfileScreen(
                        userId = id,
                        onBack = { navController.popBackStack() },
                        onBooklistClick = { bid -> navController.navigate(Routes.booklist(bid)) },
                        onBookClick = { slug -> navController.navigate(Routes.detail(slug)) },
                    )
                }

                composable(
                    route = Routes.READING_STATS,
                    enterTransition = { EnterTransitionX },
                    exitTransition = { ExitTransitionX },
                    popEnterTransition = { PopEnterTransitionX },
                    popExitTransition = { PopExitTransitionX },
                ) {
                    ReadingStatsScreen(
                        onBack = { navController.popBackStack() },
                        onAccountClick = { navController.navigate(Routes.ACCOUNT) },
                    )
                }

                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        onBack = { navController.popBackStack() },
                        onUserCenterClick = { navController.navigate(Routes.USER_CENTER) },
                        onCreatorClick = { navController.navigate(Routes.CREATOR) },
                    )
                }

                composable(Routes.CREATOR) {
                    CreatorHubScreen(
                        onBack = { navController.popBackStack() },
                        onCharacters = { navController.navigate(Routes.characterAdmin("", "")) },
                        onBooks = { navController.navigate(Routes.bookAdmin()) },
                        onAnnouncements = { navController.navigate(Routes.announcementAdmin()) },
                    )
                }

                composable(
                    route = Routes.CHARACTER_ADMIN,
                    arguments = listOf(
                        navArgument("src") { type = NavType.StringType },
                        navArgument("bookId") { type = NavType.StringType },
                    ),
                    enterTransition = { EnterTransitionX },
                    exitTransition = { ExitTransitionX },
                    popEnterTransition = { PopEnterTransitionX },
                    popExitTransition = { PopExitTransitionX },
                ) { entry ->
                    val src = entry.arguments?.getString("src").orEmpty()
                    val bookId = entry.arguments?.getString("bookId").orEmpty()
                    CharacterAdminScreen(
                        bookSourceId = src,
                        bookId = bookId,
                        onBack = { navController.popBackStack() },
                    )
                }

                composable(
                    route = Routes.BOOK_ADMIN,
                    enterTransition = { EnterTransitionX },
                    exitTransition = { ExitTransitionX },
                    popEnterTransition = { PopEnterTransitionX },
                    popExitTransition = { PopExitTransitionX },
                ) {
                    BookAdminScreen(
                        onBack = { navController.popBackStack() },
                        onManageCharacters = { src, id -> navController.navigate(Routes.characterAdmin(src, id)) },
                    )
                }

                composable(
                    route = Routes.ANNOUNCEMENT_ADMIN,
                    enterTransition = { EnterTransitionX },
                    exitTransition = { ExitTransitionX },
                    popEnterTransition = { PopEnterTransitionX },
                    popExitTransition = { PopExitTransitionX },
                ) {
                    AnnouncementAdminScreen(onBack = { navController.popBackStack() })
                }

                composable(Routes.PLUGIN_PLAZA) {
                    PluginPlazaScreen(
                        onBack = { navController.popBackStack() },
                    )
                }

                composable(Routes.USER_CENTER) {
                    UserCenterScreen(
                        onBack = { navController.popBackStack() },
                        onAccountClick = { navController.navigate(Routes.ACCOUNT) },
                        onReadingStats = { navController.navigate(Routes.READING_STATS) },
                    )
                }

                composable(Routes.ACCOUNT) {
                    AccountScreen(onBack = { navController.popBackStack() })
                }

                composable(
                    route = Routes.DETAIL,
                    arguments = listOf(navArgument("slug") { type = NavType.StringType }),
                    enterTransition = { EnterTransitionX },
                    exitTransition = { ExitTransitionX },
                    popEnterTransition = { PopEnterTransitionX },
                    popExitTransition = { PopExitTransitionX },
                ) { entry ->
                    val slug = entry.arguments?.getString("slug").orEmpty()
                    BookDetailScreen(
                        slug = slug,
                        onBack = { navController.popBackStack() },
                        onChapterClick = { chapterId ->
                            navController.navigate(Routes.reader(slug, chapterId))
                        },
                        onBookClick = { s -> navController.navigate(Routes.detail(s)) },
                        onAccountClick = { navController.navigate(Routes.ACCOUNT) },
                        onCharacterClick = { characterId ->
                            navController.navigate(Routes.character(characterId))
                        },
                    )
                }

                composable(
                    route = Routes.CHARACTER,
                    arguments = listOf(navArgument("characterId") { type = NavType.StringType }),
                    enterTransition = { EnterTransitionX },
                    exitTransition = { ExitTransitionX },
                    popEnterTransition = { PopEnterTransitionX },
                    popExitTransition = { PopExitTransitionX },
                ) { entry ->
                    val characterId = entry.arguments?.getString("characterId").orEmpty()
                    CharacterDetailScreen(
                        characterId = characterId,
                        onBack = { navController.popBackStack() },
                        onAccountClick = { navController.navigate(Routes.ACCOUNT) },
                    )
                }

                composable(
                    route = Routes.READER,
                    arguments = listOf(
                        navArgument("bookSlug") { type = NavType.StringType },
                        navArgument("chapterId") { type = NavType.StringType },
                    ),
                    enterTransition = { EnterTransitionX },
                    exitTransition = { ExitTransitionX },
                    popEnterTransition = { PopEnterTransitionX },
                    popExitTransition = { PopExitTransitionX },
                ) { entry ->
                    val bookSlug = entry.arguments?.getString("bookSlug").orEmpty()
                    val chapterId = entry.arguments?.getString("chapterId").orEmpty()
                    ReaderScreen(
                        bookSlug = bookSlug,
                        chapterId = chapterId,
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}

/** 品牌闪屏：全屏 iOS 玻璃风 — 可见渐变背景 + 纵向铺开的品牌内容（无卡片包裹） */
@Composable
private fun SplashScreen() {
    Box(modifier = Modifier.fillMaxSize()) {
        // 渐变背景：比内页更饱和，确保启动页有明确的色彩存在感
        Box(modifier = Modifier.fillMaxSize()) {
            // 基础渐变：从浅蓝白到极浅紫灰
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFFE8F0FE), // 浅蓝（顶部）
                                Color(0xFFF5F3FA), // 极浅紫灰（底部）
                            ),
                        ),
                    ),
            )
            // 左上光斑（蓝，更明显）
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset((-100).dp, (-120).dp)
                    .size(420.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color(0xFFA5C8FF).copy(alpha = 0.65f),
                                Color(0xFFA5C8FF).copy(alpha = 0f),
                            ),
                        ),
                    ),
            )
            // 右下光斑（薰衣草）
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset((80).dp, (100).dp)
                    .size(460.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color(0xFFC4B5FD).copy(alpha = 0.55f),
                                Color(0xFFC4B5FD).copy(alpha = 0f),
                            ),
                        ),
                    ),
            )
            // 中右光斑（薄荷）
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset((60).dp, (-80).dp)
                    .size(360.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color(0xFF9DD8C4).copy(alpha = 0.45f),
                                Color(0xFF9DD8C4).copy(alpha = 0f),
                            ),
                        ),
                    ),
            )
        }

        // 内容区：纵向分布，撑满屏幕
        var appeared by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { appeared = true }

        AnimatedVisibility(
            visible = appeared,
            enter = fadeIn(animationSpec = tween(600)) +
                scaleIn(
                    initialScale = 0.92f,
                    animationSpec = tween(600, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // 品牌徽标：大号圆形，系统蓝渐变底 + 白色海浪 logo
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(GlassTokens.GradientButton),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_surf_logo),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        colorFilter = ColorFilter.tint(Color.White, BlendMode.SrcIn),
                    )
                }

                Spacer(Modifier.height(32.dp))

                Text(
                    text = "冲浪阅读",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = GlassTokens.Label,
                    letterSpacing = 3.sp,
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "畅读每一页",
                    style = MaterialTheme.typography.bodyLarge,
                    color = GlassTokens.SecondaryLabel.copy(alpha = 0.75f),
                    letterSpacing = 1.sp,
                )
            }
        }

        // 底部加载指示条
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 64.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                modifier = Modifier
                    .size(width = 52.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(GlassTokens.SystemBlue.copy(alpha = 0.35f)),
            )
        }
    }
}
