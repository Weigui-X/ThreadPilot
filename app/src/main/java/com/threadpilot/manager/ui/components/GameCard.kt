package com.threadpilot.manager.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.state.ToggleableState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.threadpilot.manager.data.GameConfig
import com.threadpilot.manager.data.ThreadRule
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

@Composable
fun GameCard(
    game: GameConfig,
    query: String,
    highlightSearch: Boolean,
    expanded: Boolean,
    selectionMode: Boolean,
    selected: Boolean,
    onToggleExpand: () -> Unit,
    onToggleSelected: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 5.dp),
        colors = CardDefaults.defaultColors(
            color = if (selected) MiuixTheme.colorScheme.primaryContainer
            else MiuixTheme.colorScheme.surfaceContainer
        ),
        pressFeedbackType = PressFeedbackType.Sink,
        showIndication = true,
        onClick = { if (selectionMode) onToggleSelected() else onToggleExpand() }
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                Checkbox(
                    state = if (selected) ToggleableState.On else ToggleableState.Off,
                    onClick = onToggleSelected
                )
                Spacer(Modifier.width(2.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = highlight(game.name, query, highlightSearch),
                    style = MiuixTheme.textStyles.title4,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = highlight(game.packageName, query, highlightSearch),
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MiuixTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "${game.rules.size} 条规则",
                            style = MiuixTheme.textStyles.footnote2,
                            color = MiuixTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                    channelTag(game.packageName)?.let { tag ->
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            color = MiuixTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = tag,
                                style = MiuixTheme.textStyles.footnote2,
                                color = MiuixTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            if (!selectionMode) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "编辑")
                }
                IconButton(onClick = onShare) {
                    Icon(Icons.Filled.Share, contentDescription = "分享")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "删除")
                }
                IconButton(onClick = onToggleExpand) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.KeyboardArrowUp
                        else Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (expanded) "收起" else "展开"
                    )
                }
            }
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 10.dp)
            ) {
                game.rules.forEach { rule -> RuleRow(rule) }
            }
        }
    }
}

@Composable
private fun RuleRow(rule: ThreadRule) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (rule.threadName.isBlank()) "主进程" else rule.threadName,
            style = MiuixTheme.textStyles.body2,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Surface(
            color = MiuixTheme.colorScheme.secondaryContainer
        ) {
            Text(
                text = "CPU ${rule.cpuCores}",
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

private fun highlight(text: String, query: String, enabled: Boolean): AnnotatedString {
    val needle = query.trim()
    if (!enabled || needle.isEmpty()) return AnnotatedString(text)

    return buildAnnotatedString {
        var cursor = 0
        while (cursor < text.length) {
            val index = text.indexOf(needle, startIndex = cursor, ignoreCase = true)
            if (index < 0) {
                append(text.substring(cursor))
                break
            }
            append(text.substring(cursor, index))
            withStyle(SpanStyle(background = Color(0xFFFFF176), color = Color.Black)) {
                append(text.substring(index, index + needle.length))
            }
            cursor = index + needle.length
        }
    }
}

private fun channelTag(packageName: String): String? {
    val lower = packageName.lowercase()
    return when {
        lower.contains("tencent") || lower.contains("tmgp") || lower.contains("pubg") -> "腾讯"
        lower.contains("mihoyo") || lower.contains("hoyoverse") -> "米哈游"
        lower.contains("xiaomi") || lower.contains("miui") -> "小米"
        lower.contains("vivo") -> "vivo"
        lower.contains("oppo") -> "OPPO"
        lower.contains("huawei") || lower.contains("honor") -> "华为/荣耀"
        lower.contains("bilibili") -> "B站"
        lower.contains("samsung") -> "三星"
        else -> null
    }
}
