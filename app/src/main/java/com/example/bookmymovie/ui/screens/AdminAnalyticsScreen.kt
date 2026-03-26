package com.example.bookmymovie.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.bookmymovie.ui.viewmodel.AdminAnalyticsViewModel
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
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

            // Recent Bookings Detailed List
            RecentBookingsSection(state.recentBookings)
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
                    columnSeries {
                        series(state.weeklyProfit, state.monthlyProfit, state.yearlyProfit)
                    }
                }
            }

            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberColumnCartesianLayer(),
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
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Seat Category Distribution", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            
            val modelProducer = remember { CartesianChartModelProducer() }
            val labels = state.seatDistribution.keys.toList()
            LaunchedEffect(state.seatDistribution) {
                modelProducer.runTransaction {
                    columnSeries {
                        series(state.seatDistribution.values.map { it.toFloat() })
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
                            if (index in labels.indices) labels[index] else ""
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
fun RecentBookingsSection(bookings: List<Booking>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Detailed Booking History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        
        bookings.take(20).forEach { booking ->
            BookingDetailCard(booking)
        }
    }
}

@Composable
fun BookingDetailCard(booking: Booking) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header: User and Status
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(booking.userName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(booking.userEmail, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(
                    color = if (booking.status == "confirmed") Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(8.dp)
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

            Divider(color = MaterialTheme.colorScheme.outlineVariant)

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
private fun Icon(icon: ImageVector, contentDescription: String?, size: androidx.compose.ui.unit.Dp, tint: Color) {
    Icon(icon, contentDescription, modifier = Modifier.size(size), tint = tint)
}
