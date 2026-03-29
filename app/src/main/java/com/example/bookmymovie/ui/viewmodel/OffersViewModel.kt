package com.example.bookmymovie.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.bookmymovie.model.Offer
import com.example.bookmymovie.model.OfferApprovalStatus
import com.example.bookmymovie.model.OfferCategory
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.google.firebase.auth.FirebaseAuth
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class OffersViewModel : ViewModel() {
    private val database = FirebaseDatabase.getInstance().reference

    private val _allOffers = MutableStateFlow<List<Offer>>(emptyList())
    
    private val _selectedCategory = MutableStateFlow(OfferCategory.THEATRE_SPECIFIC)
    val selectedCategory: StateFlow<OfferCategory> = _selectedCategory.asStateFlow()

    private val _filteredOffers = MutableStateFlow<List<Offer>>(emptyList())
    val filteredOffers: StateFlow<List<Offer>> = _filteredOffers.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _platformOffers = MutableStateFlow<List<Offer>>(emptyList())
    val platformOffers: StateFlow<List<Offer>> = _platformOffers.asStateFlow()

    // Added fields for coupon checkout
    private val _appliedCoupon = MutableStateFlow<Offer?>(null)
    val appliedCoupon: StateFlow<Offer?> = _appliedCoupon.asStateFlow()

    private val _discountAmount = MutableStateFlow(0.0)
    val discountAmount: StateFlow<Double> = _discountAmount.asStateFlow()

    private val _cashbackAmount = MutableStateFlow(0.0)
    val cashbackAmount: StateFlow<Double> = _cashbackAmount.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isApplying = MutableStateFlow(false)
    val isApplying: StateFlow<Boolean> = _isApplying.asStateFlow()

    init {
        loadOffers()
    }

    private fun loadOffers() {
        database.child("offers").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val offersList = mutableListOf<Offer>()
                for (child in snapshot.children) {
                    val offer = child.getValue(Offer::class.java)
                    if (offer != null && offer.isActive && offer.getApprovalStatusEnum() == OfferApprovalStatus.APPROVED) {
                        offersList.add(offer)
                    }
                }
                _allOffers.value = offersList
                _platformOffers.value = offersList.filter { it.getCategoryEnum() == OfferCategory.PLATFORM_WIDE }
                applyFilter(_selectedCategory.value)
                _isLoading.value = false
            }

            override fun onCancelled(error: DatabaseError) {
                _isLoading.value = false
            }
        })
    }

    fun selectCategory(category: OfferCategory) {
        _selectedCategory.value = category
        applyFilter(category)
    }

    private fun applyFilter(category: OfferCategory) {
        _filteredOffers.value = _allOffers.value.filter { it.getCategoryEnum() == category }
    }

    fun applyCoupon(
        couponCode: String,
        bookingAmount: Double,
        theatreId: String?,
        movieId: String?
    ) {
        _isApplying.value = true
        _errorMessage.value = null

        val upperCode = couponCode.uppercase().trim()
        val offer = _allOffers.value.find { it.couponCode == upperCode }

        if (offer == null) {
            _errorMessage.value = "Invalid coupon code."
            _isApplying.value = false
            return
        }

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            _errorMessage.value = "User not logged in."
            _isApplying.value = false
            return
        }

        // Check redemption limit
        database.child("bookings").child(uid).orderByChild("discountCode").equalTo(upperCode)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val usedCount = snapshot.childrenCount.toInt()
                    if (usedCount >= offer.maxRedemptionsPerUser) {
                        _errorMessage.value = "Coupon already used."
                        _isApplying.value = false
                        return
                    }
                    
                    // Continue with other validations
                    validateAndApply(offer, bookingAmount, theatreId, movieId)
                }

                override fun onCancelled(error: DatabaseError) {
                    _errorMessage.value = "Failed to verify coupon usage."
                    _isApplying.value = false
                }
            })
    }

    private fun validateAndApply(
        offer: Offer,
        bookingAmount: Double,
        theatreId: String?,
        movieId: String?
    ) {
        // Strict validation: Theatre & Movie Check
        if (offer.getCategoryEnum() == OfferCategory.THEATRE_SPECIFIC || 
            offer.getCategoryEnum() == OfferCategory.MOVIE_SPECIFIC ||
            offer.getCategoryEnum() == OfferCategory.PLATFORM_WIDE) {
            
            if (offer.theatreId.isNotEmpty() && offer.theatreId != theatreId) {
                _errorMessage.value = "This coupon code is not valid for this theatre."
                _isApplying.value = false
                return
            }
            
            if (offer.movieId.isNotEmpty() && offer.movieId != movieId) {
                _errorMessage.value = "This coupon code is not valid for this movie."
                _isApplying.value = false
                return
            }
        }

        // Validation: Minimum Order Amount
        if (bookingAmount < offer.minOrderAmount) {
            _errorMessage.value = "Minimum booking amount of ₹${offer.minOrderAmount} required."
            _isApplying.value = false
            return
        }

        // Validation: Date Range
        val currentTime = System.currentTimeMillis()
        if (offer.startDate > 0L && currentTime < offer.startDate) {
            _errorMessage.value = "This coupon is not active yet."
            _isApplying.value = false
            return
        }
        if (offer.endDate > 0L && currentTime > offer.endDate) {
            _errorMessage.value = "This coupon has expired."
            _isApplying.value = false
            return
        }

        // Calculate Value (either discount or cashback)
        val calculatedValue = if (offer.getDiscountTypeEnum() == com.example.bookmymovie.model.DiscountType.PERCENTAGE) {
            val percDist = (bookingAmount * offer.discountPercentage) / 100.0
            if (offer.maxDiscountAmount > 0 && percDist > offer.maxDiscountAmount) {
                offer.maxDiscountAmount.toDouble()
            } else {
                percDist
            }
        } else {
            offer.discountAmount.toDouble()
        }

        if (offer.getCouponTypeEnum() == com.example.bookmymovie.model.CouponType.CASHBACK) {
            _discountAmount.value = 0.0
            _cashbackAmount.value = calculatedValue
        } else {
            if (calculatedValue > bookingAmount) {
                _discountAmount.value = bookingAmount
            } else {
                _discountAmount.value = calculatedValue
            }
            _cashbackAmount.value = 0.0
        }

        _appliedCoupon.value = offer
        _isApplying.value = false
    }

    fun removeCoupon() {
        _appliedCoupon.value = null
        _discountAmount.value = 0.0
        _cashbackAmount.value = 0.0
        _errorMessage.value = null
    }
}
