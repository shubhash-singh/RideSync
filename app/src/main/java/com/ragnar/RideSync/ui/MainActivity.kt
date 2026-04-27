package com.ragnar.RideSync.ui

import android.os.Bundle
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
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private companion object {
        private const val TAG = "MainActivity"
    }

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            RideSyncTheme {
                val authState by authViewModel.authState.collectAsState()

                // Determine start destination based on auth state
                val startDestination =
                        when (authState) {
                            is AuthState.Authenticated -> Screen.Home.route
                            is AuthState.Unauthenticated -> Screen.Login.route
                            // While loading / idle, default to Login (it will auto-navigate if
                            // authenticated)
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
                            startDestination = startDestination
                    )
                }
            }
        }
    }
}
