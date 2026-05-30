package com.ragnar.RideSync.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ragnar.RideSync.R
import com.ragnar.RideSync.domain.model.User
import com.ragnar.RideSync.domain.model.UserLocation
import com.ragnar.RideSync.ui.MainActivity
import com.ragnar.RideSync.utils.Constants
import com.ragnar.RideSync.utils.DebugLogger

/**
 * Phase 11 — Background Location Service.
 *
 * A foreground [Service] that keeps [FusedLocationProviderClient] running while the app is
 * minimised or the screen is locked. Every location fix is written directly to Firestore
 * (`users/{userId}.lastLocation`) so team members continue to see live positions.
 *
 * Lifecycle:
 *  - Start: send Intent with action [ACTION_START] (issued by [LocationViewModel] when the user
 *    joins a team and location permission is granted).
 *  - Stop: send Intent with action [ACTION_STOP] **or** tap the "Stop" action on the notification.
 *
 * Note: This service deliberately avoids Hilt injection to keep the service tier thin. Firebase
 * singletons are obtained via their static `getInstance()` methods — the same instances Hilt
 * provides everywhere else (Firebase SDKs are themselves singletons).
 */
class LocationForegroundService : Service() {

    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        DebugLogger.d(TAG) { "onCreate" }
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                DebugLogger.d(TAG) {
                    "Background fix: lat=${location.latitude} lng=${location.longitude}"
                }
                pushToFirestore(location.latitude, location.longitude)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                DebugLogger.i(TAG) { "ACTION_START received" }
                startForeground(NOTIFICATION_ID, buildNotification())
                requestLocationUpdates()
            }
            ACTION_STOP -> {
                DebugLogger.i(TAG) { "ACTION_STOP received" }
                stopSelf()
            }
        }
        // If the system kills the service, don't restart — the foreground flow handles it when
        // the app comes back.
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        DebugLogger.d(TAG) { "onDestroy — removing location updates" }
        fusedClient.removeLocationUpdates(locationCallback)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Location ──────────────────────────────────────────────────────────────

    @Suppress("MissingPermission") // Caller (LocationViewModel) ensures permission is granted.
    private fun requestLocationUpdates() {
        val request = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            BACKGROUND_UPDATE_INTERVAL_MS
        )
            .setMinUpdateIntervalMillis(BACKGROUND_FASTEST_INTERVAL_MS)
            .setWaitForAccurateLocation(false)
            .build()

        try {
            fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
            DebugLogger.d(TAG) { "Location updates registered (background)" }
        } catch (e: SecurityException) {
            DebugLogger.e(TAG, e) { "Missing location permission in service: ${e.localizedMessage}" }
            stopSelf()
        }
    }

    private fun pushToFirestore(lat: Double, lng: Double) {
        val uid = auth.currentUser?.uid ?: run {
            DebugLogger.w(TAG) { "pushToFirestore — no signed-in user, skipping" }
            return
        }
        val locationMap = mapOf(
            "latitude" to lat,
            "longitude" to lng,
            "updatedAt" to System.currentTimeMillis()
        )
        firestore
            .collection(Constants.COLLECTION_USERS)
            .document(uid)
            .update("lastLocation", locationMap)
            .addOnFailureListener { e ->
                DebugLogger.e(TAG, e) { "Firestore background write failed: ${e.localizedMessage}" }
            }
    }

    // ── Notification ──────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Ride Tracking",
            NotificationManager.IMPORTANCE_LOW // Low = no sound, no pop-up
        ).apply {
            description = "Shown while RideSync is tracking your location in the background."
            setShowBadge(false)
        }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        // Tapping the notification opens the app (MainActivity).
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // "Stop" action — sends ACTION_STOP back to this service.
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, LocationForegroundService::class.java).apply {
                action = ACTION_STOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return android.app.Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("RideSync · Tracking your ride")
            .setContentText("Your location is being shared with your team.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .addAction(
                android.app.Notification.Action.Builder(
                    null, // no icon needed for action
                    "Stop",
                    stopIntent
                ).build()
            )
            .build()
    }

    // ── Companion ─────────────────────────────────────────────────────────────

    companion object {
        private const val TAG = "LocationFgService"

        const val CHANNEL_ID = "ride_tracking"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.ragnar.RideSync.ACTION_START_LOCATION"
        const val ACTION_STOP  = "com.ragnar.RideSync.ACTION_STOP_LOCATION"

        /** 10-second interval when in background (balances accuracy vs. battery). */
        private const val BACKGROUND_UPDATE_INTERVAL_MS = 10_000L
        private const val BACKGROUND_FASTEST_INTERVAL_MS = 5_000L

        /** Convenience builder — starts the foreground location service. */
        fun startIntent(context: Context): Intent =
            Intent(context, LocationForegroundService::class.java).apply {
                action = ACTION_START
            }

        /** Convenience builder — stops the foreground location service. */
        fun stopIntent(context: Context): Intent =
            Intent(context, LocationForegroundService::class.java).apply {
                action = ACTION_STOP
            }
    }
}
