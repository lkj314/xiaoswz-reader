package com.xiaoswz.reader.ui.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.xiaoswz.reader.BuildConfig
import com.xiaoswz.reader.data.update.UpdateInfo
import com.xiaoswz.reader.data.update.UpdateManager
import kotlinx.coroutines.launch
import java.io.File

private sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data object UpToDate : UpdateUiState
    data class Available(val info: UpdateInfo) : UpdateUiState
    data class Downloading(val progress: Int) : UpdateUiState
    data class Ready(val file: File, val versionName: String) : UpdateUiState
    data class Failure(val message: String) : UpdateUiState
}

/**
 * 局域网更新对话框
 * @param serverUrl 当前保存的更新服务器地址
 * @param autoCheck 打开时是否立即自动检查
 */
@Composable
fun UpdateDialog(
    serverUrl: String,
    autoCheck: Boolean,
    onServerUrlChange: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val updateManager = remember { UpdateManager(context.applicationContext) }

    var uiState by remember { mutableStateOf<UpdateUiState>(UpdateUiState.Idle) }
    var urlInput by remember { mutableStateOf(serverUrl) }

    fun doCheck() {
        scope.launch {
            uiState = UpdateUiState.Checking
            onServerUrlChange(urlInput)
            updateManager.check(urlInput)
                .onSuccess { info ->
                    uiState = if (info != null) {
                        UpdateUiState.Available(info)
                    } else {
                        UpdateUiState.UpToDate
                    }
                }
                .onFailure { e ->
                    uiState = UpdateUiState.Failure(e.message ?: "无法连接更新服务器")
                }
        }
    }

    fun doDownload(info: UpdateInfo) {
        scope.launch {
            uiState = UpdateUiState.Downloading(0)
            updateManager.downloadApk(urlInput, info) { progress ->
                uiState = UpdateUiState.Downloading(progress)
            }
                .onSuccess { file ->
                    uiState = UpdateUiState.Ready(file, info.versionName)
                }
                .onFailure { e ->
                    uiState = UpdateUiState.Failure(e.message ?: "下载失败")
                }
        }
    }

    // 打开即自动检查一次
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (autoCheck) doCheck()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("应用更新") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "当前版本：v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("更新服务器") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(14.dp))

                when (val s = uiState) {
                    UpdateUiState.Idle -> Text(
                        "点击「检查」获取最新版本",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    UpdateUiState.Checking -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("正在检查更新…", style = MaterialTheme.typography.bodyMedium)
                    }

                    UpdateUiState.UpToDate -> Text(
                        "已是最新版本 ✅",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    is UpdateUiState.Available -> Column {
                        Text(
                            "发现新版本：v${s.info.versionName}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        if (!s.info.notes.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                s.info.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    is UpdateUiState.Downloading -> Column {
                        LinearProgressIndicator(
                            progress = { s.progress / 100f },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "下载中 ${s.progress}%",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }

                    is UpdateUiState.Ready -> Text(
                        "v${s.versionName} 下载完成，点击「安装」开始更新",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    is UpdateUiState.Failure -> Text(
                        s.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            when (val s = uiState) {
                is UpdateUiState.Available -> TextButton(onClick = { doDownload(s.info) }) {
                    Text("下载")
                }

                is UpdateUiState.Ready -> TextButton(onClick = {
                    updateManager.installApk(s.file)
                }) {
                    Text("安装")
                }

                is UpdateUiState.Downloading, UpdateUiState.Checking -> Unit

                else -> TextButton(onClick = { doCheck() }) {
                    Text("检查")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        },
    )
}
