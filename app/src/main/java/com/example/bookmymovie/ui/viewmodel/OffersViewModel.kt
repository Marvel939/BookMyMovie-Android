package com.example.bookmymovie.ui.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookmymovie.data.repository.CouponsRepository
import com.example.bookmymovie.data.repository.OffersRepository
import com.example.bookmymovie.data.service.OfferValidationService
import com.example.bookmymovie.model.Coupon
import com.example.bookmymovie.model.CouponValidationResult
import com.example.bookmymovie.model.Offer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OffersViewModel : ViewModel() {
    
    private val offersRepository = OffersRepository.getInstance()
    private val couponsRepository = CouponsRepository.getInstance()
    private val validationService = OfferValidationService.getInstance()

    // State for carousel offers (real-time approved offers)
    private val _carouselOffers = MutableStateFlow<List<Offer>>(emptyList())
    val carouselOffers: StateFlow<List<Offer>> = _carouselOffers.asStateFlow()

    // State for personalized offers
    private val _personalizedOffers = MutableStateFlow<List<Offer>>(emptyList())
    val personalizedOffers: StateFlow<List<Offer>> = _personalizedOffers.asStateFlow()

    // State for selected offer (detail view)
    private val _selectedOffer = MutableStateFlow<Offer?>(null)
    val selectedOffer: StateFlow<Offer?> = _selectedOffer.asStateFlow()

    // State for coupons of selected offer
    private val _offersForSelectedOffer = MutableStateFlow<List<Coupon>>(emptyList())
    val couponsForSelectedOffer: StateFlow<List<Coupon>> = _offersForSelectedOffer.asStateFlow()

    // State for applied coupon in checkout flow
    private val _appliedCoupon = MutableStateFlow<Coupon?>(null)
    val appliedCoupon: StateFlow<Coupon?> = _appliedCoupon.asStateFlow()

    // State for discount calculation
    private val _discountAmount = MutableStateFlow(0.0)
    val discountAmount: StateFlow<Double> = _discountAmount.asStateFlow()

    // State for final amount
    private val _finalAmount = MutableStateFlow(0.0)
    val finalAmount: StateFlow<Double> = _finalAmount.asStateFlow()

    // State for loading
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // State for error messages
    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()

    // State for coupon validation result
    private val _couponValidationResult = MutableStateFlow<CouponValidationResult?>(null)
    val couponValidationResult: StateFlow<CouponValidationResult?> = _couponValidationResult.asStateFlow()

    /**
     * Load home screen offers (carousel + personalized)
     */
    fun loadHomeOffers(cityId: String, userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Load approved offers for carousel (real-time listener)
                offersRepository.getOffersForCity(cityId).collect { offers ->
                    _carouselOffers.value = offers.take(10) // Show top 10 offers
                    // Keep personalized offers in sync from the same approved stream.
                    _personalizedOffers.value = offers.filter { it.isActive }
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load offers: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    /**
     * Get personalized offers based on user booking history
     * For now, returns random approved offers. Can be enhanced with ML later.
     */
    private suspend fun getPersonalizedOffers(userId: String, cityId: String): List<Offer> {
        return try {
            var personalizedOffers = emptyList<Offer>()
            offersRepository.getOffersForCity(cityId).collect { offers ->
                // Filter to show only active offers
                personalizedOffers = offers.filter { it.isActive }
            }
            personalizedOffers
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Select an offer to view details
     */
    fun selectOffer(offer: Offer) {
        _selectedOffer.value = offer
        // Load coupons for this offer
        viewModelScope.launch {
            couponsRepository.getCouponsByOffer(offer.id).collect { coupons ->
                _offersForSelectedOffer.value = coupons
            }
        }
    }

    /**
     * Clear selected offer
     */
    fun clearSelectedOffer() {
        _selectedOffer.value = null
        _offersForSelectedOffer.value = emptyList()
    }

    /**
     * Apply coupon code to booking
     */
    fun applyCoupon(
        couponCode: String,
        bookingAmount: Double,
        userId: String,
        theatreId: String? = null,
        paymentMethod: String? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = validationService.validateCouponApplication(
                    couponCode = couponCode,
                    bookingAmount = bookingAmount,
                    userId = userId,
                    theatreId = theatreId,
                    paymentMethod = paymentMethod
                )

                _couponValidationResult.value = result

                if (result.isValid && result.coupon != null) {
                    _appliedCoupon.value = result.coupon
                    _discountAmount.value = result.discountAmount
                    _finalAmount.value = result.finalAmount
                    _errorMessage.value = ""
                } else {
                    _appliedCoupon.value = null
                    _discountAmount.value = 0.0
                    _finalAmount.value = bookingAmount
                    _errorMessage.value = result.errorMessage
                }
            } catch (e: Exception) {
                _appliedCoupon.value = null
                _discountAmount.value = 0.0
                _finalAmount.value = bookingAmount
                _errorMessage.value = "Error applying coupon: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Remove applied coupon
     */
    fun removeCoupon(originalAmount: Double) {
        _appliedCoupon.value = null
        _discountAmount.value = 0.0
        _finalAmount.value = originalAmount
        _errorMessage.value = ""
        _couponValidationResult.value = null
    }

    /**
     * Get discount percentage text for UI display
     */
    fun getDiscountPercentageText(offer: Offer): String {
        return validationService.getDiscountPercentage(offer)
    }

    /**
     * Get offer description for UI display
     */
    fun getOfferDescriptionText(offer: Offer): String {
        return validationService.buildOfferDescription(offer)
    }

    /**
     * Clear all state
     */
    fun clearAll() {
        _carouselOffers.value = emptyList()
        _personalizedOffers.value = emptyList()
        _selectedOffer.value = null
        _offersForSelectedOffer.value = emptyList()
        _appliedCoupon.value = null
        _discountAmount.value = 0.0
        _finalAmount.value = 0.0
        _errorMessage.value = ""
        _couponValidationResult.value = null
    }
}
