package com.ragnar.RideSync

import android.app.Application
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.mapbox.common.MapboxOptions
import com.ragnar.RideSync.data.repository.FirestoreUserRepository
import com.ragnar.RideSync.utils.DebugLogger
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * RideSync Application class.
 * Annotated with @HiltAndroidApp to trigger Hilt's code generation
 * and serve as the application-level dependency container.
 *
 * Mapbox SDK v11 requires the access token to be set programmatically
 * before the first MapView is inflated. Application.onCreate() is the
 * earliest safe point for this — it runs before any Activity/Compose code.
 *
 * Phase 12: On every launch we fetch the current FCM registration token and
 * persist it to Firestore, ensuring it stays fresh after token rotation.
 */
@HiltAndroidApp
class RideSyncApplication : Application() {

    @Inject lateinit var userRepository: FirestoreUserRepository

    /**
     * Application-scoped coroutine scope for one-shot startup work.
     * Uses SupervisorJob so a failed child doesn't cancel siblings.
     */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // Initialise Mapbox with the public access token from BuildConfig.
        // The token value is injected at build time from local.properties → MAPBOX_ACCESS_TOKEN.
        MapboxOptions.accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN
        DebugLogger.d("RideSyncApplication") { "onCreate — Mapbox token configured" }

        // Phase 12: Keep FCM token in Firestore up-to-date.
        refreshFcmToken()
    }

    /**
     * Fetches the latest FCM registration token and writes it to the current user's Firestore
     * document. This covers the case where the token was rotated while the app was not running.
     * The [RideSyncMessagingService.onNewToken] callback handles in-session rotations.
     */
    private fun refreshFcmToken() {
        appScope.launch {
            try {
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
                    DebugLogger.d("RideSyncApplication") {
                        "refreshFcmToken — no signed-in user, skipping"
                    }
                    return@launch
                }
                val token = FirebaseMessaging.getInstance().token.await()
                DebugLogger.d("RideSyncApplication") {
                    "FCM token fetched (${token.take(20)}…) for uid=$uid"
                }
                userRepository.storeFcmToken(uid, token)
            } catch (e: Exception) {
                DebugLogger.e("RideSyncApplication", e) {
                    "refreshFcmToken failed: ${e.localizedMessage}"
                }
            }
        }
    }
}
