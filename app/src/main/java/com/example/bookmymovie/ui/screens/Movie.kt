package com.example.bookmymovie.ui.screens

data class CastMember(
    val id: Int,
    val name: String,
    val character: String,
    val imageUrl: String
)

data class CrewMember(
    val id: Int,
    val name: String,
    val job: String,
    val imageUrl: String
)

data class Review(
    val author: String,
    val content: String,
    val rating: Double?,
    val avatarUrl: String?
)

data class Movie(
    val id: String,
    val title: String,
    val posterUrl: String,
    val bannerUrl: String,
    val rating: String,
    val language: String,
    val genre: String,
    val duration: String,
    val description: String,
    val directors: List<String> = emptyList(),
    val cast: List<CastMember> = emptyList(),
    val crew: List<CrewMember> = emptyList(),
    val releaseDate: String = "Coming Soon",
    val reviews: List<Review> = emptyList()
)
