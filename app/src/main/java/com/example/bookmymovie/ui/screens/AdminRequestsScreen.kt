package com.example.bookmymovie.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.bookmymovie.ui.theme.*
import com.example.bookmymovie.ui.viewmodel.ShowtimeRequest
import com.example.bookmymovie.ui.viewmodel.TheatreOwnerProfile
import com.example.bookmymovie.ui.viewmodel.TheatreOwnerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminRequestsScreen(navController: NavController) {
    val vm: TheatreOwnerViewModel = viewModel()
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 0) vm.loadPendingOwnerRegistrations()
        else vm.loadPendingShowtimeRequests()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pending Requests", fontWeight = FontWeight.Bold, color = TextPrimary) },
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

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = CardBackground,
                contentColor = PrimaryAccent,
                divider = { HorizontalDivider(color = DividerColor) }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Owner Requests", color = if (selectedTab == 0) PrimaryAccent else TextSecondary)
                            if (vm.pendingOwnerRegistrations.isNotEmpty()) {
                                Spacer(Modifier.width(4.dp))
                                BadgeCount(vm.pendingOwnerRegistrations.size)
                            }
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Showtime Requests", color = if (selectedTab == 1) PrimaryAccent else TextSecondary)
                            if (vm.pendingShowtimeRequests.isNotEmpty()) {
                                Spacer(Modifier.width(4.dp))
                                BadgeCount(vm.pendingShowtimeRequests.size)
                            }
                        }
                    }
                )
            }

            if (vm.isLoadingAdminData) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryAccent)
                }
                return@Scaffold
            }

            when (selectedTab) {
                0 -> OwnerRegistrationRequestsTab(vm)
                1 -> ShowtimeRequestsTab(vm)
            }
        }
    }
}

// ─── Owner Registration Requests ─────────────────────────────────────────────

@Composable
private fun OwnerRegistrationRequestsTab(vm: TheatreOwnerViewModel) {
    if (vm.pendingOwnerRegistrations.isEmpty()) {
        EmptyState(
            icon = androidx.compose.material.icons.Icons.Default.PersonAdd,
            message = "No pending owner registrations",
            subtitle = "All owner requests have been reviewed"
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(DeepCharcoal),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(vm.pendingOwnerRegistrations, key = { it.uid }) { owner ->
            OwnerRegistrationCard(owner = owner, vm = vm)
        }
    }
}

@Composable
private fun OwnerRegistrationCard(owner: TheatreOwnerProfile, vm: TheatreOwnerViewModel) {
    var showRejectDialog by remember { mutableStateOf(false) }
    var reason by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var actionTaken by remember { mutableStateOf("") } // "approved" | "rejected" | ""

    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            containerColor = CardBackground,
            title = { Text("Reject Registration", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Provide a reason for rejection:", color = TextSecondary, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reason, onValueChange = { reason = it },
                        label = { Text("Reason", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFF44336), unfocusedBorderColor = DividerColor,
                            focusedContainerColor = CardBackground, unfocusedContainerColor = CardBackground,
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isProcessing = true
                        vm.rejectOwnerRegistration(owner.uid, reason) { isProcessing = false; showRejectDialog = false; actionTaken = "rejected" }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
                ) { Text("Reject") }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = false }) { Text("Cancel", color = TextSecondary) }
            }
        )
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(22.dp), color = PrimaryAccent.copy(alpha = 0.15f),
                    modifier = Modifier.size(44.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            owner.name.firstOrNull()?.toString()?.uppercase() ?: "?",
                            color = PrimaryAccent, fontWeight = FontWeight.Bold, fontSize = 18.sp
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(owner.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(owner.email, color = TextSecondary, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = DividerColor)
            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                InfoItem(Icons.Default.Store, owner.cinemaName, Modifier.weight(1f))
                InfoItem(Icons.Default.Phone, owner.phone, Modifier.weight(1f))
            }

            if (owner.placeId.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                InfoItem(Icons.Default.LocationOn, "Place ID: ${owner.placeId}", Modifier.fillMaxWidth())
            }

            Spacer(Modifier.height(16.dp))

            if (actionTaken.isNotEmpty()) {
                val isApproved = actionTaken == "approved"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (isApproved) Color(0xFF4CAF50).copy(alpha = 0.15f) else Color(0xFFF44336).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = if (isApproved) Icons.Default.Check else Icons.Default.Close,
                        contentDescription = null,
                        tint = if (isApproved) Color(0xFF4CAF50) else Color(0xFFF44336),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isApproved) "Approved" else "Rejected",
                        color = if (isApproved) Color(0xFF4CAF50) else Color(0xFFF44336),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (isProcessing) {
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp), color = PrimaryAccent)
                        }
                    } else {
                        Button(
                            onClick = {
                                isProcessing = true
                                vm.approveOwnerRegistration(owner.uid) { isProcessing = false; actionTaken = "approved" }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Approve")
                        }
                        OutlinedButton(
                            onClick = { showRejectDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF44336))
                        ) {
                            Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp), tint = Color(0xFFF44336))
                            Spacer(Modifier.width(4.dp))
                            Text("Reject", color = Color(0xFFF44336))
                        }
                    }
                }
            }
        }
    }
}

// ─── Showtime Requests Tab ────────────────────────────────────────────────────

@Composable
private fun ShowtimeRequestsTab(vm: TheatreOwnerViewModel) {
    if (vm.pendingShowtimeRequests.isEmpty()) {
        EmptyState(
            icon = Icons.Default.Movie,
            message = "No pending showtime requests",
            subtitle = "All showtime requests have been reviewed"
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(DeepCharcoal),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(vm.pendingShowtimeRequests, key = { it.requestId }) { req ->
            ShowtimeRequestCard(req = req, vm = vm)
        }
    }
}

@Composable
private fun ShowtimeRequestCard(req: ShowtimeRequest, vm: TheatreOwnerViewModel) {
    var showRejectDialog by remember { mutableStateOf(false) }
    var reason by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var actionTaken by remember { mutableStateOf("") } // "approved" | "rejected" | ""

    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            containerColor = CardBackground,
            title = { Text("Reject Showtime", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Reason for rejection:", color = TextSecondary, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reason, onValueChange = { reason = it },
                        label = { Text("Reason", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFF44336), unfocusedBorderColor = DividerColor,
                            focusedContainerColor = CardBackground, unfocusedContainerColor = CardBackground,
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isProcessing = true
                        vm.rejectShowtimeRequest(req.requestId, reason) { isProcessing = false; showRejectDialog = false; actionTaken = "rejected" }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
                ) { Text("Reject") }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = false }) { Text("Cancel", color = TextSecondary) }
            }
        )
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (req.moviePoster.isNotBlank()) {
                    AsyncImage(
                        model = req.moviePoster, contentDescription = null,
                        modifier = Modifier.size(55.dp, 80.dp)
                            .padding(end = 12.dp),
                        contentScale = ContentScale.Crop
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(req.movieName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp))
                    Text(req.cinemaName, color = PrimaryAccent, fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = DividerColor)
            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                InfoItem(Icons.Default.ScreenShare, req.screenName, Modifier.weight(1f))
                InfoItem(Icons.Default.Language, req.language, Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                InfoItem(Icons.Default.DateRange, req.date, Modifier.weight(1f))
                InfoItem(Icons.Default.Schedule, req.time, Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                PriceLabel("Silver", req.silverPrice, SilverSeat)
                PriceLabel("Gold", req.goldPrice, GoldSeat)
                PriceLabel("Platinum", req.platinumPrice, PlatinumSeat)
            }

            Spacer(Modifier.height(16.dp))

            if (actionTaken.isNotEmpty()) {
                val isApproved = actionTaken == "approved"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (isApproved) Color(0xFF4CAF50).copy(alpha = 0.15f) else Color(0xFFF44336).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = if (isApproved) Icons.Default.Check else Icons.Default.Close,
                        contentDescription = null,
                        tint = if (isApproved) Color(0xFF4CAF50) else Color(0xFFF44336),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isApproved) "Approved" else "Rejected",
                        color = if (isApproved) Color(0xFF4CAF50) else Color(0xFFF44336),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (isProcessing) {
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp), color = PrimaryAccent)
                        }
                    } else {
                        Button(
                            onClick = {
                                isProcessing = true
                                vm.approveShowtimeRequest(req.requestId) { isProcessing = false; actionTaken = "approved" }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Approve")
                        }
                        OutlinedButton(
                            onClick = { showRejectDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF44336))
                        ) {
                            Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp), tint = Color(0xFFF44336))
                            Spacer(Modifier.width(4.dp))
                            Text("Reject", color = Color(0xFFF44336))
                        }
                    }
                }
            }
        }
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

@Composable
private fun InfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, color = TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PriceLabel(label: String, price: Double, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("₹${price.toInt()}", color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(label, color = TextSecondary, fontSize = 10.sp)
    }
}

@Composable
private fun BadgeCount(count: Int) {
    Surface(shape = RoundedCornerShape(10.dp), color = PrimaryAccent) {
        Text("$count", color = Color.White, fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
    }
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String,
    subtitle: String
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(16.dp))
            Text(message, color = TextSecondary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = TextSecondary, fontSize = 13.sp)
        }
    }
}
