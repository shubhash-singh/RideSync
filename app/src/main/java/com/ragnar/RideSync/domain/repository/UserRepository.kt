package com.ragnar.RideSync.domain.repository

import com.ragnar.RideSync.domain.model.Result
import com.ragnar.RideSync.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for user profile operations stored in Firestore.
 *
 * Phase 4:
 * - Create/update user profile on sign-in.
 * - Observe profile changes in real time via Flow.
 */
interface UserRepository : BaseRepository {

    /** Creates or updates the user profile document using merge semantics. */
    fun upsertUser(user: User): Flow<Result<Unit>>

    /** Observes a user profile document in real time. Emits Success(null) when doc is missing. */
    fun observeUser(userId: String): Flow<Result<User?>>
}

