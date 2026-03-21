package com.example.bookmymovie.model

import com.google.firebase.database.PropertyName

data class Coupon(
    @PropertyName("id")
    val id: String = "",
    
    @PropertyName("code")
    val code: String = "",
    
    @PropertyName("offerId")
    val offerId: String = "",
    
    @PropertyName("redemptionCount")
    val redemptionCount: Int = 0,
    
    @PropertyName("maxRedemptions")
    val maxRedemptions: Int = 0, // 0 means unlimited
    
    @PropertyName("usedByUsers")
    val usedByUsers: Map<String, Boolean> = emptyMap(), // userId -> true
    
    @PropertyName("createdAt")
    val createdAt: Long = 0L,
    
    @PropertyName("validFrom")
    val validFrom: Long = 0L,
    
    @PropertyName("validUntil")
    val validUntil: Long = 0L,
    
    @PropertyName("isActive")
    val isActive: Boolean = true,
    
    @PropertyName("description")
    val description: String = ""
) {
    fun isExpired(currentTimeMillis: Long = System.currentTimeMillis()): Boolean {
        return currentTimeMillis > validUntil
    }
    
    fun isNotStarted(currentTimeMillis: Long = System.currentTimeMillis()): Boolean {
        return currentTimeMillis < validFrom
    }
    
    fun isRedemptionLimitReached(): Boolean {
        return maxRedemptions > 0 && redemptionCount >= maxRedemptions
    }
    
    fun hasUserAlreadyUsed(userId: String): Boolean {
        return usedByUsers.containsKey(userId) && usedByUsers[userId] == true
    }
}
