package com.threadpilot.manager.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/**
 * MIUI / HyperOS 风格主题。
 * 使用 Miuix 的 Monet 动态配色，以小米蓝为种子色；
 * 在不支持动态色的设备上回退到主题控制器生成的蓝白配色。
 */
@Composable
fun ThreadPilotTheme(
    content: @Composable () -> Unit
) {
    val controller = remember {
        ThemeController(
            colorSchemeMode = ColorSchemeMode.MonetSystem,
            keyColor = Color(0xFF3482FF)
        )
    }
    MiuixTheme(controller = controller, content = content)
}
