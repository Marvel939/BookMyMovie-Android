package com.example.bookmymovie.firebase

data class User(
    val userId: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val gender: String = "",
    val address: String = "",
    val countryCode: String = "",
    val phone: String = "",
    val dob: String = "",
    val city: String = "",
    val country: String = "",
    val state: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val profileImageUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = "active", // active | blocked
    val isDeleted: Boolean = false, // soft delete flag
    val permissions: String = "standard" // standard | premium | admin
)
