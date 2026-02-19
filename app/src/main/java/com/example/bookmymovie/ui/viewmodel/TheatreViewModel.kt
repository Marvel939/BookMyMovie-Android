package com.example.bookmymovie.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookmymovie.firebase.FirebaseMovieRepository
import com.example.bookmymovie.model.LocalMovie
import com.example.bookmymovie.model.Showtime
import com.example.bookmymovie.model.Theatre
import com.example.bookmymovie.model.TheatreShowtime
import com.example.bookmymovie.model.MovieWithShowtimes
import kotlinx.coroutines.launch

/**
 * ViewModel for theatre listing and showtime display.
 */
class TheatreViewModel : ViewModel() {

    /** All theatres in city */
    var theatres by mutableStateOf<List<Theatre>>(emptyList())
        private set

    /** Showtimes for a selected movie across all theatres */
    var movieShowtimes by mutableStateOf<MovieWithShowtimes?>(null)
        private set

    /** Showtimes for a selected theatre */
    var theatreMovies by mutableStateOf<List<Pair<LocalMovie, Showtime>>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    /**
     * Load all theatres in a city.
     */
    fun loadTheatres(city: String) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                theatres = FirebaseMovieRepository.getTheatresInCity(city)
            } catch (e: Exception) {
                errorMessage = "Failed to load theatres: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * Load showtimes for a specific movie across all theatres in a city.
     * Used when user taps a movie to see where it's playing.
     */
    fun loadShowtimesForMovie(city: String, movieId: String) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val movie = FirebaseMovieRepository.getMovie(movieId) ?: return@launch
                val allShowtimes = FirebaseMovieRepository.getShowtimesInCity(city)
                val allTheatres = FirebaseMovieRepository.getTheatresInCity(city)

                val theatreShowtimeList = mutableListOf<TheatreShowtime>()

                for ((theatreId, movieMap) in allShowtimes) {
                    val showtime = movieMap[movieId] ?: continue
                    val theatre = allTheatres.find { it.theatreId == theatreId } ?: continue
                    theatreShowtimeList.add(
                        TheatreShowtime(
                            theatre = theatre,
                            showtimes = listOf(showtime)
                        )
                    )
                }

                movieShowtimes = MovieWithShowtimes(
                    movie = movie,
                    theatreShowtimes = theatreShowtimeList
                )
            } catch (e: Exception) {
                errorMessage = "Failed to load showtimes: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * Load all movies playing at a specific theatre.
     */
    fun loadMoviesAtTheatre(city: String, theatreId: String) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val showtimes = FirebaseMovieRepository.getShowtimesForTheatre(city, theatreId)
                val moviePairs = showtimes.mapNotNull { showtime ->
                    val movie = FirebaseMovieRepository.getMovie(showtime.movieId)
                    if (movie != null) movie to showtime else null
                }
                theatreMovies = moviePairs
            } catch (e: Exception) {
                errorMessage = "Failed to load theatre movies: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
}
