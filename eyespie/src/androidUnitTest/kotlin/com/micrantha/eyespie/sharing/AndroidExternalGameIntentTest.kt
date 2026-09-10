package com.micrantha.eyespie.sharing

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AndroidExternalGameIntentTest {
    @Test
    fun view_with_eyespie_media_type_is_accepted() {
        val uri = Uri.parse("content://example/game/42")
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = uri
            type = EYESPIE_ANDROID_MIME_TYPE
        }

        assertEquals(uri, externalEyespieDocumentUri(intent))
    }

    @Test
    fun view_with_eyespie_extension_is_accepted_even_when_sender_uses_generic_type() {
        val uri = Uri.parse("content://example/shared/mission.eyespie")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            type = "application/octet-stream"
        }

        assertEquals(uri, externalEyespieDocumentUri(intent))
    }

    @Test
    fun send_with_single_clip_uri_is_accepted() {
        val uri = Uri.parse("content://example/shared/mission.eyespie")
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = EYESPIE_ANDROID_MIME_TYPE
            clipData = ClipData.newRawUri("mission.eyespie", uri)
        }

        assertEquals(uri, externalEyespieDocumentUri(intent))
    }

    @Test
    fun unrelated_binary_document_is_not_claimed() {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("content://example/shared/archive.bin"),
        ).apply {
            type = "application/octet-stream"
        }

        assertNull(externalEyespieDocumentUri(intent))
    }

    @Test
    fun network_uri_is_not_accepted_as_document_authority() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com/game.eyespie"))

        assertNull(externalEyespieDocumentUri(intent))
    }

    @Test
    fun external_file_uri_is_not_accepted_as_document_authority() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("file:///tmp/game.eyespie")).apply {
            type = EYESPIE_ANDROID_MIME_TYPE
        }

        assertNull(externalEyespieDocumentUri(intent))
    }
}
