package com.micrantha.eyespie.core.data.client

import com.micrantha.eyespie.app.AppConfig

internal fun isSupabaseConfigured(): Boolean {
    val url = AppConfig.SUPABASE_URL
    val key = AppConfig.SUPABASE_KEY
    if (url.isBlank() || key.isBlank()) return false
    if (url.contains("your-", ignoreCase = true)) return false
    if (!key.startsWith("eyJ")) return false
    return true
}

internal fun requireSupabaseConfig(): Pair<String, String> {
    val url = AppConfig.SUPABASE_URL
    val key = AppConfig.SUPABASE_KEY
    require(url.isNotBlank()) { "SUPABASE_URL is required to initialize Supabase clients." }
    require(key.isNotBlank()) { "SUPABASE_KEY is required to initialize Supabase clients." }
    return url to key
}
