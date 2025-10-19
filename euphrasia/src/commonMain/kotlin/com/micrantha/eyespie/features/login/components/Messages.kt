package com.micrantha.eyespie.features.login.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.font.FontStyle
import com.micrantha.bluebell.arch.Dispatch
import com.micrantha.bluebell.ui.model.error
import com.micrantha.eyespie.features.login.entities.LoginAction
import com.micrantha.eyespie.features.login.entities.LoginUiState
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

@Composable
fun Messages(state: LoginUiState, dispatch: Dispatch) {
        state.status.error?.let {
            Text(
                text = stringResource(it),
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.error
            )

            LaunchedEffect(Unit) {
                delay(5000)
                dispatch(LoginAction.ResetStatus)
            }
        }
    }
