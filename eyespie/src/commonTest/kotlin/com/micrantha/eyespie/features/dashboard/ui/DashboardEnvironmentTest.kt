package com.micrantha.eyespie.features.dashboard.ui

import com.micrantha.bluebell.arch.Action
import com.micrantha.bluebell.arch.Dispatcher
import com.micrantha.bluebell.i18n.repository.LocalizedRepository
import com.micrantha.bluebell.platform.FileSystem
import com.micrantha.bluebell.ui.components.Router
import com.micrantha.bluebell.ui.model.UiResult
import com.micrantha.bluebell.ui.screen.ScreenContext
import com.micrantha.eyespie.features.dashboard.ui.usecase.DashboardLoadUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import org.kodein.di.DI
import kotlin.test.Test
import kotlin.test.assertIs

class DashboardEnvironmentTest {

    private class FakeScreenContext : ScreenContext {
        override val i18n: LocalizedRepository get() = TODO()
        override val router: Router get() = TODO()
        override val dispatcher: Dispatcher get() = object : Dispatcher {
            override val dispatchScope: CoroutineScope get() = TODO()
            override fun dispatch(action: Action) = Unit
            override suspend fun send(action: Action) = Unit
        }
        override val fileSystem: FileSystem get() = TODO()
        override val di: DI get() = TODO()
    }

    private class FakeLoadUseCase : DashboardLoadUseCase {
        override fun invoke(): Flow<Result<DashboardAction.Loaded>> = TODO()
    }

    private val context = FakeScreenContext()
    private val useCase = FakeLoadUseCase()
    private val environment = DashboardEnvironment(context, useCase)

    @Test
    fun `reduce Load action should return Busy status`() {
        val state = DashboardState(status = UiResult.Default)
        val action = DashboardAction.Load

        val newState = environment.reduce(state, action)

        assertIs<UiResult.Busy>(newState.status)
    }

    @Test
    fun `reduce Loaded action should return Ready status`() {
        val state = DashboardState(status = UiResult.Busy())
        val action = DashboardAction.Loaded(
            nearbyThings = emptyList(),
            nearbyPlayers = emptyList(),
            friends = emptyList()
        )

        val newState = environment.reduce(state, action)

        assertIs<UiResult.Ready<Unit>>(newState.status)
    }
}
