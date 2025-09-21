package com.micrantha.eyespie.core.data.ai.model

data class AiResult(
    val response: String?,
    val toolCalls: List<AiTool.Call>?,
)
