package com.micrantha.eyespie.app.usecase

import com.micrantha.bluebell.ext.then
import com.micrantha.bluebell.observability.logger
import com.micrantha.bluebell.ui.components.Router
import com.micrantha.bluebell.ui.screen.ScreenContext
import com.micrantha.bluebell.ui.screen.navigate
import com.micrantha.eyespie.domain.entities.Session
import com.micrantha.eyespie.domain.repository.AccountRepository
import com.micrantha.eyespie.domain.usecase.InitGenAIUseCase
import com.micrantha.eyespie.features.dashboard.ui.DashboardScreen
import com.micrantha.eyespie.features.login.ui.LoginScreen
import com.micrantha.eyespie.features.onboarding.data.OnboardingRepository
import com.micrantha.eyespie.features.onboarding.ui.OnboardingScreen
import com.micrantha.eyespie.features.onboarding.ui.genai.GenAIDownloadScreen
import com.micrantha.eyespie.features.players.domain.entities.Player
import com.micrantha.eyespie.features.players.domain.usecase.LoadSessionPlayerUseCase
import com.micrantha.eyespie.features.players.ui.create.NewPlayerScreen
import com.micrantha.eyespie.features.scan.data.CaptureSyncRepository
import kotlinx.coroutines.flow.first

interface LoadMainUseCase {
    suspend operator fun invoke(): Result<Unit>
}

class LoadMainUseCaseImpl(
    private val context: ScreenContext,
    private val accountRepository: AccountRepository,
    private val loadSessionPlayerUseCase: LoadSessionPlayerUseCase,
    private val onboardingRepository: OnboardingRepository,
    private val initGenAIUseCase: InitGenAIUseCase,
    private val captureSyncRepository: CaptureSyncRepository
) : LoadMainUseCase {
    private val log by logger()

    override suspend operator fun invoke(): Result<Unit> = try {
        session()
            .then { session -> account(session) }
            .then { player -> newPlayer(player) }
            .then { player -> onboarding(player) }
            .then { player -> initGenAI(player) }
            .then { dashboard(Unit) }
            .recover { 
                if (it is HandledException) Result.success(Unit) else Result.failure(it)
            }.map { }
    } catch (err: Throwable) {
        log.error(err) { "unexpected error" }
        Result.failure(err)
    }

    private suspend fun onboarding(input: Player): Result<Player> {
        return if (
            onboardingRepository.hasRunOnce().not() ||
            (onboardingRepository.hasGenAI() && onboardingRepository.genAiModel().isNullOrBlank())
        ) {
            context.navigate<OnboardingScreen>(Router.Options.Replace)
            log.debug { "onboarding new user" }
            Result.failure(HandledException("onboarding required"))
        } else {
            Result.success(input)
        }
    }

    private suspend fun session(): Result<Session> {
        return accountRepository.session().onFailure {
            log.debug { "no existing session" }
            context.navigate<LoginScreen>(Router.Options.Replace)
        }.mapCatching { it }.recover { throw HandledException("no session", it) }
    }

    private suspend fun account(session: Session): Result<Player?> {
        return loadSessionPlayerUseCase(session).first().onFailure {
            log.debug { "not logged in" }
            context.navigate<LoginScreen>(Router.Options.Replace)
        }.recover { throw HandledException("account error", it) }
    }

    private fun newPlayer(player: Player?): Result<Player> {
        if (player == null) {
            log.debug { "new player" }
            context.navigate<NewPlayerScreen>(Router.Options.Replace)
            return Result.failure(HandledException("new player required"))
        }
        return Result.success(player)
    }

    private fun dashboard(input: Unit): Result<Unit> {
        context.navigate<DashboardScreen>(Router.Options.Replace)
        return Result.success(Unit)
    }

    private suspend fun initGenAI(input: Player): Result<Unit> {
        return initGenAIUseCase().onFailure {
            log.debug { "ai model not available" }
            context.navigate<GenAIDownloadScreen>(Router.Options.Replace)
        }.recover { throw HandledException("genai error", it) }
    }

    private class HandledException(message: String, cause: Throwable? = null) : Exception(message, cause)
}
