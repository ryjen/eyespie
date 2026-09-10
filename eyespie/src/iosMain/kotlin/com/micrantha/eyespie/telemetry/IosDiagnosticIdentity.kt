package com.micrantha.eyespie.telemetry

import platform.Foundation.NSBundle
import platform.UIKit.UIDevice

internal fun iosDiagnosticIdentity(): DiagnosticIdentity {
    val bundle = NSBundle.mainBundle
    val device = UIDevice.currentDevice
    return DiagnosticIdentity(
        release = DiagnosticReleaseIdentity(
            appVersion = bundleString(bundle, "CFBundleShortVersionString"),
            appBuild = bundleString(bundle, "CFBundleVersion").toIntOrNull()
                ?: error("installed iOS app build number is invalid"),
            sourceRevision = optionalBundleString(bundle, "EyespieSourceRevision"),
        ),
        runtime = DiagnosticRuntimeIdentity(
            platform = DiagnosticPlatform.IOS,
            osVersion = "iOS ${device.systemVersion}",
            mediaPipeVersion = bundleString(bundle, "EyespieMediaPipeTasksVersion"),
        ),
    )
}

private fun bundleString(bundle: NSBundle, key: String): String =
    optionalBundleString(bundle, key)
        ?: error("required iOS diagnostic identity is unavailable: $key")

private fun optionalBundleString(bundle: NSBundle, key: String): String? =
    (bundle.objectForInfoDictionaryKey(key) as? String)
        ?.trim()
        ?.takeIf { it.isNotEmpty() && !it.contains("$(") }
