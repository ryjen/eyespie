package com.micrantha.eyespie

import com.micrantha.bluebell.platform.FileSystem
import com.micrantha.bluebell.platform.GenAI
import com.micrantha.bluebell.platform.Platform
import com.micrantha.bluebell.platform.PlatformGenAI
import com.micrantha.bluebell.platform.PlatformImpl
import com.micrantha.eyespie.core.data.ai.GenAISemanticInferenceProvider
import com.micrantha.eyespie.core.data.db.DatabaseDriverFactory
import com.micrantha.eyespie.domain.ai.InferenceLocality
import com.micrantha.eyespie.domain.ai.SemanticInferenceAvailability
import com.micrantha.eyespie.domain.ai.SemanticInferenceAvailabilityController
import com.micrantha.eyespie.domain.ai.SemanticInferenceIdentity
import com.micrantha.eyespie.domain.ai.SemanticInferenceProvider
import com.micrantha.eyespie.domain.ai.SemanticInferenceReasonCode
import com.micrantha.eyespie.model.iosModelAssetModule
import org.kodein.di.DI
import org.kodein.di.bindProvider
import org.kodein.di.bindSingleton
import org.kodein.di.bindSingletonOf
import org.kodein.di.delegate
import org.kodein.di.instance

fun iosModules(app: AppDelegate) = DI {

    import(iosModelAssetModule(app.modelDeliveryCapabilities))

    bindSingleton { PlatformImpl(app.networkMonitor) }
    delegate<Platform>().to<PlatformImpl>()
    delegate<FileSystem>().to<PlatformImpl>()

    bindSingletonOf(::PlatformGenAI)
    delegate<GenAI>().to<PlatformGenAI>()

    bindSingleton {
        GenAISemanticInferenceProvider(
            genAI = instance(),
            identity = SemanticInferenceIdentity(
                providerId = "mediapipe-local",
                runtimeId = "mediapipe-genai",
                locality = InferenceLocality.LOCAL,
            ),
            imageInputValidator = { false },
            initialAvailability = SemanticInferenceAvailability.Unavailable(
                SemanticInferenceReasonCode.PLATFORM_IMAGE_INPUT_UNSUPPORTED,
            ),
        )
    }
    delegate<SemanticInferenceProvider>().to<GenAISemanticInferenceProvider>()
    delegate<SemanticInferenceAvailabilityController>().to<GenAISemanticInferenceProvider>()

    bindSingletonOf(::DatabaseDriverFactory)

    bindProvider { app.networkMonitor }
}
