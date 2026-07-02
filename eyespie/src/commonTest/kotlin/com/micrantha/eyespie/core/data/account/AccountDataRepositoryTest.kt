package com.micrantha.eyespie.core.data.account

import com.micrantha.eyespie.core.data.account.model.AccountResponse
import com.micrantha.eyespie.core.data.account.model.CurrentSession
import com.micrantha.eyespie.core.data.account.source.AccountRemoteSource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class AccountDataRepositoryTest {

    private class FakeAccountRemoteSource : AccountRemoteSource {
        var loggedIn: Boolean = false
        var accountResult: Result<AccountResponse> = Result.failure(Exception("Not found"))
        var loginResult: Result<Unit> = Result.success(Unit)

        override fun isLoggedIn() = loggedIn
        override suspend fun account() = accountResult
        override suspend fun loginAnonymous() = loginResult
        override suspend fun login(email: String, password: String) = loginResult
        override suspend fun loginWithGoogle() = loginResult
        override suspend fun register(email: String, password: String) = loginResult
        override suspend fun registerWithGoogle() = loginResult
    }

    private val remoteSource = FakeAccountRemoteSource()
    private val currentSession = CurrentSession
    private val repository = AccountDataRepository(remoteSource, currentSession)

    @Test
    fun `session should update currentSession on success`() = runTest {
        val accountResponse = AccountResponse("access", "refresh", "user")
        remoteSource.accountResult = Result.success(accountResponse)

        val result = repository.session()

        assertTrue(result.isSuccess)
        assertTrue(currentSession.isValid)
    }

    @Test
    fun `isLoggedIn should return value from remoteSource`() = runTest {
        remoteSource.loggedIn = true

        val result = repository.isLoggedIn()

        assertTrue(result)
    }
}
