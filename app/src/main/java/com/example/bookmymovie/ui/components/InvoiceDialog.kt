package com.example.bookmymovie.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.bookmymovie.model.Booking
import com.example.bookmymovie.ui.theme.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter

@Composable
fun InvoiceDialog(
    booking: Booking,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Booking Invoice",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Movie Info
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = "https://image.tmdb.org/t/p/w185${booking.moviePoster}",
                        contentDescription = null,
                        modifier = Modifier
                            .width(60.dp)
                            .height(84.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(booking.movieName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("${booking.screenName} · ${booking.screenType}", color = TextSecondary, fontSize = 13.sp)
                        Text(booking.date, color = PrimaryAccent, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(Modifier.height(20.dp))
                HorizontalDivider(color = DividerColor, thickness = 1.dp)
                Spacer(Modifier.height(20.dp))

                InvoiceRow("Booking ID", booking.bookingId.take(12).uppercase())
                InvoiceRow("Customer", booking.userName)
                InvoiceRow("Email", booking.userEmail)
                InvoiceRow("Cinema", booking.cinemaName)
                InvoiceRow("Time", booking.time)
                InvoiceRow("Seats", booking.seats.joinToString(", "))

                if (booking.discountAmount > 0) {
                    Spacer(Modifier.height(8.dp))
                    InvoiceRow("Discount (${booking.discountCode})", "-₹${booking.discountAmount}", color = Color(0xFF2ECC71))
                }

                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PrimaryAccent.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total Paid", color = TextPrimary, fontWeight = FontWeight.Medium)
                    Text("₹${booking.totalAmount}", color = PrimaryAccent, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                }

                Spacer(Modifier.height(24.dp))

                // QR Code
                val qrBitmap = remember(booking.bookingId) { generateQrBitmap(booking.bookingId, 400) }
                if (qrBitmap != null) {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .align(Alignment.CenterHorizontally)
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                
                Spacer(Modifier.height(12.dp))
                Text(
                    "Scan this QR at the cinema entrance",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun InvoiceRow(label: String, value: String, color: Color = TextPrimary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecondary, fontSize = 14.sp)
        Text(
            value,
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f, false)
        )
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
