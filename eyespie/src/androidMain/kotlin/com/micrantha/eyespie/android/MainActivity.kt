package com.micrantha.eyespie.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import com.micrantha.eyespie.App
import com.micrantha.eyespie.AppUnavailable
import com.micrantha.eyespie.app.AndroidExternalAppIntentSource
import com.micrantha.eyespie.game.createAndroidEyespieRuntime
import com.micrantha.eyespie.sharing.AndroidGameDocumentTransfer
import com.micrantha.eyespie.sharing.AndroidGameSharePresenter
import com.micrantha.eyespie.sharing.externalEyespieDocumentUri
import com.micrantha.eyespie.sharing.rememberAndroidGameDocumentTransfer

class MainActivity : ComponentActivity() {
    private lateinit var documentTransfer: AndroidGameDocumentTransfer
    private val externalAppIntents = AndroidExternalAppIntentSource()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        documentTransfer = AndroidGameDocumentTransfer(contentResolver)

        val restoredExternalDocument = savedInstanceState?.getString(STATE_PENDING_EXTERNAL_DOCUMENT)
        if (restoredExternalDocument != null) {
            val restoredUri = Uri.parse(restoredExternalDocument)
            if (!documentTransfer.offerExternalDocument(restoredUri)) {
                Log.w(TAG, "Pending Eyespie document could not be restored")
            }
        } else if (savedInstanceState == null) {
            offerExternalInput(intent)
        }

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
                val sharePresenter = remember { AndroidGameSharePresenter(this) }
                App(
                    runtime = runtime,
                    documentTransfer = transfer,
                    externalDocumentSource = documentTransfer,
                    sharePresenter = sharePresenter,
                    externalAppIntentSource = externalAppIntents,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        offerExternalInput(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        documentTransfer.pendingExternalDocumentState()?.let { pendingUri ->
            outState.putString(STATE_PENDING_EXTERNAL_DOCUMENT, pendingUri)
        }
        super.onSaveInstanceState(outState)
    }

    private fun offerExternalInput(intent: Intent?) {
        if (externalAppIntents.offer(intent)) return

        val uri = externalEyespieDocumentUri(intent) ?: return
        if (!documentTransfer.offerExternalDocument(uri)) {
            Log.w(TAG, "External Eyespie document ignored while another document operation is active")
        }
    }

    private companion object {
        const val TAG = "Eyespie"
        const val STATE_PENDING_EXTERNAL_DOCUMENT = "pending_external_document"
    }
}
