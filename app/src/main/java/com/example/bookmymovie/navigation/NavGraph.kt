package com.example.bookmymovie.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.bookmymovie.ui.screens.*
import com.example.bookmymovie.ui.viewmodel.CityMovieViewModel
import com.example.bookmymovie.ui.viewmodel.LocationViewModel
import com.example.bookmymovie.ui.viewmodel.TheatreViewModel

@Composable
fun NavGraph(
    navController: NavHostController,
    locationViewModel: LocationViewModel = viewModel(),
    cityMovieViewModel: CityMovieViewModel = viewModel(),
    theatreViewModel: TheatreViewModel = viewModel(),
    onRequestLocationPermission: () -> Unit = {}
) {
    // Wire city change to data reload
    LaunchedEffect(Unit) {
        locationViewModel.onCityChanged = { city ->
            cityMovieViewModel.loadDataForCity(city)
        }
        // Load initial data if city already selected
        locationViewModel.selectedCity?.let { city ->
            cityMovieViewModel.loadDataForCity(city)
        }
    }

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
            HomeScreen(
                navController = navController,
                locationViewModel = locationViewModel,
                cityMovieViewModel = cityMovieViewModel
            )
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
        composable(Screen.CitySelection.route) {
            CitySelectionScreen(
                locationViewModel = locationViewModel,
                onCitySelected = { city ->
                    cityMovieViewModel.loadDataForCity(city)
                    navController.popBackStack()
                },
                onRequestPermission = onRequestLocationPermission
            )
        }
        composable(
            route = Screen.TheatreList.route,
            arguments = listOf(navArgument("city") { type = NavType.StringType })
        ) { backStackEntry ->
            val city = backStackEntry.arguments?.getString("city") ?: ""
            TheatreListScreen(
                city = city,
                theatresWithDistance = cityMovieViewModel.nearbyTheatres,
                theatreViewModel = theatreViewModel,
                onTheatreClick = { theatreId ->
                    navController.navigate(Screen.TheatreDetail.createRoute(city, theatreId))
                },
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.TheatreDetail.route,
            arguments = listOf(
                navArgument("city") { type = NavType.StringType },
                navArgument("theatreId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val city = backStackEntry.arguments?.getString("city") ?: ""
            val theatreId = backStackEntry.arguments?.getString("theatreId") ?: ""
            val theatre = cityMovieViewModel.cityTheatres.find { it.theatreId == theatreId }
            if (theatre != null) {
                TheatreDetailScreen(
                    theatre = theatre,
                    city = city,
                    theatreViewModel = theatreViewModel,
                    onMovieClick = { movieId ->
                        navController.navigate(Screen.MovieDetail.createRoute(movieId))
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}
}
