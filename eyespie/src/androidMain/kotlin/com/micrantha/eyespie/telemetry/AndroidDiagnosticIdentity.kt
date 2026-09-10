package com.micrantha.eyespie.telemetry

import android.os.Build
import com.micrantha.eyespie.BuildConfig

internal fun androidDiagnosticIdentity(): DiagnosticIdentity = DiagnosticIdentity(
    release = DiagnosticReleaseIdentity(
        appVersion = BuildConfig.VERSION_NAME,
        appBuild = BuildConfig.VERSION_CODE,
        sourceRevision = BuildConfig.SOURCE_REVISION.takeIf(String::isNotBlank),
    ),
    runtime = DiagnosticRuntimeIdentity(
        platform = DiagnosticPlatform.ANDROID,
        osVersion = "Android ${Build.VERSION.RELEASE}",
        mediaPipeVersion = BuildConfig.MEDIAPIPE_TASKS_VISION_VERSION,
    ),
)
