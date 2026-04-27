package com.ragnar.RideSync.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ragnar.RideSync.BuildConfig
import com.ragnar.RideSync.utils.DebugLogger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
        onBack: () -> Unit,
        viewModel: UserViewModel = hiltViewModel(),
        modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    if (BuildConfig.DEBUG) {
        LaunchedEffect(Unit) { DebugLogger.d("ProfileScreen") { "Entered" } }
        LaunchedEffect(state) {
            when (state) {
                is UserUiState.Loading -> DebugLogger.d("ProfileScreen") { "State=Loading" }
                is UserUiState.Unauthenticated ->
                        DebugLogger.d("ProfileScreen") { "State=Unauthenticated" }
                is UserUiState.Error ->
                        DebugLogger.w("ProfileScreen") {
                            "State=Error message=${(state as UserUiState.Error).message}"
                        }
                is UserUiState.Data ->
                        DebugLogger.i("ProfileScreen") {
                            "State=Data uid=${(state as UserUiState.Data).user.id}"
                        }
            }
        }
    }

    Scaffold(
            modifier = modifier,
            topBar = {
                TopAppBar(
                        title = { Text("Profile") },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back"
                                )
                            }
                        }
                )
            }
    ) { innerPadding ->
        Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
                contentAlignment = Alignment.Center
        ) {
            when (state) {
                is UserUiState.Loading -> CircularProgressIndicator()
                is UserUiState.Unauthenticated -> Text("You are signed out.")
                is UserUiState.Error ->
                        Text(
                                text = (state as UserUiState.Error).message,
                                color = MaterialTheme.colorScheme.error
                        )
                is UserUiState.Data -> {
                    val user = (state as UserUiState.Data).user
                    Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                    ) {
                        if (!user.photoUrl.isNullOrBlank()) {
                            AsyncImage(
                                    model = user.photoUrl,
                                    contentDescription = "Profile picture",
                                    modifier = Modifier.size(96.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Profile",
                                    modifier = Modifier.size(96.dp),
                                    tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.size(16.dp))

                        Text(
                                text = user.displayName ?: "User",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.size(6.dp))

                        Text(
                                text = user.email ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.size(20.dp))

                        val teamText = user.currentTeamId?.takeIf { it.isNotBlank() } ?: "None"
                        Text(
                                text = "Current team: $teamText",
                                style = MaterialTheme.typography.bodyLarge
                        )

                        user.lastLocation?.let { loc ->
                            Spacer(modifier = Modifier.size(10.dp))
                            Text(
                                    text = "Last location: ${loc.latitude}, ${loc.longitude}",
                                    style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
    }
}
