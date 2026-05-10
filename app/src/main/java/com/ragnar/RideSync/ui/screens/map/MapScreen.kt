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
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation
import com.mapbox.maps.extension.compose.style.MapStyle
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.extension.compose.MapEffect
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

    if (com.ragnar.RideSync.BuildConfig.DEBUG) {
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

    // ── Mapbox: camera / viewport state ──────────────────────────────────────
    val mapViewportState = rememberMapViewportState {
        // Default overview of the world; will animate to user location on first fix.
        setCameraOptions {
            center(Point.fromLngLat(0.0, 0.0))
            zoom(1.5)
        }
    }

    // Animate camera to first user location fix.
    LaunchedEffect(currentLocation) {
        val loc = currentLocation ?: return@LaunchedEffect
        if (!cameraMovedToUser) {
            cameraMovedToUser = true
            DebugLogger.d("MapScreen") {
                "First location fix – animating camera to ${loc.latitude}, ${loc.longitude}"
            }
            mapViewportState.flyTo(
                    cameraOptions = CameraOptions.Builder()
                            .center(Point.fromLngLat(loc.longitude, loc.latitude))
                            .zoom(16.0)
                            .build(),
                    animationOptions =
                            com.mapbox.maps.plugin.animation.MapAnimationOptions.mapAnimationOptions {
                                duration(1000L)
                            }
            )
        }
    }

    // Phase 8: When team members have locations, fit camera to include all of them.
    LaunchedEffect(members) {
        val withLocation = members.filter { it.lastLocation != null }
        if (withLocation.size >= 2) {
            // Compute a rough geographic centre and a generous zoom.
            val lats = buildList {
                currentLocation?.let { add(it.latitude) }
                withLocation.forEach { m -> m.lastLocation?.let { add(it.latitude) } }
            }
            val lngs = buildList {
                currentLocation?.let { add(it.longitude) }
                withLocation.forEach { m -> m.lastLocation?.let { add(it.longitude) } }
            }
            if (lats.isNotEmpty() && lngs.isNotEmpty()) {
                val centerLat = (lats.min() + lats.max()) / 2.0
                val centerLng = (lngs.min() + lngs.max()) / 2.0
                // Rough zoom: shrink by 1 step for every 0.1° span (very approximate).
                val span = maxOf(lats.max() - lats.min(), lngs.max() - lngs.min())
                val zoom = (14.0 - span * 5.0).coerceIn(5.0, 14.0)
                mapViewportState.flyTo(
                        cameraOptions = CameraOptions.Builder()
                                .center(Point.fromLngLat(centerLng, centerLat))
                                .zoom(zoom)
                                .build(),
                        animationOptions =
                                com.mapbox.maps.plugin.animation.MapAnimationOptions
                                        .mapAnimationOptions { duration(1200L) }
                )
            }
        }
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

            // ── Mapbox map ────────────────────────────────────────────────────
            MapboxMap(
                    modifier = Modifier.fillMaxSize(),
                    mapViewportState = mapViewportState,
                    style = { MapStyle(style = com.mapbox.maps.Style.STANDARD) },
            ) {
                // Configure location puck on first load and re-apply when permission changes.
                MapEffect(hasLocationPermission) { mapView ->
                    mapView.location.updateSettings {
                        enabled = hasLocationPermission
                        locationPuck = createDefault2DPuck(withBearing = true)
                    }
                    DebugLogger.i("MapScreen") { "Mapbox map ready, puck enabled=$hasLocationPermission" }
                }

                // Phase 8: Render a PointAnnotation for each team member who has a known location.
                members.forEach { member ->
                    val loc = member.lastLocation ?: return@forEach
                    PointAnnotation(
                            point = Point.fromLngLat(loc.longitude, loc.latitude)
                    ) {
                        // Mapbox will render a default pin; textField shows the member name.
                        textField = member.displayName ?: "Member"
                        textSize = 12.0
                        textOffset = listOf(0.0, 2.0)
                    }
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
