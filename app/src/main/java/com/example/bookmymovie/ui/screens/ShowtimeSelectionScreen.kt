package com.example.bookmymovie.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.bookmymovie.model.CinemaShowtime
import com.example.bookmymovie.navigation.Screen
import com.example.bookmymovie.ui.theme.*
import com.example.bookmymovie.ui.viewmodel.BookingViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowtimeSelectionScreen(
    navController: NavController,
    placeId: String?,
    bookingViewModel: BookingViewModel
) {
    val safePlace = placeId ?: return
    val safeCinema = bookingViewModel.currentCinemaName

    // Generate next 7 days
    val dates = remember {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        (0..6).map { offset ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, offset)
            sdf.format(cal.time)
        }
    }
    val displaySdf = remember { SimpleDateFormat("EEE\ndd MMM", Locale.getDefault()) }
    var selectedDate by remember { mutableStateOf(dates[0]) }

    LaunchedEffect(safePlace, selectedDate) {
        bookingViewModel.loadShowtimes(safePlace, selectedDate)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Select Showtime", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 17.sp)
                        Text(safeCinema, color = TextSecondary, fontSize = 12.sp)
                    }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Date Picker Row ──────────────────────────────────────────────
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(dates) { date ->
                    val isSelected = date == selectedDate
                    val displayDate = remember(date) {
                        val sdf2 = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val d = sdf2.parse(date)!!
                        displaySdf.format(d)
                    }
                    Column(
                        modifier = Modifier
                            .width(62.dp)
                            .height(64.dp)
                            .background(
                                if (isSelected) PrimaryAccent else CardBackground,
                                RoundedCornerShape(12.dp)
                            )
                            .border(
                                1.dp,
                                if (isSelected) PrimaryAccent else DividerColor,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedDate = date },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = displayDate,
                            color = if (isSelected) Color.White else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            HorizontalDivider(color = DividerColor)
            Spacer(Modifier.height(8.dp))

            // ── Showtimes List ───────────────────────────────────────────────
            when {
                bookingViewModel.isLoadingShowtimes -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryAccent)
                    }
                }
                bookingViewModel.showtimes.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Event, null, tint = TextSecondary, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("No showtimes available", color = TextSecondary, fontSize = 15.sp)
                            Text("for ${bookingViewModel.formatDisplayDate(selectedDate)}", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(bookingViewModel.showtimes) { showtime ->
                            ShowtimeCard(
                                showtime = showtime,
                                movieName = showtime.movieName,
                                onClick = {
                                    bookingViewModel.selectedShowtime = showtime
                                    bookingViewModel.currentPlaceId = safePlace
                                    navController.navigate(Screen.SeatSelection.route)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShowtimeCard(
    showtime: CinemaShowtime,
    movieName: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CardBackground,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Time box
            Box(
                modifier = Modifier
                    .background(PrimaryAccent.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(showtime.time, color = PrimaryAccent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(movieName, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(showtime.screenName)
                    Chip(showtime.screenType)
                    Chip(showtime.language)
                }
            }
            Text("₹150+", color = TextSecondary, fontSize = 13.sp)
        }
    }
}

@Composable
private fun Chip(text: String) {
    Box(
        modifier = Modifier
            .background(DividerColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text, color = TextSecondary, fontSize = 11.sp)
    }
}
