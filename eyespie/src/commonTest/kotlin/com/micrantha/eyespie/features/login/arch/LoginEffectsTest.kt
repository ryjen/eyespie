package com.micrantha.eyespie.features.login.arch

import com.micrantha.bluebell.arch.FakeDispatcher
import com.micrantha.eyespie.app.usecase.LoadMainUseCase
import com.micrantha.eyespie.core.data.client.SupabaseConfigChecker
import com.micrantha.eyespie.core.ui.FakeScreenContext
import com.micrantha.eyespie.domain.repository.FakeAccountRepository
import com.micrantha.eyespie.features.login.entities.LoginAction.NotConfigured
import com.micrantha.eyespie.features.login.entities.LoginAction.OnLogin
import com.micrantha.eyespie.features.login.entities.LoginAction.OnLoginWithGoogle
import com.micrantha.eyespie.features.login.entities.LoginAction.OnRegister
import com.micrantha.eyespie.features.login.entities.LoginState
import com.micrantha.eyespie.features.register.ui.RegisterScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.kodein.di.DI
import org.kodein.di.bindProvider
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LoginEffectsTest {

    private val dispatcher = FakeDispatcher(CoroutineScope(UnconfinedTestDispatcher()))
    private val context = FakeScreenContext(
        dispatcher = dispatcher,
        di = DI { bindProvider { RegisterScreen() } }
    )
    private val accountRepository = FakeAccountRepository()

    private val fakeLoadMainUseCase = object : LoadMainUseCase {
        override suspend fun invoke() = Result.success(Unit)
    }

    private fun effects(supabaseConfigured: Boolean) = LoginEffects(
        context, accountRepository, fakeLoadMainUseCase,
        supabaseConfigChecker = object : SupabaseConfigChecker {
            override fun isSupabaseConfigured() = supabaseConfigured
        }
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `invoke OnLogin should dispatch NotConfigured when Supabase not configured`() = runTest {
        val state = LoginState(email = "user@example.com", password = "password123")

        effects(supabaseConfigured = false)(OnLogin, state)

        assertTrue(dispatcher.actions.any { it is NotConfigured })
    }

    @Test
    fun `invoke OnLoginWithGoogle should dispatch NotConfigured when Supabase not configured`() = runTest {
        val state = LoginState(email = "user@example.com", password = "password123")

        effects(supabaseConfigured = false)(OnLoginWithGoogle, state)

        assertTrue(dispatcher.actions.any { it is NotConfigured })
    }

    @Test
    fun `invoke OnLogin should skip when emailError is set`() = runTest {
        val stateWithErrors = LoginReducer().reduce(
            LoginState(email = "", password = "password123"),
            OnLogin
        )

        effects(supabaseConfigured = true)(OnLogin, stateWithErrors)

        assertTrue(dispatcher.actions.none { it is NotConfigured })
    }

    @Test
    fun `invoke OnLogin should skip when passwordError is set`() = runTest {
        val stateWithErrors = LoginReducer().reduce(
            LoginState(email = "user@example.com", password = ""),
            OnLogin
        )

        effects(supabaseConfigured = true)(OnLogin, stateWithErrors)

        assertTrue(dispatcher.actions.none { it is NotConfigured })
    }

    @Test
    fun `invoke OnRegister should navigate to RegisterScreen`() = runTest {
        val state = LoginState()

        effects(supabaseConfigured = true)(OnRegister, state)

        assertIs<RegisterScreen>(context.router.lastNavigatedTo)
    }
}
