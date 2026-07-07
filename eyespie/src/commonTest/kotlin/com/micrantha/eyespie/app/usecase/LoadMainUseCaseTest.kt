package com.micrantha.eyespie.app.usecase

import com.micrantha.bluebell.platform.FakeGenAI
import com.micrantha.bluebell.platform.Platform
import com.micrantha.eyespie.core.data.account.model.CurrentSession
import com.micrantha.eyespie.core.ui.FakeScreenContext
import com.micrantha.eyespie.domain.entities.Session
import com.micrantha.eyespie.domain.repository.FakeAccountRepository
import com.micrantha.eyespie.domain.usecase.InitGenAIUseCase
import com.micrantha.eyespie.features.dashboard.ui.DashboardScreen
import com.micrantha.eyespie.features.login.ui.LoginScreen
import com.micrantha.eyespie.features.onboarding.data.FakeOnboardingRepository
import com.micrantha.eyespie.features.onboarding.entities.AiModel
import com.micrantha.eyespie.features.onboarding.ui.OnboardingScreen
import com.micrantha.eyespie.features.onboarding.ui.genai.GenAIDownloadScreen
import com.micrantha.eyespie.features.onboarding.usecase.LoadModelConfig
import com.micrantha.eyespie.features.players.domain.entities.Player
import com.micrantha.eyespie.features.players.domain.repository.FakePlayerRepository
import com.micrantha.eyespie.features.players.domain.usecase.LoadSessionPlayerUseCase
import com.micrantha.eyespie.features.players.ui.create.NewPlayerScreen
import com.micrantha.eyespie.features.scan.data.FakeCaptureSyncRepository
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import org.kodein.di.DI
import org.kodein.di.bindProvider
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class LoadMainUseCaseTest {

    private val di = DI {
        bindProvider { LoginScreen() }
        bindProvider { NewPlayerScreen() }
        bindProvider { OnboardingScreen() }
        bindProvider { DashboardScreen() }
        bindProvider { GenAIDownloadScreen() }
    }

    private val context = FakeScreenContext(di)
    private val accountRepository = FakeAccountRepository()
    private val playerRepository = FakePlayerRepository()
    private val currentSession = CurrentSession
    private val loadSessionPlayerUseCase = LoadSessionPlayerUseCase(playerRepository, currentSession)
    private val onboardingRepository = FakeOnboardingRepository()
    private val llm = FakeGenAI()
    private val loadModelConfig = object : LoadModelConfig {
        override fun invoke() = Result.success(mapOf(
            "test" to AiModel("url", "c643ac136b0e526f578ce56c2253b5005c4422b5640d61f363ad1802253d86cf")
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
        override fun asset(path: okio.Path) = TODO()
        override fun checksum(path: okio.Path) = ""
        override fun resource(path: okio.Path) = TODO()
        override fun format(format: String, vararg args: Any) = ""
        override fun filesPath() = "/".toPath()
        override fun sharedFilesPath() = "/".toPath()
        override fun fileWrite(path: okio.Path, data: ByteArray) = Unit
        override fun fileRead(path: okio.Path) = byteArrayOf()
    }
    private val initGenAIUseCase = InitGenAIUseCase(llm, onboardingRepository, loadModelConfig, platform)
    private val captureSyncRepository = FakeCaptureSyncRepository()

    private val useCase = LoadMainUseCase(
        context,
        accountRepository,
        loadSessionPlayerUseCase,
        onboardingRepository,
        initGenAIUseCase,
        captureSyncRepository
    )

    @Test
    fun `when no session should navigate to LoginScreen`() = runTest {
        accountRepository.sessionResult = Result.failure(Exception("No session"))

        useCase()

        assertIs<LoginScreen>(context.router.lastNavigatedTo)
    }

    @Test
    fun `when session exists but player missing should navigate to NewPlayerScreen`() = runTest {
        accountRepository.sessionResult = Result.success(Session(id = "s", accessToken = "a", refreshToken = "r", userId = "u"))
        playerRepository.playerResult = Result.failure(Exception("Not found"))

        useCase()

        assertIs<NewPlayerScreen>(context.router.lastNavigatedTo)
    }

    @Test
    fun `when everything valid should navigate to DashboardScreen`() = runTest {
        accountRepository.sessionResult = Result.success(Session(id = "s", accessToken = "a", refreshToken = "r", userId = "u"))
        val player = Player("p1", Instant.parse("2023-01-01T00:00:00Z"), Player.Name("f", "l", "n"), "e", Player.Score(0))
        playerRepository.playerResult = Result.success(player)
        onboardingRepository.runOnce = true

        useCase()

        assertIs<DashboardScreen>(context.router.lastNavigatedTo)
    }
}
