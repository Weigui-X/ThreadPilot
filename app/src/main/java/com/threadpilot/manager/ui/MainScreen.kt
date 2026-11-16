package com.threadpilot.manager.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.threadpilot.manager.data.GameConfig
import com.threadpilot.manager.data.SettingsStore
import com.threadpilot.manager.root.RootState
import com.threadpilot.manager.ui.components.AboutDialog
import com.threadpilot.manager.ui.components.ConfirmDialog
import com.threadpilot.manager.ui.components.ExportDialog
import com.threadpilot.manager.ui.components.GameCard
import com.threadpilot.manager.ui.components.GameEditDialog
import com.threadpilot.manager.ui.components.ImportDialog
import com.threadpilot.manager.ui.components.SettingsDialog
import com.threadpilot.manager.util.ShareCodec
import top.yukonga.miuix.kmp.basic.Badge
import top.yukonga.miuix.kmp.basic.BadgedBox
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.overlay.OverlayListPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme

private data class EditRequest(val originalPackage: String?, val game: GameConfig)

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    settings: SettingsStore,
    onShareJson: (String, String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showSearch by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var editRequest by remember { mutableStateOf<EditRequest?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var showImport by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var showExport by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Set<String>?>(null) }
    var expandedPackages by remember { mutableStateOf<Set<String>>(emptySet()) }
    var backupEnabled by remember { mutableStateOf(settings.backupBeforeSave) }
    var highlightEnabled by remember { mutableStateOf(settings.highlightSearch) }
    var pendingDiscardRefresh by remember { mutableStateOf(false) }
    var pendingPath by remember { mutableStateOf<String?>(null) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            }.onSuccess { text ->
                if (text.isNullOrBlank()) {
                    toastMessage = "文件内容为空"
                } else {
                    importText = text
                    showImport = true
                }
            }.onFailure {
                toastMessage = "读取文件失败：${it.message}"
            }
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            toastMessage = it
            viewModel.consumeMessage()
        }
    }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            toastMessage = null
        }
    }

    val filtered = remember(state.games, state.query) {
        val keyword = state.query.trim()
        if (keyword.isBlank()) state.games
        else state.games.filter { game ->
            game.name.contains(keyword, ignoreCase = true) ||
                game.packageName.contains(keyword, ignoreCase = true) ||
                game.rules.any { it.threadName.contains(keyword, ignoreCase = true) }
        }
    }

    Scaffold(
        containerColor = MiuixTheme.colorScheme.background,
        topBar = {
            SmallTopAppBar(
                title = "线程管理",
                navigationIcon = {
                    if (showSearch) {
                        IconButton(onClick = {
                            showSearch = false
                            viewModel.onQueryChange("")
                        }) {
                            Icon(Icons.Filled.Close, contentDescription = "关闭搜索")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showSearch = !showSearch }) {
                        Icon(Icons.Filled.Search, contentDescription = "搜索")
                    }
                    IconButton(onClick = {
                        if (state.dirty) pendingDiscardRefresh = true
                        else viewModel.refresh()
                    }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "刷新")
                    }
                    IconButton(onClick = { viewModel.save() }) {
                        BadgedBox(badge = { if (state.dirty) Badge() }) {
                            Icon(
                                Icons.Filled.Save,
                                contentDescription = "保存到系统",
                                tint = if (state.dirty) MiuixTheme.colorScheme.primary
                                else MiuixTheme.colorScheme.onSurfaceVariantSummary
                            )
                        }
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "更多")
                    }
                }
            )
        },
        floatingActionButton = {
            if (!state.selectionMode) {
                FloatingActionButton(onClick = {
                    editRequest = EditRequest(null, GameConfig())
                }) {
                    Icon(Icons.Filled.Add, contentDescription = "新增游戏")
                }
            }
        },
        bottomBar = {
            if (state.selectionMode) {
                Surface(color = MiuixTheme.colorScheme.surfaceContainer) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("已选 ${state.selectedPackages.size} 项", style = MiuixTheme.textStyles.footnote1)
                        Spacer(Modifier.weight(1f))
                        TextButton(
                            text = "全选",
                            onClick = { viewModel.selectAllVisible(filtered.map { it.packageName }) },
                            colors = ButtonDefaults.textButtonColorsPrimary()
                        )
                        TextButton(text = "删除", onClick = { deleteTarget = state.selectedPackages })
                        TextButton(text = "取消", onClick = { viewModel.setSelectionMode(false) })
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            StatusBanner(state) { showSettings = true }

            if (showSearch) {
                TextField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    label = "搜索名称 / 包名 / 线程名",
                    useLabelAsPlaceholder = true,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            if (state.loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (filtered.isEmpty() && !state.loading) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (state.query.isBlank()) "还没有配置，点击右下角 + 新增" else "没有匹配的结果",
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                }
            } else {
                LazyColumn {
                    items(filtered, key = { it.packageName }) { game ->
                        val pkg = game.packageName
                        GameCard(
                            game = game,
                            query = state.query,
                            highlightSearch = highlightEnabled,
                            expanded = pkg in expandedPackages,
                            selectionMode = state.selectionMode,
                            selected = pkg in state.selectedPackages,
                            onToggleExpand = {
                                expandedPackages = if (pkg in expandedPackages) expandedPackages - pkg
                                else expandedPackages + pkg
                            },
                            onToggleSelected = { viewModel.toggleSelected(pkg) },
                            onEdit = { editRequest = EditRequest(pkg, game) },
                            onDelete = { deleteTarget = setOf(pkg) },
                            onShare = {
                                onShareJson(
                                    "threadpilot-${pkg}.json",
                                    ShareCodec.encode(listOf(game))
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    OverlayListPopup(
        show = showMenu,
        onDismissRequest = { showMenu = false },
        content = {
            ListPopupColumn {
                listOf(
                    DropdownItem(text = "导入配置", onClick = {
                        importText = ""
                        showImport = true
                    }),
                    DropdownItem(text = "分享配置", onClick = { showExport = true }),
                    DropdownItem(text = "批量删除", onClick = { viewModel.setSelectionMode(true) }),
                    DropdownItem(text = "设置", onClick = { showSettings = true }),
                    DropdownItem(text = "关于", onClick = { showAbout = true })
                ).forEachIndexed { index, item ->
                    top.yukonga.miuix.kmp.basic.DropdownImpl(
                        text = item.text,
                        optionSize = 5,
                        isSelected = false,
                        index = index,
                        onSelectedIndexChange = { item.onClick?.invoke() }
                    )
                }
            }
        }
    )

    editRequest?.let { request ->
        GameEditDialog(
            initial = request.game,
            isNew = request.originalPackage == null,
            onDismiss = { editRequest = null },
            onSave = { game ->
                viewModel.addOrUpdateGame(request.originalPackage, game)
                editRequest = null
            }
        )
    }

    if (showSettings) {
        SettingsDialog(
            currentPath = state.configPath,
            backupEnabled = backupEnabled,
            highlight = highlightEnabled,
            rootState = state.rootState,
            moduleProps = state.moduleProps,
            onDismiss = { showSettings = false },
            onSave = { path ->
                if (state.dirty) pendingPath = path else viewModel.setConfigPath(path)
                showSettings = false
            },
            onBackupChange = { value ->
                backupEnabled = value
                settings.backupBeforeSave = value
            },
            onHighlightChange = { value ->
                highlightEnabled = value
                settings.highlightSearch = value
            }
        )
    }

    if (showImport) {
        ImportDialog(
            initialText = importText,
            onDismiss = { showImport = false },
            onPickFile = { filePicker.launch("application/json") },
            onConfirm = { games, overwrite ->
                viewModel.importGames(games, overwrite)
                showImport = false
            }
        )
    }

    if (showExport) {
        ExportDialog(
            games = state.games,
            initialSelected = if (state.selectedPackages.isNotEmpty()) state.selectedPackages
            else state.games.map { it.packageName }.toSet(),
            onDismiss = { showExport = false },
            onShare = { name, json -> onShareJson(name, json) }
        )
    }

    deleteTarget?.let { target ->
        ConfirmDialog(
            title = "删除配置",
            message = "确定要移除选中的 ${target.size} 项配置吗？此操作在点击“保存”后才会写入文件。",
            confirmLabel = "删除",
            onConfirm = { viewModel.deletePackages(target) },
            onDismiss = { deleteTarget = null }
        )
    }

    if (showAbout) {
        AboutDialog(onDismiss = { showAbout = false })
    }

    if (pendingDiscardRefresh) {
        ConfirmDialog(
            title = "放弃未保存的修改？",
            message = "当前有尚未写入文件的修改。刷新会重新读取磁盘内容，这些修改将丢失。",
            confirmLabel = "放弃并刷新",
            onConfirm = { viewModel.refresh() },
            onDismiss = { pendingDiscardRefresh = false }
        )
    }

    pendingPath?.let { path ->
        ConfirmDialog(
            title = "切换配置路径",
            message = "切换路径后会重新加载配置，当前未保存的修改将丢失。确定继续吗？",
            confirmLabel = "切换",
            onConfirm = { viewModel.setConfigPath(path) },
            onDismiss = { pendingPath = null }
        )
    }
}

@Composable
private fun StatusBanner(state: MainUiState, onOpenSettings: () -> Unit) {
    val (text, isError) = when (val root = state.rootState) {
        is RootState.Available -> {
            if (!state.moduleInstalled) {
                "未找到模块配置文件：${state.configPath}" to true
            } else {
                "模块已就绪 · ${state.games.size} 项配置 · ${state.configPath}" to false
            }
        }
        is RootState.Denied -> "Root 授权失败：${root.detail}" to true
        RootState.NotFound -> "未检测到 Root（su），无法读写系统配置" to true
    }

    Surface(
        color = if (isError) MiuixTheme.colorScheme.errorContainer
        else MiuixTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isError) {
                Icon(Icons.Filled.Warning, contentDescription = null)
                Spacer(Modifier.padding(horizontal = 4.dp))
            }
            Text(
                text = text,
                style = MiuixTheme.textStyles.footnote1,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Filled.Settings, contentDescription = "设置")
            }
        }
    }
}
