package com.example.tiptracker.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect

private const val PERMISSION = Manifest.permission.ACCESS_COARSE_LOCATION

enum class LocationPermissionStatus {
    /** Permission granted. Revoking is only possible from system settings. */
    Granted,

    /** Not granted, but the system dialog can still be shown. */
    CanRequest,

    /** Denied in a way that stops the system dialog from appearing; only system settings can fix it. */
    SettingsRequired
}

class LocationPermissionController(
    val status: LocationPermissionStatus,
    /** Shows the system dialog and records that we've asked. No-op when it can't be shown. */
    val request: () -> Unit,
    val openAppSettings: () -> Unit
) {
    /** Does the right thing for the current status: ask, or send the user to system settings. */
    fun onClick() = when (status) {
        LocationPermissionStatus.CanRequest -> request()
        LocationPermissionStatus.Granted,
        LocationPermissionStatus.SettingsRequired -> openAppSettings()
    }
}

/**
 * @param asked persisted flag: whether we've already shown the system dialog once.
 * @param onAsked called when the dialog is about to be shown, so the caller can persist [asked].
 *
 * The OS is the source of truth for the grant; it is re-read every time the screen resumes
 * (e.g. coming back from system settings).
 */
@Composable
fun rememberLocationPermissionController(
    asked: Boolean,
    onAsked: () -> Unit
): LocationPermissionController {
    val context = LocalContext.current
    val activity = LocalActivity.current

    var granted by remember { mutableStateOf(context.hasLocationPermission()) }
    var showRationale by remember { mutableStateOf(false) }

    fun refresh() {
        granted = context.hasLocationPermission()
        showRationale = activity?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(it, PERMISSION)
        } ?: false
    }

    LifecycleResumeEffect(Unit) {
        refresh()
        onPauseOrDispose { }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refresh() }

    // Rationale is false both before the first ask and after "don't ask again", so the
    // persisted flag is what tells those two cases apart.
    val status = when {
        granted -> LocationPermissionStatus.Granted
        !asked || showRationale -> LocationPermissionStatus.CanRequest
        else -> LocationPermissionStatus.SettingsRequired
    }

    return LocationPermissionController(
        status = status,
        request = {
            if (status == LocationPermissionStatus.CanRequest) {
                onAsked()
                launcher.launch(PERMISSION)
            }
        },
        openAppSettings = {
            context.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null)
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    )
}

private fun Context.hasLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, PERMISSION) == PackageManager.PERMISSION_GRANTED
