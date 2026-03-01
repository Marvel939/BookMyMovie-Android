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
import java.text.SimpleDateFormat
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

    // ── My bookings ─────────────────────────────────────────────────────────
    var myBookings by mutableStateOf<List<Booking>>(emptyList())
        private set
    var isLoadingMyBookings by mutableStateOf(false)
        private set

    // ── Admin state ─────────────────────────────────────────────────────────
    var isAdmin by mutableStateOf(false)
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

    val totalAmount: Int get() = seatAmount + foodAmount

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
                showtimes = result.sortedBy { it.time }
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
                            available = true
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
        val data = hashMapOf("amount" to totalAmount)
        Firebase.functions
            .getHttpsCallable("createPaymentIntent")
            .call(data)
            .addOnSuccessListener { result ->
                val secret = (result.data as? Map<*, *>)?.get("clientSecret") as? String
                if (secret != null) {
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
    fun confirmBooking(movieId: String, movieName: String, moviePoster: String) {
        val showtime = selectedShowtime ?: return
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
        val userName = FirebaseAuth.getInstance().currentUser?.displayName ?: userEmail

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
                    totalAmount = totalAmount,
                    paymentStatus = "confirmed",
                    status = "confirmed",
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
        "totalAmount" to b.totalAmount,
        "paymentStatus" to b.paymentStatus,
        "status" to b.status,
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
                                seatAmount = child.child("seatAmount").getValue(Int::class.java) ?: 0,
                                foodAmount = child.child("foodAmount").getValue(Int::class.java) ?: 0,
                                totalAmount = child.child("totalAmount").getValue(Int::class.java) ?: 0,
                                status = child.child("status").getValue(String::class.java) ?: "confirmed",
                                bookedAt = child.child("bookedAt").getValue(Long::class.java) ?: 0L
                            )
                        } catch (e: Exception) { null }
                    }.sortedByDescending { it.bookedAt }
                    isLoadingMyBookings = false
                }
                override fun onCancelled(error: DatabaseError) { isLoadingMyBookings = false }
            })
    }

    // ── Admin: check if current user is admin ────────────────────────────────
    fun checkAdminStatus() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseDatabase.getInstance().getReference("admin_users").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    isAdmin = snapshot.exists()
                }
                override fun onCancelled(error: DatabaseError) {}
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
    fun addFoodItem(name: String, description: String, price: Int, category: String, imageUrl: String) {
        val itemId = "food_${System.currentTimeMillis()}"
        FirebaseDatabase.getInstance().getReference("food_menu").child(itemId)
            .setValue(mapOf(
                "name" to name,
                "description" to description,
                "price" to price,
                "category" to category,
                "imageUrl" to imageUrl,
                "available" to true
            )).addOnSuccessListener {
                foodItems = emptyList() // clear cache so it reloads
                adminActionSuccess = "'$name' added to food menu!"
                adminActionError = null
            }.addOnFailureListener { adminActionError = it.message }
    }

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
