package com.example.bookmymovie.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.bookmymovie.model.CouponType
import com.example.bookmymovie.model.DiscountType
import com.example.bookmymovie.model.Offer
import com.example.bookmymovie.model.OfferApprovalStatus
import com.example.bookmymovie.model.OfferCategory
import com.example.bookmymovie.ui.theme.*
import com.example.bookmymovie.ui.viewmodel.TheatreOwnerCreateOfferViewModel
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TheatreOwnerCreateOfferScreen(
    navController: NavController,
    theatreOwnerId: String,
    viewModel: TheatreOwnerCreateOfferViewModel = viewModel()
) {
    val context = LocalContext.current
    val ownerTheatres by viewModel.ownerTheatres.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val creationSuccess by viewModel.creationSuccess.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val movies by viewModel.movies.collectAsState()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var couponCode by remember { mutableStateOf("") }
    var couponType by remember { mutableStateOf(CouponType.DISCOUNT) }
    var discountType by remember { mutableStateOf(DiscountType.PERCENTAGE) }
    var category by remember { mutableStateOf(OfferCategory.THEATRE_SPECIFIC) }
    
    var discountPercentageStr by remember { mutableStateOf("") }
    var discountAmountStr by remember { mutableStateOf("") }
    var minOrderAmountStr by remember { mutableStateOf("") }
    var maxDiscountAmountStr by remember { mutableStateOf("") }
    var maxRedemptionsStr by remember { mutableStateOf("1") }
    
    var selectedTheatreId by remember { mutableStateOf("") }
    var selectedTheatreName by remember { mutableStateOf("Select Theatre") }
    var theatreDropdownExpanded by remember { mutableStateOf(false) }

    var selectedMovieId by remember { mutableStateOf("") }
    var selectedMovieName by remember { mutableStateOf("Select Movie") }
    var movieDropdownExpanded by remember { mutableStateOf(false) }

    // Using DatePicker for dates
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    
    val datePickerFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    
    var startDateStr by remember { mutableStateOf("") }
    var endDateStr by remember { mutableStateOf("") }
    
    val startDatePickerState = rememberDatePickerState()
    val endDatePickerState = rememberDatePickerState()

    LaunchedEffect(Unit) {
        viewModel.fetchTheatresForOwner(theatreOwnerId)
    }

    LaunchedEffect(creationSuccess) {
        if (creationSuccess) {
            Toast.makeText(context, "Offer created successfully! Pending Admin approval.", Toast.LENGTH_LONG).show()
            navController.popBackStack()
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Offer", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepCharcoal)
            )
        },
        containerColor = BackgroundColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Offer Details", color = TextPrimary, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Offer Title") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedLabelColor = PrimaryAccent,
                    unfocusedLabelColor = TextSecondary,
                    focusedBorderColor = PrimaryAccent,
                    unfocusedBorderColor = DividerColor
                )
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                    focusedLabelColor = PrimaryAccent, unfocusedLabelColor = TextSecondary,
                    focusedBorderColor = PrimaryAccent, unfocusedBorderColor = DividerColor
                )
            )

            OutlinedTextField(
                value = couponCode,
                onValueChange = { couponCode = it.uppercase() },
                label = { Text("Coupon Code") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                    focusedLabelColor = PrimaryAccent, unfocusedLabelColor = TextSecondary,
                    focusedBorderColor = PrimaryAccent, unfocusedBorderColor = DividerColor
                )
            )

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

            // Theatre Selector
            ExposedDropdownMenuBox(
                expanded = theatreDropdownExpanded,
                onExpandedChange = { theatreDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedTheatreName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Theatre") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = theatreDropdownExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                        focusedLabelColor = PrimaryAccent, unfocusedLabelColor = TextSecondary,
                        focusedBorderColor = PrimaryAccent, unfocusedBorderColor = DividerColor
                    )
                )
                ExposedDropdownMenu(
                    expanded = theatreDropdownExpanded,
                    onDismissRequest = { theatreDropdownExpanded = false }
                ) {
                    ownerTheatres.forEach { theatre ->
                        DropdownMenuItem(
                            text = { Text(theatre.name, color = TextPrimary) },
                            onClick = {
                                selectedTheatreId = theatre.id
                                selectedTheatreName = theatre.name
                                theatreDropdownExpanded = false
                            }
                        )
                    }
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

            // Movie Selector (Only for MOVIE_SPECIFIC)
            if (category == OfferCategory.MOVIE_SPECIFIC) {
                Text("Select Movie", color = TextPrimary, fontWeight = FontWeight.Bold)
                ExposedDropdownMenuBox(
                    expanded = movieDropdownExpanded,
                    onExpandedChange = { movieDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedMovieName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Search/Select Movie") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = movieDropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                            focusedLabelColor = PrimaryAccent, unfocusedLabelColor = TextSecondary,
                            focusedBorderColor = PrimaryAccent, unfocusedBorderColor = DividerColor
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = movieDropdownExpanded,
                        onDismissRequest = { movieDropdownExpanded = false }
                    ) {
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
                OutlinedTextField(
                    value = discountPercentageStr,
                    onValueChange = { discountPercentageStr = it },
                    label = { Text("Discount Percentage (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                        focusedLabelColor = PrimaryAccent, unfocusedLabelColor = TextSecondary,
                        focusedBorderColor = PrimaryAccent, unfocusedBorderColor = DividerColor
                    )
                )
                OutlinedTextField(
                    value = maxDiscountAmountStr,
                    onValueChange = { maxDiscountAmountStr = it },
                    label = { Text("Max Discount Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                        focusedLabelColor = PrimaryAccent, unfocusedLabelColor = TextSecondary,
                        focusedBorderColor = PrimaryAccent, unfocusedBorderColor = DividerColor
                    )
                )
            } else {
                OutlinedTextField(
                    value = discountAmountStr,
                    onValueChange = { discountAmountStr = it },
                    label = { Text("Discount Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                        focusedLabelColor = PrimaryAccent, unfocusedLabelColor = TextSecondary,
                        focusedBorderColor = PrimaryAccent, unfocusedBorderColor = DividerColor
                    )
                )
            }

            // Common constraints
            OutlinedTextField(
                value = minOrderAmountStr,
                onValueChange = { minOrderAmountStr = it },
                label = { Text("Minimum Order Amount Required (₹)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                    focusedLabelColor = PrimaryAccent, unfocusedLabelColor = TextSecondary,
                    focusedBorderColor = PrimaryAccent, unfocusedBorderColor = DividerColor
                )
            )
            
            OutlinedTextField(
                value = maxRedemptionsStr,
                onValueChange = { maxRedemptionsStr = it },
                label = { Text("Max Redemptions Per User") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                    focusedLabelColor = PrimaryAccent, unfocusedLabelColor = TextSecondary,
                    focusedBorderColor = PrimaryAccent, unfocusedBorderColor = DividerColor
                )
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = startDateStr,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Start Date") },
                        placeholder = { Text("yyyy-MM-dd") },
                        modifier = Modifier.fillMaxWidth().clickable { showStartDatePicker = true },
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = TextPrimary,
                            disabledLabelColor = TextSecondary,
                            disabledBorderColor = DividerColor,
                            disabledContainerColor = Color.Transparent,
                            disabledPlaceholderColor = TextSecondary
                        )
                    )
                    // Transparent overlay to capture clicks since enabled=false
                    Box(Modifier.matchParentSize().clickable { showStartDatePicker = true })
                }

                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = endDateStr,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("End Date") },
                        placeholder = { Text("yyyy-MM-dd") },
                        modifier = Modifier.fillMaxWidth().clickable { showEndDatePicker = true },
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = TextPrimary,
                            disabledLabelColor = TextSecondary,
                            disabledBorderColor = DividerColor,
                            disabledContainerColor = Color.Transparent,
                            disabledPlaceholderColor = TextSecondary
                        )
                    )
                    // Transparent overlay to capture clicks since enabled=false
                    Box(Modifier.matchParentSize().clickable { showEndDatePicker = true })
                }
            }

            // Date Picker Dialogs
            if (showStartDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showStartDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            startDatePickerState.selectedDateMillis?.let {
                                startDateStr = datePickerFormat.format(Date(it))
                            }
                            showStartDatePicker = false
                        }) { Text("OK", color = PrimaryAccent) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel", color = TextSecondary) }
                    }
                ) {
                    DatePicker(state = startDatePickerState)
                }
            }

            if (showEndDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showEndDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            endDatePickerState.selectedDateMillis?.let {
                                endDateStr = datePickerFormat.format(Date(it))
                            }
                            showEndDatePicker = false
                        }) { Text("OK", color = PrimaryAccent) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEndDatePicker = false }) { Text("Cancel", color = TextSecondary) }
                    }
                ) {
                    DatePicker(state = endDatePickerState)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (title.isEmpty() || couponCode.isEmpty() || selectedTheatreId.isEmpty()) {
                        Toast.makeText(context, "Please fill out required fields", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (category == OfferCategory.MOVIE_SPECIFIC && selectedMovieId.isEmpty()) {
                        Toast.makeText(context, "Please select a movie for this offer", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val startMillis = try { sdf.parse(startDateStr)?.time ?: 0L } catch(e: Exception) { 0L }
                    val endMillis = try { sdf.parse(endDateStr)?.time ?: 0L } catch(e: Exception) { 0L }

                    val offer = Offer(
                        title = title,
                        description = description,
                        couponCode = couponCode.uppercase().trim(),
                        theatreOwnerId = theatreOwnerId,
                        theatreId = selectedTheatreId,
                        theatreName = selectedTheatreName,
                        movieId = if (category == OfferCategory.MOVIE_SPECIFIC) selectedMovieId else "",
                        category = category.name,
                        couponType = couponType.name,
                        discountType = discountType.name,
                        discountPercentage = discountPercentageStr.toIntOrNull() ?: 0,
                        discountAmount = discountAmountStr.toIntOrNull() ?: 0,
                        maxDiscountAmount = maxDiscountAmountStr.toIntOrNull() ?: 0,
                        minOrderAmount = minOrderAmountStr.toIntOrNull() ?: 0,
                        maxRedemptionsPerUser = maxRedemptionsStr.toIntOrNull() ?: 1,
                        startDate = startMillis,
                        endDate = endMillis
                    )
                    viewModel.createOffer(offer)
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = DeepCharcoal)
                } else {
                    Text("Submit for Approval", color = DeepCharcoal, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
