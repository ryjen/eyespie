package com.micrantha.eyespie.model

/**
 * Narrow platform seam used by the Kotlin repository before scheduling model delivery.
 *
 * Swift/native composition may replace the default implementation once transport wiring exists.
 */
fun interface IosModelDeliveryCapabilities {
    fun snapshot(): IosModelDeliveryCapabilitySnapshot
}

data class IosModelDeliveryCapabilitySnapshot(
    val backgroundUrlSessionSupported: Boolean,
    val backgroundUrlSessionConfigured: Boolean,
    val backgroundAssetsSupported: Boolean,
    val backgroundAssetsConfigured: Boolean,
) {
    fun availability(): ModelDeliveryAvailability {
        val urlSessionAvailable =
            backgroundUrlSessionSupported && backgroundUrlSessionConfigured
        val backgroundAssetsAvailable =
            backgroundAssetsSupported && backgroundAssetsConfigured

        return when {
            urlSessionAvailable || backgroundAssetsAvailable -> ModelDeliveryAvailability.Available
            backgroundUrlSessionSupported || backgroundAssetsSupported ->
                ModelDeliveryAvailability.NotConfigured
            else -> ModelDeliveryAvailability.UnsupportedOperatingSystem
        }
    }
}

internal object DefaultIosModelDeliveryCapabilities : IosModelDeliveryCapabilities {
    override fun snapshot() = IosModelDeliveryCapabilitySnapshot(
        backgroundUrlSessionSupported = true,
        backgroundUrlSessionConfigured = true,
        backgroundAssetsSupported = false,
        backgroundAssetsConfigured = false,
    )
}
