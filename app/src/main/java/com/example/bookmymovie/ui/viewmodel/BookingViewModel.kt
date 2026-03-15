package com.example.bookmymovie.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookmymovie.model.Booking
import com.example.bookmymovie.model.CartFoodItem
import com.example.bookmymovie.model.FoodItem
import com.example.bookmymovie.model.SeatData
import com.example.bookmymovie.model.CinemaShowtime
import com.example.bookmymovie.services.ReminderScheduler
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class BookingViewModel : ViewModel() {

    // ── Showtime state ──────────────────────────────────────────────────────
    var showtimes by mutableStateOf<List<CinemaShowtime>>(emptyList())
        private set
    var isLoadingShowtimes by mutableStateOf(false)
        private set
    var selectedShowtime by mutableStateOf<CinemaShowtime?>(null)

    // ── Seat state ──────────────────────────────────────────────────────────
    var seats by mutableStateOf<Map<String, SeatData>>(emptyMap())
        private set
    var isLoadingSeats by mutableStateOf(false)
        private set
    var selectedSeats by mutableStateOf<Set<String>>(emptySet())

    // ── Food state ──────────────────────────────────────────────────────────
    var foodItems by mutableStateOf<List<FoodItem>>(emptyList())
        private set
    var isLoadingFood by mutableStateOf(false)
        private set
    val foodCart = mutableStateMapOf<String, Int>() // itemId -> qty

    // ── Format & Language state ─────────────────────────────────────────────
    var selectedFormat by mutableStateOf("")
    var selectedLanguage by mutableStateOf("")

    // ── Booking state ───────────────────────────────────────────────────────
    var currentPlaceId by mutableStateOf("")
    var currentCinemaName by mutableStateOf("")
    var currentCinemaAddress by mutableStateOf("")
    var isCreatingBooking by mutableStateOf(false)
    var confirmedBooking by mutableStateOf<Booking?>(null)
    var bookingError by mutableStateOf<String?>(null)

    // ── Payment state ───────────────────────────────────────────────────────
    var isRequestingPayment by mutableStateOf(false)
    var paymentError by mutableStateOf<String?>(null)
    var pendingPaymentIntentId by mutableStateOf<String?>(null)
    var walletBalance by mutableStateOf(0.0)
        private set

    // ── Refund state ────────────────────────────────────────────────────────
    var isRefunding by mutableStateOf(false)
        private set
    var refundMessage by mutableStateOf<String?>(null)
        private set
    var refundError by mutableStateOf<String?>(null)
        private set

    // ── My bookings ─────────────────────────────────────────────────────────
    var myBookings by mutableStateOf<List<Booking>>(emptyList())
        private set
    var isLoadingMyBookings by mutableStateOf(false)
        private set

    // ── Admin state ─────────────────────────────────────────────────────────
    var isAdmin by mutableStateOf(false)
    var isAdminChecked by mutableStateOf(false)
        private set
    var adminActionSuccess by mutableStateOf<String?>(null)
    var adminActionError by mutableStateOf<String?>(null)

    // ── Computed totals ─────────────────────────────────────────────────────
    val seatAmount: Int
        get() = selectedSeats.sumOf { seats[it]?.price ?: 0 }

    val foodAmount: Int
        get() = foodCart.entries.sumOf { (itemId, qty) ->
            (foodItems.find { it.itemId == itemId }?.price ?: 0) * qty
        }

    val ticketGstRate: Int get() = 18

    val ticketGstAmount: Int
        get() = (seatAmount * (ticketGstRate / 100.0)).roundToInt()

    val convenienceFeeAmount: Int
        get() = if (seatAmount > 0) 30 else 0

    val convenienceFeeGstAmount: Int
        get() = (convenienceFeeAmount * 0.18).roundToInt()

    val refundableAmount: Int get() = seatAmount + foodAmount

    val nonRefundableAmount: Int get() = ticketGstAmount + convenienceFeeAmount + convenienceFeeGstAmount

    val totalAmount: Int
        get() = seatAmount + foodAmount + ticketGstAmount + convenienceFeeAmount + convenienceFeeGstAmount

    val cartItems: List<CartFoodItem>
        get() = foodCart.entries
            .filter { it.value > 0 }
            .mapNotNull { (id, qty) ->
                foodItems.find { it.itemId == id }?.let { CartFoodItem(it, qty) }
            }

    // ── Load showtimes for a cinema on a specific date ──────────────────────
    fun loadShowtimes(placeId: String, date: String) {
        isLoadingShowtimes = true
        showtimes = emptyList()
        val ref = FirebaseDatabase.getInstance().getReference("showtimes").child(placeId)
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val result = mutableListOf<CinemaShowtime>()
                snapshot.children.forEach { screenSnap ->
                    val screenId = screenSnap.key ?: return@forEach
                    screenSnap.children.forEach { stSnap ->
                        val stDate = stSnap.child("date").getValue(String::class.java) ?: ""
                        if (stDate == date) {
                            // Read formatPrices map
                            val fpMap = mutableMapOf<String, Map<String, Int>>()
                            stSnap.child("formatPrices").children.forEach { fmtSnap ->
                                val fmtKey = fmtSnap.key ?: return@forEach
                                val priceMap = mutableMapOf<String, Int>()
                                fmtSnap.children.forEach { priceSnap ->
                                    val typeKey = priceSnap.key ?: return@forEach
                                    priceMap[typeKey] = (priceSnap.value as? Long)?.toInt() ?: 0
                                }
                                fpMap[fmtKey] = priceMap
                            }

                            result.add(
                                CinemaShowtime(
                                    showtimeId = stSnap.key ?: "",
                                    screenId = screenId,
                                    screenName = stSnap.child("screenName").getValue(String::class.java) ?: "Screen",
                                    screenType = stSnap.child("screenType").getValue(String::class.java) ?: "2D",
                                    movieId = stSnap.child("movieId").getValue(String::class.java) ?: "",
                                    movieName = stSnap.child("movieName").getValue(String::class.java) ?: "",
                                    moviePoster = stSnap.child("moviePoster").getValue(String::class.java) ?: "",
                                    date = stDate,
                                    time = stSnap.child("time").getValue(String::class.java) ?: "",
                                    language = stSnap.child("language").getValue(String::class.java) ?: "English",
                                    formats = stSnap.child("formats").getValue(String::class.java) ?: "",
                                    formatPrices = fpMap
                                )
                            )
                        }
                    }
                }
                // Filter out showtimes that have already passed if the date is today
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val filtered = if (date == todayStr) {
                    val now = Calendar.getInstance()
                    val currentHour = now.get(Calendar.HOUR_OF_DAY)
                    val currentMinute = now.get(Calendar.MINUTE)
                    result.filter { st ->
                        try {
                            val timeParts = st.time.uppercase(Locale.ENGLISH)
                            val sdfTime = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
                            val parsed = sdfTime.parse(timeParts)
                            if (parsed != null) {
                                val cal = Calendar.getInstance().apply { time = parsed }
                                val showHour = cal.get(Calendar.HOUR_OF_DAY)
                                val showMinute = cal.get(Calendar.MINUTE)
                                showHour > currentHour || (showHour == currentHour && showMinute > currentMinute)
                            } else true
                        } catch (_: Exception) { true }
                    }
                } else result

                showtimes = filtered.sortedBy { it.time }
                isLoadingShowtimes = false
            }
            override fun onCancelled(error: DatabaseError) { isLoadingShowtimes = false }
        })
    }

    // ── Load seats for a showtime ────────────────────────────────────────────
    fun loadSeats(placeId: String, screenId: String, showtimeId: String) {
        isLoadingSeats = true
        selectedSeats = emptySet()
        val ref = FirebaseDatabase.getInstance()
            .getReference("seats").child(placeId).child(screenId).child(showtimeId)
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val map = mutableMapOf<String, SeatData>()
                snapshot.children.forEach { child ->
                    val seatId = child.key ?: return@forEach
                    map[seatId] = SeatData(
                        seatId = seatId,
                        row = child.child("row").getValue(String::class.java) ?: "",
                        col = child.child("col").getValue(Int::class.java) ?: 0,
                        type = child.child("type").getValue(String::class.java) ?: "silver",
                        price = child.child("price").getValue(Int::class.java) ?: 150,
                        booked = child.child("booked").getValue(Boolean::class.java) ?: false,
                        bookedByUid = child.child("bookedByUid").getValue(String::class.java) ?: ""
                    )
                }
                seats = map
                isLoadingSeats = false
            }
            override fun onCancelled(error: DatabaseError) { isLoadingSeats = false }
        })
    }

    // ── Load food menu ───────────────────────────────────────────────────────
    fun loadFoodMenu() {
        if (foodItems.isNotEmpty()) return
        isLoadingFood = true
        FirebaseDatabase.getInstance().getReference("food_menu")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    foodItems = snapshot.children.mapNotNull { child ->
                        val available = child.child("available").getValue(Boolean::class.java) ?: true
                        if (!available) return@mapNotNull null
                        FoodItem(
                            itemId = child.key ?: "",
                            name = child.child("name").getValue(String::class.java) ?: "",
                            description = child.child("description").getValue(String::class.java) ?: "",
                            price = child.child("price").getValue(Int::class.java) ?: 0,
                            category = child.child("category").getValue(String::class.java) ?: "Snacks",
                            imageUrl = child.child("imageUrl").getValue(String::class.java) ?: "",
                            available = true,
                            ml = child.child("ml").getValue(Int::class.java) ?: 0
                        )
                    }
                    isLoadingFood = false
                }
                override fun onCancelled(error: DatabaseError) { isLoadingFood = false }
            })
    }

    // ── Toggle seat selection ────────────────────────────────────────────────
    fun toggleSeat(seatId: String) {
        val seat = seats[seatId] ?: return
        if (seat.booked) return
        selectedSeats = if (selectedSeats.contains(seatId))
            selectedSeats - seatId
        else
            selectedSeats + seatId
    }

    // ── Update food cart ────────────────────────────────────────────────────
    fun updateFoodQty(itemId: String, qty: Int) {
        if (qty <= 0) foodCart.remove(itemId) else foodCart[itemId] = qty
    }

    // ── Fetch Stripe PaymentIntent clientSecret ──────────────────────────────
    fun fetchPaymentIntent(onSecret: (String) -> Unit, onError: (String) -> Unit) {
        isRequestingPayment = true
        paymentError = null
        pendingPaymentIntentId = null
        val data = hashMapOf("amount" to totalAmount)
        Firebase.functions
            .getHttpsCallable("createPaymentIntent")
            .call(data)
            .addOnSuccessListener { result ->
                val response = result.data as? Map<*, *>
                val secret = response?.get("clientSecret") as? String
                val paymentIntentId = response?.get("paymentIntentId") as? String
                if (secret != null) {
                    pendingPaymentIntentId = paymentIntentId
                    isRequestingPayment = false
                    onSecret(secret)
                } else {
                    paymentError = "Failed to get payment details"
                    isRequestingPayment = false
                    onError("Failed to get payment details")
                }
            }
            .addOnFailureListener { e ->
                paymentError = e.message
                isRequestingPayment = false
                onError(e.message ?: "Payment setup failed")
            }
    }

    fun processWalletPayment(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        isRequestingPayment = true
        paymentError = null
        val data = hashMapOf("amount" to totalAmount)
        Firebase.functions
            .getHttpsCallable("processWalletPayment")
            .call(data)
            .addOnSuccessListener { result ->
                val response = result.data as? Map<*, *>
                val walletTxId = response?.get("walletTxId") as? String
                if (!walletTxId.isNullOrBlank()) {
                    isRequestingPayment = false
                    onSuccess(walletTxId)
                } else {
                    isRequestingPayment = false
                    onError("Wallet payment failed")
                }
            }
            .addOnFailureListener { e ->
                paymentError = e.message
                isRequestingPayment = false
                onError(e.message ?: "Wallet payment failed")
            }
    }

    fun loadWalletBalance() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseDatabase.getInstance().getReference("users").child(uid).child("walletBalance")
            .get()
            .addOnSuccessListener { snap ->
                walletBalance = snap.getValue(Double::class.java) ?: 0.0
            }
            .addOnFailureListener {
                walletBalance = 0.0
            }
    }

    fun notifyPaymentFailure(movieName: String, amount: Int, reason: String) {
        val data = hashMapOf(
            "movieName" to movieName,
            "amount" to amount,
            "reason" to reason
        )
        Firebase.functions
            .getHttpsCallable("notifyPaymentFailure")
            .call(data)
    }

    // ── Send booking confirmation email ──────────────────────────────────────
    private fun sendBookingEmail(booking: Booking) {
        val data = hashMapOf(
            "email" to booking.userEmail,
            "booking" to hashMapOf(
                "bookingId"   to booking.bookingId,
                "movieName"   to booking.movieName,
                "cinemaName"  to booking.cinemaName,
                "screenName"  to booking.screenName,
                "screenType"  to booking.screenType,
                "date"        to booking.date,
                "time"        to booking.time,
                "language"    to booking.language,
                "seats"       to booking.seats,
                "totalAmount" to booking.totalAmount
            )
        )
        Firebase.functions
            .getHttpsCallable("sendBookingEmail")
            .call(data)
    }

    // ── Send booking confirmation via WhatsApp (Cloud Function) ───────────────
    private fun sendBookingWhatsApp(booking: Booking) {
        val uid = booking.userId
        // Fetch user's phone number from the database, then call cloud function
        FirebaseDatabase.getInstance().getReference("users").child(uid)
            .get().addOnSuccessListener { snapshot ->
                val countryCode = snapshot.child("countryCode").getValue(String::class.java) ?: ""
                val phone = snapshot.child("phone").getValue(String::class.java) ?: ""
                val fullPhone = countryCode + phone

                if (fullPhone.isNotEmpty() && fullPhone.startsWith("+")) {
                    val data = hashMapOf(
                        "phone" to fullPhone,
                        "booking" to hashMapOf(
                            "bookingId"   to booking.bookingId,
                            "movieName"   to booking.movieName,
                            "cinemaName"  to booking.cinemaName,
                            "screenName"  to booking.screenName,
                            "screenType"  to booking.screenType,
                            "date"        to booking.date,
                            "time"        to booking.time,
                            "language"    to booking.language,
                            "seats"       to booking.seats,
                            "totalAmount" to booking.totalAmount
                        )
                    )
                    Firebase.functions
                        .getHttpsCallable("sendBookingWhatsApp")
                        .call(data)
                        .addOnSuccessListener {
                            Log.d("BookingVM", "WhatsApp booking confirmation sent")
                        }
                        .addOnFailureListener { e ->
                            Log.e("BookingVM", "WhatsApp send failed: ${e.message}")
                        }
                } else {
                    Log.w("BookingVM", "User has no valid phone number for WhatsApp")
                }
            }
    }

    // ── Schedule 15-min reminder (call from UI with Context) ─────────────────
    fun scheduleShowReminder(context: Context, booking: Booking) {
        val uid = booking.userId
        FirebaseDatabase.getInstance().getReference("users").child(uid)
            .get().addOnSuccessListener { snapshot ->
                val countryCode = snapshot.child("countryCode").getValue(String::class.java) ?: ""
                val phone = snapshot.child("phone").getValue(String::class.java) ?: ""
                val fullPhone = countryCode + phone

                ReminderScheduler.scheduleShowReminder(
                    context = context,
                    movieName = booking.movieName,
                    showDate = booking.date,
                    showTime = booking.time,
                    theaterName = booking.cinemaName,
                    phoneNumber = if (fullPhone.startsWith("+")) fullPhone else "",
                    seats = booking.seats.joinToString(", ")
                )
                Log.d("BookingVM", "Show reminder scheduled for ${booking.movieName}")
            }
    }

    // ── Confirm booking & save to Firebase ──────────────────────────────────
    fun confirmBooking(
        movieId: String,
        movieName: String,
        moviePoster: String,
        paymentMethod: String = "stripe",
        paymentReference: String? = null
    ) {
        val showtime = selectedShowtime ?: return
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
        val userName = FirebaseAuth.getInstance().currentUser?.displayName ?: userEmail
        val paymentIntentId = paymentReference ?: pendingPaymentIntentId

        if (paymentIntentId.isNullOrBlank()) {
            bookingError = "Missing payment confirmation. Please try payment again."
            return
        }

        isCreatingBooking = true
        bookingError = null

        viewModelScope.launch {
            try {
                val bookingId = "BKG-${System.currentTimeMillis()}-${UUID.randomUUID().toString().take(6).uppercase()}"
                val db = FirebaseDatabase.getInstance()

                // 1. Mark selected seats as booked
                val seatsRef = db.getReference("seats")
                    .child(currentPlaceId)
                    .child(showtime.screenId)
                    .child(showtime.showtimeId)

                selectedSeats.forEach { seatId ->
                    seatsRef.child(seatId).child("booked").setValue(true)
                    seatsRef.child(seatId).child("bookedByUid").setValue(uid)
                }

                // 2. Build food items list for booking
                val foodList = cartItems.map { cartItem ->
                    mapOf(
                        "name" to cartItem.item.name,
                        "qty" to cartItem.qty,
                        "price" to cartItem.item.price,
                        "total" to (cartItem.item.price * cartItem.qty)
                    )
                }

                // 3. Build seat types map
                val seatTypesMap = selectedSeats.associateWith { seats[it]?.type ?: "silver" }

                // 4. Build booking object
                val booking = Booking(
                    bookingId = bookingId,
                    userId = uid,
                    userEmail = userEmail,
                    userName = userName,
                    placeId = currentPlaceId,
                    cinemaName = currentCinemaName,
                    cinemaAddress = currentCinemaAddress,
                    screenId = showtime.screenId,
                    screenName = showtime.screenName,
                    screenType = selectedFormat.ifBlank { showtime.screenType },
                    showtimeId = showtime.showtimeId,
                    movieId = movieId,
                    movieName = movieName,
                    moviePoster = moviePoster,
                    date = showtime.date,
                    time = showtime.time,
                    language = selectedLanguage.ifBlank { showtime.language },
                    seats = selectedSeats.toList(),
                    seatTypes = seatTypesMap,
                    seatAmount = seatAmount,
                    foodItems = foodList,
                    foodAmount = foodAmount,
                    ticketGstRate = ticketGstRate,
                    ticketGstAmount = ticketGstAmount,
                    convenienceFeeAmount = convenienceFeeAmount,
                    convenienceFeeGstAmount = convenienceFeeGstAmount,
                    totalAmount = totalAmount,
                    refundableAmount = refundableAmount,
                    nonRefundableAmount = nonRefundableAmount,
                    paymentIntentId = paymentIntentId,
                    paymentMethod = paymentMethod,
                    paymentStatus = "paid",
                    status = "confirmed",
                    refundStatus = "none",
                    bookedAt = System.currentTimeMillis()
                )

                // 5. Save booking to Firebase
                db.getReference("bookings").child(uid).child(bookingId)
                    .setValue(bookingToMap(booking))
                    .addOnSuccessListener {
                        // Also save to flat all_bookings collection for admin/reporting
                        db.getReference("all_bookings").child(bookingId)
                            .setValue(bookingToMap(booking))
                        confirmedBooking = booking
                        isCreatingBooking = false
                        sendBookingEmail(booking)
                        sendBookingWhatsApp(booking)
                        pendingPaymentIntentId = null
                        // Reset selection state
                        selectedSeats = emptySet()
                        selectedFormat = ""
                        selectedLanguage = ""
                        foodCart.clear()
                        selectedShowtime = null
                    }
                    .addOnFailureListener { e ->
                        bookingError = e.message
                        isCreatingBooking = false
                    }
            } catch (e: Exception) {
                bookingError = e.message
                isCreatingBooking = false
            }
        }
    }

    private fun bookingToMap(b: Booking): Map<String, Any?> = mapOf(
        "bookingId" to b.bookingId,
        "userId" to b.userId,
        "userEmail" to b.userEmail,
        "userName" to b.userName,
        "placeId" to b.placeId,
        "cinemaName" to b.cinemaName,
        "cinemaAddress" to b.cinemaAddress,
        "screenId" to b.screenId,
        "screenName" to b.screenName,
        "screenType" to b.screenType,
        "showtimeId" to b.showtimeId,
        "movieId" to b.movieId,
        "movieName" to b.movieName,
        "moviePoster" to b.moviePoster,
        "date" to b.date,
        "time" to b.time,
        "language" to b.language,
        "seats" to b.seats.mapIndexed { i, s -> i.toString() to s }.toMap(),
        "seatTypes" to b.seatTypes,
        "seatAmount" to b.seatAmount,
        "foodItems" to b.foodItems.mapIndexed { i, f -> i.toString() to f }.toMap(),
        "foodAmount" to b.foodAmount,
        "ticketGstRate" to b.ticketGstRate,
        "ticketGstAmount" to b.ticketGstAmount,
        "convenienceFeeAmount" to b.convenienceFeeAmount,
        "convenienceFeeGstAmount" to b.convenienceFeeGstAmount,
        "totalAmount" to b.totalAmount,
        "refundableAmount" to b.refundableAmount,
        "nonRefundableAmount" to b.nonRefundableAmount,
        "paymentIntentId" to b.paymentIntentId,
        "paymentMethod" to b.paymentMethod,
        "paymentStatus" to b.paymentStatus,
        "status" to b.status,
        "refundStatus" to b.refundStatus,
        "refundReason" to b.refundReason,
        "refundId" to b.refundId,
        "refundedAt" to b.refundedAt,
        "bookedAt" to b.bookedAt
    )

    // ── Load my bookings ─────────────────────────────────────────────────────
    fun loadMyBookings() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        isLoadingMyBookings = true
        FirebaseDatabase.getInstance().getReference("bookings").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    myBookings = snapshot.children.mapNotNull { child ->
                        try {
                            val seats = child.child("seats").children
                                .mapNotNull { it.getValue(String::class.java) }
                            val seatAmount = child.child("seatAmount").getValue(Int::class.java) ?: 0
                            val foodAmount = child.child("foodAmount").getValue(Int::class.java) ?: 0
                            val ticketGstAmount = child.child("ticketGstAmount").getValue(Int::class.java) ?: 0
                            val convenienceFeeAmount = child.child("convenienceFeeAmount").getValue(Int::class.java) ?: 0
                            val convenienceFeeGstAmount = child.child("convenienceFeeGstAmount").getValue(Int::class.java) ?: 0
                            val fallbackRefundable = seatAmount + foodAmount
                            val fallbackNonRefundable = ticketGstAmount + convenienceFeeAmount + convenienceFeeGstAmount
                            Booking(
                                bookingId = child.child("bookingId").getValue(String::class.java) ?: "",
                                userId = uid,
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
                                seatAmount = seatAmount,
                                foodAmount = foodAmount,
                                ticketGstRate = child.child("ticketGstRate").getValue(Int::class.java) ?: 18,
                                ticketGstAmount = ticketGstAmount,
                                convenienceFeeAmount = convenienceFeeAmount,
                                convenienceFeeGstAmount = convenienceFeeGstAmount,
                                totalAmount = child.child("totalAmount").getValue(Int::class.java) ?: 0,
                                refundableAmount = child.child("refundableAmount").getValue(Int::class.java) ?: fallbackRefundable,
                                nonRefundableAmount = child.child("nonRefundableAmount").getValue(Int::class.java) ?: fallbackNonRefundable,
                                paymentIntentId = child.child("paymentIntentId").getValue(String::class.java) ?: "",
                                paymentMethod = child.child("paymentMethod").getValue(String::class.java) ?: "stripe",
                                paymentStatus = child.child("paymentStatus").getValue(String::class.java) ?: "paid",
                                status = child.child("status").getValue(String::class.java) ?: "confirmed",
                                refundStatus = child.child("refundStatus").getValue(String::class.java) ?: "none",
                                refundReason = child.child("refundReason").getValue(String::class.java) ?: "",
                                refundId = child.child("refundId").getValue(String::class.java) ?: "",
                                refundedAt = child.child("refundedAt").getValue(Long::class.java) ?: 0L,
                                bookedAt = child.child("bookedAt").getValue(Long::class.java) ?: 0L
                            )
                        } catch (e: Exception) { null }
                    }.sortedByDescending { it.bookedAt }
                    isLoadingMyBookings = false
                }
                override fun onCancelled(error: DatabaseError) { isLoadingMyBookings = false }
            })
    }

    fun requestRefund(bookingId: String, onComplete: (Boolean, String) -> Unit) {
        if (bookingId.isBlank()) {
            onComplete(false, "Invalid booking")
            return
        }
        isRefunding = true
        refundError = null
        refundMessage = null

        val data = hashMapOf("bookingId" to bookingId)
        Firebase.functions
            .getHttpsCallable("requestBookingRefund")
            .call(data)
            .addOnSuccessListener {
                isRefunding = false
                refundMessage = "Refund processed successfully"
                loadMyBookings()
                onComplete(true, refundMessage!!)
            }
            .addOnFailureListener { e ->
                isRefunding = false
                val msg = e.message ?: "Refund request failed"
                refundError = msg
                onComplete(false, msg)
            }
    }

    // ── Admin: check if current user is admin ────────────────────────────────
    fun checkAdminStatus() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            isAdminChecked = true
            return
        }
        FirebaseDatabase.getInstance().getReference("admin_users").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    isAdmin = snapshot.exists()
                    isAdminChecked = true
                }
                override fun onCancelled(error: DatabaseError) {
                    isAdminChecked = true
                }
            })
    }

    // ── Admin: add screen to cinema ──────────────────────────────────────────
    fun addScreen(placeId: String, screenName: String, screenType: String, rows: Int, cols: Int,
                  silverRows: Int, goldRows: Int) {
        val screenId = "screen_${System.currentTimeMillis()}"
        val ref = FirebaseDatabase.getInstance().getReference("cinema_screens")
            .child(placeId).child(screenId)
        ref.setValue(mapOf(
            "name" to screenName,
            "totalRows" to rows,
            "totalCols" to cols,
            "screenType" to screenType
        )).addOnSuccessListener {
            adminActionSuccess = "Screen '$screenName' added successfully!"
            adminActionError = null
        }.addOnFailureListener { adminActionError = it.message }
    }

    // ── Admin: add showtime ──────────────────────────────────────────────────
    fun addShowtime(placeId: String, screenId: String, screenName: String, screenType: String,
                    movieId: String, movieName: String, moviePoster: String,
                    date: String, time: String, language: String,
                    silverRows: Int, goldRows: Int, platinumRows: Int, cols: Int) {
        val showtimeId = "st_${System.currentTimeMillis()}"
        val db = FirebaseDatabase.getInstance()

        // Save showtime
        db.getReference("showtimes").child(placeId).child(screenId).child(showtimeId)
            .setValue(mapOf(
                "screenName" to screenName,
                "screenType" to screenType,
                "movieId" to movieId,
                "movieName" to movieName,
                "moviePoster" to moviePoster,
                "date" to date,
                "time" to time,
                "language" to language
            ))

        // Initialize seats
        val seatsRef = db.getReference("seats").child(placeId).child(screenId).child(showtimeId)
        val rows = ('A'.code until ('A'.code + silverRows + goldRows + platinumRows))
            .map { it.toChar().toString() }

        rows.forEachIndexed { rowIdx, row ->
            val type = when {
                rowIdx < silverRows -> "silver"
                rowIdx < silverRows + goldRows -> "gold"
                else -> "platinum"
            }
            val price = when (type) { "gold" -> 250; "platinum" -> 400; else -> 150 }
            for (col in 1..cols) {
                val seatId = "$row$col"
                seatsRef.child(seatId).setValue(mapOf(
                    "row" to row, "col" to col, "type" to type,
                    "price" to price, "booked" to false, "bookedByUid" to ""
                ))
            }
        }
        adminActionSuccess = "Showtime added for $movieName at $time on $date!"
        adminActionError = null
    }

    // ── Admin: add food item ─────────────────────────────────────────────────
    fun addFoodItem(name: String, description: String, price: Int, category: String, imageUrl: String, ml: Int = 0) {
        val itemId = "food_${System.currentTimeMillis()}"
        val data = mutableMapOf<String, Any>(
            "name" to name,
            "description" to description,
            "price" to price,
            "category" to category,
            "imageUrl" to imageUrl,
            "available" to true
        )
        if (category == "Beverages" && ml > 0) data["ml"] = ml
        FirebaseDatabase.getInstance().getReference("food_menu").child(itemId)
            .setValue(data).addOnSuccessListener {
                foodItems = emptyList() // clear cache so it reloads
                adminActionSuccess = "'$name' added to food menu!"
                adminActionError = null
            }.addOnFailureListener { adminActionError = it.message }
    }

    // ── Admin: upload food image to Firebase Storage ─────────────────────────
    var foodImageUploading by mutableStateOf(false)
        private set
    var foodImageUrl by mutableStateOf("")
        private set

    fun uploadFoodImage(uri: android.net.Uri, context: android.content.Context) {
        foodImageUploading = true
        val fileName = "food_${System.currentTimeMillis()}.jpg"
        val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance()
            .reference.child("food_images/$fileName")
        storageRef.putFile(uri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    foodImageUrl = downloadUrl.toString()
                    foodImageUploading = false
                }.addOnFailureListener {
                    adminActionError = "Failed to get download URL"
                    foodImageUploading = false
                }
            }
            .addOnFailureListener {
                adminActionError = "Image upload failed: ${it.message}"
                foodImageUploading = false
            }
    }

    fun clearFoodImageUrl() { foodImageUrl = "" }

    // ── Load screens for admin ───────────────────────────────────────────────
    var cinemaScreens by mutableStateOf<Map<String, String>>(emptyMap()) // screenId -> name
        private set

    fun loadCinemaScreens(placeId: String) {
        FirebaseDatabase.getInstance().getReference("cinema_screens").child(placeId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    cinemaScreens = snapshot.children.associate { child ->
                        (child.key ?: "") to (child.child("name").getValue(String::class.java) ?: "")
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // ── Apply format-based pricing to loaded seats ─────────────────────────
    fun applyFormatPricing() {
        val showtime = selectedShowtime ?: return
        val priceMap = showtime.formatPrices[selectedFormat] ?: return
        seats = seats.mapValues { (_, seat) ->
            val newPrice = priceMap[seat.type] ?: seat.price
            seat.copy(price = newPrice)
        }
    }

    fun resetBookingError() { bookingError = null }
    fun resetAdminMessages() { adminActionSuccess = null; adminActionError = null }

    fun clearBookingState() {
        selectedShowtime = null
        selectedSeats = emptySet()
        selectedFormat = ""
        selectedLanguage = ""
        foodCart.clear()
        pendingPaymentIntentId = null
        confirmedBooking = null
        bookingError = null
    }

    fun todayDate(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    fun formatDisplayDate(date: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val d = sdf.parse(date)
            SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(d!!)
        } catch (e: Exception) { date }
    }
}
