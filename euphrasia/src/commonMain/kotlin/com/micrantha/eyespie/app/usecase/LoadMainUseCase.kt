package com.micrantha.eyespie.app.usecase

import com.micrantha.bluebell.app.Log
import com.micrantha.bluebell.ext.then
import com.micrantha.bluebell.ui.components.Router
import com.micrantha.bluebell.ui.screen.ScreenContext
import com.micrantha.bluebell.ui.screen.navigate
import com.micrantha.eyespie.domain.entities.Session
import com.micrantha.eyespie.domain.repository.AccountRepository
import com.micrantha.eyespie.features.dashboard.ui.DashboardScreen
import com.micrantha.eyespie.features.login.ui.LoginScreen
import com.micrantha.eyespie.features.onboarding.data.OnboardingRepository
import com.micrantha.eyespie.features.onboarding.ui.OnboardingScreen
import com.micrantha.eyespie.features.players.domain.entities.Player
import com.micrantha.eyespie.features.players.domain.usecase.LoadSessionPlayerUseCase
import com.micrantha.eyespie.features.players.ui.create.NewPlayerScreen
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class LoadMainUseCase(
    private val context: ScreenContext,
    private val accountRepository: AccountRepository,
    private val loadSessionPlayerUseCase: LoadSessionPlayerUseCase,
    private val onboardingRepository: OnboardingRepository,
) {

    suspend operator fun invoke(): Result<Unit> = try {
        session()
            .then(::account)
            .then(::newPlayer)
            .then(::onboarding)
            .then(::dashboard)
    } catch (err: Throwable) {
        Log.e("main", err) { "unexpected error" }
        Result.failure(err)
    }

    private suspend fun onboarding(input: Player): Result<Player> {
        return if (
            onboardingRepository.hasRunOnce().not() ||
            (onboardingRepository.hasGenAI() && onboardingRepository.modelFile().isNullOrBlank())
            ) {
            context.navigate<OnboardingScreen>(Router.Options.Replace)
            Result.failure(IllegalStateException())
        } else {
            Result.success(input)
        }
    }

    private suspend fun session(): Result<Session> {
        return accountRepository.session().onFailure {
            context.navigate<LoginScreen>(Router.Options.Replace)
        }
    }

    private suspend fun account(session: Session): Result<Player?> {
        return loadSessionPlayerUseCase(session).onFailure {
            context.navigate<LoginScreen>(Router.Options.Replace)
        }
    }

    private fun newPlayer(player: Player?): Result<Player> {
        if (player == null) {
            context.navigate<NewPlayerScreen>(Router.Options.Replace)
            return Result.failure(IllegalStateException())
        }
        return Result.success(player)
    }

    private fun dashboard(input: Player): Result<Unit> {
        context.navigate<DashboardScreen>(Router.Options.Replace)
        return Result.success(Unit)
    }
}
