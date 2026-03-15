package com.example.bookmymovie.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.bookmymovie.model.Booking
import com.example.bookmymovie.ui.theme.*
import com.example.bookmymovie.ui.viewmodel.BookingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyBookingsScreen(
    navController: NavController,
    bookingViewModel: BookingViewModel
) {
    LaunchedEffect(Unit) { bookingViewModel.loadMyBookings() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Bookings", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepCharcoal)
            )
        },
        containerColor = DeepCharcoal
    ) { padding ->
        when {
            bookingViewModel.myBookings.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ConfirmationNumber, null, tint = TextSecondary, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("No bookings yet", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                        Text("Your booked tickets will appear here", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(bookingViewModel.myBookings) { booking ->
                        BookingHistoryCard(booking, bookingViewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingHistoryCard(
    booking: Booking,
    bookingViewModel: BookingViewModel
) {
    val context = LocalContext.current
    var showRefundDialog by remember { mutableStateOf(false) }

    if (showRefundDialog) {
        AlertDialog(
            onDismissRequest = { showRefundDialog = false },
            containerColor = CardBackground,
            title = { Text("Request Refund", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Amount Paid: ₹${booking.totalAmount}", color = TextPrimary, fontSize = 13.sp)
                    Text("Refundable (Seats + Food): ₹${booking.refundableAmount}", color = TextPrimary, fontSize = 13.sp)
                    Text("Non-refundable (GST + Convenience): ₹${booking.nonRefundableAmount}", color = TextSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Proceed with refund request?", color = TextSecondary, fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        bookingViewModel.requestRefund(booking.bookingId) { ok, msg ->
                            showRefundDialog = false
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !bookingViewModel.isRefunding,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
                ) {
                    if (bookingViewModel.isRefunding) {
                        CircularProgressIndicator(color = androidx.compose.ui.graphics.Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                    } else {
                        Text("Confirm")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showRefundDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CardBackground,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(14.dp)) {
            AsyncImage(
                model = "https://image.tmdb.org/t/p/w185${booking.moviePoster}",
                contentDescription = null,
                modifier = Modifier.width(68.dp).height(96.dp).clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(booking.movieName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(4.dp))
                Text(booking.cinemaName, color = TextSecondary, fontSize = 12.sp)
                Text("${bookingViewModel.formatDisplayDate(booking.date)} · ${booking.time}", color = TextSecondary, fontSize = 12.sp)
                Text("${booking.screenName} · ${booking.language}", color = TextSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Text("Seats: ${booking.seats.joinToString(", ")}", color = TextPrimary, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val statusColor = when (booking.status) {
                        "confirmed" -> androidx.compose.ui.graphics.Color(0xFF2ECC71)
                        "cancelled" -> PrimaryAccent
                        else -> TextSecondary
                    }
                    Text(
                        booking.status.replaceFirstChar { it.uppercase() },
                        color = statusColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                    Text("₹${booking.totalAmount}", color = PrimaryAccent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                if (booking.status == "confirmed" && booking.refundStatus != "succeeded") {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showRefundDialog = true },
                        enabled = !bookingViewModel.isRefunding,
                        border = ButtonDefaults.outlinedButtonBorder,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Request Refund", color = PrimaryAccent, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
