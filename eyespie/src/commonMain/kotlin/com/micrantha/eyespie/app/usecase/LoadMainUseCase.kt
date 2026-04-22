package com.micrantha.eyespie.app.usecase

import com.micrantha.bluebell.observability.logger
import com.micrantha.bluebell.ext.then
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

class LoadMainUseCase(
    private val context: ScreenContext,
    private val accountRepository: AccountRepository,
    private val loadSessionPlayerUseCase: LoadSessionPlayerUseCase,
    private val onboardingRepository: OnboardingRepository,
    private val initGenAIUseCase: InitGenAIUseCase
) {
    private val log by logger()

    suspend operator fun invoke(): Result<Unit> = try {
        session()
            .then(::account)
            .then(::newPlayer)
            .then(::onboarding)
            .then(::initGenAI)
            .then(::dashboard)
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
            Result.failure(IllegalStateException())
        } else {
            Result.success(input)
        }
    }

    private suspend fun session(): Result<Session> {
        return accountRepository.session().onFailure {
            log.debug { "no existing session" }
            context.navigate<LoginScreen>(Router.Options.Replace)
        }
    }

    private suspend fun account(session: Session): Result<Player?> {
        return loadSessionPlayerUseCase(session).onFailure {
            log.debug { "not logged in"}
            context.navigate<LoginScreen>(Router.Options.Replace)
        }
    }

    private fun newPlayer(player: Player?): Result<Player> {
        if (player == null) {
            log.debug { "new player" }
            context.navigate<NewPlayerScreen>(Router.Options.Replace)
            return Result.failure(IllegalStateException())
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
        }
    }
}
