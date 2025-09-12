package com.micrantha.eyespie.app

import com.micrantha.bluebell.platform.AppConfigDelegate
import com.micrantha.bluebell.platform.DefaultAppConfigDelegate
import com.micrantha.eyespie.config.EnvConfig
import com.micrantha.eyespie.config.EnvConfig.MODEL_MAX
import com.micrantha.eyespie.config.get
import com.micrantha.eyespie.config.getValue

object AppConfig {
    val LOGIN_EMAIL by DefaultAppConfigDelegate("user@example.com")
    val LOGIN_PASSWORD by DefaultAppConfigDelegate("******")
    val SUPABASE_URL by AppConfigDelegate
    val SUPABASE_KEY by AppConfigDelegate

    val MODELS by lazy {
        val models = mutableMapOf<String, String>()
        for (i in 0 until MODEL_MAX) {
            models.put(EnvConfig.get("MODEL_${i}_NAME")!!, EnvConfig.get("MODEL_${i}_URL")!!)
        }
        models.toMap()
    }
}
