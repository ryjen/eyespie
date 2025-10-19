package com.micrantha.eyespie.core.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import com.micrantha.bluebell.arch.Dispatch
import com.micrantha.eyespie.domain.repository.RealtimeRepository
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.kodein.di.compose.rememberInstance

@Composable
fun RealtimeDataEnabledEffect(key: Any? = Unit, dispatch: Dispatch) {
    val repository by rememberInstance<RealtimeRepository>()
    val scope = rememberCoroutineScope()

    DisposableEffect(key, repository) {

        scope.launch {
            repository.start()
        }

        // TODO: dispatch other entities or break down per entity
        repository.things().onEach(dispatch::send).launchIn(scope)

        onDispose {
            repository.stop()
        }
    }
}
