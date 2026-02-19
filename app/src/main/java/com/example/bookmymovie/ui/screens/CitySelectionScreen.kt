package com.example.bookmymovie.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bookmymovie.ui.theme.*
import com.example.bookmymovie.ui.viewmodel.LocationViewModel

/**
 * City selection screen — shown when no city is saved or when user
 * wants to change city. Supports GPS auto-detection and manual selection.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitySelectionScreen(
    locationViewModel: LocationViewModel,
    onCitySelected: (String) -> Unit,
    onRequestPermission: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredCities = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            locationViewModel.supportedCities
        } else {
            locationViewModel.supportedCities.filter {
                it.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Select Your City",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepCharcoal
                )
            )
        },
        containerColor = DeepCharcoal
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // GPS detection card
            item {
                Spacer(modifier = Modifier.height(8.dp))
                GPSDetectionCard(
                    locationViewModel = locationViewModel,
                    onRequestPermission = onRequestPermission,
                    onCityDetected = onCitySelected
                )
            }

            // Divider
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Divider(
                        modifier = Modifier.weight(1f),
                        color = DividerColor,
                        thickness = 1.dp
                    )
                    Text(
                        "  OR SELECT MANUALLY  ",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Divider(
                        modifier = Modifier.weight(1f),
                        color = DividerColor,
                        thickness = 1.dp
                    )
                }
            }

            // Search bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search city...", color = TextSecondary) },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = TextSecondary)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = TextSecondary)
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PrimaryAccent,
                        unfocusedBorderColor = DividerColor,
                        cursorColor = PrimaryAccent,
                        focusedContainerColor = CardBackground,
                        unfocusedContainerColor = CardBackground
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // Popular cities header
            item {
                Text(
                    "Popular Cities",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // City list
            items(filteredCities) { city ->
                CityListItem(
                    city = city,
                    isSelected = city == locationViewModel.selectedCity,
                    onClick = {
                        locationViewModel.selectCity(city)
                        onCitySelected(city)
                    }
                )
            }

            // Empty state
            if (filteredCities.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No cities found matching \"$searchQuery\"",
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun GPSDetectionCard(
    locationViewModel: LocationViewModel,
    onRequestPermission: () -> Unit,
    onCityDetected: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                if (locationViewModel.hasPermission) {
                    locationViewModel.detectCityFromGPS()
                } else {
                    onRequestPermission()
                }
            },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            PrimaryAccent.copy(alpha = 0.8f),
                            PrimaryAccent.copy(alpha = 0.4f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.MyLocation,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Detect My Location",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (locationViewModel.hasPermission)
                            "Tap to auto-detect your city via GPS"
                        else
                            "Grant location permission to auto-detect",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )
                }
                if (locationViewModel.isDetectingLocation) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        }
    }

    // Error message
    locationViewModel.locationError?.let { error ->
        Text(
            text = error,
            color = PrimaryAccent,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 8.dp, start = 4.dp)
        )
    }

    // Auto-navigate when city detected via GPS
    LaunchedEffect(locationViewModel.selectedCity, locationViewModel.locationMethod) {
        if (locationViewModel.locationMethod == "gps" && locationViewModel.selectedCity != null) {
            onCityDetected(locationViewModel.selectedCity!!)
        }
    }
}

@Composable
private fun CityListItem(
    city: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) PrimaryAccent.copy(alpha = 0.15f) else CardBackground,
        label = "cityBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) PrimaryAccent else Color.Transparent,
        label = "cityBorder"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,
            brush = Brush.linearGradient(listOf(PrimaryAccent, PrimaryAccent))
        ) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.LocationCity,
                contentDescription = null,
                tint = if (isSelected) PrimaryAccent else TextSecondary,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = city,
                color = if (isSelected) PrimaryAccent else TextPrimary,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = "Selected",
                    tint = PrimaryAccent,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
