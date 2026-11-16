package com.threadpilot.manager.data

/**
 * 单条线程绑定规则。
 *
 * 对应 applist.conf 中的一行：
 *   pkg{threadName}=cpuCores     -> threadName 非空
 *   pkg=cpuCores                 -> threadName 为空（主进程默认绑定）
 */
data class ThreadRule(
    val threadName: String = "",
    val cpuCores: String = ""
)

/** 一个游戏/应用的完整配置。 */
data class GameConfig(
    val name: String = "",
    val packageName: String = "",
    val rules: List<ThreadRule> = emptyList()
)

/** 导入导出用的数据载体（JSON）。 */
data class ConfigBundle(
    val version: String = "1.0",
    val exportTime: Long = System.currentTimeMillis(),
    val games: List<GameConfig> = emptyList()
)
