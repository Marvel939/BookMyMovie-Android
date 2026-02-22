package com.example.bookmymovie.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.bookmymovie.MainActivity
import com.example.bookmymovie.navigation.Screen
import com.example.bookmymovie.ui.theme.*
import com.example.bookmymovie.ui.viewmodel.BookingViewModel
import com.example.bookmymovie.ui.viewmodel.PLACES_API_KEY
import com.example.bookmymovie.ui.viewmodel.NearbyTheatresViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CinemaDetailScreen(navController: NavController, placeId: String?) {
    val context = LocalContext.current
    val nearbyTheatresViewModel: NearbyTheatresViewModel =
        viewModel(context as MainActivity)
    val bookingViewModel: BookingViewModel = viewModel(context as MainActivity)

    val theatre = remember(placeId) {
        nearbyTheatresViewModel.getTheatreByPlaceId(placeId ?: "")
    }

    // Kick off reviews fetch when screen opens
    LaunchedEffect(placeId) {
        if (!placeId.isNullOrEmpty()) {
            nearbyTheatresViewModel.fetchPlaceDetails(placeId)
        }
    }

    // Back navigation — always signals MovieDetailScreen to reopen the theatres sheet
    val goBack = {
        navController.previousBackStackEntry
            ?.savedStateHandle
            ?.set("show_theatres_sheet", true)
        navController.popBackStack()
    }
    BackHandler { goBack() }

    if (theatre == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepCharcoal),
            contentAlignment = Alignment.Center
        ) {
            Text("Cinema not found", color = TextSecondary)
        }
        return
    }

    val reviews = nearbyTheatresViewModel.placeReviewsMap[theatre.placeId]

    // Build exactly 5 images: cinema photos fill first slots,
    // remaining slots filled with street view at 4 different heading angles
    // (0=north, 90=east, 180=south, 270=west) — same look as Google Maps street view
    val streetViewHeadings = listOf(0, 90, 180, 270)
    val imageUrls: List<String> = buildList {
        addAll(theatre.photoUrls.take(4))
        if (theatre.lat != 0.0 && theatre.lng != 0.0) {
            val needed = (5 - size).coerceAtLeast(if (isEmpty()) 1 else 0)
            streetViewHeadings.take(needed).forEach { heading ->
                add(
                    "https://maps.googleapis.com/maps/api/streetview" +
                            "?size=640x480" +
                            "&location=${theatre.lat},${theatre.lng}" +
                            "&heading=$heading" +
                            "&pitch=10" +
                            "&fov=90" +
                            "&source=outdoor" +
                            "&key=$PLACES_API_KEY"
                )
            }
        }
    }.take(5)

    // Static Map URL for the Location section in the body
    val staticMapUrl = if (theatre.lat != 0.0 && theatre.lng != 0.0) {
        "https://maps.googleapis.com/maps/api/staticmap" +
                "?center=${theatre.lat},${theatre.lng}" +
                "&zoom=16&size=600x350&scale=2" +
                "&markers=color:red%7C${theatre.lat},${theatre.lng}" +
                "&key=$PLACES_API_KEY"
    } else null

    val pagerState = rememberPagerState(pageCount = { imageUrls.size.coerceAtLeast(1) })

    var isRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(nearbyTheatresViewModel.isLoadingReviews) {
        if (!nearbyTheatresViewModel.isLoadingReviews && isRefreshing) isRefreshing = false
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            if (!placeId.isNullOrEmpty()) {
                isRefreshing = true
                nearbyTheatresViewModel.placeReviewsMap.remove(placeId)
                nearbyTheatresViewModel.fetchPlaceDetails(placeId)
            }
        }
    ) {
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
            // ── TOP HERO: swipeable pager of cinema photos + street view ───────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                if (imageUrls.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(CardBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                } else {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        AsyncImage(
                            model = imageUrls[page],
                            contentDescription = "Cinema image ${page + 1}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(CardBackground)
                        )
                    }
                    // Dot indicators
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        repeat(imageUrls.size) { i ->
                            Box(
                                modifier = Modifier
                                    .size(
                                        width = if (pagerState.currentPage == i) 20.dp else 7.dp,
                                        height = 7.dp
                                    )
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        if (pagerState.currentPage == i)
                                            PrimaryAccent
                                        else
                                            Color.White.copy(alpha = 0.5f)
                                    )
                            )
                        }
                    }
                }
                // Gradient overlay at bottom
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(listOf(Color.Transparent, DeepCharcoal))
                        )
                )
                // Back button
                IconButton(
                    onClick = { goBack() },
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopStart)
                        .background(DeepCharcoal.copy(alpha = 0.6f), RoundedCornerShape(50))
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                // Share button
                IconButton(
                    onClick = {
                        val shareText = buildString {
                            append(theatre.name)
                            if (theatre.address.isNotEmpty()) append("\n${theatre.address}")
                            append("\nhttps://maps.google.com/?q=${Uri.encode(theatre.name)}" +
                                    "&ll=${theatre.lat},${theatre.lng}")
                        }
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(
                            Intent.createChooser(shareIntent, "Share Cinema Location")
                        )
                    },
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopEnd)
                        .background(DeepCharcoal.copy(alpha = 0.6f), RoundedCornerShape(50))
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = TextPrimary)
                }
            }

            // ── DETAILS ────────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp)
            ) {
                Text(
                    text = theatre.name,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(Modifier.height(8.dp))

                if (theatre.rating != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, null, tint = StarYellow, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            String.format("%.1f / 5.0", theatre.rating),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }

                if (theatre.address.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.LocationOn, null, tint = PrimaryAccent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(theatre.address, fontSize = 14.sp, color = TextSecondary, lineHeight = 20.sp)
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ── MAP IMAGE (where cinema photo used to be) ─────────────────
                if (staticMapUrl != null) {
                    Text("Location", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Spacer(Modifier.height(10.dp))
                    AsyncImage(
                        model = staticMapUrl,
                        contentDescription = "Map",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                    Spacer(Modifier.height(24.dp))
                }

                // ── OPEN IN MAPS ───────────────────────────────────────────────
                OutlinedButton(
                    onClick = {
                        val uri = Uri.parse("geo:${theatre.lat},${theatre.lng}?q=${Uri.encode(theatre.name)}")
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                            .apply { setPackage("com.google.android.apps.maps") }
                        if (intent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(intent)
                        } else {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(
                                    "https://maps.google.com/?q=${Uri.encode(theatre.name)}&ll=${theatre.lat},${theatre.lng}"
                                ))
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryAccent),
                    border = ButtonDefaults.outlinedButtonBorder
                ) {
                    Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Open in Google Maps", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(12.dp))

                // ── BOOK TICKETS ───────────────────────────────────────────────
                Button(
                    onClick = {
                        bookingViewModel.currentCinemaName = theatre.name
                        bookingViewModel.currentCinemaAddress = theatre.address
                        bookingViewModel.currentPlaceId = theatre.placeId
                        navController.navigate(Screen.ShowtimeSelection.createRoute(theatre.placeId))
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    Text("Book Tickets", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(Modifier.height(28.dp))

                // ── REVIEWS ────────────────────────────────────────────────────
                Text("Reviews", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(12.dp))

                when {
                    nearbyTheatresViewModel.isLoadingReviews && reviews == null -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = PrimaryAccent,
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 2.5.dp
                            )
                        }
                    }
                    reviews.isNullOrEmpty() -> {
                        Text("No reviews available.", fontSize = 14.sp, color = TextSecondary)
                    }
                    else -> {
                        reviews.forEach { review ->
                            ReviewCard(review = review)
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
    }
}

@Composable
private fun ReviewCard(review: com.example.bookmymovie.data.api.PlaceReview) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Profile photo
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(DividerColor),
                contentAlignment = Alignment.Center
            ) {
                if (!review.profilePhotoUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = review.profilePhotoUrl,
                        contentDescription = review.authorName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Default.Person, null, tint = TextSecondary, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(review.authorName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(review.relativeTime, fontSize = 12.sp, color = TextSecondary)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, null, tint = StarYellow, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(3.dp))
                Text("${review.rating}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        }
        if (review.text.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(review.text, fontSize = 13.sp, color = TextSecondary, lineHeight = 19.sp)
        }
    }
}
