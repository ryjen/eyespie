package com.micrantha.eyespie.domain.repository

import com.micrantha.eyespie.domain.entities.Session

class FakeAccountRepository : AccountRepository {
    var sessionResult: Result<Session> = Result.failure(Exception("Not set"))
    var loggedIn: Boolean = false

    override suspend fun session() = sessionResult
    override suspend fun isLoggedIn() = loggedIn
    override suspend fun login(email: String, passwd: String) = sessionResult
    override suspend fun loginAnonymous() = Result.success(Unit)
    override suspend fun loginWithGoogle() = sessionResult
    override suspend fun register(email: String, passwd: String) = Result.success(Unit)
    override suspend fun registerWithGoogle() = Result.success(Unit)
}
