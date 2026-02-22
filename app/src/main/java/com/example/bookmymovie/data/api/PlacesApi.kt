package com.example.bookmymovie.data.api

import retrofit2.http.GET
import retrofit2.http.Query

interface PlacesApi {

    @GET("nearbysearch/json")
    suspend fun getNearbyTheatres(
        @Query("location") location: String,
        @Query("radius") radius: Int,
        @Query("type") type: String,
        @Query("key") apiKey: String
    ): PlacesNearbyResponse

    @GET("details/json")
    suspend fun getPlaceDetails(
        @Query("place_id") placeId: String,
        @Query("fields") fields: String = "reviews",
        @Query("key") apiKey: String
    ): PlaceDetailsResponse
}
