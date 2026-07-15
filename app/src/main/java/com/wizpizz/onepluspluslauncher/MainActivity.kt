package com.wizpizz.onepluspluslauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.wizpizz.onepluspluslauncher.service.ModuleService
import com.wizpizz.onepluspluslauncher.ui.MainScreen
import com.wizpizz.onepluspluslauncher.ui.OnePlusPlusLauncherTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    private val scopeRefreshEvents = MutableStateFlow(0L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ModuleService.initialize()
        enableEdgeToEdge()

        setContent {
            val service by ModuleService.service.collectAsState()
            val scopeRefreshEvent by scopeRefreshEvents.collectAsState()
            OnePlusPlusLauncherTheme {
                MainScreen(service, scopeRefreshEvent)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        scopeRefreshEvents.value++
    }
}
