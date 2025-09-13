package com.micrantha.eyespie.android

import android.app.Application
import com.cactus.CactusContextInitializer

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CactusContextInitializer.initialize(this)
    }
}
