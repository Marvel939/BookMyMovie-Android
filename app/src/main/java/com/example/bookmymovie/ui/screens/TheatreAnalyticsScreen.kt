package com.example.bookmymovie.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bookmymovie.model.Booking
import com.example.bookmymovie.ui.components.DonutChart
import com.example.bookmymovie.ui.components.ChartData
import com.example.bookmymovie.ui.components.Legend
import com.example.bookmymovie.ui.components.InvoiceDialog
import com.example.bookmymovie.ui.theme.*
import com.example.bookmymovie.ui.viewmodel.TheatreAnalyticsViewModel
import com.example.bookmymovie.ui.viewmodel.TheatreAnalyticsState
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries

@Composable
fun TheatreAnalyticsContent() {
    val viewModel: TheatreAnalyticsViewModel = viewModel()
    val state by viewModel.state.collectAsState()

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrimaryAccent)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(DeepCharcoal),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Text(
                    "Theatre Performance",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    "Real-time insights for your cinema",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    TheatreMetricCard(
                        title = "Total Revenue",
                        value = "₹${state.totalRevenue.toInt()}",
                        icon = Icons.Default.Payments,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.weight(1f)
                    )
                    TheatreMetricCard(
                        title = "Tickets Sold",
                        value = state.totalTicketsSold.toString(),
                        icon = Icons.Default.ConfirmationNumber,
                        color = Color(0xFF2196F3),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(16.dp))
                TheatreMetricCard(
                    title = "Total Refunds",
                    value = "₹${state.totalRefunds.toInt()}",
                    icon = Icons.Default.Redeem,
                    color = Color(0xFFE53935),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Profit Breakdown
            item {
                TheatreProfitSection(state)
            }

            // Top Movies
            item {
                TheatreTopMoviesChart(state)
            }

            // Refund Chart
            item {
                TheatreRefundChart(state)
            }

            // Recent Transactions
            item {
                TheatreRecentBookingsSection(state.recentBookings)
            }

            // Refund History Section
            item {
                TheatreRefundHistorySection(state.refundBookings)
            }
        }
    }
}

@Composable
fun TheatreMetricCard(title: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(12.dp))
            Text(title, color = TextSecondary, fontSize = 12.sp)
            Text(value, color = color, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TheatreProfitSection(state: TheatreAnalyticsState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Profit Breakdown", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ProfitItem("Weekly", "₹${state.weeklyProfit.toInt()}", Color(0xFF4CAF50))
                ProfitItem("Monthly", "₹${state.monthlyProfit.toInt()}", Color(0xFF2196F3))
                ProfitItem("Yearly", "₹${state.yearlyProfit.toInt()}", Color(0xFFFF9800))
            }
        }
    }
}

@Composable
fun ProfitItem(label: String, value: String, color: Color) {
    Column {
        Text(label, color = TextSecondary, fontSize = 12.sp)
        Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TheatreTopMoviesChart(state: TheatreAnalyticsState) {
    if (state.topMovies.isEmpty()) return
    
    val modelProducer = remember { CartesianChartModelProducer() }
    val movieTitles = remember(state.topMovies) { state.topMovies.keys.toList() }
    
    LaunchedEffect(state.topMovies) {
        modelProducer.runTransaction {
            columnSeries {
                series(state.topMovies.values.toList())
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                "Top Movies by Revenue", 
                style = MaterialTheme.typography.titleLarge, 
                fontWeight = FontWeight.Bold, 
                color = TextPrimary
            )
            
            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberColumnCartesianLayer(),
                    startAxis = rememberStartAxis(),
                    bottomAxis = rememberBottomAxis(
                        valueFormatter = { value, _, _ -> 
                            movieTitles.getOrNull(value.toInt())?.take(10) ?: ""
                        }
                    ),
                ),
                modelProducer = modelProducer,
                modifier = Modifier.fillMaxWidth().height(200.dp),
            )
            
            // Legend
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.topMovies.forEach { (title, revenue) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).background(PrimaryAccent, CircleShape))
                            Spacer(Modifier.width(8.dp))
                            Text(title, fontSize = 12.sp, color = TextPrimary)
                        }
                        Text("₹${revenue.toInt()}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PrimaryAccent)
                    }
                }
            }
        }
    }
}

@Composable
fun TheatreRecentBookingsSection(bookings: List<Booking>) {
    var selectedBooking by remember { mutableStateOf<Booking?>(null) }
    
    if (selectedBooking != null) {
        InvoiceDialog(booking = selectedBooking!!) {
            selectedBooking = null
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Recent Transactions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
        bookings.forEach { booking ->
            TheatreBookingCard(booking) {
                selectedBooking = booking
            }
        }
    }
}

@Composable
fun TheatreBookingCard(booking: Booking, onViewInvoice: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, DividerColor)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(booking.movieName, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("${booking.date} | ${booking.time}", fontSize = 12.sp, color = TextSecondary)
                }
                TextButton(onClick = onViewInvoice) {
                    Text("Invoice", color = PrimaryAccent, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("₹${booking.totalAmount}", color = PrimaryAccent, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (booking.status == "confirmed") Color(0xFF2ECC71).copy(alpha = 0.1f) else Color.Red.copy(alpha = 0.1f)
                ) {
                    Text(
                        booking.status.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (booking.status == "confirmed") Color(0xFF2ECC71) else Color.Red
                    )
                }
            }
        }
    }
}
@Composable
fun TheatreRefundChart(state: TheatreAnalyticsState) {
    val chartData = listOf(
        ChartData("Net Revenue", (state.totalRevenue - state.totalRefunds).toFloat().coerceAtLeast(0f), Color(0xFF2ECC71)),
        ChartData("Refunds", state.totalRefunds.toFloat(), Color(0xFFE53935))
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Revenue vs Refunds (Donut)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                DonutChart(
                    data = chartData,
                    modifier = Modifier.size(120.dp),
                    centerLabel = "Total",
                    centerValue = "₹${state.totalRevenue.toInt()}"
                )
                Legend(chartData, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun TheatreRefundHistorySection(refunds: List<Booking>) {
    var selectedBooking by remember { mutableStateOf<Booking?>(null) }
    
    if (selectedBooking != null) {
        InvoiceDialog(booking = selectedBooking!!) {
            selectedBooking = null
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Refund History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFFE53935))
        if (refunds.isEmpty()) {
            Text("No refund transactions found.", color = TextSecondary, fontSize = 14.sp)
        }
        refunds.forEach { booking ->
            TheatreRefundDetailCard(booking) {
                selectedBooking = booking
            }
        }
    }
}

@Composable
fun TheatreRefundDetailCard(booking: Booking, onViewInvoice: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        border = BorderStroke(1.5.dp, Color(0xFFE53935))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).background(Color(0xFFE53935), CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text("REFUNDED", fontWeight = FontWeight.Bold, color = Color(0xFFE53935), fontSize = 11.sp, letterSpacing = 1.sp)
                    }
                    Text(booking.bookingId.take(12).uppercase(), fontWeight = FontWeight.ExtraBold, color = TextPrimary, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 16.sp)
                }
                Surface(
                    color = Color(0xFFE53935).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "₹${booking.refundableAmount}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFE53935),
                        fontSize = 18.sp
                    )
                }
            }
            
            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(36.dp).background(Color(0xFFE53935).copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, null, tint = Color(0xFFE53935), modifier = Modifier.size(18.dp))
                }
                Column {
                    Text(booking.userName, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                    Text(booking.movieName, color = TextSecondary, fontSize = 12.sp)
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = onViewInvoice,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935).copy(alpha = 0.2f), contentColor = Color(0xFFE53935)),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Details", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
