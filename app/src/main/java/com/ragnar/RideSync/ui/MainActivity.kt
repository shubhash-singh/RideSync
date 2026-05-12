package com.ragnar.RideSync.ui

import android.app.PictureInPictureParams
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.ragnar.RideSync.BuildConfig
import com.ragnar.RideSync.ui.navigation.RideSyncNavGraph
import com.ragnar.RideSync.ui.navigation.Screen
import com.ragnar.RideSync.ui.screens.auth.AuthState
import com.ragnar.RideSync.ui.screens.auth.AuthViewModel
import com.ragnar.RideSync.ui.theme.RideSyncTheme
import com.ragnar.RideSync.utils.DebugLogger
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main entry point activity for RideSync. Determines start destination based on current auth state:
 * - If a user is already signed in → Home screen
 * - Otherwise → Login screen
 *
 * Phase 11: Supports Picture-in-Picture (PiP) mode. When the home button is pressed while the
 * Map screen is visible, the app shrinks into a floating PiP window showing the live map and
 * route progress — similar to Google Maps navigation.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private companion object {
        private const val TAG = "MainActivity"
        /** 16:9 aspect ratio for the PiP window (landscape card). */
        private val PIP_ASPECT_RATIO = Rational(16, 9)
    }

    private val authViewModel: AuthViewModel by viewModels()

    /**
     * Tracks whether the user is currently on the Map screen.
     * Only enter PiP when this is true — we don't want a PiP window for login/home screens.
     */
    val isOnMapScreen = mutableStateOf(false)

    /**
     * Exposed to the Compose tree so MapScreen can adapt its UI when inside PiP.
     * Updated in [onPictureInPictureModeChanged].
     */
    val isInPipMode = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            RideSyncTheme {
                val authState by authViewModel.authState.collectAsState()

                val startDestination =
                    when (authState) {
                        is AuthState.Authenticated -> Screen.Home.route
                        is AuthState.Unauthenticated -> Screen.Login.route
                        else -> Screen.Login.route
                    }

                if (BuildConfig.DEBUG) {
                    LaunchedEffect(startDestination) {
                        DebugLogger.d(TAG) {
                            "Start destination = $startDestination (authState=${authState::class.simpleName})"
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    RideSyncNavGraph(
                        navController = navController,
                        startDestination = startDestination,
                        onMapScreenActive = { active -> isOnMapScreen.value = active }
                    )
                }
            }
        }
    }

    /**
     * Called when the user presses the Home button. This is the standard trigger for entering PiP
     * in navigation apps (same as Google Maps).
     * We only enter PiP if the user is currently viewing the Map screen.
     */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (isOnMapScreen.value) {
            enterPipMode()
        }
    }

    /**
     * Called by the system when PiP mode changes.
     * We update [isInPipMode] so the Compose UI can react and hide all non-essential UI
     * (top bar, bottom sheet, permission banners, etc.).
     */
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        isInPipMode.value = isInPictureInPictureMode
        DebugLogger.d(TAG) { "PiP mode changed → $isInPictureInPictureMode" }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun enterPipMode() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val params = PictureInPictureParams.Builder()
            .setAspectRatio(PIP_ASPECT_RATIO)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setSeamlessResizeEnabled(false) // smoother entry animation
                }
            }
            .build()
        enterPictureInPictureMode(params)
        DebugLogger.d(TAG) { "Entered PiP mode" }
    }
}
