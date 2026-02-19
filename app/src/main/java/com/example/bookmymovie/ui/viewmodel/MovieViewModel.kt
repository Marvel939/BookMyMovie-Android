package com.example.bookmymovie.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookmymovie.data.repository.MovieRepository
import com.example.bookmymovie.ui.screens.Movie
import kotlinx.coroutines.launch

class MovieViewModel : ViewModel() {

    var selectedCity by mutableStateOf("Mumbai")
    var cityLoaded by mutableStateOf(false)

    var nowPlayingMovies by mutableStateOf<List<Movie>>(emptyList())
        private set

    var upcomingMovies by mutableStateOf<List<Movie>>(emptyList())
        private set

    var popularMovies by mutableStateOf<List<Movie>>(emptyList())
        private set

    var topRatedMovies by mutableStateOf<List<Movie>>(emptyList())
        private set

    var isLoading by mutableStateOf(true)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    init {
        loadMovies()
    }

    fun loadMovies() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val nowPlaying = MovieRepository.fetchTrendingDay()
                val upcoming = MovieRepository.fetchUpcoming()
                val popular = MovieRepository.fetchPopular()
                val topRated = MovieRepository.fetchTopRated()
                nowPlayingMovies = nowPlaying
                upcomingMovies = upcoming
                popularMovies = popular
                topRatedMovies = topRated
                if (nowPlaying.isEmpty() && upcoming.isEmpty()) {
                    errorMessage = "No movies found. Please check your internet connection."
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage = when {
                    e.message?.contains("Unable to resolve host") == true ->
                        "No internet connection. Please check your network."
                    e.message?.contains("timeout") == true ->
                        "Connection timed out. Please try again."
                    else -> e.message ?: "Failed to load movies"
                }
            } finally {
                isLoading = false
            }
        }
    }
}
