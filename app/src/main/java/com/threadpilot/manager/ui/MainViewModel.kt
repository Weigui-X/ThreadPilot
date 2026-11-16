package com.threadpilot.manager.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.threadpilot.manager.data.ConfigParser
import com.threadpilot.manager.data.ConfigRepository
import com.threadpilot.manager.data.GameConfig
import com.threadpilot.manager.data.SettingsStore
import com.threadpilot.manager.root.RootManager
import com.threadpilot.manager.root.RootState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MainUiState(
    val games: List<GameConfig> = emptyList(),
    val query: String = "",
    val rootState: RootState = RootState.NotFound,
    val moduleInstalled: Boolean = false,
    val moduleProps: Map<String, String> = emptyMap(),
    val configPath: String = "",
    val loading: Boolean = false,
    val saving: Boolean = false,
    val dirty: Boolean = false,
    val message: String? = null,
    val selectedPackages: Set<String> = emptySet(),
    val selectionMode: Boolean = false
)

class MainViewModel(
    private val repository: ConfigRepository,
    private val settings: SettingsStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    /** 重新检测 Root / 模块状态并从磁盘加载配置。 */
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, configPath = repository.currentPath()) }
            val root = withContext(Dispatchers.IO) { RootManager.checkRoot() }
            if (root is RootState.Available) {
                val result = withContext(Dispatchers.IO) { repository.load() }
                result
                    .onSuccess { games ->
                        _uiState.update { it.copy(games = games, dirty = false) }
                    }
                    .onFailure { error ->
                        postMessage("读取配置失败：${error.message ?: "未知错误"}")
                    }
                val installed = withContext(Dispatchers.IO) { repository.moduleInstalled() }
                val props = withContext(Dispatchers.IO) { repository.moduleProperties() }
                _uiState.update { it.copy(moduleInstalled = installed, moduleProps = props) }
            }
            _uiState.update { it.copy(rootState = root, loading = false) }
        }
    }

    fun save() {
        val snapshot = _uiState.value.games
        viewModelScope.launch {
            _uiState.update { it.copy(saving = true) }
            val result = withContext(Dispatchers.IO) { repository.save(snapshot) }
            result
                .onSuccess {
                    // 保存期间若用户又做了编辑，保留脏标记，避免误认为已同步
                    if (snapshot == _uiState.value.games) {
                        _uiState.update { it.copy(dirty = false) }
                    }
                }
                .onFailure { postMessage("保存失败：${it.message ?: "未知错误"}") }
            if (result.isSuccess) postMessage("已保存到 ${repository.currentPath()}")
            _uiState.update { it.copy(saving = false) }
        }
    }

    /** originalPackage 为空表示新增；否则按原包名查找并替换。 */
    fun addOrUpdateGame(originalPackage: String?, game: GameConfig) {
        if (!ConfigParser.isValidPackageName(game.packageName)) {
            postMessage("包名格式不正确")
            return
        }
        if (game.rules.isEmpty()) {
            postMessage("至少添加一条线程规则")
            return
        }
        if (game.rules.any { !ConfigParser.isValidCpuCores(it.cpuCores) }) {
            postMessage("CPU 核心格式不正确，示例：7 / 2-4 / 0,2,4")
            return
        }

        val duplicate = _uiState.value.games.any {
            it.packageName == game.packageName && it.packageName != originalPackage
        }
        if (duplicate) {
            postMessage("该包名已存在，请改名或直接在原条目上编辑")
            return
        }

        _uiState.update { state ->
            val updated = state.games.toMutableList()
            val index = if (originalPackage == null) -1 else
                updated.indexOfFirst { it.packageName == originalPackage }
            if (index >= 0) updated[index] = game else updated += game
            state.copy(games = updated, dirty = true)
        }
    }

    fun deletePackages(packages: Set<String>) {
        if (packages.isEmpty()) return
        _uiState.update { state ->
            state.copy(
                games = state.games.filterNot { it.packageName in packages },
                selectedPackages = state.selectedPackages - packages,
                dirty = true
            )
        }
        postMessage("已移除 ${packages.size} 项，记得点击保存")
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun setConfigPath(path: String) {
        if (path.isBlank()) {
            postMessage("路径不能为空")
            return
        }
        settings.configPath = path
        postMessage("配置路径已更新，正在重新加载")
        refresh()
    }

    fun importGames(incoming: List<GameConfig>, overwrite: Boolean) {
        if (incoming.isEmpty()) {
            postMessage("文件中没有可导入的配置")
            return
        }
        _uiState.update { state ->
            val result = state.games.toMutableList()
            incoming.forEach { game ->
                val index = result.indexOfFirst { it.packageName == game.packageName }
                when {
                    index >= 0 && overwrite -> result[index] = game
                    index >= 0 -> { /* 保留原配置 */ }
                    else -> result += game
                }
            }
            state.copy(games = result, dirty = true)
        }
        postMessage("已导入 ${incoming.size} 项配置（记得保存）")
    }

    fun setSelectionMode(enabled: Boolean) {
        _uiState.update {
            it.copy(
                selectionMode = enabled,
                selectedPackages = if (enabled) it.selectedPackages else emptySet()
            )
        }
    }

    fun toggleSelected(packageName: String) {
        _uiState.update { state ->
            val next = if (packageName in state.selectedPackages)
                state.selectedPackages - packageName
            else state.selectedPackages + packageName
            state.copy(selectedPackages = next)
        }
    }

    fun selectAllVisible(visiblePackages: List<String>) {
        _uiState.update { state ->
            val allSelected = visiblePackages.all { it in state.selectedPackages }
            state.copy(
                selectedPackages = if (allSelected) emptySet() else visiblePackages.toSet()
            )
        }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun postMessage(message: String) {
        _uiState.update { it.copy(message = message) }
    }
}
