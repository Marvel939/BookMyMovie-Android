package com.example.bookmymovie.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.bookmymovie.model.CouponType
import com.example.bookmymovie.model.DiscountType
import com.example.bookmymovie.model.Offer
import com.example.bookmymovie.model.OfferApprovalStatus
import com.example.bookmymovie.model.OfferCategory
import com.example.bookmymovie.ui.theme.*
import com.example.bookmymovie.ui.viewmodel.AdminCreateOfferViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCreateOfferScreen(
    navController: NavController,
    viewModel: AdminCreateOfferViewModel = viewModel()
) {
    val context = LocalContext.current
    val isLoading by viewModel.isLoading.collectAsState()
    val creationSuccess by viewModel.creationSuccess.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val movies by viewModel.movies.collectAsState()
    val theatres by viewModel.theatres.collectAsState()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var couponCode by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(OfferCategory.THEATRE_SPECIFIC) }
    var couponType by remember { mutableStateOf(CouponType.DISCOUNT) }
    var discountType by remember { mutableStateOf(DiscountType.PERCENTAGE) }
    var discountPercentageStr by remember { mutableStateOf("") }
    var discountAmountStr by remember { mutableStateOf("") }
    var maxDiscountAmountStr by remember { mutableStateOf("") }
    var minOrderAmountStr by remember { mutableStateOf("") }
    var maxRedemptionsStr by remember { mutableStateOf("1") }

    var selectedTheatreId by remember { mutableStateOf("ALL") }
    var selectedTheatreName by remember { mutableStateOf("Across all theatres") }
    var theatreDropdownExpanded by remember { mutableStateOf(false) }

    var selectedMovieId by remember { mutableStateOf("ALL") }
    var selectedMovieName by remember { mutableStateOf("Across all movies") }
    var movieDropdownExpanded by remember { mutableStateOf(false) }

    var startDateStr by remember { mutableStateOf("") }
    var endDateStr by remember { mutableStateOf("") }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(creationSuccess) {
        if (creationSuccess) {
            Toast.makeText(context, "Offer created successfully!", Toast.LENGTH_SHORT).show()
            viewModel.resetSuccess()
            navController.popBackStack()
        }
    }

    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        startDateStr = sdf.format(Date(it))
                    }
                    showStartDatePicker = false
                }) { Text("OK", color = PrimaryAccent) }
            }
        ) { DatePicker(state = datePickerState) }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        endDateStr = sdf.format(Date(it))
                    }
                    showEndDatePicker = false
                }) { Text("OK", color = PrimaryAccent) }
            }
        ) { DatePicker(state = datePickerState) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin: Create Offer", fontWeight = FontWeight.Bold, color = TextPrimary) },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AdminTextField("Offer Title", title) { title = it }
            AdminTextField("Description", description) { description = it }
            AdminTextField("Coupon Code (Ex: SAVE50)", couponCode) { couponCode = it.uppercase() }

            // Coupon Category Selection
            Text("Coupon Category", color = TextPrimary, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(CouponType.DISCOUNT, CouponType.CASHBACK).forEach { type ->
                    FilterChip(
                        selected = couponType == type,
                        onClick = { couponType = type },
                        label = { Text(if (type == CouponType.DISCOUNT) "Discount Coupon" else "Cashback Coupon") }
                    )
                }
            }

            // Category Selection
            Text("Offer Category", color = TextPrimary, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(OfferCategory.THEATRE_SPECIFIC, OfferCategory.MOVIE_SPECIFIC).forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(if (cat == OfferCategory.THEATRE_SPECIFIC) "Theatre Specific" else "Movie Specific") }
                    )
                }
            }

            // Theatre Selector (Visible for THEATRE_SPECIFIC)
            if (category == OfferCategory.THEATRE_SPECIFIC) {
                Text("Select Theatre Range", color = TextPrimary, fontWeight = FontWeight.Bold)
                ExposedDropdownMenuBox(
                    expanded = theatreDropdownExpanded,
                    onExpandedChange = { theatreDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedTheatreName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(theatreDropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                            focusedBorderColor = PrimaryAccent, unfocusedBorderColor = DividerColor
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = theatreDropdownExpanded,
                        onDismissRequest = { theatreDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Across all theatres", color = PrimaryAccent) },
                            onClick = {
                                selectedTheatreId = "ALL"
                                selectedTheatreName = "Across all theatres"
                                theatreDropdownExpanded = false
                            }
                        )
                        theatres.forEach { theatre ->
                            DropdownMenuItem(
                                text = { Text(theatre.name, color = TextPrimary) },
                                onClick = {
                                    selectedTheatreId = theatre.placeId
                                    selectedTheatreName = theatre.name
                                    theatreDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Movie Selector (Always show for MOVIE_SPECIFIC, optional for others?)
            if (category == OfferCategory.MOVIE_SPECIFIC) {
                Text("Select Movie Range", color = TextPrimary, fontWeight = FontWeight.Bold)
                ExposedDropdownMenuBox(
                    expanded = movieDropdownExpanded,
                    onExpandedChange = { movieDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedMovieName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(movieDropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                            focusedBorderColor = PrimaryAccent, unfocusedBorderColor = DividerColor
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = movieDropdownExpanded,
                        onDismissRequest = { movieDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Across all movies", color = PrimaryAccent) },
                            onClick = {
                                selectedMovieId = "ALL"
                                selectedMovieName = "Across all movies"
                                movieDropdownExpanded = false
                            }
                        )
                        movies.forEach { movie ->
                            DropdownMenuItem(
                                text = { Text(movie.title, color = TextPrimary) },
                                onClick = {
                                    selectedMovieId = movie.id
                                    selectedMovieName = movie.title
                                    movieDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Discount Type
            Text("Discount Type", color = TextPrimary, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(DiscountType.PERCENTAGE, DiscountType.FIXED_AMOUNT).forEach { type ->
                    FilterChip(
                        selected = discountType == type,
                        onClick = { discountType = type },
                        label = { Text(if (type == DiscountType.PERCENTAGE) "Percentage %" else "Fixed Amount ₹") }
                    )
                }
            }

            // Discount Fields
            if (discountType == DiscountType.PERCENTAGE) {
                AdminTextField("Discount Percentage (%)", discountPercentageStr, KeyboardType.Number) { discountPercentageStr = it }
                AdminTextField("Max Discount Amount (₹)", maxDiscountAmountStr, KeyboardType.Number) { maxDiscountAmountStr = it }
            } else {
                AdminTextField("Discount Amount (₹)", discountAmountStr, KeyboardType.Number) { discountAmountStr = it }
            }

            AdminTextField("Min Order Amount (₹)", minOrderAmountStr, KeyboardType.Number) { minOrderAmountStr = it }
            AdminTextField("Max Redemptions Per User", maxRedemptionsStr, KeyboardType.Number) { maxRedemptionsStr = it }

            // Date Selection
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = startDateStr,
                    onValueChange = {},
                    label = { Text("Start Date") },
                    readOnly = true,
                    modifier = Modifier.weight(1f),
                    trailingIcon = { IconButton(onClick = { showStartDatePicker = true }) { Icon(Icons.Default.CalendarToday, null) } }
                )
                OutlinedTextField(
                    value = endDateStr,
                    onValueChange = {},
                    label = { Text("End Date") },
                    readOnly = true,
                    modifier = Modifier.weight(1f),
                    trailingIcon = { IconButton(onClick = { showEndDatePicker = true }) { Icon(Icons.Default.CalendarToday, null) } }
                )
            }

            if (errorMessage != null) {
                Text(errorMessage!!, color = Color.Red, fontSize = 12.sp)
            }

            Button(
                onClick = {
                    if (title.isEmpty() || couponCode.isEmpty()) {
                        Toast.makeText(context, "Please fill required fields", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val startMillis = try { sdf.parse(startDateStr)?.time ?: 0L } catch(e: Exception) { 0L }
                    val endMillis = try { sdf.parse(endDateStr)?.time ?: 0L } catch(e: Exception) { 0L }

                    // Determine Category
                    val finalCategory = when {
                         category == OfferCategory.THEATRE_SPECIFIC && selectedTheatreId == "ALL" -> OfferCategory.PLATFORM_WIDE
                         category == OfferCategory.MOVIE_SPECIFIC && selectedMovieId == "ALL" -> OfferCategory.PLATFORM_WIDE
                         else -> category
                    }

                    val offer = Offer(
                        title = title,
                        description = description,
                        couponCode = couponCode.uppercase().trim(),
                        theatreId = if (selectedTheatreId == "ALL") "" else selectedTheatreId,
                        theatreName = if (selectedTheatreId == "ALL") "All Theatres" else selectedTheatreName,
                        movieId = if (selectedMovieId == "ALL") "" else selectedMovieId,
                        category = finalCategory.name,
                        couponType = couponType.name,
                        discountType = discountType.name,
                        discountPercentage = discountPercentageStr.toIntOrNull() ?: 0,
                        discountAmount = discountAmountStr.toIntOrNull() ?: 0,
                        maxDiscountAmount = maxDiscountAmountStr.toIntOrNull() ?: 0,
                        minOrderAmount = minOrderAmountStr.toIntOrNull() ?: 0,
                        maxRedemptionsPerUser = maxRedemptionsStr.toIntOrNull() ?: 1,
                        startDate = startMillis,
                        endDate = endMillis,
                        theatreOwnerId = "ADMIN_PLATFORM",
                        approvalStatus = OfferApprovalStatus.APPROVED.name,
                        isActive = true
                    )
                    viewModel.createPlatformOffer(context, offer)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else Text("Create Platform Offer", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun AdminTextField(label: String, value: String, keyboardType: KeyboardType = KeyboardType.Text, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
            focusedLabelColor = PrimaryAccent, unfocusedLabelColor = TextSecondary,
            focusedBorderColor = PrimaryAccent, unfocusedBorderColor = DividerColor
        )
    )
}
