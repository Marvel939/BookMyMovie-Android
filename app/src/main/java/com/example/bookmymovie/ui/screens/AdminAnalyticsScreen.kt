package com.example.bookmymovie.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.bookmymovie.model.Booking
import com.example.bookmymovie.model.StreamingTransaction
import com.example.bookmymovie.ui.components.ChartData
import com.example.bookmymovie.ui.components.DonutChart
import com.example.bookmymovie.ui.components.Legend
import com.example.bookmymovie.ui.components.InvoiceDialog
import com.example.bookmymovie.ui.theme.*
import com.example.bookmymovie.ui.viewmodel.AdminAnalyticsViewModel
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAnalyticsScreen(
    navController: NavController,
    viewModel: AdminAnalyticsViewModel = viewModel()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Analytics", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            AnalyticsTabContent(viewModel)
        }
    }
}

@Composable
fun AnalyticsTabContent(viewModel: AdminAnalyticsViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Summary Cards Grid
            SummaryCards(state)

            // Profit Charts Section
            ProfitTrends(state)

            // Top Movies Chart
            TopMoviesChart(state)

            // Seat Distribution Chart
            SeatDistributionChart(state)

            // Refund Chart Section
            RefundChartSection(state)
                
            Spacer(Modifier.height(16.dp))
            
            StreamingAnalyticsSection(state)
            
            Spacer(Modifier.height(16.dp))
            
            RecentBookingsSection(state.recentBookings)
            
            Spacer(Modifier.height(24.dp))
            
            RefundHistorySection(state.refundBookings)
            
            Spacer(Modifier.height(24.dp))
            
            StreamingHistorySection(state.streamingTransactions)
        }
    }
}

@Composable
fun SummaryCards(state: com.example.bookmymovie.ui.viewmodel.AdminAnalyticsState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Key Metrics", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                title = "Total Revenue",
                value = "₹${String.format("%.2f", state.totalProfit)}",
                icon = Icons.Default.Payments,
                color = Color(0xFF4CAF50),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Tickets Sold",
                value = state.totalTicketsSold.toString(),
                icon = Icons.Default.ConfirmationNumber,
                color = Color(0xFF2196F3),
                modifier = Modifier.weight(1f)
            )
        }
        
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                title = "Total Users",
                value = state.totalUsers.toString(),
                icon = Icons.Default.People,
                color = Color(0xFFFF9800),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Theatres",
                value = state.totalTheatres.toString(),
                icon = Icons.Default.TheaterComedy,
                color = Color(0xFFE91E63),
                modifier = Modifier.weight(1f)
            )
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                title = "Owners",
                value = state.totalTheatreOwners.toString(),
                icon = Icons.Default.Business,
                color = Color(0xFF9C27B0),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Offers",
                value = state.totalOffers.toString(),
                icon = Icons.Default.LocalOffer,
                color = Color(0xFF00BCD4),
                modifier = Modifier.weight(1f)
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                title = "Total Refunds",
                value = "₹${String.format("%.0f", state.totalRefunds)}",
                icon = Icons.Default.Redeem,
                color = Color(0xFFE53935),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Stream Rev",
                value = "₹${String.format("%.0f", state.totalStreamingRevenue)}",
                icon = Icons.Default.Stream,
                color = Color(0xFF00C853),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, color = color)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun ProfitTrends(state: com.example.bookmymovie.ui.viewmodel.AdminAnalyticsState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Profit Breakdown", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SmallMetric("Weekly", "₹${String.format("%.0f", state.weeklyProfit)}", Color(0xFF4CAF50))
                SmallMetric("Monthly", "₹${String.format("%.0f", state.monthlyProfit)}", Color(0xFF2196F3))
                SmallMetric("Yearly", "₹${String.format("%.0f", state.yearlyProfit)}", Color(0xFFFF9800))
            }
            
            // Profit Chart
            val modelProducer = remember { CartesianChartModelProducer() }
            LaunchedEffect(state.weeklyProfit, state.monthlyProfit, state.yearlyProfit) {
                modelProducer.runTransaction {
                    lineSeries {
                        series(state.weeklyProfit, state.monthlyProfit, state.yearlyProfit)
                    }
                }
            }

            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberLineCartesianLayer(),
                    startAxis = rememberStartAxis(),
                    bottomAxis = rememberBottomAxis(
                        valueFormatter = { value, _, _ ->
                            when (value.toInt()) {
                                0 -> "Weekly"
                                1 -> "Monthly"
                                2 -> "Yearly"
                                else -> ""
                            }
                        }
                    )
                ),
                modelProducer = modelProducer,
                modifier = Modifier.height(200.dp).fillMaxWidth()
            )
        }
    }
}

@Composable
fun SmallMetric(label: String, value: String, color: Color) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun TopMoviesChart(state: com.example.bookmymovie.ui.viewmodel.AdminAnalyticsState) {
    if (state.topMovies.isEmpty()) return
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Top 5 Movies by Revenue", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            
            val modelProducer = remember { CartesianChartModelProducer() }
            LaunchedEffect(state.topMovies) {
                modelProducer.runTransaction {
                    columnSeries {
                        series(state.topMovies.map { it.second })
                    }
                }
            }

            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberColumnCartesianLayer(),
                    startAxis = rememberStartAxis(),
                    bottomAxis = rememberBottomAxis(
                        valueFormatter = { value, _, _ ->
                            val index = value.toInt()
                            if (index in state.topMovies.indices) {
                                state.topMovies[index].first.take(8) + ".."
                            } else ""
                        }
                    )
                ),
                modelProducer = modelProducer,
                modifier = Modifier.height(200.dp).fillMaxWidth()
            )
        }
    }
}

@Composable
fun SeatDistributionChart(state: com.example.bookmymovie.ui.viewmodel.AdminAnalyticsState) {
    if (state.seatDistribution.isEmpty()) return
    
    val chartData = state.seatDistribution.map { (label, value) ->
        val color = when(label.lowercase()) {
            "silver" -> Color(0xFF9E9E9E)
            "gold" -> Color(0xFFFFD700)
            "platinum" -> Color(0xFFE5E4E2)
            else -> Color.Cyan
        }
        ChartData(label, value.toFloat(), color)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Seat Category (Donut)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                DonutChart(
                    data = chartData,
                    modifier = Modifier.size(140.dp),
                    centerLabel = "Total",
                    centerValue = state.seatDistribution.values.sum().toString()
                )
                Legend(chartData, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun RecentBookingsSection(bookings: List<Booking>) {
    var selectedBooking by remember { mutableStateOf<Booking?>(null) }
    
    if (selectedBooking != null) {
        InvoiceDialog(booking = selectedBooking!!) {
            selectedBooking = null
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Detailed Booking History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        bookings.forEach { booking ->
            BookingDetailCard(booking) {
                selectedBooking = booking
            }
        }
    }
}

@Composable
fun BookingDetailCard(booking: Booking, onViewInvoice: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header: Booking ID and Status
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Booking ID", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(booking.bookingId.take(12).uppercase(), fontWeight = FontWeight.ExtraBold, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (booking.status == "confirmed") Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                ) {
                    Text(
                        booking.status.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (booking.status == "confirmed") Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // User Info
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text(booking.userName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(booking.userEmail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onViewInvoice, colors = ButtonDefaults.textButtonColors(contentColor = PrimaryAccent)) {
                    Text("View Invoice", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Movie Info
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Movie", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(booking.movieName, fontWeight = FontWeight.Bold)
                }
                Column(Modifier.weight(1f)) {
                    Text("Date & Time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${booking.date} | ${booking.time}", fontWeight = FontWeight.Medium)
                }
            }

            // Cinema and Seats
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Cinema", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(booking.cinemaName, fontSize = 13.sp)
                }
                Column(Modifier.weight(1f)) {
                    Text("Seats", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(booking.seats.joinToString(", "), fontWeight = FontWeight.Bold)
                }
            }

            // Payment and Discount
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    if (booking.discountAmount > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalOffer, contentDescription = null, size = 12.dp, tint = Color(0xFF4CAF50))
                            Spacer(Modifier.width(4.dp))
                            Text("Discount: ₹${booking.discountAmount} (${booking.discountCode})", fontSize = 11.sp, color = Color(0xFF2E7D32))
                        }
                    }
                    Text("Total Paid", style = MaterialTheme.typography.labelSmall)
                    Text("₹${booking.totalAmount}", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                }
                Text(
                    "via ${booking.paymentMethod.replaceFirstChar { it.uppercase() }}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun RefundChartSection(state: com.example.bookmymovie.ui.viewmodel.AdminAnalyticsState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Revenue vs Refunds", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            val modelProducer = remember { CartesianChartModelProducer() }
            LaunchedEffect(state.totalProfit, state.totalRefunds) {
                modelProducer.runTransaction {
                    columnSeries {
                        series(state.totalProfit.toFloat(), state.totalRefunds.toFloat())
                    }
                }
            }

            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberColumnCartesianLayer(),
                    startAxis = rememberStartAxis(),
                    bottomAxis = rememberBottomAxis(
                        valueFormatter = { value, _, _ ->
                            val index = value.toInt()
                            if (index == 0) "Revenue" else if (index == 1) "Refunds" else ""
                        }
                    )
                ),
                modelProducer = modelProducer,
                modifier = Modifier.height(200.dp).fillMaxWidth()
            )
        }
    }
}

@Composable
fun RefundHistorySection(refunds: List<Booking>) {
    var selectedBooking by remember { mutableStateOf<Booking?>(null) }
    
    if (selectedBooking != null) {
        InvoiceDialog(booking = selectedBooking!!) {
            selectedBooking = null
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Refund History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFFE53935))
        if (refunds.isEmpty()) {
            Text("No refund transactions found.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        }
        refunds.forEach { booking ->
            RefundDetailCard(booking) {
                selectedBooking = booking
            }
        }
    }
}

@Composable
fun RefundDetailCard(booking: Booking, onViewInvoice: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        border = BorderStroke(1.5.dp, Color(0xFFEF5350))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).background(Color(0xFFEF5350), CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text("REFUNDED", fontWeight = FontWeight.Bold, color = Color(0xFFEF5350), fontSize = 11.sp, letterSpacing = 1.sp)
                    }
                    Text(booking.bookingId.take(12).uppercase(), fontWeight = FontWeight.ExtraBold, color = Color.White, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 16.sp)
                }
                Surface(
                    color = Color(0xFFEF5350).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "₹${booking.refundableAmount}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFEF5350),
                        fontSize = 18.sp
                    )
                }
            }
            
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(36.dp).background(Color(0xFFEF5350).copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, null, tint = Color(0xFFEF5350), modifier = Modifier.size(18.dp))
                }
                Column {
                    Text(booking.userName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                    Text(booking.movieName, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = onViewInvoice,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350).copy(alpha = 0.2f), contentColor = Color(0xFFEF5350)),
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

@Composable
fun StreamingAnalyticsSection(state: com.example.bookmymovie.ui.viewmodel.AdminAnalyticsState) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Streaming Performance", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = Color(0xFF00C853))
        
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            val streamData = listOf(
                ChartData("Buy", state.buyCount.toFloat(), Color(0xFF00C853)),
                ChartData("Rent", state.rentCount.toFloat(), Color(0xFF2979FF))
            )
            
            Card(
                Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Buy vs Rent", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    DonutChart(data = streamData, modifier = Modifier.size(100.dp), centerValue = (state.buyCount + state.rentCount).toString())
                    Spacer(Modifier.height(16.dp))
                    Legend(streamData)
                }
            }

            Card(
                Modifier.weight(1.2f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Top Streamers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    state.topStreamingMovies.forEach { (title, revenue) ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(title.take(12), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("₹${revenue.toInt()}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF00C853))
                        }
                        LinearProgressIndicator(
                            progress = { (revenue / (state.totalStreamingRevenue.coerceAtLeast(1.0))).toFloat() },
                            modifier = Modifier.fillMaxWidth().height(4.dp).padding(vertical = 4.dp),
                            color = Color(0xFF00C853),
                            trackColor = Color(0xFF00C853).copy(alpha = 0.1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StreamingHistorySection(transactions: List<StreamingTransaction>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Streaming Transactions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF00C853))
        if (transactions.isEmpty()) {
            Text("No stream transactions found.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        }
        transactions.forEach { tx ->
            StreamingTransactionCard(tx)
        }
    }
}

@Composable
fun StreamingTransactionCard(tx: StreamingTransaction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2E1B)),
        border = BorderStroke(1.dp, Color(0xFF00C853).copy(alpha = 0.3f))
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).background(Color(0xFF00C853).copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(if (tx.type == "buy") Icons.Default.ShoppingCart else Icons.Default.Schedule, null, tint = Color(0xFF00C853), modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(tx.movieTitle, fontWeight = FontWeight.Bold, color = Color.White)
                Text("User ID: ${tx.userId.take(8).uppercase()}", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("₹${tx.amount.toInt()}", fontWeight = FontWeight.Black, color = Color(0xFF00C853), fontSize = 16.sp)
                Text(tx.type.uppercase(), fontWeight = FontWeight.Bold, fontSize = 10.sp, color = if (tx.type == "buy") Color(0xFF00C853) else Color(0xFF2979FF))
            }
        }
    }
}

@Composable
private fun Icon(icon: ImageVector, contentDescription: String?, size: androidx.compose.ui.unit.Dp, tint: Color) {
    Icon(icon, contentDescription, modifier = Modifier.size(size), tint = tint)
}
