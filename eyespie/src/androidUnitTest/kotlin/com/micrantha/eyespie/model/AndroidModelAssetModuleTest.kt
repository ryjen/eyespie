package com.micrantha.eyespie.model

import android.content.Context
import com.google.android.play.core.assetpacks.AssetPackManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.kodein.di.DI
import org.kodein.di.direct
import org.kodein.di.instance
import kotlin.test.Test
import kotlin.test.assertIs

class AndroidModelAssetModuleTest {
    @Test
    fun resolvesPlayAssetDeliveryRepositoryBehindSharedContract() {
        val context = mockk<Context>(relaxed = true) {
            every { applicationContext } returns this
        }
        val assetPackManager = mockk<AssetPackManager>(relaxed = true) {
            every { getPackLocation(any()) } returns null
        }
        val dependencies = DI {
            import(
                androidModelAssetModule(
                    context = context,
                    assetPackManager = assetPackManager,
                    smokeChecker = ModelRuntimeSmokeChecker { RuntimeSmokeCheckResult.Passed },
                ),
            )
        }

        val repository = assertIs<PlayAssetDeliveryModelRepository>(
            dependencies.direct.instance<ModelAssetRepository>(),
        )

        verify(exactly = 1) { assetPackManager.registerListener(any()) }
        repository.close()
    }
}
