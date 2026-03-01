package com.example.bookmymovie.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
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

    // Seat count selection state
    var maxSeats by remember { mutableIntStateOf(0) }
    var showSeatCountDialog by remember { mutableStateOf(true) }

    LaunchedEffect(safePlace, safeScreen, safeShowtime) {
        bookingViewModel.loadSeats(safePlace, safeScreen, safeShowtime)
    }

    // Apply format pricing after seats are loaded
    LaunchedEffect(bookingViewModel.seats) {
        if (bookingViewModel.seats.isNotEmpty() && bookingViewModel.selectedFormat.isNotBlank()) {
            bookingViewModel.applyFormatPricing()
        }
    }

    // Get format-based prices for display
    val formatPriceMap = remember(bookingViewModel.selectedFormat, showtime) {
        showtime.formatPrices[bookingViewModel.selectedFormat]
    }
    val silverPrice = formatPriceMap?.get("silver") ?: bookingViewModel.seats.values.firstOrNull { it.type == "silver" }?.price ?: 150
    val goldPrice = formatPriceMap?.get("gold") ?: bookingViewModel.seats.values.firstOrNull { it.type == "gold" }?.price ?: 250
    val platinumPrice = formatPriceMap?.get("platinum") ?: bookingViewModel.seats.values.firstOrNull { it.type == "platinum" }?.price ?: 400

    // Group into a matrix
    val seatMatrix by remember(bookingViewModel.seats) {
        derivedStateOf {
            bookingViewModel.seats.values
                .groupBy { it.row }
                .entries
                .sortedBy { it.key }
        }
    }

    // Seat count selection dialog
    if (showSeatCountDialog) {
        SeatCountDialog(
            onSelectCount = { count ->
                maxSeats = count
                showSeatCountDialog = false
            },
            onDismiss = { navController.popBackStack() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Select Seats", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 17.sp)
                        if (maxSeats > 0) {
                            Text(
                                "${bookingViewModel.selectedSeats.size}/$maxSeats seats selected",
                                color = if (bookingViewModel.selectedSeats.size == maxSeats) PrimaryAccent else TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = TextPrimary)
                    }
                },
                actions = {
                    if (maxSeats > 0) {
                        TextButton(onClick = {
                            bookingViewModel.selectedSeats = emptySet()
                            showSeatCountDialog = true
                        }) {
                            Text("$maxSeats", color = PrimaryAccent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.EventSeat, null, tint = PrimaryAccent, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepCharcoal)
            )
        },
        bottomBar = {
            if (bookingViewModel.selectedSeats.isNotEmpty()) {
                Surface(
                    color = CardBackground,
                    shadowElevation = 12.dp,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "${bookingViewModel.selectedSeats.size} Seat${if (bookingViewModel.selectedSeats.size > 1) "s" else ""} Selected",
                                color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "₹${bookingViewModel.seatAmount}",
                                color = PrimaryAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            )
                        }
                        Button(
                            onClick = { navController.navigate(Screen.FoodBeverage.route) },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.Default.ConfirmationNumber, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Continue", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        containerColor = DeepCharcoal
    ) { padding ->
        if (!showSeatCountDialog) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Curved Screen with Glow ──────────────────────────────────
                item {
                    CurvedScreen()
                    Spacer(Modifier.height(28.dp))
                }

                // ── Seat Sections ────────────────────────────────────────────
                val silverRows = seatMatrix.filter { (_, seats) -> seats.any { it.type == "silver" } }
                val goldRows = seatMatrix.filter { (_, seats) -> seats.any { it.type == "gold" } }
                val platinumRows = seatMatrix.filter { (_, seats) -> seats.any { it.type == "platinum" } }

                // Silver Section
                if (silverRows.isNotEmpty()) {
                    item { SeatSectionHeader(label = "SILVER", price = "₹$silverPrice", color = SilverSeat) }
                    items(silverRows) { (row, seats) ->
                        SeatRow(
                            row = row,
                            seats = seats.sortedBy { it.col },
                            selectedSeats = bookingViewModel.selectedSeats,
                            maxSeats = maxSeats,
                            onToggle = { seatId ->
                                if (bookingViewModel.selectedSeats.contains(seatId)) {
                                    bookingViewModel.toggleSeat(seatId)
                                } else if (bookingViewModel.selectedSeats.size < maxSeats) {
                                    bookingViewModel.toggleSeat(seatId)
                                }
                            }
                        )
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }

                // Gold Section
                if (goldRows.isNotEmpty()) {
                    item { SeatSectionHeader(label = "GOLD", price = "₹$goldPrice", color = GoldSeat) }
                    items(goldRows) { (row, seats) ->
                        SeatRow(
                            row = row,
                            seats = seats.sortedBy { it.col },
                            selectedSeats = bookingViewModel.selectedSeats,
                            maxSeats = maxSeats,
                            onToggle = { seatId ->
                                if (bookingViewModel.selectedSeats.contains(seatId)) {
                                    bookingViewModel.toggleSeat(seatId)
                                } else if (bookingViewModel.selectedSeats.size < maxSeats) {
                                    bookingViewModel.toggleSeat(seatId)
                                }
                            }
                        )
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }

                // Platinum Section
                if (platinumRows.isNotEmpty()) {
                    item { SeatSectionHeader(label = "PLATINUM", price = "₹$platinumPrice", color = PlatinumSeat) }
                    items(platinumRows) { (row, seats) ->
                        SeatRow(
                            row = row,
                            seats = seats.sortedBy { it.col },
                            selectedSeats = bookingViewModel.selectedSeats,
                            maxSeats = maxSeats,
                            onToggle = { seatId ->
                                if (bookingViewModel.selectedSeats.contains(seatId)) {
                                    bookingViewModel.toggleSeat(seatId)
                                } else if (bookingViewModel.selectedSeats.size < maxSeats) {
                                    bookingViewModel.toggleSeat(seatId)
                                }
                            }
                        )
                    }
                }

                // ── Legend ───────────────────────────────────────────────────
                item {
                    Spacer(Modifier.height(28.dp))
                    SeatLegend(
                        silverPrice = silverPrice,
                        goldPrice = goldPrice,
                        platinumPrice = platinumPrice
                    )
                    Spacer(Modifier.height(100.dp))
                }
            }
        }
    }
}

// ─── Seat Count Selection Dialog ─────────────────────────────────────────────

@Composable
private fun SeatCountDialog(
    onSelectCount: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        shape = RoundedCornerShape(24.dp),
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(PrimaryAccent.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.EventSeat, null, tint = PrimaryAccent, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "How many seats?",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Select the number of seats you want to book",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column {
                for (rowStart in listOf(1, 6)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (i in rowStart..rowStart + 4) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0xFF2A2A35),
                                                Color(0xFF1E1E26)
                                            )
                                        )
                                    )
                                    .border(1.dp, DividerColor, RoundedCornerShape(14.dp))
                                    .clickable { onSelectCount(i) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "$i",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                        }
                    }
                    if (rowStart == 1) Spacer(Modifier.height(10.dp))
                }
            }
        },
        confirmButton = {}
    )
}

// ─── Curved Screen ───────────────────────────────────────────────────────────

@Composable
private fun CurvedScreen() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(80.dp)
        ) {
            val w = size.width
            val h = size.height

            // Glow effect
            val glowPath = Path().apply {
                moveTo(0f, h * 0.6f)
                quadraticBezierTo(w / 2, -h * 0.3f, w, h * 0.6f)
                lineTo(w, h)
                quadraticBezierTo(w / 2, h * 0.3f, 0f, h)
                close()
            }
            drawPath(
                path = glowPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF4FC3F7).copy(alpha = 0.08f),
                        Color(0xFF4FC3F7).copy(alpha = 0.02f),
                        Color.Transparent
                    )
                )
            )

            // Screen curve stroke
            val screenPath = Path().apply {
                moveTo(w * 0.02f, h * 0.55f)
                quadraticBezierTo(w / 2, -h * 0.1f, w * 0.98f, h * 0.55f)
            }
            drawPath(
                path = screenPath,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF4FC3F7).copy(alpha = 0.3f),
                        Color(0xFF4FC3F7).copy(alpha = 0.8f),
                        Color(0xFF4FC3F7).copy(alpha = 0.3f)
                    )
                ),
                style = Stroke(width = 3f)
            )

            // Screen reflection line
            val reflectionPath = Path().apply {
                moveTo(w * 0.1f, h * 0.62f)
                quadraticBezierTo(w / 2, h * 0.05f, w * 0.9f, h * 0.62f)
            }
            drawPath(
                path = reflectionPath,
                color = Color(0xFF4FC3F7).copy(alpha = 0.15f),
                style = Stroke(width = 1.5f)
            )
        }

        Text(
            "SCREEN",
            color = Color(0xFF4FC3F7).copy(alpha = 0.6f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 6.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp)
        )
    }
}

// ─── Seat Section Header ─────────────────────────────────────────────────────

@Composable
private fun SeatSectionHeader(label: String, price: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(16.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(8.dp))
        Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(Modifier.width(8.dp))
        Text(price, color = TextSecondary, fontSize = 11.sp)
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .height(1.dp)
                .weight(2f)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(color.copy(alpha = 0.3f), Color.Transparent)
                    )
                )
        )
    }
}

// ─── Seat Row ────────────────────────────────────────────────────────────────

@Composable
private fun SeatRow(
    row: String,
    seats: List<SeatData>,
    selectedSeats: Set<String>,
    maxSeats: Int,
    onToggle: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            row,
            color = TextSecondary.copy(alpha = 0.6f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(18.dp),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.width(4.dp))

        seats.forEach { seat ->
            val isSelected = selectedSeats.contains(seat.seatId)
            val canSelect = !seat.booked && (isSelected || selectedSeats.size < maxSeats)
            PremiumSeat(
                seat = seat,
                isSelected = isSelected,
                canSelect = canSelect,
                onClick = { if (canSelect && !seat.booked) onToggle(seat.seatId) }
            )
        }

        Spacer(Modifier.width(4.dp))
        Text(
            row,
            color = TextSecondary.copy(alpha = 0.6f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(18.dp),
            textAlign = TextAlign.Center
        )
    }
}

// ─── Premium Seat (Canvas-drawn chair) ───────────────────────────────────────

@Composable
private fun PremiumSeat(
    seat: SeatData,
    isSelected: Boolean,
    canSelect: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 800f),
        label = "seatScale"
    )

    val seatColor by animateColorAsState(
        targetValue = when {
            seat.booked -> Color(0xFF2A2A30)
            isSelected -> PrimaryAccent
            seat.type == "silver" -> SilverSeat
            seat.type == "gold" -> GoldSeat
            seat.type == "platinum" -> PlatinumSeat
            else -> SilverSeat
        },
        label = "seatColor"
    )

    val glowAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.4f else 0f,
        label = "glowAlpha"
    )

    Box(
        modifier = Modifier
            .padding(1.5.dp)
            .size(30.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clickable(enabled = !seat.booked && canSelect) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Glow behind selected seat
            if (glowAlpha > 0f) {
                drawCircle(
                    color = PrimaryAccent.copy(alpha = glowAlpha * 0.3f),
                    radius = w * 0.6f,
                    center = Offset(w / 2, h / 2)
                )
            }

            // Seat backrest
            val backrestPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        left = w * 0.12f,
                        top = h * 0.05f,
                        right = w * 0.88f,
                        bottom = h * 0.52f,
                        topLeftCornerRadius = CornerRadius(w * 0.2f),
                        topRightCornerRadius = CornerRadius(w * 0.2f),
                        bottomLeftCornerRadius = CornerRadius(w * 0.05f),
                        bottomRightCornerRadius = CornerRadius(w * 0.05f)
                    )
                )
            }
            drawPath(
                path = backrestPath,
                brush = Brush.verticalGradient(
                    colors = if (seat.booked) {
                        listOf(Color(0xFF2A2A30), Color(0xFF222228))
                    } else {
                        listOf(seatColor.copy(alpha = 0.9f), seatColor.copy(alpha = 0.65f))
                    }
                )
            )

            // Seat cushion
            val cushionPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        left = w * 0.08f,
                        top = h * 0.55f,
                        right = w * 0.92f,
                        bottom = h * 0.78f,
                        cornerRadius = CornerRadius(w * 0.1f)
                    )
                )
            }
            drawPath(
                path = cushionPath,
                brush = Brush.verticalGradient(
                    colors = if (seat.booked) {
                        listOf(Color(0xFF1E1E24), Color(0xFF1A1A20))
                    } else {
                        listOf(seatColor.copy(alpha = 0.75f), seatColor.copy(alpha = 0.5f))
                    }
                )
            )

            // Armrests
            val armWidth = w * 0.08f
            drawRoundRect(
                color = if (seat.booked) Color(0xFF222228) else seatColor.copy(alpha = 0.5f),
                topLeft = Offset(w * 0.02f, h * 0.38f),
                size = Size(armWidth, h * 0.42f),
                cornerRadius = CornerRadius(w * 0.04f)
            )
            drawRoundRect(
                color = if (seat.booked) Color(0xFF222228) else seatColor.copy(alpha = 0.5f),
                topLeft = Offset(w * 0.9f, h * 0.38f),
                size = Size(armWidth, h * 0.42f),
                cornerRadius = CornerRadius(w * 0.04f)
            )

            // Highlight on backrest
            if (!seat.booked) {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.2f), Color.Transparent)
                    ),
                    topLeft = Offset(w * 0.2f, h * 0.08f),
                    size = Size(w * 0.6f, h * 0.2f),
                    cornerRadius = CornerRadius(w * 0.15f)
                )
            }

            // X mark for booked seats
            if (seat.booked) {
                val cx = w / 2
                val cy = h * 0.4f
                val r = w * 0.12f
                drawLine(Color(0xFF555555), Offset(cx - r, cy - r), Offset(cx + r, cy + r), strokeWidth = 1.5f)
                drawLine(Color(0xFF555555), Offset(cx + r, cy - r), Offset(cx - r, cy + r), strokeWidth = 1.5f)
            }

            // Border for selected
            if (isSelected) {
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.5f),
                    topLeft = Offset(w * 0.08f, h * 0.05f),
                    size = Size(w * 0.84f, h * 0.73f),
                    cornerRadius = CornerRadius(w * 0.18f),
                    style = Stroke(width = 1.2f)
                )
            }
        }

        // Seat number
        if (!seat.booked) {
            Text(
                seat.col.toString(),
                color = Color.White.copy(alpha = if (isSelected) 1f else 0.7f),
                fontSize = 7.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.offset(y = (-2).dp)
            )
        }
    }
}

// ─── Seat Legend ─────────────────────────────────────────────────────────────

@Composable
private fun SeatLegend(silverPrice: Int, goldPrice: Int, platinumPrice: Int) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CardBackground,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendChip(color = SilverSeat, label = "Silver", price = "₹$silverPrice")
                LegendChip(color = GoldSeat, label = "Gold", price = "₹$goldPrice")
                LegendChip(color = PlatinumSeat, label = "Platinum", price = "₹$platinumPrice")
            }
            HorizontalDivider(color = DividerColor)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendStatus(color = SilverSeat, label = "Available")
                LegendStatus(color = PrimaryAccent, label = "Selected")
                LegendStatus(color = Color(0xFF2A2A30), label = "Booked")
            }
        }
    }
}

@Composable
private fun LegendChip(color: Color, label: String, price: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.size(14.dp).background(color, RoundedCornerShape(4.dp)))
        Column {
            Text(label, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Text(price, color = TextSecondary, fontSize = 9.sp)
        }
    }
}

@Composable
private fun LegendStatus(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(modifier = Modifier.size(14.dp)) {
            val w = size.width
            val h = size.height
            drawRoundRect(
                color = color,
                topLeft = Offset(w * 0.1f, h * 0.05f),
                size = Size(w * 0.8f, h * 0.55f),
                cornerRadius = CornerRadius(w * 0.2f)
            )
            drawRoundRect(
                color = color.copy(alpha = 0.7f),
                topLeft = Offset(w * 0.05f, h * 0.6f),
                size = Size(w * 0.9f, h * 0.3f),
                cornerRadius = CornerRadius(w * 0.12f)
            )
        }
        Text(label, color = TextSecondary, fontSize = 11.sp)
    }
}
