package com.example.bookmymovie.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.bookmymovie.navigation.Screen
import com.example.bookmymovie.ui.theme.*
import com.example.bookmymovie.ui.viewmodel.OwnerScreen
import com.example.bookmymovie.ui.viewmodel.ShowtimeRequest
import com.example.bookmymovie.ui.viewmodel.TheatreOwnerViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TheatreOwnerPanelScreen(navController: NavController) {
    val vm: TheatreOwnerViewModel = viewModel()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddScreenDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.loadOwnerProfile() }

    val profile = vm.ownerProfile

    if (showAddScreenDialog) {
        AddScreenDialog(
            onDismiss = { showAddScreenDialog = false },
            onAdd = { name, type, silver, gold, platinum ->
                vm.addScreen(name, type, silver, gold, platinum,
                    onSuccess = { showAddScreenDialog = false },
                    onError = { showAddScreenDialog = false }
                )
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Owner Panel", fontWeight = FontWeight.Bold, color = TextPrimary)
                        if (profile != null) {
                            Text(profile.cinemaName, color = PrimaryAccent, fontSize = 12.sp)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        FirebaseAuth.getInstance().signOut()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.TheatreOwnerPanel.route) { inclusive = true }
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, null, tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        navController.navigate(Screen.OwnerSchedule.route)
                    }) {
                        Icon(Icons.Default.Add, "Schedule Movie", tint = PrimaryAccent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepCharcoal)
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showAddScreenDialog = true },
                    containerColor = PrimaryAccent
                ) {
                    Icon(Icons.Default.Add, "Add Screen", tint = Color.White)
                }
            }
        },
        containerColor = DeepCharcoal
    ) { padding ->

        if (vm.isLoadingProfile) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryAccent)
            }
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab, containerColor = CardBackground, contentColor = PrimaryAccent,
                divider = { HorizontalDivider(color = DividerColor) }) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                    text = { Text("My Screens", color = if (selectedTab == 0) PrimaryAccent else TextSecondary) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                    text = { Text("Requests", color = if (selectedTab == 1) PrimaryAccent else TextSecondary) })
            }

            LaunchedEffect(selectedTab) {
                if (selectedTab == 1) vm.loadMyShowtimeRequests()
            }

            when (selectedTab) {
                0 -> OwnerScreensTab(vm.ownerScreens, onAddMovie = { screen ->
                    navController.navigate(Screen.OwnerSchedule.route)
                })
                1 -> OwnerRequestsTab(vm.myShowtimeRequests)
            }
        }
    }
}

// ─── Screens Tab ─────────────────────────────────────────────────────────────

@Composable
private fun OwnerScreensTab(screens: List<OwnerScreen>, onAddMovie: (OwnerScreen) -> Unit) {
    if (screens.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.ScreenShare, null, tint = TextSecondary, modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(16.dp))
                Text("No screens added yet", color = TextSecondary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text("Tap + to add a screen to your cinema", color = TextSecondary, fontSize = 13.sp)
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(DeepCharcoal),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(screens) { screen ->
            OwnerScreenCard(screen = screen, onSchedule = { onAddMovie(screen) })
        }
    }
}

@Composable
private fun OwnerScreenCard(screen: OwnerScreen, onSchedule: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ScreenShare, null, tint = PrimaryAccent, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text(screen.screenName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.weight(1f))
                Surface(shape = RoundedCornerShape(20.dp), color = PrimaryAccent.copy(alpha = 0.15f)) {
                    Text(screen.screenType, color = PrimaryAccent, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                SeatChip("Silver", screen.silverSeats, SilverSeat)
                SeatChip("Gold", screen.goldSeats, GoldSeat)
                SeatChip("Platinum", screen.platinumSeats, PlatinumSeat)
            }
            Spacer(Modifier.height(12.dp))
            Text("Total ${screen.totalSeats} seats", color = TextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onSchedule,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
            ) {
                Icon(Icons.Default.Movie, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Schedule a Movie", fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun SeatChip(label: String, count: Int, color: Color) {
    Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.15f)) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(count.toString(), color = color, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(label, color = color, fontSize = 11.sp)
        }
    }
}

// ─── Requests Tab ─────────────────────────────────────────────────────────────

@Composable
private fun OwnerRequestsTab(requests: List<ShowtimeRequest>) {
    if (requests.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Movie, null, tint = TextSecondary, modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(16.dp))
                Text("No requests submitted yet", color = TextSecondary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text("Schedule a movie to submit a request", color = TextSecondary, fontSize = 13.sp)
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(DeepCharcoal),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(requests) { req ->
            OwnerRequestCard(req)
        }
    }
}

@Composable
private fun OwnerRequestCard(req: ShowtimeRequest) {
    val (statusColor, statusIcon, statusLabel) = when (req.status) {
        "approved" -> Triple(Color(0xFF4CAF50), Icons.Default.CheckCircle, "Approved")
        "rejected" -> Triple(Color(0xFFF44336), Icons.Default.Warning, "Rejected")
        else -> Triple(Color(0xFFFF9800), Icons.Default.HourglassEmpty, "Pending Review")
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(statusIcon, null, tint = statusColor, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(statusLabel, color = statusColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(Modifier.height(10.dp))
            Text(req.movieName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoPill("${req.date}  ${req.time}")
                InfoPill(req.screenName)
                InfoPill(req.language)
            }
            if (req.status == "rejected" && req.rejectedReason.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFF44336).copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()) {
                    Text("Reason: ${req.rejectedReason}", color = Color(0xFFF44336), fontSize = 12.sp,
                        modifier = Modifier.padding(10.dp))
                }
            }
        }
    }
}

@Composable
private fun InfoPill(text: String) {
    Surface(shape = RoundedCornerShape(20.dp), color = SecondaryBackground) {
        Text(text, color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

// ─── Add Screen Dialog ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddScreenDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, Int, Int, Int) -> Unit
) {
    var screenName by remember { mutableStateOf("") }
    var screenType by remember { mutableStateOf("2D") }
    var silverSeats by remember { mutableStateOf("40") }
    var goldSeats by remember { mutableStateOf("60") }
    var platinumSeats by remember { mutableStateOf("20") }
    var typeExpanded by remember { mutableStateOf(false) }
    val screenTypes = listOf("2D", "3D", "4DX", "IMAX")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        title = { Text("Add Screen", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OwnerTextField(value = screenName, onValueChange = { screenName = it }, label = "Screen Name (e.g. Screen 1)")

                ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = it }) {
                    OutlinedTextField(
                        value = screenType, onValueChange = {},
                        readOnly = true, label = { Text("Screen Type", color = TextSecondary) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryAccent, unfocusedBorderColor = DividerColor,
                            focusedContainerColor = CardBackground, unfocusedContainerColor = CardBackground,
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                        )
                    )
                    ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false },
                        modifier = Modifier.background(CardBackground)) {
                        screenTypes.forEach { t ->
                            DropdownMenuItem(text = { Text(t, color = TextPrimary) }, onClick = { screenType = t; typeExpanded = false })
                        }
                    }
                }

                Text("Seat Configuration", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = silverSeats, onValueChange = { silverSeats = it },
                        label = { Text("Silver", color = TextSecondary, fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SilverSeat, unfocusedBorderColor = DividerColor,
                            focusedContainerColor = CardBackground, unfocusedContainerColor = CardBackground,
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = goldSeats, onValueChange = { goldSeats = it },
                        label = { Text("Gold", color = TextSecondary, fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldSeat, unfocusedBorderColor = DividerColor,
                            focusedContainerColor = CardBackground, unfocusedContainerColor = CardBackground,
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = platinumSeats, onValueChange = { platinumSeats = it },
                        label = { Text("Platinum", color = TextSecondary, fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PlatinumSeat, unfocusedBorderColor = DividerColor,
                            focusedContainerColor = CardBackground, unfocusedContainerColor = CardBackground,
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val s = silverSeats.toIntOrNull() ?: 0
                    val g = goldSeats.toIntOrNull() ?: 0
                    val p = platinumSeats.toIntOrNull() ?: 0
                    if (screenName.isNotBlank()) onAdd(screenName, screenType, s, g, p)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}
