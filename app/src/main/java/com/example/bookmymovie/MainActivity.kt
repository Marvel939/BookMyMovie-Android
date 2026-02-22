package com.example.bookmymovie

import android.Manifest
import android.app.Activity
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.example.bookmymovie.navigation.NavGraph
import com.example.bookmymovie.ui.theme.BookmyMovieTheme
import com.example.bookmymovie.ui.viewmodel.NearbyTheatresViewModel
import com.example.bookmymovie.utils.NotificationHelper
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.Locale

class MainActivity : ComponentActivity() {

    val nearbyTheatresViewModel: NearbyTheatresViewModel by viewModels()

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) showWelcomeNotification()
    }

    private val requestLocationPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) checkLocationServicesAndDetect()
    }

    // Handles the "Enable location services?" system dialog result
    private val locationSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            detectLocationAndFetchTheatres()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkNotificationPermission()
        checkLocationPermission()
        setContent {
            BookmyMovieTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavGraph(navController = navController)
                }
            }
        }
    }

    private fun checkLocationPermission() {
        val fineGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (fineGranted || coarseGranted) {
            checkLocationServicesAndDetect()
        } else {
            requestLocationPermission.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    /**
     * Checks if the device's location services (GPS) are enabled.
     * If off, shows the system dialog asking the user to turn them on.
     * If already on, proceeds directly to location detection.
     */
    private fun checkLocationServicesAndDetect() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 5000L
        ).build()
        val settingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true)
            .build()
        val settingsClient = LocationServices.getSettingsClient(this)
        settingsClient.checkLocationSettings(settingsRequest)
            .addOnSuccessListener {
                // GPS is already on — detect immediately
                detectLocationAndFetchTheatres()
            }
            .addOnFailureListener { exception ->
                if (exception is ResolvableApiException) {
                    // Shows the "Turn on location?" system dialog
                    try {
                        locationSettingsLauncher.launch(
                            IntentSenderRequest.Builder(exception.resolution).build()
                        )
                    } catch (e: IntentSender.SendIntentException) {
                        e.printStackTrace()
                    }
                }
            }
    }

    private fun detectLocationAndFetchTheatres() {
        val fusedClient = LocationServices.getFusedLocationProviderClient(this)
        try {
            // First try getLastLocation (instant, works if GPS was recently used)
            fusedClient.lastLocation.addOnSuccessListener { lastLocation ->
                if (lastLocation != null) {
                    processLocation(lastLocation.latitude, lastLocation.longitude)
                } else {
                    // Fall back to fresh getCurrentLocation
                    fusedClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        CancellationTokenSource().token
                    ).addOnSuccessListener { location ->
                        location ?: return@addOnSuccessListener
                        processLocation(location.latitude, location.longitude)
                    }
                }
            }.addOnFailureListener {
                // If lastLocation fails, try getCurrentLocation
                fusedClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    CancellationTokenSource().token
                ).addOnSuccessListener { location ->
                    location ?: return@addOnSuccessListener
                    processLocation(location.latitude, location.longitude)
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun processLocation(lat: Double, lng: Double) {
        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(lat, lng, 1) { addresses ->
                    val city = addresses.firstOrNull()?.locality
                        ?: addresses.firstOrNull()?.subAdminArea
                        ?: "Unknown"
                    nearbyTheatresViewModel.saveLocationToFirebase(city, lat, lng)
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                val city = addresses?.firstOrNull()?.locality
                    ?: addresses?.firstOrNull()?.subAdminArea
                    ?: "Unknown"
                nearbyTheatresViewModel.saveLocationToFirebase(city, lat, lng)
            }
        } catch (e: Exception) {
            nearbyTheatresViewModel.saveLocationToFirebase("Unknown", lat, lng)
        }
        nearbyTheatresViewModel.fetchNearbyTheatres(lat, lng, this)
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                showWelcomeNotification()
            }
        } else {
            showWelcomeNotification()
        }
    }

    private fun showWelcomeNotification() {
        val notificationHelper = NotificationHelper(this)
        notificationHelper.showNotification(
            "Welcome to BookmyMovie!",
            "Explore the latest movies and book your tickets now."
        )
    }
}
