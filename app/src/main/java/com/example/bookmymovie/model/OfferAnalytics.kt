package com.example.bookmymovie.model

import com.google.firebase.database.PropertyName

data class OfferAnalytics(
    @PropertyName("id")
    val id: String = "",
    
    @PropertyName("offerId")
    val offerId: String = "",
    
    @PropertyName("couponId")
    val couponId: String = "",
    
    @PropertyName("userId")
    val userId: String = "",
    
    @PropertyName("bookingId")
    val bookingId: String = "",
    
    @PropertyName("appliedAmount")
    val appliedAmount: Double = 0.0, // Discount amount in rupees
    
    @PropertyName("originalAmount")
    val originalAmount: Double = 0.0, // Original booking amount
    
    @PropertyName("finalAmount")
    val finalAmount: Double = 0.0, // Final amount after discount
    
    @PropertyName("timestamp")
    val timestamp: Long = 0L,
    
    @PropertyName("theatreId")
    val theatreId: String = "",
    
    @PropertyName("movieId")
    val movieId: String = ""
)
