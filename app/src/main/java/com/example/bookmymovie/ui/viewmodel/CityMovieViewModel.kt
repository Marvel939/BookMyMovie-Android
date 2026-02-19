package com.example.bookmymovie.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookmymovie.firebase.FirebaseMovieRepository
import com.example.bookmymovie.firebase.UserRepository
import com.example.bookmymovie.model.LocalMovie
import com.example.bookmymovie.model.Theatre
import com.example.bookmymovie.model.UserPreference
import com.example.bookmymovie.repository.RecommendationEngine
import com.example.bookmymovie.utils.LocationHelper
import kotlinx.coroutines.launch

/**
 * ViewModel for the location-based movie suggestion system.
 * Fetches theatres, movies, and showtimes for the selected city,
 * then applies the recommendation engine to categorize movies.
 */
class CityMovieViewModel(application: Application) : AndroidViewModel(application) {

    private val locationHelper = LocationHelper(application)

    // ─── UI STATE ────────────────────────────────────────────

    /** Banner carousel movies (top 5 recommended) */
    var bannerMovies by mutableStateOf<List<LocalMovie>>(emptyList())
        private set

    /** Now showing in user's city */
    var nowShowingMovies by mutableStateOf<List<LocalMovie>>(emptyList())
        private set

    /** New releases (last 7 days) */
    var newReleases by mutableStateOf<List<LocalMovie>>(emptyList())
        private set

    /** Trending in user's city (by popularity) */
    var trendingMovies by mutableStateOf<List<LocalMovie>>(emptyList())
        private set

    /** Personalized recommendations */
    var recommendedMovies by mutableStateOf<List<LocalMovie>>(emptyList())
        private set

    /** Theatres in user's city sorted by distance */
    var nearbyTheatres by mutableStateOf<List<Pair<Theatre, Double>>>(emptyList())
        private set

    /** All theatres in city (unsorted) */
    var cityTheatres by mutableStateOf<List<Theatre>>(emptyList())
        private set

    /** All movies currently available in city */
    var allCityMovies by mutableStateOf<List<LocalMovie>>(emptyList())
        private set

    /** Current city being displayed */
    var currentCity by mutableStateOf<String?>(null)
        private set

    /** Loading state */
    var isLoading by mutableStateOf(false)
        private set

    /** Error message */
    var errorMessage by mutableStateOf<String?>(null)
        private set

    /** User preferences */
    var userPreference by mutableStateOf(UserPreference())
        private set

    // ─── DATA LOADING ────────────────────────────────────────

    /**
     * Load all movie data for a given city.
     * This is the main entry point — call when city changes.
     */
    fun loadDataForCity(city: String) {
        if (city == currentCity && allCityMovies.isNotEmpty()) return // Already loaded

        currentCity = city
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                // 1. Fetch user preferences
                userPreference = try {
                    UserRepository.getUserPreferences()
                } catch (e: Exception) {
                    UserPreference()
                }

                // 2. Fetch theatres in city
                val theatres = FirebaseMovieRepository.getTheatresInCity(city)
                cityTheatres = theatres

                // 3. Sort theatres by distance
                nearbyTheatres = locationHelper.sortTheatresByDistance(theatres)

                // 4. Fetch movie IDs from showtimes
                val movieIds = FirebaseMovieRepository.getMovieIdsInCity(city)

                // 5. Fetch movie details
                val movies = if (movieIds.isNotEmpty()) {
                    FirebaseMovieRepository.getMoviesByIds(movieIds.toList())
                } else {
                    // Fallback: fetch all movies if no showtimes configured
                    FirebaseMovieRepository.getAllMovies()
                }
                allCityMovies = movies

                // 6. Apply recommendation engine to categorize
                categorizeMovies(movies, userPreference)

            } catch (e: Exception) {
                errorMessage = "Failed to load movies: ${e.message}"
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * Refresh data for the current city.
     */
    fun refresh() {
        val city = currentCity ?: return
        currentCity = null // Force reload
        loadDataForCity(city)
    }

    /**
     * Categorize movies into UI sections using the recommendation engine.
     */
    private fun categorizeMovies(movies: List<LocalMovie>, prefs: UserPreference) {
        // Banner: top 5 by combined score
        bannerMovies = RecommendationEngine.getBannerMovies(movies, prefs, limit = 5)

        // Now Showing: currently running movies
        nowShowingMovies = RecommendationEngine.getNowShowing(movies, limit = 20)

        // New Releases: released in last 7 days
        newReleases = RecommendationEngine.getNewReleases(movies, onlyRecent = true, limit = 10)

        // Trending: by popularity score
        trendingMovies = RecommendationEngine.getTrending(movies, limit = 10)

        // Recommended: personalized
        recommendedMovies = RecommendationEngine.getRecommended(movies, prefs, limit = 10)
    }

    /**
     * Get movies for a specific genre.
     */
    fun getMoviesByGenre(genre: String): List<LocalMovie> {
        return RecommendationEngine.getByGenre(allCityMovies, genre)
    }

    /**
     * Get all unique genres from current city movies.
     */
    fun getAvailableGenres(): List<String> {
        return allCityMovies
            .flatMap { it.genre }
            .distinct()
            .sorted()
    }
}
