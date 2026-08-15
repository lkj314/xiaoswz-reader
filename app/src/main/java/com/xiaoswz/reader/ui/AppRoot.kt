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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
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
import com.xiaoswz.reader.ui.bookstore.BookstoreScreen
import com.xiaoswz.reader.ui.detail.BookDetailScreen
import com.xiaoswz.reader.ui.reader.ReaderScreen
import com.xiaoswz.reader.ui.settings.SettingsScreen
import com.xiaoswz.reader.ui.bookshelf.BookshelfScreen
import com.xiaoswz.reader.ui.theme.SurfReaderTheme
import com.xiaoswz.reader.ui.components.WhaleBackground
import com.xiaoswz.reader.ui.components.LiquidGlassCard
import com.xiaoswz.reader.ui.theme.GlassTokens
import com.xiaoswz.reader.R
import kotlinx.coroutines.delay
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape

object Routes {
    const val BOOKSTORE = "bookstore"
    const val BOOKSHELF = "bookshelf"
    const val SETTINGS = "settings"
    const val DETAIL = "detail/{slug}"
    const val READER = "reader/{bookSlug}/{chapterId}"

    // slug 可能含中文，必须 URL 编码后再拼路由
    fun detail(slug: String) = "detail/${Uri.encode(slug)}"
    fun reader(bookSlug: String, chapterId: String) =
        "reader/${Uri.encode(bookSlug)}/${Uri.encode(chapterId)}"
}

/** 三个顶层目标（显示底部导航栏），详情/阅读器为覆盖式全屏，不显示底栏 */
private val TopLevelRoutes = setOf(Routes.BOOKSTORE, Routes.BOOKSHELF, Routes.SETTINGS)

private data class BottomTab(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

private val BottomTabs = listOf(
    BottomTab(Routes.BOOKSTORE, "书城", Icons.Default.Home),
    BottomTab(Routes.BOOKSHELF, "书架", Icons.Default.MenuBook),
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
        popUpTo(Routes.BOOKSTORE) { saveState = true }
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
                startDestination = Routes.BOOKSTORE,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                composable(Routes.BOOKSTORE) {
                    BookstoreScreen(
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

                composable(Routes.SETTINGS) {
                    SettingsScreen(onBack = { navController.popBackStack() })
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

/** 品牌闪屏：纯 iOS 玻璃风 — 浅色渐变背景 + 悬浮玻璃卡（海浪 logo + 品牌字标），无任何角色图 */
@Composable
private fun SplashScreen() {
    Box(modifier = Modifier.fillMaxSize()) {
        // 与内页一致的浅色玻璃背景，保证启动→内页视觉连续
        WhaleBackground {}
        // 中央悬浮玻璃卡
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            var appeared by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { appeared = true }
            AnimatedVisibility(
                visible = appeared,
                enter = fadeIn(animationSpec = tween(500)) +
                    scaleIn(
                        initialScale = 0.92f,
                        animationSpec = tween(500, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                    ),
            ) {
                LiquidGlassCard(
                    modifier = Modifier.width(300.dp),
                    radius = GlassTokens.RadiusXL,
                    fillAlpha = 0.7f,
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 10.dp),
                    ) {
                        // 海浪 logo（系统蓝着色，置于圆形玻璃徽标内）
                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .clip(CircleShape)
                                .background(GlassTokens.GradientButton),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_surf_logo),
                                contentDescription = null,
                                modifier = Modifier.size(46.dp),
                                colorFilter = ColorFilter.tint(Color.White, BlendMode.SrcIn),
                            )
                        }
                        Spacer(Modifier.height(20.dp))
                        Text(
                            text = "冲浪阅读",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = GlassTokens.Label,
                            letterSpacing = 2.sp,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "畅读每一页",
                            style = MaterialTheme.typography.bodyMedium,
                            color = GlassTokens.SecondaryLabel,
                        )
                    }
                }
            }
        }
        // 底部极简加载指示
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                modifier = Modifier
                    .padding(bottom = 56.dp)
                    .size(width = 44.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(GlassTokens.SystemBlue.copy(alpha = 0.3f)),
            )
        }
    }
}
