package com.xiaoswz.reader.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.xiaoswz.reader.R

/**
 * Meta 设计语言线性图标库入口。
 *
 * 全站统一从这里取图标，替换原生 Material Icons：
 * 24dp 视口 / 1.75dp 圆头描边 / 纯黑可 tint，符合 Meta 中性线性图标规范。
 *
 * 用法：`Icon(MetaIcons.Star, contentDescription = null, tint = WhaleColors.SystemBlue)`
 *
 * 暴露为 ImageVector，可直接用于 `Icon(...)` 与 `imageVector =` / `icon =` 参数。
 */
object MetaIcons {
    val AccountBalance: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_bank)

    val AccountCircle: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_account_circle)

    val Add: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_add)

    val ArrowBack: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_arrow_back)

    val ArrowForward: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_chevron_right)

    val AutoStories: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_book_open)

    val Block: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_block)

    val BorderOuter: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_frame)

    val BugReport: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_bug)

    val Campaign: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_megaphone)

    val Chat: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_chat)

    val ChatBubbleOutline: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_chat)

    val Close: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_close)

    val Cloud: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_cloud)

    val Create: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_edit)

    val Delete: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_delete)

    val Edit: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_edit)

    val EmojiEvents: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_trophy)

    val Extension: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_extension)

    val Favorite: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_favorite)

    val FavoriteBorder: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_heart)

    val FormatIndentIncrease: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_indent)

    val FormatSize: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_format_size)

    val Forum: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_forum)

    val Gavel: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_gavel)

    val GraphicEq: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_equalizer)

    val Groups: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_groups)

    val Info: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_info)

    val KeyboardArrowDown: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_chevron_down)

    val KeyboardArrowUp: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_chevron_up)

    val Lightbulb: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_lightbulb)

    val Lock: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_lock)

    val Menu: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_menu)

    val MenuBook: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_menu_book)

    val MonetizationOn: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_coin)

    val NavigateBefore: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_chevron_left)

    val NavigateNext: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_chevron_right)

    val Nightlight: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_moon)

    val Palette: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_palette)

    val Person: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_person)

    val PersonAdd: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_person_add)

    val PieChart: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_pie_chart)

    val PlayArrow: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_play)

    val PlaylistAdd: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_playlist_add)

    val Savings: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_safe)

    val Search: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_search)

    val Send: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_send)

    val Settings: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_settings)

    val Share: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_share)

    val Star: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_star)

    val StarBorder: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_star)

    val Stop: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_stop)

    val Storage: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_storage)

    val Subject: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_subject)

    val SwapHoriz: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_swap)

    val SystemUpdate: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_update)

    val Timer: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_timer)

    val TrendingDown: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_trending_down)

    val TrendingUp: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_trending_up)

    val VerticalAlignCenter: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_align_center)

    val VolumeUp: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_volume_up)

    val Warning: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.ic_meta_warning)

}
