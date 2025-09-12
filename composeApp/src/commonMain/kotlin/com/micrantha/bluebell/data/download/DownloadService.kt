package com.micrantha.bluebell.data.download

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.head
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray
import okio.BufferedSink
import kotlin.time.ExperimentalTime

class DownloadService(
    private val httpClient: HttpClient = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 60000
            connectTimeoutMillis = 30000
            socketTimeoutMillis = 30000
        }
        expectSuccess = false
    },
) {

    interface Listener {
        suspend fun onProgress(bytesDownloaded: Long, progress: Int)
        fun onCompleted() = Unit
        fun onStart(totalBytes: Long)
    }

    private data class DownloadFileInfo(
        val contentLength: Long?,
        val supportsResume: Boolean
    )

    @OptIn(ExperimentalTime::class)
    suspend fun downloadFile(
        url: String,
        outputSink: BufferedSink,
        resumePosition: Long,
        listener: Listener? = null,
    ) {
        val fileInfo = try {
            getFileInfo(url, resumePosition)
        } catch (_: Exception) {
            null
        }

        httpClient.prepareGet(url) {
            if (resumePosition > 0 && fileInfo?.supportsResume == true) {
                header(HttpHeaders.Range, "bytes=${resumePosition}-")
            }
        }.execute { response ->
            val isPartial = response.status == HttpStatusCode.PartialContent

            if (!response.status.isSuccess() && !isPartial) {
                throw Exception("HTTP Error: ${response.status.value} - ${response.status.description}")
            }

            val contentLength = response.contentLength() ?: fileInfo?.contentLength ?: -1L
            val totalBytes =
                if (isPartial && contentLength > 0) contentLength + resumePosition else contentLength
            val source = response.bodyAsChannel()
            var totalBytesRead = resumePosition

            listener?.onStart(totalBytes)

            while (!source.isClosedForRead) {
                val bytesRead = source.readRemaining(8192L)
                if (bytesRead.exhausted()) {
                    break
                }

                bytesRead.readByteArray().apply {
                    outputSink.write(this)
                    totalBytesRead += size
                }

                val progress = if (totalBytes > 0) {
                    (totalBytesRead.toFloat() / totalBytes.toFloat()) * 100f
                } else 0f

                listener?.onProgress(totalBytesRead, progress.toInt())
            }
        }
        listener?.onCompleted()
    }

    private suspend fun getFileInfo(url: String, resumePosition: Long): DownloadFileInfo {
        val response = httpClient.head(url) {
            if (resumePosition > 0) {
                header(HttpHeaders.Range, "bytes=$resumePosition-")
            }
        }
        if (!response.status.isSuccess()) {
            throw Exception("HTTP Error: ${response.status.value} - ${response.status.description}")
        }

        val contentLength = response.headers[HttpHeaders.ContentLength]?.toLongOrNull()
        val acceptsRanges = response.headers[HttpHeaders.AcceptRanges]?.contains("bytes") == true

        return DownloadFileInfo(
            contentLength = contentLength,
            supportsResume = acceptsRanges
        )
    }

    fun close() {
        httpClient.close()
    }
}
