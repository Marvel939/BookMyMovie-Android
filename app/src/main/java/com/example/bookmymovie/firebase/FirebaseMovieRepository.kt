package com.example.bookmymovie.firebase

import com.example.bookmymovie.model.City
import com.example.bookmymovie.model.LocalMovie
import com.example.bookmymovie.model.Showtime
import com.example.bookmymovie.model.Theatre
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Repository for fetching city, theatre, movie, and showtime data
 * from Firebase Realtime Database.
 *
 * Database structure:
 *   cities/{cityName} -> City
 *   theatres/{cityName}/{theatreId} -> Theatre
 *   movies/{movieId} -> LocalMovie
 *   showtimes/{cityName}/{theatreId}/{movieId} -> Showtime
 */
object FirebaseMovieRepository {

    private val database = FirebaseDatabase.getInstance()
    private val citiesRef = database.getReference("cities")
    private val theatresRef = database.getReference("theatres")
    private val moviesRef = database.getReference("movies")
    private val showtimesRef = database.getReference("showtimes")

    // ─── CITIES ──────────────────────────────────────────────

    /**
     * Fetch all available cities as a one-shot suspend call.
     */
    suspend fun getCities(): List<City> = suspendCancellableCoroutine { cont ->
        citiesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val cities = snapshot.children.mapNotNull { child ->
                    child.getValue(City::class.java)?.copy(name = child.key ?: "")
                }
                cont.resume(cities)
            }

            override fun onCancelled(error: DatabaseError) {
                cont.resumeWithException(error.toException())
            }
        })
    }

    /**
     * Observe cities as a Flow for real-time updates.
     */
    fun observeCities(): Flow<List<City>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val cities = snapshot.children.mapNotNull { child ->
                    child.getValue(City::class.java)?.copy(name = child.key ?: "")
                }
                trySend(cities)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        citiesRef.addValueEventListener(listener)
        awaitClose { citiesRef.removeEventListener(listener) }
    }

    // ─── THEATRES ────────────────────────────────────────────

    /**
     * Fetch all theatres in a given city.
     */
    suspend fun getTheatresInCity(city: String): List<Theatre> =
        suspendCancellableCoroutine { cont ->
            theatresRef.child(city)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val theatres = snapshot.children.mapNotNull { child ->
                            child.getValue(Theatre::class.java)?.copy(
                                theatreId = child.key ?: "",
                                city = city
                            )
                        }
                        cont.resume(theatres)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        cont.resumeWithException(error.toException())
                    }
                })
        }

    /**
     * Observe theatres in a city with real-time updates.
     */
    fun observeTheatresInCity(city: String): Flow<List<Theatre>> = callbackFlow {
        val ref = theatresRef.child(city)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val theatres = snapshot.children.mapNotNull { child ->
                    child.getValue(Theatre::class.java)?.copy(
                        theatreId = child.key ?: "",
                        city = city
                    )
                }
                trySend(theatres)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // ─── MOVIES ──────────────────────────────────────────────

    /**
     * Fetch a single movie by ID.
     */
    suspend fun getMovie(movieId: String): LocalMovie? =
        suspendCancellableCoroutine { cont ->
            moviesRef.child(movieId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val movie = snapshot.getValue(LocalMovie::class.java)
                            ?.copy(movieId = snapshot.key ?: "")
                        cont.resume(movie)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        cont.resumeWithException(error.toException())
                    }
                })
        }

    /**
     * Fetch multiple movies by their IDs.
     */
    suspend fun getMoviesByIds(movieIds: List<String>): List<LocalMovie> {
        return movieIds.mapNotNull { id -> getMovie(id) }
    }

    /**
     * Fetch all movies from Firebase.
     */
    suspend fun getAllMovies(): List<LocalMovie> = suspendCancellableCoroutine { cont ->
        moviesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val movies = snapshot.children.mapNotNull { child ->
                    child.getValue(LocalMovie::class.java)?.copy(movieId = child.key ?: "")
                }
                cont.resume(movies)
            }

            override fun onCancelled(error: DatabaseError) {
                cont.resumeWithException(error.toException())
            }
        })
    }

    // ─── SHOWTIMES ───────────────────────────────────────────

    /**
     * Fetch all showtimes in a city (across all theatres).
     * Returns a map: theatreId -> (movieId -> Showtime)
     */
    suspend fun getShowtimesInCity(city: String): Map<String, Map<String, Showtime>> =
        suspendCancellableCoroutine { cont ->
            showtimesRef.child(city)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val result = mutableMapOf<String, Map<String, Showtime>>()
                        for (theatreSnap in snapshot.children) {
                            val theatreId = theatreSnap.key ?: continue
                            val movieShowtimes = mutableMapOf<String, Showtime>()
                            for (movieSnap in theatreSnap.children) {
                                val movieId = movieSnap.key ?: continue
                                val showtime = movieSnap.getValue(Showtime::class.java)
                                    ?.copy(
                                        movieId = movieId,
                                        theatreId = theatreId,
                                        city = city
                                    )
                                if (showtime != null) {
                                    movieShowtimes[movieId] = showtime
                                }
                            }
                            result[theatreId] = movieShowtimes
                        }
                        cont.resume(result)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        cont.resumeWithException(error.toException())
                    }
                })
        }

    /**
     * Fetch showtimes for a specific theatre.
     */
    suspend fun getShowtimesForTheatre(
        city: String,
        theatreId: String
    ): List<Showtime> = suspendCancellableCoroutine { cont ->
        showtimesRef.child(city).child(theatreId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val showtimes = snapshot.children.mapNotNull { child ->
                        child.getValue(Showtime::class.java)?.copy(
                            movieId = child.key ?: "",
                            theatreId = theatreId,
                            city = city
                        )
                    }
                    cont.resume(showtimes)
                }

                override fun onCancelled(error: DatabaseError) {
                    cont.resumeWithException(error.toException())
                }
            })
    }

    /**
     * Extract all unique movie IDs that have showtimes in a given city.
     */
    suspend fun getMovieIdsInCity(city: String): Set<String> {
        val showtimes = getShowtimesInCity(city)
        return showtimes.values.flatMap { it.keys }.toSet()
    }

    /**
     * Get all movies currently showing in a city
     * by fetching showtime movie IDs and then fetching movie details.
     */
    suspend fun getMoviesInCity(city: String): List<LocalMovie> {
        val movieIds = getMovieIdsInCity(city)
        return getMoviesByIds(movieIds.toList())
    }

    // ─── OBSERVE SHOWTIMES ───────────────────────────────────

    /**
     * Observe showtimes in a city as a real-time Flow.
     */
    fun observeShowtimesInCity(city: String): Flow<Map<String, Map<String, Showtime>>> =
        callbackFlow {
            val ref = showtimesRef.child(city)
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val result = mutableMapOf<String, Map<String, Showtime>>()
                    for (theatreSnap in snapshot.children) {
                        val theatreId = theatreSnap.key ?: continue
                        val movieShowtimes = mutableMapOf<String, Showtime>()
                        for (movieSnap in theatreSnap.children) {
                            val movieId = movieSnap.key ?: continue
                            val showtime = movieSnap.getValue(Showtime::class.java)
                                ?.copy(
                                    movieId = movieId,
                                    theatreId = theatreId,
                                    city = city
                                )
                            if (showtime != null) {
                                movieShowtimes[movieId] = showtime
                            }
                        }
                        result[theatreId] = movieShowtimes
                    }
                    trySend(result)
                }

                override fun onCancelled(error: DatabaseError) {
                    close(error.toException())
                }
            }
            ref.addValueEventListener(listener)
            awaitClose { ref.removeEventListener(listener) }
        }
}
