package com.micrantha.eyespie.core.data.ai.model

data class AiTool<out Out : Any, in In : Any>(
    val id: String,
    val description: String,
    val parameters: Map<String, Param>? = null,
    val function: suspend (In) -> Out
) {
    data class Call(
        val id: String,
        val arguments: Map<String, String>? = null,
    )

    data class Param(
        val type: String,
        val description: String,
        val required: Boolean = false,
    )
}
