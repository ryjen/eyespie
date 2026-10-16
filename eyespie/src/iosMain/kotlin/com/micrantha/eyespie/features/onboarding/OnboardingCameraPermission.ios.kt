@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.micrantha.eyespie.features.onboarding

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.micrantha.eyespie.presentation.theme.EyespieSecondaryAction
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFoundation.*
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationOpenSettingsURLString
import kotlin.coroutines.resume

@Composable
actual fun OnboardingCameraPermission() {
    val scope = rememberCoroutineScope()
    var status by remember {
        mutableStateOf(AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo))
    }
    DisposableEffect(Unit) {
        val observer = NSNotificationCenter.defaultCenter.addObserverForName(
            UIApplicationDidBecomeActiveNotification,
            null,
            null,
        ) {
            status = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)
        }
        onDispose { NSNotificationCenter.defaultCenter.removeObserver(observer) }
    }

    if (status == AVAuthorizationStatusAuthorized) {
        Text("Camera enabled")
    } else if (status == AVAuthorizationStatusNotDetermined) {
        EyespieSecondaryAction(
            text = "Enable camera",
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                scope.launch {
                    suspendCancellableCoroutine<Unit> { continuation ->
                        AVCaptureDevice.requestAccessForMediaType(mediaType = AVMediaTypeVideo) { _: Boolean ->
                            if (continuation.isActive) continuation.resume(Unit)
                        }
                    }
                    status = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)
                }
            },
        )
        Text("Optional. You can continue without camera access and enable it later.")
    } else {
        Text("Camera access is off. Enable it in iOS Settings when you need the camera.")
        EyespieSecondaryAction(
            text = "Open Settings",
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                NSURL(string = UIApplicationOpenSettingsURLString)?.let { settingsUrl ->
                    UIApplication.sharedApplication.openURL(settingsUrl)
                }
            },
        )
    }
}
