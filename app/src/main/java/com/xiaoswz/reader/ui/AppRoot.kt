package com.xiaoswz.reader.ui

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
import com.xiaoswz.reader.ui.bookstore.BookstoreScreen
import com.xiaoswz.reader.ui.detail.BookDetailScreen
import com.xiaoswz.reader.ui.reader.ReaderScreen
import com.xiaoswz.reader.ui.settings.SettingsScreen
import com.xiaoswz.reader.ui.bookshelf.BookshelfScreen
import com.xiaoswz.reader.ui.theme.SurfReaderTheme
import kotlinx.coroutines.delay
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.shape.RoundedCornerShape

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
    val themeMode by appSettings.themeModeFlow.collectAsState(initial = AppThemeMode.SYSTEM)
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        else -> systemDark
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

    Scaffold(
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

/** 品牌闪屏：渐变背景 + 图标 + 应用名，约 1.1s 后淡出 */
@Composable
private fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primaryContainer,
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.AutoStories,
                contentDescription = null,
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(24.dp)),
                tint = MaterialTheme.colorScheme.onPrimary,
            )
            Text(
                text = "冲浪阅读",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                text = "畅读每一页",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}
