package com.threadpilot.manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.threadpilot.manager.data.ConfigRepository
import com.threadpilot.manager.data.SettingsStore
import com.threadpilot.manager.ui.MainScreen
import com.threadpilot.manager.ui.MainViewModel
import com.threadpilot.manager.ui.theme.ThreadPilotTheme
import com.threadpilot.manager.util.shareJsonFile

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val settings = SettingsStore(this)
        val repository = ConfigRepository(settings)
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(repository, settings) as T
            }
        }
        val viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        setContent {
            ThreadPilotTheme {
                MainScreen(
                    viewModel = viewModel,
                    settings = settings,
                    onShareJson = { name, json -> shareJsonFile(this, name, json) }
                )
            }
        }
    }
}
