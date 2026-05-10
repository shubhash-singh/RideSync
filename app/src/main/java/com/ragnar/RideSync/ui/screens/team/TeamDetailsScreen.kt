package com.ragnar.RideSync.ui.screens.team

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.ragnar.RideSync.domain.model.TeamMember

/**
 * Shows the active team: name, 6-char code (copyable), member list, and leave / disband actions.
 *
 * @param teamId Firestore team document ID.
 * @param onLeft Called after leave or disband (navigates back to lobby).
 * @param onBack Standard back navigation.
 *
 * Phase 7: Team Creation & Joining System
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamDetailsScreen(
        teamId: String,
        onLeft: () -> Unit,
        onBack: () -> Unit,
        viewModel: TeamViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboard = LocalClipboardManager.current

    // Navigate out on leave / disband
    LaunchedEffect(uiState) { if (uiState is TeamUiState.Idle) onLeft() }

    LaunchedEffect(uiState) {
        if (uiState is TeamUiState.Error) {
            snackbarHostState.showSnackbar((uiState as TeamUiState.Error).message)
            viewModel.clearError()
        }
    }

    val currentUid = remember { FirebaseAuth.getInstance().currentUser?.uid }

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
        when (val state = uiState) {
            is TeamUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is TeamUiState.InTeam -> {
                val team = state.team
                val members = state.members
                val isLeader = currentUid == team.leaderId
                var showLeaveDialog by rememberSaveable { mutableStateOf(false) }
                var showDisbandDialog by rememberSaveable { mutableStateOf(false) }

                LazyColumn(
                        modifier =
                                Modifier.fillMaxSize()
                                        .padding(innerPadding)
                                        .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Spacer(Modifier.height(4.dp))
                        // ── Team header card ──────────────────────────────────
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                    modifier = Modifier.padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                        text = team.name,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold
                                )

                                Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                            text = "Code:",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Surface(
                                            shape = MaterialTheme.shapes.small,
                                            color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                                text = team.code,
                                                modifier =
                                                        Modifier.padding(
                                                                horizontal = 10.dp,
                                                                vertical = 4.dp
                                                        ),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color =
                                                        MaterialTheme.colorScheme
                                                                .onPrimaryContainer,
                                                letterSpacing =
                                                        androidx.compose.ui.unit.TextUnit
                                                                .Unspecified
                                        )
                                    }
                                    FilledTonalIconButton(
                                            onClick = {
                                                clipboard.setText(AnnotatedString(team.code))
                                            },
                                            modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                                Icons.Default.ContentCopy,
                                                contentDescription = "Copy code",
                                                modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                if (isLeader) {
                                    Text(
                                            text = "You are the team leader",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    // ── Members section ───────────────────────────────────────
                    item {
                        Text(
                                "${members.size} member${if (members.size != 1) "s" else ""}",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        HorizontalDivider()
                    }

                    if (members.isEmpty()) {
                        item {
                            Text(
                                    "No members yet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    } else {
                        items(members, key = { it.userId }) { member ->
                            MemberRow(member = member, isLeader = member.userId == team.leaderId)
                        }
                    }

                    // ── Action buttons ────────────────────────────────────────
                    item {
                        Spacer(Modifier.height(4.dp))

                        OutlinedButton(
                                onClick = { showLeaveDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors =
                                        ButtonDefaults.outlinedButtonColors(
                                                contentColor = MaterialTheme.colorScheme.error
                                        )
                        ) {
                            Icon(
                                    Icons.AutoMirrored.Filled.ExitToApp,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Leave Team")
                        }

                        if (isLeader) {
                            Spacer(Modifier.height(8.dp))
                            Button(
                                    onClick = { showDisbandDialog = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors =
                                            ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.error
                                            )
                            ) {
                                Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Disband Team")
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                    }
                }

                if (showLeaveDialog) {
                    AlertDialog(
                            onDismissRequest = { showLeaveDialog = false },
                            title = { Text("Leave Team?") },
                            text = { Text("You will need the code to rejoin.") },
                            confirmButton = {
                                Button(
                                        colors =
                                                ButtonDefaults.buttonColors(
                                                        containerColor =
                                                                MaterialTheme.colorScheme.error
                                                ),
                                        onClick = {
                                            showLeaveDialog = false
                                            viewModel.leaveTeam()
                                        }
                                ) { Text("Leave") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showLeaveDialog = false }) { Text("Cancel") }
                            }
                    )
                }

                if (showDisbandDialog) {
                    AlertDialog(
                            onDismissRequest = { showDisbandDialog = false },
                            title = { Text("Disband Team?") },
                            text = {
                                Text(
                                        "This will remove all members and permanently delete the team."
                                )
                            },
                            confirmButton = {
                                Button(
                                        colors =
                                                ButtonDefaults.buttonColors(
                                                        containerColor =
                                                                MaterialTheme.colorScheme.error
                                                ),
                                        onClick = {
                                            showDisbandDialog = false
                                            viewModel.disbandTeam()
                                        }
                                ) { Text("Disband") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDisbandDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                    )
                }
            }
            else -> {
                /* Idle / Error — LaunchedEffect already navigated away */
            }
        }
    }
}

@Composable
private fun MemberRow(member: TeamMember, isLeader: Boolean) {
    ListItem(
            headlineContent = {
                Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(member.displayName ?: "Unknown")
                    if (isLeader) {
                        Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                    "Leader",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            },
            leadingContent = {
                if (member.photoUrl != null) {
                    AsyncImage(
                            model = member.photoUrl,
                            contentDescription = member.displayName,
                            modifier = Modifier.size(40.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                    text = (member.displayName?.firstOrNull() ?: '?').uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
    )
}
