package com.traveltrip.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.traveltrip.ui.navigation.TravelTripNavGraph
import com.traveltrip.ui.theme.TravelTripTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main entry point activity for TravelTrip. Sets up edge-to-edge display, Compose theme, and the
 * navigation host.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TravelTripTheme {
                Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    TravelTripNavGraph(navController = navController)
                }
            }
        }
    }
}
