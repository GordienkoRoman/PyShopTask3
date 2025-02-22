@file:OptIn(ExperimentalPermissionsApi::class)

package com.example.pyshoptask3.ui


import androidx.compose.runtime.Composable
import com.example.pyshoptask3.ui.FaceDetector.FaceDetectorScreen
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState


@Composable
fun MainScreen() {

    val cameraPermissionState: PermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    MainContent(
        hasPermission = cameraPermissionState.status.isGranted,
        onRequestPermission = cameraPermissionState::launchPermissionRequest
    )
}

@Composable
private fun MainContent(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit
) {

    if (hasPermission) {
        FaceDetectorScreen()
    } else {
        NoPermissionScreen(onRequestPermission)
    }
}