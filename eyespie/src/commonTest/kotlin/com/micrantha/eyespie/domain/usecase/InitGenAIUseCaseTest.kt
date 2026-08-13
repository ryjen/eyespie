package com.micrantha.eyespie.domain.usecase

import com.micrantha.bluebell.platform.GenAI
import com.micrantha.bluebell.platform.GenAIConfig
import com.micrantha.bluebell.platform.GenAIRequest
import com.micrantha.bluebell.platform.NetworkMonitor
import com.micrantha.bluebell.platform.Platform
import com.micrantha.eyespie.features.onboarding.data.FakeOnboardingRepository
import com.micrantha.eyespie.features.onboarding.entities.AiModel
import com.micrantha.eyespie.features.onboarding.usecase.LoadModelConfig
import com.micrantha.eyespie.features.onboarding.usecase.ModelIntegrityException
import com.micrantha.eyespie.features.onboarding.usecase.ModelIntegrityFailure
import com.micrantha.eyespie.features.onboarding.usecase.ModelIntegrityVerifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import okio.Path
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class InitGenAIUseCaseTest {

    private val llm = TrackingGenAI()
    private val onboardingRepository = FakeOnboardingRepository()
    private var configuredModel = model()
    private val loadModelConfig = object : LoadModelConfig {
        override fun invoke() = Result.success(mapOf("test" to configuredModel))
    }
    private val platform = object : Platform {
        override val name = "Fake"
        override val networkMonitor = object : NetworkMonitor {
            override fun startMonitoring(onUpdate: (Boolean) -> Unit) = Unit
            override fun stopMonitoring() = Unit
        }
        override fun format(epochSeconds: Long, format: String, timeZone: String) = ""
        override val locale: com.micrantha.bluebell.platform.Locale get() = TODO()
        override fun asset(path: Path) = TODO()
        override fun checksum(path: Path): String? = null
        override fun resource(path: Path) = TODO()
        override fun format(format: String, vararg args: Any) = ""
        override fun filesPath() = "/files".toPath()
        override fun sharedFilesPath() = "/shared".toPath()
        override fun fileWrite(path: Path, data: ByteArray) = Unit
        override fun fileRead(path: Path) = byteArrayOf()
    }
    private val fileSystem = FakeFileSystem()
    private val verifier = ModelIntegrityVerifier(fileSystem)
    private val useCase = InitGenAIUseCase(
        llm,
        onboardingRepository,
        loadModelConfig,
        platform,
        verifier,
    )

    @Test
    fun `invoke should succeed if genai disabled`() = runTest {
        onboardingRepository.hasGenAIValue = false

        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(0, llm.initializeCalls)
    }

    @Test
    fun `invoke should initialize verified model bytes`() = runTest {
        onboardingRepository.hasGenAIValue = true
        onboardingRepository.model = "test"
        writeConfiguredModel()

        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(1, llm.initializeCalls)
        fileSystem.checkNoOpenFiles()
    }

    @Test
    fun `invoke should reject checksum mismatch before initialization`() = runTest {
        onboardingRepository.hasGenAIValue = true
        onboardingRepository.model = "test"
        configuredModel = model(checksum = "0".repeat(64))
        writeConfiguredModel()

        val result = useCase()

        assertIntegrityFailure(result, ModelIntegrityFailure.ChecksumMismatch)
        assertEquals(0, llm.initializeCalls)
    }

    @Test
    fun `invoke should reject missing model before initialization`() = runTest {
        onboardingRepository.hasGenAIValue = true
        onboardingRepository.model = "test"

        val result = useCase()

        assertIntegrityFailure(result, ModelIntegrityFailure.ModelUnavailable)
        assertEquals(0, llm.initializeCalls)
    }

    @Test
    fun `invoke should reject missing checksum before initialization`() = runTest {
        onboardingRepository.hasGenAIValue = true
        onboardingRepository.model = "test"
        configuredModel = model(checksum = null)

        val result = useCase()

        assertIntegrityFailure(result, ModelIntegrityFailure.MissingExpectedChecksum)
        assertEquals(0, llm.initializeCalls)
    }

    private fun writeConfiguredModel() {
        val path = platform.sharedFilesPath().resolve("${configuredModel.fileName()}.litertlm")
        fileSystem.createDirectories(path.parent!!)
        fileSystem.write(path) { writeUtf8(MODEL_CONTENT) }
    }

    private fun assertIntegrityFailure(result: Result<Unit>, failure: ModelIntegrityFailure) {
        val error = assertIs<ModelIntegrityException>(result.exceptionOrNull())
        assertEquals(failure, error.failure)
        fileSystem.checkNoOpenFiles()
    }

    private class TrackingGenAI : GenAI {
        var initializeCalls = 0

        override fun initialize(config: GenAIConfig): Result<Unit> {
            initializeCalls += 1
            return Result.success(Unit)
        }

        override fun newSession(config: GenAIConfig.Session) = Result.success(Unit)
        override fun generate(request: GenAIRequest) = Result.failure<String>(UnsupportedOperationException())
        override fun generateFlow(request: GenAIRequest): Flow<String> = emptyFlow()
        override fun close() = Unit
        override fun cancel() = Unit
    }

    private companion object {
        const val MODEL_URL = "https://example.invalid/test-model.litertlm"
        const val MODEL_CONTENT = "test model payload"
        const val MODEL_DIGEST = "bae77ae8633e61e7906d62148fecbf0f322507fe9b145afb5e3081af6b0e8b88"

        fun model(checksum: String? = MODEL_DIGEST) = AiModel(
            url = MODEL_URL,
            checksum = checksum,
        )
    }
}
