package com.example.bookmymovie.data.api

import com.google.gson.annotations.SerializedName

data class TmdbMovieListResponse(
    val results: List<TmdbMovie>,
    val page: Int,
    @SerializedName("total_pages") val totalPages: Int
)

data class TmdbMovie(
    val id: Int,
    val title: String,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("vote_average") val voteAverage: Double,
    @SerializedName("original_language") val originalLanguage: String,
    @SerializedName("genre_ids") val genreIds: List<Int> = emptyList(),
    val overview: String,
    @SerializedName("release_date") val releaseDate: String?
)

data class TmdbMovieDetail(
    val id: Int,
    val title: String,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("vote_average") val voteAverage: Double,
    @SerializedName("original_language") val originalLanguage: String,
    val genres: List<TmdbGenre>,
    val overview: String,
    @SerializedName("release_date") val releaseDate: String?,
    val runtime: Int?,
    val credits: TmdbCredits?
)

data class TmdbGenre(
    val id: Int,
    val name: String
)

data class TmdbGenreListResponse(
    val genres: List<TmdbGenre>
)

data class TmdbCredits(
    val cast: List<TmdbCastMember>,
    val crew: List<TmdbCrewMember>
)

data class TmdbCastMember(
    val id: Int,
    val name: String,
    val character: String?,
    @SerializedName("profile_path") val profilePath: String?
)

data class TmdbCrewMember(
    val id: Int,
    val name: String,
    val job: String,
    val department: String,
    @SerializedName("profile_path") val profilePath: String?
)

data class TmdbReviewResponse(
    val results: List<TmdbReview>,
    val page: Int,
    @SerializedName("total_results") val totalResults: Int
)

data class TmdbReview(
    val id: String,
    val author: String,
    val content: String,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("author_details") val authorDetails: TmdbAuthorDetails?
)

data class TmdbAuthorDetails(
    val name: String?,
    val username: String?,
    val rating: Double?,
    @SerializedName("avatar_path") val avatarPath: String?
)

data class TmdbPersonDetail(
    val id: Int,
    val name: String,
    val biography: String?,
    val birthday: String?,
    val deathday: String?,
    @SerializedName("place_of_birth") val placeOfBirth: String?,
    @SerializedName("profile_path") val profilePath: String?,
    @SerializedName("known_for_department") val knownForDepartment: String?,
    @SerializedName("movie_credits") val movieCredits: TmdbPersonMovieCredits?
)

data class TmdbPersonMovieCredits(
    val cast: List<TmdbMovie>?,
    val crew: List<TmdbMovie>?
)
