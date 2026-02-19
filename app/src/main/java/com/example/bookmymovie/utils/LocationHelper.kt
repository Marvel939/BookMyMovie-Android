package com.example.bookmymovie.utils

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Handles location detection, reverse geocoding to city name,
 * and persistent city storage via SharedPreferences.
 *
 * Usage flow:
 * 1. Check permission with [hasLocationPermission]
 * 2. If granted, call [detectCity] to get city from GPS
 * 3. If denied, let user pick manually → call [saveCity]
 * 4. Read last city with [getSavedCity]
 */
class LocationHelper(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "bookmymovie_location"
        private const val KEY_CITY = "selected_city"
        private const val KEY_LATITUDE = "last_latitude"
        private const val KEY_LONGITUDE = "last_longitude"
        private const val KEY_LOCATION_METHOD = "location_method" // "gps" or "manual"

        /** Supported cities (must match Firebase keys exactly) */
        val SUPPORTED_CITIES = listOf(
            "Ahmedabad",
            "Mumbai",
            "Delhi",
            "Bangalore",
            "Chennai",
            "Hyderabad",
            "Kolkata",
            "Pune",
            "Jaipur",
            "Surat"
        )
    }

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ─── PERMISSION CHECK ────────────────────────────────────

    /**
     * Check if location permission is granted.
     */
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    // ─── GPS LOCATION ────────────────────────────────────────

    /**
     * Get the current device location using FusedLocationProviderClient.
     * Requires location permission to be granted.
     *
     * @throws SecurityException if permission not granted
     * @throws Exception if location unavailable
     */
    @Suppress("MissingPermission")
    suspend fun getCurrentLocation(): Location {
        if (!hasLocationPermission()) {
            throw SecurityException("Location permission not granted")
        }

        return suspendCancellableCoroutine { cont ->
            val cancellationToken = CancellationTokenSource()

            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                cancellationToken.token
            ).addOnSuccessListener { location ->
                if (location != null) {
                    cont.resume(location)
                } else {
                    // Fall back to last known location
                    fusedLocationClient.lastLocation
                        .addOnSuccessListener { lastLocation ->
                            if (lastLocation != null) {
                                cont.resume(lastLocation)
                            } else {
                                cont.resumeWithException(
                                    Exception("Unable to determine location")
                                )
                            }
                        }
                        .addOnFailureListener { e ->
                            cont.resumeWithException(e)
                        }
                }
            }.addOnFailureListener { e ->
                cont.resumeWithException(e)
            }

            cont.invokeOnCancellation {
                cancellationToken.cancel()
            }
        }
    }

    // ─── REVERSE GEOCODING ───────────────────────────────────

    /**
     * Convert GPS coordinates to a city name using Android Geocoder.
     * Returns the closest supported city, or the raw city name if not in supported list.
     */
    @Suppress("DEPRECATION")
    fun geocodeToCity(latitude: Double, longitude: Double): String? {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // For API 33+, use synchronous call (still works on background thread)
                geocoder.getFromLocation(latitude, longitude, 5)
            } else {
                geocoder.getFromLocation(latitude, longitude, 5)
            }

            if (addresses.isNullOrEmpty()) return null

            // Try to match with supported cities
            for (address in addresses) {
                val city = address.locality
                    ?: address.subAdminArea
                    ?: address.adminArea
                    ?: continue

                // Exact match
                val matched = SUPPORTED_CITIES.find {
                    it.equals(city, ignoreCase = true)
                }
                if (matched != null) return matched

                // Partial match (e.g., "Mumbai Suburban" -> "Mumbai")
                val partial = SUPPORTED_CITIES.find {
                    city.contains(it, ignoreCase = true) || it.contains(city, ignoreCase = true)
                }
                if (partial != null) return partial
            }

            // Return the raw city name from the first address
            addresses.firstOrNull()?.locality
                ?: addresses.firstOrNull()?.subAdminArea
                ?: addresses.firstOrNull()?.adminArea
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Complete flow: detect GPS location → reverse geocode → return city name.
     * If the detected city is not in supported list, returns null.
     */
    suspend fun detectCity(): String? {
        return try {
            val location = getCurrentLocation()
            prefs.edit()
                .putFloat(KEY_LATITUDE, location.latitude.toFloat())
                .putFloat(KEY_LONGITUDE, location.longitude.toFloat())
                .apply()

            val city = geocodeToCity(location.latitude, location.longitude)
            if (city != null && city in SUPPORTED_CITIES) {
                saveCity(city, method = "gps")
                city
            } else {
                // City detected but not supported
                city
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ─── CITY PERSISTENCE ────────────────────────────────────

    /**
     * Save the selected city to SharedPreferences.
     */
    fun saveCity(city: String, method: String = "manual") {
        prefs.edit()
            .putString(KEY_CITY, city)
            .putString(KEY_LOCATION_METHOD, method)
            .apply()
    }

    /**
     * Get the previously saved city, or null if none saved.
     */
    fun getSavedCity(): String? {
        return prefs.getString(KEY_CITY, null)
    }

    /**
     * Get the method used to determine the city ("gps" or "manual").
     */
    fun getLocationMethod(): String {
        return prefs.getString(KEY_LOCATION_METHOD, "manual") ?: "manual"
    }

    /**
     * Get last known coordinates.
     */
    fun getLastCoordinates(): Pair<Double, Double>? {
        val lat = prefs.getFloat(KEY_LATITUDE, Float.MIN_VALUE)
        val lng = prefs.getFloat(KEY_LONGITUDE, Float.MIN_VALUE)
        return if (lat != Float.MIN_VALUE && lng != Float.MIN_VALUE) {
            Pair(lat.toDouble(), lng.toDouble())
        } else null
    }

    /**
     * Clear saved city data.
     */
    fun clearCity() {
        prefs.edit().clear().apply()
    }

    // ─── DISTANCE CALCULATION ────────────────────────────────

    /**
     * Calculate distance in kilometers between two coordinates using Haversine formula.
     */
    fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val earthRadius = 6371.0 // km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return earthRadius * c
    }

    /**
     * Sort theatres by distance from user's last known location.
     * Returns list of pairs: (Theatre, distanceKm).
     */
    fun sortTheatresByDistance(
        theatres: List<com.example.bookmymovie.model.Theatre>
    ): List<Pair<com.example.bookmymovie.model.Theatre, Double>> {
        val coords = getLastCoordinates() ?: return theatres.map { it to Double.MAX_VALUE }
        val (userLat, userLng) = coords
        return theatres
            .map { theatre ->
                val distance = calculateDistance(
                    userLat, userLng,
                    theatre.latitude, theatre.longitude
                )
                theatre to distance
            }
            .sortedBy { it.second }
    }
}
