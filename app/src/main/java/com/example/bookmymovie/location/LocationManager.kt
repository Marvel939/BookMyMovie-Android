package com.example.bookmymovie.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager as AndroidLocationManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

object LocationManager {
    private var fusedLocationClient: FusedLocationProviderClient? = null

    fun initialize(context: Context) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    }

    suspend fun getCurrentLocation(context: Context): Location? = withContext(Dispatchers.Main) {
        if (!hasLocationPermission(context)) {
            return@withContext null
        }

        val fusedClient = fusedLocationClient ?: LocationServices.getFusedLocationProviderClient(context)
        
        return@withContext withTimeoutOrNull(5000L) {
            suspendCancellableCoroutine { continuation ->
                val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
                    .setMaxUpdateAgeMillis(0)  // Get fresh location, not cached
                    .build()

                try {
                    val locationCallback = object : com.google.android.gms.location.LocationCallback() {
                        override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                            super.onLocationResult(result)
                            val location = result.lastLocation
                            try {
                                fusedClient.removeLocationUpdates(this)
                            } catch (e: Exception) {}
                            continuation.resume(location)
                        }
                    }

                    fusedClient.requestLocationUpdates(locationRequest, locationCallback, null)

                    continuation.invokeOnCancellation {
                        try {
                            fusedClient.removeLocationUpdates(locationCallback)
                        } catch (e: Exception) {}
                    }
                } catch (e: SecurityException) {
                    continuation.resume(null)
                }
            }
        }
    }

    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isGpsEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as AndroidLocationManager
        return locationManager.isProviderEnabled(AndroidLocationManager.GPS_PROVIDER)
    }

    fun getRequiredPermissions(): Array<String> {
        return arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }
}
