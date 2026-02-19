package com.example.bookmymovie.repository

import com.example.bookmymovie.model.LocalMovie
import com.example.bookmymovie.model.ScoredMovie
import com.example.bookmymovie.model.UserPreference

/**
 * Recommendation engine that scores and ranks movies based on:
 *   - Popularity score (weight: 0.5)
 *   - New release status (weight: 0.3)
 *   - User genre preference match (weight: 0.2)
 *
 * Formula:
 *   score = (normalizedPopularity * 0.5)
 *         + (isNewRelease * 0.3)
 *         + (genreMatchRatio * 0.2)
 */
object RecommendationEngine {

    private const val WEIGHT_POPULARITY = 0.5
    private const val WEIGHT_NEW_RELEASE = 0.3
    private const val WEIGHT_GENRE_MATCH = 0.2

    /**
     * Score and rank all movies based on the recommendation algorithm.
     *
     * @param movies All movies available in the user's city
     * @param userPreference The user's preference data (genre history)
     * @return List of ScoredMovie sorted by score descending
     */
    fun rankMovies(
        movies: List<LocalMovie>,
        userPreference: UserPreference
    ): List<ScoredMovie> {
        if (movies.isEmpty()) return emptyList()

        val maxPopularity = movies.maxOf { it.popularityScore }.coerceAtLeast(1.0)
        val topGenres = userPreference.topGenres(limit = 5)

        return movies.map { movie ->
            val popularityComponent = normalizePopularity(movie.popularityScore, maxPopularity)
            val newReleaseComponent = if (movie.isNewRelease(days = 7)) 1.0 else 0.0
            val genreMatchComponent = calculateGenreMatch(movie.genre, topGenres)

            val score = (popularityComponent * WEIGHT_POPULARITY) +
                    (newReleaseComponent * WEIGHT_NEW_RELEASE) +
                    (genreMatchComponent * WEIGHT_GENRE_MATCH)

            ScoredMovie(
                movie = movie,
                score = score,
                popularityComponent = popularityComponent,
                newReleaseComponent = newReleaseComponent,
                genreMatchComponent = genreMatchComponent
            )
        }.sortedByDescending { it.score }
    }

    /**
     * Get recommended movies (personalized based on genre history).
     */
    fun getRecommended(
        movies: List<LocalMovie>,
        userPreference: UserPreference,
        limit: Int = 10
    ): List<LocalMovie> {
        return rankMovies(movies, userPreference)
            .take(limit)
            .map { it.movie }
    }

    /**
     * Get movies sorted by release date descending (newest first).
     * Optionally filter to only new releases (last N days).
     */
    fun getNewReleases(
        movies: List<LocalMovie>,
        onlyRecent: Boolean = true,
        recentDays: Int = 7,
        limit: Int = 10
    ): List<LocalMovie> {
        val filtered = if (onlyRecent) {
            movies.filter { it.isNewRelease(recentDays) }
        } else {
            movies
        }
        return filtered
            .sortedByDescending { it.releaseDate }
            .take(limit)
    }

    /**
     * Get trending movies sorted by popularity score.
     */
    fun getTrending(
        movies: List<LocalMovie>,
        limit: Int = 10
    ): List<LocalMovie> {
        return movies
            .sortedByDescending { it.popularityScore }
            .take(limit)
    }

    /**
     * Get currently showing movies (released within 60 days).
     */
    fun getNowShowing(
        movies: List<LocalMovie>,
        limit: Int = 20
    ): List<LocalMovie> {
        return movies
            .filter { it.isNowShowing() }
            .sortedByDescending { it.popularityScore }
            .take(limit)
    }

    /**
     * Get top banner movies (top 5 by combined score).
     */
    fun getBannerMovies(
        movies: List<LocalMovie>,
        userPreference: UserPreference,
        limit: Int = 5
    ): List<LocalMovie> {
        return rankMovies(movies, userPreference)
            .take(limit)
            .map { it.movie }
    }

    /**
     * Get movies filtered by a specific genre.
     */
    fun getByGenre(
        movies: List<LocalMovie>,
        genre: String,
        limit: Int = 10
    ): List<LocalMovie> {
        return movies
            .filter { genre in it.genre }
            .sortedByDescending { it.popularityScore }
            .take(limit)
    }

    // ─── INTERNAL SCORING HELPERS ────────────────────────────

    /**
     * Normalize popularity score to 0.0 - 1.0 range.
     */
    private fun normalizePopularity(score: Double, maxScore: Double): Double {
        return if (maxScore > 0) (score / maxScore).coerceIn(0.0, 1.0) else 0.0
    }

    /**
     * Calculate how well a movie's genres match the user's preferred genres.
     * Returns a ratio from 0.0 to 1.0.
     *
     * If user has no genre preferences, returns 0.5 (neutral).
     */
    private fun calculateGenreMatch(
        movieGenres: List<String>,
        userTopGenres: List<String>
    ): Double {
        if (userTopGenres.isEmpty()) return 0.5 // Neutral if no history
        if (movieGenres.isEmpty()) return 0.0

        val matchCount = movieGenres.count { genre ->
            userTopGenres.any { it.equals(genre, ignoreCase = true) }
        }
        return (matchCount.toDouble() / movieGenres.size).coerceIn(0.0, 1.0)
    }
}
