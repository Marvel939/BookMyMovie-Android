package com.example.bookmymovie.data.repository

import com.example.bookmymovie.data.api.RetrofitClient
import com.example.bookmymovie.data.api.TmdbMovie
import com.example.bookmymovie.data.api.TmdbMovieDetail
import com.example.bookmymovie.data.api.TmdbPersonDetail
import com.example.bookmymovie.ui.screens.CastMember
import com.example.bookmymovie.ui.screens.CrewMember
import com.example.bookmymovie.ui.screens.Movie
import com.example.bookmymovie.ui.screens.Review

object MovieRepository {

    const val API_KEY = "1afed21bf996256e6d6aad1ff85dee16"

    private const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500"
    private const val BACKDROP_BASE_URL = "https://image.tmdb.org/t/p/original"

    private val api = RetrofitClient.api
    private var genreMap: Map<Int, String> = emptyMap()

    private suspend fun loadGenres() {
        if (genreMap.isEmpty()) {
            try {
                val response = api.getGenres(API_KEY)
                genreMap = response.genres.associate { it.id to it.name }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun fetchNowPlaying(): List<Movie> {
        loadGenres()
        val response = api.getNowPlaying(API_KEY)
        return response.results.map { it.toMovie() }
    }

    suspend fun fetchTrendingDay(): List<Movie> {
        loadGenres()
        val response = api.getTrendingDay(API_KEY)
        return response.results.map { it.toMovie() }
    }

    suspend fun fetchUpcoming(): List<Movie> {
        loadGenres()
        val response = api.getUpcoming(API_KEY)
        return response.results.map { it.toMovie() }
    }

    suspend fun fetchPopular(): List<Movie> {
        loadGenres()
        val response = api.getPopular(API_KEY)
        return response.results.map { it.toMovie() }
    }

    suspend fun fetchTopRated(): List<Movie> {
        loadGenres()
        val response = api.getTopRated(API_KEY)
        return response.results.map { it.toMovie() }
    }

    suspend fun fetchMovieDetail(movieId: Int): Movie? {
        return try {
            val detail = api.getMovieDetails(movieId, API_KEY)
            val reviews = try {
                val reviewResponse = api.getMovieReviews(movieId, API_KEY)
                reviewResponse.results.map { r ->
                    Review(
                        author = r.author,
                        content = r.content,
                        rating = r.authorDetails?.rating,
                        avatarUrl = r.authorDetails?.avatarPath?.let {
                            if (it.startsWith("/http")) it.substring(1) else "$IMAGE_BASE_URL$it"
                        }
                    )
                }
            } catch (e: Exception) {
                emptyList()
            }
            detail.toMovie(reviews)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun fetchPersonDetail(personId: Int): TmdbPersonDetail? {
        return try {
            api.getPersonDetails(personId, API_KEY)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun personMoviesToMovieList(person: TmdbPersonDetail): List<Movie> {
        val castMovies = person.movieCredits?.cast ?: emptyList()
        val crewMovies = person.movieCredits?.crew ?: emptyList()
        val allMovies = (castMovies + crewMovies)
            .distinctBy { it.id }
            .sortedByDescending { it.voteAverage }
            .take(20)
        return allMovies.map { it.toMovie() }
    }

    suspend fun searchMovies(query: String): List<Movie> {
        loadGenres()
        val response = api.searchMovies(API_KEY, query)
        return response.results.map { it.toMovie() }
    }

    private fun TmdbMovie.toMovie(): Movie {
        val genreNames = genreIds.mapNotNull { genreMap[it] }.joinToString(", ")
        val year = releaseDate?.take(4)
        val displayTitle = if (!year.isNullOrBlank()) "$title ($year)" else title
        return Movie(
            id = id.toString(),
            title = displayTitle,
            posterUrl = if (posterPath != null) "$IMAGE_BASE_URL$posterPath" else "",
            bannerUrl = if (backdropPath != null) "$BACKDROP_BASE_URL$backdropPath"
                        else if (posterPath != null) "$IMAGE_BASE_URL$posterPath" else "",
            rating = String.format("%.1f/10", voteAverage),
            language = originalLanguage.uppercase(),
            genre = genreNames.ifEmpty { "Movie" },
            duration = "",
            description = overview,
            releaseDate = releaseDate ?: "Coming Soon"
        )
    }

    private fun TmdbMovieDetail.toMovie(reviews: List<Review> = emptyList()): Movie {
        val genreNames = genres.joinToString(", ") { it.name }
        val hours = (runtime ?: 0) / 60
        val minutes = (runtime ?: 0) % 60
        val durationStr = if (runtime != null && runtime > 0) "${hours}h ${minutes}m" else ""

        val directors = credits?.crew?.filter { it.job == "Director" }?.map { it.name } ?: emptyList()
        val castMembers = credits?.cast?.take(10)?.map {
            CastMember(
                id = it.id,
                name = it.name,
                character = it.character ?: "",
                imageUrl = if (it.profilePath != null) "$IMAGE_BASE_URL${it.profilePath}" else ""
            )
        } ?: emptyList()
        val crewMembers = credits?.crew?.filter { it.job != "Director" }?.take(6)?.map {
            CrewMember(
                id = it.id,
                name = it.name,
                job = it.job,
                imageUrl = if (it.profilePath != null) "$IMAGE_BASE_URL${it.profilePath}" else ""
            )
        } ?: emptyList()
        val year = releaseDate?.take(4)
        val displayTitle = if (!year.isNullOrBlank()) "$title ($year)" else title

        return Movie(
            id = id.toString(),
            title = displayTitle,
            posterUrl = if (posterPath != null) "$IMAGE_BASE_URL$posterPath" else "",
            bannerUrl = if (backdropPath != null) "$BACKDROP_BASE_URL$backdropPath"
                        else if (posterPath != null) "$IMAGE_BASE_URL$posterPath" else "",
            rating = String.format("%.1f/10", voteAverage),
            language = originalLanguage.uppercase(),
            genre = genreNames.ifEmpty { "Movie" },
            duration = durationStr,
            description = overview,
            directors = directors,
            cast = castMembers,
            crew = crewMembers,
            releaseDate = releaseDate ?: "Coming Soon",
            reviews = reviews
        )
    }
}
