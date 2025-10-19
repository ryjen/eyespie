package com.micrantha.eyespie.features.onboarding.data

import com.micrantha.bluebell.domain.usecase.dispatchUseCase
import com.micrantha.eyespie.core.data.client.createHttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.DataConversion.install
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders.ContentEncoding
import io.ktor.util.ContentEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

private const val MODEL_REPO_URL = "https://dubnium.tail4d84c.ts.net/models"

class ModelMetaRepository {
    private val client by lazy {
        createHttpClient {
            url(MODEL_REPO_URL)
        }
    }

    private fun DefaultRequest.DefaultRequestBuilder.install(plugin: Any, scope: () -> Unit) {}

    suspend fun listModels(): Result<List<String>> =
        dispatchUseCase(Dispatchers.IO) {
            client.get("/?get=list&folders=*").bodyAsText().lines()
    }

    suspend fun modelUrl(name: String): Result<String> =
        dispatchUseCase(Dispatchers.IO) {
            client.get("/${name}/?get=list").bodyAsText()
        }

    suspend fun modelChecksum(name: String): Result<String> =
        dispatchUseCase(Dispatchers.IO) {
            val resp = client.get("/${name}/sha256/?get=list")
            client.get(resp.bodyAsText().trim()).bodyAsText()
        }
}
