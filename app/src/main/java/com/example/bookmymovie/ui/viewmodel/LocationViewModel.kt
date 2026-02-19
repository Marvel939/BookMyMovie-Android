package com.example.bookmymovie.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookmymovie.utils.LocationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel managing city selection via GPS or manual input.
 * Persists selection to SharedPreferences and triggers data reload.
 */
class LocationViewModel(application: Application) : AndroidViewModel(application) {

    private val locationHelper = LocationHelper(application)

    /** Currently selected city */
    var selectedCity by mutableStateOf<String?>(null)
        private set

    /** Whether GPS detection is in progress */
    var isDetectingLocation by mutableStateOf(false)
        private set

    /** Error message from location detection */
    var locationError by mutableStateOf<String?>(null)
        private set

    /** Whether location permission is available */
    var hasPermission by mutableStateOf(false)
        private set

    /** Detection method: "gps" or "manual" */
    var locationMethod by mutableStateOf("manual")
        private set

    /** All supported cities for manual selection */
    val supportedCities = LocationHelper.SUPPORTED_CITIES

    /** Callback for when city changes — set by consumers to trigger reload */
    var onCityChanged: ((String) -> Unit)? = null

    init {
        // Load saved city on init
        selectedCity = locationHelper.getSavedCity()
        locationMethod = locationHelper.getLocationMethod()
        hasPermission = locationHelper.hasLocationPermission()
    }

    /**
     * Attempt to detect city from GPS.
     * Falls back to saved city if detection fails.
     */
    fun detectCityFromGPS() {
        viewModelScope.launch {
            isDetectingLocation = true
            locationError = null
            try {
                val city = withContext(Dispatchers.IO) {
                    locationHelper.detectCity()
                }
                if (city != null && city in LocationHelper.SUPPORTED_CITIES) {
                    selectedCity = city
                    locationMethod = "gps"
                    locationHelper.saveCity(city, "gps")
                    onCityChanged?.invoke(city)
                } else if (city != null) {
                    // Detected a city but it's not in our supported list
                    locationError = "\"$city\" is not supported yet. Please select manually."
                } else {
                    locationError = "Could not detect your city. Please select manually."
                }
            } catch (e: SecurityException) {
                locationError = "Location permission required."
            } catch (e: Exception) {
                locationError = "Location detection failed: ${e.message}"
            } finally {
                isDetectingLocation = false
            }
        }
    }

    /**
     * Manually select a city.
     */
    fun selectCity(city: String) {
        selectedCity = city
        locationMethod = "manual"
        locationHelper.saveCity(city, "manual")
        locationError = null
        onCityChanged?.invoke(city)
    }

    /**
     * Update permission state (call after permission result).
     */
    fun updatePermissionState(granted: Boolean) {
        hasPermission = granted
        if (granted && selectedCity == null) {
            detectCityFromGPS()
        }
    }

    /**
     * Get the LocationHelper instance for distance calculations.
     */
    fun getLocationHelper(): LocationHelper = locationHelper
}
