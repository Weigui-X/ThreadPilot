package com.threadpilot.manager.data

/**
 * applist.conf 解析器。
 *
 * 支持的行格式：
 *   #游戏名称              -> 注释行开启一个新游戏分段
 *   com.xxx{ThreadName}=7   -> 指定线程绑定到 CPU 核心 7
 *   com.xxx{ThreadName}=2-4 -> 绑定到核心 2 至 4
 *   com.xxx=0-7             -> 无线程名，代表主进程/全局规则
 */
object ConfigParser {

    private val PACKAGE_REGEX = Regex("^[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+$")

    fun parse(text: String): List<GameConfig> {
        val result = mutableListOf<GameConfig>()
        var currentName = ""
        // 使用 LinkedHashMap 保证同一个游戏内的行顺序不被打乱
        val currentRules = LinkedHashMap<String, MutableList<ThreadRule>>()

        fun flush() {
            for ((pkg, rules) in currentRules) {
                if (pkg.isNotBlank()) {
                    result += GameConfig(
                        name = currentName.ifBlank { pkg },
                        packageName = pkg,
                        rules = rules.toList()
                    )
                }
            }
            currentRules.clear()
            currentName = ""
        }

        text.lineSequence().forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty()) return@forEach

            when {
                line.startsWith("#") -> {
                    flush()
                    currentName = line.removePrefix("#").trim()
                }

                else -> {
                    val eq = line.indexOf('=')
                    if (eq <= 0) return@forEach

                    val left = line.substring(0, eq).trim()
                    val cores = line.substring(eq + 1).trim()
                    if (cores.isEmpty()) return@forEach

                    val pkg: String
                    val threadName: String
                    if (left.contains('{') && left.endsWith("}")) {
                        pkg = left.substringBefore('{').trim()
                        threadName = left.substringAfter('{').substringBeforeLast('}').trim()
                    } else {
                        pkg = left
                        threadName = ""
                    }
                    if (pkg.isEmpty()) return@forEach

                    currentRules.getOrPut(pkg) { mutableListOf() } += ThreadRule(threadName, cores)
                }
            }
        }
        flush()
        return result
    }

    fun serialize(games: List<GameConfig>): String = buildString {
        games.forEach { game ->
            appendLine("#${game.name.ifBlank { game.packageName }}")
            game.rules.forEach { rule ->
                if (rule.threadName.isBlank()) {
                    appendLine("${game.packageName}=${rule.cpuCores}")
                } else {
                    appendLine("${game.packageName}{${rule.threadName}}=${rule.cpuCores}")
                }
            }
            appendLine()
        }
    }

    fun isValidPackageName(value: String): Boolean = PACKAGE_REGEX.matches(value.trim())

    /** 形如 7 / 0-3 / 0,2,4 或 0-3,7；不做严格语义校验，仅拦截明显非法字符。 */
    fun isValidCpuCores(value: String): Boolean =
        value.isNotBlank() && value.all { it.isDigit() || it == '-' || it == ',' || it == ' ' }
}
