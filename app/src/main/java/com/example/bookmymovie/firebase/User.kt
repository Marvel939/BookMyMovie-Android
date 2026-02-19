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
    val profileImageUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
