package com.micrantha.eyespie.android

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import com.micrantha.eyespie.App
import com.micrantha.eyespie.AppUnavailable
import com.micrantha.eyespie.game.createAndroidEyespieRuntime
import com.micrantha.eyespie.sharing.AndroidGameDocumentTransfer
import com.micrantha.eyespie.sharing.externalEyespieDocumentUri
import com.micrantha.eyespie.sharing.rememberAndroidGameDocumentTransfer

class MainActivity : ComponentActivity() {
    private lateinit var documentTransfer: AndroidGameDocumentTransfer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        documentTransfer = AndroidGameDocumentTransfer(contentResolver)
        offerExternalGameDocument(intent)
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
                val transfer = rememberAndroidGameDocumentTransfer(documentTransfer)
                App(
                    runtime = runtime,
                    documentTransfer = transfer,
                    externalDocumentSource = documentTransfer,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        offerExternalGameDocument(intent)
    }

    private fun offerExternalGameDocument(intent: Intent?) {
        val uri = externalEyespieDocumentUri(intent) ?: return
        if (!documentTransfer.offerExternalDocument(uri)) {
            Log.w(TAG, "External Eyespie document ignored while another document operation is active")
        }
    }

    private companion object {
        const val TAG = "Eyespie"
    }
}
