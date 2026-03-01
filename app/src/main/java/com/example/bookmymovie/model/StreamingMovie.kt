package com.example.bookmymovie.model

data class StreamingMovie(
    val movieId: String = "",
    val title: String = "",
    val posterUrl: String = "",
    val bannerUrl: String = "",
    val description: String = "",
    val genre: String = "",
    val duration: String = "",
    val releaseYear: Int = 0,
    val rating: Double = 0.0,
    val language: String = "",
    val director: String = "",
    val cast: List<String> = emptyList(),
    val trailerUrl: String = "",
    val streamUrl: String = "",
    val ottPlatform: String = "",
    val ottLogoUrl: String = "",
    val rentPrice: Double = 0.0,
    val buyPrice: Double = 0.0,
    val rentDurationDays: Int = 30,
    val isExclusive: Boolean = false,
    val isActive: Boolean = true,
    val source: String = "tmdb" // "tmdb" or "admin"
)
