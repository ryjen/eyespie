package com.micrantha.eyespie.features.login.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.micrantha.bluebell.ui.model.Ready
import com.micrantha.eyespie.core.PreviewContext
import com.micrantha.eyespie.features.login.entities.LoginUiState

@Preview
@Composable
fun LoginPreview() = PreviewContext(
    LoginUiState(
        email = "account@example.com",
        password = "P@ssw0rd123",
        status = Ready(),
        isEmailMasked = false,
        isPasswordMasked = true
    )
) {
    LoginScreen()
}
