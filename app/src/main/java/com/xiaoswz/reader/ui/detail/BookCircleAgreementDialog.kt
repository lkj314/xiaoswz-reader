package com.xiaoswz.reader.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.xiaoswz.reader.ui.components.MetaButton

private sealed class AgrBlock {
    data class H(val text: String) : AgrBlock()      // 小节标题
    data class P(val text: String) : AgrBlock()      // 正文段落
    data class B(val text: String) : AgrBlock()      // 要点（圆点）
    data class Q(val text: String) : AgrBlock()      // 加粗引述（金句）
}

/** 用户需知正文（与《书圈用户需知-范文.md》一致） */
private val AGREEMENT_BLOCKS = listOf(
    AgrBlock.P("第一次进入书圈前，请花一分钟读完这段话。它不是枯燥的条款，而是「书圈」与「书币」这套系统存在的原因，以及它们真正的含义。"),

    AgrBlock.H("一、为什么会有书圈，和看起来这么「繁杂」的书币？"),
    AgrBlock.P("你可能会想：一个看小说的 App，为什么要有「书圈」「书币」「董事会」「交易所」「稳定币」这些东西？"),
    AgrBlock.P("答案很简单，也很认真：书圈本质是一个「金融模拟器」。"),
    AgrBlock.P("我们做书圈，不是要塞给你一套氪金玩法，而是想借着你热爱的 IP 与故事，让你亲手感受经济是怎么运转的、货币是怎么流通的。你会在书圈里经历铸造、通胀预期、持股、治理、交易、套利、破产退市——这些在现实世界里遥远而抽象的概念，在这里会变成你钱包里真实跳动的数字和份额。"),
    AgrBlock.P("所以看似繁杂，是因为它诚实地复刻了一个经济体该有的样子。复杂，正是「经济」本身的样子。"),

    AgrBlock.H("二、书圈是什么？"),
    AgrBlock.P("书圈 = 一个以某部作品为边界的独立小经济体。"),
    AgrBlock.P("每一部作品（每一「书」）都拥有自己专属的硬通货——也就是这套书圈里的「书币」。它像一个独立国家的货币：有自己的发行量、自己的交易所、自己的董事会和持有者。"),
    AgrBlock.B("每个书圈一次性铸造 10 万枚书币，此后绝不增发；"),
    AgrBlock.B("系统不付息、不分红、不回收——书币的价值完全由圈内人与市场决定；"),
    AgrBlock.B("当书圈的「国库」归零，领取窗口自动关闭。"),
    AgrBlock.P("你在这个书圈里发表过审核通过的章评、提交过角色标签，就获得了参与这个经济体的资格。"),

    AgrBlock.H("三、书币到底是什么？——它锚定的是「权力」，不是数字"),
    AgrBlock.P("这是整套设计最核心的一句话，请记住："),
    AgrBlock.Q("所有书币，本质上都是「权力对价」。书币锚定的从来不是冰冷的数字，而是权力本身。"),

    AgrBlock.H("1. 对读者：书币 = 书圈里的「话语权」"),
    AgrBlock.B("治理的权力：书圈里圈主由持股（投资份额）最高的人担任，没有竞价拍卖——钱在哪里，治理权就在哪里。"),
    AgrBlock.B("一呼百应的权力：当你持有的书币足够多，整个书圈都不得不认真对待你的声音；你就是那个能定调的人。"),
    AgrBlock.B("影响作者创作的权力：如果你的书币还不够多到一言九鼎，你也可以团结其他人，在社区里一起发帖、提出意见。因为你们手上的书币若集中抛售，会对整个书圈造成毁灭性冲击——这种「用脚投票」的能力，本身就是你和作者谈判的筹码。"),

    AgrBlock.H("2. 对作者：书币让「一言堂」成为过去"),
    AgrBlock.P("正因书币对价了权力，作者不再拥有那种无视任何外部意见的至高话语权。一个作者如果听不进读者建议、一意孤行，圈内的书币价值就会应声暴跌——而书币的价值，直接牵动着作者自己的曝光与收益。换句话说，作者必须和持有权力（书币）的读者们积极沟通、共治共创。"),

    AgrBlock.H("3. 书币 = 广告流量，是买量的核心衡量指标"),
    AgrBlock.P("这里藏着整个生态最关键的一环："),
    AgrBlock.B("书圈的书币价值，本身就是广告流量的加权；"),
    AgrBlock.B("它同时也是广告方愿意投放买流的核心衡量指标。"),
    AgrBlock.B("一个治理井井有条、书币价值极高的书，如果你是广告方，当然更愿意买它的广告位。"),
    AgrBlock.B("而广告位的进入，又会带来利好消息，反过来促进整个书圈生态与书籍内容创作的繁荣发展。"),

    AgrBlock.H("4. 多方共赢：每一方都拿到了自己真正需要的东西"),
    AgrBlock.B("普通读者：一个治理有序、氛围良好的社区；"),
    AgrBlock.B("资深读者：真正影响圈子走向的治理话语权；"),
    AgrBlock.B("作者：流量曝光，以及与读者积极沟通的舞台；"),
    AgrBlock.B("广告商：可用数据判断、稳定可预期的买量收益。"),
    AgrBlock.P("而广告商的加入，又会让书圈生态更加繁荣，相关书币随之迎来重大利好、进一步升值——一个所有人都愿意让它变好的正向循环，就这么转起来了。"),

    AgrBlock.H("四、你能在这里做什么？"),
    AgrBlock.B("领取与持有：符合条件的读者可领取书圈初始书币，成为经济体的早期成员。"),
    AgrBlock.B("交易：书币交易所在书币之间自由兑换，限价订单簿撮合、零费率，B000001 作为基准锚定币。"),
    AgrBlock.B("治理与投资：用持股影响圈子走向；多个董事会可发行「理财产品」——把不同书圈的书币按权重打包成资产包（如「A 书圈 30% + B 书圈 70%」），你用钱包里的书币认购份额，收益每日结算、分成打入你的个人钱包。"),
    AgrBlock.B("稳定币（稀缺的荣耀）：部分资产包在持续正向收益后，有极低概率产出书圈稳定币——全平台恒定只有 1 万枚，由真实的 B000001 以 1:10 背书且绝对守恒、绝不增发。它代表被市场验证逻辑印证过的「权力凝结」，越接近上限越难获得。"),
    AgrBlock.B("退市与清算：长期负收益的资产包会被退市并为所有人结算清仓——经济有涨有跌，风险与权力同在。"),

    AgrBlock.H("五、设计哲学小结"),
    AgrBlock.P("书圈是一堂用你喜欢的 IP 上的「游戏化金融模拟」。它把抽象的经济学，变成你可以参与、可以犯错、可以真正理解的体验。书币是这套体验里「权力」的载体——你拥有的不是数字，是你在一个故事宇宙里说话的分量；而正是这份分量，让作者、读者、广告商第一次坐在了同一张桌子上。"),
)

/**
 * 书圈用户需知：首次进入书圈强制弹窗。
 * 不可点外部/返回取消；必须勾选【我知道了】后才能点「进入书圈」。
 * 勾选确认后由调用方持久化标志（只弹一次）。
 */
@Composable
fun BookCircleAgreementDialog(onAgree: () -> Unit) {
    var checked by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { /* 强制阅读：禁止外部/返回取消 */ }) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "书圈 · 用户需知",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(20.dp),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 440.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    for (block in AGREEMENT_BLOCKS) {
                        when (block) {
                            is AgrBlock.H -> Text(
                                block.text,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            is AgrBlock.P -> Text(
                                block.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            is AgrBlock.B -> Text(
                                "• " + block.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            is AgrBlock.Q -> Text(
                                block.text,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { checked = !checked }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = checked, onCheckedChange = { checked = it })
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "我知道了",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.weight(1f))
                    MetaButton(
                        text = "进入书圈",
                        onClick = onAgree,
                        modifier = Modifier.weight(1f),
                        enabled = checked,
                    )
                }
            }
        }
    }
}
