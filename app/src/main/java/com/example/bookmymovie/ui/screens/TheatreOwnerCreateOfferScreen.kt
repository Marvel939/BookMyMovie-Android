package com.example.bookmymovie.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bookmymovie.ui.viewmodel.TheatreViewModel
import com.example.bookmymovie.model.Theatre
import com.example.bookmymovie.model.OfferCategory
import com.example.bookmymovie.model.OfferType
import com.example.bookmymovie.ui.viewmodel.TheatreOwnerCreateOfferViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TheatreOwnerCreateOfferScreen(
    theatreOwnerId: String = "",
    theatreOwnerName: String = "",
    cityId: String = "1",
    isAdminMode: Boolean = false,
    onBackClick: () -> Unit = {},
    onOfferCreated: () -> Unit = {},
    viewModel: TheatreOwnerCreateOfferViewModel = viewModel()
) {
    val offerTitle by viewModel.offerTitle.collectAsState()
    val offerDescription by viewModel.offerDescription.collectAsState()
    val offerType by viewModel.offerType.collectAsState()
    val offerCategory by viewModel.offerCategory.collectAsState()
    val targetMovieName by viewModel.targetMovieName.collectAsState()
    val discountValue by viewModel.discountValue.collectAsState()
    val minBookingAmount by viewModel.minBookingAmount.collectAsState()
    val validFrom by viewModel.validFrom.collectAsState()
    val validUntil by viewModel.validUntil.collectAsState()
    val selectedTheatreName by viewModel.selectedTheatreName.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val couponCodes by viewModel.couponCodes.collectAsState()

    val theatreViewModel: TheatreViewModel = viewModel()
    val theatres = theatreViewModel.theatres
    var expandedTheatreDropdown by remember { mutableStateOf(false) }

    var expandedOfferType by remember { mutableStateOf(false) }
    var expandedOfferCategory by remember { mutableStateOf(false) }
    var newCouponCode by remember { mutableStateOf("") }
    var newCouponMaxRedemptions by remember { mutableStateOf("") }
    var showFromDatePicker by remember { mutableStateOf(false) }
    var showUntilDatePicker by remember { mutableStateOf(false) }
    val availableCategories = if (isAdminMode) {
        listOf(OfferCategory.PLATFORM_WIDE, OfferCategory.BANK_PAYMENT)
    } else {
        listOf(OfferCategory.THEATRE_SPECIFIC, OfferCategory.MOVIE_SPECIFIC)
    }

    LaunchedEffect(isAdminMode) {
        if (offerCategory !in availableCategories) {
            viewModel.setOfferCategory(availableCategories.first())
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Create Offer") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp)
        ) {
            // Success message
            if (successMessage.isNotEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.tertiaryContainer)
                            .padding(12.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = successMessage,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontSize = 12.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = onOfferCreated,
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Done")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Error message
            if (errorMessage.isNotEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .padding(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Basic Info Section
            item {
                Text(
                    text = "Offer Information",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            item {
                ExposedDropdownMenuBox(
                    expanded = expandedOfferCategory,
                    onExpandedChange = { expandedOfferCategory = !expandedOfferCategory }
                ) {
                    TextField(
                        value = offerCategory.name,
                        onValueChange = {},
                        label = { Text("Offer Category") },
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedOfferCategory)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        enabled = !isLoading
                    )

                    ExposedDropdownMenu(
                        expanded = expandedOfferCategory,
                        onDismissRequest = { expandedOfferCategory = false }
                    ) {
                        availableCategories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    viewModel.setOfferCategory(category)
                                    expandedOfferCategory = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = offerTitle,
                    onValueChange = { viewModel.setOfferTitle(it) },
                    label = { Text("Offer Title") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = offerDescription,
                    onValueChange = { viewModel.setOfferDescription(it) },
                    label = { Text("Description") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    maxLines = 4,
                    enabled = !isLoading
                )

                if (offerCategory == OfferCategory.MOVIE_SPECIFIC) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = targetMovieName,
                        onValueChange = { viewModel.setTargetMovieName(it) },
                        label = { Text("Target Movie Name") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    )
                }

                if (offerCategory == OfferCategory.THEATRE_SPECIFIC) {
                    Spacer(modifier = Modifier.height(12.dp))
                    // Load theatres for the city when theatre-specific selected
                    LaunchedEffect(cityId) {
                        theatreViewModel.loadTheatres(cityId)
                    }

                    ExposedDropdownMenuBox(
                        expanded = expandedTheatreDropdown,
                        onExpandedChange = { expandedTheatreDropdown = !expandedTheatreDropdown }
                    ) {
                        TextField(
                            value = selectedTheatreName.ifEmpty { "Select Theatre" },
                            onValueChange = {},
                            label = { Text("Theatre") },
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTheatreDropdown) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            enabled = !isLoading
                        )

                        ExposedDropdownMenu(
                            expanded = expandedTheatreDropdown,
                            onDismissRequest = { expandedTheatreDropdown = false }
                        ) {
                            theatres.forEach { theatre: Theatre ->
                                DropdownMenuItem(
                                    text = { Text(theatre.name) },
                                    onClick = {
                                        viewModel.selectTheatre(theatre.theatreId, theatre.name)
                                        expandedTheatreDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (offerCategory == OfferCategory.BANK_PAYMENT) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Bank / Payment Offer",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "This category is restricted to Stripe payments only.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Offer Type & Value
            item {
                Text(
                    text = "Discount Details",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            item {
                ExposedDropdownMenuBox(
                    expanded = expandedOfferType,
                    onExpandedChange = { expandedOfferType = !expandedOfferType }
                ) {
                    TextField(
                        value = offerType.name,
                        onValueChange = {},
                        label = { Text("Offer Type") },
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedOfferType)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        enabled = !isLoading
                    )

                    ExposedDropdownMenu(
                        expanded = expandedOfferType,
                        onDismissRequest = { expandedOfferType = false }
                    ) {
                        OfferType.values().forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name) },
                                onClick = {
                                    viewModel.setOfferType(type)
                                    expandedOfferType = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = discountValue,
                    onValueChange = { viewModel.setDiscountValue(it) },
                    label = { Text(
                        when (offerType) {
                            OfferType.PERCENTAGE -> "Discount %"
                            OfferType.FIXED_AMOUNT -> "Discount Amount (₹)"
                            OfferType.BUY_GET -> "Discount Value"
                        }
                    ) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = minBookingAmount,
                    onValueChange = { viewModel.setMinBookingAmount(it) },
                    label = { Text("Min Booking Amount (₹)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Date Selection
            item {
                Text(
                    text = "Validity Period",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showFromDatePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("From: ${formatDate(validFrom)}")
                    }

                    OutlinedButton(
                        onClick = { showUntilDatePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Until: ${formatDate(validUntil)}")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Coupons Section
            item {
                Text(
                    text = "Coupon Codes",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Text(
                    text = "Add at least one coupon code for this offer",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        OutlinedTextField(
                            value = newCouponCode,
                            onValueChange = { newCouponCode = it.uppercase() },
                            label = { Text("Coupon Code") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = newCouponMaxRedemptions,
                            onValueChange = { newCouponMaxRedemptions = it },
                            label = { Text("Max Redemptions (0 = unlimited)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            enabled = !isLoading
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                if (newCouponCode.isNotEmpty()) {
                                    viewModel.addCouponCode(
                                        newCouponCode,
                                        newCouponMaxRedemptions.toIntOrNull() ?: 0
                                    )
                                    newCouponCode = ""
                                    newCouponMaxRedemptions = ""
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.End)
                                .height(36.dp),
                            enabled = newCouponCode.isNotEmpty() && !isLoading
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.width(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Added coupons list
            items(couponCodes) { couponCode ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = couponCode,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = {
                                viewModel.removeCouponCode(couponCodes.indexOf(couponCode))
                            }
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "Remove")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            if (couponCodes.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Submit button
            item {
                Button(
                    onClick = {
                        viewModel.submitOffer(theatreOwnerId, theatreOwnerName, cityId, isAdminMode)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled = !isLoading
                ) {
                    Text(if (isLoading) "Submitting..." else "Submit for Approval")
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Date Picker Dialogs
    if (showFromDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = validFrom,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis >= System.currentTimeMillis()
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showFromDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { viewModel.setValidFrom(it) }
                    showFromDatePicker = false
                }) {
                    Text("OK")
                }
            }
        ) {
            androidx.compose.material3.DatePicker(state = state)
        }
    }

    if (showUntilDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = validUntil,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis >= validFrom
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showUntilDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { viewModel.setValidUntil(it) }
                    showUntilDatePicker = false
                }) {
                    Text("OK")
                }
            }
        ) {
            androidx.compose.material3.DatePicker(state = state)
        }
    }
}

private fun formatDate(timeMillis: Long): String {
    val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timeMillis))
}
