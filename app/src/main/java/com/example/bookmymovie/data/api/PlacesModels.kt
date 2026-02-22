package com.example.bookmymovie.data.api

import com.google.gson.annotations.SerializedName

data class PlacesNearbyResponse(
    val results: List<PlaceResult>,
    val status: String
)

data class PlaceResult(
    val name: String,
    val vicinity: String?,
    val rating: Double?,
    @SerializedName("place_id") val placeId: String,
    val photos: List<PlacePhoto>?,
    val geometry: PlaceGeometry?
)

data class PlacePhoto(
    @SerializedName("photo_reference") val photoReference: String,
    val height: Int,
    val width: Int
)

data class PlaceGeometry(
    val location: PlaceLatLng
)

data class PlaceLatLng(
    val lat: Double,
    val lng: Double
)

// ---- Place Details (for reviews) ----

data class PlaceDetailsResponse(
    val result: PlaceDetailResult?,
    val status: String
)

data class PlaceDetailResult(
    val reviews: List<PlaceReview>?
)

data class PlaceReview(
    @SerializedName("author_name") val authorName: String,
    @SerializedName("profile_photo_url") val profilePhotoUrl: String?,
    val rating: Int,
    val text: String,
    @SerializedName("relative_time_description") val relativeTime: String
)
