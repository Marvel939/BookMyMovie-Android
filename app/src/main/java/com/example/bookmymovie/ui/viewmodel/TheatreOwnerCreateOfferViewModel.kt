package com.example.bookmymovie.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookmymovie.data.repository.OfferApprovalRepository
import com.example.bookmymovie.data.repository.OffersRepository
import com.example.bookmymovie.data.repository.CouponsRepository
import com.example.bookmymovie.model.Coupon
import com.example.bookmymovie.model.Offer
import com.example.bookmymovie.model.OfferApproval
import com.example.bookmymovie.model.OfferCategory
import com.example.bookmymovie.model.OfferStatus
import com.example.bookmymovie.model.OfferType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TheatreOwnerCreateOfferViewModel : ViewModel() {
    
    private val offersRepository = OffersRepository.getInstance()
    private val couponsRepository = CouponsRepository.getInstance()
    private val approvalRepository = OfferApprovalRepository.getInstance()

    // Form state - Offer details
    private val _offerTitle = MutableStateFlow("")
    val offerTitle: StateFlow<String> = _offerTitle.asStateFlow()

    private val _offerDescription = MutableStateFlow("")
    val offerDescription: StateFlow<String> = _offerDescription.asStateFlow()

    private val _offerType = MutableStateFlow(OfferType.PERCENTAGE)
    val offerType: StateFlow<OfferType> = _offerType.asStateFlow()

    private val _offerCategory = MutableStateFlow(OfferCategory.THEATRE_SPECIFIC)
    val offerCategory: StateFlow<OfferCategory> = _offerCategory.asStateFlow()

    private val _targetMovieName = MutableStateFlow("")
    val targetMovieName: StateFlow<String> = _targetMovieName.asStateFlow()

    private val _discountValue = MutableStateFlow("")
    val discountValue: StateFlow<String> = _discountValue.asStateFlow()

    private val _minBookingAmount = MutableStateFlow("200")
    val minBookingAmount: StateFlow<String> = _minBookingAmount.asStateFlow()

    private val _validFrom = MutableStateFlow(System.currentTimeMillis())
    val validFrom: StateFlow<Long> = _validFrom.asStateFlow()

    private val _validUntil = MutableStateFlow(System.currentTimeMillis() + (30 * 24 * 60 * 60 * 1000)) // 30 days
    val validUntil: StateFlow<Long> = _validUntil.asStateFlow()

    private val _selectedTheatreId = MutableStateFlow("")
    val selectedTheatreId: StateFlow<String> = _selectedTheatreId.asStateFlow()

    private val _selectedTheatreName = MutableStateFlow("")
    val selectedTheatreName: StateFlow<String> = _selectedTheatreName.asStateFlow()

    private val _applicableToAllCities = MutableStateFlow(false)
    val applicableToAllCities: StateFlow<Boolean> = _applicableToAllCities.asStateFlow()

    private val _imageUrl = MutableStateFlow("")
    val imageUrl: StateFlow<String> = _imageUrl.asStateFlow()

    private val _termsAndConditions = MutableStateFlow("")
    val termsAndConditions: StateFlow<String> = _termsAndConditions.asStateFlow()

    // Coupons list
    private val _couponCodes = MutableStateFlow<List<String>>(emptyList())
    val couponCodes: StateFlow<List<String>> = _couponCodes.asStateFlow()

    private val _couponMaxRedemptions = MutableStateFlow<List<Int>>(emptyList())
    val couponMaxRedemptions: StateFlow<List<Int>> = _couponMaxRedemptions.asStateFlow()

    // UI state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow("")
    val successMessage: StateFlow<String> = _successMessage.asStateFlow()

    // State for theatre owner's offers history
    private val _theatreOwnerOffers = MutableStateFlow<List<OfferApproval>>(emptyList())
    val theatreOwnerOffers: StateFlow<List<OfferApproval>> = _theatreOwnerOffers.asStateFlow()

    /**
     * Update offer title
     */
    fun setOfferTitle(title: String) {
        _offerTitle.value = title
    }

    /**
     * Update offer description
     */
    fun setOfferDescription(description: String) {
        _offerDescription.value = description
    }

    /**
     * Update offer type
     */
    fun setOfferType(type: OfferType) {
        _offerType.value = type
    }

    /**
     * Update offer category
     */
    fun setOfferCategory(category: OfferCategory) {
        _offerCategory.value = category
        if (category == OfferCategory.BANK_PAYMENT || category == OfferCategory.PLATFORM_WIDE) {
            _applicableToAllCities.value = true
        }
    }

    /**
     * Update target movie for movie-specific offers
     */
    fun setTargetMovieName(name: String) {
        _targetMovieName.value = name
    }

    /**
     * Update discount value
     */
    fun setDiscountValue(value: String) {
        _discountValue.value = value
    }

    /**
     * Update minimum booking amount
     */
    fun setMinBookingAmount(amount: String) {
        _minBookingAmount.value = amount
    }

    /**
     * Update valid from date
     */
    fun setValidFrom(timestamp: Long) {
        _validFrom.value = timestamp
    }

    /**
     * Update valid until date
     */
    fun setValidUntil(timestamp: Long) {
        _validUntil.value = timestamp
    }

    /**
     * Select theatre
     */
    fun selectTheatre(theatreId: String, theatreName: String) {
        _selectedTheatreId.value = theatreId
        _selectedTheatreName.value = theatreName
    }

    /**
     * Toggle applicable to all cities
     */
    fun setApplicableToAllCities(applicable: Boolean) {
        _applicableToAllCities.value = applicable
    }

    /**
     * Set image URL
     */
    fun setImageUrl(url: String) {
        _imageUrl.value = url
    }

    /**
     * Set terms and conditions
     */
    fun setTermsAndConditions(text: String) {
        _termsAndConditions.value = text
    }

    /**
     * Add a coupon code
     */
    fun addCouponCode(code: String, maxRedemptions: Int) {
        val codes = _couponCodes.value.toMutableList()
        val redemptions = _couponMaxRedemptions.value.toMutableList()
        
        codes.add(code)
        redemptions.add(maxRedemptions)
        
        _couponCodes.value = codes
        _couponMaxRedemptions.value = redemptions
    }

    /**
     * Remove a coupon code
     */
    fun removeCouponCode(index: Int) {
        val codes = _couponCodes.value.toMutableList()
        val redemptions = _couponMaxRedemptions.value.toMutableList()
        
        if (index in 0 until codes.size) {
            codes.removeAt(index)
            redemptions.removeAt(index)
            
            _couponCodes.value = codes
            _couponMaxRedemptions.value = redemptions
        }
    }

    /**
     * Submit offer for approval
     */
    fun submitOffer(
        theatreOwnerId: String,
        theatreOwnerName: String,
        cityId: String,
        isAdminMode: Boolean
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = ""
            _successMessage.value = ""

            try {
                // Auto-bind theatre values for owner mode when not explicitly selected
                if (!isAdminMode && _selectedTheatreId.value.isBlank()) {
                    _selectedTheatreId.value = theatreOwnerId
                    _selectedTheatreName.value = if (theatreOwnerName.isBlank()) "Owner Theatre" else theatreOwnerName
                }

                // Role policy: platform-wide and bank/payment offers are admin-only
                if (!isAdminMode &&
                    (_offerCategory.value == OfferCategory.PLATFORM_WIDE || _offerCategory.value == OfferCategory.BANK_PAYMENT)
                ) {
                    _errorMessage.value = "Only admin can create Platform-Wide and Bank/Payment offers"
                    _isLoading.value = false
                    return@launch
                }

                // Validation
                if (_offerTitle.value.isEmpty()) {
                    _errorMessage.value = "Offer title is required"
                    _isLoading.value = false
                    return@launch
                }

                if (_discountValue.value.isEmpty()) {
                    _errorMessage.value = "Discount value is required"
                    _isLoading.value = false
                    return@launch
                }

                if ((_offerCategory.value == OfferCategory.THEATRE_SPECIFIC ||
                            _offerCategory.value == OfferCategory.MOVIE_SPECIFIC) &&
                    _selectedTheatreId.value.isEmpty()
                ) {
                    _errorMessage.value = "Please select a theatre"
                    _isLoading.value = false
                    return@launch
                }

                if (_offerCategory.value == OfferCategory.MOVIE_SPECIFIC && _targetMovieName.value.isBlank()) {
                    _errorMessage.value = "Please enter target movie name"
                    _isLoading.value = false
                    return@launch
                }

                if (_couponCodes.value.isEmpty()) {
                    _errorMessage.value = "Please add at least one coupon code"
                    _isLoading.value = false
                    return@launch
                }

                // Create offer
                val offer = Offer(
                    title = _offerTitle.value,
                    description = _offerDescription.value,
                    type = _offerType.value.name,
                    value = _discountValue.value.toDoubleOrNull() ?: 0.0,
                    minBookingAmount = _minBookingAmount.value.toDoubleOrNull() ?: 200.0,
                    theatreOwnerId = theatreOwnerId,
                    theatreId = _selectedTheatreId.value,
                    theatreName = _selectedTheatreName.value,
                    cityId = cityId,
                    validFrom = _validFrom.value,
                    validUntil = _validUntil.value,
                    imageUrl = _imageUrl.value,
                    termsAndConditions = _termsAndConditions.value,
                    category = _offerCategory.value.name,
                    targetMovieName = _targetMovieName.value,
                    paymentMethod = if (_offerCategory.value == OfferCategory.BANK_PAYMENT) "STRIPE" else "ANY",
                    applicableToAllCities = _applicableToAllCities.value ||
                            _offerCategory.value == OfferCategory.BANK_PAYMENT ||
                            _offerCategory.value == OfferCategory.PLATFORM_WIDE,
                    status = if (isAdminMode) OfferStatus.APPROVED.name else OfferStatus.PENDING.name
                )

                // Save offer
                val offerId = offersRepository.createOffer(offer)

                // Create coupons for the offer and persist to Firebase
                _couponCodes.value.forEachIndexed { index, codeRaw ->
                    val code = codeRaw.trim().uppercase()
                    val coupon = Coupon(
                        code = code,
                        offerId = offerId,
                        maxRedemptions = _couponMaxRedemptions.value.getOrNull(index) ?: 0,
                        validFrom = _validFrom.value,
                        validUntil = _validUntil.value,
                        isActive = isAdminMode // activate immediately only if admin created
                    )
                    try {
                        couponsRepository.createCoupon(coupon)
                    } catch (e: Exception) {
                        // If coupon creation fails, continue but capture error for user
                        _errorMessage.value = "Failed to create coupon $code: ${e.message}"
                    }
                }

                if (!isAdminMode) {
                    // Create approval request for owner-created offers
                    val approval = OfferApproval(
                        offerId = offerId,
                        offer = offer,
                        theatreOwnerId = theatreOwnerId,
                        theatreOwnerName = theatreOwnerName,
                        theatreName = _selectedTheatreName.value,
                        createdAt = System.currentTimeMillis()
                    )
                    approvalRepository.createApprovalRequest(approval)
                    _successMessage.value = "Offer submitted for approval! Admin will review and approve/reject soon."
                } else {
                    _successMessage.value = "Offer created successfully"
                }

                // Clear form
                clearForm()

                // Reload owner offers
                loadTheatreOwnerOffers(theatreOwnerId)

            } catch (e: Exception) {
                _errorMessage.value = "Failed to submit offer: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Load offers created by theatre owner
     */
    fun loadTheatreOwnerOffers(theatreOwnerId: String) {
        viewModelScope.launch {
            try {
                approvalRepository.getApprovalsByTheatreOwner(theatreOwnerId).collect { approvals ->
                    _theatreOwnerOffers.value = approvals
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load offers: ${e.message}"
            }
        }
    }

    /**
     * Clear form for new offer
     */
    private fun clearForm() {
        _offerTitle.value = ""
        _offerDescription.value = ""
        _offerType.value = OfferType.PERCENTAGE
        _offerCategory.value = OfferCategory.THEATRE_SPECIFIC
        _targetMovieName.value = ""
        _discountValue.value = ""
        _minBookingAmount.value = "200"
        _validFrom.value = System.currentTimeMillis()
        _validUntil.value = System.currentTimeMillis() + (30 * 24 * 60 * 60 * 1000)
        _selectedTheatreId.value = ""
        _selectedTheatreName.value = ""
        _imageUrl.value = ""
        _termsAndConditions.value = ""
        _couponCodes.value = emptyList()
        _couponMaxRedemptions.value = emptyList()
    }

    /**
     * Clear all messages
     */
    fun clearMessages() {
        _errorMessage.value = ""
        _successMessage.value = ""
    }
}
