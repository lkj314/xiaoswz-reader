package com.xiaoswz.reader.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Meta 书封色板（模板 00003 延伸）
 *
 * 用途：封面缺失 / 加载中时，用书名哈希确定性地生成一枚「排版式书封」占位，
 * 替代过去的纯灰空框。选色原则：
 * - 低饱和柔彩（pastel），与白底灰边卡片体系同频，不抢真实封面的视觉；
 * - 每对均为「上浅 → 下略深」的同色系双色，保证墨阶文字在任何一对上都有足够对比；
 * - 10 组循环，避免相邻卡片撞色过于集中。
 */
@Immutable
data class CoverPalette(
    val top: Color,
    val bottom: Color,
    /** 书名墨色（深，保证可读） */
    val ink: Color,
    /** 作者 / 次要信息墨色 */
    val inkSoft: Color,
)

object MetaCoverPalette {

    /** 10 组柔彩：雾蓝 / 沙 / 鼠尾草 / 陶土 / 淡紫 / 亚麻 / 藕粉 / 薄荷 / 石板 / 麦 */
    private val PALETTES = listOf(
        CoverPalette(Color(0xFFE6EFF9), Color(0xFFC6D9EE), Color(0xFF0A1317), Color(0xFF5A6B7C)), // Mist
        CoverPalette(Color(0xFFF5EBDD), Color(0xFFE7D6BE), Color(0xFF0A1317), Color(0xFF6B5F4E)), // Sand
        CoverPalette(Color(0xFFE1EDE2), Color(0xFFC4DACA), Color(0xFF0A1317), Color(0xFF536753)), // Sage
        CoverPalette(Color(0xFFF6E2DA), Color(0xFFE8C7BB), Color(0xFF0A1317), Color(0xFF6E5750)), // Clay
        CoverPalette(Color(0xFFEAE5F5), Color(0xFFD3CAEC), Color(0xFF0A1317), Color(0xFF5C556E)), // Lilac
        CoverPalette(Color(0xFFF0EDE7), Color(0xFFDDD8CE), Color(0xFF0A1317), Color(0xFF66625A)), // Linen
        CoverPalette(Color(0xFFF9E5EA), Color(0xFFF0CAD3), Color(0xFF0A1317), Color(0xFF6F5459)), // Blush
        CoverPalette(Color(0xFFDCF2EA), Color(0xFFBDE3D5), Color(0xFF0A1317), Color(0xFF4E6A5F)), // Mint
        CoverPalette(Color(0xFFE7EBEF), Color(0xFFCCD3DA), Color(0xFF0A1317), Color(0xFF5A626A)), // Slate
        CoverPalette(Color(0xFFF7F0DA), Color(0xFFEBDDBB), Color(0xFF0A1317), Color(0xFF6A6146)), // Wheat
    )

    /** FNV-1a 32 位哈希，稳定且与 JVM 版本无关 */
    private fun hash(seed: String): Long {
        var h: Long = 0x811C9DC5L
        for (ch in seed) {
            h = h xor (ch.code and 0xFF).toLong()
            h = (h * 0x01000193L) and 0xFFFFFFFFL
        }
        return h
    }

    /**
     * 由书名（或任意种子）确定性取色。同一本书在任何界面、任何时候都拿到同一配色，
     * 避免列表刷新时封面颜色跳变。
     */
    fun of(seed: String): CoverPalette {
        if (seed.isBlank()) return PALETTES[0]
        val idx = ((hash(seed) and 0xFFFFFFFFL).toInt()) % PALETTES.size
        return PALETTES[idx]
    }

    /** 封面渐变：左上 → 右下（需传入实际像素尺寸，避免 Infinity 造成的着色器退化） */
    fun brush(palette: CoverPalette, widthPx: Float, heightPx: Float): Brush {
        val w = if (widthPx.isFinite() && widthPx > 0f) widthPx else 1f
        val h = if (heightPx.isFinite() && heightPx > 0f) heightPx else 1f
        return Brush.linearGradient(
            colors = listOf(palette.top, palette.bottom),
            start = Offset(0f, 0f),
            end = Offset(w, h),
        )
    }

    /** 扁平单色兜底（极小尺寸 / 无法测量时） */
    fun flat(palette: CoverPalette): Color = palette.bottom
}
