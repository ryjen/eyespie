package com.micrantha.eyespie

import android.content.Context
import com.google.android.play.core.assetpacks.AssetPackManagerFactory
import com.micrantha.bluebell.get
import com.micrantha.bluebell.platform.AndroidNetworkMonitor
import com.micrantha.bluebell.platform.BackgroundDownloader
import com.micrantha.bluebell.platform.FileSystem
import com.micrantha.bluebell.platform.GenAI
import com.micrantha.bluebell.platform.Platform
import com.micrantha.bluebell.platform.PlatformGenAI
import com.micrantha.bluebell.platform.PlatformImpl
import com.micrantha.eyespie.core.data.ai.GenAISemanticInferenceProvider
import com.micrantha.eyespie.core.data.db.DatabaseDriverFactory
import com.micrantha.eyespie.domain.ai.InferenceLocality
import com.micrantha.eyespie.domain.ai.SemanticInferenceAvailabilityController
import com.micrantha.eyespie.domain.ai.SemanticInferenceIdentity
import com.micrantha.eyespie.domain.ai.SemanticInferenceProvider
import com.micrantha.eyespie.domain.ai.SemanticInferenceProviderSetup
import com.micrantha.eyespie.model.androidModelAssetModule
import com.micrantha.eyespie.platform.scan.LoadCameraImageUseCase
import com.micrantha.eyespie.platform.scan.LoadCameraImageUseCaseImpl
import java.io.File
import org.kodein.di.DI
import org.kodein.di.bindFactory
import org.kodein.di.bindInstance
import org.kodein.di.bindProviderOf
import org.kodein.di.bindSingleton
import org.kodein.di.bindSingletonOf
import org.kodein.di.delegate
import org.kodein.di.instance

fun androidDependencies(
    context: Context,
) = DI {
    val applicationContext = context.applicationContext

    bindInstance { applicationContext }
    importOnce(
        androidModelAssetModule(
            context = applicationContext,
            assetPackManager = AssetPackManagerFactory.getInstance(applicationContext),
        ),
    )

    bindSingletonOf(::PlatformImpl)
    delegate<Platform>().to<PlatformImpl>()
    delegate<FileSystem>().to<PlatformImpl>()

    bindSingleton { DatabaseDriverFactory(instance()) }

    bindProviderOf(::AndroidNetworkMonitor)

    bindSingleton { LoadCameraImageUseCaseImpl(instance()) }
    delegate<LoadCameraImageUseCase>().to<LoadCameraImageUseCaseImpl>()

    bindSingleton { PlatformGenAI(instance()) }
    delegate<GenAI>().to<PlatformGenAI>()

    bindSingleton {
        GenAISemanticInferenceProvider(
            genAI = instance(),
            identity = SemanticInferenceIdentity(
                providerId = "mediapipe-local",
                runtimeId = "mediapipe-genai",
                locality = InferenceLocality.LOCAL,
            ),
            imageInputValidator = { path -> File(path.toString()).isFile },
        )
    }
    delegate<SemanticInferenceProvider>().to<GenAISemanticInferenceProvider>()
    delegate<SemanticInferenceProviderSetup>().to<GenAISemanticInferenceProvider>()
    delegate<SemanticInferenceAvailabilityController>().to<GenAISemanticInferenceProvider>()

    bindFactory { namespace: String ->
        BackgroundDownloader(
            get(), get(), namespace
        )
    }
}
