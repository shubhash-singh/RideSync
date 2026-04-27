package com.ragnar.RideSync.ui.screens.home

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ragnar.RideSync.BuildConfig
import com.ragnar.RideSync.ui.screens.auth.AuthState
import com.ragnar.RideSync.ui.screens.auth.AuthViewModel
import com.ragnar.RideSync.utils.DebugLogger

/**
 * Home screen — the landing page of RideSync after authentication. Shows user info (avatar + name),
 * navigation to Map and Team screens, and a sign-out action.
 *
 * @param onNavigateToMap Callback when user taps "Open Map"
 * @param onNavigateToTeam Callback when user taps "My Team"
 * @param onNavigateToProfile Callback when user taps "Profile"
 * @param onSignOut Callback when sign-out completes, to navigate back to Login
 * @param authViewModel Shared auth ViewModel for user info and sign-out
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
        onNavigateToMap: () -> Unit,
        onNavigateToTeam: () -> Unit,
        onNavigateToProfile: () -> Unit,
        onSignOut: () -> Unit,
        authViewModel: AuthViewModel = hiltViewModel(),
        modifier: Modifier = Modifier
) {
        if (BuildConfig.DEBUG) {
                LaunchedEffect(Unit) { DebugLogger.d("HomeScreen") { "Entered" } }
        }

        val authState by authViewModel.authState.collectAsState()

        // Extract user info from auth state
        val userName = (authState as? AuthState.Authenticated)?.user?.displayName ?: "User"
        val photoUrl = (authState as? AuthState.Authenticated)?.user?.photoUrl?.toString()

        // Navigate back to login on sign-out
        androidx.compose.runtime.LaunchedEffect(authState) {
                if (authState is AuthState.Unauthenticated) {
                        onSignOut()
                }
        }

        Scaffold(
                modifier = modifier,
                topBar = {
                        TopAppBar(
                                title = { Text("RideSync") },
                                colors =
                                        TopAppBarDefaults.topAppBarColors(
                                                containerColor = MaterialTheme.colorScheme.surface
                                        ),
                                actions = {
                                        IconButton(
                                                onClick = {
                                                        DebugLogger.d("HomeScreen") {
                                                                "Navigate -> Profile"
                                                        }
                                                        onNavigateToProfile()
                                                }
                                        ) {
                                                Icon(
                                                        imageVector = Icons.Default.Person,
                                                        contentDescription = "Profile",
                                                        tint =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant
                                                )
                                        }
                                        IconButton(
                                                onClick = {
                                                        DebugLogger.d("HomeScreen") {
                                                                "Sign-out tapped"
                                                        }
                                                        authViewModel.signOut()
                                                }
                                        ) {
                                                Icon(
                                                        imageVector =
                                                                Icons.AutoMirrored.Filled.Logout,
                                                        contentDescription = "Sign Out",
                                                        tint =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant
                                                )
                                        }
                                }
                        )
                }
        ) { innerPadding ->
                Column(
                        modifier =
                                Modifier.fillMaxSize()
                                        .padding(innerPadding)
                                        .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                ) {
                        // User avatar
                        if (photoUrl != null) {
                                AsyncImage(
                                        model = photoUrl,
                                        contentDescription = "Profile picture",
                                        modifier = Modifier.size(80.dp).clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                )
                        } else {
                                Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Profile",
                                        modifier = Modifier.size(80.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Welcome text
                        Text(
                                text = "Welcome,",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                                text = userName,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(48.dp))

                        // Open Map button
                        Button(
                                onClick = {
                                        DebugLogger.d("HomeScreen") { "Navigate -> Map" }
                                        onNavigateToMap()
                                },
                                modifier =
                                        Modifier.fillMaxWidth().height(56.dp).animateContentSize()
                        ) {
                                Icon(
                                        imageVector = Icons.Default.Map,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(
                                        text = "Open Map",
                                        style = MaterialTheme.typography.titleMedium
                                )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // My Team button
                        OutlinedButton(
                                onClick = {
                                        DebugLogger.d("HomeScreen") { "Navigate -> Team (not implemented)" }
                                        onNavigateToTeam()
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                                Icon(
                                        imageVector = Icons.Default.Group,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(text = "My Team", style = MaterialTheme.typography.titleMedium)
                        }
                }
        }
}
