package com.micrantha.eyespie.game

import com.micrantha.eyespie.features.onboarding.OnboardingPreferenceStore
import com.micrantha.eyespie.sharing.GameBundleService

class EyespieRuntime(
    val gameLoop: LocalGameLoop,
    val bundleService: GameBundleService,
    val onboardingPreferences: OnboardingPreferenceStore,
    val gameThumbnailCache: GameThumbnailCache,
)
