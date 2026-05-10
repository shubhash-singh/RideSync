package com.ragnar.RideSync.ui.screens.team

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Lobby screen shown when the user has no active team.
 *
 * Provides two expandable cards — Create a team (name field) and Join a team (6-char code field).
 *
 * @param onTeamJoined Called after a successful create or join with the new team id.
 * @param onBack Navigation back action.
 *
 * Phase 7: Team Creation & Joining System
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamLobbyScreen(
        onTeamJoined: (teamId: String) -> Unit,
        onBack: () -> Unit,
        viewModel: TeamViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    // Guard: only navigate away once — prevents re-entry if state updates after pop
    var navigatedToTeam by remember { mutableStateOf(false) }

    // Navigate to team details once we're InTeam
    LaunchedEffect(uiState) {
        if (!navigatedToTeam && uiState is TeamUiState.InTeam) {
            navigatedToTeam = true
            onTeamJoined((uiState as TeamUiState.InTeam).team.id)
        }
    }

    // Show errors in snackbars
    LaunchedEffect(uiState) {
        if (uiState is TeamUiState.Error) {
            snackbarHostState.showSnackbar((uiState as TeamUiState.Error).message)
            viewModel.clearError()
        }
    }

    Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                        title = { Text("My Team") },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back"
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
                                .padding(horizontal = 20.dp)
                                .verticalScroll(rememberScrollState())
                                .imePadding(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            Text(
                    "You're not in a team yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val isLoading = uiState is TeamUiState.Loading

            // ── Create Team card ──────────────────────────────────────────────
            CreateTeamCard(isLoading = isLoading, onCreateTeam = { viewModel.createTeam(it) })

            // ── Join Team card ────────────────────────────────────────────────
            JoinTeamCard(isLoading = isLoading, onJoinTeam = { viewModel.joinTeam(it) })

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CreateTeamCard(isLoading: Boolean, onCreateTeam: (String) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }

    ElevatedCard(modifier = Modifier.fillMaxWidth().animateContentSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                            "Create a Team",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                    )
                    Text(
                            "Set a name and share the code with friends",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { expanded = !expanded }, enabled = !isLoading) {
                    Icon(
                            if (expanded) Icons.Default.Group else Icons.Default.Add,
                            contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }

            AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Team name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions =
                                    KeyboardOptions(
                                            capitalization = KeyboardCapitalization.Words,
                                            imeAction = ImeAction.Done
                                    ),
                            keyboardActions =
                                    KeyboardActions(
                                            onDone = {
                                                if (name.isNotBlank() && !isLoading)
                                                        onCreateTeam(name)
                                            }
                                    ),
                            enabled = !isLoading
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                            onClick = { onCreateTeam(name) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = name.isNotBlank() && !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Create Team")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JoinTeamCard(isLoading: Boolean, onJoinTeam: (String) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var code by rememberSaveable { mutableStateOf("") }

    ElevatedCard(modifier = Modifier.fillMaxWidth().animateContentSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                        Icons.Default.Tag,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(28.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                            "Join a Team",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                    )
                    Text(
                            "Enter the 6-character code from your team leader",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { expanded = !expanded }, enabled = !isLoading) {
                    Icon(
                            if (expanded) Icons.Default.Group else Icons.Default.Tag,
                            contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }

            AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    OutlinedTextField(
                            value = code,
                            onValueChange = { if (it.length <= 6) code = it.uppercase() },
                            label = { Text("Team code") },
                            placeholder = { Text("e.g. A3B7C1") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions =
                                    KeyboardOptions(
                                            capitalization = KeyboardCapitalization.Characters,
                                            imeAction = ImeAction.Done
                                    ),
                            keyboardActions =
                                    KeyboardActions(
                                            onDone = {
                                                if (code.length == 6 && !isLoading) onJoinTeam(code)
                                            }
                                    ),
                            enabled = !isLoading
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                            onClick = { onJoinTeam(code) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = code.length == 6 && !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Join Team")
                        }
                    }
                }
            }
        }
    }
}
