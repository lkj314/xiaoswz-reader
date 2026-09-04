package com.xiaoswz.reader.ui.booklist
import com.xiaoswz.reader.ui.components.MetaCoverImage

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaoswz.reader.data.BookRepository
import com.xiaoswz.reader.data.model.BookDto
import com.xiaoswz.reader.ui.components.MetaButton
import com.xiaoswz.reader.ui.components.MetaButtonVariant
import com.xiaoswz.reader.ui.theme.GlassTokens
import kotlinx.coroutines.launch

/**
 * 书单内加书组件（0.8.0）：搜索书城目录，点选即加入当前书单。
 * 书目来自 /api/books（已带 uid 书号），可直接把 uid 写进书单项。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookPickerSheet(
    onDismiss: () -> Unit,
    onPicked: (BookDto) -> Unit,
) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val repo = remember { BookRepository() }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<BookDto>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = GlassTokens.GroupedBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxSize()
                .padding(16.dp),
        ) {
            Text(
                "添加书籍到书单",
                style = MaterialTheme.typography.titleLarge,
                color = GlassTokens.Label,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("搜索书名 / 作者") },
                )
                MetaButton(
                    text = "搜索",
                    modifier = Modifier,
                    onClick = {
                        scope.launch {
                            searching = true
                            searched = false
                            repo.getBooks(1, "latest", query)
                                .onSuccess { resp -> results = resp.books ?: emptyList() }
                                .onFailure { results = emptyList() }
                            searching = false
                            searched = true
                        }
                    },
                    enabled = query.trim().isNotEmpty() && !searching,
                )
            }
            Spacer(Modifier.height(12.dp))
            if (searching) {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GlassTokens.SystemBlue)
                }
            } else if (searched && results.isEmpty()) {
                Text("未找到相关书籍", color = GlassTokens.SecondaryLabel, modifier = Modifier.padding(16.dp))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(results, key = { it.slug ?: it.id ?: it.toString() }) { book ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(GlassTokens.RadiusLG))
                                .background(GlassTokens.GlassFillStrong)
                                .clickable { onPicked(book) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(GlassTokens.RadiusMD)).background(GlassTokens.GlassFill),
                                contentAlignment = Alignment.Center,
                            ) {
                                MetaCoverImage(
                                    model = book.coverImage,
                                    title = book.title ?: "",
                                    modifier = Modifier.fillMaxSize(),
                                    shape = RoundedCornerShape(GlassTokens.RadiusMD),
                                    cornerRadius = GlassTokens.RadiusMD,
                                    contentScale = ContentScale.Crop,
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(book.title ?: "未知书目", style = MaterialTheme.typography.bodyLarge, color = GlassTokens.Label, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(book.displayAuthor, style = MaterialTheme.typography.labelSmall, color = GlassTokens.SecondaryLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    book.uid?.let {
                                        Spacer(Modifier.width(8.dp))
                                        Text("书号 $it", style = MaterialTheme.typography.labelSmall, color = GlassTokens.SystemBlue)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
