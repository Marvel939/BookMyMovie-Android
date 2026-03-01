package com.example.bookmymovie.model

data class StreamingTransaction(
    val transactionId: String = "",
    val userId: String = "",
    val movieId: String = "",
    val movieTitle: String = "",
    val type: String = "rent",
    val amount: Double = 0.0,
    val paymentIntentId: String = "",
    val status: String = "completed",
    val timestamp: Long = 0L
)
