package com.example.bookmymovie.ui.screens

data class Country(val code: String, val name: String, val flag: String)

val countryList = listOf(
    Country("+91", "India", "🇮🇳"),
    Country("+1", "USA", "🇺🇸"),
    Country("+44", "UK", "🇬🇧"),
    Country("+61", "Australia", "🇦🇺"),
    Country("+81", "Japan", "🇯🇵"),
    Country("+971", "UAE", "🇦🇪")
)
