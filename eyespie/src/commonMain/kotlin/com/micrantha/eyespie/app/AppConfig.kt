package com.micrantha.eyespie.app

import com.micrantha.eyespie.config.AppConfigDelegate
import com.micrantha.eyespie.config.DefaultAppConfigDelegate
object AppConfig {
    val LOGIN_EMAIL by DefaultAppConfigDelegate("user@example.com")
    val LOGIN_PASSWORD by DefaultAppConfigDelegate("******")
    val SUPABASE_URL by AppConfigDelegate
    val SUPABASE_KEY by AppConfigDelegate
}
