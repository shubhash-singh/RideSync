package com.ragnar.RideSync.ui.screens.map

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.ragnar.RideSync.BuildConfig
import com.ragnar.RideSync.domain.model.TeamMember
import com.ragnar.RideSync.utils.DebugLogger
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
        onBack: () -> Unit,
        modifier: Modifier = Modifier,
        locationViewModel: LocationViewModel = hiltViewModel(),
        teamMapViewModel: TeamMapViewModel = hiltViewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = remember(context) { context.findActivity() }

    var requestedOnce by rememberSaveable { mutableStateOf(false) }
    var showRationaleDialog by rememberSaveable { mutableStateOf(false) }
    var hasLocationPermission by remember { mutableStateOf(context.hasAnyLocationPermission()) }
    var cameraMovedToUser by rememberSaveable { mutableStateOf(false) }

    // Observe live location from ViewModel
    val currentLocation by locationViewModel.locationState.collectAsStateWithLifecycle()

    // Phase 8: team member locations
    val members by teamMapViewModel.members.collectAsStateWithLifecycle()
    val hasTeam = !teamMapViewModel.teamId.isNullOrBlank()

    if (BuildConfig.DEBUG) {
        LaunchedEffect(Unit) { DebugLogger.d("MapScreen") { "Entered" } }
        LaunchedEffect(hasLocationPermission, requestedOnce) {
            DebugLogger.d("MapScreen") {
                "Permission state: hasLocationPermission=$hasLocationPermission requestedOnce=$requestedOnce"
            }
        }
    }

    val launcher =
            rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { _ ->
                DebugLogger.d("MapScreen") { "Permission dialog returned. Re-checking..." }
                hasLocationPermission = context.hasAnyLocationPermission()
            }

    // Keep permission state in sync when returning from Settings.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasLocationPermission = context.hasAnyLocationPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Start location tracking as soon as permission is granted.
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            DebugLogger.d("MapScreen") { "Permission granted – starting location tracking" }
            locationViewModel.startTracking()
        }
    }

    val shouldShowRationale =
            remember(hasLocationPermission, requestedOnce) {
                if (hasLocationPermission || activity == null) return@remember false
                ActivityCompat.shouldShowRequestPermissionRationale(
                        activity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) ||
                        ActivityCompat.shouldShowRequestPermissionRationale(
                                activity,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                        )
            }

    val isPermanentlyDenied =
            remember(hasLocationPermission, requestedOnce, shouldShowRationale) {
                requestedOnce && !hasLocationPermission && !shouldShowRationale
            }

    val requestPermissions: () -> Unit = {
        DebugLogger.d("MapScreen") { "Requesting location permissions..." }
        requestedOnce = true
        launcher.launch(
                arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                )
        )
    }

    val onGrantLocationClick: () -> Unit = {
        if (shouldShowRationale) {
            showRationaleDialog = true
        } else {
            requestPermissions()
        }
    }

    if (showRationaleDialog) {
        AlertDialog(
                onDismissRequest = { showRationaleDialog = false },
                title = { Text("Allow location access?") },
                text = {
                    Text(
                            "RideSync uses your location to show the blue dot on the map. " +
                                    "You can still use the map without granting location."
                    )
                },
                confirmButton = {
                    Button(
                            onClick = {
                                showRationaleDialog = false
                                requestPermissions()
                            }
                    ) { Text("Continue") }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showRationaleDialog = false }) { Text("Not now") }
                }
        )
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(0.0, 0.0), 1.5f)
    }

    // Animate camera to first user location fix.
    LaunchedEffect(currentLocation) {
        val loc = currentLocation ?: return@LaunchedEffect
        if (!cameraMovedToUser) {
            cameraMovedToUser = true
            DebugLogger.d("MapScreen") {
                "First location fix – animating camera to ${loc.latitude}, ${loc.longitude}"
            }
            cameraPositionState.animate(
                    update =
                            CameraUpdateFactory.newLatLngZoom(
                                    LatLng(loc.latitude, loc.longitude),
                                    16f
                            ),
                    durationMs = 1000
            )
        }
    }

    // Phase 8: When team members have locations, fit camera to include all of them.
    LaunchedEffect(members) {
        val withLocation = members.filter { it.lastLocation != null }
        if (withLocation.size >= 2) {
            val boundsBuilder = LatLngBounds.builder()
            currentLocation?.let { boundsBuilder.include(LatLng(it.latitude, it.longitude)) }
            withLocation.forEach { m ->
                m.lastLocation?.let { loc ->
                    boundsBuilder.include(LatLng(loc.latitude, loc.longitude))
                }
            }
            try {
                cameraPositionState.animate(
                        update = CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 120),
                        durationMs = 1200
                )
            } catch (_: Exception) {
                /* bounds builder may throw if only 1 point */
            }
        }
    }

    val uiSettings =
            remember(hasLocationPermission) {
                MapUiSettings(
                        zoomControlsEnabled = true,
                        myLocationButtonEnabled = hasLocationPermission
                )
            }

    val properties =
            remember(hasLocationPermission) {
                MapProperties(isMyLocationEnabled = hasLocationPermission)
            }

    val scaffoldState = rememberBottomSheetScaffoldState()

    BottomSheetScaffold(
            modifier = modifier,
            scaffoldState = scaffoldState,
            sheetPeekHeight = if (hasTeam && members.isNotEmpty()) 72.dp else 0.dp,
            sheetContent = {
                // Phase 8: Team member list in a bottom sheet
                if (hasTeam) {
                    MemberListSheet(members = members, currentLocation = currentLocation)
                }
            },
            topBar = {
                TopAppBar(
                        title = { Text("Map") },
                        navigationIcon = {
                            IconButton(
                                    onClick = {
                                        DebugLogger.d("MapScreen") { "Back tapped" }
                                        onBack()
                                    }
                            ) {
                                Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back"
                                )
                            }
                        },
                        actions = {
                            // Team member count badge (Phase 8)
                            if (hasTeam) {
                                BadgedBox(
                                        badge = {
                                            if (members.isNotEmpty()) {
                                                Badge { Text(members.size.toString()) }
                                            }
                                        }
                                ) {
                                    Icon(
                                            Icons.Default.Group,
                                            contentDescription = "Team members",
                                            modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                            }
                        }
                )
            }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = properties,
                    uiSettings = uiSettings,
                    onMapLoaded = { DebugLogger.i("MapScreen") { "GoogleMap loaded" } }
            ) {
                // Phase 8: Render a standard marker for each team member who has a known location
                members.forEach { member ->
                    val loc = member.lastLocation ?: return@forEach
                    // MarkerState is keyed to the member's latest coordinates so it
                    // recomposes (and re-positions) on every location update.
                    val markerState =
                            remember(member.userId, loc.latitude, loc.longitude) {
                                MarkerState(position = LatLng(loc.latitude, loc.longitude))
                            }
                    Marker(
                            state = markerState,
                            title = member.displayName ?: "Member",
                            snippet =
                                    String.format(
                                            Locale.US,
                                            "%.4f, %.4f",
                                            loc.latitude,
                                            loc.longitude
                                    )
                    )
                }
            }

            // ── Live lat/lng chip ─────────────────────────────────────────────
            AnimatedVisibility(
                    visible = hasLocationPermission && currentLocation != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp)
            ) {
                currentLocation?.let { loc ->
                    Row(
                            modifier =
                                    Modifier.background(
                                                    color =
                                                            MaterialTheme.colorScheme.surface.copy(
                                                                    alpha = 0.90f
                                                            ),
                                                    shape = MaterialTheme.shapes.small
                                            )
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                        )
                        Text(
                                text =
                                        String.format(
                                                Locale.US,
                                                "%.5f, %.5f",
                                                loc.latitude,
                                                loc.longitude
                                        ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // ── Permission banner ─────────────────────────────────────────────
            AnimatedVisibility(
                    visible = !hasLocationPermission,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Surface(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        tonalElevation = 3.dp,
                        shadowElevation = 2.dp,
                        shape = MaterialTheme.shapes.large
                ) {
                    Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                        )
                        Text(
                                text =
                                        if (isPermanentlyDenied) {
                                            "Location permission is disabled. Enable it in Settings to show your position."
                                        } else {
                                            "Grant location permission to show your position on the map."
                                        },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.size(2.dp))
                        if (isPermanentlyDenied) {
                            OutlinedButton(onClick = { context.openAppSettings() }) {
                                Text("Open Settings")
                            }
                        } else {
                            Button(onClick = onGrantLocationClick) { Text("Grant Permission") }
                        }
                    }
                }
            }
        }
    }
}


// ─── Phase 8: Member bottom sheet ────────────────────────────────────────────


@Composable
private fun MemberListSheet(members: List<TeamMember>, currentLocation: Location?) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
                "${members.size} team member${if (members.size != 1) "s" else ""}",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
            items(members, key = { it.userId }) { member ->
                val distanceText =
                        remember(member.lastLocation, currentLocation) {
                            val loc = member.lastLocation
                            val cur = currentLocation
                            if (loc != null && cur != null) {
                                val result = FloatArray(1)
                                Location.distanceBetween(
                                        cur.latitude,
                                        cur.longitude,
                                        loc.latitude,
                                        loc.longitude,
                                        result
                                )
                                val dist = result[0]
                                if (dist < 1000) "${dist.toInt()} m away"
                                else "${"%.1f".format(dist / 1000)} km away"
                            } else "Location unknown"
                        }
                ListItem(
                        headlineContent = { Text(member.displayName ?: "Unknown") },
                        supportingContent = {
                            Text(distanceText, style = MaterialTheme.typography.bodySmall)
                        },
                        leadingContent = {
                            if (member.photoUrl != null) {
                                AsyncImage(
                                        model = member.photoUrl,
                                        contentDescription = member.displayName,
                                        modifier = Modifier.size(36.dp).clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                )
                            } else {
                                Surface(
                                        modifier = Modifier.size(36.dp),
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                                (member.displayName?.firstOrNull() ?: '?')
                                                        .uppercase(),
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }
                )
            }
        }
    }
}

// ─── Extension helpers ───────────────────────────────────────────────────────

private fun Context.hasAnyLocationPermission(): Boolean {
    val coarse =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
    val fine =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
    return coarse || fine
}

private fun Context.openAppSettings() {
    val intent =
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
    startActivity(intent)
}

private fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
