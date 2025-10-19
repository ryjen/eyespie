package com.micrantha.bluebell.ext

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
suspend inline fun <T, R> Result<T>.then(transform: suspend (T) -> Result<R>): Result<R> {
    contract {
        callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
    }
    return try {
        transform(getOrThrow())
    } catch (err: Throwable) {
        Result.failure(err)
    }
}
