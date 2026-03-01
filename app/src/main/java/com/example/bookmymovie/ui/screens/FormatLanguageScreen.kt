package com.example.bookmymovie.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.bookmymovie.navigation.Screen
import com.example.bookmymovie.ui.theme.*
import com.example.bookmymovie.ui.viewmodel.BookingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormatLanguageScreen(
    navController: NavController,
    bookingViewModel: BookingViewModel
) {
    val showtime = bookingViewModel.selectedShowtime ?: run {
        navController.popBackStack()
        return
    }

    // Auto-set format from showtime (one format per showtime)
    val showtimeFormat = remember(showtime) {
        showtime.formats.trim().ifBlank { showtime.screenType.ifBlank { "2D" } }
    }
    val availableLanguages = remember(showtime) {
        showtime.language
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .ifEmpty { listOf("English") }
    }

    var selectedLanguage by remember { mutableStateOf("") }

    // Auto-set format and language if only one option
    LaunchedEffect(showtimeFormat, availableLanguages) {
        bookingViewModel.selectedFormat = showtimeFormat
        if (availableLanguages.size == 1) selectedLanguage = availableLanguages[0]
    }

    // Get prices for the showtime's format
    val formatPrices = remember(showtimeFormat, showtime) {
        showtime.formatPrices[showtimeFormat]
    }

    val canProceed = selectedLanguage.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            showtime.movieName,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 17.sp,
                            maxLines = 1
                        )
                        Text(
                            "${showtime.screenName} • ${showtime.time} • $showtimeFormat",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepCharcoal)
            )
        },
        bottomBar = {
            Surface(
                color = CardBackground,
                shadowElevation = 12.dp,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ) {
                Button(
                    onClick = {
                        bookingViewModel.selectedFormat = showtimeFormat
                        bookingViewModel.selectedLanguage = selectedLanguage
                        bookingViewModel.applyFormatPricing()
                        navController.navigate(Screen.SeatSelection.route)
                    },
                    enabled = canProceed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryAccent,
                        disabledContainerColor = PrimaryAccent.copy(alpha = 0.3f)
                    )
                ) {
                    Icon(Icons.Default.ConfirmationNumber, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Select Seats",
                        color = if (canProceed) Color.White else Color.White.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        },
        containerColor = DeepCharcoal
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // ── Language Selection ───────────────────────────────────────────
            LanguageSection(
                languages = availableLanguages,
                selectedLanguage = selectedLanguage,
                onSelect = { selectedLanguage = it }
            )

            // ── Price Summary (for the showtime's format) ────────────────
            if (formatPrices != null) {
                PriceSummaryCard(
                    format = showtimeFormat,
                    prices = formatPrices
                )
            }
        }
    }
}

// ─── Language Selection Section ──────────────────────────────────────────────

@Composable
private fun LanguageSection(
    languages: List<String>,
    selectedLanguage: String,
    onSelect: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF4FC3F7).copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Translate, null, tint = Color(0xFF4FC3F7), modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Select Language",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        "Choose audio language",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Language chips in a flow-like layout
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                languages.chunked(3).forEach { rowLangs ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowLangs.forEach { lang ->
                            val isSelected = selectedLanguage == lang
                            LanguageChip(
                                language = lang,
                                isSelected = isSelected,
                                onClick = { onSelect(lang) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Fill remaining space if fewer than 3 items
                        repeat(3 - rowLangs.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageChip(
    language: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF4FC3F7).copy(alpha = 0.15f) else SecondaryBackground,
        label = "langBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF4FC3F7) else DividerColor,
        label = "langBorder"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    null,
                    tint = Color(0xFF4FC3F7),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                language,
                color = if (isSelected) Color(0xFF4FC3F7) else TextPrimary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 14.sp
            )
        }
    }
}

// ─── Price Summary Card ─────────────────────────────────────────────────────

@Composable
private fun PriceSummaryCard(
    format: String,
    prices: Map<String, Int>
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF4CAF50).copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CurrencyRupee, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "${format.uppercase()} Pricing",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        "Seat prices for selected format",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PriceTier(
                    label = "Silver",
                    price = prices["silver"] ?: 0,
                    color = SilverSeat
                )
                PriceTier(
                    label = "Gold",
                    price = prices["gold"] ?: 0,
                    color = GoldSeat
                )
                PriceTier(
                    label = "Platinum",
                    price = prices["platinum"] ?: 0,
                    color = PlatinumSeat
                )
            }
        }
    }
}

@Composable
private fun PriceTier(
    label: String,
    price: Int,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(modifier = Modifier.size(32.dp)) {
            val w = size.width
            val h = size.height
            // Mini seat icon
            drawRoundRect(
                color = color,
                topLeft = Offset(w * 0.15f, h * 0.05f),
                size = Size(w * 0.7f, h * 0.5f),
                cornerRadius = CornerRadius(w * 0.2f)
            )
            drawRoundRect(
                color = color.copy(alpha = 0.7f),
                topLeft = Offset(w * 0.1f, h * 0.55f),
                size = Size(w * 0.8f, h * 0.3f),
                cornerRadius = CornerRadius(w * 0.12f)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(2.dp))
        Text(
            "₹$price",
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
    }
}
