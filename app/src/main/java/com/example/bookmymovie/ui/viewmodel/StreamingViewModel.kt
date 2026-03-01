package com.example.bookmymovie.ui.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookmymovie.data.repository.StreamingRepository
import com.example.bookmymovie.model.LibraryItem
import com.example.bookmymovie.model.StreamingMovie
import kotlinx.coroutines.launch

class StreamingViewModel : ViewModel() {

    companion object {
        private const val TAG = "StreamingViewModel"
    }

    // ── Catalog ─────────────────────────────────────────────────────────────
    var allMovies by mutableStateOf<List<StreamingMovie>>(emptyList())
        private set
    var filteredMovies by mutableStateOf<List<StreamingMovie>>(emptyList())
        private set
    var highlightMovies by mutableStateOf<List<StreamingMovie>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set

    // ── Filters ─────────────────────────────────────────────────────────────
    var selectedPlatform by mutableStateOf<String?>(null)
        private set
    var selectedGenre by mutableStateOf<String?>(null)
        private set
    var searchQuery by mutableStateOf("")
        private set

    // ── Library ─────────────────────────────────────────────────────────────
    var library by mutableStateOf<List<LibraryItem>>(emptyList())
        private set
    var isLoadingLibrary by mutableStateOf(false)
        private set

    // ── Detail ──────────────────────────────────────────────────────────────
    var selectedMovie by mutableStateOf<StreamingMovie?>(null)
        private set
    var isLoadingDetail by mutableStateOf(false)
        private set

    // IMPORTANT: Default is null = NOT owned. Only set to non-null after Firebase confirms ownership.
    var isMovieOwned by mutableStateOf<LibraryItem?>(null)
        private set
    var isCheckingOwnership by mutableStateOf(true)
        private set

    // ── Purchase ────────────────────────────────────────────────────────────
    var isPurchasing by mutableStateOf(false)
        private set
    var purchaseSuccess by mutableStateOf(false)
        private set
    var purchaseError by mutableStateOf<String?>(null)
        private set

    val availablePlatforms: List<String>
        get() = allMovies.map { it.ottPlatform }.distinct().sorted()

    val availableGenres: List<String>
        get() = allMovies.flatMap { it.genre.split(",", "/", "&").map { g -> g.trim() } }
            .filter { it.isNotBlank() }.distinct().sorted()

    init {
        loadCatalog()
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CATALOG
    // ═══════════════════════════════════════════════════════════════════════

    fun loadCatalog() {
        viewModelScope.launch {
            isLoading = true
            try {
                allMovies = StreamingRepository.getAllMovies()
                highlightMovies = allMovies.take(6)
                applyFilters()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load catalog", e)
            } finally {
                isLoading = false
            }
        }
    }

    fun setFilter(platform: String? = selectedPlatform, genre: String? = selectedGenre) {
        selectedPlatform = platform
        selectedGenre = genre
        applyFilters()
    }

    fun clearFilters() {
        selectedPlatform = null
        selectedGenre = null
        searchQuery = ""
        applyFilters()
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
        applyFilters()
    }

    private fun applyFilters() {
        filteredMovies = allMovies.filter { movie ->
            val matchesPlatform = selectedPlatform == null || movie.ottPlatform == selectedPlatform
            val matchesGenre = selectedGenre == null || movie.genre.contains(selectedGenre!!, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                movie.title.contains(searchQuery, ignoreCase = true) ||
                movie.director.contains(searchQuery, ignoreCase = true) ||
                movie.cast.any { it.contains(searchQuery, ignoreCase = true) }
            matchesPlatform && matchesGenre && matchesSearch
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DETAIL — Reset ownership FIRST, then check Firebase
    // ═══════════════════════════════════════════════════════════════════════

    fun loadMovieDetail(movieId: String) {
        viewModelScope.launch {
            // CRITICAL: Reset ownership to null BEFORE checking Firebase
            // This prevents showing "Watch Now" for movies user doesn't own
            isMovieOwned = null
            isCheckingOwnership = true
            isLoadingDetail = true

            try {
                // Try local first, then fetch from Firebase
                selectedMovie = allMovies.find { it.movieId == movieId }
                    ?: StreamingRepository.getMovieById(movieId)

                // Now check Firebase — will return null if user doesn't own this movie
                val libraryItem = StreamingRepository.isMovieInLibrary(movieId)
                isMovieOwned = libraryItem  // null = not owned, non-null = owned

                Log.d(TAG, "Movie $movieId - owned: ${libraryItem != null}, type: ${libraryItem?.type}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load detail for $movieId", e)
                isMovieOwned = null  // On error, treat as not owned
            } finally {
                isLoadingDetail = false
                isCheckingOwnership = false
            }
        }
    }

    fun recheckOwnership(movieId: String) {
        viewModelScope.launch {
            isCheckingOwnership = true
            try {
                val libraryItem = StreamingRepository.isMovieInLibrary(movieId)
                isMovieOwned = libraryItem
                Log.d(TAG, "Recheck movie $movieId - owned: ${libraryItem != null}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to recheck ownership", e)
            } finally {
                isCheckingOwnership = false
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // LIBRARY
    // ═══════════════════════════════════════════════════════════════════════

    fun loadLibrary() {
        viewModelScope.launch {
            isLoadingLibrary = true
            try {
                library = StreamingRepository.getUserLibrary()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load library", e)
            } finally {
                isLoadingLibrary = false
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PURCHASE
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Called ONLY after Stripe payment succeeds.
     * Records the purchase in Firebase and refreshes ownership.
     */
    fun completePurchase(movie: StreamingMovie, type: String, paymentIntentId: String) {
        viewModelScope.launch {
            isPurchasing = true
            purchaseError = null
            purchaseSuccess = false
            try {
                StreamingRepository.recordPurchase(
                    movie = movie,
                    type = type,
                    paymentIntentId = paymentIntentId
                )

                purchaseSuccess = true
                // Refresh ownership so UI shows "Watch Now"
                isMovieOwned = StreamingRepository.isMovieInLibrary(movie.movieId)
                Log.d(TAG, "Purchase recorded for ${movie.title}, type: $type, pi: $paymentIntentId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to record purchase after payment", e)
                purchaseError = "Payment succeeded but failed to save: ${e.message}"
            } finally {
                isPurchasing = false
            }
        }
    }

    fun resetPurchaseState() {
        purchaseSuccess = false
        purchaseError = null
        isPurchasing = false
    }
}
