package com.micrantha.eyespie.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.micrantha.eyespie.App
import com.micrantha.eyespie.AppUnavailable
import com.micrantha.eyespie.game.createAndroidEyespieRuntime
import com.micrantha.eyespie.sharing.AndroidBundleActions
import com.micrantha.eyespie.sharing.rememberAndroidGameDocumentTransfer

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
                val documentTransfer = rememberAndroidGameDocumentTransfer()
                var contentVersion by remember { mutableIntStateOf(0) }
                Box {
                    key(contentVersion) {
                        App(runtime)
                    }
                    AndroidBundleActions(
                        runtime = runtime,
                        transfer = documentTransfer,
                        onImported = { contentVersion += 1 },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                    )
                }
            }
        }
    }
}
