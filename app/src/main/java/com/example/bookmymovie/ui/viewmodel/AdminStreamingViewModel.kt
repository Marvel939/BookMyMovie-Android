package com.example.bookmymovie.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookmymovie.data.repository.StreamingRepository
import com.example.bookmymovie.model.StreamingMovie
import com.example.bookmymovie.model.StreamingTransaction
import kotlinx.coroutines.launch

class AdminStreamingViewModel : ViewModel() {

    var movies by mutableStateOf<List<StreamingMovie>>(emptyList())
        private set
    var transactions by mutableStateOf<List<StreamingTransaction>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var isSaving by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    // Form state
    var title by mutableStateOf("")
    var posterUrl by mutableStateOf("")
    var bannerUrl by mutableStateOf("")
    var description by mutableStateOf("")
    var genre by mutableStateOf("")
    var duration by mutableStateOf("")
    var releaseYear by mutableStateOf("")
    var rating by mutableStateOf("")
    var language by mutableStateOf("Hindi")
    var director by mutableStateOf("")
    var castInput by mutableStateOf("")
    var trailerUrl by mutableStateOf("")
    var streamUrl by mutableStateOf("")
    var ottPlatform by mutableStateOf("Netflix")
    var rentPrice by mutableStateOf("")
    var buyPrice by mutableStateOf("")
    var rentDurationDays by mutableStateOf("30")
    var isExclusive by mutableStateOf(false)

    var editingMovieId by mutableStateOf<String?>(null)
        private set

    init {
        loadMovies()
    }

    fun loadMovies() {
        viewModelScope.launch {
            isLoading = true
            try {
                movies = StreamingRepository.getAllAdminMovies()
            } catch (e: Exception) {
                errorMessage = "Failed to load: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun loadTransactions() {
        viewModelScope.launch {
            try {
                transactions = StreamingRepository.getAllTransactions()
            } catch (e: Exception) {
                errorMessage = "Failed to load transactions: ${e.message}"
            }
        }
    }

    fun prepareForEdit(movie: StreamingMovie) {
        editingMovieId = movie.movieId
        title = movie.title
        posterUrl = movie.posterUrl
        bannerUrl = movie.bannerUrl
        description = movie.description
        genre = movie.genre
        duration = movie.duration
        releaseYear = movie.releaseYear.toString()
        rating = movie.rating.toString()
        language = movie.language
        director = movie.director
        castInput = movie.cast.joinToString(", ")
        trailerUrl = movie.trailerUrl
        streamUrl = movie.streamUrl
        ottPlatform = movie.ottPlatform
        rentPrice = movie.rentPrice.toString()
        buyPrice = movie.buyPrice.toString()
        rentDurationDays = movie.rentDurationDays.toString()
        isExclusive = movie.isExclusive
    }

    fun clearForm() {
        editingMovieId = null
        title = ""; posterUrl = ""; bannerUrl = ""; description = ""
        genre = ""; duration = ""; releaseYear = ""; rating = ""
        language = "Hindi"; director = ""; castInput = ""
        trailerUrl = ""; streamUrl = ""; ottPlatform = "Netflix"
        rentPrice = ""; buyPrice = ""; rentDurationDays = "30"
        isExclusive = false
    }

    fun validateForm(): String? {
        if (title.isBlank()) return "Title is required"
        if (posterUrl.isBlank()) return "Poster URL is required"
        if (genre.isBlank()) return "Genre is required"
        if (duration.isBlank()) return "Duration is required"
        if (releaseYear.isBlank() || releaseYear.toIntOrNull() == null) return "Valid release year required"
        if (ottPlatform.isBlank()) return "OTT Platform is required"
        if (rentPrice.isBlank() || rentPrice.toDoubleOrNull() == null) return "Valid rent price required"
        if (buyPrice.isBlank() || buyPrice.toDoubleOrNull() == null) return "Valid buy price required"
        return null
    }

    fun saveMovie(onSuccess: () -> Unit) {
        val error = validateForm()
        if (error != null) {
            errorMessage = error
            return
        }

        viewModelScope.launch {
            isSaving = true
            errorMessage = null
            try {
                val movie = StreamingMovie(
                    movieId = editingMovieId ?: "",
                    title = title.trim(),
                    posterUrl = posterUrl.trim(),
                    bannerUrl = bannerUrl.trim(),
                    description = description.trim(),
                    genre = genre.trim(),
                    duration = duration.trim(),
                    releaseYear = releaseYear.toIntOrNull() ?: 0,
                    rating = rating.toDoubleOrNull() ?: 0.0,
                    language = language.trim(),
                    director = director.trim(),
                    cast = castInput.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                    trailerUrl = trailerUrl.trim(),
                    streamUrl = streamUrl.trim(),
                    ottPlatform = ottPlatform.trim(),
                    rentPrice = rentPrice.toDoubleOrNull() ?: 0.0,
                    buyPrice = buyPrice.toDoubleOrNull() ?: 0.0,
                    rentDurationDays = rentDurationDays.toIntOrNull() ?: 30,
                    isExclusive = isExclusive,
                    isActive = true,
                    source = "admin"
                )

                if (editingMovieId != null) {
                    StreamingRepository.updateStreamingMovie(movie)
                    message = "Movie updated successfully!"
                } else {
                    StreamingRepository.addStreamingMovie(movie)
                    message = "Movie added successfully!"
                }
                clearForm()
                loadMovies()
                onSuccess()
            } catch (e: Exception) {
                errorMessage = "Failed to save: ${e.message}"
            } finally {
                isSaving = false
            }
        }
    }

    fun deleteMovie(movieId: String) {
        viewModelScope.launch {
            try {
                StreamingRepository.deleteStreamingMovie(movieId)
                message = "Movie deleted"
                loadMovies()
            } catch (e: Exception) {
                errorMessage = "Failed to delete: ${e.message}"
            }
        }
    }

    fun toggleActive(movieId: String, isActive: Boolean) {
        viewModelScope.launch {
            try {
                StreamingRepository.toggleMovieActive(movieId, isActive)
                loadMovies()
            } catch (e: Exception) {
                errorMessage = "Failed to update: ${e.message}"
            }
        }
    }

    fun clearMessage() { message = null }
    fun clearError() { errorMessage = null }
}
