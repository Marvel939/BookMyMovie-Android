package com.example.bookmymovie.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.bookmymovie.navigation.Screen
import com.example.bookmymovie.ui.theme.*
import com.example.bookmymovie.ui.viewmodel.BookingViewModel
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.rememberPaymentSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingSummaryScreen(
    navController: NavController,
    bookingViewModel: BookingViewModel
) {
    val showtime = bookingViewModel.selectedShowtime
    val selectedSeatIds = bookingViewModel.selectedSeats
    val selectedSeats   = bookingViewModel.seats.values.filter { it.seatId in selectedSeatIds }
    val cartItems       = bookingViewModel.cartItems
    var paymentSheetError by remember { mutableStateOf<String?>(null) }

    val paymentSheet = rememberPaymentSheet { result ->
        when (result) {
            is PaymentSheetResult.Completed -> {
                paymentSheetError = null
                showtime?.let {
                    bookingViewModel.confirmBooking(it.movieId, it.movieName, it.moviePoster)
                }
            }
            is PaymentSheetResult.Canceled -> { /* user closed sheet — do nothing */ }
            is PaymentSheetResult.Failed -> {
                paymentSheetError = result.error.message ?: "Payment failed"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Order Summary", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepCharcoal)
            )
        },
        bottomBar = {
            val errorMsg = bookingViewModel.bookingError
            Column {
                if (!errorMsg.isNullOrEmpty()) {
                    Text(errorMsg, color = PrimaryAccent, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                }
                if (!paymentSheetError.isNullOrEmpty()) {
                    Text(paymentSheetError!!, color = Color(0xFFF44336), fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp))
                }
                Surface(color = CardBackground, shadowElevation = 8.dp) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Seats", color = TextSecondary)
                            Text("₹${bookingViewModel.seatAmount}", color = TextPrimary)
                        }
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Food & Beverages", color = TextSecondary)
                            Text("₹${bookingViewModel.foodAmount}", color = TextPrimary)
                        }
                        Spacer(Modifier.height(6.dp))
                        HorizontalDivider(color = DividerColor)
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Total", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("₹${bookingViewModel.totalAmount}", color = PrimaryAccent, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        Spacer(Modifier.height(12.dp))
                        val isBusy = bookingViewModel.isCreatingBooking || bookingViewModel.isRequestingPayment
                        Button(
                            onClick = {
                                paymentSheetError = null
                                bookingViewModel.fetchPaymentIntent(
                                    onSecret = { clientSecret ->
                                        paymentSheet.presentWithPaymentIntent(
                                            clientSecret,
                                            PaymentSheet.Configuration(
                                                merchantDisplayName = "BookMyMovie"
                                            )
                                        )
                                    },
                                    onError = { err -> paymentSheetError = err }
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isBusy
                        ) {
                            if (isBusy) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Confirm & Pay", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        },
        containerColor = DeepCharcoal
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Movie & Showtime Info ─────────────────────────────────────────
            showtime?.let { st ->
                item {
                    Surface(shape = RoundedCornerShape(16.dp), color = CardBackground, modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(12.dp)) {
                            AsyncImage(
                                model = "https://image.tmdb.org/t/p/w185${st.moviePoster}",
                                contentDescription = null,
                                modifier = Modifier.width(72.dp).height(100.dp).clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.align(Alignment.CenterVertically)) {
                                Text(st.movieName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(Modifier.height(6.dp))
                                InfoRow("Cinema", bookingViewModel.currentCinemaName)
                                InfoRow("Screen", "${st.screenName} · ${st.screenType}")
                                InfoRow("Date",   "${bookingViewModel.formatDisplayDate(st.date)} at ${st.time}")
                                InfoRow("Lang",   st.language)
                            }
                        }
                    }
                }
            }

            // ── Selected Seats ───────────────────────────────────────────────
            item {
                SectionHeader(icon = Icons.Default.EventSeat, title = "Seats")
            }
            items(selectedSeats) { seat ->
                Row(
                    Modifier.fillMaxWidth().background(CardBackground, RoundedCornerShape(10.dp)).padding(12.dp),
                    Arrangement.SpaceBetween, Alignment.CenterVertically
                ) {
                    Text("Seat ${seat.row}${seat.col}", color = TextPrimary, fontSize = 14.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        val typeColor = when (seat.type) {
                            "gold" -> GoldSeat; "platinum" -> PlatinumSeat; else -> SilverSeat
                        }
                        Box(Modifier.background(typeColor.copy(0.2f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                            Text(seat.type.replaceFirstChar { it.uppercase() }, color = typeColor, fontSize = 11.sp)
                        }
                        Text("₹${seat.price}", color = PrimaryAccent, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ── Food Items ───────────────────────────────────────────────────
            if (cartItems.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    SectionHeader(icon = Icons.Default.Restaurant, title = "Food & Beverages")
                }
                items(cartItems) { cartFood ->
                    Row(
                        Modifier.fillMaxWidth().background(CardBackground, RoundedCornerShape(10.dp)).padding(12.dp),
                        Arrangement.SpaceBetween, Alignment.CenterVertically
                    ) {
                        Text(cartFood.item.name, color = TextPrimary)
                        Text("${cartFood.qty} × ₹${cartFood.item.price} = ₹${cartFood.qty * cartFood.item.price}", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            }

            item { Spacer(Modifier.height(140.dp)) }
        }
    }

    // Reset any previous confirmed booking so a second booking doesn't skip payment
    LaunchedEffect(Unit) {
        bookingViewModel.confirmedBooking = null
    }

    // Navigate to confirmation once booking is created
    LaunchedEffect(bookingViewModel.confirmedBooking) {
        if (bookingViewModel.confirmedBooking != null) {
            navController.navigate(Screen.BookingConfirmation.route) {
                popUpTo(Screen.BookingSummary.route) { inclusive = true }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    if (value.isEmpty()) return
    Row {
        Text("$label: ", color = TextSecondary, fontSize = 12.sp)
        Text(value, color = TextPrimary, fontSize = 12.sp)
    }
}

@Composable
private fun SectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, tint = PrimaryAccent, modifier = Modifier.size(18.dp))
        Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}
