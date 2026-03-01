package com.example.bookmymovie.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.bookmymovie.data.api.PlaceReview
import com.example.bookmymovie.data.api.PlacesRetrofitClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// IMPORTANT: Replace with your own Google Places API key.
// Steps:
//   1. Go to https://console.cloud.google.com
//   2. Enable "Places API", "Geocoding API" and "Maps Static API"
//   3. Create an API key and paste it below
// ---------------------------------------------------------------------------
const val PLACES_API_KEY = "AIzaSyBiElVGO0SCnOBUfXs_aAlmlKph1MKnuKc"

data class NearbyTheatre(
    val name: String,
    val address: String,
    val rating: Double?,
    val photoUrl: String?,          // first photo (used in bottom sheet card)
    val photoUrls: List<String> = emptyList(), // up to 4 photos for detail screen
    val placeId: String,
    val lat: Double = 0.0,
    val lng: Double = 0.0
)

class NearbyTheatresViewModel : ViewModel() {

    var nearbyTheatres by mutableStateOf<List<NearbyTheatre>>(emptyList())
        private set

    var isLoadingTheatres by mutableStateOf(false)
        private set

    var theatresError by mutableStateOf<String?>(null)
        private set

    /** Prevents re-fetching on every recomposition / navigation back */
    var theatresLoaded by mutableStateOf(false)
        private set

    /** Reviews fetched via Place Details API, keyed by placeId */
    val placeReviewsMap = mutableStateMapOf<String, List<PlaceReview>>()

    /** True while fetching reviews for a place */
    var isLoadingReviews by mutableStateOf(false)
        private set

    /** Last fetched coordinates — used by refresh() */
    private var lastLat: Double = 0.0
    private var lastLng: Double = 0.0

    /** City selected by user from dropdown — used to filter Firebase cinemas */
    var selectedCity by mutableStateOf("")

    init {
        // Do NOT preload Firebase here — loadTheatresFromFirebase() is called
        // only when there is no internet (inside fetchNearbyTheatres).
        // This ensures online users always see only their real-time 10 km radius results.
    }

    /** Load previously saved cinemas from root Firebase cinemas collection (offline fallback) */
    private fun loadTheatresFromFirebase() {
        val ref = FirebaseDatabase.getInstance().getReference("cinemas")
        // keepSynced keeps this node's data available offline
        ref.keepSynced(true)
        // ValueEventListener reads from local cache immediately when offline
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val cached = snapshot.children.mapNotNull { child ->
                        val name = child.child("name").getValue(String::class.java)
                            ?: return@mapNotNull null
                        val photoUrls = child.child("photoUrls").children
                            .mapNotNull { it.getValue(String::class.java) }
                        NearbyTheatre(
                            name = name,
                            address = child.child("address").getValue(String::class.java) ?: "",
                            rating = child.child("rating").getValue(Double::class.java),
                            photoUrl = photoUrls.firstOrNull()
                                ?: child.child("photoUrl").getValue(String::class.java),
                            photoUrls = photoUrls,
                            placeId = child.child("placeId").getValue(String::class.java) ?: "",
                            lat = child.child("lat").getValue(Double::class.java) ?: 0.0,
                            lng = child.child("lng").getValue(Double::class.java) ?: 0.0
                        )
                    }
                    if (cached.isNotEmpty()) {
                        nearbyTheatres = cached
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                // silently ignore — fresh fetch will follow on connectivity
            }
        })
    }

    fun fetchNearbyTheatres(lat: Double, lng: Double, context: Context) {
        lastLat = lat
        lastLng = lng
        // If there is no internet, show only Firebase-cached cinemas and return
        if (!isInternetAvailable(context)) {
            loadTheatresFromFirebase()
            return
        }
        // Internet available — fetch real-time 10 km radius results from Places API
        viewModelScope.launch {
            isLoadingTheatres = true
            theatresError = null
            try {
                val response = PlacesRetrofitClient.api.getNearbyTheatres(
                    location = "$lat,$lng",
                    radius = 10000,        // 10 km
                    type = "movie_theater",
                    apiKey = PLACES_API_KEY
                )
                val fetched = response.results.map { place ->
                    val photoUrls = place.photos?.take(4)?.map { photo ->
                        "https://maps.googleapis.com/maps/api/place/photo" +
                                "?maxwidth=800&photoreference=${photo.photoReference}&key=$PLACES_API_KEY"
                    } ?: emptyList()
                    NearbyTheatre(
                        name = place.name,
                        address = place.vicinity ?: "",
                        rating = place.rating,
                        photoUrl = photoUrls.firstOrNull(),
                        photoUrls = photoUrls,
                        placeId = place.placeId,
                        lat = place.geometry?.location?.lat ?: 0.0,
                        lng = place.geometry?.location?.lng ?: 0.0
                    )
                }
                nearbyTheatres = fetched
                theatresLoaded = true
                // Save to Firebase (adds new, updates existing)
                saveCinemasToFirebase(fetched)
            } catch (e: Exception) {
                // Show cached data if available, otherwise show error
                if (nearbyTheatres.isEmpty()) {
                    theatresError = "Could not load nearby cinemas. Check your internet connection."
                }
                e.printStackTrace()
            } finally {
                isLoadingTheatres = false
            }
        }
    }

    /** Re-fetches theatres using the last known coordinates.
     *  If location was never obtained, falls back to city-filtered Firebase cinemas. */
    fun refresh(context: Context) {
        if (lastLat != 0.0 || lastLng != 0.0) {
            fetchNearbyTheatres(lastLat, lastLng, context)
        } else if (selectedCity.isNotBlank()) {
            filterTheatresByCity(selectedCity)
        } else {
            // Location never granted, no city selected — load all Firebase cinemas
            loadTheatresFromFirebase()
        }
    }

    /** Filters Firebase cinemas by city name (matches against address field).
     *  Also includes approved theatre-owner registrations whose city matches. */
    fun filterTheatresByCity(city: String) {
        selectedCity = city
        isLoadingTheatres = true
        theatresError = null
        nearbyTheatres = emptyList()

        val db = FirebaseDatabase.getInstance()
        val cinemasRef = db.getReference("cinemas")
        val ownersRef = db.getReference("theatre_owners")

        // Track completion of both queries
        var cinemasResult: List<NearbyTheatre>? = null
        var ownersResult: List<NearbyTheatre>? = null

        fun mergeResults() {
            // Wait until both queries complete
            val c = cinemasResult ?: return
            val o = ownersResult ?: return

            // Merge, deduplicate by placeId (cinema entries take priority)
            val existingPlaceIds = c.map { it.placeId }.toSet()
            val merged = c + o.filter { it.placeId.isBlank() || it.placeId !in existingPlaceIds }

            isLoadingTheatres = false
            nearbyTheatres = merged
            if (merged.isEmpty()) {
                theatresError = "No cinemas found in $city"
            }
        }

        // 1. Fetch from cinemas/ collection (Google Places cache)
        cinemasRef.keepSynced(true)
        cinemasRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val all = if (snapshot.exists()) {
                    snapshot.children.mapNotNull { child ->
                        val name = child.child("name").getValue(String::class.java)
                            ?: return@mapNotNull null
                        val photoUrls = child.child("photoUrls").children
                            .mapNotNull { it.getValue(String::class.java) }
                        NearbyTheatre(
                            name = name,
                            address = child.child("address").getValue(String::class.java) ?: "",
                            rating = child.child("rating").getValue(Double::class.java),
                            photoUrl = photoUrls.firstOrNull()
                                ?: child.child("photoUrl").getValue(String::class.java),
                            photoUrls = photoUrls,
                            placeId = child.child("placeId").getValue(String::class.java) ?: "",
                            lat = child.child("lat").getValue(Double::class.java) ?: 0.0,
                            lng = child.child("lng").getValue(Double::class.java) ?: 0.0
                        )
                    }
                } else emptyList()
                cinemasResult = all.filter { it.address.contains(city, ignoreCase = true) }
                mergeResults()
            }
            override fun onCancelled(error: DatabaseError) {
                cinemasResult = emptyList()
                mergeResults()
            }
        })

        // 2. Fetch approved theatre owners registered in this city
        ownersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val registered = if (snapshot.exists()) {
                    snapshot.children.mapNotNull { child ->
                        val status = child.child("status").getValue(String::class.java) ?: ""
                        if (status != "approved") return@mapNotNull null
                        val ownerCity = child.child("city").getValue(String::class.java) ?: ""
                        if (!ownerCity.equals(city, ignoreCase = true)) return@mapNotNull null
                        val cinemaName = child.child("cinemaName").getValue(String::class.java)
                            ?: return@mapNotNull null
                        NearbyTheatre(
                            name = cinemaName,
                            address = ownerCity,
                            rating = null,
                            photoUrl = null,
                            photoUrls = emptyList(),
                            placeId = child.child("placeId").getValue(String::class.java) ?: "",
                            lat = 0.0,
                            lng = 0.0
                        )
                    }
                } else emptyList()
                ownersResult = registered
                mergeResults()
            }
            override fun onCancelled(error: DatabaseError) {
                ownersResult = emptyList()
                mergeResults()
            }
        })
    }

    /** Returns true if the device has an active internet connection */
    private fun isInternetAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /** Saves fetched cinemas to root Firebase cinemas/{placeId} collection */
    private fun saveCinemasToFirebase(theatres: List<NearbyTheatre>) {
        val ref = FirebaseDatabase.getInstance().getReference("cinemas")
        theatres.forEach { theatre ->
            val safeKey = theatre.placeId.replace(Regex("[.#$\\[\\]/]"), "_")
            // Build indexed map for photoUrls so Firebase stores them reliably
            val photoUrlsMap = theatre.photoUrls
                .mapIndexed { i, url -> i.toString() to url }
                .toMap()
            ref.child(safeKey).setValue(
                mapOf(
                    "name" to theatre.name,
                    "address" to theatre.address,
                    "rating" to theatre.rating,
                    "photoUrl" to theatre.photoUrl,
                    "photoUrls" to photoUrlsMap,
                    "placeId" to theatre.placeId,
                    "lat" to theatre.lat,
                    "lng" to theatre.lng
                )
            ).addOnFailureListener { e ->
                e.printStackTrace()
            }
        }
    }

    /** Fetches Google user reviews for a cinema via Place Details API */
    fun fetchPlaceDetails(placeId: String) {
        if (placeReviewsMap.containsKey(placeId)) return   // already fetched
        viewModelScope.launch {
            isLoadingReviews = true
            try {
                val response = PlacesRetrofitClient.api.getPlaceDetails(
                    placeId = placeId,
                    fields = "reviews",
                    apiKey = PLACES_API_KEY
                )
                placeReviewsMap[placeId] = response.result?.reviews ?: emptyList()
            } catch (e: Exception) {
                placeReviewsMap[placeId] = emptyList()
                e.printStackTrace()
            } finally {
                isLoadingReviews = false
            }
        }
    }

    /** Returns a theatre by placeId for the detail screen */
    fun getTheatreByPlaceId(placeId: String): NearbyTheatre? =
        nearbyTheatres.firstOrNull { it.placeId == placeId }

    /** Saves auto-detected city + coordinates to Firebase users/{uid} */
    fun saveLocationToFirebase(city: String, lat: Double, lng: Double) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ref = FirebaseDatabase.getInstance().getReference("users").child(uid)
        ref.child("city").setValue(city)
        ref.child("lat").setValue(lat)
        ref.child("lng").setValue(lng)
    }
}

