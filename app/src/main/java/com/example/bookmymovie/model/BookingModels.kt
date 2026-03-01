package com.example.bookmymovie.model

data class CinemaShowtime(
    val showtimeId: String = "",
    val screenId: String = "",
    val screenName: String = "",
    val screenType: String = "2D",
    val movieId: String = "",
    val movieName: String = "",
    val moviePoster: String = "",
    val date: String = "",
    val time: String = "",
    val language: String = "English",
    val formats: String = "",
    val formatPrices: Map<String, Map<String, Int>> = emptyMap()
)

data class SeatData(
    val seatId: String = "",
    val row: String = "",
    val col: Int = 0,
    val type: String = "silver",   // silver | gold | platinum
    val price: Int = 150,
    val booked: Boolean = false,
    val bookedByUid: String = ""
)

data class FoodItem(
    val itemId: String = "",
    val name: String = "",
    val description: String = "",
    val price: Int = 0,
    val category: String = "Snacks",  // Snacks | Beverages | Combos
    val imageUrl: String = "",
    val available: Boolean = true
)

data class CartFoodItem(
    val item: FoodItem,
    val qty: Int
)

data class Booking(
    val bookingId: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val userName: String = "",
    val placeId: String = "",
    val cinemaName: String = "",
    val cinemaAddress: String = "",
    val screenId: String = "",
    val screenName: String = "",
    val screenType: String = "2D",
    val showtimeId: String = "",
    val movieId: String = "",
    val movieName: String = "",
    val moviePoster: String = "",
    val date: String = "",
    val time: String = "",
    val language: String = "English",
    val seats: List<String> = emptyList(),
    val seatTypes: Map<String, String> = emptyMap(),
    val seatAmount: Int = 0,
    val foodItems: List<Map<String, Any>> = emptyList(),
    val foodAmount: Int = 0,
    val totalAmount: Int = 0,
    val paymentStatus: String = "confirmed",
    val status: String = "confirmed",
    val bookedAt: Long = 0L
)
