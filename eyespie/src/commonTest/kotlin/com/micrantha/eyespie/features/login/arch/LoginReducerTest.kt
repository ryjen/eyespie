package com.micrantha.eyespie.features.login.arch

import com.micrantha.bluebell.ui.model.UiResult
import com.micrantha.eyespie.domain.entities.Session
import com.micrantha.eyespie.features.login.entities.LoginAction.ChangedEmail
import com.micrantha.eyespie.features.login.entities.LoginAction.ChangedPassword
import com.micrantha.eyespie.features.login.entities.LoginAction.NotConfigured
import com.micrantha.eyespie.features.login.entities.LoginAction.OnError
import com.micrantha.eyespie.features.login.entities.LoginAction.OnLogin
import com.micrantha.eyespie.features.login.entities.LoginAction.OnLoginWithGoogle
import com.micrantha.eyespie.features.login.entities.LoginAction.OnSuccess
import com.micrantha.eyespie.features.login.entities.LoginAction.ResetStatus
import com.micrantha.eyespie.features.login.entities.LoginAction.ToggleEmailMask
import com.micrantha.eyespie.features.login.entities.LoginAction.TogglePasswordMask
import com.micrantha.eyespie.features.login.entities.LoginState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertIs

class LoginReducerTest {

    private val reducer = LoginReducer()

    @Test
    fun `reduce OnLogin with empty email should set emailError`() {
        val state = LoginState(email = "", password = "password123")

        val newState = reducer.reduce(state, OnLogin)

        assertNotNull(newState.emailError)
        assertNull(newState.passwordError)
        assertIs<UiResult.Default>(newState.status)
    }

    @Test
    fun `reduce OnLogin with empty password should set passwordError`() {
        val state = LoginState(email = "user@example.com", password = "")

        val newState = reducer.reduce(state, OnLogin)

        assertNull(newState.emailError)
        assertNotNull(newState.passwordError)
        assertIs<UiResult.Default>(newState.status)
    }

    @Test
    fun `reduce OnLogin with both empty should set both errors`() {
        val state = LoginState(email = "", password = "")

        val newState = reducer.reduce(state, OnLogin)

        assertNotNull(newState.emailError)
        assertNotNull(newState.passwordError)
        assertIs<UiResult.Default>(newState.status)
    }

    @Test
    fun `reduce OnLogin with valid fields should set Busy status`() {
        val state = LoginState(email = "user@example.com", password = "password123")

        val newState = reducer.reduce(state, OnLogin)

        assertNull(newState.emailError)
        assertNull(newState.passwordError)
        assertIs<UiResult.Busy>(newState.status)
    }

    @Test
    fun `reduce ChangedEmail should clear emailError`() {
        val stateWithErrors = reducer.reduce(
            LoginState(email = "", password = "password123"),
            OnLogin
        )
        assertNotNull(stateWithErrors.emailError)

        val newState = reducer.reduce(stateWithErrors, ChangedEmail("new@example.com"))

        assertEquals("new@example.com", newState.email)
        assertNull(newState.emailError)
    }

    @Test
    fun `reduce ChangedPassword should clear passwordError`() {
        val stateWithErrors = reducer.reduce(
            LoginState(email = "user@example.com", password = ""),
            OnLogin
        )
        assertNotNull(stateWithErrors.passwordError)

        val newState = reducer.reduce(stateWithErrors, ChangedPassword("newpassword"))

        assertEquals("newpassword", newState.password)
        assertNull(newState.passwordError)
    }

    @Test
    fun `reduce OnLoginWithGoogle should set Busy status`() {
        val state = LoginState(email = "user@example.com", password = "password123")

        val newState = reducer.reduce(state, OnLoginWithGoogle)

        assertIs<UiResult.Busy>(newState.status)
    }

    @Test
    fun `reduce OnError should set Failure status`() {
        val state = LoginState(status = UiResult.Busy())

        val newState = reducer.reduce(state, OnError(Exception("test")))

        assertIs<UiResult.Failure>(newState.status)
    }

    @Test
    fun `reduce NotConfigured should set Failure status`() {
        val state = LoginState(status = UiResult.Busy())

        val newState = reducer.reduce(state, NotConfigured)

        assertIs<UiResult.Failure>(newState.status)
    }

    @Test
    fun `reduce OnSuccess should set Default status`() {
        val state = LoginState(status = UiResult.Busy())
        val session = Session(id = "1", accessToken = "token", refreshToken = "refresh", userId = "user-id")

        val newState = reducer.reduce(state, OnSuccess(session))

        assertIs<UiResult.Default>(newState.status)
    }

    @Test
    fun `reduce ResetStatus should set Default status`() {
        val state = LoginState(status = UiResult.Failure())

        val newState = reducer.reduce(state, ResetStatus)

        assertIs<UiResult.Default>(newState.status)
    }

    @Test
    fun `reduce ToggleEmailMask should toggle isEmailMasked`() {
        val state = LoginState(email = "user@example.com", isEmailMasked = false)

        val newState = reducer.reduce(state, ToggleEmailMask)

        assertEquals(true, newState.isEmailMasked)
    }

    @Test
    fun `reduce TogglePasswordMask should toggle isPasswordMasked`() {
        val state = LoginState(isPasswordMasked = true)

        val newState = reducer.reduce(state, TogglePasswordMask)

        assertEquals(false, newState.isPasswordMasked)
    }

    @Test
    fun `reduce OnLogin with blank email should not proceed to Busy`() {
        val state = LoginState(email = "   ", password = "password123")

        val newState = reducer.reduce(state, OnLogin)

        assertNotNull(newState.emailError)
        assertIs<UiResult.Default>(newState.status)
    }

    @Test
    fun `reduce OnLogin with blank password should not proceed to Busy`() {
        val state = LoginState(email = "user@example.com", password = "   ")

        val newState = reducer.reduce(state, OnLogin)

        assertNotNull(newState.passwordError)
        assertIs<UiResult.Default>(newState.status)
    }
}
