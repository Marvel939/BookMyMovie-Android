package com.example.bookmymovie

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.bookmymovie.firebase.DummyDataSeeder
import com.example.bookmymovie.navigation.NavGraph
import com.example.bookmymovie.ui.theme.BookmyMovieTheme
import com.example.bookmymovie.ui.viewmodel.LocationViewModel
import com.example.bookmymovie.utils.NotificationHelper
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var locationViewModel: LocationViewModel

    // Location permission callback
    private var onLocationPermissionResult: ((Boolean) -> Unit)? = null

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            showWelcomeNotification()
        }
    }

    private val requestLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        val granted = fineGranted || coarseGranted
        locationViewModel.updatePermissionState(granted)
        onLocationPermissionResult?.invoke(granted)
        onLocationPermissionResult = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        locationViewModel = ViewModelProvider(this)[LocationViewModel::class.java]

        // Seed dummy data if not already seeded
        seedDummyDataIfNeeded()

        checkNotificationPermission()

        setContent {
            BookmyMovieTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavGraph(
                        navController = navController,
                        locationViewModel = locationViewModel,
                        onRequestLocationPermission = { requestLocationPermission() }
                    )
                }
            }
        }
    }

    private fun requestLocationPermission() {
        requestLocationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun seedDummyDataIfNeeded() {
        lifecycleScope.launch {
            try {
                if (!DummyDataSeeder.isSeeded()) {
                    val result = DummyDataSeeder.seedAll()
                    if (result.isSuccess) {
                        android.util.Log.d("BookmyMovie", "Dummy data seeded successfully")
                    } else {
                        android.util.Log.e("BookmyMovie", "Failed to seed data: ${result.exceptionOrNull()}")
                    }
                } else {
                    android.util.Log.d("BookmyMovie", "Dummy data already seeded")
                }
            } catch (e: Exception) {
                android.util.Log.e("BookmyMovie", "Error checking/seeding data", e)
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
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
