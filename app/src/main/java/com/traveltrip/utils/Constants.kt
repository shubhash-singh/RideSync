package com.traveltrip.utils

/** Application-wide constants. */
object Constants {
    /** Firestore collection names */
    const val COLLECTION_USERS = "users"
    const val COLLECTION_TEAMS = "teams"
    const val COLLECTION_MEMBERS = "members"

    /** Location update intervals (milliseconds) */
    const val LOCATION_UPDATE_INTERVAL = 5_000L
    const val LOCATION_FASTEST_INTERVAL = 2_000L

    /** Team code length */
    const val TEAM_CODE_LENGTH = 6
}
