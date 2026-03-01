package com.example.bookmymovie.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.bookmymovie.model.LibraryItem
import com.example.bookmymovie.navigation.Screen
import com.example.bookmymovie.ui.theme.*
import com.example.bookmymovie.ui.viewmodel.StreamingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyLibraryScreen(
    navController: NavController,
    streamingViewModel: StreamingViewModel
) {
    LaunchedEffect(Unit) { streamingViewModel.loadLibrary() }

    var selectedTab by remember { mutableIntStateOf(0) } // 0=All, 1=Owned, 2=Rented

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VideoLibrary, null, tint = PrimaryAccent, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("My Library", fontWeight = FontWeight.Bold, color = TextPrimary)
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
        containerColor = DeepCharcoal
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Filter tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = CardBackground,
                contentColor = PrimaryAccent,
                divider = { HorizontalDivider(color = DividerColor) }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("All", color = if (selectedTab == 0) PrimaryAccent else TextSecondary) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Owned", color = if (selectedTab == 1) PrimaryAccent else TextSecondary) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Rented", color = if (selectedTab == 2) PrimaryAccent else TextSecondary) }
                )
            }

            val filteredItems = when (selectedTab) {
                1 -> streamingViewModel.library.filter { it.type == "buy" }
                2 -> streamingViewModel.library.filter { it.type == "rent" }
                else -> streamingViewModel.library
            }

            if (streamingViewModel.isLoadingLibrary) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryAccent)
                }
            } else if (filteredItems.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.VideoLibrary, null, tint = TextSecondary, modifier = Modifier.size(56.dp))
                        Spacer(Modifier.height(16.dp))
                        Text(
                            when (selectedTab) {
                                1 -> "No owned movies"
                                2 -> "No rented movies"
                                else -> "Your library is empty"
                            },
                            color = TextSecondary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("Browse and rent/buy movies to build your library", color = TextSecondary, fontSize = 13.sp)
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { navController.navigate(Screen.StreamBrowse.route) },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text("Browse Movies")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().background(DeepCharcoal),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredItems, key = { it.purchaseId }) { item ->
                        LibraryItemCard(
                            item = item,
                            onClick = {
                                navController.navigate(Screen.StreamDetail.createRoute(item.movieId))
                            },
                            onPlay = {
                                navController.navigate(Screen.StreamPlayer.createRoute(item.movieId))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryItemCard(
    item: LibraryItem,
    onClick: () -> Unit,
    onPlay: () -> Unit
) {
    val isExpired = item.isExpired()

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpired) CardBackground.copy(alpha = 0.5f) else CardBackground
        ),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(12.dp)) {
            // Poster
            Box(
                modifier = Modifier
                    .width(90.dp)
                    .height(130.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                AsyncImage(
                    model = item.posterUrl,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Play overlay
                if (!isExpired) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.4f),
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(onClick = onPlay)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.PlayCircleFilled,
                                null,
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.title,
                    color = if (isExpired) TextSecondary else TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))

                // Type badge
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (item.type == "buy") Color(0xFF4CAF50).copy(alpha = 0.15f) else PrimaryAccent.copy(alpha = 0.15f)
                    ) {
                        Text(
                            if (item.type == "buy") "Owned" else "Rented",
                            color = if (item.type == "buy") Color(0xFF4CAF50) else PrimaryAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    if (item.ottPlatform.isNotBlank()) {
                        Surface(shape = RoundedCornerShape(4.dp), color = SecondaryBackground) {
                            Text(item.ottPlatform, color = TextSecondary, fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))

                Text("Paid: ₹${item.amountPaid.toInt()}", color = TextSecondary, fontSize = 12.sp)

                // Expiry info for rented items
                if (item.type == "rent") {
                    Spacer(Modifier.height(4.dp))
                    if (isExpired) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, null, tint = Color(0xFFF44336), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Expired", color = Color(0xFFF44336), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    } else {
                        val remaining = item.remainingDays()
                        val daysColor = if (remaining <= 3) Color(0xFFFF9800) else TextSecondary
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccessTime, null, tint = daysColor, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("$remaining days left", color = daysColor, fontSize = 12.sp)
                        }
                    }
                }

                // Play button for non-expired items
                if (!isExpired) {
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onPlay,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Play", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
