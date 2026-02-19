package com.example.bookmymovie.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.bookmymovie.navigation.Screen
import com.example.bookmymovie.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class UserReview(
    val reviewId: String = "",
    val movieId: String = "",
    val movieTitle: String = "",
    val moviePosterUrl: String = "",
    val movieBannerUrl: String = "",
    val movieRating: String = "",
    val movieLanguage: String = "",
    val movieGenre: String = "",
    val movieDuration: String = "",
    val movieDescription: String = "",
    val movieReleaseDate: String = "",
    val author: String = "",
    val content: String = "",
    val rating: Double = 0.0,
    val tags: List<String> = emptyList(),
    val reviewDate: String = "",
    val timestamp: Long = 0L
)

fun getUserReviewsFlow(): Flow<List<UserReview>> = callbackFlow {
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val db = FirebaseDatabase.getInstance().getReference("reviews")

    val listener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val reviews = mutableListOf<UserReview>()
            for (movieSnap in snapshot.children) {
                val movieId = movieSnap.key ?: continue
                for (reviewSnap in movieSnap.children) {
                    val reviewUserId = reviewSnap.child("userId").getValue(String::class.java) ?: ""
                    if (reviewUserId == userId) {
                        val tagsList = mutableListOf<String>()
                        reviewSnap.child("tags").children.forEach { tagSnap ->
                            tagSnap.getValue(String::class.java)?.let { tagsList.add(it) }
                        }
                        val review = UserReview(
                            reviewId = reviewSnap.key ?: "",
                            movieId = movieId,
                            movieTitle = reviewSnap.child("movieTitle").getValue(String::class.java) ?: "",
                            moviePosterUrl = reviewSnap.child("moviePosterUrl").getValue(String::class.java) ?: "",
                            movieBannerUrl = reviewSnap.child("movieBannerUrl").getValue(String::class.java) ?: "",
                            movieRating = reviewSnap.child("movieRating").getValue(String::class.java) ?: "",
                            movieLanguage = reviewSnap.child("movieLanguage").getValue(String::class.java) ?: "",
                            movieGenre = reviewSnap.child("movieGenre").getValue(String::class.java) ?: "",
                            movieDuration = reviewSnap.child("movieDuration").getValue(String::class.java) ?: "",
                            movieDescription = reviewSnap.child("movieDescription").getValue(String::class.java) ?: "",
                            movieReleaseDate = reviewSnap.child("movieReleaseDate").getValue(String::class.java) ?: "",
                            author = reviewSnap.child("author").getValue(String::class.java) ?: "",
                            content = reviewSnap.child("content").getValue(String::class.java) ?: "",
                            rating = reviewSnap.child("rating").getValue(Double::class.java) ?: 0.0,
                            tags = tagsList,
                            reviewDate = reviewSnap.child("reviewDate").getValue(String::class.java) ?: "",
                            timestamp = reviewSnap.child("timestamp").getValue(Long::class.java) ?: 0L
                        )
                        reviews.add(review)
                    }
                }
            }
            reviews.sortByDescending { it.timestamp }
            trySend(reviews)
        }

        override fun onCancelled(error: DatabaseError) {
            close(error.toException())
        }
    }
    db.addValueEventListener(listener)
    awaitClose { db.removeEventListener(listener) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyReviewsScreen(navController: NavController) {
    val reviews by getUserReviewsFlow().collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "My Reviews",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepCharcoal)
            )
        },
        containerColor = DeepCharcoal
    ) { padding ->
        if (reviews.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.RateReview,
                        contentDescription = null,
                        tint = TextSecondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No reviews yet",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Watch a movie and share your thoughts\nby adding a review!",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(reviews) { review ->
                    UserReviewCard(
                        review = review,
                        onClick = {
                            navController.navigate(Screen.MovieDetail.createRoute(review.movieId))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun UserReviewCard(
    review: UserReview,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = CardBackground,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                if (review.moviePosterUrl.isNotBlank()) {
                    AsyncImage(
                        model = review.moviePosterUrl,
                        contentDescription = review.movieTitle,
                        modifier = Modifier
                            .width(80.dp)
                            .height(120.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = review.movieTitle.ifBlank { "Movie" },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    // Movie info row
                    val infoItems = listOfNotNull(
                        review.movieReleaseDate.takeIf { it.isNotBlank() },
                        review.movieLanguage.takeIf { it.isNotBlank() },
                        review.movieDuration.takeIf { it.isNotBlank() }
                    )
                    if (infoItems.isNotEmpty()) {
                        Text(
                            text = infoItems.joinToString(" • "),
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                    if (review.movieGenre.isNotBlank()) {
                        Text(
                            text = review.movieGenre,
                            fontSize = 11.sp,
                            color = TextSecondary.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    // User's rating
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = StarYellow,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Your Rating: ",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = String.format("%.0f", review.rating),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "/10",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        if (review.movieRating.isNotBlank()) {
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "TMDB: ${review.movieRating}",
                                fontSize = 11.sp,
                                color = TextSecondary.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            // Review date/time
            val displayDateTime = if (review.reviewDate.isNotBlank()) {
                val timeStr = if (review.timestamp > 0L) {
                    SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(review.timestamp))
                } else ""
                if (timeStr.isNotBlank()) "${review.reviewDate} at $timeStr" else review.reviewDate
            } else if (review.timestamp > 0L) {
                SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault()).format(Date(review.timestamp))
            } else ""
            if (displayDateTime.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        tint = TextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = displayDateTime,
                        fontSize = 11.sp,
                        color = TextSecondary.copy(alpha = 0.6f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            // Review content
            Text(
                text = review.content,
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 19.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
            // Tags
            if (review.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    review.tags.forEach { tag ->
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
}
