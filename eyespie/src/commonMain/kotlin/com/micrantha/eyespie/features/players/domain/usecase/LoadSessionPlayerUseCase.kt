package com.micrantha.eyespie.features.players.domain.usecase

import com.micrantha.bluebell.ui.components.Router
import com.micrantha.bluebell.ui.screen.ScreenContext
import com.micrantha.bluebell.ui.screen.navigate
import com.micrantha.eyespie.core.data.account.model.CurrentSession
import com.micrantha.eyespie.domain.entities.Session
import com.micrantha.eyespie.features.dashboard.ui.DashboardScreen
import com.micrantha.eyespie.features.login.ui.LoginScreen
import com.micrantha.eyespie.features.players.domain.entities.Player
import com.micrantha.eyespie.features.players.domain.repository.PlayerRepository
import com.micrantha.eyespie.features.players.ui.create.NewPlayerScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class LoadSessionPlayerUseCase(
    private val playerRepository: PlayerRepository,
    private val currentSession: CurrentSession
) {
    operator fun invoke(session: Session): Flow<Result<Player?>> = flow {
        currentSession.update(session)
        emitAll(playerRepository.player(session.userId).onEach { res ->
            res.onSuccess { player ->
                currentSession.update(player)
            }
        }.map { res ->
            Result.success(res.getOrNull())
        })
    }
}
