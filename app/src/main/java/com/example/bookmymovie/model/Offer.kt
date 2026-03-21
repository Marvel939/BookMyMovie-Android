package com.example.bookmymovie.model

import com.google.firebase.database.PropertyName

enum class OfferType {
    PERCENTAGE,      // e.g., 20% off
    FIXED_AMOUNT,    // e.g., ₹100 off
    BUY_GET          // e.g., Buy 2 get 1 free or similar
}

enum class OfferCategory {
    THEATRE_SPECIFIC,
    MOVIE_SPECIFIC,
    PLATFORM_WIDE,
    BANK_PAYMENT
}

enum class OfferStatus {
    PENDING,
    APPROVED,
    REJECTED
}

data class Offer(
    @PropertyName("id")
    val id: String = "",
    
    @PropertyName("title")
    val title: String = "",
    
    @PropertyName("description")
    val description: String = "",
    
    @PropertyName("type")
    val type: String = OfferType.PERCENTAGE.name,
    
    @PropertyName("value")
    val value: Double = 0.0, // percentage value or fixed amount
    
    @PropertyName("minBookingAmount")
    val minBookingAmount: Double = 200.0, // Minimum ₹200 rule
    
    @PropertyName("theatreOwnerId")
    val theatreOwnerId: String = "",
    
    @PropertyName("theatreId")
    val theatreId: String = "",
    
    @PropertyName("theatreName")
    val theatreName: String = "",
    
    @PropertyName("cityId")
    val cityId: String = "",
    
    @PropertyName("cityName")
    val cityName: String = "",
    
    @PropertyName("createdAt")
    val createdAt: Long = 0L,
    
    @PropertyName("validFrom")
    val validFrom: Long = 0L,
    
    @PropertyName("validUntil")
    val validUntil: Long = 0L,
    
    @PropertyName("status")
    val status: String = OfferStatus.PENDING.name,
    
    @PropertyName("imageUrl")
    val imageUrl: String = "",
    
    @PropertyName("termsAndConditions")
    val termsAndConditions: String = "",

    @PropertyName("category")
    val category: String = OfferCategory.THEATRE_SPECIFIC.name,

    @PropertyName("targetMovieName")
    val targetMovieName: String = "",

    @PropertyName("paymentMethod")
    val paymentMethod: String = "ANY",
    
    @PropertyName("applicableToAllCities")
    val applicableToAllCities: Boolean = false,
    
    @PropertyName("isActive")
    val isActive: Boolean = true
) {
    fun getOfferTypeEnum(): OfferType = OfferType.valueOf(type)
    fun getStatusEnum(): OfferStatus = OfferStatus.valueOf(status)
    fun getCategoryEnum(): OfferCategory = OfferCategory.valueOf(category)
    fun isValid(): Boolean = isActive && status == OfferStatus.APPROVED.name
    
    companion object {
        val EMPTY = Offer()
    }
}
