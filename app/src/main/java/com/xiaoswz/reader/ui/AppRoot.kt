package com.xiaoswz.reader.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.xiaoswz.reader.ui.bookstore.BookstoreScreen
import com.xiaoswz.reader.ui.detail.BookDetailScreen
import com.xiaoswz.reader.ui.reader.ReaderScreen

object Routes {
    const val BOOKSTORE = "bookstore"
    const val DETAIL = "detail/{slug}"
    const val READER = "reader/{bookSlug}/{chapterId}"

    // slug 可能含中文，必须 URL 编码后再拼路由
    fun detail(slug: String) = "detail/${Uri.encode(slug)}"
    fun reader(bookSlug: String, chapterId: String) =
        "reader/${Uri.encode(bookSlug)}/${Uri.encode(chapterId)}"
}

@Composable
fun AppRoot() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.BOOKSTORE) {

        composable(Routes.BOOKSTORE) {
            BookstoreScreen(
                onBookClick = { slug -> navController.navigate(Routes.detail(slug)) },
            )
        }

        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("slug") { type = NavType.StringType }),
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
