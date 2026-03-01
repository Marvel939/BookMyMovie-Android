package com.example.bookmymovie.ui.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookmymovie.data.api.RetrofitClient
import com.example.bookmymovie.data.repository.MovieRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

// ─── Data Models ─────────────────────────────────────────────────────────────

data class TheatreOwnerProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val cinemaName: String = "",
    val placeId: String = "",
    val phone: String = "",
    val status: String = "pending", // pending | approved | rejected
    val registeredAt: Long = 0L,
    val approvedAt: Long = 0L,
    val rejectedReason: String = ""
)

data class OwnerScreen(
    val screenId: String = "",
    val screenName: String = "",
    val screenType: String = "2D", // 2D | 3D | 4DX | IMAX
    val totalSeats: Int = 0,
    val silverSeats: Int = 0,
    val goldSeats: Int = 0,
    val platinumSeats: Int = 0,
    val placeId: String = "",
    val ownerUid: String = ""
)

data class ShowtimeRequest(
    val requestId: String = "",
    val placeId: String = "",
    val ownerUid: String = "",
    val cinemaName: String = "",
    val screenId: String = "",
    val screenName: String = "",
    val screenType: String = "",
    val movieId: String = "",
    val movieName: String = "",
    val moviePoster: String = "",
    val movieDurationMinutes: Int = 120,
    val date: String = "",
    val time: String = "",
    val language: String = "Hindi",
    val silverPrice: Double = 150.0,
    val goldPrice: Double = 250.0,
    val platinumPrice: Double = 400.0,
    val status: String = "pending", // pending | approved | rejected
    val submittedAt: Long = 0L,
    val reviewedAt: Long = 0L,
    val rejectedReason: String = ""
)

data class ConflictResult(
    val hasConflict: Boolean = false,
    val isPending: Boolean = false,
    val conflictingMovie: String = "",
    val occupiedFrom: String = "",
    val occupiedUntil: String = "",
    val nextAvailableSlot: String = ""
)

data class TmdbMovieSearchResult(
    val id: Int = 0,
    val title: String = "",
    val posterPath: String = "",
    val releaseDate: String = "",
    val overview: String = "",
    val runtime: Int = 0
)

data class CinemaEntry(
    val name: String = "",
    val placeId: String = "",
    val address: String = ""
)

// ─── ViewModel ─────────────────────────────────────────────────────────────────────────

class TheatreOwnerViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance()

    // Owner profile state
    var ownerProfile by mutableStateOf<TheatreOwnerProfile?>(null)
        private set

    var isLoadingProfile by mutableStateOf(false)
        private set

    var ownerStatus by mutableStateOf<String?>(null) // null = not checked yet
        private set

    // Screens owned by this owner
    var ownerScreens by mutableStateOf<List<OwnerScreen>>(emptyList())
        private set

    // Showtime requests submitted by this owner
    var myShowtimeRequests by mutableStateOf<List<ShowtimeRequest>>(emptyList())
        private set

    // --- Admin side ---
    var pendingOwnerRegistrations by mutableStateOf<List<TheatreOwnerProfile>>(emptyList())
        private set

    var pendingShowtimeRequests by mutableStateOf<List<ShowtimeRequest>>(emptyList())
        private set

    var isLoadingAdminData by mutableStateOf(false)
        private set

    // All screens (admin view)
    var allScreens by mutableStateOf<List<OwnerScreen>>(emptyList())
        private set

    var isLoadingAllScreens by mutableStateOf(false)
        private set

    /** Maps placeId → cinemaName for approved theatre owners (used in admin filter) */
    var cinemaNameMap by mutableStateOf<Map<String, String>>(emptyMap())
        private set

    // Approved showtimes (admin view)
    var approvedShowtimesList by mutableStateOf<List<ShowtimeRequest>>(emptyList())
        private set

    var isLoadingApprovedShowtimes by mutableStateOf(false)
        private set

    // Movie search
    var movieSearchResults by mutableStateOf<List<TmdbMovieSearchResult>>(emptyList())
        private set

    var isSearchingMovies by mutableStateOf(false)
        private set

    // Cinemas list (for registration dropdown)
    var cinemasList by mutableStateOf<List<CinemaEntry>>(emptyList())
        private set

    var isLoadingCinemas by mutableStateOf(false)
        private set

    // Conflict result
    var conflictResult by mutableStateOf(ConflictResult())

    fun clearMovieSearch() {
        movieSearchResults = emptyList()
    }

    fun loadCinemasFromFirebase() {
        if (cinemasList.isNotEmpty()) return
        isLoadingCinemas = true
        db.getReference("cinemas").get()
            .addOnSuccessListener { snap ->
                val list = mutableListOf<CinemaEntry>()
                snap.children.forEach { child ->
                    val cinName = child.child("name").value as? String ?: return@forEach
                    if (cinName.isBlank()) return@forEach
                    list.add(
                        CinemaEntry(
                            name = cinName,
                            placeId = child.child("placeId").value as? String ?: child.key ?: "",
                            address = child.child("address").value as? String ?: ""
                        )
                    )
                }
                cinemasList = list.sortedBy { it.name }
                isLoadingCinemas = false
            }
            .addOnFailureListener { isLoadingCinemas = false }
    }

    // ─── Owner Registration ───────────────────────────────────────────────────

    fun registerTheatreOwner(
        uid: String,
        name: String,
        email: String,
        cinemaName: String,
        placeId: String,
        phone: String,
        city: String = "",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val profile = mapOf(
            "uid" to uid,
            "name" to name,
            "email" to email,
            "cinemaName" to cinemaName,
            "placeId" to placeId,
            "phone" to phone,
            "city" to city,
            "status" to "pending",
            "registeredAt" to System.currentTimeMillis(),
            "approvedAt" to 0L,
            "rejectedReason" to ""
        )
        db.getReference("theatre_owners").child(uid).setValue(profile)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Unknown error") }
    }

    fun checkOwnerStatus(uid: String, onResult: (String) -> Unit) {
        db.getReference("theatre_owners").child(uid).get()
            .addOnSuccessListener { snap ->
                val status = snap.child("status").value as? String ?: "not_found"
                ownerStatus = status
                if (snap.exists()) {
                    ownerProfile = TheatreOwnerProfile(
                        uid = uid,
                        name = snap.child("name").value as? String ?: "",
                        email = snap.child("email").value as? String ?: "",
                        cinemaName = snap.child("cinemaName").value as? String ?: "",
                        placeId = snap.child("placeId").value as? String ?: "",
                        phone = snap.child("phone").value as? String ?: "",
                        status = status,
                        registeredAt = snap.child("registeredAt").value as? Long ?: 0L,
                        approvedAt = snap.child("approvedAt").value as? Long ?: 0L,
                        rejectedReason = snap.child("rejectedReason").value as? String ?: ""
                    )
                }
                onResult(status)
            }
            .addOnFailureListener { onResult("error") }
    }

    fun loadOwnerProfile() {
        val uid = auth.currentUser?.uid ?: return
        isLoadingProfile = true
        db.getReference("theatre_owners").child(uid).get()
            .addOnSuccessListener { snap ->
                isLoadingProfile = false
                if (snap.exists()) {
                    ownerProfile = TheatreOwnerProfile(
                        uid = uid,
                        name = snap.child("name").value as? String ?: "",
                        email = snap.child("email").value as? String ?: "",
                        cinemaName = snap.child("cinemaName").value as? String ?: "",
                        placeId = snap.child("placeId").value as? String ?: "",
                        phone = snap.child("phone").value as? String ?: "",
                        status = snap.child("status").value as? String ?: "pending",
                        registeredAt = snap.child("registeredAt").value as? Long ?: 0L,
                        approvedAt = snap.child("approvedAt").value as? Long ?: 0L,
                        rejectedReason = snap.child("rejectedReason").value as? String ?: ""
                    )
                    ownerStatus = ownerProfile?.status
                    loadOwnerScreens()
                    loadMyShowtimeRequests()
                }
            }
            .addOnFailureListener { isLoadingProfile = false }
    }

    // ─── Owner Screens ────────────────────────────────────────────────────────

    fun loadOwnerScreens() {
        val uid = auth.currentUser?.uid ?: return
        val placeId = ownerProfile?.placeId ?: return
        db.getReference("cinema_screens").child(placeId).get()
            .addOnSuccessListener { snap ->
                val list = mutableListOf<OwnerScreen>()
                snap.children.forEach { child ->
                    if ((child.child("ownerUid").value as? String) == uid) {
                        list.add(
                            OwnerScreen(
                                screenId = child.key ?: "",
                                screenName = child.child("screenName").value as? String ?: "",
                                screenType = child.child("screenType").value as? String ?: "2D",
                                totalSeats = (child.child("totalSeats").value as? Long)?.toInt() ?: 0,
                                silverSeats = (child.child("silverSeats").value as? Long)?.toInt() ?: 0,
                                goldSeats = (child.child("goldSeats").value as? Long)?.toInt() ?: 0,
                                platinumSeats = (child.child("platinumSeats").value as? Long)?.toInt() ?: 0,
                                placeId = placeId,
                                ownerUid = uid
                            )
                        )
                    }
                }
                ownerScreens = list
            }
    }

    fun addScreen(
        screenName: String,
        screenType: String,
        silverSeats: Int,
        goldSeats: Int,
        platinumSeats: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid ?: return
        val placeId = ownerProfile?.placeId ?: run { onError("No cinema linked"); return }
        val screenId = db.getReference("cinema_screens").child(placeId).push().key ?: return
        val data = mapOf(
            "screenId" to screenId,
            "screenName" to screenName,
            "screenType" to screenType,
            "silverSeats" to silverSeats,
            "goldSeats" to goldSeats,
            "platinumSeats" to platinumSeats,
            "totalSeats" to (silverSeats + goldSeats + platinumSeats),
            "placeId" to placeId,
            "ownerUid" to uid
        )
        db.getReference("cinema_screens").child(placeId).child(screenId).setValue(data)
            .addOnSuccessListener {
                loadOwnerScreens()
                onSuccess()
            }
            .addOnFailureListener { onError(it.message ?: "Failed") }
    }

    // ─── Showtime Requests ────────────────────────────────────────────────────

    fun loadMyShowtimeRequests() {
        val uid = auth.currentUser?.uid ?: return
        db.getReference("showtime_requests")
            .orderByChild("ownerUid").equalTo(uid)
            .get()
            .addOnSuccessListener { snap ->
                val list = mutableListOf<ShowtimeRequest>()
                snap.children.forEach { child ->
                    list.add(parseShowtimeRequest(child))
                }
                myShowtimeRequests = list.sortedByDescending { it.submittedAt }
            }
    }

    fun checkConflict(
        placeId: String,
        screenId: String,
        date: String,
        timeStr: String,
        movieDurationMinutes: Int
    ) {
        viewModelScope.launch {
            try {
                val snap = db.getReference("showtime_requests")
                    .orderByChild("placeId").equalTo(placeId)
                    .get().await()

                val newStartMins = parseTimeToMinutes(timeStr)
                val newEndMins = newStartMins + movieDurationMinutes + 30

                var foundConflict = false
                var foundPending = false
                var conflictMovie = ""
                var conflictFrom = ""
                var conflictUntil = ""

                snap.children.forEach { child ->
                    val req = parseShowtimeRequest(child)
                    if (req.screenId != screenId) return@forEach
                    if (req.date != date) return@forEach
                    if (req.status == "rejected") return@forEach

                    val existingStart = parseTimeToMinutes(req.time)
                    val existingEnd = existingStart + req.movieDurationMinutes + 30

                    val overlaps = newStartMins < existingEnd && newEndMins > existingStart
                    if (overlaps) {
                        if (req.status == "pending") foundPending = true
                        else foundConflict = true
                        conflictMovie = req.movieName
                        conflictFrom = req.time
                        conflictUntil = minutesToTimeStr(existingEnd)
                    }
                }

                val nextSlot = if (foundConflict || foundPending) {
                    val snap2 = db.getReference("showtime_requests")
                        .orderByChild("placeId").equalTo(placeId)
                        .get().await()
                    var latestEnd = newStartMins
                    snap2.children.forEach { child ->
                        val req = parseShowtimeRequest(child)
                        if (req.screenId == screenId && req.date == date && req.status != "rejected") {
                            val end = parseTimeToMinutes(req.time) + req.movieDurationMinutes + 30
                            if (end > latestEnd) latestEnd = end
                        }
                    }
                    minutesToTimeStr(latestEnd)
                } else ""

                conflictResult = ConflictResult(
                    hasConflict = foundConflict,
                    isPending = foundPending,
                    conflictingMovie = conflictMovie,
                    occupiedFrom = conflictFrom,
                    occupiedUntil = conflictUntil,
                    nextAvailableSlot = nextSlot
                )
            } catch (_: Exception) {
                conflictResult = ConflictResult()
            }
        }
    }

    fun clearConflict() { conflictResult = ConflictResult() }

    fun submitShowtimeRequest(
        screen: OwnerScreen,
        movieId: String,
        movieName: String,
        moviePoster: String,
        movieDurationMinutes: Int,
        date: String,
        time: String,
        language: String,
        formats: String,
        formatPricesMap: Map<String, Map<String, Int>>,
        silverPrice: Double,
        goldPrice: Double,
        platinumPrice: Double,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid ?: run { onError("Not logged in"); return }
        val profile = ownerProfile ?: run { onError("Profile not loaded"); return }
        val reqId = db.getReference("showtime_requests").push().key ?: run { onError("DB error"); return }

        val data = mutableMapOf<String, Any>(
            "requestId" to reqId,
            "placeId" to profile.placeId,
            "ownerUid" to uid,
            "cinemaName" to profile.cinemaName,
            "screenId" to screen.screenId,
            "screenName" to screen.screenName,
            "screenType" to screen.screenType,
            "movieId" to movieId,
            "movieName" to movieName,
            "moviePoster" to moviePoster,
            "movieDurationMinutes" to movieDurationMinutes,
            "date" to date,
            "time" to time,
            "language" to language,
            "formats" to formats,
            "formatPrices" to formatPricesMap,
            "silverPrice" to silverPrice,
            "goldPrice" to goldPrice,
            "platinumPrice" to platinumPrice,
            "silverSeats" to screen.silverSeats,
            "goldSeats" to screen.goldSeats,
            "platinumSeats" to screen.platinumSeats,
            "status" to "pending",
            "submittedAt" to System.currentTimeMillis(),
            "reviewedAt" to 0L,
            "rejectedReason" to ""
        )
        db.getReference("showtime_requests").child(reqId).setValue(data)
            .addOnSuccessListener {
                loadMyShowtimeRequests()
                onSuccess()
            }
            .addOnFailureListener { onError(it.message ?: "Submit failed") }
    }

    // ─── Admin Functions ──────────────────────────────────────────────────────

    fun loadAllScreens() {
        isLoadingAllScreens = true
        // First, get all approved theatre owners to know which placeIds are valid
        db.getReference("theatre_owners").get()
            .addOnSuccessListener { ownersSnap ->
                val approvedPlaceIds = mutableSetOf<String>()
                val nameMap = mutableMapOf<String, String>()
                ownersSnap.children.forEach { ownerChild ->
                    val status = ownerChild.child("status").value as? String ?: ""
                    val placeId = ownerChild.child("placeId").value as? String ?: ""
                    val cName = ownerChild.child("cinemaName").value as? String ?: ""
                    if (status == "approved" && placeId.isNotBlank()) {
                        approvedPlaceIds.add(placeId)
                        if (cName.isNotBlank()) nameMap[placeId] = cName
                    }
                }
                cinemaNameMap = nameMap
                // Now load screens and filter to only approved theatre owners
                db.getReference("cinema_screens").get()
                    .addOnSuccessListener { snap ->
                        val list = mutableListOf<OwnerScreen>()
                        snap.children.forEach { placeSnap ->
                            val placeKey = placeSnap.key ?: ""
                            // Only include screens whose placeId belongs to an approved owner
                            if (placeKey !in approvedPlaceIds) return@forEach
                            placeSnap.children.forEach { screenSnap ->
                                list.add(
                                    OwnerScreen(
                                        screenId = screenSnap.key ?: "",
                                        screenName = screenSnap.child("screenName").value as? String ?: "",
                                        screenType = screenSnap.child("screenType").value as? String ?: "2D",
                                        totalSeats = (screenSnap.child("totalSeats").value as? Long)?.toInt() ?: 0,
                                        silverSeats = (screenSnap.child("silverSeats").value as? Long)?.toInt() ?: 0,
                                        goldSeats = (screenSnap.child("goldSeats").value as? Long)?.toInt() ?: 0,
                                        platinumSeats = (screenSnap.child("platinumSeats").value as? Long)?.toInt() ?: 0,
                                        placeId = screenSnap.child("placeId").value as? String ?: placeKey,
                                        ownerUid = screenSnap.child("ownerUid").value as? String ?: ""
                                    )
                                )
                            }
                        }
                        allScreens = list
                        isLoadingAllScreens = false
                    }
                    .addOnFailureListener { isLoadingAllScreens = false }
            }
            .addOnFailureListener { isLoadingAllScreens = false }
    }

    fun loadApprovedShowtimes() {
        isLoadingApprovedShowtimes = true
        db.getReference("showtime_requests").orderByChild("status").equalTo("approved").get()
            .addOnSuccessListener { snap ->
                val list = mutableListOf<ShowtimeRequest>()
                snap.children.forEach { child -> list.add(parseShowtimeRequest(child)) }
                approvedShowtimesList = list.sortedByDescending { it.reviewedAt }
                isLoadingApprovedShowtimes = false
            }
            .addOnFailureListener { isLoadingApprovedShowtimes = false }
    }

    fun loadPendingOwnerRegistrations() {
        isLoadingAdminData = true
        db.getReference("theatre_owners").orderByChild("status").equalTo("pending").get()
            .addOnSuccessListener { snap ->
                val list = mutableListOf<TheatreOwnerProfile>()
                snap.children.forEach { child ->
                    list.add(
                        TheatreOwnerProfile(
                            uid = child.key ?: "",
                            name = child.child("name").value as? String ?: "",
                            email = child.child("email").value as? String ?: "",
                            cinemaName = child.child("cinemaName").value as? String ?: "",
                            placeId = child.child("placeId").value as? String ?: "",
                            phone = child.child("phone").value as? String ?: "",
                            status = child.child("status").value as? String ?: "pending",
                            registeredAt = child.child("registeredAt").value as? Long ?: 0L
                        )
                    )
                }
                pendingOwnerRegistrations = list
                isLoadingAdminData = false
            }
            .addOnFailureListener { isLoadingAdminData = false }
    }

    fun approveOwnerRegistration(uid: String, onDone: () -> Unit) {
        db.getReference("theatre_owners").child(uid).updateChildren(
            mapOf("status" to "approved", "approvedAt" to System.currentTimeMillis())
        ).addOnCompleteListener {
            loadPendingOwnerRegistrations()
            onDone()
        }
    }

    fun rejectOwnerRegistration(uid: String, reason: String, onDone: () -> Unit) {
        db.getReference("theatre_owners").child(uid).updateChildren(
            mapOf("status" to "rejected", "rejectedReason" to reason)
        ).addOnCompleteListener {
            loadPendingOwnerRegistrations()
            onDone()
        }
    }

    fun loadPendingShowtimeRequests() {
        isLoadingAdminData = true
        db.getReference("showtime_requests").orderByChild("status").equalTo("pending").get()
            .addOnSuccessListener { snap ->
                val list = mutableListOf<ShowtimeRequest>()
                snap.children.forEach { child -> list.add(parseShowtimeRequest(child)) }
                pendingShowtimeRequests = list.sortedByDescending { it.submittedAt }
                isLoadingAdminData = false
            }
            .addOnFailureListener { isLoadingAdminData = false }
    }

    fun approveShowtimeRequest(requestId: String, onDone: () -> Unit) {
        val requestRef = db.getReference("showtime_requests").child(requestId)
        requestRef.get().addOnSuccessListener { snapshot ->
            val placeId     = snapshot.child("placeId").getValue(String::class.java) ?: ""
            val screenId    = snapshot.child("screenId").getValue(String::class.java) ?: ""
            val screenName  = snapshot.child("screenName").getValue(String::class.java) ?: ""
            val screenType  = snapshot.child("screenType").getValue(String::class.java) ?: "2D"
            val movieId     = snapshot.child("movieId").getValue(String::class.java) ?: ""
            val movieName   = snapshot.child("movieName").getValue(String::class.java) ?: ""
            val moviePoster = snapshot.child("moviePoster").getValue(String::class.java) ?: ""
            val date        = snapshot.child("date").getValue(String::class.java) ?: ""
            val time        = snapshot.child("time").getValue(String::class.java) ?: ""
            val language    = snapshot.child("language").getValue(String::class.java) ?: "Hindi"
            val formats     = snapshot.child("formats").getValue(String::class.java) ?: ""
            val silverPrice = snapshot.child("silverPrice").getValue(Double::class.java) ?: 150.0
            val goldPrice   = snapshot.child("goldPrice").getValue(Double::class.java) ?: 250.0
            val platinumPrice = snapshot.child("platinumPrice").getValue(Double::class.java) ?: 400.0

            // Read formatPrices map from request
            val formatPricesMap = mutableMapOf<String, Map<String, Any>>()
            snapshot.child("formatPrices").children.forEach { fmtSnap ->
                val fmtKey = fmtSnap.key ?: return@forEach
                val priceMap = mutableMapOf<String, Any>()
                fmtSnap.children.forEach { priceSnap ->
                    val typeKey = priceSnap.key ?: return@forEach
                    priceMap[typeKey] = (priceSnap.value as? Long)?.toInt() ?: 0
                }
                formatPricesMap[fmtKey] = priceMap
            }

            // Read screen config to get silver/gold/platinum seat counts
            db.getReference("cinema_screens").child(placeId).child(screenId).get()
                .addOnSuccessListener { screenSnap ->
                    val silverSeats  = (screenSnap.child("silverSeats").value as? Long)?.toInt() ?: 40
                    val goldSeats    = (screenSnap.child("goldSeats").value as? Long)?.toInt() ?: 60
                    val platinumSeats = (screenSnap.child("platinumSeats").value as? Long)?.toInt() ?: 20

                    val showtimeId = "st_${System.currentTimeMillis()}"

                    // Write showtime entry to the user-facing showtimes path
                    val showtimeData = mutableMapOf<String, Any>(
                        "screenName"    to screenName,
                        "screenType"    to screenType,
                        "movieId"       to movieId,
                        "movieName"     to movieName,
                        "moviePoster"   to moviePoster,
                        "date"          to date,
                        "time"          to time,
                        "language"      to language,
                        "formats"       to formats,
                        "silverPrice"   to silverPrice,
                        "goldPrice"     to goldPrice,
                        "platinumPrice" to platinumPrice
                    )
                    if (formatPricesMap.isNotEmpty()) {
                        showtimeData["formatPrices"] = formatPricesMap
                    }
                    db.getReference("showtimes").child(placeId).child(screenId).child(showtimeId)
                        .setValue(showtimeData)

                    // Initialize seats using row-based layout (10 seats per row)
                    val seatsRef = db.getReference("seats").child(placeId).child(screenId).child(showtimeId)
                    val cols = 10
                    val silverRows   = maxOf(1, (silverSeats + cols - 1) / cols)
                    val goldRows     = maxOf(1, (goldSeats + cols - 1) / cols)
                    val platinumRows = maxOf(1, (platinumSeats + cols - 1) / cols)
                    val totalRows    = silverRows + goldRows + platinumRows
                    val rowLetters   = ('A'.code until ('A'.code + totalRows)).map { it.toChar().toString() }

                    rowLetters.forEachIndexed { rowIdx, row ->
                        val type = when {
                            rowIdx < silverRows -> "silver"
                            rowIdx < silverRows + goldRows -> "gold"
                            else -> "platinum"
                        }
                        val price = when (type) {
                            "gold"     -> goldPrice.toInt()
                            "platinum" -> platinumPrice.toInt()
                            else       -> silverPrice.toInt()
                        }
                        for (col in 1..cols) {
                            seatsRef.child("$row$col").setValue(mapOf(
                                "row" to row, "col" to col, "type" to type,
                                "price" to price, "booked" to false, "bookedByUid" to ""
                            ))
                        }
                    }

                    // Mark request as approved
                    requestRef.updateChildren(
                        mapOf("status" to "approved", "reviewedAt" to System.currentTimeMillis())
                    ).addOnCompleteListener {
                        loadPendingShowtimeRequests()
                        onDone()
                    }
                }
                .addOnFailureListener {
                    // Screen config unavailable — still approve the request
                    requestRef.updateChildren(
                        mapOf("status" to "approved", "reviewedAt" to System.currentTimeMillis())
                    ).addOnCompleteListener {
                        loadPendingShowtimeRequests()
                        onDone()
                    }
                }
        }.addOnFailureListener {
            // Fallback: approve without writing showtime
            requestRef.updateChildren(
                mapOf("status" to "approved", "reviewedAt" to System.currentTimeMillis())
            ).addOnCompleteListener {
                loadPendingShowtimeRequests()
                onDone()
            }
        }
    }

    fun rejectShowtimeRequest(requestId: String, reason: String, onDone: () -> Unit) {
        db.getReference("showtime_requests").child(requestId).updateChildren(
            mapOf(
                "status" to "rejected",
                "rejectedReason" to reason,
                "reviewedAt" to System.currentTimeMillis()
            )
        ).addOnCompleteListener {
            loadPendingShowtimeRequests()
            onDone()
        }
    }

    // ─── Movie Search (TMDB) ──────────────────────────────────────────────────

    fun searchMovies(query: String) {
        if (query.length < 2) { movieSearchResults = emptyList(); return }
        isSearchingMovies = true
        viewModelScope.launch {
            try {
                val response = RetrofitClient.api.searchMovies(
                    apiKey = MovieRepository.API_KEY,
                    query = query
                )
                movieSearchResults = response.results.map { r ->
                    TmdbMovieSearchResult(
                        id = r.id,
                        title = r.title,
                        posterPath = if (r.posterPath != null) "https://image.tmdb.org/t/p/w200${r.posterPath}" else "",
                        releaseDate = r.releaseDate ?: "",
                        overview = r.overview ?: ""
                    )
                }
            } catch (_: Exception) {
                movieSearchResults = emptyList()
            } finally {
                isSearchingMovies = false
            }
        }
    }

    fun fetchMovieDetails(movieId: Int, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            try {
                val detail = RetrofitClient.api.getMovieDetails(movieId, MovieRepository.API_KEY)
                onResult(detail.runtime ?: 120)
            } catch (_: Exception) {
                onResult(120)
            }
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun parseShowtimeRequest(child: com.google.firebase.database.DataSnapshot): ShowtimeRequest {
        return ShowtimeRequest(
            requestId = child.child("requestId").value as? String ?: child.key ?: "",
            placeId = child.child("placeId").value as? String ?: "",
            ownerUid = child.child("ownerUid").value as? String ?: "",
            cinemaName = child.child("cinemaName").value as? String ?: "",
            screenId = child.child("screenId").value as? String ?: "",
            screenName = child.child("screenName").value as? String ?: "",
            screenType = child.child("screenType").value as? String ?: "",
            movieId = child.child("movieId").value as? String ?: "",
            movieName = child.child("movieName").value as? String ?: "",
            moviePoster = child.child("moviePoster").value as? String ?: "",
            movieDurationMinutes = (child.child("movieDurationMinutes").value as? Long)?.toInt() ?: 120,
            date = child.child("date").value as? String ?: "",
            time = child.child("time").value as? String ?: "",
            language = child.child("language").value as? String ?: "Hindi",
            silverPrice = (child.child("silverPrice").value as? Number)?.toDouble() ?: 150.0,
            goldPrice = (child.child("goldPrice").value as? Number)?.toDouble() ?: 250.0,
            platinumPrice = (child.child("platinumPrice").value as? Number)?.toDouble() ?: 400.0,
            status = child.child("status").value as? String ?: "pending",
            submittedAt = child.child("submittedAt").value as? Long ?: 0L,
            reviewedAt = child.child("reviewedAt").value as? Long ?: 0L,
            rejectedReason = child.child("rejectedReason").value as? String ?: ""
        )
    }

    /** Parses "10:00 AM" or "22:30" to total minutes since midnight */
    private fun parseTimeToMinutes(timeStr: String): Int {
        return try {
            val sdf = if (timeStr.contains("AM", true) || timeStr.contains("PM", true)) {
                SimpleDateFormat("hh:mm a", Locale.US)
            } else {
                SimpleDateFormat("HH:mm", Locale.US)
            }
            val date = sdf.parse(timeStr) ?: return 0
            val cal = Calendar.getInstance().apply { time = date }
            cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        } catch (_: Exception) { 0 }
    }

    private fun minutesToTimeStr(mins: Int): String {
        val h = (mins / 60) % 24
        val m = mins % 60
        val amPm = if (h < 12) "AM" else "PM"
        val h12 = if (h % 12 == 0) 12 else h % 12
        return "%02d:%02d %s".format(h12, m, amPm)
    }
}


