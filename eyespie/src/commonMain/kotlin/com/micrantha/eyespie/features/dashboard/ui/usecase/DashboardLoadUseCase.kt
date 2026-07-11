package com.micrantha.eyespie.features.dashboard.ui.usecase

import com.micrantha.bluebell.domain.usecase.flowUseCase
import com.micrantha.eyespie.core.data.account.model.CurrentSession
import com.micrantha.eyespie.domain.entities.Location.Point
import com.micrantha.eyespie.domain.repository.ThingRepository
import com.micrantha.eyespie.features.dashboard.ui.DashboardAction.Loaded
import com.micrantha.eyespie.features.players.domain.entities.Player
import com.micrantha.eyespie.features.players.domain.repository.PlayerRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

interface DashboardLoadUseCase {
    operator fun invoke(): kotlinx.coroutines.flow.Flow<Result<Loaded>>
}

class DashboardLoadUseCaseImpl(
    private val thingsRepository: ThingRepository,
    private val playerRepository: PlayerRepository,
    private val currentSession: CurrentSession
) : DashboardLoadUseCase {
    override operator fun invoke() = flowUseCase {
        val player = currentSession.requirePlayer()
        val location = player.location?.point

        combine(
            flow = if (location != null) thingsRepository.nearby(location) else flowOf(Result.success(emptyList())),
            flow2 = if (location != null) playerRepository.nearby(location) else flowOf(Result.success(emptyList())),
            flow3 = playerRepository.players(),
        ) { nearbyThings, nearbyPlayers, friends ->
            Loaded(
                nearbyThings = nearbyThings.getOrDefault(emptyList()),
                nearbyPlayers = nearbyPlayers.getOrDefault(emptyList()),
                friends = friends.getOrDefault(emptyList())
            )
        }
    }
}
