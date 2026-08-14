package com.micrantha.eyespie.domain.usecase

import com.micrantha.bluebell.platform.GenAI
import com.micrantha.bluebell.platform.GenAIConfig
import com.micrantha.bluebell.platform.GenAIRequest
import com.micrantha.bluebell.platform.NetworkMonitor
import com.micrantha.bluebell.platform.Platform
import com.micrantha.eyespie.domain.ai.InferenceLocality
import com.micrantha.eyespie.domain.ai.SemanticInferenceAvailability
import com.micrantha.eyespie.domain.ai.SemanticInferenceAvailabilityController
import com.micrantha.eyespie.domain.ai.SemanticInferenceCapabilities
import com.micrantha.eyespie.domain.ai.SemanticInferenceDiagnosticCode
import com.micrantha.eyespie.domain.ai.SemanticInferenceIdentity
import com.micrantha.eyespie.domain.ai.SemanticInferenceProvider
import com.micrantha.eyespie.domain.ai.SemanticInferenceReasonCode
import com.micrantha.eyespie.domain.ai.SemanticInferenceRequest
import com.micrantha.eyespie.features.onboarding.data.FakeOnboardingRepository
import com.micrantha.eyespie.features.onboarding.entities.AiModel
import com.micrantha.eyespie.features.onboarding.usecase.LoadModelConfig
import com.micrantha.eyespie.features.onboarding.usecase.ModelIntegrityException
import com.micrantha.eyespie.features.onboarding.usecase.ModelIntegrityFailure
import com.micrantha.eyespie.features.onboarding.usecase.ModelIntegrityVerifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val provider = TrackingProvider()
    private val useCase = InitGenAIUseCase(
        llm,
        onboardingRepository,
        loadModelConfig,
        platform,
        verifier,
        provider,
        provider,
    )

    @Test
    fun `invoke should leave provider not configured if genai disabled`() = runTest {
        onboardingRepository.hasGenAIValue = false

        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(0, llm.initializeCalls)
        assertIs<SemanticInferenceAvailability.NotConfigured>(provider.availability.value)
    }

    @Test
    fun `invoke should initialize verified model validate session and publish capabilities`() = runTest {
        onboardingRepository.hasGenAIValue = true
        onboardingRepository.model = "test"
        writeConfiguredModel()

        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(1, llm.initializeCalls)
        assertEquals(1, llm.newSessionCalls)
        val available = assertIs<SemanticInferenceAvailability.Available>(provider.availability.value)
        assertTrue(available.capabilities.imageInput)
        assertTrue(available.capabilities.cancellation)
        assertEquals(1024, available.capabilities.maxContextTokens)
        fileSystem.checkNoOpenFiles()
    }

    @Test
    fun `invoke should reject checksum mismatch before initialization and mark provider failed`() = runTest {
        onboardingRepository.hasGenAIValue = true
        onboardingRepository.model = "test"
        configuredModel = model(checksum = "0".repeat(64))
        writeConfiguredModel()

        val result = useCase()

        assertIntegrityFailure(result, ModelIntegrityFailure.ChecksumMismatch)
        assertEquals(0, llm.initializeCalls)
        assertEquals(
            SemanticInferenceDiagnosticCode.MODEL_INTEGRITY_FAILED,
            assertIs<SemanticInferenceAvailability.Failed>(provider.availability.value).diagnosticCode,
        )
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

    @Test
    fun `platform unsupported provider skips runtime initialization`() = runTest {
        provider.availability.value = SemanticInferenceAvailability.Unavailable(
            SemanticInferenceReasonCode.PLATFORM_IMAGE_INPUT_UNSUPPORTED,
        )
        onboardingRepository.hasGenAIValue = true
        onboardingRepository.model = "test"

        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(0, llm.initializeCalls)
        assertEquals(0, llm.newSessionCalls)
        assertEquals(
            SemanticInferenceReasonCode.PLATFORM_IMAGE_INPUT_UNSUPPORTED,
            assertIs<SemanticInferenceAvailability.Unavailable>(provider.availability.value).reasonCode,
        )
    }

    @Test
    fun `runtime initialization failure marks provider failed`() = runTest {
        onboardingRepository.hasGenAIValue = true
        onboardingRepository.model = "test"
        writeConfiguredModel()
        llm.initializeResult = Result.failure(IllegalStateException("runtime failed"))

        val result = useCase()

        assertTrue(result.isFailure)
        assertEquals(
            SemanticInferenceDiagnosticCode.RUNTIME_INITIALIZATION_FAILED,
            assertIs<SemanticInferenceAvailability.Failed>(provider.availability.value).diagnosticCode,
        )
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
        var newSessionCalls = 0
        var initializeResult: Result<Unit> = Result.success(Unit)

        override fun initialize(config: GenAIConfig): Result<Unit> {
            initializeCalls += 1
            return initializeResult
        }

        override fun newSession(config: GenAIConfig.Session): Result<Unit> {
            newSessionCalls += 1
            return Result.success(Unit)
        }

        override fun generate(request: GenAIRequest) = Result.failure<String>(UnsupportedOperationException())
        override fun generateFlow(request: GenAIRequest): Flow<String> = emptyFlow()
        override fun close() = Unit
        override fun cancel() = Unit
    }

    private class TrackingProvider : SemanticInferenceProvider, SemanticInferenceAvailabilityController {
        override val identity = SemanticInferenceIdentity(
            providerId = "test-local",
            runtimeId = "test-runtime",
            locality = InferenceLocality.LOCAL,
        )
        override val availability = MutableStateFlow<SemanticInferenceAvailability>(
            SemanticInferenceAvailability.NotConfigured,
        )

        override suspend fun generate(request: SemanticInferenceRequest) =
            Result.failure<String>(UnsupportedOperationException())
        override fun generateFlow(request: SemanticInferenceRequest): Flow<String> = emptyFlow()
        override fun cancel() = Unit
        override suspend fun close() = Unit
        override suspend fun markNotConfigured() {
            availability.value = SemanticInferenceAvailability.NotConfigured
        }
        override suspend fun markInitializing() {
            availability.value = SemanticInferenceAvailability.Initializing
        }
        override suspend fun markAvailable(capabilities: SemanticInferenceCapabilities) {
            availability.value = SemanticInferenceAvailability.Available(capabilities)
        }
        override suspend fun markUnavailable(reasonCode: String) {
            availability.value = SemanticInferenceAvailability.Unavailable(reasonCode)
        }
        override suspend fun markFailed(diagnosticCode: String) {
            availability.value = SemanticInferenceAvailability.Failed(diagnosticCode)
        }
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
