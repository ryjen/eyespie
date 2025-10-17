package com.micrantha.eyespie.app.ui.usecase

import com.micrantha.bluebell.app.Log
import com.micrantha.bluebell.domain.usecase.InitGenAIUseCase
import com.micrantha.bluebell.ext.ResultPipeline
import com.micrantha.bluebell.ui.components.Router
import com.micrantha.bluebell.ui.screen.ScreenContext
import com.micrantha.bluebell.ui.screen.navigate
import com.micrantha.eyespie.domain.entities.Session
import com.micrantha.eyespie.domain.repository.AccountRepository
import com.micrantha.eyespie.features.login.ui.LoginScreen
import com.micrantha.eyespie.features.onboarding.data.OnboardingRepository
import com.micrantha.eyespie.features.onboarding.ui.OnboardingScreen
import com.micrantha.eyespie.features.players.domain.usecase.LoadSessionPlayerUseCase

class LoadMainUseCase(
    private val context: ScreenContext,
    private val accountRepository: AccountRepository,
    private val loadSessionPlayerUseCase: LoadSessionPlayerUseCase,
    private val initGenAIUseCase: InitGenAIUseCase,
    private val onboardingRepository: OnboardingRepository,
) {

    suspend operator fun invoke(): Result<Session> = try {
        ResultPipeline
            .fromResult(::onboarding)
            .then(::genai)
            .then(::account)
            .invoke(Unit)
    } catch (err: Throwable) {
        Log.e("main", err) { "unexpected error" }
        Result.failure(err)
    }

    private suspend fun onboarding(input: Unit): Result<Unit> {
        return if (onboardingRepository.hasRunOnce().not()) {
            context.navigate<OnboardingScreen>(Router.Options.Replace)
            Result.failure(IllegalStateException())
        } else {
            Result.success(Unit)
        }
    }

    private suspend fun genai(input: Unit): Result<Unit> {
        return if (onboardingRepository.hasGenAI()) {
            initGenAIUseCase().onFailure {
                context.navigate<OnboardingScreen>(Router.Options.Replace)
            }
        } else {
            Result.success(Unit)
        }
    }

    private suspend fun account(input: Unit): Result<Session> {
        return accountRepository.session().onFailure {
            context.navigate<LoginScreen>(Router.Options.Replace)
        }.onSuccess { session ->
            loadSessionPlayerUseCase.withNavigation(session)
        }
    }
}
