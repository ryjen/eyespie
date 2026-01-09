package com.micrantha.eyespie.core.data.client

import com.micrantha.eyespie.app.AppConfig

internal fun requireSupabaseConfig(): Pair<String, String> {
    val url = AppConfig.SUPABASE_URL
    val key = AppConfig.SUPABASE_KEY
    require(url.isNotBlank()) { "SUPABASE_URL is required to initialize Supabase clients." }
    require(key.isNotBlank()) { "SUPABASE_KEY is required to initialize Supabase clients." }
    return url to key
}
