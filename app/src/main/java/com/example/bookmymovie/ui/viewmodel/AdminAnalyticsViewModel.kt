package com.example.bookmymovie.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.bookmymovie.model.Booking
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

data class AdminAnalyticsState(
    val totalTicketsSold: Int = 0,
    val totalProfit: Double = 0.0,
    val weeklyProfit: Double = 0.0,
    val monthlyProfit: Double = 0.0,
    val yearlyProfit: Double = 0.0,
    val totalUsers: Int = 0,
    val totalTheatreOwners: Int = 0,
    val totalOffers: Int = 0,
    val totalTheatres: Int = 0,
    val topMovies: List<Pair<String, Double>> = emptyList(),
    val seatDistribution: Map<String, Int> = emptyMap(),
    val recentBookings: List<Booking> = emptyList(),
    val isLoading: Boolean = true
)

class AdminAnalyticsViewModel : ViewModel() {

    private val db = FirebaseDatabase.getInstance()
    
    private val _state = MutableStateFlow(AdminAnalyticsState())
    val state: StateFlow<AdminAnalyticsState> = _state.asStateFlow()

    init {
        startListening()
    }

    private fun startListening() {
        listenToBookings()
        listenToUsers()
        listenToTheatreOwners()
        listenToOffers()
        listenToTheatres()
    }

    private fun listenToBookings() {
        db.getReference("all_bookings").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val bookings = mutableListOf<Booking>()
                var totalTickets = 0
                var totalProfit = 0.0
                var weeklyProfit = 0.0
                var monthlyProfit = 0.0
                var yearlyProfit = 0.0
                val movieRevenueMap = mutableMapOf<String, Double>()
                val seatTypeMap = mutableMapOf<String, Int>()

                val now = System.currentTimeMillis()
                val calendar = Calendar.getInstance()
                
                calendar.timeInMillis = now
                val currentYear = calendar.get(Calendar.YEAR)
                val currentMonth = calendar.get(Calendar.MONTH)
                val currentWeek = calendar.get(Calendar.WEEK_OF_YEAR)

                snapshot.children.forEach { child ->
                    val booking = parseBooking(child) ?: return@forEach
                    bookings.add(booking)
                    
                    if (booking.status == "confirmed") {
                        val amount = booking.totalAmount.toDouble()
                        totalProfit += amount
                        totalTickets += booking.seats.size
                        
                        // Profit breakdown
                        calendar.timeInMillis = booking.bookedAt
                        val bookingYear = calendar.get(Calendar.YEAR)
                        val bookingMonth = calendar.get(Calendar.MONTH)
                        val bookingWeek = calendar.get(Calendar.WEEK_OF_YEAR)
                        
                        if (bookingYear == currentYear) {
                            yearlyProfit += amount
                            if (bookingMonth == currentMonth) {
                                monthlyProfit += amount
                            }
                            if (bookingWeek == currentWeek) {
                                weeklyProfit += amount
                            }
                        }

                        // Top Movies
                        movieRevenueMap[booking.movieName] = (movieRevenueMap[booking.movieName] ?: 0.0) + amount
                        
                        // Seat Distribution
                        booking.seatTypes.values.forEach { type ->
                            seatTypeMap[type] = (seatTypeMap[type] ?: 0) + 1
                        }
                    }
                }

                _state.value = _state.value.copy(
                    recentBookings = bookings.sortedByDescending { it.bookedAt },
                    totalTicketsSold = totalTickets,
                    totalProfit = totalProfit,
                    weeklyProfit = weeklyProfit,
                    monthlyProfit = monthlyProfit,
                    yearlyProfit = yearlyProfit,
                    topMovies = movieRevenueMap.toList().sortedByDescending { it.second }.take(5),
                    seatDistribution = seatTypeMap,
                    isLoading = false
                )
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun listenToUsers() {
        db.getReference("users").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _state.value = _state.value.copy(totalUsers = snapshot.childrenCount.toInt())
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun listenToTheatreOwners() {
        db.getReference("theatre_owners").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _state.value = _state.value.copy(totalTheatreOwners = snapshot.childrenCount.toInt())
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun listenToOffers() {
        db.getReference("offers").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _state.value = _state.value.copy(totalOffers = snapshot.childrenCount.toInt())
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun listenToTheatres() {
        // We count from both 'cinemas' (Google Places cache) and 'theatres' (seeded data)
        // and also approved 'theatre_owners' to get a complete picture.
        
        val database = FirebaseDatabase.getInstance()
        var theatresCount = 0
        var cinemasCount = 0
        var approvedOwnersCount = 0

        // 1. Listen to 'theatres' (city-based structure)
        database.getReference("theatres").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var total = 0
                snapshot.children.forEach { citySnap ->
                    total += citySnap.childrenCount.toInt()
                }
                theatresCount = total
                updateTotalTheatres(theatresCount, cinemasCount, approvedOwnersCount)
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        // 2. Listen to 'cinemas' (placeId-based structure)
        database.getReference("cinemas").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                cinemasCount = snapshot.childrenCount.toInt()
                updateTotalTheatres(theatresCount, cinemasCount, approvedOwnersCount)
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        // 3. Listen to 'theatre_owners' for approved ones (as fallback)
        database.getReference("theatre_owners").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                approvedOwnersCount = snapshot.children.count { 
                    it.child("status").value == "approved" 
                }
                updateTotalTheatres(theatresCount, cinemasCount, approvedOwnersCount)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun updateTotalTheatres(t: Int, c: Int, o: Int) {
        // We use the maximum to avoid double counting if nodes overlap, 
        // or sum them if they are distinct. In this app, 'cinemas' is the most accurate for real data.
        // If 'cinemas' is empty, we fall back to others.
        val total = maxOf(t, c, o)
        _state.value = _state.value.copy(totalTheatres = total)
    }

    private fun parseBooking(child: DataSnapshot): Booking? {
        return try {
            val seats = child.child("seats").children.mapNotNull { it.getValue(String::class.java) }
            val seatTypes = mutableMapOf<String, String>()
            child.child("seatTypes").children.forEach { 
                val key = it.key ?: return@forEach
                val value = it.getValue(String::class.java) ?: return@forEach
                seatTypes[key] = value
            }
            
            val foodItems = mutableListOf<Map<String, Any>>()
            child.child("foodItems").children.forEach { foodSnap ->
                val foodMap = mutableMapOf<String, Any>()
                foodSnap.children.forEach { propSnap ->
                    val key = propSnap.key ?: return@forEach
                    val value = propSnap.value ?: return@forEach
                    foodMap[key] = value
                }
                foodItems.add(foodMap)
            }

            Booking(
                bookingId = child.child("bookingId").getValue(String::class.java) ?: "",
                userId = child.child("userId").getValue(String::class.java) ?: "",
                userEmail = child.child("userEmail").getValue(String::class.java) ?: "",
                userName = child.child("userName").getValue(String::class.java) ?: "",
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
                ticketGstRate = (child.child("ticketGstRate").value as? Long)?.toInt() ?: 18,
                ticketGstAmount = (child.child("ticketGstAmount").value as? Long)?.toInt() ?: 0,
                convenienceFeeAmount = (child.child("convenienceFeeAmount").value as? Long)?.toInt() ?: 0,
                convenienceFeeGstAmount = (child.child("convenienceFeeGstAmount").value as? Long)?.toInt() ?: 0,
                totalAmount = (child.child("totalAmount").value as? Long)?.toInt() ?: 0,
                discountAmount = (child.child("discountAmount").value as? Long)?.toInt() ?: 0,
                discountCode = child.child("discountCode").getValue(String::class.java) ?: "",
                appliedCouponId = child.child("appliedCouponId").getValue(String::class.java) ?: "",
                refundableAmount = (child.child("refundableAmount").value as? Long)?.toInt() ?: 0,
                nonRefundableAmount = (child.child("nonRefundableAmount").value as? Long)?.toInt() ?: 0,
                paymentIntentId = child.child("paymentIntentId").getValue(String::class.java) ?: "",
                paymentMethod = child.child("paymentMethod").getValue(String::class.java) ?: "stripe",
                paymentStatus = child.child("paymentStatus").getValue(String::class.java) ?: "paid",
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
