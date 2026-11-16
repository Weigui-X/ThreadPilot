package com.threadpilot.manager.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.state.ToggleableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.threadpilot.manager.data.GameConfig
import com.threadpilot.manager.util.ShareCodec
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ImportDialog(
    initialText: String,
    onDismiss: () -> Unit,
    onPickFile: () -> Unit,
    onConfirm: (List<GameConfig>, Boolean) -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    var preview by remember { mutableStateOf<List<GameConfig>>(emptyList()) }
    var error by remember { mutableStateOf("") }
    var overwrite by remember { mutableStateOf(false) }
    var show by remember { mutableStateOf(true) }

    fun close() {
        show = false
        onDismiss()
    }

    LaunchedEffect(initialText) {
        if (initialText.isNotBlank()) {
            text = initialText
            preview = runCatching { ShareCodec.decode(initialText) }.getOrDefault(emptyList())
            error = if (preview.isEmpty()) "未能从文件中解析出配置" else ""
        }
    }

    OverlayDialog(
        show = show,
        title = "导入配置",
        onDismissRequest = { close() },
        content = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).heightIn(max = 420.dp)) {
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    label = "粘贴 JSON 配置内容",
                    useLabelAsPlaceholder = true,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 200.dp)
                )
                Row {
                    TextButton(text = "选择文件", onClick = onPickFile)
                    TextButton(
                        text = "解析",
                        onClick = {
                            runCatching { ShareCodec.decode(text) }
                                .onSuccess { list ->
                                    preview = list
                                    error = if (list.isEmpty()) "未解析到配置" else ""
                                }
                                .onFailure { error = "JSON 解析失败：${it.message}" }
                        },
                        colors = ButtonDefaults.textButtonColorsPrimary()
                    )
                }

                if (error.isNotBlank()) {
                    Text(error, color = MiuixTheme.colorScheme.error, style = MiuixTheme.textStyles.footnote1)
                }

                preview.forEach { game ->
                    Text(
                        "· ${game.name} (${game.packageName}) — ${game.rules.size} 条规则",
                        style = MiuixTheme.textStyles.footnote1
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        state = if (overwrite) ToggleableState.On else ToggleableState.Off,
                        onClick = { overwrite = !overwrite }
                    )
                    Text("相同包名时覆盖当前配置", style = MiuixTheme.textStyles.footnote1)
                }

                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(text = "取消", onClick = { close() }, modifier = Modifier.weight(1f))
                    Button(
                        onClick = {
                            onConfirm(preview, overwrite)
                            close()
                        },
                        enabled = preview.isNotEmpty(),
                        colors = ButtonDefaults.buttonColorsPrimary(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("导入")
                    }
                }
            }
        }
    )
}

@Composable
fun ExportDialog(
    games: List<GameConfig>,
    initialSelected: Set<String>,
    onDismiss: () -> Unit,
    onShare: (String, String) -> Unit
) {
    var selected by remember { mutableStateOf(initialSelected) }
    var show by remember { mutableStateOf(true) }

    fun close() {
        show = false
        onDismiss()
    }

    OverlayDialog(
        show = show,
        title = "分享配置",
        onDismissRequest = { close() },
        content = {
            Column(modifier = Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState())) {
                Text("选择要导出的游戏：", style = MiuixTheme.textStyles.footnote1)
                Spacer(Modifier.height(4.dp))
                games.forEach { game ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            state = if (game.packageName in selected) ToggleableState.On else ToggleableState.Off,
                            onClick = {
                                selected = if (game.packageName in selected) selected - game.packageName
                                else selected + game.packageName
                            }
                        )
                        Text(
                            "${game.name} (${game.packageName})",
                            style = MiuixTheme.textStyles.footnote1
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(text = "取消", onClick = { close() }, modifier = Modifier.weight(1f))
                    Button(
                        onClick = {
                            val chosen = games.filter { it.packageName in selected }
                            onShare("threadpilot-config.json", ShareCodec.encode(chosen))
                            close()
                        },
                        enabled = selected.isNotEmpty(),
                        colors = ButtonDefaults.buttonColorsPrimary(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("生成并分享")
                    }
                }
            }
        }
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String = "确定",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var show by remember { mutableStateOf(true) }

    fun close() {
        show = false
        onDismiss()
    }

    OverlayDialog(
        show = show,
        title = title,
        summary = message,
        onDismissRequest = { close() },
        content = {
            Row {
                TextButton(text = "取消", onClick = { close() }, modifier = Modifier.weight(1f))
                Button(
                    onClick = {
                        onConfirm()
                        close()
                    },
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(confirmLabel)
                }
            }
        }
    )
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    var show by remember { mutableStateOf(true) }

    fun close() {
        show = false
        onDismiss()
    }

    OverlayDialog(
        show = show,
        title = "关于 线程管理",
        onDismissRequest = { close() },
        content = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("版本 1.0.0", style = MiuixTheme.textStyles.body2)
                Spacer(Modifier.height(8.dp))
                Text(
                    "本应用是独立实现的线程优化模块配置管理器，界面采用小米 Miuix 组件库，用于直接编辑 applist.conf。它不依赖、也不包含任何第三方配置管理项目的源码。",
                    style = MiuixTheme.textStyles.footnote1
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "修改系统配置需要 Root 权限。错误的 CPU 核心设置可能影响性能或导致卡顿，请在保存前确认配置正确。保存前会自动创建 .tp-bak 备份。",
                    style = MiuixTheme.textStyles.footnote1
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { close() },
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("知道了")
                }
            }
        }
    )
}
