package com.micrantha.eyespie.core.ui.component

import com.apollographql.apollo.exception.ApolloNetworkException
import com.micrantha.eyespie.app.S
import com.micrantha.eyespie.generated.resources.model_status_failure
import com.micrantha.eyespie.generated.resources.network_failure

fun Throwable.toI18n() = when (this) {
    is ApolloNetworkException -> S.network_failure
    else -> S.model_status_failure
}
