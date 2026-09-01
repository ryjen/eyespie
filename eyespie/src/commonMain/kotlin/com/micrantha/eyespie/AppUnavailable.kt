package com.micrantha.eyespie

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.micrantha.eyespie.ui.EyespieTheme

@Composable
fun AppUnavailable() {
    EyespieTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Eyespie",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.headlineLarge,
                )
                Spacer(Modifier.height(12.dp))
                Text("The local game runtime could not be initialized.")
                Text(
                    "Verify required local components and relaunch the app.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
