package com.example.bookmymovie.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.bookmymovie.navigation.Screen
import com.example.bookmymovie.ui.theme.*
import com.example.bookmymovie.ui.viewmodel.BookingViewModel
import com.example.bookmymovie.ui.viewmodel.OwnerScreen
import com.example.bookmymovie.ui.viewmodel.ShowtimeRequest
import com.example.bookmymovie.ui.viewmodel.TheatreOwnerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    navController: NavController,
    bookingViewModel: BookingViewModel
) {
    LaunchedEffect(Unit) { bookingViewModel.checkAdminStatus() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Panel", fontWeight = FontWeight.Bold, color = TextPrimary) },
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
        LaunchedEffect(bookingViewModel.isAdmin) {
            if (!bookingViewModel.isAdmin) {
                navController.navigate(Screen.AdminAuth.route) {
                    popUpTo(Screen.AdminPanel.route) { inclusive = true }
                }
            }
        }
        if (!bookingViewModel.isAdmin) return@Scaffold

        val ownerVm: TheatreOwnerViewModel = viewModel()
        val tabs = listOf("Screens", "Showtimes", "Food", "Requests", "Stream")
        var selectedTab by remember { mutableIntStateOf(0) }

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(selectedTabIndex = selectedTab, containerColor = CardBackground, contentColor = PrimaryAccent, edgePadding = 0.dp) {
                tabs.forEachIndexed { idx, t ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = { Text(t, color = if (selectedTab == idx) PrimaryAccent else TextSecondary) }
                    )
                }
            }
            when (selectedTab) {
                0 -> ApprovedScreensTab(ownerVm)
                1 -> ApprovedShowtimesTab(ownerVm)
                2 -> AddFoodTab(bookingViewModel)
                3 -> {
                    LaunchedEffect(Unit) {
                        navController.navigate(Screen.AdminRequests.route)
                    }
                }
                4 -> {
                    LaunchedEffect(Unit) {
                        navController.navigate(Screen.AdminStreamingCatalog.route)
                    }
                }
            }
        }
    }
}

// ─── Approved Screens Tab ────────────────────────────────────────────────────────

@Composable
private fun ApprovedScreensTab(vm: TheatreOwnerViewModel) {
    LaunchedEffect(Unit) { vm.loadAllScreens() }
    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        when {
            vm.isLoadingAllScreens -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = PrimaryAccent
            )
            vm.allScreens.isEmpty() -> Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("No screens registered yet.", color = TextSecondary, fontSize = 14.sp)
                Text("Theatre owners add screens from their panel.", color = TextSecondary, fontSize = 12.sp)
            }
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(vm.allScreens) { screen -> ScreenInfoCard(screen) }
            }
        }
    }
}

@Composable
private fun ScreenInfoCard(screen: OwnerScreen) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(screen.screenName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = PrimaryAccent.copy(alpha = 0.15f)
                ) {
                    Text(
                        screen.screenType,
                        color = PrimaryAccent,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text("Cinema ID: ${screen.placeId}", color = TextSecondary, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(screen.silverSeats.toString(), color = SilverSeat, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Silver", color = TextSecondary, fontSize = 10.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(screen.goldSeats.toString(), color = GoldSeat, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Gold", color = TextSecondary, fontSize = 10.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(screen.platinumSeats.toString(), color = PlatinumSeat, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Platinum", color = TextSecondary, fontSize = 10.sp)
                }
            }
            Text("Total: ${screen.totalSeats} seats", color = TextSecondary, fontSize = 12.sp)
        }
    }
}

// ─── Approved Showtimes Tab ───────────────────────────────────────────────────────

@Composable
private fun ApprovedShowtimesTab(vm: TheatreOwnerViewModel) {
    LaunchedEffect(Unit) { vm.loadApprovedShowtimes() }
    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        when {
            vm.isLoadingApprovedShowtimes -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = PrimaryAccent
            )
            vm.approvedShowtimesList.isEmpty() -> Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("No approved showtimes yet.", color = TextSecondary, fontSize = 14.sp)
                Text("Approve owner requests from the Requests tab.", color = TextSecondary, fontSize = 12.sp)
            }
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(vm.approvedShowtimesList) { showtime -> ApprovedShowtimeCard(showtime) }
            }
        }
    }
}

@Composable
private fun ApprovedShowtimeCard(st: ShowtimeRequest) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            if (st.moviePoster.isNotBlank()) {
                AsyncImage(
                    model = st.moviePoster,
                    contentDescription = null,
                    modifier = Modifier
                        .size(60.dp, 88.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    st.movieName,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(st.cinemaName, color = PrimaryAccent, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${st.screenName} • ${st.screenType} • ${st.language}", color = TextSecondary, fontSize = 11.sp)
                Text("${st.date}   ${st.time}", color = TextSecondary, fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("₹${st.silverPrice.toInt()} Silver", color = SilverSeat, fontSize = 11.sp)
                    Text("₹${st.goldPrice.toInt()} Gold", color = GoldSeat, fontSize = 11.sp)
                    Text("₹${st.platinumPrice.toInt()} Platinum", color = PlatinumSeat, fontSize = 11.sp)
                }
            }
        }
    }
}

// ─── Add Food Tab ────────────────────────────────────────────────────────────────

@Composable
private fun AddFoodTab(vm: BookingViewModel) {
    var name        by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price       by remember { mutableStateOf("") }
    var category    by remember { mutableStateOf("Snacks") }
    var imageUrl    by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Add Food Item", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)

        AdminField("Item Name", name) { name = it }
        AdminField("Description", description) { description = it }
        AdminField("Price (₹)", price, keyboardType = KeyboardType.Number) { price = it }
        AdminField("Category (Snacks / Beverages / Combos)", category) { category = it }
        AdminField("Image URL (optional)", imageUrl) { imageUrl = it }

        vm.adminActionError?.let { Text(it, color = PrimaryAccent, fontSize = 12.sp) }
        vm.adminActionSuccess?.let { Text(it, color = Color(0xFF2ECC71), fontSize = 12.sp) }

        Button(
            onClick = {
                vm.addFoodItem(
                    name = name.trim(),
                    description = description.trim(),
                    price = price.toIntOrNull() ?: 0,
                    category = category.trim(),
                    imageUrl = imageUrl.trim()
                )
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Add Food Item", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

// ─── Helper ──────────────────────────────────────────────────────────────────────

@Composable
private fun AdminField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextSecondary, fontSize = 12.sp) },
        modifier = modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PrimaryAccent,
            unfocusedBorderColor = DividerColor,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            cursorColor = PrimaryAccent
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        shape = RoundedCornerShape(10.dp)
    )
}
