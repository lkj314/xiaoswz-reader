package com.xiaoswz.reader.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import com.xiaoswz.reader.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/** 服务器上 version.json 的结构 */
@Serializable
data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val notes: String? = null,
)

/**
 * 局域网自动更新
 * 流程：GET {server}/version.json → 比对 versionCode → 下载 APK → FileProvider 调起安装
 */
class UpdateManager(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    /** 检查更新：有更新返回 UpdateInfo，已最新返回 null，失败抛异常（包在 Result 里） */
    suspend fun check(serverUrl: String): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
        runCatching {
            val url = serverUrl.trim().trimEnd('/') + "/version.json"
            client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body ?: return@use null
                val info = json.decodeFromString<UpdateInfo>(body.string())
                if (info.versionCode > BuildConfig.VERSION_CODE) info else null
            }
        }
    }

    /** 下载 APK 到应用私有目录，onProgress 回调 0-100 */
    suspend fun downloadApk(
        serverUrl: String,
        info: UpdateInfo,
        onProgress: (Int) -> Unit,
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val url = if (info.apkUrl.startsWith("http")) {
                info.apkUrl
            } else {
                serverUrl.trim().trimEnd('/') + "/" + info.apkUrl.trimStart('/')
            }
            val dir = File(context.getExternalFilesDir(null), "updates")
            dir.mkdirs()
            val file = File(dir, "surf-reader-${info.versionName}.apk")

            client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                if (!response.isSuccessful) error("下载失败：HTTP ${response.code}")
                val body = response.body ?: error("空响应体")
                val total = body.contentLength()
                body.byteStream().use { input ->
                    FileOutputStream(file).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var downloaded = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            if (total > 0) {
                                onProgress((downloaded * 100 / total).toInt().coerceIn(0, 100))
                            }
                        }
                        output.flush()
                    }
                }
            }
            onProgress(100)
            file
        }
    }

    /**
     * 调起系统安装器
     * @return true=已调起安装；false=未授权"安装未知应用"，已跳转设置页（授权后需再次点击安装）
     */
    fun installApk(file: File): Boolean {
        if (!context.packageManager.canRequestPackageInstalls()) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return false
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${BuildConfig.APPLICATION_ID}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return true
    }
}
