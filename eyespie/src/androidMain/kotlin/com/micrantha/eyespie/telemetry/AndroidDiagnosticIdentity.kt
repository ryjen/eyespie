package com.micrantha.eyespie.telemetry

import android.os.Build
import com.micrantha.eyespie.BuildConfig

internal fun androidDiagnosticIdentity(): DiagnosticIdentity = DiagnosticIdentity(
    release = DiagnosticReleaseIdentity(
        appVersion = BuildConfig.VERSION_NAME,
        appBuild = BuildConfig.VERSION_CODE,
    ),
    runtime = DiagnosticRuntimeIdentity(
        platform = DiagnosticPlatform.ANDROID,
        osVersion = "Android ${Build.VERSION.RELEASE}",
        mediaPipeVersion = BuildConfig.MEDIAPIPE_TASKS_VISION_VERSION,
    ),
)
