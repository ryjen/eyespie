package com.micrantha.eyespie.features.scan.ui.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.kodein.rememberScreenModel
import com.micrantha.bluebell.arch.Dispatch
import com.micrantha.bluebell.ui.theme.Dimensions
import com.micrantha.eyespie.data.PendingCapture
import com.micrantha.eyespie.features.scan.ui.sync.PendingSyncAction.Back
import com.micrantha.eyespie.features.scan.ui.sync.PendingSyncAction.Delete
import com.micrantha.eyespie.features.scan.ui.sync.PendingSyncAction.Load

class PendingSyncScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val screenModel: PendingSyncScreenModel = rememberScreenModel()

        LaunchedEffect(Unit) {
            screenModel.dispatch(Load)
        }

        val state by screenModel.state.collectAsState()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Pending Captures") },
                    navigationIcon = {
                        IconButton(onClick = { screenModel.dispatch(Back) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(Dimensions.Padding.medium)
            ) {
                items(state.pending) { item ->
                    PendingItem(item, screenModel)
                    HorizontalDivider()
                }
            }
        }
    }

    @Composable
    private fun PendingItem(item: PendingCapture, dispatch: Dispatch) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Dimensions.Padding.small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = "Capture ${item.id}", style = MaterialTheme.typography.bodyLarge)
                Text(text = item.created_at, style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = { dispatch(Delete(item.id)) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}
