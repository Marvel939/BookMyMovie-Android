package com.example.bookmymovie.firebase

import com.example.bookmymovie.model.City
import com.example.bookmymovie.model.LocalMovie
import com.example.bookmymovie.model.Showtime
import com.example.bookmymovie.model.Theatre
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Seeds the Firebase Realtime Database with realistic dummy data
 * for cities, theatres, movies, and showtimes.
 *
 * Call [seedAll] once to populate the database.
 * Call [clearAll] to wipe seeded data.
 */
object DummyDataSeeder {

    private val database = FirebaseDatabase.getInstance()

    // ─── CITIES ──────────────────────────────────────────────

    private val cities = mapOf(
        "Ahmedabad" to City("Ahmedabad", "Gujarat", "India", 23.0225, 72.5714, true),
        "Mumbai" to City("Mumbai", "Maharashtra", "India", 19.0760, 72.8777, true),
        "Delhi" to City("Delhi", "Delhi", "India", 28.7041, 77.1025, true),
        "Bangalore" to City("Bangalore", "Karnataka", "India", 12.9716, 77.5946, true),
        "Chennai" to City("Chennai", "Tamil Nadu", "India", 13.0827, 80.2707, true),
        "Hyderabad" to City("Hyderabad", "Telangana", "India", 17.3850, 78.4867, true),
        "Kolkata" to City("Kolkata", "West Bengal", "India", 22.5726, 88.3639, true),
        "Pune" to City("Pune", "Maharashtra", "India", 18.5204, 73.8567, true),
        "Jaipur" to City("Jaipur", "Rajasthan", "India", 26.9124, 75.7873, true),
        "Surat" to City("Surat", "Gujarat", "India", 21.1702, 72.8311, true)
    )

    // ─── THEATRES ────────────────────────────────────────────

    private val theatres = mapOf(
        "Ahmedabad" to listOf(
            Theatre("t_ahm_1", "PVR Acropolis", "Acropolis Mall, Thaltej", 23.0378, 72.5066, "Ahmedabad", 5, listOf("IMAX", "Dolby Atmos", "Parking", "Food Court"), "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=400", 4.3, 2150),
            Theatre("t_ahm_2", "INOX Ahmedabad One", "Ahmedabad One Mall, Vastrapur", 23.0340, 72.5275, "Ahmedabad", 7, listOf("4DX", "Dolby Atmos", "Recliner"), "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=400", 4.5, 3200),
            Theatre("t_ahm_3", "Cinepolis Alpha One", "Alpha One Mall, Vastrapur", 23.0302, 72.5299, "Ahmedabad", 6, listOf("3D", "Parking", "Wheelchair Accessible"), "https://images.unsplash.com/photo-1595769816263-9b910be24d5f?w=400", 4.1, 1800),
            Theatre("t_ahm_4", "Rajhans Cineplex", "Paldi, Ahmedabad", 23.0088, 72.5636, "Ahmedabad", 4, listOf("Parking", "Food Court"), "https://images.unsplash.com/photo-1440404653325-ab127d49abc1?w=400", 3.9, 1200),
            Theatre("t_ahm_5", "Wide Angle Multiplex", "SG Highway, Ahmedabad", 23.0469, 72.5098, "Ahmedabad", 3, listOf("Dolby Sound", "Parking"), "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=400", 4.0, 900)
        ),
        "Mumbai" to listOf(
            Theatre("t_mum_1", "PVR IMAX Lower Parel", "Palladium Mall, Lower Parel", 18.9944, 72.8260, "Mumbai", 8, listOf("IMAX", "Dolby Atmos", "Luxury Recliner", "Valet Parking"), "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=400", 4.6, 5400),
            Theatre("t_mum_2", "INOX Megaplex", "Inorbit Mall, Malad", 19.1775, 72.8416, "Mumbai", 11, listOf("ScreenX", "4DX", "IMAX", "Food Court"), "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=400", 4.4, 4200),
            Theatre("t_mum_3", "Cinepolis Andheri", "Fun Republic, Andheri", 19.1296, 72.8336, "Mumbai", 6, listOf("3D", "Dolby Atmos", "Parking"), "https://images.unsplash.com/photo-1595769816263-9b910be24d5f?w=400", 4.2, 3100),
            Theatre("t_mum_4", "Regal Cinema", "Colaba, Mumbai", 18.9217, 72.8321, "Mumbai", 1, listOf("Heritage", "Iconic"), "https://images.unsplash.com/photo-1440404653325-ab127d49abc1?w=400", 4.7, 6800),
            Theatre("t_mum_5", "PVR Juhu", "Juhu, Mumbai", 19.0980, 72.8263, "Mumbai", 4, listOf("Dolby Atmos", "Parking", "Premium"), "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=400", 4.3, 2900)
        ),
        "Delhi" to listOf(
            Theatre("t_del_1", "PVR Select Citywalk", "Saket, New Delhi", 28.5285, 77.2190, "Delhi", 6, listOf("IMAX", "4DX", "Luxury Dining"), "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=400", 4.5, 4800),
            Theatre("t_del_2", "PVR Director's Cut", "Ambience Mall, Vasant Kunj", 28.5375, 77.1541, "Delhi", 3, listOf("Premium", "Luxury Recliner", "Gourmet Dining"), "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=400", 4.8, 3600),
            Theatre("t_del_3", "INOX Nehru Place", "Nehru Place, New Delhi", 28.5491, 77.2533, "Delhi", 5, listOf("Dolby Atmos", "3D", "Parking"), "https://images.unsplash.com/photo-1595769816263-9b910be24d5f?w=400", 4.1, 2200),
            Theatre("t_del_4", "Wave Cinemas", "Noida Sector 18", 28.5699, 77.3218, "Delhi", 8, listOf("MX4D", "IMAX", "Food Court"), "https://images.unsplash.com/photo-1440404653325-ab127d49abc1?w=400", 4.0, 3500)
        ),
        "Bangalore" to listOf(
            Theatre("t_blr_1", "PVR Orion Mall", "Rajajinagar, Bangalore", 13.0107, 77.5550, "Bangalore", 7, listOf("IMAX", "Dolby Atmos", "Premium"), "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=400", 4.4, 4100),
            Theatre("t_blr_2", "INOX Garuda Mall", "Magrath Road, Bangalore", 12.9693, 77.6103, "Bangalore", 5, listOf("3D", "Dolby Sound", "Parking"), "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=400", 4.2, 3300),
            Theatre("t_blr_3", "Cinepolis Nexus Mall", "Koramangala, Bangalore", 12.9352, 77.6245, "Bangalore", 6, listOf("4DX", "Dolby Atmos", "Recliner"), "https://images.unsplash.com/photo-1595769816263-9b910be24d5f?w=400", 4.3, 2800)
        ),
        "Hyderabad" to listOf(
            Theatre("t_hyd_1", "PVR Next Galleria", "Panjagutta, Hyderabad", 17.4349, 78.4507, "Hyderabad", 6, listOf("IMAX", "Luxury", "Parking"), "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=400", 4.3, 3400),
            Theatre("t_hyd_2", "INOX GVK One", "Banjara Hills, Hyderabad", 17.4236, 78.4471, "Hyderabad", 5, listOf("Dolby Atmos", "Premium", "Food Court"), "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=400", 4.5, 4000),
            Theatre("t_hyd_3", "AMB Cinemas", "Gachibowli, Hyderabad", 17.4401, 78.3489, "Hyderabad", 4, listOf("4K Laser", "Dolby Atmos", "Luxury Recliner"), "https://images.unsplash.com/photo-1595769816263-9b910be24d5f?w=400", 4.7, 5200)
        )
    )

    // ─── MOVIES ──────────────────────────────────────────────

    private val movies = listOf(
        LocalMovie(
            movieId = "m1",
            title = "Singham Again",
            posterUrl = "https://image.tmdb.org/t/p/w500/placeholder1.jpg",
            bannerUrl = "https://image.tmdb.org/t/p/w780/placeholder1.jpg",
            genre = listOf("Action", "Drama"),
            releaseDate = "2026-02-14",
            rating = 8.2,
            popularityScore = 95.0,
            duration = "2h 38m",
            language = "Hindi",
            description = "Bajirao Singham returns in an epic action-drama to fight corruption at an unprecedented scale.",
            certification = "UA",
            directors = listOf("Rohit Shetty"),
            cast = listOf("Ajay Devgn", "Deepika Padukone", "Tiger Shroff"),
            trailerUrl = ""
        ),
        LocalMovie(
            movieId = "m2",
            title = "Pushpa 3: The Rampage",
            posterUrl = "https://image.tmdb.org/t/p/w500/placeholder2.jpg",
            bannerUrl = "https://image.tmdb.org/t/p/w780/placeholder2.jpg",
            genre = listOf("Action", "Thriller"),
            releaseDate = "2026-02-12",
            rating = 8.5,
            popularityScore = 98.0,
            duration = "2h 55m",
            language = "Telugu",
            description = "Pushpa Raj faces his biggest challenge yet as new enemies rise from every corner.",
            certification = "UA",
            directors = listOf("Sukumar"),
            cast = listOf("Allu Arjun", "Rashmika Mandanna", "Fahadh Faasil"),
            trailerUrl = ""
        ),
        LocalMovie(
            movieId = "m3",
            title = "War 2",
            posterUrl = "https://image.tmdb.org/t/p/w500/placeholder3.jpg",
            bannerUrl = "https://image.tmdb.org/t/p/w780/placeholder3.jpg",
            genre = listOf("Action", "Spy"),
            releaseDate = "2026-02-18",
            rating = 7.8,
            popularityScore = 88.0,
            duration = "2h 20m",
            language = "Hindi",
            description = "Kabir returns from the shadows for one final mission that will determine the fate of the nation.",
            certification = "UA",
            directors = listOf("Aditya Chopra"),
            cast = listOf("Hrithik Roshan", "Jr NTR", "Kiara Advani"),
            trailerUrl = ""
        ),
        LocalMovie(
            movieId = "m4",
            title = "Stree 3",
            posterUrl = "https://image.tmdb.org/t/p/w500/placeholder4.jpg",
            bannerUrl = "https://image.tmdb.org/t/p/w780/placeholder4.jpg",
            genre = listOf("Comedy", "Horror"),
            releaseDate = "2026-02-16",
            rating = 7.5,
            popularityScore = 82.0,
            duration = "2h 10m",
            language = "Hindi",
            description = "The gang from Chanderi faces a new supernatural threat that's scarier and funnier than ever.",
            certification = "UA",
            directors = listOf("Amar Kaushik"),
            cast = listOf("Rajkummar Rao", "Shraddha Kapoor", "Pankaj Tripathi"),
            trailerUrl = ""
        ),
        LocalMovie(
            movieId = "m5",
            title = "Dhoom 4",
            posterUrl = "https://image.tmdb.org/t/p/w500/placeholder5.jpg",
            bannerUrl = "https://image.tmdb.org/t/p/w780/placeholder5.jpg",
            genre = listOf("Action", "Thriller"),
            releaseDate = "2026-01-25",
            rating = 7.2,
            popularityScore = 76.0,
            duration = "2h 25m",
            language = "Hindi",
            description = "The most stylish heist series returns with mind-bending action and an unpredictable villain.",
            certification = "UA",
            directors = listOf("Vijay Krishna Acharya"),
            cast = listOf("Ranveer Singh", "Alia Bhatt", "Abhishek Bachchan"),
            trailerUrl = ""
        ),
        LocalMovie(
            movieId = "m6",
            title = "KGF Chapter 3",
            posterUrl = "https://image.tmdb.org/t/p/w500/placeholder6.jpg",
            bannerUrl = "https://image.tmdb.org/t/p/w780/placeholder6.jpg",
            genre = listOf("Action", "Drama"),
            releaseDate = "2026-02-01",
            rating = 8.8,
            popularityScore = 97.0,
            duration = "3h 05m",
            language = "Kannada",
            description = "The legend of Rocky continues in the final chapter of the KGF saga.",
            certification = "UA",
            directors = listOf("Prashanth Neel"),
            cast = listOf("Yash", "Raveena Tandon", "Sanjay Dutt"),
            trailerUrl = ""
        ),
        LocalMovie(
            movieId = "m7",
            title = "Dream Girl 3",
            posterUrl = "https://image.tmdb.org/t/p/w500/placeholder7.jpg",
            bannerUrl = "https://image.tmdb.org/t/p/w780/placeholder7.jpg",
            genre = listOf("Comedy", "Romance"),
            releaseDate = "2026-02-19",
            rating = 6.8,
            popularityScore = 65.0,
            duration = "2h 05m",
            language = "Hindi",
            description = "Karam returns with a new avatar and a hilarious new identity crisis.",
            certification = "U",
            directors = listOf("Raaj Shaandilyaa"),
            cast = listOf("Ayushmann Khurrana", "Nushrratt Bharuccha"),
            trailerUrl = ""
        ),
        LocalMovie(
            movieId = "m8",
            title = "The Kerala Story 2",
            posterUrl = "https://image.tmdb.org/t/p/w500/placeholder8.jpg",
            bannerUrl = "https://image.tmdb.org/t/p/w780/placeholder8.jpg",
            genre = listOf("Drama", "Thriller"),
            releaseDate = "2026-01-10",
            rating = 7.0,
            popularityScore = 70.0,
            duration = "2h 30m",
            language = "Hindi",
            description = "A gripping sequel that explores new dimensions of courage and truth.",
            certification = "A",
            directors = listOf("Sudipto Sen"),
            cast = listOf("Adah Sharma"),
            trailerUrl = ""
        ),
        LocalMovie(
            movieId = "m9",
            title = "Bhool Bhulaiyaa 4",
            posterUrl = "https://image.tmdb.org/t/p/w500/placeholder9.jpg",
            bannerUrl = "https://image.tmdb.org/t/p/w780/placeholder9.jpg",
            genre = listOf("Comedy", "Horror"),
            releaseDate = "2026-02-15",
            rating = 7.3,
            popularityScore = 78.0,
            duration = "2h 15m",
            language = "Hindi",
            description = "Rooh Baba enters the most haunted mansion yet, with twice the comedy and scares.",
            certification = "UA",
            directors = listOf("Anees Bazmee"),
            cast = listOf("Kartik Aaryan", "Vidya Balan", "Triptii Dimri"),
            trailerUrl = ""
        ),
        LocalMovie(
            movieId = "m10",
            title = "RRR 2",
            posterUrl = "https://image.tmdb.org/t/p/w500/placeholder10.jpg",
            bannerUrl = "https://image.tmdb.org/t/p/w780/placeholder10.jpg",
            genre = listOf("Action", "Drama", "Historical"),
            releaseDate = "2026-02-17",
            rating = 9.0,
            popularityScore = 99.0,
            duration = "3h 10m",
            language = "Telugu",
            description = "The epic saga returns — fire and water unite again in a new chapter of Indian history.",
            certification = "UA",
            directors = listOf("S.S. Rajamouli"),
            cast = listOf("Ram Charan", "Jr NTR", "Alia Bhatt"),
            trailerUrl = ""
        ),
        LocalMovie(
            movieId = "m11",
            title = "Panchayat: The Movie",
            posterUrl = "https://image.tmdb.org/t/p/w500/placeholder11.jpg",
            bannerUrl = "https://image.tmdb.org/t/p/w780/placeholder11.jpg",
            genre = listOf("Comedy", "Drama"),
            releaseDate = "2026-02-10",
            rating = 8.6,
            popularityScore = 85.0,
            duration = "2h 20m",
            language = "Hindi",
            description = "Phulera comes alive on the big screen — Sachiv Ji faces his biggest panchayat crisis yet.",
            certification = "U",
            directors = listOf("Deepak Kumar Mishra"),
            cast = listOf("Jitendra Kumar", "Neena Gupta", "Raghubir Yadav"),
            trailerUrl = ""
        ),
        LocalMovie(
            movieId = "m12",
            title = "Pathaan 2",
            posterUrl = "https://image.tmdb.org/t/p/w500/placeholder12.jpg",
            bannerUrl = "https://image.tmdb.org/t/p/w780/placeholder12.jpg",
            genre = listOf("Action", "Spy"),
            releaseDate = "2026-01-20",
            rating = 7.9,
            popularityScore = 91.0,
            duration = "2h 35m",
            language = "Hindi",
            description = "Agent Pathaan embarks on a high-octane global mission to prevent a cyber catastrophe.",
            certification = "UA",
            directors = listOf("Siddharth Anand"),
            cast = listOf("Shah Rukh Khan", "John Abraham", "Deepika Padukone"),
            trailerUrl = ""
        )
    )

    // ─── SHOWTIMES ───────────────────────────────────────────

    private fun generateShowtimes(city: String, theatreIds: List<String>): Map<String, Map<String, Showtime>> {
        val timeSlots = listOf(
            listOf("10:00 AM", "1:30 PM", "5:00 PM", "8:30 PM"),
            listOf("11:00 AM", "2:30 PM", "6:00 PM", "9:30 PM"),
            listOf("10:30 AM", "2:00 PM", "5:30 PM", "9:00 PM"),
            listOf("12:00 PM", "3:30 PM", "7:00 PM", "10:00 PM")
        )

        val formats = listOf("2D", "3D", "IMAX", "4DX")
        val prices = listOf(250.0, 350.0, 500.0, 600.0)
        val today = "2026-02-19"
        val movieIds = movies.map { it.movieId }

        val result = mutableMapOf<String, Map<String, Showtime>>()

        theatreIds.forEachIndexed { theatreIndex, theatreId ->
            val movieMap = mutableMapOf<String, Showtime>()
            // Each theatre shows 4-6 random movies
            val moviesForTheatre = movieIds.shuffled().take(4 + theatreIndex % 3)

            moviesForTheatre.forEachIndexed { movieIndex, movieId ->
                val timeSlot = timeSlots[(theatreIndex + movieIndex) % timeSlots.size]
                val format = formats[theatreIndex % formats.size]
                val price = prices[theatreIndex % prices.size]

                movieMap[movieId] = Showtime(
                    movieId = movieId,
                    theatreId = theatreId,
                    city = city,
                    date = today,
                    times = timeSlot,
                    screenNumber = (movieIndex % 4) + 1,
                    price = price,
                    availableSeats = 50 + (movieIndex * 10),
                    totalSeats = 200,
                    format = format
                )
            }
            result[theatreId] = movieMap
        }

        return result
    }

    // ─── SEED ALL ────────────────────────────────────────────

    /**
     * Seed the entire database with dummy data.
     * Safe to call multiple times — overwrites existing data.
     */
    suspend fun seedAll(): Result<Unit> {
        return try {
            // 1. Seed cities
            val citiesRef = database.getReference("cities")
            cities.forEach { (name, city) ->
                citiesRef.child(name).setValue(city).await()
            }

            // 2. Seed theatres
            val theatresRef = database.getReference("theatres")
            theatres.forEach { (cityName, theatreList) ->
                theatreList.forEach { theatre ->
                    theatresRef.child(cityName).child(theatre.theatreId).setValue(theatre).await()
                }
            }

            // 3. Seed movies
            val moviesRef = database.getReference("movies")
            movies.forEach { movie ->
                moviesRef.child(movie.movieId).setValue(movie).await()
            }

            // 4. Seed showtimes
            val showtimesRef = database.getReference("showtimes")
            theatres.forEach { (cityName, theatreList) ->
                val cityShowtimes = generateShowtimes(cityName, theatreList.map { it.theatreId })
                cityShowtimes.forEach { (theatreId, movieShowtimes) ->
                    movieShowtimes.forEach { (movieId, showtime) ->
                        showtimesRef.child(cityName).child(theatreId).child(movieId)
                            .setValue(showtime).await()
                    }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Clear all seeded data from the database.
     */
    suspend fun clearAll(): Result<Unit> {
        return try {
            database.getReference("cities").removeValue().await()
            database.getReference("theatres").removeValue().await()
            database.getReference("movies").removeValue().await()
            database.getReference("showtimes").removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if data has been seeded by looking for the cities node.
     */
    suspend fun isSeeded(): Boolean {
        return try {
            val snapshot = suspendCancellableCoroutine { cont ->
                database.getReference("cities").get()
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resumeWithException(it) }
            }
            snapshot.exists() && snapshot.childrenCount > 0
        } catch (e: Exception) {
            false
        }
    }
}
