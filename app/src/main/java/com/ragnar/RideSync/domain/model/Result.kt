package com.ragnar.RideSync.domain.model

/**
 * Sealed class representing the result of an operation. Used across repositories and ViewModels for
 * consistent error handling.
 *
 * @param T The type of the successful result data.
 */
sealed class Result<out T> {
    /** Operation completed successfully with [data]. */
    data class Success<out T>(val data: T) : Result<T>()

    /** Operation failed with an [exception] and optional [message]. */
    data class Error(val exception: Throwable? = null, val message: String? = null) :
            Result<Nothing>()

    /** Operation is currently in progress. */
    data object Loading : Result<Nothing>()
}
