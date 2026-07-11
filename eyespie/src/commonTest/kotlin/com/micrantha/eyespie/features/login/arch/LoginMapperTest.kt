package com.micrantha.eyespie.features.login.arch

import com.micrantha.bluebell.ui.model.UiResult
import com.micrantha.eyespie.features.login.entities.LoginState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class LoginMapperTest {

    private val mapper = LoginMapper()

    @Test
    fun `map should pass email and password strings`() {
        val state = LoginState(email = "user@example.com", password = "secret")

        val uiState = mapper.map(state)

        assertEquals("user@example.com", uiState.email)
        assertEquals("secret", uiState.password)
    }

    @Test
    fun `map should pass emailError when set via OnLogin`() {
        val stateWithErrors = LoginReducer().reduce(
            LoginState(email = "", password = "password123"),
            com.micrantha.eyespie.features.login.entities.LoginAction.OnLogin
        )

        val uiState = mapper.map(stateWithErrors)

        assertNotNull(uiState.emailError)
    }

    @Test
    fun `map should pass passwordError when set via OnLogin`() {
        val stateWithErrors = LoginReducer().reduce(
            LoginState(email = "user@example.com", password = ""),
            com.micrantha.eyespie.features.login.entities.LoginAction.OnLogin
        )

        val uiState = mapper.map(stateWithErrors)

        assertNotNull(uiState.passwordError)
    }

    @Test
    fun `map should pass null errors when not set`() {
        val state = LoginState(email = "user@example.com", password = "secret")

        val uiState = mapper.map(state)

        assertNull(uiState.emailError)
        assertNull(uiState.passwordError)
    }

    @Test
    fun `map should pass status`() {
        val state = LoginState(status = UiResult.Busy())

        val uiState = mapper.map(state)

        assert(uiState.status is UiResult.Busy)
    }

    @Test
    fun `map should default isEmailMasked to true when email is non-blank`() {
        val state = LoginState(email = "user@example.com", isEmailMasked = null)

        val uiState = mapper.map(state)

        assertEquals(true, uiState.isEmailMasked)
    }

    @Test
    fun `map should default isEmailMasked to false when email is blank`() {
        val state = LoginState(email = "", isEmailMasked = null)

        val uiState = mapper.map(state)

        assertEquals(false, uiState.isEmailMasked)
    }

    @Test
    fun `map should respect explicit isEmailMasked value`() {
        val state = LoginState(email = "user@example.com", isEmailMasked = false)

        val uiState = mapper.map(state)

        assertEquals(false, uiState.isEmailMasked)
    }

    @Test
    fun `map should pass isPasswordMasked`() {
        val state = LoginState(isPasswordMasked = false)

        val uiState = mapper.map(state)

        assertEquals(false, uiState.isPasswordMasked)
    }
}
