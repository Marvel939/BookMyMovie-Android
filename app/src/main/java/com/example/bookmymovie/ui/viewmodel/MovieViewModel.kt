package com.example.bookmymovie.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookmymovie.data.repository.MovieRepository
import com.example.bookmymovie.data.repository.StreamingRepository
import com.example.bookmymovie.ui.screens.Movie
import com.example.bookmymovie.model.StreamingMovie
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

    // OTT Movies
    var ottMovies by mutableStateOf<List<StreamingMovie>>(emptyList())
        private set

    var isLoading by mutableStateOf(true)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    // Filter state
    var selectedFilter by mutableStateOf<String?>(null)
        private set

    var filteredNowPlayingMovies by mutableStateOf<List<Movie>>(emptyList())
        private set

    var filteredPopularMovies by mutableStateOf<List<Movie>>(emptyList())
        private set

    var filteredTopRatedMovies by mutableStateOf<List<Movie>>(emptyList())
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
                val ottCatalog = StreamingRepository.fetchOTTCatalog()
                
                nowPlayingMovies = nowPlaying
                upcomingMovies = upcoming
                popularMovies = popular
                topRatedMovies = topRated
                ottMovies = ottCatalog
                
                // Initialize filtered lists
                filteredNowPlayingMovies = nowPlaying
                filteredPopularMovies = popular
                filteredTopRatedMovies = topRated
                
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

    fun setFilter(filterName: String?) {
        selectedFilter = filterName
        applyFilter(filterName)
    }

    private fun applyFilter(filterName: String?) {
        if (filterName == null) {
            // Show all movies
            filteredNowPlayingMovies = nowPlayingMovies
            filteredPopularMovies = popularMovies
            filteredTopRatedMovies = topRatedMovies
        } else if (filterName == "OTT") {
            // Show only OTT movies
            filteredNowPlayingMovies = emptyList()
            filteredPopularMovies = emptyList()
            filteredTopRatedMovies = emptyList()
        } else {
            // Filter by genre
            filteredNowPlayingMovies = nowPlayingMovies.filter { movie ->
                movie.genre.split(",", "/", "&").map { it.trim() }.contains(filterName)
            }
            filteredPopularMovies = popularMovies.filter { movie ->
                movie.genre.split(",", "/", "&").map { it.trim() }.contains(filterName)
            }
            filteredTopRatedMovies = topRatedMovies.filter { movie ->
                movie.genre.split(",", "/", "&").map { it.trim() }.contains(filterName)
            }
        }
    }

    fun getAvailableGenres(): List<String> {
        val allMovies = nowPlayingMovies + upcomingMovies + popularMovies + topRatedMovies
        return allMovies
            .flatMap { it.genre.split(",", "/", "&").map { g -> g.trim() } }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }
}
