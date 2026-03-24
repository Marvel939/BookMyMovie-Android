package com.example.bookmymovie.model

enum class OfferCategory {
    THEATRE_SPECIFIC,
    MOVIE_SPECIFIC,
    PLATFORM_WIDE,
    BANK_PAYMENT
}

enum class OfferApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED
}

enum class DiscountType {
    PERCENTAGE,
    FIXED_AMOUNT
}

data class Offer(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = OfferCategory.PLATFORM_WIDE.name,
    
    // Theatre Owner specifics
    val theatreOwnerId: String = "",
    val theatreId: String = "",
    val theatreName: String = "",
    val movieId: String = "", // Used if MOVIE_SPECIFIC
    
    // Discount specifics
    val discountType: String = DiscountType.PERCENTAGE.name,
    val discountPercentage: Int = 0,
    val discountAmount: Int = 0,
    val maxDiscountAmount: Int = 0, // Cap for percentage
    val minOrderAmount: Int = 0, // Minimum ticket value required
    
    // Coupon specifics
    val couponCode: String = "",
    val startDate: Long = 0,
    val endDate: Long = 0,
    val maxRedemptionsPerUser: Int = 1,
    
    // Approval Status
    val approvalStatus: String = OfferApprovalStatus.APPROVED.name, // Will default to PENDING when created by theatre owners
    
    val isActive: Boolean = true
) {
    fun getCategoryEnum(): OfferCategory {
        return try {
            OfferCategory.valueOf(category)
        } catch (e: Exception) {
            OfferCategory.PLATFORM_WIDE
        }
    }
    
    fun getApprovalStatusEnum(): OfferApprovalStatus {
        return try {
            OfferApprovalStatus.valueOf(approvalStatus)
        } catch (e: Exception) {
            OfferApprovalStatus.PENDING
        }
    }
    
    fun getDiscountTypeEnum(): DiscountType {
        return try {
            DiscountType.valueOf(discountType)
        } catch (e: Exception) {
            DiscountType.PERCENTAGE
        }
    }
}
