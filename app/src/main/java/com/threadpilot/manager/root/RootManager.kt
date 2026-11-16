package com.threadpilot.manager.root

import android.util.Base64
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.FutureTask

/** Root 权限状态。 */
sealed interface RootState {
    data object Available : RootState
    data class Denied(val detail: String) : RootState
    data object NotFound : RootState
}

class ShellException(message: String) : Exception(message)

private data class ShellOutput(val stdout: String, val stderr: String, val code: Int)

/**
 * 通过 su 以 root 身份读写系统保护目录。
 * 写入时使用 base64 中转，避免配置内容里的引号/换行被 shell 解析破坏。
 */
object RootManager {

    fun checkRoot(): RootState {
        return runProcess(arrayOf("su", "-c", "id")).fold(
            onSuccess = { shell ->
                if (shell.stdout.contains("uid=0")) RootState.Available
                else RootState.Denied(
                    shell.stderr.ifBlank { shell.stdout.ifBlank { "su 未返回 root 身份" } }
                )
            },
            onFailure = { error ->
                if (error is IOException) RootState.NotFound
                else RootState.Denied(error.message ?: "Root 检测失败")
            }
        )
    }

    fun fileExists(path: String): Boolean =
        exec("test -f ${quote(path)} && echo TP_YES")
            .getOrNull()?.stdout?.contains("TP_YES") == true

    fun readFile(path: String): Result<String> =
        exec("cat ${quote(path)}").map { it.stdout }

    fun writeFile(path: String, content: String): Result<Unit> {
        val encoded = Base64.encodeToString(content.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        val dir = path.substringBeforeLast('/', "")
        val mkdir = if (dir.isNotBlank() && dir != path) "mkdir -p ${quote(dir)} && " else ""
        return exec("${mkdir}printf '%s' ${quote(encoded)} | base64 -d > ${quote(path)}").map { }
    }

    fun copyFile(from: String, to: String): Result<Unit> =
        exec("cp -f ${quote(from)} ${quote(to)}").map { }

    /** 读取模块目录下的 module.prop，用于展示模块名称与版本。 */
    fun readModuleProp(configPath: String): Map<String, String> {
        val dir = configPath.substringBeforeLast('/', "")
        if (dir.isBlank() || dir == configPath) return emptyMap()
        val text = readFile("$dir/module.prop").getOrNull() ?: return emptyMap()
        return text.lineSequence().mapNotNull { line ->
            if (line.trimStart().startsWith("#")) return@mapNotNull null
            val i = line.indexOf('=')
            if (i <= 0) null else line.substring(0, i).trim() to line.substring(i + 1).trim()
        }.toMap()
    }

    private fun exec(command: String): Result<ShellOutput> =
        runProcess(arrayOf("su", "-c", command))

    /** 并发消费 stdout / stderr，避免某一路缓冲区写满导致进程阻塞。 */
    private fun runProcess(argv: Array<String>): Result<ShellOutput> = try {
        val process = Runtime.getRuntime().exec(argv)
        val stdoutTask = FutureTask<String> { readAll(process.inputStream) }
        val stderrTask = FutureTask<String> { readAll(process.errorStream) }
        Thread(stdoutTask).start()
        Thread(stderrTask).start()

        val code = process.waitFor()
        val out = stdoutTask.get()
        val err = stderrTask.get()
        if (code == 0) {
            Result.success(ShellOutput(out, err, code))
        } else {
            Result.failure(ShellException(err.ifBlank { out.ifBlank { "命令执行失败（退出码 $code）" } }))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun readAll(stream: InputStream): String =
        stream.bufferedReader(Charsets.UTF_8).use { it.readText() }

    /** POSIX 单引号转义：把 ' 替换为 '\''。 */
    private fun quote(value: String): String = "'" + value.replace("'", "'\\''") + "'"
}
