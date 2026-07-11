package com.micrantha.eyespie.features.dashboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.kodein.rememberScreenModel
import com.micrantha.bluebell.arch.Dispatch
import com.micrantha.bluebell.ui.components.TabPager
import com.micrantha.bluebell.ui.components.status.FailureContent
import com.micrantha.bluebell.ui.components.status.LoadingContent
import com.micrantha.bluebell.ui.model.UiResult.Failure
import com.micrantha.bluebell.ui.model.UiResult.Ready
import com.micrantha.bluebell.ui.theme.Dimensions
import com.micrantha.eyespie.app.S
import com.micrantha.eyespie.core.ui.component.AppTitle
import com.micrantha.eyespie.core.ui.component.RealtimeDataEnabledEffect
import com.micrantha.eyespie.features.dashboard.ui.DashboardAction.Load
import com.micrantha.eyespie.features.dashboard.ui.DashboardAction.ScanNewThing
import com.micrantha.eyespie.features.dashboard.ui.component.FriendsTabContent
import com.micrantha.eyespie.features.dashboard.ui.component.NearbyTabContent
import com.micrantha.eyespie.features.dashboard.ui.component.ScanNewThingCard
import eyespie.app.generated.resources.friends
import eyespie.app.generated.resources.loading_dashboard
import eyespie.app.generated.resources.nearby
import org.jetbrains.compose.resources.stringResource

class DashboardScreen : Screen {

    @Composable
    override fun Content() {
        val screenModel: DashboardScreenModel = rememberScreenModel()

        LaunchedEffect(Unit) {
            screenModel.dispatch(Load)
        }

        val state by screenModel.state.collectAsState()

        Render(state, screenModel)
    }

    @Composable
    fun Render(
        state: DashboardUiState,
        dispatch: Dispatch,
    ) {

        RealtimeDataEnabledEffect(dispatch = dispatch)

        Column(
            modifier = Modifier.fillMaxSize()
        ) {

            SyncStatusHeader(state)

            AppTitle()

            ScanNewThingCard {
                dispatch(ScanNewThing)
            }

            Box(modifier = Modifier.weight(1f)) {
                when (state.status) {
                    is Ready -> ContentPager(state.status.data, dispatch)
                    is Failure -> FailureContent(state.status.message)
                    else -> LoadingContent(S.loading_dashboard)
                }
            }
        }
    }

    @Composable
    private fun SyncStatusHeader(state: DashboardUiState) {
        val count = (state.status as? Ready)?.data?.pendingSyncCount ?: 0
        if (count > 0) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(Dimensions.Padding.small),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BadgedBox(
                    badge = {
                        Badge { Text(count.toString()) }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Syncing",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Text(
                    text = " Syncing $count captures...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }

    @Composable
    fun ContentPager(data: DashboardUiState.Data, dispatch: Dispatch) {
        TabPager(
            stringResource(S.nearby),
            stringResource(S.friends)
        ) { page, _ ->
            when (page) {
                0 -> NearbyTabContent(data.nearby, dispatch)
                1 -> FriendsTabContent(data.friends, dispatch)
            }
        }
    }
}
