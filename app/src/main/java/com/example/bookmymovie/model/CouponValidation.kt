package com.example.bookmymovie.model

data class CouponValidationResult(
    val isValid: Boolean = false,
    val discountAmount: Double = 0.0,
    val finalAmount: Double = 0.0,
    val errorMessage: String = "",
    val coupon: Coupon? = null,
    val offer: Offer? = null
)

sealed class CouponValidationError {
    data class CouponNotFound(val message: String = "Coupon not found") : CouponValidationError()
    data class CouponExpired(val message: String = "Coupon has expired") : CouponValidationError()
    data class CouponNotStarted(val message: String = "Coupon is not yet valid") : CouponValidationError()
    data class RedemptionLimitReached(val message: String = "Coupon redemption limit reached") : CouponValidationError()
    data class MinimumAmountNotMet(val message: String = "Minimum booking amount (₹200) not met") : CouponValidationError()
    data class AlreadyUsedByUser(val message: String = "You have already used this coupon") : CouponValidationError()
    data class OfferNotAvailable(val message: String = "Associated offer is not available") : CouponValidationError()
    data class TheatreNotApplicable(val message: String = "Coupon not applicable to this theatre") : CouponValidationError()
    data class GenericError(val message: String = "An error occurred while validating coupon") : CouponValidationError()
}
