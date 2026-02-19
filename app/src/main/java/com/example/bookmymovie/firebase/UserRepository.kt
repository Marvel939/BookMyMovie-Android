package com.example.bookmymovie.firebase

import com.example.bookmymovie.model.Booking
import com.example.bookmymovie.model.UserPreference
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Repository for managing user-specific data: bookings, preferences, and genre history.
 * Maps to: users/{userId}/bookings, users/{userId}/preferences
 */
object UserRepository {

    private val database = FirebaseDatabase.getInstance()
    private val usersRef = database.getReference("users")
    private val auth = FirebaseAuth.getInstance()

    private fun currentUserId(): String? = auth.currentUser?.uid

    // ─── BOOKINGS ────────────────────────────────────────────

    /**
     * Save a new booking and update genre preferences automatically.
     */
    suspend fun saveBooking(booking: Booking): Result<String> {
        val userId = currentUserId() ?: return Result.failure(Exception("User not logged in"))
        return try {
            val bookingRef = usersRef.child(userId).child("bookings").push()
            val bookingId = bookingRef.key ?: return Result.failure(Exception("Failed to generate booking key"))
            val bookingWithId = booking.copy(
                bookingId = bookingId,
                bookedAt = System.currentTimeMillis()
            )
            suspendCancellableCoroutine { cont ->
                bookingRef.setValue(bookingWithId)
                    .addOnSuccessListener { cont.resume(Unit) }
                    .addOnFailureListener { cont.resumeWithException(it) }
            }
            // Update genre preferences based on this booking
            updateGenrePreferences(booking.genre)
            Result.success(bookingId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch all bookings for current user.
     */
    suspend fun getBookings(): List<Booking> {
        val userId = currentUserId() ?: return emptyList()
        return suspendCancellableCoroutine { cont ->
            usersRef.child(userId).child("bookings")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val bookings = snapshot.children.mapNotNull { child ->
                            child.getValue(Booking::class.java)
                        }
                        cont.resume(bookings)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        cont.resumeWithException(error.toException())
                    }
                })
        }
    }

    /**
     * Observe bookings in real-time.
     */
    fun observeBookings(): Flow<List<Booking>> = callbackFlow {
        val userId = currentUserId()
        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val ref = usersRef.child(userId).child("bookings")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val bookings = snapshot.children.mapNotNull { child ->
                    child.getValue(Booking::class.java)
                }
                trySend(bookings)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // ─── PREFERENCES ─────────────────────────────────────────

    /**
     * Fetch user preferences.
     */
    suspend fun getUserPreferences(): UserPreference {
        val userId = currentUserId() ?: return UserPreference()
        return suspendCancellableCoroutine { cont ->
            usersRef.child(userId).child("preferences")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val prefs = snapshot.getValue(UserPreference::class.java)
                            ?: UserPreference()
                        cont.resume(prefs)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        cont.resumeWithException(error.toException())
                    }
                })
        }
    }

    /**
     * Observe user preferences as a real-time Flow.
     */
    fun observeUserPreferences(): Flow<UserPreference> = callbackFlow {
        val userId = currentUserId()
        if (userId == null) {
            trySend(UserPreference())
            close()
            return@callbackFlow
        }
        val ref = usersRef.child(userId).child("preferences")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val prefs = snapshot.getValue(UserPreference::class.java) ?: UserPreference()
                trySend(prefs)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /**
     * Save the user's last selected city.
     */
    suspend fun saveLastCity(city: String) {
        val userId = currentUserId() ?: return
        suspendCancellableCoroutine { cont ->
            usersRef.child(userId).child("preferences").child("lastCity")
                .setValue(city)
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
    }

    /**
     * Increment genre watch counts when a booking is made.
     */
    private suspend fun updateGenrePreferences(genres: List<String>) {
        if (genres.isEmpty()) return
        val userId = currentUserId() ?: return
        val currentPrefs = getUserPreferences()
        val updatedGenres = currentPrefs.preferredGenres.toMutableMap()
        for (genre in genres) {
            updatedGenres[genre] = (updatedGenres[genre] ?: 0) + 1
        }
        suspendCancellableCoroutine { cont ->
            usersRef.child(userId).child("preferences").child("preferredGenres")
                .setValue(updatedGenres)
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
    }

    /**
     * Add a theatre to favorites.
     */
    suspend fun addFavoriteTheatre(theatreId: String) {
        val userId = currentUserId() ?: return
        val currentPrefs = getUserPreferences()
        if (theatreId in currentPrefs.favoriteTheatres) return
        val updated = currentPrefs.favoriteTheatres + theatreId
        suspendCancellableCoroutine { cont ->
            usersRef.child(userId).child("preferences").child("favoriteTheatres")
                .setValue(updated)
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
    }

    /**
     * Remove a theatre from favorites.
     */
    suspend fun removeFavoriteTheatre(theatreId: String) {
        val userId = currentUserId() ?: return
        val currentPrefs = getUserPreferences()
        val updated = currentPrefs.favoriteTheatres - theatreId
        suspendCancellableCoroutine { cont ->
            usersRef.child(userId).child("preferences").child("favoriteTheatres")
                .setValue(updated)
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
    }
}
