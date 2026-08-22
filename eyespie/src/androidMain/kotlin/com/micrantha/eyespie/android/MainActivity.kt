package com.micrantha.eyespie.android

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import com.micrantha.eyespie.App
import com.micrantha.eyespie.AppUnavailable
import com.micrantha.eyespie.game.createAndroidEyespieRuntime
import com.micrantha.eyespie.sharing.rememberAndroidGameDocumentTransfer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val runtime = remember {
                try {
                    createAndroidEyespieRuntime(this)
                } catch (exception: Exception) {
                    Log.e(TAG, "Eyespie runtime initialization failed", exception)
                    null
                }
            }
            if (runtime == null) {
                AppUnavailable()
            } else {
                val documentTransfer = rememberAndroidGameDocumentTransfer()
                App(runtime, documentTransfer)
            }
        }
    }

    private companion object {
        const val TAG = "Eyespie"
    }
}
