package com.example.bookmymovie.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.bookmymovie.MainActivity
import com.example.bookmymovie.ui.screens.*
import com.example.bookmymovie.ui.viewmodel.BookingViewModel
import com.example.bookmymovie.ui.viewmodel.StreamingViewModel

@Composable
fun NavGraph(navController: NavHostController) {
    val context = LocalContext.current
    val bookingViewModel: BookingViewModel = viewModel(context as MainActivity)
    val streamingViewModel: StreamingViewModel = viewModel(context as MainActivity)

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(navController)
        }
        composable(Screen.Login.route) {
            LoginScreen(navController)
        }
        composable(Screen.Signup.route) {
            SignupScreen(navController)
        }
        composable(Screen.Home.route) {
            HomeScreen(navController, streamingViewModel = streamingViewModel)
        }
        composable(Screen.Notifications.route) {
            NotificationsScreen(navController)
        }
        composable(Screen.Profile.route) {
            ProfileScreen(navController)
        }
        composable(Screen.Search.route) {
            SearchScreen(navController)
        }
        composable(
            route = Screen.MovieDetail.route,
            arguments = listOf(navArgument("movieId") { type = NavType.StringType })
        ) { backStackEntry ->
            val movieId = backStackEntry.arguments?.getString("movieId")
            MovieDetailScreen(navController, movieId)
        }
        composable(
            route = Screen.PersonDetail.route,
            arguments = listOf(navArgument("personId") { type = NavType.StringType })
        ) { backStackEntry ->
            val personId = backStackEntry.arguments?.getString("personId")
            PersonDetailScreen(navController, personId)
        }
        composable(Screen.MyWishlists.route) {
            MyWishlistsScreen(navController)
        }
        composable(Screen.MyReviews.route) {
            MyReviewsScreen(navController)
        }
        composable(
            route = Screen.WishlistDetail.route,
            arguments = listOf(navArgument("wishlistId") { type = NavType.StringType })
        ) { backStackEntry ->
            val wishlistId = backStackEntry.arguments?.getString("wishlistId")
            WishlistScreen(navController, wishlistId)
        }
        composable(
            route = Screen.CinemaDetail.route,
            arguments = listOf(navArgument("placeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val placeId = backStackEntry.arguments?.getString("placeId")
            CinemaDetailScreen(navController, placeId)
        }
        // ── Booking Flow ──────────────────────────────────────────────────────
        composable(
            route = Screen.ShowtimeSelection.route,
            arguments = listOf(navArgument("placeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val placeId = backStackEntry.arguments?.getString("placeId")
            ShowtimeSelectionScreen(navController, placeId, bookingViewModel)
        }
        composable(Screen.SeatSelection.route) {
            SeatSelectionScreen(navController, bookingViewModel)
        }
        composable(Screen.FoodBeverage.route) {
            FoodBeverageScreen(navController, bookingViewModel)
        }
        composable(Screen.BookingSummary.route) {
            BookingSummaryScreen(navController, bookingViewModel)
        }
        composable(Screen.BookingConfirmation.route) {
            BookingConfirmationScreen(navController, bookingViewModel)
        }
        composable(Screen.MyBookings.route) {
            MyBookingsScreen(navController, bookingViewModel)
        }
        composable(Screen.AdminPanel.route) {
            AdminPanelScreen(navController, bookingViewModel)
        }
        composable(Screen.AdminAuth.route) {
            AdminAuthScreen(navController)
        }
        composable(Screen.TheatreOwnerAuth.route) {
            TheatreOwnerAuthScreen(navController)
        }
        composable(Screen.TheatreOwnerPanel.route) {
            TheatreOwnerPanelScreen(navController)
        }
        composable(Screen.OwnerSchedule.route) {
            OwnerScheduleScreen(navController)
        }
        composable(Screen.AdminRequests.route) {
            AdminRequestsScreen(navController)
        }
        composable(Screen.PhoneAuth.route) {
            PhoneAuthScreen(navController)
        }
        composable(Screen.EditProfile.route) {
            EditProfileScreen(navController)
        }

        // ── Streaming Flow ────────────────────────────────────────────────────
        composable(Screen.StreamBrowse.route) {
            StreamBrowseScreen(navController, streamingViewModel)
        }
        composable(
            route = Screen.StreamDetail.route,
            arguments = listOf(navArgument("movieId") { type = NavType.StringType })
        ) { backStackEntry ->
            val movieId = backStackEntry.arguments?.getString("movieId") ?: ""
            StreamDetailScreen(navController, streamingViewModel, movieId)
        }
        composable(Screen.MyLibrary.route) {
            MyLibraryScreen(navController, streamingViewModel)
        }
        composable(
            route = Screen.StreamPlayer.route,
            arguments = listOf(navArgument("movieId") { type = NavType.StringType })
        ) { backStackEntry ->
            val movieId = backStackEntry.arguments?.getString("movieId") ?: ""
            StreamPlayerScreen(navController, streamingViewModel, movieId)
        }
        composable(Screen.AdminStreamingCatalog.route) {
            AdminStreamingCatalogScreen(navController)
        }
        composable(Screen.AdminAddStreamingMovie.route) {
            AdminAddStreamingMovieScreen(navController)
        }
    }
}

