package com.example.bookmymovie.model

data class ChatMessage(
    val id: String = "",
    val role: String = "",   // "user" or "model"
    val text: String = "",
    val timestamp: Long = 0L,
    val sessionId: String = ""   // groups messages into sessions; empty = legacy
)
