package com.xiaoswz.reader.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 冲浪阅读 设计系统：Meta（模板 00003）
 * 设计语言：白底灰边 · 黑胶囊 CTA · 钴蓝 #0064E0 主流程/购买 · 语义成功/危险/注意。
 * 中文适配字体 Noto Sans SC（设备含 Noto Sans CJK 时即其观感，系统回退）。
 * 令牌值对齐画布设计稿「冲浪阅读-Meta设计升级」（Ardot）。
 *
 * 注：对象名 WhaleColors / WhaleRadius 保留以兼容既有 40+ 处引用，色值已全部切换为 Meta。
 * 新代码建议直接使用语义命名（OceanMid=钴蓝、Foam=表面柔云、TextPrimary=墨主文字）。
 */
object WhaleColors {

    // ── 强调色（Meta 钴蓝）──
    val OceanMid = Color(0xFF0064E0)        // 主强调：CTA / 选中态 / 链接 / 购买流程
    val OceanDeep = Color(0xFF0457CB)       // 钴蓝深：渐变尽头 / 强对比
    val OceanLight = Color(0xFF0064E0)      // 兼容旧名 → OceanMid
    val WhaleBlue = Color(0xFF0064E0)       // 兼容旧名 → OceanMid
    val WhaleNavy = Color(0xFF0457CB)       // 兼容旧名 → OceanDeep
    val CtaPrimary = Color(0xFF0064E0)      // 兼容旧名 → OceanMid
    val SystemBlue = Color(0xFF0064E0)      // 兼容旧名 → OceanMid
    val SystemBlueDark = Color(0xFF0457CB)  // 兼容旧名 → OceanDeep

    // ── 背景（Meta 白 / 浅灰）──
    val OceanBase = Color(0xFFFFFFFF)       // Canvas 白
    val OceanSurface = Color(0xFFFFFFFF)    // 卡片浅底
    val Foam = Color(0xFFF1F4F7)           // 表面柔云 / 分组背景

    // ── 文字（墨阶）──
    val TextPrimary = Color(0xFF0A1317)    // Ink Deep：主文字
    val TextSecondary = Color(0xFF5D6C7B)  // Steel：辅助文字
    val TextDisabled = Color(0xFF8595A4)    // 禁用 / 占位
    val TextAccent = Color(0xFF0064E0)      // 强调文字：链接 / 数值

    // ── 语义 / 状态色（A股：红涨绿跌）──
    val LoveRose = Color(0xFFE41E3F)       // 涨 / 危险（Critical）
    val SuccessMint = Color(0xFF31A24C)    // 跌 / 成功（Success）
    val WarningGold = Color(0xFFF2A918)     // 注意（Attention）
    val ErrorCoral = Color(0xFFE41E3F)

    // ── 玻璃材质（Meta 浅色：近白底 + 发丝边，去除彩色光斑）──
    val GlassSurface = Color(0xFFFFFFFF).copy(alpha = 0.95f)
    val GlassBorder = Color(0xFFDEE1E6)    // 发丝边
    val GlassHighlight = Color(0xFFFFFFFF).copy(alpha = 0.60f)

    // ── 渐变配方 ── 钴蓝主按钮渐变 #0064E0 → #0457CB
    val GradientButton: Brush
        get() = Brush.verticalGradient(listOf(OceanMid, OceanDeep))

    val GradientOcean: Brush
        get() = Brush.verticalGradient(listOf(OceanMid, OceanDeep))

    // 页面背景：白 → 极浅灰，干净无彩
    val GradientBackground: Brush
        get() = Brush.verticalGradient(
            listOf(Color(0xFFFFFFFF), Color(0xFFF6F7F9)),
        )

    val GradientCard: Brush
        get() = Brush.verticalGradient(
            listOf(Color(0xFFFFFFFF), Color(0xFFFBFBFC)),
        )

    val GradientSplash: Brush
        get() = Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF6F7F9)))

    val GradientShelf: Brush
        get() = GradientBackground

    val GradientReaderEdge: Brush
        get() = GradientBackground
}

/** 圆角系统（Meta 偏紧凑：xs2 · sm4 · md8 · lg12 · xl16 · full） */
object WhaleRadius {
    val XS = 2.dp
    val SM = 4.dp
    val MD = 8.dp
    val LG = 12.dp
    val XL = 16.dp
    val Full = 999.dp
}
