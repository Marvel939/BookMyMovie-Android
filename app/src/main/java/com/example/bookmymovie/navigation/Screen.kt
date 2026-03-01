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
    object CitySelection : Screen("city_selection")
    object TheatreList : Screen("theatre_list/{city}") {
        fun createRoute(city: String) = "theatre_list/$city"
    }
    object TheatreDetail : Screen("theatre_detail/{city}/{theatreId}") {
        fun createRoute(city: String, theatreId: String) = "theatre_detail/$city/$theatreId"
    }
    object CinemaDetail : Screen("cinema_detail/{placeId}") {
        fun createRoute(placeId: String) = "cinema_detail/$placeId"
    }
    object ShowtimeSelection : Screen("showtime_selection/{placeId}") {
        fun createRoute(placeId: String) = "showtime_selection/$placeId"
    }
    object FormatLanguage : Screen("format_language")
    object SeatSelection : Screen("seat_selection")
    object FoodBeverage : Screen("food_beverage")
    object BookingSummary : Screen("booking_summary")
    object BookingConfirmation : Screen("booking_confirmation")
    object MyBookings : Screen("my_bookings")
    object AdminPanel : Screen("admin_panel")
    object AdminAuth : Screen("admin_auth")
    object TheatreOwnerAuth : Screen("theatre_owner_auth")
    object TheatreOwnerPanel : Screen("theatre_owner_panel")
    object OwnerSchedule : Screen("owner_schedule")
    object AdminRequests : Screen("admin_requests")
    object PhoneAuth : Screen("phone_auth")
    object EditProfile : Screen("edit_profile")

    // Streaming
    object StreamBrowse : Screen("stream_browse")
    object StreamDetail : Screen("stream_detail/{movieId}") {
        fun createRoute(movieId: String) = "stream_detail/$movieId"
    }
    object MyLibrary : Screen("my_library")
    object StreamPlayer : Screen("stream_player/{movieId}") {
        fun createRoute(movieId: String) = "stream_player/$movieId"
    }
    object AdminStreamingCatalog : Screen("admin_streaming_catalog")
    object AdminAddStreamingMovie : Screen("admin_add_streaming_movie")
}
