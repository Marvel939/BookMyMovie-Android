package com.example.bookmymovie.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Signup : Screen("signup")
    object Home : Screen("home")
    object Notifications : Screen("notifications")
    object Profile : Screen("profile")
    object Search : Screen("search")
    object MovieDetail : Screen("movie_detail/{movieId}") {
        fun createRoute(movieId: String) = "movie_detail/$movieId"
    }
    object PersonDetail : Screen("person_detail/{personId}") {
        fun createRoute(personId: Int) = "person_detail/$personId"
    }
    object MyWishlists : Screen("my_wishlists")
    object MyReviews : Screen("my_reviews")
    object WishlistDetail : Screen("wishlist_detail/{wishlistId}") {
        fun createRoute(wishlistId: String) = "wishlist_detail/$wishlistId"
    }
}
