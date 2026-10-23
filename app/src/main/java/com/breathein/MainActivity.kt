package com.breathein

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.breathein.ui.BreatheInApp
import com.breathein.ui.theme.BreatheInTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val container get() = (application as BreatheInApplication).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BreatheInTheme {
                BreatheInApp(container)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumeMonitoringIfEnabled()
    }

    /** Restarts the service if the user had it on (e.g. after it stopped because a permission was revoked). */
    private fun resumeMonitoringIfEnabled() {
        lifecycleScope.launch {
            val controller = container.monitorController
            val wanted = container.settingsRepository.monitoringEnabled.first()
            if (wanted && !controller.isRunning.value && container.permissionChecker.allGranted()) {
                controller.start()
            }
        }
    }
}
