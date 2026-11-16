package com.threadpilot.manager.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.threadpilot.manager.data.SettingsStore
import com.threadpilot.manager.root.RootState
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SettingsDialog(
    currentPath: String,
    backupEnabled: Boolean,
    highlight: Boolean,
    rootState: RootState,
    moduleProps: Map<String, String>,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onBackupChange: (Boolean) -> Unit,
    onHighlightChange: (Boolean) -> Unit
) {
    var path by remember { mutableStateOf(currentPath) }
    var show by remember { mutableStateOf(true) }

    fun close() {
        show = false
        onDismiss()
    }

    OverlayDialog(
        show = show,
        title = "设置",
        onDismissRequest = { close() },
        content = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("配置路径", style = MiuixTheme.textStyles.title3)
                Spacer(Modifier.height(4.dp))
                TextField(
                    value = path,
                    onValueChange = { path = it },
                    label = "applist.conf 完整路径",
                    useLabelAsPlaceholder = true,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(
                    text = "恢复默认路径",
                    onClick = { path = SettingsStore.DEFAULT_PATH }
                )
                Text(
                    text = "默认指向 AppOpt 模块。若你的模块安装在其他目录，可在此修改路径；应用会自动检测该目录下的 module.prop。",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )

                Spacer(Modifier.height(12.dp))
                SwitchRow(
                    title = "保存前自动备份",
                    subtitle = "在原文件旁生成 .tp-bak 备份",
                    checked = backupEnabled,
                    onCheckedChange = onBackupChange
                )
                SwitchRow(
                    title = "搜索关键词高亮",
                    subtitle = "在名称和包名中标记匹配文字",
                    checked = highlight,
                    onCheckedChange = onHighlightChange
                )

                Spacer(Modifier.height(12.dp))
                Text("状态", style = MiuixTheme.textStyles.title3)
                Spacer(Modifier.height(4.dp))
                val rootText = when (rootState) {
                    is RootState.Available -> "Root：已授权"
                    is RootState.Denied -> "Root：被拒绝（${rootState.detail}）"
                    RootState.NotFound -> "Root：未检测到 su"
                }
                Text(rootText, style = MiuixTheme.textStyles.footnote1)
                if (moduleProps.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "模块：${moduleProps["name"] ?: "未知"} ${moduleProps["version"] ?: ""}",
                        style = MiuixTheme.textStyles.footnote1
                    )
                }

                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        text = "取消",
                        onClick = { close() },
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            onSave(path)
                            close()
                        },
                        colors = ButtonDefaults.buttonColorsPrimary(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("保存并重载")
                    }
                }
            }
        }
    )
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MiuixTheme.textStyles.body2)
            Text(
                subtitle,
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
    Spacer(Modifier.height(8.dp))
}
