package com.example.bookmymovie.data.api

import retrofit2.http.GET
import retrofit2.http.Query

interface PlacesApi {

    @GET("nearbysearch/json")
    suspend fun getNearbyTheatres(
        @Query("location") location: String,
        @Query("radius") radius: Int,
        @Query("type") type: String,
        @Query("fields") fields: String = "formatted_address,geometry,icon,icon_mask_base_uri,icon_background_color,name,opening_hours,photos,place_id,plus_code,types,rating,user_ratings_total",
        @Query("key") apiKey: String
    ): PlacesNearbyResponse

    @GET("details/json")
    suspend fun getPlaceDetails(
        @Query("place_id") placeId: String,
        @Query("fields") fields: String = "reviews",
        @Query("key") apiKey: String
    ): PlaceDetailsResponse
}
