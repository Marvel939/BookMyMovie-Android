package com.example.bookmymovie.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.bookmymovie.model.LocalMovie
import com.example.bookmymovie.model.Theatre
import com.example.bookmymovie.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Location-aware home screen sections for the movie booking system.
 * These composables integrate into the existing HomeScreen.
 */

// ─── BANNER CAROUSEL ────────────────────────────────────────

/**
 * Auto-scrolling banner carousel showing top recommended movies.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MovieBannerCarousel(
    movies: List<LocalMovie>,
    onMovieClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (movies.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { movies.size })

    // Auto-scroll
    LaunchedEffect(pagerState) {
        while (true) {
            delay(4000)
            val nextPage = (pagerState.currentPage + 1) % movies.size
            pagerState.animateScrollToPage(nextPage)
        }
    }

    Column(modifier = modifier) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            pageSpacing = 12.dp,
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) { page ->
            val movie = movies[page]
            BannerCard(
                movie = movie,
                onClick = { onMovieClick(movie.movieId) }
            )
        }

        // Page indicators
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(movies.size) { index ->
                val selected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(if (selected) 10.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected) PrimaryAccent else TextSecondary.copy(alpha = 0.4f)
                        )
                )
            }
        }
    }
}

@Composable
private fun BannerCard(
    movie: LocalMovie,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Banner image
            AsyncImage(
                model = movie.bannerUrl.ifEmpty { movie.posterUrl },
                contentDescription = movie.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.8f)
                            ),
                            startY = 100f
                        )
                    )
            )

            // Movie info
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text(
                    movie.title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Rating
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            tint = StarYellow,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            String.format("%.1f", movie.rating),
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                    // Genre
                    Text(
                        movie.genre.take(2).joinToString(", "),
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )
                    // Duration
                    if (movie.duration.isNotEmpty()) {
                        Text(
                            "• ${movie.duration}",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // New Release badge
            if (movie.isNewRelease()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    color = PrimaryAccent,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "NEW",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

// ─── SECTION HEADER ─────────────────────────────────────────

@Composable
fun SectionHeader(
    title: String,
    icon: @Composable (() -> Unit)? = null,
    onSeeAllClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            icon?.invoke()
            Text(
                title,
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        if (onSeeAllClick != null) {
            TextButton(onClick = onSeeAllClick) {
                Text(
                    "See All",
                    color = PrimaryAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = PrimaryAccent,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ─── NOW SHOWING MOVIES ROW ─────────────────────────────────

@Composable
fun NowShowingRow(
    movies: List<LocalMovie>,
    onMovieClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (movies.isEmpty()) return

    Column(modifier = modifier) {
        SectionHeader(
            title = "Now Showing",
            icon = {
                Icon(
                    Icons.Filled.PlayCircle,
                    contentDescription = null,
                    tint = PrimaryAccent,
                    modifier = Modifier.size(22.dp)
                )
            }
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(movies) { movie ->
                LocalMovieCard(
                    movie = movie,
                    onClick = { onMovieClick(movie.movieId) }
                )
            }
        }
    }
}

// ─── NEW RELEASES ROW ───────────────────────────────────────

@Composable
fun NewReleasesRow(
    movies: List<LocalMovie>,
    onMovieClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (movies.isEmpty()) return

    Column(modifier = modifier) {
        SectionHeader(
            title = "New Releases",
            icon = {
                Icon(
                    Icons.Filled.NewReleases,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(22.dp)
                )
            }
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(movies) { movie ->
                LocalMovieCard(
                    movie = movie,
                    onClick = { onMovieClick(movie.movieId) },
                    showNewBadge = true
                )
            }
        }
    }
}

// ─── TRENDING ROW ───────────────────────────────────────────

@Composable
fun TrendingRow(
    movies: List<LocalMovie>,
    city: String,
    onMovieClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (movies.isEmpty()) return

    Column(modifier = modifier) {
        SectionHeader(
            title = "Trending in $city",
            icon = {
                Icon(
                    Icons.Filled.TrendingUp,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(22.dp)
                )
            }
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(movies) { movie ->
                LocalMovieCard(
                    movie = movie,
                    onClick = { onMovieClick(movie.movieId) }
                )
            }
        }
    }
}

// ─── RECOMMENDED ROW ────────────────────────────────────────

@Composable
fun RecommendedRow(
    movies: List<LocalMovie>,
    onMovieClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (movies.isEmpty()) return

    Column(modifier = modifier) {
        SectionHeader(
            title = "Recommended For You",
            icon = {
                Icon(
                    Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFF9C27B0),
                    modifier = Modifier.size(22.dp)
                )
            }
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(movies) { movie ->
                LocalMovieCard(
                    movie = movie,
                    onClick = { onMovieClick(movie.movieId) }
                )
            }
        }
    }
}

// ─── NEARBY THEATRES ROW ────────────────────────────────────

@Composable
fun NearbyTheatresRow(
    theatres: List<Pair<Theatre, Double>>,
    onTheatreClick: (String) -> Unit,
    onSeeAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (theatres.isEmpty()) return

    Column(modifier = modifier) {
        SectionHeader(
            title = "Theatres Near You",
            icon = {
                Icon(
                    Icons.Filled.TheaterComedy,
                    contentDescription = null,
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(22.dp)
                )
            },
            onSeeAllClick = onSeeAllClick
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(theatres.take(5)) { (theatre, distance) ->
                TheatreHorizontalCard(
                    theatre = theatre,
                    distance = distance,
                    onClick = { onTheatreClick(theatre.theatreId) }
                )
            }
        }
    }
}

// ─── MOVIE CARD ─────────────────────────────────────────────

@Composable
fun LocalMovieCard(
    movie: LocalMovie,
    onClick: () -> Unit,
    showNewBadge: Boolean = false,
    modifier: Modifier = Modifier
) {
    val interactionScale = remember { Animatable(1f) }

    Column(
        modifier = modifier
            .width(140.dp)
            .clickable(onClick = onClick)
            .graphicsLayer {
                scaleX = interactionScale.value
                scaleY = interactionScale.value
            }
    ) {
        Box {
            // Poster
            AsyncImage(
                model = movie.posterUrl,
                contentDescription = movie.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            // Rating badge
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        tint = StarYellow,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        String.format("%.1f", movie.rating),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // New badge
            if (showNewBadge && movie.isNewRelease()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    color = PrimaryAccent,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        "NEW",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Title
        Text(
            movie.title,
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Genre
        Text(
            movie.genre.take(2).joinToString(", "),
            color = TextSecondary,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ─── THEATRE HORIZONTAL CARD ────────────────────────────────

@Composable
private fun TheatreHorizontalCard(
    theatre: Theatre,
    distance: Double,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(220.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Theatre image
            if (theatre.imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = theatre.imageUrl,
                    contentDescription = theatre.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                theatre.name,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Distance
            if (distance < Double.MAX_VALUE) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.NearMe,
                        contentDescription = null,
                        tint = PrimaryAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (distance < 1.0) "${(distance * 1000).toInt()} m"
                        else String.format("%.1f km", distance),
                        color = PrimaryAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Rating
            if (theatre.rating > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        tint = StarYellow,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        String.format("%.1f", theatre.rating),
                        color = TextPrimary,
                        fontSize = 12.sp
                    )
                }
            }

            // Screens
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "${theatre.screens} Screen${if (theatre.screens > 1) "s" else ""}",
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}
