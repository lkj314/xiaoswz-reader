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
import okhttp3.Protocol
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
 * 自动更新管理器
 * 流程：GET {server}/version.json → 比对 versionCode → 下载 APK → FileProvider 调起安装
 */
class UpdateManager(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 国内网络环境复杂，raw.githubusercontent 与 jsDelivr 都可能被中间设备
     * 对 HTTP/2 连接发送 RST_STREAM。强制 HTTP/1.1 并拉长超时，可显著降低
     * "stream was reset: CANCEL" 概率。
     */
    private val client = OkHttpClient.Builder()
        .protocols(listOf(Protocol.HTTP_1_1))
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        // 更新通道候选（按优先级）：用户手填 > 硬编码 raw > jsDelivr CDN 镜像
        // 任一通道可达即可完成检查+下载，避免 raw 国内偶发超时导致整段更新失败
        private val FALLBACK_SERVERS = listOf(
            BuildConfig.DEFAULT_UPDATE_SERVER,
            "https://cdn.jsdelivr.net/gh/lkj314/xiaoswz-reader@main/lan-update",
        )
    }

    // check 成功时记录实际命中的服务器，downloadApk 优先复用同一通道
    @Volatile
    private var lastCheckServer: String? = null

    private fun fetchInfo(serverUrl: String): UpdateInfo? {
        val url = serverUrl.trim().trimEnd('/') + "/version.json"
        client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body ?: return null
            return json.decodeFromString<UpdateInfo>(body.string())
        }
    }

    /** 检查更新：有更新返回 UpdateInfo，已最新返回 null，失败抛异常（包在 Result 里） */
    suspend fun check(serverUrl: String): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
        runCatching {
            val userUrl = serverUrl.trim().trimEnd('/')
            // 候选顺序：用户手填 -> 内置 fallback 通道（去重）
            val candidates = linkedSetOf(userUrl).apply {
                addAll(FALLBACK_SERVERS)
            }
            val errors = mutableListOf<String>()
            for (url in candidates) {
                // 每个通道快速重试 2 次：raw 国内偶发丢包，重试可显著提高成功率
                repeat(2) { attempt ->
                    try {
                        val info = fetchInfo(url)
                        if (info != null) {
                            lastCheckServer = url
                            return@runCatching if (info.versionCode > BuildConfig.VERSION_CODE) info else null
                        }
                    } catch (e: Exception) {
                        if (attempt == 1) errors.add("$url: ${e.javaClass.simpleName}")
                    }
                }
            }
            error("所有更新通道均不可用（请检查网络或在设置页手动填写更新服务器）。\n详情：${errors.joinToString(", ")}")
        }
    }

    /** 下载 APK 到应用私有目录，onProgress 回调 0-100 */
    suspend fun downloadApk(
        serverUrl: String,
        info: UpdateInfo,
        onProgress: (Int) -> Unit,
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val userUrl = serverUrl.trim().trimEnd('/')
            // 下载通道候选：优先复用 check 命中的通道；否则用户手填 -> fallback（去重）
            val candidates = linkedSetOf<String>().apply {
                add((lastCheckServer ?: userUrl).trim().trimEnd('/'))
                add(userUrl)
                addAll(FALLBACK_SERVERS)
            }

            val dir = File(context.getExternalFilesDir(null), "updates")
            dir.mkdirs()
            val file = File(dir, "surf-reader-${info.versionName}.apk")

            val errors = mutableListOf<String>()
            for (base in candidates) {
                val url = if (info.apkUrl.startsWith("http")) {
                    info.apkUrl
                } else {
                    base + "/" + info.apkUrl.trimStart('/')
                }
                repeat(2) { attempt ->
                    try {
                        downloadFromUrl(url, file, onProgress)
                        return@runCatching file
                    } catch (e: Exception) {
                        val summary = "$base: ${e.javaClass.simpleName}(${e.message?.take(40)})"
                        if (attempt == 1) errors.add(summary)
                    }
                }
            }
            error("APK 下载失败，所有通道均不可用。\n详情：${errors.joinToString(", ")}")
        }
    }

    private fun downloadFromUrl(
        url: String,
        file: File,
        onProgress: (Int) -> Unit,
    ) {
        client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}")
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
