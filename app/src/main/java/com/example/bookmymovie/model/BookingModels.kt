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
    val available: Boolean = true,
    val ml: Int = 0  // millilitres, used for Beverages
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
    val ticketGstRate: Int = 18,
    val ticketGstAmount: Int = 0,
    val convenienceFeeAmount: Int = 0,
    val convenienceFeeGstAmount: Int = 0,
    val totalAmount: Int = 0,
    val discountAmount: Int = 0,
    val discountCode: String = "",
    val appliedCouponId: String = "",
    val refundableAmount: Int = 0,
    val nonRefundableAmount: Int = 0,
    val paymentIntentId: String = "",
    val paymentMethod: String = "stripe", // stripe | wallet
    val paymentStatus: String = "confirmed",
    val status: String = "confirmed",
    val refundStatus: String = "none", // none | succeeded | failed
    val refundReason: String = "",
    val refundId: String = "",
    val refundedAt: Long = 0L,
    val bookedAt: Long = 0L
)
