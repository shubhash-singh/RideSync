
package com.ragnar.RideSync.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.ragnar.RideSync.domain.model.Result
import com.ragnar.RideSync.domain.repository.AuthRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase implementation of AuthRepository.
 * Handles Google Sign-In via Firebase Auth credentials and observes auth state changes.
 */
@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : AuthRepository {

    override val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser

    override val isUserAuthenticated: Boolean
        get() = firebaseAuth.currentUser != null

    override fun signInWithGoogle(idToken: String): Flow<Result<FirebaseUser>> = flow {
        emit(Result.Loading)
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val user = authResult.user
            if (user != null) {
                emit(Result.Success(user))
            } else {
                emit(Result.Error(message = "Sign-in succeeded but user is null"))
            }
        } catch (e: Exception) {
            emit(Result.Error(exception = e, message = e.localizedMessage))
        }
    }

    override fun signOut(): Flow<Result<Unit>> = flow {
        emit(Result.Loading)
        try {
            firebaseAuth.signOut()
            emit(Result.Success(Unit))
        } catch (e: Exception) {
            emit(Result.Error(exception = e, message = e.localizedMessage))
        }
    }

    override fun getAuthStateFlow(): Flow<Boolean> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser != null)
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }
}
