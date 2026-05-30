package com.ragnar.RideSync.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ragnar.RideSync.R
import com.ragnar.RideSync.ui.MainActivity
import com.ragnar.RideSync.utils.Constants
import com.ragnar.RideSync.utils.DebugLogger

/**
 * Phase 12 — Firebase Cloud Messaging receiver.
 *
 * Handles two FCM callbacks:
 *  1. [onNewToken] — persists the refreshed FCM token to Firestore so other clients and future
 *     Cloud Functions can send targeted push messages to this device.
 *  2. [onMessageReceived] — displays a local [NotificationCompat] notification when a data
 *     message arrives. Expected payload keys:
 *       - `event`   → "member_joined" | "destination_changed" | "team_disbanded"
 *       - `name`    → display name of the member (for member_joined)
 *       - `address` → destination address (for destination_changed)
 *
 * Topic subscriptions (Option C):
 *   The client subscribes to `team_{teamId}` on join and unsubscribes on leave/disband.
 *   This means any future Cloud Function (or direct FCM call) can fan-out to the whole team
 *   by targeting that topic — no device token enumeration required.
 */
class RideSyncMessagingService : FirebaseMessagingService() {

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    // ── FCM token refresh ─────────────────────────────────────────────────────

    /**
     * Called whenever the FCM token is refreshed (first install, app clear-data, token rotation).
     * We write it to `users/{userId}.fcmToken` so other clients can look it up.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        DebugLogger.d(TAG) { "onNewToken: token=${token.take(20)}…" }
        val uid = auth.currentUser?.uid ?: run {
            DebugLogger.w(TAG) { "onNewToken — no signed-in user; token will be stored on next sign-in" }
            return
        }
        storeFcmToken(uid, token)
    }

    // ── Message handling ──────────────────────────────────────────────────────

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        DebugLogger.d(TAG) { "onMessageReceived: data=${message.data}" }

        val event   = message.data["event"]   ?: return
        val name    = message.data["name"]    ?: "Someone"
        val address = message.data["address"] ?: "a new location"

        val (title, body) = when (event) {
            EVENT_MEMBER_JOINED      -> "👋 Member joined" to "$name joined your team."
            EVENT_DESTINATION_CHANGED -> "📍 Destination updated" to "New destination: $address"
            EVENT_TEAM_DISBANDED     -> "Team disbanded" to "Your leader has disbanded the team."
            else -> {
                DebugLogger.w(TAG) { "Unknown FCM event: $event" }
                return
            }
        }

        showNotification(title, body)
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun storeFcmToken(userId: String, token: String) {
        firestore
            .collection(Constants.COLLECTION_USERS)
            .document(userId)
            .update("fcmToken", token)
            .addOnSuccessListener {
                DebugLogger.d(TAG) { "FCM token stored for uid=$userId" }
            }
            .addOnFailureListener { e ->
                DebugLogger.e(TAG, e) { "Failed to store FCM token: ${e.localizedMessage}" }
            }
    }

    private fun showNotification(title: String, body: String) {
        ensureNotificationChannel()

        // Tapping the notification opens MainActivity.
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, FCM_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openIntent)
            .build()

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(FCM_NOTIFICATION_ID_BASE + System.currentTimeMillis().toInt(), notification)
    }

    private fun ensureNotificationChannel() {
        val channel = NotificationChannel(
            FCM_CHANNEL_ID,
            "Team Notifications",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications for team events: member joins, destination changes, and team disbanded."
        }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    // ── Companion ─────────────────────────────────────────────────────────────

    companion object {
        private const val TAG = "RideSyncMsgService"

        const val FCM_CHANNEL_ID = "team_notifications"

        /** Base ID; we add timestamp LSBs so multiple notifications don't overwrite each other. */
        private const val FCM_NOTIFICATION_ID_BASE = 2000

        // FCM data-message event keys (must match whatever the server/Cloud Function sends).
        const val EVENT_MEMBER_JOINED       = "member_joined"
        const val EVENT_DESTINATION_CHANGED = "destination_changed"
        const val EVENT_TEAM_DISBANDED      = "team_disbanded"

        /** FCM topic name for a given team. All members subscribe on join, unsubscribe on leave. */
        fun teamTopic(teamId: String) = "team_$teamId"
    }
}
