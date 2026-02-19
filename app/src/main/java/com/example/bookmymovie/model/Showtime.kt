package com.example.bookmymovie.model

/**
 * Represents showtimes for a movie at a theatre.
 * Maps to: showtimes/{cityName}/{theatreId}/{movieId}
 */
data class Showtime(
    val movieId: String = "",
    val theatreId: String = "",
    val city: String = "",
    val date: String = "",               // "2026-02-19"
    val times: List<String> = emptyList(), // ["10:00 AM", "1:30 PM", "7:00 PM"]
    val screenNumber: Int = 1,
    val price: Double = 0.0,
    val availableSeats: Int = 0,
    val totalSeats: Int = 0,
    val format: String = "2D"             // "2D", "3D", "IMAX"
) {
    constructor() : this("", "", "", "", emptyList(), 1, 0.0, 0, 0, "2D")
}

/**
 * Aggregated showtime info combining theatre + movie + times.
 * Used in UI to display a movie's availability across theatres.
 */
data class TheatreShowtime(
    val theatre: Theatre,
    val showtimes: List<Showtime>
)

/**
 * Aggregated info for a movie with all its theatre showtimes in a city.
 */
data class MovieWithShowtimes(
    val movie: LocalMovie,
    val theatreShowtimes: List<TheatreShowtime>
)
