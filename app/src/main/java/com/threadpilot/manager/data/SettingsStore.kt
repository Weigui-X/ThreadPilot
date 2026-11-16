package com.threadpilot.manager.data

import android.content.Context

/** 应用偏好设置（轻量，使用 SharedPreferences）。 */
class SettingsStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("threadpilot_settings", Context.MODE_PRIVATE)

    var configPath: String
        get() = prefs.getString(KEY_PATH, DEFAULT_PATH) ?: DEFAULT_PATH
        set(value) = prefs.edit().putString(KEY_PATH, value.trim()).apply()

    var backupBeforeSave: Boolean
        get() = prefs.getBoolean(KEY_BACKUP, true)
        set(value) = prefs.edit().putBoolean(KEY_BACKUP, value).apply()

    var highlightSearch: Boolean
        get() = prefs.getBoolean(KEY_HIGHLIGHT, true)
        set(value) = prefs.edit().putBoolean(KEY_HIGHLIGHT, value).apply()

    companion object {
        const val DEFAULT_PATH = "/data/adb/modules/AppOpt/applist.conf"
        private const val KEY_PATH = "config_path"
        private const val KEY_BACKUP = "backup_before_save"
        private const val KEY_HIGHLIGHT = "highlight_search"
    }
}
