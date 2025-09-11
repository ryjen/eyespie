package com.micrantha.bluebell.data.download

import io.ktor.client.HttpClient
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

class DownloadService(
    private val httpClient: HttpClient = HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 60000
            connectTimeoutMillis = 30000
            socketTimeoutMillis = 30000
        }
        expectSuccess = false
    },
) {

    private data class DownloadFileInfo(
        val fileName: String,
        val contentLength: Long,
        val supportsResume: Boolean
    )

    suspend fun downloadFile(
        url: String,
        outputSink: BufferedSink,
        resumePosition: Long,
        progressCallback: suspend (bytesDownloaded: Long, totalBytes: Long, progress: Float) -> Unit
    ) {
        val fileInfo = try {
            getFileInfo(url, resumePosition)
        } catch (e: Exception) {
            null
        }
        httpClient.prepareGet(url) {
            if (resumePosition > 0 && fileInfo?.supportsResume == true) {
                header(HttpHeaders.Range, "bytes=${resumePosition}-")
            }
        }.execute { response ->

            if (!response.status.isSuccess() && response.status != HttpStatusCode.PartialContent) {
                throw Exception("HTTP Error: ${response.status.value} - ${response.status.description}")
            }

            val contentLength = fileInfo?.contentLength ?: response.contentLength() ?: -1L
            val totalBytes = if (contentLength > 0) contentLength + resumePosition else -1L
            val source = response.bodyAsChannel()
            var totalBytesRead = resumePosition

            val onProgress = suspend {
                val progress = if (totalBytes > 0) {
                    totalBytesRead.toFloat() / totalBytes.toFloat()
                } else 0f

                progressCallback(totalBytesRead, totalBytes, progress)
            }

            val bufferSize = 8192L

            while (!source.isClosedForRead) {
                val bytesRead = source.readRemaining(bufferSize)
                if (bytesRead.exhausted()) {
                    break
                }

                val bytes = bytesRead.readByteArray()

                outputSink.write(bytes)

                totalBytesRead += bytes.size

                onProgress()
            }
        }
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

        val contentLength = response.headers[HttpHeaders.ContentLength]?.toLongOrNull() ?: -1L
        val fileName = response.headers[HttpHeaders.ContentDisposition]
            ?.let { disposition ->
                disposition.substringAfter("filename=", "")
                    .trim('"')
                    .takeIf { it.isNotEmpty() }
            } ?: url.substringAfterLast('/').takeIf { it.isNotEmpty() } ?: "download"

        val acceptsRanges = response.headers[HttpHeaders.AcceptRanges]?.contains("bytes") == true

        return DownloadFileInfo(
            fileName = fileName,
            contentLength = contentLength,
            supportsResume = acceptsRanges
        )
    }

    fun close() {
        httpClient.close()
    }
}
