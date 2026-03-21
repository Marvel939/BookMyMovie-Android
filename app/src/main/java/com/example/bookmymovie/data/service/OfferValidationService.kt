package com.example.bookmymovie.data.service

import com.example.bookmymovie.data.repository.CouponsRepository
import com.example.bookmymovie.data.repository.OffersRepository
import com.example.bookmymovie.model.Coupon
import com.example.bookmymovie.model.CouponValidationError
import com.example.bookmymovie.model.CouponValidationResult
import com.example.bookmymovie.model.Offer
import com.example.bookmymovie.model.OfferCategory
import com.example.bookmymovie.model.OfferStatus
import com.example.bookmymovie.model.OfferType
import kotlin.math.roundToInt

class OfferValidationService(
    private val offersRepository: OffersRepository = OffersRepository.getInstance(),
    private val couponsRepository: CouponsRepository = CouponsRepository.getInstance()
) {

    /**
     * Validate a coupon and calculate discount
     * @param couponCode The coupon code to validate
     * @param bookingAmount The original booking amount
     * @param userId The user trying to apply the coupon
     * @param theatreId Optional: theatre ID for offer applicability check
     * @return CouponValidationResult with validation status and discount amount
     */
    suspend fun validateCouponApplication(
        couponCode: String,
        bookingAmount: Double,
        userId: String,
        theatreId: String? = null,
        paymentMethod: String? = null
    ): CouponValidationResult {
        return try {
            // Step 1: Check if coupon exists
            val coupon = couponsRepository.getCouponByCode(couponCode)
                ?: return CouponValidationResult(
                    isValid = false,
                    errorMessage = "Coupon not found",
                    discountAmount = 0.0,
                    finalAmount = bookingAmount
                )

            // NOTE: fetch associated offer early so we can apply theatre-specific semantics
            val offer = offersRepository.getOfferById(coupon.offerId)
                ?: return CouponValidationResult(
                    isValid = false,
                    errorMessage = "Associated offer is not available",
                    coupon = coupon,
                    discountAmount = 0.0,
                    finalAmount = bookingAmount
                )

            // Step 2: Check if coupon is active. For theatre-specific coupons allow
            // usage when the booking theatre matches the offer theatre even if the
            // coupon `isActive` flag is false (owner-created coupons for that
            // theatre should be usable immediately). Otherwise respect the flag.
            val isActiveEffective = coupon.isActive || (
                !theatreId.isNullOrEmpty() && offer.theatreId.isNotEmpty() && offer.theatreId == theatreId
            )

            if (!isActiveEffective) {
                return CouponValidationResult(
                    isValid = false,
                    errorMessage = "This coupon is no longer active",
                    coupon = coupon,
                    offer = offer,
                    discountAmount = 0.0,
                    finalAmount = bookingAmount
                )
            }

            // Step 3: Check expiry
            val currentTime = System.currentTimeMillis()
            if (coupon.isExpired(currentTime)) {
                return CouponValidationResult(
                    isValid = false,
                    errorMessage = "Coupon has expired",
                    coupon = coupon,
                    offer = offer,
                    discountAmount = 0.0,
                    finalAmount = bookingAmount
                )
            }

            // Step 4: Check if coupon is valid to use (validFrom date)
            if (coupon.isNotStarted(currentTime)) {
                return CouponValidationResult(
                    isValid = false,
                    errorMessage = "This coupon is not yet valid",
                    coupon = coupon,
                    discountAmount = 0.0,
                    finalAmount = bookingAmount
                )
            }

            // Step 5: Check redemption limit
            if (coupon.isRedemptionLimitReached()) {
                return CouponValidationResult(
                    isValid = false,
                    errorMessage = "Coupon redemption limit has been reached",
                    coupon = coupon,
                    discountAmount = 0.0,
                    finalAmount = bookingAmount
                )
            }

            // Step 6: Check if user has already used this coupon
            if (coupon.hasUserAlreadyUsed(userId)) {
                return CouponValidationResult(
                    isValid = false,
                    errorMessage = "You have already used this coupon",
                    coupon = coupon,
                    discountAmount = 0.0,
                    finalAmount = bookingAmount
                )
            }

            // Step 8: Check if offer is approved and valid
            if (!offer.isValid()) {
                return CouponValidationResult(
                    isValid = false,
                    errorMessage = "Associated offer is not available",
                    coupon = coupon,
                    offer = offer,
                    discountAmount = 0.0,
                    finalAmount = bookingAmount
                )
            }

            // Step 9: Check minimum booking amount
            if (bookingAmount < offer.minBookingAmount) {
                return CouponValidationResult(
                    isValid = false,
                    errorMessage = "Minimum booking amount of ₹${offer.minBookingAmount} required",
                    coupon = coupon,
                    offer = offer,
                    discountAmount = 0.0,
                    finalAmount = bookingAmount
                )
            }

            // Step 10: Check if theatre is applicable (if theatreId provided)
            if (!theatreId.isNullOrEmpty() && offer.theatreId != theatreId) {
                return CouponValidationResult(
                    isValid = false,
                    errorMessage = "This coupon is not applicable to the selected theatre",
                    coupon = coupon,
                    offer = offer,
                    discountAmount = 0.0,
                    finalAmount = bookingAmount
                )
            }

            // Step 10.5: Enforce Stripe-only for bank/payment category offers
            if (
                offer.getCategoryEnum() == OfferCategory.BANK_PAYMENT &&
                !paymentMethod.isNullOrEmpty() &&
                paymentMethod.lowercase() != "stripe"
            ) {
                return CouponValidationResult(
                    isValid = false,
                    errorMessage = "This bank/payment offer is valid only with Stripe payment",
                    coupon = coupon,
                    offer = offer,
                    discountAmount = 0.0,
                    finalAmount = bookingAmount
                )
            }

            // Step 11: Calculate discount
            val discountAmount = calculateDiscount(bookingAmount, offer)
            val finalAmount = bookingAmount - discountAmount

            // All validations passed
            return CouponValidationResult(
                isValid = true,
                errorMessage = "",
                discountAmount = discountAmount,
                finalAmount = finalAmount,
                coupon = coupon,
                offer = offer
            )

        } catch (e: Exception) {
            CouponValidationResult(
                isValid = false,
                errorMessage = "Error validating coupon: ${e.message}",
                discountAmount = 0.0,
                finalAmount = bookingAmount
            )
        }
    }

    /**
     * Calculate the discount amount based on offer type
     */
    fun calculateDiscount(bookingAmount: Double, offer: Offer): Double {
        return when (offer.getOfferTypeEnum()) {
            OfferType.PERCENTAGE -> {
                // Example: 20% off on ₹500 = ₹100
                val discount = (bookingAmount * offer.value) / 100.0
                // Round to nearest rupee
                discount.roundToInt().toDouble()
            }
            OfferType.FIXED_AMOUNT -> {
                // Example: ₹100 off
                // Cap discount to not exceed booking amount
                minOf(offer.value, bookingAmount)
            }
            OfferType.BUY_GET -> {
                // Buy X Get Y offer - simplified: discount is the value specified
                // Example: Buy 2 tickets, get 40% off on food/beverage = discount value
                minOf(offer.value, bookingAmount)
            }
        }
    }

    /**
     * Check if an offer is currently valid
     */
    fun isOfferValid(offer: Offer): Boolean {
        return offer.isValid() && 
                System.currentTimeMillis() >= offer.validFrom &&
                System.currentTimeMillis() <= offer.validUntil
    }

    /**
     * Check if minimum booking amount is met
     */
    fun isMinimumAmountMet(bookingAmount: Double, offer: Offer): Boolean {
        return bookingAmount >= offer.minBookingAmount
    }

    /**
     * Check if offer is applicable to theatre
     */
    fun isOfferApplicableToTheatre(offer: Offer, theatreId: String): Boolean {
        return offer.theatreId == theatreId || offer.applicableToAllCities
    }

    /**
     * Get discount percentage for UI display
     */
    fun getDiscountPercentage(offer: Offer): String {
        return when (offer.getOfferTypeEnum()) {
            OfferType.PERCENTAGE -> "${offer.value.toInt()}% OFF"
            OfferType.FIXED_AMOUNT -> "₹${offer.value.toInt()} OFF"
            OfferType.BUY_GET -> "Buy & Get"
        }
    }

    /**
     * Build a readable offer description for display
     */
    fun buildOfferDescription(offer: Offer): String {
        return when (offer.getOfferTypeEnum()) {
            OfferType.PERCENTAGE -> "Get ${offer.value.toInt()}% discount on movie tickets"
            OfferType.FIXED_AMOUNT -> "Get ₹${offer.value.toInt()} discount on bookings above ₹${offer.minBookingAmount.toInt()}"
            OfferType.BUY_GET -> offer.description.ifEmpty { "Special offer available" }
        }
    }

    companion object {
        private var instance: OfferValidationService? = null

        fun getInstance(): OfferValidationService {
            if (instance == null) {
                instance = OfferValidationService()
            }
            return instance!!
        }
    }
}
