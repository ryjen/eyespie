package com.micrantha.eyespie.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.micrantha.eyespie.App
import com.micrantha.eyespie.game.createAndroidOfflineRuntime

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val runtime = createAndroidOfflineRuntime(applicationContext)
        setContent { App(runtime) }
    }
}
