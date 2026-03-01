package com.example.bookmymovie.model

data class LibraryItem(
    val purchaseId: String = "",
    val movieId: String = "",
    val title: String = "",
    val posterUrl: String = "",
    val type: String = "rent",
    val purchasedAt: Long = 0L,
    val expiresAt: Long? = null,
    val amountPaid: Double = 0.0,
    val paymentIntentId: String = "",
    val ottPlatform: String = ""
) {
    fun isExpired(): Boolean {
        if (type == "buy") return false
        val exp = expiresAt ?: return false
        return System.currentTimeMillis() > exp
    }

    fun remainingDays(): Int {
        if (type == "buy") return Int.MAX_VALUE
        val exp = expiresAt ?: return 0
        val diff = exp - System.currentTimeMillis()
        return if (diff > 0) (diff / (1000 * 60 * 60 * 24)).toInt() else 0
    }
}
