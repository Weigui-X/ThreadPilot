package com.threadpilot.manager.util

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 本地错误日志 + 全局异常兜底，方便在无调试器环境下排查问题。 */
class ErrorLogger(context: Context) : Thread.UncaughtExceptionHandler {

    private val logFile = File(context.applicationContext.filesDir, "logs/errors.log")
    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    private val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    init {
        logFile.parentFile?.mkdirs()
    }

    fun install() {
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    fun log(tag: String, message: String) {
        runCatching {
            logFile.appendText("[${formatter.format(Date())}] $tag: $message\n")
            if (logFile.length() > MAX_BYTES) {
                val lines = logFile.readLines().takeLast(MAX_LINES)
                logFile.writeText(lines.joinToString("\n") + "\n")
            }
        }
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        log("FATAL", "${thread.name}: ${throwable.stackTraceToString()}")
        defaultHandler?.uncaughtException(thread, throwable)
    }

    fun readLog(): String = runCatching { logFile.readText() }.getOrDefault("（暂无日志）")

    private companion object {
        const val MAX_BYTES = 512L * 1024L
        const val MAX_LINES = 500
    }
}
