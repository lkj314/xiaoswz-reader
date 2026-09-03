package com.xiaoswz.reader.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.xiaoswz.reader.ui.theme.GlassTokens
import com.xiaoswz.reader.ui.theme.MetaIcons

/**
 * 内容举报底部弹层（0.7.8）：通用举报帖子 / 书单。reason 可为 null（后端允许匿名空理由）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportSheet(
    title: String,
    onDismiss: () -> Unit,
    onSubmit: (String?) -> Unit,
) {
    var reason by remember { mutableStateOf("") }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // 修复：举报理由输入框在键盘弹起时被遮挡 → 补 imePadding 顶起内容
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .padding(bottom = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = GlassTokens.Label)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(MetaIcons.Close, contentDescription = "关闭", tint = GlassTokens.Label)
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = reason,
                onValueChange = { if (it.length <= 200) reason = it },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                label = { Text("举报理由（可选）") },
                placeholder = { Text("如：垃圾广告 / 违规内容", color = GlassTokens.SecondaryLabel) },
                singleLine = false,
                shape = RoundedCornerShape(GlassTokens.RadiusMD),
                colors = androidx.compose.material3.TextFieldDefaults.colors(
                    focusedContainerColor = GlassTokens.GlassFill,
                    unfocusedContainerColor = GlassTokens.GlassFill,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = GlassTokens.SystemBlue,
                    focusedTextColor = GlassTokens.Label,
                    unfocusedTextColor = GlassTokens.Label,
                ),
            )
            Spacer(Modifier.height(14.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(GlassTokens.RadiusPill))
                    .background(GlassTokens.GradientButton)
                    .clickable { onSubmit(reason.trim().ifBlank { null }) }
                    .padding(vertical = 13.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("提交举报", color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
