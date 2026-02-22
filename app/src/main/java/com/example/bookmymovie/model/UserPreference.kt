package com.example.bookmymovie.model

/**
 * Represents user preferences and booking history for personalization.
 * Maps to: users/{userId}/preferences
 */
data class UserPreference(
    val preferredGenres: Map<String, Int> = emptyMap(), // genre -> watch count
    val preferredLanguages: List<String> = emptyList(),
    val lastCity: String = "",
    val favoriteTheatres: List<String> = emptyList()
) {
    constructor() : this(emptyMap(), emptyList(), "", emptyList())

    /**
     * Returns genres sorted by watch count descending.
     */
    fun topGenres(limit: Int = 3): List<String> {
        return preferredGenres.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key }
    }
}

/**
 * Represents a single booking record.
 * Maps to: users/{userId}/bookings/{bookingId}
 */
data class BookingRecord(
    val bookingId: String = "",
    val movieId: String = "",
    val movieTitle: String = "",
    val theatreId: String = "",
    val theatreName: String = "",
    val city: String = "",
    val date: String = "",
    val time: String = "",
    val seats: Int = 1,
    val totalAmount: Double = 0.0,
    val genre: List<String> = emptyList(),
    val bookedAt: Long = 0L
) {
    constructor() : this("", "", "", "", "", "", "", "", 1, 0.0, emptyList(), 0L)
}

/**
 * Scored movie used by the recommendation engine.
 */
data class ScoredMovie(
    val movie: LocalMovie,
    val score: Double,
    val popularityComponent: Double,
    val newReleaseComponent: Double,
    val genreMatchComponent: Double
)
