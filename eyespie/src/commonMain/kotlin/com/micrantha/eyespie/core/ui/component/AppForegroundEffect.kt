package com.micrantha.eyespie.core.ui.component

import androidx.compose.runtime.Composable

@Composable
expect fun AppForegroundEffect(onForeground: () -> Unit)
