package com.threadpilot.manager.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.threadpilot.manager.data.ConfigParser
import com.threadpilot.manager.data.GameConfig
import com.threadpilot.manager.data.ThreadRule
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun GameEditDialog(
    initial: GameConfig,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (GameConfig) -> Unit
) {
    var name by remember { mutableStateOf(initial.name) }
    var packageName by remember { mutableStateOf(initial.packageName) }
    var rules by remember { mutableStateOf(initial.rules) }
    var error by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(true) }

    fun close() {
        show = false
        onDismiss()
    }

    OverlayDialog(
        show = show,
        title = if (isNew) "新增游戏配置" else "编辑游戏配置",
        onDismissRequest = { close() },
        content = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "游戏名称",
                    useLabelAsPlaceholder = true,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                TextField(
                    value = packageName,
                    onValueChange = { packageName = it.trim() },
                    label = "包名，例如 com.tencent.tmgp.sgame",
                    useLabelAsPlaceholder = true,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(14.dp))
                Text("线程规则", style = MiuixTheme.textStyles.title3)
                HorizontalDivider()
                Spacer(Modifier.height(4.dp))

                rules.forEachIndexed { index, rule ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextField(
                            value = rule.threadName,
                            onValueChange = { value ->
                                rules = rules.toMutableList().also { it[index] = rule.copy(threadName = value) }
                            },
                            label = "线程名（留空=主进程）",
                            useLabelAsPlaceholder = true,
                            singleLine = true,
                            modifier = Modifier.weight(1.2f)
                        )
                        Spacer(Modifier.width(8.dp))
                        TextField(
                            value = rule.cpuCores,
                            onValueChange = { value ->
                                rules = rules.toMutableList().also { it[index] = rule.copy(cpuCores = value) }
                            },
                            label = "CPU",
                            useLabelAsPlaceholder = true,
                            singleLine = true,
                            modifier = Modifier.weight(0.8f)
                        )
                        IconButton(onClick = {
                            rules = rules.filterIndexed { i, _ -> i != index }
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = "删除该规则")
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }

                TextButton(
                    text = "+ 添加线程规则",
                    onClick = { rules = rules + ThreadRule() },
                    colors = ButtonDefaults.textButtonColorsPrimary()
                )

                if (error.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(error, color = MiuixTheme.colorScheme.error, style = MiuixTheme.textStyles.footnote1)
                }

                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        text = "取消",
                        onClick = { close() },
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            val message = when {
                                name.isBlank() -> "请填写游戏名称"
                                !ConfigParser.isValidPackageName(packageName) -> "包名格式不正确"
                                rules.isEmpty() -> "至少添加一条线程规则"
                                rules.any { !ConfigParser.isValidCpuCores(it.cpuCores) } ->
                                    "CPU 核心格式不正确，示例：7 / 2-4 / 0,2,4"
                                else -> null
                            }
                            if (message == null) {
                                onSave(
                                    GameConfig(
                                        name = name.trim(),
                                        packageName = packageName.trim(),
                                        rules = rules.map {
                                            it.copy(
                                                threadName = it.threadName.trim(),
                                                cpuCores = it.cpuCores.trim()
                                            )
                                        }
                                    )
                                )
                                close()
                            } else {
                                error = message
                            }
                        },
                        colors = ButtonDefaults.buttonColorsPrimary(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    )
}
