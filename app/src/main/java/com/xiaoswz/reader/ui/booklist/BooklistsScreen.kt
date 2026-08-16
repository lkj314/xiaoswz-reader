package com.xiaoswz.reader.ui.booklist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.xiaoswz.reader.data.api.BooklistSummary
import com.xiaoswz.reader.ui.components.AppTopBar
import com.xiaoswz.reader.ui.theme.GlassTokens
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BooklistsScreen(
    onBack: () -> Unit,
    onBooklistClick: (String) -> Unit,
    onAccountClick: () -> Unit,
    viewModel: BooklistsViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var showCreate by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.load(refresh = true) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "书单",
                onBack = onBack,
                showLogo = true,
                actions = {
                    IconButton(onClick = { showCreate = true }) {
                        Icon(Icons.Default.Add, contentDescription = "新建书单", tint = GlassTokens.Label)
                    }
                },
            )
        },
        containerColor = Color.Transparent,
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.scope == "all",
                    onClick = { viewModel.switchScope("all") },
                    label = { Text("全部") },
                )
                FilterChip(
                    selected = state.scope == "official",
                    onClick = { viewModel.switchScope("official") },
                    label = { Text("官方") },
                )
                FilterChip(
                    selected = state.scope == "mine",
                    onClick = { viewModel.switchScope("mine") },
                    label = { Text("我的") },
                )
            }

            if (state.isLoading && state.lists.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GlassTokens.SystemBlue)
                }
            } else if (state.lists.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (state.scope == "mine") "登录后可查看你创建的书单" else "还没有书单，点右上角新建一个吧",
                        color = GlassTokens.SecondaryLabel,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.lists, key = { it.id }) { bl ->
                        BooklistRow(bl = bl, onClick = { onBooklistClick(bl.id) })
                    }
                    if (state.isLoadingMore) {
                        item { Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = GlassTokens.SystemBlue)
                        } }
                    }
                }
            }
        }
    }

    if (showCreate) {
        CreateBooklistSheet(
            onDismiss = { showCreate = false },
            onCreate = { title, desc, cover ->
                viewModel.create(title, desc, cover) { result ->
                    result.onSuccess { showCreate = false }
                        .onFailure { /* 错误已在 VM error；此处可 toast */ }
                }
            },
        )
    }
}

@Composable
private fun BooklistRow(bl: BooklistSummary, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GlassTokens.RadiusLG))
            .background(GlassTokens.GlassFillStrong)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(72.dp).clip(RoundedCornerShape(GlassTokens.RadiusMD)).background(GlassTokens.GlassFill),
            contentAlignment = Alignment.Center,
        ) {
            if (!bl.coverUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = bl.coverUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(GlassTokens.RadiusMD)),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(Icons.Default.MenuBook, contentDescription = null, tint = GlassTokens.TertiaryLabel, modifier = Modifier.size(32.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                bl.title,
                style = MaterialTheme.typography.titleMedium,
                color = GlassTokens.Label,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            bl.description?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = GlassTokens.SecondaryLabel,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    bl.owner.displayName ?: "读者",
                    style = MaterialTheme.typography.labelSmall,
                    color = GlassTokens.TertiaryLabel,
                )
                Spacer(Modifier.width(12.dp))
                Icon(Icons.Default.Star, contentDescription = null, tint = GlassTokens.SystemBlue, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(2.dp))
                Text("${bl.collectCount}", style = MaterialTheme.typography.labelSmall, color = GlassTokens.TertiaryLabel)
                Spacer(Modifier.width(12.dp))
                Text("${bl.itemCount} 本", style = MaterialTheme.typography.labelSmall, color = GlassTokens.TertiaryLabel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateBooklistSheet(
    onDismiss: () -> Unit,
    onCreate: (String, String?, String?) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var cover by remember { mutableStateOf("") }
    val enabled = title.trim().length in 1..60

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text("新建书单", style = MaterialTheme.typography.titleLarge, color = GlassTokens.Label, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("标题（必填）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = desc,
                onValueChange = { if (it.length <= 500) desc = it },
                label = { Text("简介（可选）") },
                modifier = Modifier.fillMaxWidth().height(96.dp),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = cover,
                onValueChange = { cover = it },
                label = { Text("封面图链接（可选，http(s)）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(18.dp))
            androidx.compose.material3.Button(
                onClick = { onCreate(title.trim(), desc.trim().ifBlank { null }, cover.trim().ifBlank { null }) },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(GlassTokens.RadiusMD),
            ) {
                Text("创建")
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
