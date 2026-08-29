package com.xiaoswz.reader.ui.detail

import com.xiaoswz.reader.data.api.FundAssetInput
import com.xiaoswz.reader.data.api.HubBookDto

/** 锚定币书 ID（全局硬通货，恒定兑换稳定币的底层） */
const val ANCHOR_BOOK = "B000001"

/**
 * 用户可解锁模版的能力快照（由书圈首页 hub 数据派生）。
 * 用于在客户端评估每个模版的解锁条件——纯展示性门槛，降低上手门槛、增加游戏感。
 */
data class TemplateContext(
    val directorBookCount: Int,   // 是几个书圈的董事
    val heldBookCount: Int,       // 持有（余额/锁仓/投资）几种书币
    val b000001Balance: Int,      // 持有多少枚锚定币
    val subscribedFundCount: Int, // 已认购几个理财产品
)

/** UI 层模版（含当前是否已解锁） */
data class FundTemplateUi(
    val id: String,
    val name: String,
    val emoji: String,
    val risk: String,       // 低 / 中 / 高
    val tagline: String,
    val desc: String,
    val unlockHint: String,
    val unlocked: Boolean,
)

/**
 * 理财包模版定义。
 * - buildAssets：根据「所选书」与「用户全部持仓书」展开成资产构成（书ID:权重）。
 * - isUnlocked：解锁条件。
 */
data class FundTemplate(
    val id: String,
    val name: String,
    val emoji: String,
    val risk: String,
    val tagline: String,
    val desc: String,
    val unlockHint: String,
    val isUnlocked: (TemplateContext) -> Boolean,
    val buildAssets: (selectedBookId: String, myBooks: List<HubBookDto>) -> List<FundAssetInput>,
)

/** 取用户持仓书（按余额+锁仓+投资综合排序，排除某本书） */
private fun topBooks(myBooks: List<HubBookDto>, exclude: String, n: Int): List<HubBookDto> =
    myBooks.filter { it.bookId != exclude }
        .sortedByDescending { it.myBalance + it.myLocked + it.myInvested + it.myShares }
        .take(n)

/** 把若干书按相等权重铺满 100% */
private fun spread(bookIds: List<String>, per: Int): List<FundAssetInput> {
    if (bookIds.isEmpty()) return emptyList()
    val last = bookIds.size - 1
    return bookIds.mapIndexed { i, id ->
        val w = if (i == last) 100 - per * (bookIds.size - 1) else per
        FundAssetInput(id, w.toDouble())
    }
}

/** 全部模版（顺序即展示顺序） */
val FUND_TEMPLATES: List<FundTemplate> = listOf(
    FundTemplate(
        id = "steady",
        name = "稳健基石包",
        emoji = "🛡️",
        risk = "低",
        tagline = "全仓你自己的书圈，最稳的起步选择",
        desc = "100% 配置你所选书圈的书币，无跨书波动，适合第一次发产品。",
        unlockHint = "无门槛 · 所有董事可用",
        isUnlocked = { true },
        buildAssets = { selected, _ -> listOf(FundAssetInput(selected, 100.0)) },
    ),
    FundTemplate(
        id = "anchor",
        name = "锚定护航包",
        emoji = "⚓",
        risk = "低",
        tagline = "70% 你的书 + 30% 锚定币 B000001",
        desc = "加入锚定币对冲，即便你的书圈短期回调，B000001 的刚性背书也能托底净值。",
        unlockHint = "需持有 ≥ 100 枚 B000001",
        isUnlocked = { it.b000001Balance >= 100 },
        buildAssets = { selected, _ ->
            listOf(FundAssetInput(selected, 70.0), FundAssetInput(ANCHOR_BOOK, 30.0))
        },
    ),
    FundTemplate(
        id = "trio",
        name = "三书联动包",
        emoji = "🔗",
        risk = "中",
        tagline = "50% 你的书 + 另两本持仓最多各 25%",
        desc = "把你的书圈与社区里最活跃的另两本捆绑，共享多书圈流量与稳定币产出机会。",
        unlockHint = "需是 ≥ 2 个书圈的董事",
        isUnlocked = { it.directorBookCount >= 2 },
        buildAssets = { selected, myBooks ->
            val others = topBooks(myBooks, selected, 2).map { it.bookId }
            val ids = listOf(selected) + others
            when (others.size) {
                0 -> listOf(FundAssetInput(selected, 100.0))
                1 -> listOf(FundAssetInput(selected, 50.0), FundAssetInput(others[0], 50.0))
                else -> listOf(
                    FundAssetInput(selected, 50.0),
                    FundAssetInput(others[0], 25.0),
                    FundAssetInput(others[1], 25.0),
                )
            }
        },
    ),
    FundTemplate(
        id = "market",
        name = "全市场分散包",
        emoji = "🌐",
        risk = "中",
        tagline = "你的持仓前 5 本书各 20%",
        desc = "把鸡蛋放在多个篮子里，平摊单一书圈风险，吃全市场平均收益。",
        unlockHint = "需持有 ≥ 3 种书币",
        isUnlocked = { it.heldBookCount >= 3 },
        buildAssets = { selected, myBooks ->
            val ids = (listOf(selected) + topBooks(myBooks, selected, 4).map { it.bookId }).take(5)
            spread(ids, 20)
        },
    ),
    FundTemplate(
        id = "hunter",
        name = "稳定币猎手套餐",
        emoji = "💎",
        risk = "高",
        tagline = "1 本主书 + 4 本配角，专为刷稳定币而生",
        desc = "高波动、多书分散，更易在连续正收益日触发稳定币抓取；高风险高上限。",
        unlockHint = "需已认购 ≥ 1 个理财产品",
        isUnlocked = { it.subscribedFundCount >= 1 },
        buildAssets = { selected, myBooks ->
            val others = topBooks(myBooks, selected, 4).map { it.bookId }
            val ids = listOf(selected) + others
            when (others.size) {
                0 -> listOf(FundAssetInput(selected, 100.0))
                1 -> listOf(FundAssetInput(selected, 40.0), FundAssetInput(others[0], 60.0))
                2 -> listOf(FundAssetInput(selected, 40.0), FundAssetInput(others[0], 30.0), FundAssetInput(others[1], 30.0))
                3 -> listOf(FundAssetInput(selected, 40.0), FundAssetInput(others[0], 20.0), FundAssetInput(others[1], 20.0), FundAssetInput(others[2], 20.0))
                else -> listOf(
                    FundAssetInput(selected, 40.0),
                    FundAssetInput(others[0], 15.0),
                    FundAssetInput(others[1], 15.0),
                    FundAssetInput(others[2], 15.0),
                    FundAssetInput(others[3], 15.0),
                )
            }
        },
    ),
)

/** 由 hub 数据计算解锁上下文 */
fun computeTemplateContext(
    directorBooks: List<HubBookDto>,
    allBooks: List<HubBookDto>,
    subscribedFundCount: Int,
    anchorBalance: Int = 0,
): TemplateContext {
    val held = allBooks.count { it.myBalance > 0 || it.myLocked > 0 || it.myInvested > 0 || it.myShares > 0 }
    // 锚定币 B000001 不在书圈 books 列表里，余额由 hub.anchorBalance 单独下发（见 #2 修复）
    val b000001 = anchorBalance
    return TemplateContext(
        directorBookCount = directorBooks.size,
        heldBookCount = held,
        b000001Balance = b000001,
        subscribedFundCount = subscribedFundCount,
    )
}

/** 把模版列表映射为带解锁状态的 UI 模型 */
fun buildTemplateUiList(ctx: TemplateContext): List<FundTemplateUi> =
    FUND_TEMPLATES.map { t ->
        FundTemplateUi(
            id = t.id,
            name = t.name,
            emoji = t.emoji,
            risk = t.risk,
            tagline = t.tagline,
            desc = t.desc,
            unlockHint = t.unlockHint,
            unlocked = t.isUnlocked(ctx),
        )
    }
