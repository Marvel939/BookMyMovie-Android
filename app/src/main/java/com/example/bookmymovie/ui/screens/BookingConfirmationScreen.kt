package com.example.bookmymovie.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.bookmymovie.navigation.Screen
import com.example.bookmymovie.ui.theme.*
import com.example.bookmymovie.ui.viewmodel.BookingViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingConfirmationScreen(
    navController: NavController,
    bookingViewModel: BookingViewModel
) {
    val booking = bookingViewModel.confirmedBooking ?: return

    Scaffold(containerColor = DeepCharcoal) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            // ── Success Icon ─────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color(0xFF1A3A1A), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color(0xFF2ECC71), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(44.dp))
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Booking Confirmed!", color = Color(0xFF2ECC71), fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Text("Your tickets have been booked successfully", color = TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center)

            Spacer(Modifier.height(28.dp))

            // ── Ticket Card ──────────────────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = CardBackground,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Movie + poster
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = "https://image.tmdb.org/t/p/w185${booking.moviePoster}",
                            contentDescription = null,
                            modifier = Modifier.width(56.dp).height(80.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(booking.movieName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("${booking.screenName} · ${booking.screenType}", color = TextSecondary, fontSize = 12.sp)
                            Text(booking.language, color = TextSecondary, fontSize = 12.sp)
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = DividerColor)
                    Spacer(Modifier.height(12.dp))

                    TicketRow("Booking ID", booking.bookingId.take(12).uppercase())
                    TicketRow("Cinema", booking.cinemaName)
                    TicketRow("Date & Time", "${bookingViewModel.formatDisplayDate(booking.date)} at ${booking.time}")
                    TicketRow("Seats", booking.seats.joinToString(", "))
                    if (booking.foodItems.isNotEmpty()) {
                        TicketRow("Food Items", "${booking.foodItems.size} item(s)")
                    }

                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = DividerColor)
                    Spacer(Modifier.height(12.dp))

                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text("Total Paid", color = TextSecondary, fontSize = 14.sp)
                        Text("₹${booking.totalAmount}", color = PrimaryAccent, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }

                    Spacer(Modifier.height(20.dp))

                    // ── QR Code ──────────────────────────────────────────────
                    Text("Scan at Cinema Entry", color = TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    val qrBitmap = remember(booking.bookingId) { generateQrBitmap(booking.bookingId, 480) }
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier
                                .size(180.dp)
                                .align(Alignment.CenterHorizontally)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .padding(8.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Done Button ──────────────────────────────────────────────────
            Button(
                onClick = {
                    bookingViewModel.clearBookingState()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Back to Home", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = { navController.navigate(Screen.MyBookings.route) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("View My Bookings", color = PrimaryAccent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun generateQrBitmap(content: String, size: Int): Bitmap? {
    return try {
        val hints = mapOf(EncodeHintType.MARGIN to 1)
        val bitMatrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bmp.setPixel(x, y, if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
            }
        }
        bmp
    } catch (e: Exception) { null }
}

@Composable
private fun TicketRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextSecondary, fontSize = 13.sp)
        Text(value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.End, modifier = Modifier.weight(1f, false))
    }
}
