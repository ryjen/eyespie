package com.micrantha.bluebell.domain.repository

import com.micrantha.bluebell.domain.entities.LocalizedString
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString

interface LocalizedRepository {
    fun string(str: LocalizedString) = run {
        runBlocking { getString(str) }
    }

    fun string(str: LocalizedString, vararg args: Any) = run {
        runBlocking { getString(str, *args) }
    }

    fun format(epochSeconds: Long, format: String, timeZone: String): String

    fun format(format: String, vararg args: Any): String
}
