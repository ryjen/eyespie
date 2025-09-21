package com.micrantha.eyespie.core.data.ai.source

import com.cactus.CactusCompletionParams
import com.cactus.CactusInitParams
import com.cactus.CactusLM
import com.cactus.ChatMessage
import com.cactus.models.ToolParameter
import com.cactus.models.createTool
import com.micrantha.eyespie.core.data.ai.model.AiPrompt
import com.micrantha.eyespie.core.data.ai.model.AiResult
import com.micrantha.eyespie.core.data.ai.model.AiTool

class AgentLocalSource(
    private val llm: CactusLM,
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

    suspend fun generate(prompts: List<AiPrompt>, tools: Map<String, AiTool<*, *>>) = try {
        val completion = llm.generateCompletion(
            prompts.map {
                ChatMessage(
                    role = it.role,
                    content = it.prompt
                )
            },
            CactusCompletionParams(
                tools = tools.map { (name, tool) ->
                    createTool(
                        name = name,
                        description = tool.description,
                        parameters = tool.parameters?.map { (name, param) ->
                            name to ToolParameter(
                                type = param.type,
                                description = param.description,
                                required = param.required
                            )
                        }?.toMap() ?: emptyMap()
                    )
                }
            )
        )
        if (completion == null) {
            throw IllegalStateException("No result")
        }

        if (completion.success.not()) {
            throw IllegalStateException("Not successful")
        }

        val result = AiResult(
            response = completion.response,
            toolCalls = completion.toolCalls?.map {
                AiTool.Call(
                    id = tools[it.name]?.id ?: it.name,
                    arguments = it.arguments
                )
            }
        )

        Result.success(result)
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
