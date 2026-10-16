package com.micrantha.eyespie.features.onboarding

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.micrantha.eyespie.presentation.theme.EyespieSecondaryAction

private const val CAMERA_PREFS = "eyespie-camera"
private const val CAMERA_REQUEST_ATTEMPTED = "request-attempted"

@Composable
actual fun OnboardingCameraPermission() {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val preferences = remember(context) {
        context.getSharedPreferences(CAMERA_PREFS, Context.MODE_PRIVATE)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
        preferences.edit().putBoolean(CAMERA_REQUEST_ATTEMPTED, true).apply()
        granted = allowed
    }

    if (granted) {
        Text("Camera enabled")
    } else {
        EyespieSecondaryAction(
            text = "Enable camera",
            modifier = Modifier.fillMaxWidth(),
            onClick = { launcher.launch(Manifest.permission.CAMERA) },
        )
        Text("Optional. You can continue without camera access and enable it later.")
    }
}
