package com.micrantha.eyespie.model

/**
 * Platform capability determined before a model download is scheduled.
 *
 * Transport details remain platform-specific; shared consumers receive the existing
 * [ModelAssetState] lifecycle and stable diagnostic codes.
 */
enum class ModelDeliveryAvailability {
    Available,
    UnsupportedOperatingSystem,
    NotConfigured,
}

fun ModelDeliveryAvailability.initialModelAssetState(): ModelAssetState = when (this) {
    ModelDeliveryAvailability.Available -> ModelAssetState.NotInstalled
    ModelDeliveryAvailability.UnsupportedOperatingSystem -> ModelAssetState.Failed(
        stage = FailureStage.Compatibility,
        recoverable = false,
        diagnosticCode = "model_delivery_os_unsupported",
    )
    ModelDeliveryAvailability.NotConfigured -> ModelAssetState.Failed(
        stage = FailureStage.Scheduling,
        recoverable = false,
        diagnosticCode = "model_delivery_not_configured",
    )
}
