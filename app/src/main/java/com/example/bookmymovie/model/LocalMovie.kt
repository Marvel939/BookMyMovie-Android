package com.example.bookmymovie.model

/**
 * Represents a movie stored in Firebase Realtime Database.
 * Maps to: movies/{movieId}
 *
 * This is separate from the TMDB-based Movie model used elsewhere.
 * It represents movies available for booking in the local system.
 */
data class LocalMovie(
    val movieId: String = "",
    val title: String = "",
    val posterUrl: String = "",
    val bannerUrl: String = "",
    val genre: List<String> = emptyList(),
    val releaseDate: String = "",       // ISO format: "2026-02-14"
    val rating: Double = 0.0,
    val popularityScore: Double = 0.0,
    val duration: String = "",           // e.g. "2h 30m"
    val language: String = "",
    val description: String = "",
    val certification: String = "",      // e.g. "UA", "A", "U"
    val directors: List<String> = emptyList(),
    val cast: List<String> = emptyList(),
    val trailerUrl: String = ""
) {
    constructor() : this(
        "", "", "", "", emptyList(), "", 0.0, 0.0,
        "", "", "", "", emptyList(), emptyList(), ""
    )

    /**
     * Check if this movie was released within the last [days] days.
     */
    fun isNewRelease(days: Int = 7): Boolean {
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val releaseTime = sdf.parse(releaseDate)?.time ?: return false
            val now = System.currentTimeMillis()
            val diffDays = (now - releaseTime) / (1000 * 60 * 60 * 24)
            diffDays in 0..days
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if this movie is currently showing (released and not older than 60 days).
     */
    fun isNowShowing(): Boolean {
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val releaseTime = sdf.parse(releaseDate)?.time ?: return false
            val now = System.currentTimeMillis()
            val diffDays = (now - releaseTime) / (1000 * 60 * 60 * 24)
            diffDays in 0..60
        } catch (e: Exception) {
            false
        }
    }
}
