package com.xiaoswz.reader.ui

import android.net.Uri
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.xiaoswz.reader.ui.bookstore.BookstoreScreen
import com.xiaoswz.reader.ui.detail.BookDetailScreen
import com.xiaoswz.reader.ui.reader.ReaderScreen
import com.xiaoswz.reader.ui.settings.SettingsScreen
import com.xiaoswz.reader.ui.bookshelf.BookshelfScreen

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
    val icon: ImageVector,
)

private val BottomTabs = listOf(
    BottomTab(Routes.BOOKSTORE, "书城", Icons.Default.Home),
    BottomTab(Routes.BOOKSHELF, "书架", Icons.Default.MenuBook),
    BottomTab(Routes.SETTINGS, "设置", Icons.Default.Settings),
)

// 全应用页面转场：横向滑动 + 淡入（书城→详情→阅读不再硬切）
private val EnterTransitionX: EnterTransition =
    slideInHorizontally(
        initialOffsetX = { it },
        animationSpec = tween(300, easing = FastOutSlowInEasing),
    ) + fadeIn(animationSpec = tween(300))

private val ExitTransitionX: ExitTransition =
    slideOutHorizontally(
        targetOffsetX = { -it },
        animationSpec = tween(300, easing = FastOutSlowInEasing),
    ) + fadeOut(animationSpec = tween(300))

private val PopEnterTransitionX: EnterTransition =
    slideInHorizontally(
        initialOffsetX = { -it },
        animationSpec = tween(300, easing = FastOutSlowInEasing),
    ) + fadeIn(animationSpec = tween(300))

private val PopExitTransitionX: ExitTransition =
    slideOutHorizontally(
        targetOffsetX = { it },
        animationSpec = tween(300, easing = FastOutSlowInEasing),
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
