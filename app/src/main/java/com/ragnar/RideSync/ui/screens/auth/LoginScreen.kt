package com.ragnar.RideSync.ui.screens.auth

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.ragnar.RideSync.utils.Constants
import kotlinx.coroutines.launch

/**
 * Login screen with Google Sign-In using Credential Manager API. Displays app branding and a
 * sign-in button. On success, navigates to Home.
 *
 * @param onSignInSuccess Callback invoked when sign-in completes successfully.
 * @param viewModel The auth ViewModel, injected by Hilt.
 */
@Composable
fun LoginScreen(
        onSignInSuccess: () -> Unit,
        viewModel: AuthViewModel = hiltViewModel(),
        modifier: Modifier = Modifier
) {
    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Navigate when authenticated
    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onSignInSuccess()
        }
    }

    // Show toast on error
    LaunchedEffect(authState) {
        if (authState is AuthState.Error) {
            Toast.makeText(context, (authState as AuthState.Error).message, Toast.LENGTH_LONG)
                    .show()
            viewModel.clearError()
        }
    }

    val isLoading = authState is AuthState.Loading

    Box(
            modifier =
                    modifier.fillMaxSize()
                            .background(
                                    Brush.verticalGradient(
                                            colors =
                                                    listOf(
                                                            MaterialTheme.colorScheme.primary.copy(
                                                                    alpha = 0.08f
                                                            ),
                                                            MaterialTheme.colorScheme.surface,
                                                            MaterialTheme.colorScheme.surface
                                                    )
                                    )
                            )
    ) {
        Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
        ) {
            // App icon
            Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = "RideSync",
                    modifier = Modifier.size(100.dp),
                    tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(20.dp))

            // App name
            Text(
                    text = "RideSync",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tagline
            Text(
                    text = "Track your team in real-time\nduring your travel adventures",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(64.dp))

            // Google Sign-In Button
            Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                val credentialManager = CredentialManager.create(context)

                                val googleIdOption =
                                        GetGoogleIdOption.Builder()
                                                .setFilterByAuthorizedAccounts(false)
                                                .setServerClientId(Constants.WEB_CLIENT_ID)
                                                .build()

                                val request =
                                        GetCredentialRequest.Builder()
                                                .addCredentialOption(googleIdOption)
                                                .build()

                                val result =
                                        credentialManager.getCredential(
                                                request = request,
                                                context = context
                                        )

                                val googleIdTokenCredential =
                                        GoogleIdTokenCredential.createFrom(result.credential.data)

                                val idToken = googleIdTokenCredential.idToken
                                viewModel.signInWithGoogleIdToken(idToken)
                            } catch (e: GetCredentialCancellationException) {
                                // User cancelled — do nothing
                            } catch (e: Exception) {
                                Toast.makeText(
                                                context,
                                                "Sign-in failed: ${e.localizedMessage}",
                                                Toast.LENGTH_LONG
                                        )
                                        .show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors =
                            ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                            )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "Signing in...", style = MaterialTheme.typography.titleMedium)
                } else {
                    Text(text = "Sign in with Google", style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Subtle footer
            Text(
                    text = "Sign in to create or join a team",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }

        // Loading overlay
        AnimatedVisibility(
                visible = isLoading,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
        ) {
            // Already handled inline with the button
        }
    }
}
