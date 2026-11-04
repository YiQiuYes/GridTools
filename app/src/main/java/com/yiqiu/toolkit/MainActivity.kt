package com.yiqiu.toolkit

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.yiqiu.toolkit.ui.ToolKitApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge; ComponentActivity.enableEdgeToEdge also handles
        // status/navigation bar icon contrast for light/dark themes.
        enableEdgeToEdge()

        // Let the adaptive bottom bar colors extend to the bottom of the screen.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        setContent {
            ToolKitApp()
        }
    }
}
