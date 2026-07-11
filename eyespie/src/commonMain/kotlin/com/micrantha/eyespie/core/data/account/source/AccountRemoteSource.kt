package com.micrantha.eyespie.core.data.account.source

import com.micrantha.eyespie.core.data.account.model.AccountResponse
import com.micrantha.eyespie.core.data.client.SupaClient
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email

internal interface AccountRemoteSource {
    fun isLoggedIn(): Boolean
    suspend fun account(): Result<AccountResponse>
    suspend fun currentAccount(): Result<AccountResponse>
    suspend fun loginAnonymous(): Result<Unit>
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun loginWithGoogle(): Result<Unit>
    suspend fun register(email: String, password: String): Result<Unit>
    suspend fun registerWithGoogle(): Result<Unit>
}

internal class SupabaseAccountRemoteSource(
    private val client: SupaClient
) : AccountRemoteSource {
    override fun isLoggedIn(): Boolean = client.auth().currentSessionOrNull()?.user != null

    override suspend fun account() = try {
        val session = client.auth().apply { loadFromStorage() }.currentSessionOrNull()!!
        val user = session.user!!
        Result.success(AccountResponse(session.accessToken, session.refreshToken, user.id))
    } catch (e: Throwable) {
        Result.failure(e)
    }

    override suspend fun currentAccount() = try {
        val session = client.auth().currentSessionOrNull()!!
        val user = session.user!!
        Result.success(AccountResponse(session.accessToken, session.refreshToken, user.id))
    } catch (e: Throwable) {
        Result.failure(e)
    }

    override suspend fun loginAnonymous() = try {
        client.auth().signInAnonymously()
        Result.success(Unit)
    } catch (e: Throwable) {
        Result.failure(e)
    }

    override suspend fun login(email: String, password: String) = try {
        client.auth().signInWith(Email) {
            this.email = email
            this.password = password
        }
        Result.success(Unit)
    } catch (e: Throwable) {
        Result.failure(e)
    }

    override suspend fun loginWithGoogle() = try {
        client.auth().signInWith(Google)
        Result.success(Unit)
    } catch (e: Throwable) {
        Result.failure(e)
    }

    override suspend fun register(email: String, password: String) = try {
        client.auth().signUpWith(Email) {
            this.email = email
            this.password = password
        }
        Result.success(Unit)
    } catch (e: Throwable) {
        Result.failure(e)
    }

    override suspend fun registerWithGoogle() = try {
        client.auth().signUpWith(Google)
        Result.success(Unit)
    } catch (e: Throwable) {
        Result.failure(e)
    }
}
