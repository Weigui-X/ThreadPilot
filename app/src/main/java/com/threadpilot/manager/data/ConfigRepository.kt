package com.threadpilot.manager.data

import com.threadpilot.manager.root.RootManager

/** 配置仓库：把文件读写与业务逻辑隔离开。 */
class ConfigRepository(private val settings: SettingsStore) {

    fun currentPath(): String = settings.configPath

    fun load(): Result<List<GameConfig>> =
        RootManager.readFile(settings.configPath).map { ConfigParser.parse(it) }

    fun save(games: List<GameConfig>): Result<Unit> {
        val path = settings.configPath
        // 保存前先备份原文件，失败不阻断保存
        if (settings.backupBeforeSave && RootManager.fileExists(path)) {
            RootManager.copyFile(path, "$path.tp-bak")
        }
        return RootManager.writeFile(path, ConfigParser.serialize(games))
    }

    fun moduleInstalled(): Boolean = RootManager.fileExists(settings.configPath)

    fun moduleProperties(): Map<String, String> = RootManager.readModuleProp(settings.configPath)
}
