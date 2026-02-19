package com.example.bookmymovie.model

/**
 * Represents a city available in the booking system.
 * Maps to: cities/{cityName}
 */
data class City(
    val name: String = "",
    val state: String = "",
    val country: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val isActive: Boolean = true
) {
    /** No-arg constructor for Firebase deserialization */
    constructor() : this("", "", "", 0.0, 0.0, true)
}
