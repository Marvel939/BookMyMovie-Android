package com.example.bookmymovie.data.repository

import android.util.Log
import com.example.bookmymovie.data.api.RetrofitClient
import com.example.bookmymovie.model.LibraryItem
import com.example.bookmymovie.model.StreamingMovie
import com.example.bookmymovie.model.StreamingTransaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await

object StreamingRepository {

    private val db = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val catalogRef = db.getReference("streaming_catalog")
    private val libraryRef = db.getReference("user_library")
    private val transactionsRef = db.getReference("streaming_transactions")
    private val tmdbApi = RetrofitClient.api

    private const val TAG = "StreamingRepository"
    private const val IMAGE_BASE = "https://image.tmdb.org/t/p/w500"
    private const val BACKDROP_BASE = "https://image.tmdb.org/t/p/original"

    // TMDB provider IDs → platform names (India region)
    private val OTT_PROVIDERS = mapOf(
        8   to "Netflix",
        119 to "Prime Video",
        122 to "Hotstar",
        232 to "Zee5",
        237 to "SonyLIV",
        220 to "JioCinema"
    )

    // ═══════════════════════════════════════════════════════════════════════
    // CATALOG — TMDB OTT movies + Firebase admin movies
    // ═══════════════════════════════════════════════════════════════════════

    suspend fun getAllMovies(): List<StreamingMovie> {
        return try {
            coroutineScope {
                val tmdbDeferred = async { fetchTmdbOttMovies() }
                val firebaseDeferred = async { getFirebaseCatalogMovies() }

                val tmdbMovies = tmdbDeferred.await()
                val firebaseMovies = firebaseDeferred.await()

                // Firebase admin movies override TMDB if same title exists
                val firebaseTitles = firebaseMovies.map { it.title.lowercase() }.toSet()
                val merged = firebaseMovies + tmdbMovies.filter {
                    it.title.lowercase() !in firebaseTitles
                }

                merged.filter { it.isActive }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get movies", e)
            // Fallback: try Firebase only
            try {
                getFirebaseCatalogMovies().filter { it.isActive }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    /**
     * Fetch popular OTT movies from TMDB via discover endpoint.
     */
    private suspend fun fetchTmdbOttMovies(): List<StreamingMovie> {
        return try {
            val providerIds = OTT_PROVIDERS.keys.joinToString("|")

            // Fetch 2 pages for variety
            val movies = mutableListOf<StreamingMovie>()
            for (page in 1..2) {
                val response = tmdbApi.discoverMoviesWithProviders(
                    apiKey = MovieRepository.API_KEY,
                    providerIds = providerIds,
                    watchRegion = "IN",
                    page = page
                )
                // For each movie, try to get its watch providers to tag the OTT platform
                val pageMovies = coroutineScope {
                    response.results.map { tmdbMovie ->
                        async {
                            tmdbMovieToStreamingMovie(tmdbMovie)
                        }
                    }.awaitAll()
                }
                movies.addAll(pageMovies.filterNotNull())
            }

            Log.d(TAG, "Fetched ${movies.size} TMDB OTT movies")
            movies.distinctBy { it.title.lowercase() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch TMDB OTT movies", e)
            emptyList()
        }
    }

    private suspend fun tmdbMovieToStreamingMovie(
        tmdbMovie: com.example.bookmymovie.data.api.TmdbMovie
    ): StreamingMovie? {
        return try {
            // Get watch providers for this movie
            val ottPlatform = try {
                val providers = tmdbApi.getWatchProviders(tmdbMovie.id, MovieRepository.API_KEY)
                val inProviders = providers.results?.get("IN")
                val allProviders = (inProviders?.flatrate ?: emptyList()) +
                                   (inProviders?.rent ?: emptyList()) +
                                   (inProviders?.buy ?: emptyList())
                val matched = allProviders.firstOrNull { OTT_PROVIDERS.containsKey(it.providerId) }
                if (matched != null) OTT_PROVIDERS[matched.providerId] ?: "OTT"
                else {
                    // If no known provider matched, still include with first provider name
                    allProviders.firstOrNull()?.providerName ?: "OTT"
                }
            } catch (_: Exception) { "OTT" }

            // Fetch movie details to get runtime
            val duration = try {
                val detail = tmdbApi.getMovieDetails(tmdbMovie.id, MovieRepository.API_KEY)
                val runtime = detail.runtime ?: 0
                if (runtime > 0) "${runtime / 60}h ${runtime % 60}m" else ""
            } catch (_: Exception) { "" }

            val year = tmdbMovie.releaseDate?.take(4)?.toIntOrNull() ?: 0
            val genreNames = MovieRepository.let {
                // Use genre IDs from the movie
                tmdbMovie.genreIds.mapNotNull { id -> genreIdToName(id) }.joinToString(", ")
            }

            StreamingMovie(
                movieId = "tmdb_${tmdbMovie.id}",
                title = tmdbMovie.title,
                posterUrl = if (tmdbMovie.posterPath != null) "$IMAGE_BASE${tmdbMovie.posterPath}" else "",
                bannerUrl = if (tmdbMovie.backdropPath != null) "$BACKDROP_BASE${tmdbMovie.backdropPath}"
                           else if (tmdbMovie.posterPath != null) "$IMAGE_BASE${tmdbMovie.posterPath}" else "",
                description = tmdbMovie.overview,
                genre = genreNames.ifEmpty { "Movie" },
                duration = duration,
                releaseYear = year,
                rating = tmdbMovie.voteAverage,
                language = tmdbMovie.originalLanguage.uppercase(),
                ottPlatform = ottPlatform,
                rentPrice = 149.0,
                buyPrice = 499.0,
                rentDurationDays = 30,
                isActive = true,
                source = "tmdb"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert TMDB movie ${tmdbMovie.title}", e)
            null
        }
    }

    // Simple genre ID → name map
    private val GENRE_MAP = mapOf(
        28 to "Action", 12 to "Adventure", 16 to "Animation", 35 to "Comedy",
        80 to "Crime", 99 to "Documentary", 18 to "Drama", 10751 to "Family",
        14 to "Fantasy", 36 to "History", 27 to "Horror", 10402 to "Music",
        9648 to "Mystery", 10749 to "Romance", 878 to "Sci-Fi", 10770 to "TV Movie",
        53 to "Thriller", 10752 to "War", 37 to "Western"
    )

    private fun genreIdToName(id: Int): String? = GENRE_MAP[id]

    /**
     * Fetch only Firebase catalog movies (admin-added).
     */
    private suspend fun getFirebaseCatalogMovies(): List<StreamingMovie> {
        return try {
            val snap = catalogRef.get().await()
            snap.children.mapNotNull { child -> parseStreamingMovie(child) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get Firebase catalog", e)
            emptyList()
        }
    }

    suspend fun getMovieById(movieId: String): StreamingMovie? {
        return try {
            val snap = catalogRef.child(movieId).get().await()
            if (!snap.exists()) return null
            parseStreamingMovie(snap)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get movie $movieId", e)
            null
        }
    }

    private fun parseStreamingMovie(snap: com.google.firebase.database.DataSnapshot): StreamingMovie? {
        return try {
            StreamingMovie(
                movieId = snap.key ?: return null,
                title = snap.child("title").value as? String ?: "",
                posterUrl = snap.child("posterUrl").value as? String ?: "",
                bannerUrl = snap.child("bannerUrl").value as? String ?: "",
                description = snap.child("description").value as? String ?: "",
                genre = snap.child("genre").value as? String ?: "",
                duration = snap.child("duration").value as? String ?: "",
                releaseYear = (snap.child("releaseYear").value as? Long)?.toInt() ?: 0,
                rating = (snap.child("rating").value as? Number)?.toDouble() ?: 0.0,
                language = snap.child("language").value as? String ?: "",
                director = snap.child("director").value as? String ?: "",
                cast = (snap.child("cast").value as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                trailerUrl = snap.child("trailerUrl").value as? String ?: "",
                streamUrl = snap.child("streamUrl").value as? String ?: "",
                ottPlatform = snap.child("ottPlatform").value as? String ?: "",
                ottLogoUrl = snap.child("ottLogoUrl").value as? String ?: "",
                rentPrice = (snap.child("rentPrice").value as? Number)?.toDouble() ?: 149.0,
                buyPrice = (snap.child("buyPrice").value as? Number)?.toDouble() ?: 499.0,
                rentDurationDays = (snap.child("rentDurationDays").value as? Long)?.toInt() ?: 30,
                isExclusive = snap.child("isExclusive").value as? Boolean ?: false,
                isActive = snap.child("isActive").value as? Boolean ?: true,
                source = snap.child("source").value as? String ?: "admin"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Parse error", e)
            null
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // USER LIBRARY — Check ownership from Firebase
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Check if user owns or has active rental for a specific movie.
     * Returns null if user does NOT have access.
     */
    suspend fun isMovieInLibrary(movieId: String): LibraryItem? {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Log.d(TAG, "isMovieInLibrary: No user logged in")
            return null
        }

        return try {
            val snap = libraryRef.child(uid).get().await()
            if (!snap.exists() || !snap.hasChildren()) {
                Log.d(TAG, "isMovieInLibrary: No library entries for user $uid")
                return null
            }

            val matchingItems = mutableListOf<LibraryItem>()

            for (child in snap.children) {
                val mId = child.child("movieId").value as? String
                if (mId == null || mId != movieId) continue

                val item = LibraryItem(
                    purchaseId = child.key ?: continue,
                    movieId = mId,
                    title = child.child("title").value as? String ?: "",
                    posterUrl = child.child("posterUrl").value as? String ?: "",
                    type = child.child("type").value as? String ?: "rent",
                    purchasedAt = child.child("purchasedAt").value as? Long ?: 0L,
                    expiresAt = child.child("expiresAt").value as? Long,
                    amountPaid = (child.child("amountPaid").value as? Number)?.toDouble() ?: 0.0,
                    paymentIntentId = child.child("paymentIntentId").value as? String ?: "",
                    ottPlatform = child.child("ottPlatform").value as? String ?: ""
                )
                matchingItems.add(item)
            }

            if (matchingItems.isEmpty()) {
                Log.d(TAG, "isMovieInLibrary: Movie $movieId NOT found in user library")
                return null
            }

            // Return only non-expired items, preferring "buy" over "rent"
            val activeItems = matchingItems.filter { !it.isExpired() }
            if (activeItems.isEmpty()) {
                Log.d(TAG, "isMovieInLibrary: Movie $movieId found but ALL entries are expired")
                return null
            }

            val result = activeItems.find { it.type == "buy" } ?: activeItems.first()
            Log.d(TAG, "isMovieInLibrary: Movie $movieId FOUND - type=${result.type}, expired=${result.isExpired()}")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check library for movie $movieId", e)
            null
        }
    }

    /**
     * Get all library items for current user.
     */
    suspend fun getUserLibrary(): List<LibraryItem> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        return try {
            val snap = libraryRef.child(uid).get().await()
            if (!snap.exists()) return emptyList()
            snap.children.mapNotNull { child ->
                try {
                    LibraryItem(
                        purchaseId = child.key ?: return@mapNotNull null,
                        movieId = child.child("movieId").value as? String ?: "",
                        title = child.child("title").value as? String ?: "",
                        posterUrl = child.child("posterUrl").value as? String ?: "",
                        type = child.child("type").value as? String ?: "rent",
                        purchasedAt = child.child("purchasedAt").value as? Long ?: 0L,
                        expiresAt = child.child("expiresAt").value as? Long,
                        amountPaid = (child.child("amountPaid").value as? Number)?.toDouble() ?: 0.0,
                        paymentIntentId = child.child("paymentIntentId").value as? String ?: "",
                        ottPlatform = child.child("ottPlatform").value as? String ?: ""
                    )
                } catch (e: Exception) { null }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get library", e)
            emptyList()
        }
    }

    suspend fun getActiveLibraryItems(): List<LibraryItem> {
        return getUserLibrary().filter { !it.isExpired() }
    }

    suspend fun getExpiredLibraryItems(): List<LibraryItem> {
        return getUserLibrary().filter { it.isExpired() }
    }

    /**
     * Record purchase — called ONLY after successful payment.
     */
    suspend fun recordPurchase(
        movie: StreamingMovie,
        type: String,
        paymentIntentId: String
    ): String {
        val uid = auth.currentUser?.uid ?: throw Exception("User not authenticated")

        val purchaseId = libraryRef.child(uid).push().key
            ?: throw Exception("Failed to generate purchase ID")

        val now = System.currentTimeMillis()
        val amountPaid = if (type == "rent") movie.rentPrice else movie.buyPrice
        val expiresAt = if (type == "rent") {
            now + (movie.rentDurationDays * 24L * 60 * 60 * 1000)
        } else null

        val libraryData = mapOf(
            "movieId" to movie.movieId,
            "title" to movie.title,
            "posterUrl" to movie.posterUrl,
            "type" to type,
            "purchasedAt" to now,
            "expiresAt" to expiresAt,
            "amountPaid" to amountPaid,
            "paymentIntentId" to paymentIntentId,
            "ottPlatform" to movie.ottPlatform
        )
        libraryRef.child(uid).child(purchaseId).setValue(libraryData).await()

        val txId = transactionsRef.push().key ?: "tx_${now}"
        val txData = mapOf(
            "transactionId" to txId,
            "userId" to uid,
            "movieId" to movie.movieId,
            "movieTitle" to movie.title,
            "type" to type,
            "amount" to amountPaid,
            "paymentIntentId" to paymentIntentId,
            "status" to "completed",
            "timestamp" to now
        )
        transactionsRef.child(txId).setValue(txData).await()

        Log.d(TAG, "Purchase recorded: $purchaseId for movie ${movie.title}")
        return purchaseId
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ADMIN
    // ═══════════════════════════════════════════════════════════════════════

    suspend fun addStreamingMovie(movie: StreamingMovie): String {
        val key = catalogRef.push().key ?: throw Exception("Failed to generate ID")
        catalogRef.child(key).setValue(movieToMap(movie)).await()
        return key
    }

    suspend fun updateStreamingMovie(movie: StreamingMovie) {
        if (movie.movieId.isBlank()) throw Exception("Movie ID is required")
        catalogRef.child(movie.movieId).setValue(movieToMap(movie)).await()
    }

    suspend fun deleteStreamingMovie(movieId: String) {
        catalogRef.child(movieId).removeValue().await()
    }

    suspend fun toggleMovieActive(movieId: String, isActive: Boolean) {
        catalogRef.child(movieId).child("isActive").setValue(isActive).await()
    }

    suspend fun getAllTransactions(): List<StreamingTransaction> {
        return try {
            val snap = transactionsRef.get().await()
            snap.children.mapNotNull { child ->
                try {
                    StreamingTransaction(
                        transactionId = child.key ?: return@mapNotNull null,
                        userId = child.child("userId").value as? String ?: "",
                        movieId = child.child("movieId").value as? String ?: "",
                        movieTitle = child.child("movieTitle").value as? String ?: "",
                        type = child.child("type").value as? String ?: "rent",
                        amount = (child.child("amount").value as? Number)?.toDouble() ?: 0.0,
                        paymentIntentId = child.child("paymentIntentId").value as? String ?: "",
                        status = child.child("status").value as? String ?: "completed",
                        timestamp = child.child("timestamp").value as? Long ?: 0L
                    )
                } catch (e: Exception) { null }
            }.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get transactions", e)
            emptyList()
        }
    }

    // Fetch ALL admin movies (including inactive) for admin panel
    suspend fun getAllAdminMovies(): List<StreamingMovie> {
        return try {
            val snap = catalogRef.get().await()
            snap.children.mapNotNull { child ->
                parseStreamingMovie(child)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load admin movies", e)
            emptyList()
        }
    }

    private fun movieToMap(movie: StreamingMovie): Map<String, Any?> = mapOf(
        "title" to movie.title,
        "posterUrl" to movie.posterUrl,
        "bannerUrl" to movie.bannerUrl,
        "description" to movie.description,
        "genre" to movie.genre,
        "duration" to movie.duration,
        "releaseYear" to movie.releaseYear,
        "rating" to movie.rating,
        "language" to movie.language,
        "director" to movie.director,
        "cast" to movie.cast,
        "trailerUrl" to movie.trailerUrl,
        "streamUrl" to movie.streamUrl,
        "ottPlatform" to movie.ottPlatform,
        "ottLogoUrl" to movie.ottLogoUrl,
        "rentPrice" to movie.rentPrice,
        "buyPrice" to movie.buyPrice,
        "rentDurationDays" to movie.rentDurationDays,
        "isExclusive" to movie.isExclusive,
        "isActive" to movie.isActive,
        "source" to movie.source
    )
}
