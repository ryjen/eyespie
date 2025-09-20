package com.micrantha.eyespie.core.data.ai.source

import com.cactus.CactusCompletionParams
import com.cactus.CactusInitParams
import com.cactus.CactusLM
import com.cactus.ChatMessage
import com.cactus.models.ToolParameter
import com.cactus.models.createTool

class LLMLocalSource(
    private val llm: CactusLM
) {
    suspend fun init(model: String? = null) = try {
        Result.success(
            llm.initializeModel(
                CactusInitParams(
                    model = model
                )
            )
        )
    } catch (e: Throwable) {
        Result.failure(e)
    }

    suspend fun generate(prompt: String, imagePath: String) = try {

        val result = llm.generateCompletion(
            listOf(ChatMessage(
                role = "user",
                content = prompt
            )),
            CactusCompletionParams(
                tools = listOf(
                    createTool(
                            name = "query_image",
                            description = "Get the embeddings of an image",
                            parameters = mapOf(
                                    "image" to ToolParameter(
                                        type = "string",
                                        description = "The path to the image"
                                    )
                            )
                    )
                )
            )
        )
        if (result == null || result.success.not()) {
            throw IllegalStateException("No result")
        }

        result.toolCalls?.forEach {

        }

        Result.success(result.response ?: "")
    } catch (e: Throwable) {
        Result.failure(e)
    }

    suspend fun download() = try {
        Result.success(
            llm.downloadModel()
        )
    } catch (e: Throwable) {
        Result.failure(e)
    }
}
