package com.micrantha.eyespie.domain.usecase

import com.micrantha.bluebell.platform.FakeGenAI
import com.micrantha.bluebell.platform.Platform
import com.micrantha.eyespie.features.onboarding.entities.AiModel
import com.micrantha.eyespie.features.onboarding.data.FakeOnboardingRepository
import com.micrantha.eyespie.features.onboarding.usecase.LoadModelConfig
import kotlinx.coroutines.test.runTest
import okio.Path
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertTrue

class InitGenAIUseCaseTest {

    private val llm = FakeGenAI()
    private val onboardingRepository = FakeOnboardingRepository()
    private val loadModelConfig = object : LoadModelConfig {
        override fun invoke() = Result.success(mapOf(
            "test" to AiModel("url", "28e5ebabd9d8f6e237df63da2b503785093f0229241bc7021198f63c43b93269")
        ))
    }
    private val platform = object : Platform {
        override val name = "Fake"
        override val networkMonitor = object : com.micrantha.bluebell.platform.NetworkMonitor {
            override fun startMonitoring(onUpdate: (Boolean) -> Unit) = Unit
            override fun stopMonitoring() = Unit
        }
        override fun format(epochSeconds: Long, format: String, timeZone: String) = ""
        override val locale: com.micrantha.bluebell.platform.Locale get() = TODO()
        override fun asset(path: Path) = TODO()
        override fun checksum(path: Path) = ""
        override fun resource(path: Path) = TODO()
        override fun format(format: String, vararg args: Any) = ""
        override fun filesPath() = "/".toPath()
        override fun sharedFilesPath() = "/".toPath()
        override fun fileWrite(path: Path, data: ByteArray) = Unit
        override fun fileRead(path: Path) = byteArrayOf()
    }
    private val useCase = InitGenAIUseCase(llm, onboardingRepository, loadModelConfig, platform)

    @Test
    fun `invoke should succeed if genai disabled`() = runTest {
        onboardingRepository.hasGenAIValue = false

        val result = useCase()

        assertTrue(result.isSuccess)
    }

    @Test
    fun `invoke should succeed if genai enabled and model available`() = runTest {
        onboardingRepository.hasGenAIValue = true
        onboardingRepository.model = "test"
        
        val result = useCase()

        assertTrue(result.isSuccess)
    }
}
