package com.threadpilot.manager.util

import com.threadpilot.manager.data.GameConfig
import com.threadpilot.manager.data.ThreadRule
import org.json.JSONArray
import org.json.JSONObject

/** JSON 配置导入导出（使用 Android 内置的 org.json，无需额外依赖）。 */
object ShareCodec {

    fun encode(games: List<GameConfig>): String {
        val root = JSONObject()
        root.put("version", "1.0")
        root.put("exportTime", System.currentTimeMillis())

        val array = JSONArray()
        games.forEach { game ->
            val gameObj = JSONObject()
            gameObj.put("name", game.name)
            gameObj.put("packageName", game.packageName)

            val rules = JSONArray()
            game.rules.forEach { rule ->
                rules.put(
                    JSONObject()
                        .put("threadName", rule.threadName)
                        .put("cpuCores", rule.cpuCores)
                )
            }
            gameObj.put("threadConfigs", rules)
            array.put(gameObj)
        }
        root.put("games", array)
        return root.toString(2)
    }

    fun decode(text: String): List<GameConfig> {
        val root = JSONObject(text)
        val array = root.optJSONArray("games") ?: return emptyList()
        val result = mutableListOf<GameConfig>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val pkg = obj.optString("packageName", "").trim()
            if (pkg.isEmpty()) continue

            val rules = mutableListOf<ThreadRule>()
            val ruleArray = obj.optJSONArray("threadConfigs")
            if (ruleArray != null) {
                for (j in 0 until ruleArray.length()) {
                    val r = ruleArray.optJSONObject(j) ?: continue
                    rules += ThreadRule(
                        threadName = r.optString("threadName", "").trim(),
                        cpuCores = r.optString("cpuCores", "").trim()
                    )
                }
            }
            result += GameConfig(
                name = obj.optString("name", "").ifBlank { pkg },
                packageName = pkg,
                rules = rules
            )
        }
        return result
    }
}
