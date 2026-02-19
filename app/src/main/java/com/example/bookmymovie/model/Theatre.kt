package com.example.bookmymovie.model

/**
 * Represents a theatre/cinema hall in a city.
 * Maps to: theatres/{cityName}/{theatreId}
 */
data class Theatre(
    val theatreId: String = "",
    val name: String = "",
    val address: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val city: String = "",
    val screens: Int = 1,
    val facilities: List<String> = emptyList(),
    val imageUrl: String = "",
    val rating: Double = 0.0,
    val totalRatings: Int = 0
) {
    constructor() : this("", "", "", 0.0, 0.0, "", 1, emptyList(), "", 0.0, 0)
}
