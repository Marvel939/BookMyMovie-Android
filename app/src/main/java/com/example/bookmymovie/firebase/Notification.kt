package com.example.bookmymovie.firebase

data class Notification(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "", // offer, booking, refund, update
    val timestamp: Long = 0L,
    val isRead: Boolean = false,
    val relatedId: String = "" // offerId, bookingId, refundId, etc.
)
