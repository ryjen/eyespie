package com.micrantha.eyespie.presentation

import androidx.compose.runtime.Composable
import com.micrantha.eyespie.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun cameraUnavailableMessage(): String = stringResource(Res.string.failure_camera_unavailable)

@Composable
fun targetCameraPermissionMessage(): String = stringResource(Res.string.failure_camera_permission_target)

@Composable
fun clueTargetCameraPermissionMessage(): String = stringResource(Res.string.failure_camera_permission_clue_target)

@Composable
fun playCameraPermissionMessage(): String = stringResource(Res.string.failure_camera_permission_guess)
