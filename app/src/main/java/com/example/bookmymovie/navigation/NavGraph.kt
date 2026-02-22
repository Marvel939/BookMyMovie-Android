package com.example.bookmymovie.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.bookmymovie.ui.screens.*

@Composable
fun NavGraph(navController: NavHostController) {
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
            HomeScreen(navController)
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
    }
}
