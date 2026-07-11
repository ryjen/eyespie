package com.micrantha.eyespie.core.data.account

import com.micrantha.eyespie.core.data.account.model.CurrentSession
import com.micrantha.eyespie.core.data.account.source.AccountRemoteSource
import com.micrantha.eyespie.domain.entities.Session
import com.micrantha.eyespie.domain.repository.AccountRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal class AccountDataRepository(
    private val remoteSource: AccountRemoteSource,
    private val currentSession: CurrentSession,
) : AccountRepository {

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun session() = remoteSource.account()
        .recoverCatching { remoteSource.currentAccount().getOrThrow() }
        .map { data ->
            Session(
                accessToken = data.accessToken,
                refreshToken = data.refreshToken,
                userId = data.userId,
                id = Uuid.random().toString()
            )
        }.onSuccess {
            currentSession.update(it)
        }

    override suspend fun isLoggedIn() = remoteSource.isLoggedIn()

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun login(email: String, passwd: String): Result<Session> {
        return remoteSource.login(email, passwd).mapCatching {
            remoteSource.currentAccount().map { data ->
                Session(
                    accessToken = data.accessToken,
                    refreshToken = data.refreshToken,
                    userId = data.userId,
                    id = Uuid.random().toString()
                )
            }.getOrThrow()
        }.onSuccess {
            currentSession.update(it)
        }
    }

    override suspend fun loginAnonymous() = remoteSource.loginAnonymous()

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun loginWithGoogle() = remoteSource.loginWithGoogle().mapCatching {
        remoteSource.currentAccount().map { data ->
            Session(
                accessToken = data.accessToken,
                refreshToken = data.refreshToken,
                userId = data.userId,
                id = Uuid.random().toString()
            )
        }.getOrThrow()
    }.onSuccess {
        currentSession.update(it)
    }

    override suspend fun register(email: String, passwd: String) =
        remoteSource.register(email, passwd)

    override suspend fun registerWithGoogle() = remoteSource.registerWithGoogle()
}
