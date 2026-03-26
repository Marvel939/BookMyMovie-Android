package com.example.bookmymovie.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.bookmymovie.model.Booking
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

data class TheatreAnalyticsState(
    val totalRevenue: Double = 0.0,
    val totalTicketsSold: Int = 0,
    val totalRefunds: Double = 0.0,
    val weeklyProfit: Double = 0.0,
    val monthlyProfit: Double = 0.0,
    val yearlyProfit: Double = 0.0,
    val profitData: Map<String, Double> = emptyMap(), // For Chart
    val topMovies: Map<String, Double> = emptyMap(), // For Chart
    val recentBookings: List<Booking> = emptyList(),
    val refundBookings: List<Booking> = emptyList(),
    val isLoading: Boolean = true
)

class TheatreAnalyticsViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance()
    private val _state = MutableStateFlow(TheatreAnalyticsState())
    val state: StateFlow<TheatreAnalyticsState> = _state.asStateFlow()

    private var placeId: String? = null

    init {
        loadOwnerPlaceId()
    }

    private fun loadOwnerPlaceId() {
        val uid = auth.currentUser?.uid ?: return
        db.getReference("theatre_owners").child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                placeId = snapshot.child("placeId").value as? String
                if (placeId != null) {
                    listenToBookings()
                } else {
                    _state.value = _state.value.copy(isLoading = false)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                _state.value = _state.value.copy(isLoading = false)
            }
        })
    }

    private fun listenToBookings() {
        val currentPlaceId = placeId ?: return
        db.getReference("all_bookings").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val allBookings = mutableListOf<Booking>()
                val refundBookings = mutableListOf<Booking>()
                snapshot.children.forEach { child ->
                    val booking = parseBooking(child)
                    // Filter by placeId
                    if (booking != null && booking.placeId == currentPlaceId) {
                        allBookings.add(booking)
                        if (booking.refundStatus == "succeeded") {
                            refundBookings.add(booking)
                        }
                    }
                }

                processAnalytics(allBookings, refundBookings)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun processAnalytics(bookings: List<Booking>, refunds: List<Booking>) {
        val now = Calendar.getInstance()
        var totalRev = 0.0
        var totalTickets = 0
        var totalRefunds = 0.0
        var weekly = 0.0
        var monthly = 0.0
        var yearly = 0.0
        
        val movieSales = mutableMapOf<String, Double>()
        val profitHistory = mutableMapOf<String, Double>() // Simplified for chart

        bookings.forEach { b ->
            if (b.status == "confirmed") {
                totalRev += b.totalAmount
                totalTickets += b.seats.size
                
                val cal = Calendar.getInstance().apply { timeInMillis = b.bookedAt }
                
                // Weekly
                if (cal.get(Calendar.WEEK_OF_YEAR) == now.get(Calendar.WEEK_OF_YEAR) && 
                    cal.get(Calendar.YEAR) == now.get(Calendar.YEAR)) {
                    weekly += b.totalAmount
                }
                
                // Monthly
                if (cal.get(Calendar.MONTH) == now.get(Calendar.MONTH) && 
                    cal.get(Calendar.YEAR) == now.get(Calendar.YEAR)) {
                    monthly += b.totalAmount
                }
                
                // Yearly
                if (cal.get(Calendar.YEAR) == now.get(Calendar.YEAR)) {
                    yearly += b.totalAmount
                }

                movieSales[b.movieName] = (movieSales[b.movieName] ?: 0.0) + b.totalAmount
                
                // Chart Data (Last 7 days profit)
                val dayKey = "${cal.get(Calendar.DAY_OF_MONTH)}/${cal.get(Calendar.MONTH) + 1}"
                profitHistory[dayKey] = (profitHistory[dayKey] ?: 0.0) + b.totalAmount
            }

            if (b.refundStatus == "succeeded") {
                totalRefunds += b.refundableAmount.toDouble()
            }
        }

        _state.value = TheatreAnalyticsState(
            totalRevenue = totalRev,
            totalTicketsSold = totalTickets,
            totalRefunds = totalRefunds,
            weeklyProfit = weekly,
            monthlyProfit = monthly,
            yearlyProfit = yearly,
            profitData = profitHistory.toList().takeLast(7).toMap(),
            topMovies = movieSales.toList().sortedByDescending { it.second }.take(5).toMap(),
            recentBookings = bookings.sortedByDescending { it.bookedAt },
            refundBookings = refunds.sortedByDescending { it.refundedAt },
            isLoading = false
        )
    }

    private fun parseBooking(child: DataSnapshot): Booking? {
        return try {
            val seats = (child.child("seats").value as? List<*>)?.map { it.toString() } ?: emptyList()
            val seatTypes = (child.child("seatTypes").value as? Map<*, *>)?.map { it.key.toString() to it.value.toString() }?.toMap() ?: emptyMap()
            val foodItems = (child.child("foodItems").value as? List<*>)?.map { it as Map<String, Any> } ?: emptyList()

            Booking(
                bookingId = child.key ?: "",
                userId = child.child("userId").getValue(String::class.java) ?: "",
                userEmail = child.child("userEmail").getValue(String::class.java) ?: "",
                userName = child.child("userName").getValue(String::class.java) ?: "Guest",
                placeId = child.child("placeId").getValue(String::class.java) ?: "",
                cinemaName = child.child("cinemaName").getValue(String::class.java) ?: "",
                cinemaAddress = child.child("cinemaAddress").getValue(String::class.java) ?: "",
                screenId = child.child("screenId").getValue(String::class.java) ?: "",
                screenName = child.child("screenName").getValue(String::class.java) ?: "",
                screenType = child.child("screenType").getValue(String::class.java) ?: "2D",
                showtimeId = child.child("showtimeId").getValue(String::class.java) ?: "",
                movieId = child.child("movieId").getValue(String::class.java) ?: "",
                movieName = child.child("movieName").getValue(String::class.java) ?: "",
                moviePoster = child.child("moviePoster").getValue(String::class.java) ?: "",
                date = child.child("date").getValue(String::class.java) ?: "",
                time = child.child("time").getValue(String::class.java) ?: "",
                language = child.child("language").getValue(String::class.java) ?: "English",
                seats = seats,
                seatTypes = seatTypes,
                seatAmount = (child.child("seatAmount").value as? Long)?.toInt() ?: 0,
                foodItems = foodItems,
                foodAmount = (child.child("foodAmount").value as? Long)?.toInt() ?: 0,
                totalAmount = (child.child("totalAmount").value as? Long)?.toInt() ?: 0,
                discountAmount = (child.child("discountAmount").value as? Long)?.toInt() ?: 0,
                discountCode = child.child("discountCode").getValue(String::class.java) ?: "",
                refundableAmount = (child.child("refundableAmount").value as? Long)?.toInt() ?: 0,
                nonRefundableAmount = (child.child("nonRefundableAmount").value as? Long)?.toInt() ?: 0,
                paymentMethod = child.child("paymentMethod").getValue(String::class.java) ?: "stripe",
                status = child.child("status").getValue(String::class.java) ?: "confirmed",
                refundStatus = child.child("refundStatus").getValue(String::class.java) ?: "none",
                refundReason = child.child("refundReason").getValue(String::class.java) ?: "",
                refundId = child.child("refundId").getValue(String::class.java) ?: "",
                refundedAt = (child.child("refundedAt").value as? Long) ?: 0L,
                bookedAt = (child.child("bookedAt").value as? Long) ?: 0L
            )
        } catch (e: Exception) {
            null
        }
    }
}
