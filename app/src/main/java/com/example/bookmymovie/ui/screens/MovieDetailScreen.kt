package com.example.bookmymovie.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.bookmymovie.data.repository.MovieRepository
import com.example.bookmymovie.data.repository.WishlistManager
import com.example.bookmymovie.navigation.Screen
import com.example.bookmymovie.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bookmymovie.MainActivity
import com.example.bookmymovie.ui.viewmodel.NearbyTheatresViewModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun MovieDetailScreen(navController: NavController, movieId: String?) {
    var movie by remember { mutableStateOf<Movie?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showWishlistDialog by remember { mutableStateOf(false) }
    var showAddReviewDialog by remember { mutableStateOf(false) }
    var showTheatresSheet by remember { mutableStateOf(false) }
    // Reopen theatres sheet automatically when returning from CinemaDetailScreen
    val _sheetSsh = navController.currentBackStackEntry?.savedStateHandle
    val _showSheetResult by (_sheetSsh
        ?.getStateFlow("show_theatres_sheet", false)
        ?.collectAsState() ?: remember { mutableStateOf(false) })
    LaunchedEffect(_showSheetResult) {
        if (_showSheetResult) {
            showTheatresSheet = true
            _sheetSsh?.set("show_theatres_sheet", false)
        }
    }
    // Scoped to the Activity so it shares data fetched in MainActivity
    val nearbyTheatresViewModel: NearbyTheatresViewModel =
        viewModel(LocalContext.current as MainActivity)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Wishlist state
    val wishlists by WishlistManager.getWishlistsFlow().collectAsState(initial = emptyMap())
    val movieInWishlists by WishlistManager.isMovieInAnyWishlistFlow(movieId ?: "")
        .collectAsState(initial = emptySet())
    val isInAnyWishlist = movieInWishlists.isNotEmpty()

    // User's own review for this movie
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val userReview by remember(movieId, currentUserId) {
        callbackFlow<Map<String, Any?>?> {
            if (movieId == null || currentUserId == null) {
                trySend(null)
                awaitClose { }
            } else {
                val ref = FirebaseDatabase.getInstance()
                    .getReference("reviews").child(movieId)
                val listener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        var found: Map<String, Any?>? = null
                        for (child in snapshot.children) {
                            val uid = child.child("userId").getValue(String::class.java)
                            if (uid == currentUserId) {
                                val tagsList = mutableListOf<String>()
                                child.child("tags").children.forEach { tagSnap ->
                                    tagSnap.getValue(String::class.java)?.let { tagsList.add(it) }
                                }
                                found = mapOf(
                                    "author" to (child.child("author").getValue(String::class.java) ?: ""),
                                    "content" to (child.child("content").getValue(String::class.java) ?: ""),
                                    "rating" to (child.child("rating").getValue(Double::class.java)),
                                    "tags" to tagsList,
                                    "reviewDate" to (child.child("reviewDate").getValue(String::class.java) ?: ""),
                                    "timestamp" to (child.child("timestamp").getValue(Long::class.java) ?: 0L)
                                )
                                break
                            }
                        }
                        trySend(found)
                    }
                    override fun onCancelled(error: DatabaseError) {
                        close(error.toException())
                    }
                }
                ref.addValueEventListener(listener)
                awaitClose { ref.removeEventListener(listener) }
            }
        }
    }.collectAsState(initial = null)

    // All Firebase reviews for this movie (from all users)
    val allFirebaseReviews by remember(movieId) {
        callbackFlow<List<Map<String, Any?>>> {
            if (movieId == null) {
                trySend(emptyList())
                awaitClose { }
            } else {
                val ref = FirebaseDatabase.getInstance()
                    .getReference("reviews").child(movieId)
                val listener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val reviewsList = mutableListOf<Map<String, Any?>>()
                        for (child in snapshot.children) {
                            val tagsList = mutableListOf<String>()
                            child.child("tags").children.forEach { tagSnap ->
                                tagSnap.getValue(String::class.java)?.let { tagsList.add(it) }
                            }
                            val review = mapOf(
                                "author" to (child.child("author").getValue(String::class.java) ?: ""),
                                "content" to (child.child("content").getValue(String::class.java) ?: ""),
                                "rating" to child.child("rating").getValue(Double::class.java),
                                "userId" to (child.child("userId").getValue(String::class.java) ?: ""),
                                "tags" to tagsList,
                                "reviewDate" to (child.child("reviewDate").getValue(String::class.java) ?: ""),
                                "timestamp" to (child.child("timestamp").getValue(Long::class.java) ?: 0L)
                            )
                            reviewsList.add(review)
                        }
                        // Sort by timestamp descending
                        reviewsList.sortByDescending { (it["timestamp"] as? Long) ?: 0L }
                        trySend(reviewsList)
                    }
                    override fun onCancelled(error: DatabaseError) {
                        close(error.toException())
                    }
                }
                ref.addValueEventListener(listener)
                awaitClose { ref.removeEventListener(listener) }
            }
        }
    }.collectAsState(initial = emptyList())

    LaunchedEffect(movieId) {
        isLoading = true
        val id = movieId?.toIntOrNull()
        if (id != null) {
            movie = MovieRepository.fetchMovieDetail(id)
        }
        isLoading = false
    }

    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepCharcoal),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = PrimaryAccent)
        }
        return
    }

    val currentMovie = movie ?: run {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepCharcoal),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Movie not found", color = TextSecondary, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { navController.popBackStack() },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("Go Back")
                }
            }
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepCharcoal)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Hero poster section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(480.dp)
            ) {
                AsyncImage(
                    model = currentMovie.posterUrl,
                    contentDescription = currentMovie.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Top gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    DeepCharcoal.copy(alpha = 0.7f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                // Bottom gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    DeepCharcoal
                                )
                            )
                        )
                )
                // Back button
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .padding(top = 40.dp, start = 8.dp)
                        .align(Alignment.TopStart)
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                // Wishlist icon
                IconButton(
                    onClick = { showWishlistDialog = true },
                    modifier = Modifier
                        .padding(top = 40.dp, end = 8.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Icon(
                        if (isInAnyWishlist) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Wishlist",
                        tint = if (isInAnyWishlist) PrimaryAccent else TextPrimary
                    )
                }
            }

            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .offset(y = (-50).dp)
            ) {
                Text(
                    text = currentMovie.title,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Rating and language row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CardBackground
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = StarYellow,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = currentMovie.rating,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = currentMovie.language,
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Genre and duration chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    currentMovie.genre.split(",").forEach { genre ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = CardBackground,
                            contentColor = TextPrimary
                        ) {
                            Text(
                                text = genre.trim(),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = CardBackground,
                        contentColor = TextSecondary
                    ) {
                        Text(
                            text = currentMovie.duration,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(DividerColor)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Synopsis",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = currentMovie.description,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(28.dp))

                if (currentMovie.directors.isNotEmpty()) {
                    DetailSection("Directors", currentMovie.directors.joinToString(", "))
                    Spacer(modifier = Modifier.height(20.dp))
                }

                if (currentMovie.cast.isNotEmpty()) {
                    Text(
                        text = "Cast",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        currentMovie.cast.forEach { member ->
                            PersonCard(
                                name = member.name,
                                subtitle = member.character,
                                imageUrl = member.imageUrl,
                                onClick = {
                                    navController.navigate(Screen.PersonDetail.createRoute(member.id))
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                if (currentMovie.crew.isNotEmpty()) {
                    Text(
                        text = "Crew",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        currentMovie.crew.forEach { member ->
                            PersonCard(
                                name = member.name,
                                subtitle = member.job,
                                imageUrl = member.imageUrl,
                                onClick = {
                                    navController.navigate(Screen.PersonDetail.createRoute(member.id))
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                if (currentMovie.reviews.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(DividerColor)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Reviews",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    currentMovie.reviews.take(5).forEach { review ->
                        ReviewCard(review)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // User's own review for this movie
                if (userReview != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(DividerColor)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Your Review",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ReviewCard(
                        Review(
                            author = userReview!!["author"] as? String ?: "",
                            content = userReview!!["content"] as? String ?: "",
                            rating = userReview!!["rating"] as? Double,
                            avatarUrl = null
                        )
                    )
                    // Show review date/time
                    val reviewDate = userReview!!["reviewDate"] as? String ?: ""
                    val reviewTimestamp = userReview!!["timestamp"] as? Long ?: 0L
                    val displayDateTime = if (reviewDate.isNotBlank()) {
                        val timeStr = if (reviewTimestamp > 0L) {
                            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(reviewTimestamp))
                        } else ""
                        if (timeStr.isNotBlank()) "$reviewDate at $timeStr" else reviewDate
                    } else if (reviewTimestamp > 0L) {
                        SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault()).format(Date(reviewTimestamp))
                    } else ""
                    if (displayDateTime.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = null,
                                tint = TextSecondary.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = displayDateTime,
                                fontSize = 12.sp,
                                color = TextSecondary.copy(alpha = 0.7f)
                            )
                        }
                    }
                    // Show tags if present
                    @Suppress("UNCHECKED_CAST")
                    val reviewTags = userReview!!["tags"] as? List<String> ?: emptyList()
                    if (reviewTags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            reviewTags.forEach { tag ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = PrimaryAccent.copy(alpha = 0.15f),
                                    border = ButtonDefaults.outlinedButtonBorder.copy(
                                        brush = Brush.horizontalGradient(listOf(PrimaryAccent.copy(alpha = 0.4f), PrimaryAccent.copy(alpha = 0.4f)))
                                    )
                                ) {
                                    Text(
                                        text = tag,
                                        fontSize = 11.sp,
                                        color = PrimaryAccent,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // All user reviews from Firebase (excluding current user)
                val otherUserReviews = allFirebaseReviews.filter {
                    (it["userId"] as? String) != currentUserId
                }
                if (otherUserReviews.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(DividerColor)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "User Reviews (${otherUserReviews.size})",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    otherUserReviews.forEach { fbReview ->
                        val author = fbReview["author"] as? String ?: "User"
                        val content = fbReview["content"] as? String ?: ""
                        val rating = fbReview["rating"] as? Double
                        val fbDate = fbReview["reviewDate"] as? String ?: ""
                        val fbTimestamp = fbReview["timestamp"] as? Long ?: 0L
                        @Suppress("UNCHECKED_CAST")
                        val fbTags = fbReview["tags"] as? List<String> ?: emptyList()

                        // Review card
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = CardBackground,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                // Author and rating row
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Author avatar
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(PrimaryAccent.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = author.firstOrNull()?.uppercase() ?: "U",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryAccent
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = author,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary
                                        )
                                        // Date/time
                                        val fbDisplayDateTime = if (fbDate.isNotBlank()) {
                                            val fbTimeStr = if (fbTimestamp > 0L) {
                                                SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(fbTimestamp))
                                            } else ""
                                            if (fbTimeStr.isNotBlank()) "$fbDate at $fbTimeStr" else fbDate
                                        } else if (fbTimestamp > 0L) {
                                            SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault()).format(Date(fbTimestamp))
                                        } else ""
                                        if (fbDisplayDateTime.isNotBlank()) {
                                            Text(
                                                text = fbDisplayDateTime,
                                                fontSize = 11.sp,
                                                color = TextSecondary.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                    if (rating != null) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Star,
                                                contentDescription = null,
                                                tint = StarYellow,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = String.format("%.0f", rating),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = "/10",
                                                fontSize = 11.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                // Content
                                Text(
                                    text = content,
                                    fontSize = 13.sp,
                                    color = TextSecondary,
                                    lineHeight = 19.sp
                                )
                                // Tags
                                if (fbTags.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        fbTags.forEach { tag ->
                                            Surface(
                                                shape = RoundedCornerShape(16.dp),
                                                color = PrimaryAccent.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = tag,
                                                    fontSize = 11.sp,
                                                    color = PrimaryAccent,
                                                    fontWeight = FontWeight.Medium,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // Add Review button
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showAddReviewDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryAccent),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.horizontalGradient(listOf(PrimaryAccent, PrimaryAccent))
                    )
                ) {
                    Icon(
                        Icons.Default.RateReview,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Add Review",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(100.dp))
            }
        }

        // Book Tickets button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            DeepCharcoal
                        )
                    )
                )
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Button(
                onClick = { showTheatresSheet = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Text(
                    "Book Tickets",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }

    // Wishlist Dialog
    if (showWishlistDialog) {
        WishlistDialog(
            wishlists = wishlists,
            movieInWishlists = movieInWishlists,
            onDismiss = { showWishlistDialog = false },
            onCreateWishlist = { name ->
                scope.launch {
                    WishlistManager.createWishlist(name)
                }
            },
            onToggleWishlist = { wishlistId, isAdded ->
                scope.launch {
                    val m = movie ?: return@launch
                    if (isAdded) {
                        WishlistManager.removeMovieFromWishlist(wishlistId, m.id)
                    } else {
                        WishlistManager.addMovieToWishlist(
                            wishlistId,
                            com.example.bookmymovie.data.repository.WishlistItem(
                                movieId = m.id,
                                title = m.title,
                                posterUrl = m.posterUrl,
                                bannerUrl = m.bannerUrl,
                                rating = m.rating,
                                language = m.language,
                                genre = m.genre,
                                duration = m.duration,
                                description = m.description,
                                releaseDate = m.releaseDate
                            )
                        )
                        Toast.makeText(context, "Saved to wishlist!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // Add Review Dialog
    if (showAddReviewDialog) {
        AddReviewDialog(
            onDismiss = { showAddReviewDialog = false },
            onSubmit = { reviewText, rating, reviewTags, reviewDate ->
                scope.launch {
                    val m = movie ?: return@launch
                    try {
                        val userId = FirebaseAuth.getInstance()
                            .currentUser?.uid ?: return@launch
                        val userRef = FirebaseDatabase.getInstance()
                            .getReference("users").child(userId)
                        val userSnap = userRef.get().await()
                        val firstName = userSnap.child("firstName").getValue(String::class.java) ?: "User"
                        val reviewRef = FirebaseDatabase.getInstance()
                            .getReference("reviews").child(m.id).push()
                        val reviewData = mapOf(
                            "author" to firstName,
                            "content" to reviewText,
                            "rating" to rating,
                            "userId" to userId,
                            "movieId" to m.id,
                            "movieTitle" to m.title,
                            "moviePosterUrl" to m.posterUrl,
                            "movieBannerUrl" to m.bannerUrl,
                            "movieRating" to m.rating,
                            "movieLanguage" to m.language,
                            "movieGenre" to m.genre,
                            "movieDuration" to m.duration,
                            "movieDescription" to m.description,
                            "movieReleaseDate" to m.releaseDate,
                            "tags" to reviewTags,
                            "reviewDate" to reviewDate,
                            "timestamp" to com.google.firebase.database.ServerValue.TIMESTAMP
                        )
                        reviewRef.setValue(reviewData).await()
                        Toast.makeText(context, "Review submitted!", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Failed to submit review", Toast.LENGTH_SHORT).show()
                    }
                }
                showAddReviewDialog = false
            }
        )
    }

    if (showTheatresSheet) {
        NearbyTheatresBottomSheet(
            viewModel = nearbyTheatresViewModel,
            onDismiss = { showTheatresSheet = false },
            onTheatreClick = { placeId ->
                showTheatresSheet = false
                navController.navigate(Screen.CinemaDetail.createRoute(placeId))
            }
        )
    }
}

@Composable
private fun DetailSection(title: String, content: String) {
    Column {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = content,
            fontSize = 14.sp,
            color = TextSecondary,
            modifier = Modifier.padding(top = 6.dp),
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun PersonCard(
    name: String,
    subtitle: String,
    imageUrl: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            .clickable(onClick = onClick)
    ) {
        if (imageUrl.isNotEmpty()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = name,
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(CardBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = name,
                    tint = TextSecondary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = name,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        if (subtitle.isNotEmpty()) {
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ReviewCard(review: Review) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CardBackground,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (review.avatarUrl != null && review.avatarUrl.isNotEmpty()) {
                    AsyncImage(
                        model = review.avatarUrl,
                        contentDescription = review.author,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(DividerColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = review.author.take(1).uppercase(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = review.author,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                if (review.rating != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = StarYellow,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = String.format("%.0f", review.rating),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = review.content,
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 19.sp,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun WishlistDialog(
    wishlists: Map<String, com.example.bookmymovie.data.repository.Wishlist>,
    movieInWishlists: Set<String>,
    onDismiss: () -> Unit,
    onCreateWishlist: (String) -> Unit,
    onToggleWishlist: (String, Boolean) -> Unit
) {
    var showCreateField by remember { mutableStateOf(false) }
    var newWishlistName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Save to Wishlist",
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column {
                if (wishlists.isEmpty() && !showCreateField) {
                    Text(
                        "No wishlists yet. Create one to get started!",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                wishlists.forEach { (id, wishlist) ->
                    val isInThis = movieInWishlists.contains(id)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isInThis) PrimaryAccent.copy(alpha = 0.15f) else Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleWishlist(id, isInThis) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (isInThis) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = null,
                                tint = if (isInThis) PrimaryAccent else TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = wishlist.name,
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${wishlist.movies.size} movies",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (showCreateField) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newWishlistName,
                        onValueChange = { newWishlistName = it },
                        label = { Text("Wishlist name", color = TextSecondary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryAccent,
                            unfocusedBorderColor = DividerColor,
                            cursorColor = PrimaryAccent,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        TextButton(onClick = {
                            showCreateField = false
                            newWishlistName = ""
                        }) {
                            Text("Cancel", color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Button(
                            onClick = {
                                if (newWishlistName.isNotBlank()) {
                                    onCreateWishlist(newWishlistName.trim())
                                    newWishlistName = ""
                                    showCreateField = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Create", color = Color.White)
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showCreateField = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryAccent),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.horizontalGradient(listOf(PrimaryAccent, PrimaryAccent))
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create New Wishlist", fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = PrimaryAccent, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = CardBackground,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun AddReviewDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, Double, List<String>, String) -> Unit
) {
    var reviewText by remember { mutableStateOf("") }
    var selectedRating by remember { mutableStateOf(0) }
    var tagInput by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf<List<String>>(emptyList()) }
    val calendar = remember { Calendar.getInstance() }
    var selectedDate by remember {
        mutableStateOf(
            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(calendar.time)
        )
    }
    val context = LocalContext.current

    val suggestedTags = listOf(
        "Must Watch", "Masterpiece", "Great Acting", "Visually Stunning",
        "Good Story", "Emotional", "Funny", "Thrilling", "Boring",
        "Overrated", "Underrated", "Family Friendly", "Mind Blowing"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add Review", fontWeight = FontWeight.Bold, color = TextPrimary)
        },
        text = {
            Column {
                // Date picker
                Text("Review Date", color = TextSecondary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(
                    onClick = {
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                calendar.set(year, month, dayOfMonth)
                                selectedDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                                    .format(calendar.time)
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.horizontalGradient(listOf(DividerColor, DividerColor))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = PrimaryAccent
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = selectedDate,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Rating", color = TextSecondary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (i in 1..10) {
                        IconButton(
                            onClick = { selectedRating = i },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "Star $i",
                                tint = if (i <= selectedRating) StarYellow else DividerColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = reviewText,
                    onValueChange = { reviewText = it },
                    label = { Text("Write your review...", color = TextSecondary) },
                    minLines = 3,
                    maxLines = 6,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryAccent,
                        unfocusedBorderColor = DividerColor,
                        cursorColor = PrimaryAccent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Tags", color = TextSecondary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                // Selected tags
                if (tags.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tags.forEach { tag ->
                            FilterChip(
                                selected = true,
                                onClick = { tags = tags - tag },
                                label = { Text(tag, fontSize = 12.sp) },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove",
                                        modifier = Modifier.size(14.dp),
                                        tint = TextPrimary
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryAccent.copy(alpha = 0.2f),
                                    selectedLabelColor = TextPrimary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    selectedBorderColor = PrimaryAccent,
                                    enabled = true,
                                    selected = true
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                // Custom tag input
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = tagInput,
                        onValueChange = { tagInput = it },
                        label = { Text("Add custom tag", color = TextSecondary, fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryAccent,
                            unfocusedBorderColor = DividerColor,
                            cursorColor = PrimaryAccent,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.weight(1f),
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            val trimmed = tagInput.trim()
                            if (trimmed.isNotBlank() && trimmed !in tags) {
                                tags = tags + trimmed
                                tagInput = ""
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add tag", tint = PrimaryAccent)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                // Suggested tags
                Text("Suggested", color = TextSecondary.copy(alpha = 0.6f), fontSize = 11.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    suggestedTags.filter { it !in tags }.forEach { tag ->
                        FilterChip(
                            selected = false,
                            onClick = { tags = tags + tag },
                            label = { Text(tag, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = DividerColor.copy(alpha = 0.3f),
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = DividerColor,
                                enabled = true,
                                selected = false
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (reviewText.isNotBlank() && selectedRating > 0) {
                        onSubmit(reviewText.trim(), selectedRating.toDouble(), tags, selectedDate)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                shape = RoundedCornerShape(12.dp),
                enabled = reviewText.isNotBlank() && selectedRating > 0
            ) {
                Text("Submit", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = CardBackground,
        shape = RoundedCornerShape(20.dp)
    )
}
