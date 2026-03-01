package com.example.bookmymovie.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.bookmymovie.ui.theme.*
import com.example.bookmymovie.ui.viewmodel.OwnerScreen
import com.example.bookmymovie.ui.viewmodel.TheatreOwnerViewModel
import com.example.bookmymovie.ui.viewmodel.TmdbMovieSearchResult
import java.util.*

private val SHOW_TIMES = listOf(
    "09:00 AM", "10:00 AM", "11:00 AM", "12:00 PM",
    "01:00 PM", "02:00 PM", "03:00 PM", "04:00 PM",
    "05:00 PM", "06:00 PM", "07:00 PM", "08:00 PM",
    "09:00 PM", "10:00 PM", "11:00 PM"
)

private val LANGUAGES = listOf("Hindi", "English", "Tamil", "Telugu", "Kannada", "Malayalam", "Bengali", "Marathi")
private val FORMATS = listOf("2D", "3D", "IMAX", "4DX")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerScheduleScreen(navController: NavController) {
    val vm: TheatreOwnerViewModel = viewModel()
    val context = LocalContext.current

    // ── State ────────────────────────────────────────────────────────────────
    var movieQuery by remember { mutableStateOf("") }
    var selectedMovie by remember { mutableStateOf<TmdbMovieSearchResult?>(null) }
    var movieDurationMinutes by remember { mutableIntStateOf(120) }
    var selectedScreen by remember { mutableStateOf<OwnerScreen?>(null) }
    var selectedDate by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") }
    var selectedLanguage by remember { mutableStateOf(setOf("Hindi")) }
    var selectedFormat by remember { mutableStateOf("2D") }
    // Pricing for the selected format (silver, gold, platinum)
    var formatPricing by remember {
        mutableStateOf(Triple("150", "250", "400"))
    }
    var showMovieSearch by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Load owner profile + screens on first launch
    LaunchedEffect(Unit) { vm.loadOwnerProfile() }

    // Trigger conflict check when screen + date + time change
    LaunchedEffect(selectedScreen, selectedDate, selectedTime) {
        val scr = selectedScreen ?: return@LaunchedEffect
        val d = selectedDate.ifBlank { return@LaunchedEffect }
        val t = selectedTime.ifBlank { return@LaunchedEffect }
        val placeId = vm.ownerProfile?.placeId ?: return@LaunchedEffect
        vm.checkConflict(placeId, scr.screenId, d, t, movieDurationMinutes)
    }

    // Reload when duration changes (movie changed)
    LaunchedEffect(movieDurationMinutes) {
        val scr = selectedScreen ?: return@LaunchedEffect
        val d = selectedDate.ifBlank { return@LaunchedEffect }
        val t = selectedTime.ifBlank { return@LaunchedEffect }
        val placeId = vm.ownerProfile?.placeId ?: return@LaunchedEffect
        vm.checkConflict(placeId, scr.screenId, d, t, movieDurationMinutes)
    }

    // Date picker
    val cal = Calendar.getInstance()
    val datePicker = DatePickerDialog(
        context,
        { _, y, m, d -> selectedDate = "%04d-%02d-%02d".format(y, m + 1, d); vm.clearConflict() },
        cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
    ).also { it.datePicker.minDate = System.currentTimeMillis() - 1000 }

    // Movie search dialog
    if (showMovieSearch) {
        MovieSearchDialog(
            query = movieQuery,
            onQueryChange = { movieQuery = it; vm.searchMovies(it) },
            results = vm.movieSearchResults,
            isLoading = vm.isSearchingMovies,
            onSelect = { movie ->
                selectedMovie = movie
                showMovieSearch = false
                movieQuery = ""
                vm.clearMovieSearch()
                vm.fetchMovieDetails(movie.id) { dur -> movieDurationMinutes = dur }
            },
            onDismiss = { showMovieSearch = false; movieQuery = ""; vm.clearMovieSearch() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Schedule a Movie", fontWeight = FontWeight.Bold, color = TextPrimary) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Step 1: Select Movie ────────────────────────────────────────
            SectionCard(title = "1. Select Movie", icon = Icons.Default.Movie) {
                if (selectedMovie != null) {
                    SelectedMovieRow(
                        movie = selectedMovie!!,
                        durationMinutes = movieDurationMinutes,
                        onClear = { selectedMovie = null; vm.clearConflict() }
                    )
                } else {
                    Button(
                        onClick = { showMovieSearch = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryBackground)
                    ) {
                        Icon(Icons.Default.Search, null, tint = PrimaryAccent)
                        Spacer(Modifier.width(8.dp))
                        Text("Search & Select Movie", color = PrimaryAccent)
                    }
                }
            }

            // ── Step 2: Select Screen ───────────────────────────────────────
            SectionCard(title = "2. Select Screen", icon = Icons.Default.ScreenShare) {
                if (vm.ownerScreens.isEmpty()) {
                    Text("No screens available. Add a screen from the panel first.",
                        color = TextSecondary, fontSize = 13.sp)
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(vm.ownerScreens) { screen ->
                            ScreenChip(
                                screen = screen,
                                isSelected = selectedScreen?.screenId == screen.screenId,
                                onClick = { selectedScreen = screen; vm.clearConflict() }
                            )
                        }
                    }
                }
            }

            // ── Step 3: Date ────────────────────────────────────────────────
            SectionCard(title = "3. Select Date", icon = Icons.Default.DateRange) {
                OutlinedButton(
                    onClick = { datePicker.show() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DividerColor),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = SecondaryBackground)
                ) {
                    Icon(Icons.Default.DateRange, null, tint = PrimaryAccent)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (selectedDate.isBlank()) "Pick a Date" else selectedDate,
                        color = if (selectedDate.isBlank()) TextSecondary else TextPrimary
                    )
                }
            }

            // ── Step 4: Time Slot ───────────────────────────────────────────
            SectionCard(title = "4. Select Time Slot", icon = Icons.Default.Schedule) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(SHOW_TIMES) { time ->
                        TimeChip(
                            time = time,
                            isSelected = selectedTime == time,
                            onClick = { selectedTime = time; vm.clearConflict() }
                        )
                    }
                }
            }

            // ── Conflict Banner ─────────────────────────────────────────────
            val conflict = vm.conflictResult
            if ((conflict.hasConflict || conflict.isPending) && selectedScreen != null && selectedDate.isNotBlank() && selectedTime.isNotBlank()) {
                ConflictBanner(
                    screenName = selectedScreen!!.screenName,
                    conflict = conflict,
                    onPickDifferentTime = { selectedTime = "" },
                    onPickDifferentScreen = { selectedScreen = null }
                )
            }

            // ── Step 5: Language ────────────────────────────────────────────
            SectionCard(title = "5. Language", icon = Icons.Default.Translate) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(LANGUAGES) { lang ->
                        FilterChip(
                            selected = selectedLanguage.contains(lang),
                            onClick = {
                                selectedLanguage = if (selectedLanguage.contains(lang)) {
                                    selectedLanguage - lang
                                } else {
                                    selectedLanguage + lang
                                }
                            },
                            label = { Text(lang, fontSize = 13.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryAccent,
                                selectedLabelColor = Color.White,
                                containerColor = SecondaryBackground,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }
            }

            // ── Step 6: Format ──────────────────────────────────────────────
            SectionCard(title = "6. Format", icon = Icons.Default.Theaters) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(FORMATS) { fmt ->
                        FilterChip(
                            selected = selectedFormat == fmt,
                            onClick = { selectedFormat = fmt },
                            label = { Text(fmt, fontSize = 13.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryAccent,
                                selectedLabelColor = Color.White,
                                containerColor = SecondaryBackground,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }
            }

            // ── Step 7: Pricing ──────────────────────────────────────────────
            SectionCard(title = "7. $selectedFormat Pricing", icon = Icons.Default.CurrencyRupee) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PriceField("Silver ₹", formatPricing.first, SilverSeat, Modifier.weight(1f)) {
                        formatPricing = formatPricing.copy(first = it)
                    }
                    PriceField("Gold ₹", formatPricing.second, GoldSeat, Modifier.weight(1f)) {
                        formatPricing = formatPricing.copy(second = it)
                    }
                    PriceField("Platinum ₹", formatPricing.third, PlatinumSeat, Modifier.weight(1f)) {
                        formatPricing = formatPricing.copy(third = it)
                    }
                }
            }

            // ── Submit Button ───────────────────────────────────────────────
            val canSubmit = selectedMovie != null &&
                    selectedScreen != null &&
                    selectedDate.isNotBlank() &&
                    selectedTime.isNotBlank() &&
                    selectedLanguage.isNotEmpty() &&
                    selectedFormat.isNotBlank() &&
                    !conflict.hasConflict

            if (isSubmitting) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryAccent)
                }
            } else {
                Button(
                    onClick = {
                        if (!canSubmit) {
                            val msg = when {
                                selectedMovie == null -> "Please select a movie"
                                selectedScreen == null -> "Please select a screen"
                                selectedDate.isBlank() -> "Please select a date"
                                selectedTime.isBlank() -> "Please select a time slot"
                                selectedLanguage.isEmpty() -> "Please select at least one language"
                                selectedFormat.isBlank() -> "Please select a format"
                                conflict.hasConflict -> "Please resolve the time conflict first"
                                else -> "Please fill all fields"
                            }
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isSubmitting = true
                        // Build format prices map (single format)
                        val fmtPricesMap = mapOf(
                            selectedFormat to mapOf(
                                "silver" to (formatPricing.first.toDoubleOrNull() ?: 150.0).toInt(),
                                "gold" to (formatPricing.second.toDoubleOrNull() ?: 250.0).toInt(),
                                "platinum" to (formatPricing.third.toDoubleOrNull() ?: 400.0).toInt()
                            )
                        )
                        vm.submitShowtimeRequest(
                            screen = selectedScreen!!,
                            movieId = selectedMovie!!.id.toString(),
                            movieName = selectedMovie!!.title,
                            moviePoster = selectedMovie!!.posterPath,
                            movieDurationMinutes = movieDurationMinutes,
                            date = selectedDate,
                            time = selectedTime,
                            language = selectedLanguage.sorted().joinToString(", "),
                            formats = selectedFormat,
                            formatPricesMap = fmtPricesMap,
                            silverPrice = formatPricing.first.toDoubleOrNull() ?: 150.0,
                            goldPrice = formatPricing.second.toDoubleOrNull() ?: 250.0,
                            platinumPrice = formatPricing.third.toDoubleOrNull() ?: 400.0,
                            onSuccess = {
                                isSubmitting = false
                                Toast.makeText(context, "Request submitted for admin approval!", Toast.LENGTH_LONG).show()
                                navController.popBackStack()
                            },
                            onError = { err ->
                                isSubmitting = false
                                Toast.makeText(context, "Error: $err", Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (canSubmit) PrimaryAccent else TextSecondary.copy(alpha = 0.3f)
                    ),
                    enabled = true
                ) {
                    Icon(Icons.Default.Send, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Submit for Admin Approval", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ─── Section Card ─────────────────────────────────────────────────────────────

@Composable
private fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = PrimaryAccent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

// ─── Selected Movie Row ───────────────────────────────────────────────────────

@Composable
private fun SelectedMovieRow(
    movie: TmdbMovieSearchResult,
    durationMinutes: Int,
    onClear: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (movie.posterPath.isNotBlank()) {
            AsyncImage(
                model = movie.posterPath, contentDescription = null,
                modifier = Modifier.size(55.dp, 80.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(movie.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (movie.releaseDate.isNotBlank()) {
                Text(movie.releaseDate, color = TextSecondary, fontSize = 12.sp)
            }
            Text("Duration: ${durationMinutes}min", color = PrimaryAccent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        IconButton(onClick = onClear) {
            Icon(Icons.Default.Close, null, tint = TextSecondary)
        }
    }
}

// ─── Screen Chip ─────────────────────────────────────────────────────────────

@Composable
private fun ScreenChip(screen: OwnerScreen, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) PrimaryAccent else SecondaryBackground,
        modifier = Modifier.clickable { onClick() }
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(screen.screenName, color = if (isSelected) Color.White else TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(screen.screenType, color = if (isSelected) Color.White.copy(0.8f) else TextSecondary, fontSize = 11.sp)
            Text("${screen.totalSeats} seats", color = if (isSelected) Color.White.copy(0.7f) else TextSecondary, fontSize = 10.sp)
        }
    }
}

// ─── Time Chip ────────────────────────────────────────────────────────────────

@Composable
private fun TimeChip(time: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) PrimaryAccent else SecondaryBackground,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            time,
            color = if (isSelected) Color.White else TextSecondary,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

// ─── Conflict Banner ──────────────────────────────────────────────────────────

@Composable
private fun ConflictBanner(
    screenName: String,
    conflict: com.example.bookmymovie.ui.viewmodel.ConflictResult,
    onPickDifferentTime: () -> Unit,
    onPickDifferentScreen: () -> Unit
) {
    val isHard = conflict.hasConflict
    val bgColor = if (isHard) Color(0xFFF44336).copy(alpha = 0.12f) else Color(0xFFFF9800).copy(alpha = 0.12f)
    val borderColor = if (isHard) Color(0xFFF44336) else Color(0xFFFF9800)
    val titleText = if (isHard) "⛔  Time Conflict Detected!" else "⚠️  Pending Request Exists"

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        modifier = Modifier.fillMaxWidth().border(1.dp, borderColor, RoundedCornerShape(14.dp))
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(titleText, color = borderColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            Text("$screenName is occupied:", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(
                "\"${conflict.conflictingMovie}\" runs ${conflict.occupiedFrom} → ${conflict.occupiedUntil}",
                color = TextSecondary, fontSize = 13.sp
            )
            if (conflict.nextAvailableSlot.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text("Next available slot: ${conflict.nextAvailableSlot}", color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onPickDifferentTime,
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                    modifier = Modifier.weight(1f)
                ) { Text("Change Time", color = borderColor, fontSize = 12.sp) }
                OutlinedButton(
                    onClick = onPickDifferentScreen,
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DividerColor),
                    modifier = Modifier.weight(1f)
                ) { Text("Change Screen", color = TextSecondary, fontSize = 12.sp) }
            }
        }
    }
}

// ─── Price Field ──────────────────────────────────────────────────────────────

@Composable
private fun PriceField(label: String, value: String, accentColor: Color, modifier: Modifier, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange,
        label = { Text(label, fontSize = 11.sp, color = accentColor) },
        modifier = modifier,
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accentColor, unfocusedBorderColor = DividerColor,
            focusedContainerColor = CardBackground, unfocusedContainerColor = CardBackground,
            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
        )
    )
}

// ─── Movie Search Dialog ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MovieSearchDialog(
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<TmdbMovieSearchResult>,
    isLoading: Boolean,
    onSelect: (TmdbMovieSearchResult) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        title = { Text("Search Movie", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = query, onValueChange = onQueryChange,
                    label = { Text("Movie name...", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = PrimaryAccent) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryAccent, unfocusedBorderColor = DividerColor,
                        focusedContainerColor = SecondaryBackground, unfocusedContainerColor = SecondaryBackground,
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                    )
                )
                Spacer(Modifier.height(12.dp))
                if (isLoading) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryAccent, modifier = Modifier.size(32.dp))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(results) { movie ->
                            MovieSearchResultRow(movie = movie, onClick = { onSelect(movie) })
                        }
                        if (results.isEmpty() && query.length >= 2) {
                            item {
                                Text("No results found", color = TextSecondary, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

@Composable
private fun MovieSearchResultRow(movie: TmdbMovieSearchResult, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SecondaryBackground)
            .clickable { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (movie.posterPath.isNotBlank()) {
            AsyncImage(
                model = movie.posterPath, contentDescription = null,
                modifier = Modifier.size(40.dp, 58.dp).clip(RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(Modifier.size(40.dp, 58.dp).clip(RoundedCornerShape(6.dp)).background(DividerColor))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(movie.title, color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (movie.releaseDate.isNotBlank()) {
                Text(movie.releaseDate, color = TextSecondary, fontSize = 12.sp)
            }
        }
    }
}
