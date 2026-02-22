package com.example.bookmymovie.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.bookmymovie.model.FoodItem
import com.example.bookmymovie.navigation.Screen
import com.example.bookmymovie.ui.theme.*
import com.example.bookmymovie.ui.viewmodel.BookingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodBeverageScreen(
    navController: NavController,
    bookingViewModel: BookingViewModel
) {
    LaunchedEffect(Unit) { bookingViewModel.loadFoodMenu() }

    val categories = listOf("Snacks", "Beverages", "Combos")
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Food & Beverages", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepCharcoal)
            )
        },
        bottomBar = {
            Surface(color = CardBackground, shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val movieId  = bookingViewModel.selectedShowtime?.movieId  ?: ""
                    val movieName= bookingViewModel.selectedShowtime?.movieName ?: ""
                    val moviePoster = bookingViewModel.selectedShowtime?.moviePoster ?: ""
                    Column {
                        Text("Food Total", color = TextSecondary, fontSize = 12.sp)
                        Text("₹${bookingViewModel.foodAmount}", color = PrimaryAccent, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Button(
                        onClick = { navController.navigate(Screen.BookingSummary.route) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Review Order", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        containerColor = DeepCharcoal
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // ── Tabs ─────────────────────────────────────────────────────────
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = CardBackground,
                contentColor = PrimaryAccent
            ) {
                categories.forEachIndexed { idx, cat ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = { Text(cat, color = if (selectedTab == idx) PrimaryAccent else TextSecondary) }
                    )
                }
            }

            when {
                bookingViewModel.isLoadingFood -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryAccent)
                    }
                }
                else -> {
                    val filteredItems = bookingViewModel.foodItems.filter {
                        it.category == categories[selectedTab] && it.available
                    }
                    if (filteredItems.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No items in this category", color = TextSecondary)
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredItems) { item ->
                                FoodItemCard(
                                    item = item,
                                    qty = bookingViewModel.foodCart[item.itemId] ?: 0,
                                    onQtyChange = { bookingViewModel.updateFoodQty(item.itemId, it) }
                                )
                            }
                            item { Spacer(Modifier.height(72.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FoodItemCard(
    item: FoodItem,
    qty: Int,
    onQtyChange: (Int) -> Unit
) {
    Surface(shape = RoundedCornerShape(16.dp), color = CardBackground, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (item.imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.name,
                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.size(72.dp).background(DividerColor, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🍿", fontSize = 28.sp)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                if (item.description.isNotEmpty()) {
                    Text(item.description, color = TextSecondary, fontSize = 12.sp, maxLines = 2)
                }
                Spacer(Modifier.height(4.dp))
                Text("₹${item.price}", color = PrimaryAccent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Spacer(Modifier.width(12.dp))
            // Quantity stepper
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { if (qty > 0) onQtyChange(qty - 1) },
                    modifier = Modifier.size(32.dp),
                    enabled = qty > 0
                ) {
                    Icon(Icons.Default.Remove, null, tint = if (qty > 0) PrimaryAccent else TextSecondary, modifier = Modifier.size(18.dp))
                }
                Text(qty.toString(), color = TextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.widthIn(min = 24.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                IconButton(
                    onClick = { onQtyChange(qty + 1) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Add, null, tint = PrimaryAccent, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
