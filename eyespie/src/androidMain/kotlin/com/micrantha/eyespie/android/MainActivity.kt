package com.micrantha.eyespie.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import com.micrantha.eyespie.App
import com.micrantha.eyespie.AppUnavailable
import com.micrantha.eyespie.game.createAndroidEyespieRuntime

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val runtime = remember {
                try {
                    createAndroidEyespieRuntime(this)
                } catch (_: Exception) {
                    null
                }
            }
            if (runtime == null) {
                AppUnavailable()
            } else {
                App(runtime)
            }
        }
    }
}
