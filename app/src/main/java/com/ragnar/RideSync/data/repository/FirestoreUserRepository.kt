package com.ragnar.RideSync.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.ragnar.RideSync.data.model.UserDto
import com.ragnar.RideSync.data.model.toDomain
import com.ragnar.RideSync.data.model.toDto
import com.ragnar.RideSync.domain.model.Result
import com.ragnar.RideSync.domain.model.User
import com.ragnar.RideSync.domain.repository.UserRepository
import com.ragnar.RideSync.utils.Constants
import com.ragnar.RideSync.utils.DebugLogger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

/**
 * Firestore implementation of [UserRepository].
 *
 * Stores user profile documents at: users/{userId}
 */
@Singleton
class FirestoreUserRepository @Inject constructor(
        private val firestore: FirebaseFirestore
) : UserRepository {

    private companion object {
        private const val TAG = "FirestoreUserRepo"
    }

    override fun upsertUser(user: User): Flow<Result<Unit>> = flow {
        DebugLogger.d(TAG) { "upsertUser(uid=${user.id}) start" }
        emit(Result.Loading)
        try {
            val dto = user.toDto()
            val data = dto.toMergeMap()

            firestore.collection(Constants.COLLECTION_USERS).document(user.id).set(data, SetOptions.merge()).await()

            DebugLogger.i(TAG) { "upsertUser(uid=${user.id}) success" }
            emit(Result.Success(Unit))
        } catch (e: Exception) {
            DebugLogger.e(TAG, e) { "upsertUser(uid=${user.id}) failed: ${e.localizedMessage}" }
            emit(Result.Error(exception = e, message = e.localizedMessage))
        }
    }

    override fun observeUser(userId: String): Flow<Result<User?>> = callbackFlow {
        DebugLogger.d(TAG) { "observeUser(uid=$userId) start" }
        trySend(Result.Loading)

        val registration =
                firestore.collection(Constants.COLLECTION_USERS).document(userId).addSnapshotListener {
                        snapshot,
                        error ->
                    if (error != null) {
                        DebugLogger.e(TAG, error) { "observeUser(uid=$userId) listener error: ${error.localizedMessage}" }
                        trySend(Result.Error(exception = error, message = error.localizedMessage))
                        return@addSnapshotListener
                    }

                    if (snapshot == null || !snapshot.exists()) {
                        DebugLogger.w(TAG) { "observeUser(uid=$userId) document missing" }
                        trySend(Result.Success(null))
                        return@addSnapshotListener
                    }

                    val dto = snapshot.toObject(UserDto::class.java)
                    val user = dto?.toDomain(userId)
                    DebugLogger.d(TAG) { "observeUser(uid=$userId) snapshot update (mapped=${user != null})" }
                    trySend(Result.Success(user))
                }

        awaitClose { registration.remove() }
    }

    /**
     * Builds a Firestore map for SetOptions.merge() that does not overwrite existing values with
     * null/blank strings.
     */
    private fun UserDto.toMergeMap(): Map<String, Any> {
        val data = mutableMapOf<String, Any>()

        displayName?.takeIf { it.isNotBlank() }?.let { data["displayName"] = it }
        email?.takeIf { it.isNotBlank() }?.let { data["email"] = it }
        photoUrl?.takeIf { it.isNotBlank() }?.let { data["photoUrl"] = it }
        currentTeamId?.takeIf { it.isNotBlank() }?.let { data["currentTeamId"] = it }
        fcmToken?.takeIf { it.isNotBlank() }?.let { data["fcmToken"] = it }

        val loc = lastLocation
        val lat = loc?.lat
        val lng = loc?.lng
        if (lat != null && lng != null) {
            val locMap = mutableMapOf<String, Any>("lat" to lat, "lng" to lng)
            loc.updatedAt?.let { locMap["updatedAt"] = it }
            data["lastLocation"] = locMap
        }

        return data
    }

    /**
     * Phase 12: Persists the FCM registration token to `users/{userId}.fcmToken`.
     * Uses a targeted field update so we never overwrite the rest of the user document.
     */
    override suspend fun storeFcmToken(userId: String, token: String) {
        DebugLogger.d(TAG) { "storeFcmToken(uid=$userId)" }
        try {
            firestore
                .collection(Constants.COLLECTION_USERS)
                .document(userId)
                .update("fcmToken", token)
                .await()
            DebugLogger.i(TAG) { "storeFcmToken success for uid=$userId" }
        } catch (e: Exception) {
            DebugLogger.e(TAG, e) { "storeFcmToken failed for uid=$userId: ${e.localizedMessage}" }
        }
    }
}
