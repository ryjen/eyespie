package com.micrantha.eyespie.game

import com.micrantha.eyespie.features.onboarding.OnboardingPreferenceStore
import com.micrantha.eyespie.sharing.GameBundleService
import com.micrantha.eyespie.telemetry.DiagnosticExportService
import com.micrantha.eyespie.telemetry.DiagnosticHistory

class EyespieRuntime(
    val gameLoop: LocalGameLoop,
    val bundleService: GameBundleService,
    val onboardingPreferences: OnboardingPreferenceStore,
    val gameThumbnailCache: GameThumbnailCache,
    val diagnostics: DiagnosticHistory,
    val diagnosticExport: DiagnosticExportService,
)
