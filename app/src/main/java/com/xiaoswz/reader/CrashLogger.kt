package com.xiaoswz.reader

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 全局崩溃采集器（调试用）
 * 把未捕获异常的完整堆栈写入文件，方便无设备环境下定位崩溃。
 * 路径：
 *   - 内部：context.filesDir/crash.log
 *   - 外部（易取）：Android/data/com.xiaoswz.reader/files/crash.log
 */
object CrashLogger {

    fun install(context: Context) {
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { write(context, throwable) }
            // 仍交给系统默认处理器，保持原有"闪退"表现，便于用户复现
            prev?.uncaughtException(thread, throwable)
        }
    }

    /** 供业务层主动上报已捕获的异常（如协程内异常），写入崩溃日志但不终止进程 */
    fun report(context: Context, t: Throwable) {
        runCatching { write(context, t) }
    }

    /** 读取最近一次崩溃日志（外部存储优先，回退内部存储）。无日志返回 null */
    fun getLog(context: Context): String? {
        val external = runCatching {
            context.getExternalFilesDir(null)?.let { File(it, "crash.log") }
                ?.takeIf { it.exists() }?.readText()
        }.getOrNull()
        if (!external.isNullOrBlank()) return external

        return runCatching {
            File(context.filesDir, "crash.log").takeIf { it.exists() }?.readText()
        }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    /** 是否存在崩溃日志 */
    fun hasLog(context: Context): Boolean = getLog(context) != null

    private fun write(context: Context, t: Throwable) {
        val sw = StringWriter()
        sw.append("==== 崩溃时间: ")
            .append(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))
            .append(" ====\n")
        sw.append("设备: ${Build.MANUFACTURER} ${Build.MODEL} / Android ${Build.VERSION.SDK_INT}\n")
        sw.append("线程: ${Thread.currentThread().name}\n")
        sw.append("异常: ${t.javaClass.name}: ${t.message}\n\n")
        t.printStackTrace(PrintWriter(sw))

        var cause = t.cause
        var depth = 0
        while (cause != null && depth < 5) {
            sw.append("\n---- Caused by ----\n")
            cause.printStackTrace(PrintWriter(sw))
            cause = cause.cause
            depth++
        }
        val text = sw.toString()

        runCatching { File(context.filesDir, "crash.log").writeText(text) }
        runCatching {
            context.getExternalFilesDir(null)?.let {
                File(it, "crash.log").writeText(text)
            }
        }
    }
}
