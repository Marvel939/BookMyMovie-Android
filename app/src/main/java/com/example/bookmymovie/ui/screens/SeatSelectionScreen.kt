package com.example.bookmymovie.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.bookmymovie.model.SeatData
import com.example.bookmymovie.navigation.Screen
import com.example.bookmymovie.ui.theme.*
import com.example.bookmymovie.ui.viewmodel.BookingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeatSelectionScreen(
    navController: NavController,
    bookingViewModel: BookingViewModel
) {
    val showtime = bookingViewModel.selectedShowtime ?: return
    val safePlace    = bookingViewModel.currentPlaceId
    val safeScreen   = showtime.screenId
    val safeShowtime = showtime.showtimeId

    LaunchedEffect(safePlace, safeScreen, safeShowtime) {
        bookingViewModel.loadSeats(safePlace, safeScreen, safeShowtime)
    }

    // Group into a matrix
    val seatMatrix by remember(bookingViewModel.seats) {
        derivedStateOf {
            bookingViewModel.seats.values
                .groupBy { it.row }
                .entries
                .sortedBy { it.key }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Seats", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepCharcoal)
            )
        },
        bottomBar = {
            if (bookingViewModel.selectedSeats.isNotEmpty()) {
                Surface(color = CardBackground, shadowElevation = 8.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "${bookingViewModel.selectedSeats.size} seat(s) selected",
                                color = TextPrimary, fontWeight = FontWeight.SemiBold
                            )
                            Text("₹${bookingViewModel.seatAmount}", color = PrimaryAccent, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        Button(
                            onClick = {
                                navController.navigate(Screen.FoodBeverage.route)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Add Food", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        containerColor = DeepCharcoal
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp)
        ) {
            // ── Screen Label ─────────────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                        .background(DividerColor, RoundedCornerShape(8.dp))
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("SCREEN", color = TextSecondary, letterSpacing = 4.sp, fontSize = 11.sp)
                }
                Spacer(Modifier.height(24.dp))
            }

            // ── Seat Grid ────────────────────────────────────────────────────
            items(seatMatrix) { (row, seats) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Row label
                    Text(row, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(20.dp), textAlign = TextAlign.Center)
                    Spacer(Modifier.width(4.dp))
                    seats.sortedBy { it.col }.forEach { seat ->
                        SeatBox(
                            seat = seat,
                            isSelected = bookingViewModel.selectedSeats.contains(seat.seatId),
                            onClick = { if (!seat.booked) bookingViewModel.toggleSeat(seat.seatId) }
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
            }

            // ── Legend ───────────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(24.dp))
                HorizontalDivider(color = DividerColor)
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LegendItem(color = SilverSeat, label = "Silver ₹150")
                    LegendItem(color = GoldSeat,   label = "Gold ₹250")
                    LegendItem(color = PlatinumSeat, label = "Platinum ₹400")
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LegendItem(color = PrimaryAccent, label = "Selected")
                    LegendItem(color = Color(0xFF3A3A3A), label = "Booked")
                }
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun SeatBox(
    seat: SeatData,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = when {
        seat.booked -> Color(0xFF3A3A3A)
        isSelected  -> PrimaryAccent
        seat.type == "silver" -> SilverSeat
        seat.type == "gold"   -> GoldSeat
        seat.type == "platinum" -> PlatinumSeat
        else -> SilverSeat
    }
    Box(
        modifier = Modifier
            .padding(2.dp)
            .size(26.dp)
            .background(bgColor, RoundedCornerShape(5.dp))
            .border(
                1.dp,
                if (isSelected) Color.White.copy(alpha = 0.6f) else Color.Transparent,
                RoundedCornerShape(5.dp)
            )
            .clickable(enabled = !seat.booked) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(seat.col.toString(), color = Color.White, fontSize = 8.sp)
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(color, RoundedCornerShape(3.dp))
        )
        Text(label, color = TextSecondary, fontSize = 11.sp)
    }
}
