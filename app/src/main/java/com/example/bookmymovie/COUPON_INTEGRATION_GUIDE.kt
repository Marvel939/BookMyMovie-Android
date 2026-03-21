@file:Suppress("UNUSED_PARAMETER", "UNUSED_VARIABLE", "UNCHECKED_CAST", "unused")

package com.example.bookmymovie

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bookmymovie.ui.viewmodel.OffersViewModel
import com.example.bookmymovie.ui.components.CouponInputField
import com.example.bookmymovie.model.Booking

/**
 * INTEGRATION GUIDE: Adding Coupons to BookingSummaryScreen
 * 
 * This guide explains how to integrate the coupon system into your existing BookingSummaryScreen.
 * 
 * 1. IMPORT THE NECESSARY COMPONENTS
 */

// Add these imports to your BookingSummaryScreen
// import com.example.bookmymovie.ui.viewmodel.OffersViewModel
// import com.example.bookmymovie.ui.components.offers.CouponInputField
// import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * 2. UPDATE YOUR BOOKING DATA MODEL
 * 
 * Open model/Booking.kt and add these fields to the Booking data class:
 */

/*
data class Booking(
    // ... existing fields ...
    
    // ADD THESE NEW FIELDS:
    @PropertyName("appliedCouponId")
    val appliedCouponId: String? = null,
    
    @PropertyName("appliedCouponCode")
    val appliedCouponCode: String? = null,
    
    @PropertyName("discountAmount")
    val discountAmount: Double = 0.0,
    
    @PropertyName("originalPrice")
    val originalPrice: Double = 0.0,
    
    @PropertyName("finalPrice")
    val finalPrice: Double = 0.0
)
*/

/**
 * 3. UPDATE BOOKING SUMMARY SCREEN
 * 
 * Add the CouponInputField component to your BookingSummaryScreen:
 */

/*
// Example implementation:
@Composable
fun BookingSummaryScreen(
    bookingData: Booking,
    theatreId: String,
    showId: String,
    onConfirmBooking: (Booking) -> Unit,
    onBackClick: () -> Unit
) {
    // Create OffersViewModel for coupon management
    val offersViewModel: OffersViewModel = viewModel()
    
    // Get coupon-related states
    val appliedCoupon by offersViewModel.appliedCoupon.collectAsState()
    val discountAmount by offersViewModel.discountAmount.collectAsState()
    val finalAmount by offersViewModel.finalAmount.collectAsState()
    val couponValidationResult by offersViewModel.couponValidationResult.collectAsState()
    val errorMessage by offersViewModel.errorMessage.collectAsState()
    val isLoading by offersViewModel.isLoading.collectAsState()
    
    var couponCode by remember { mutableStateOf("") }
    val originalAmount = calculateOriginalBookingAmount(bookingData)
    
    Column(modifier = Modifier.fillMaxSize()) {
        // ... Your existing booking summary content ...
        
        // PRICE BREAKDOWN SECTION
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Original Price:")
                    Text("₹${originalAmount.toInt()}")
                }
                
                // COUPON INPUT FIELD
                CouponInputField(
                    offersViewModel = offersViewModel,
                    minBookingAmount = originalAmount.toInt()
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Discount row (only show if discount applied)
                if (appliedCoupon != null && discountAmount > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Discount:",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "- ₹${discountAmount.toInt()}",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                // Final price
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Total Price:",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "₹${finalAmount.toInt()}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (discountAmount > 0) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // CONFIRM BOOKING BUTTON
        Button(
            onClick = {
                // Update booking data with coupon info before submitting
                val updatedBookingData = bookingData.copy(
                    discountAmount = discountAmount,
                    discountCode = appliedCoupon?.code ?: ""
                )
                onConfirmBooking(updatedBookingData)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(48.dp),
            enabled = finalAmount > 0
        ) {
            Text("Confirm & Pay ₹${finalAmount.toInt()}")
        }
    }
}
*/

/**
 * 4. UPDATE PAYMENT PROCESSING
 * 
 * When creating booking in your repository/service:
 */

/*
// In your booking creation function (e.g., in BookingRepository):
suspend fun createBooking(bookingRequest: Booking): String {
    // ... existing code ...
    
    val booking = Booking(
        // ... existing fields ...
        discountAmount = bookingRequest.discountAmount,
        discountCode = bookingRequest.discountCode
    )
    
    // Call Cloud Function to validate coupon and process payment
    // val result = callCloudFunction("createBookingWithCoupon", mapOf(
    //     "booking" to booking,
    //     "couponCode" to (bookingRequest.discountCode ?: "")
    // ))
    
    // ... rest of booking creation ...
}
*/

/**
 * 5. CLOUD FUNCTION IMPLEMENTATION
 * 
 * Add this to your functions/index.js:
 */

/*
exports.createBookingWithCoupon = functions.https.onCall(async (data, context) => {
    const { booking, couponCode } = data;
    const uid = context.auth.uid;
    
    try {
        // If coupon is applied, validate and redeem it
        if (couponCode) {
            // Query coupons by code
            const couponSnapshot = await admin.database()
                .ref('offer_coupons')
                .orderByChild('code')
                .equalTo(couponCode)
                .once('value');
            
            if (!couponSnapshot.exists()) {
                throw new Error('Coupon not found');
            }
            
            const couponId = Object.keys(couponSnapshot.val())[0];
            const coupon = couponSnapshot.val()[couponId];
            
            // Validate coupon
            const currentTime = Date.now();
            if (coupon.validUntil < currentTime) {
                throw new Error('Coupon has expired');
            }
            
            if (coupon.maxRedemptions > 0 && coupon.redemptionCount >= coupon.maxRedemptions) {
                throw new Error('Coupon redemption limit reached');
            }
            
            if (coupon.usedByUsers && coupon.usedByUsers[uid]) {
                throw new Error('You have already used this coupon');
            }
            
            // Increment redemption count
            await admin.database()
                .ref(`offer_coupons/${couponId}/redemptionCount`)
                .set(coupon.redemptionCount + 1);
            
            // Mark user as having used this coupon
            await admin.database()
                .ref(`offer_coupons/${couponId}/usedByUsers/${uid}`)
                .set(true);
            
            // Log analytics
            await admin.database()
                .ref('offer_analytics').push().set({
                    offerId: coupon.offerId,
                    couponId: couponId,
                    userId: uid,
                    bookingId: booking.id,
                    appliedAmount: booking.discountAmount,
                    originalAmount: booking.originalPrice,
                    finalAmount: booking.finalPrice,
                    timestamp: currentTime,
                    theatreId: booking.theatreId,
                    movieId: booking.movieId
                });
        }
        
        // Create the booking with discounted price
        const bookingId = admin.database().ref().child('bookings').push().key;
        await admin.database()
            .ref(`bookings/${bookingId}`)
            .set({
                ...booking,
                id: bookingId,
                createdAt: Date.now()
            });
        
        // Process payment with finalPrice (already discounted)
        // Call Stripe or your payment provider with booking.finalPrice
        
        return { success: true, bookingId };
        
    } catch (error) {
        throw new functions.https.HttpsError('failed-precondition', error.message);
    }
});
*/

/**
 * 6. UPDATE BOOKING REQUEST MODEL
 * 
 * Update your BookingRequest data class to include coupon fields:
 */

/*
data class BookingRequest(
    // ... existing fields ...
    val appliedCouponId: String? = null,
    val appliedCouponCode: String? = null,
    val discountAmount: Double = 0.0,
    val originalPrice: Double = 0.0,
    val finalPrice: Double = 0.0
)
*/

/**
 * 7. HELPER FUNCTIONS
 */

/*
fun calculateOriginalBookingAmount(bookingData: BookingRequest): Double {
    // Calculate based on: seats + food items
    // This should match your existing booking calculation logic
    var total = 0.0
    total += bookingData.bookedSeats.size * bookingData.seatPrice
    total += bookingData.foodItems.sumOf { it.price * it.quantity }
    return total
}

fun getCurrentUserId(): String {
    // Get from Firebase Auth
    return FirebaseAuth.getInstance().currentUser?.uid ?: ""
}
*/

/**
 * 8. DISPLAYING COUPON INFO IN BOOKING CONFIRMATION
 * 
 * In your BookingConfirmationScreen:
 */

/*
if (booking.appliedCouponCode?.isNotEmpty() == true) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Coupon Applied", fontWeight = FontWeight.Bold)
            Text("Code: ${booking.appliedCouponCode}")
            Text("Discount: ₹${booking.discountAmount.toInt()}")
        }
    }
}
*/

/**
 * END OF INTEGRATION GUIDE
 * 
 * Summary of Changes:
 * 1. ✅ Import CouponInputField component
 * 2. ✅ Add discount fields to Booking model
 * 3. ✅ Add CouponInputField to BookingSummaryScreen
 * 4. ✅ Update price calculation logic
 * 5. ✅ Update booking creation to include coupon
 * 6. ✅ Implement Cloud Function for validation
 * 7. ✅ Update BookingRequest model
 * 8. ✅ Show coupon info in confirmation screen
 */
