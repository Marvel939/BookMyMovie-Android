package com.example.bookmymovie.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.bookmymovie.ui.theme.*
import com.example.bookmymovie.ui.viewmodel.ShowtimeRequest
import com.example.bookmymovie.ui.viewmodel.TheatreOwnerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TheatreOwnerScheduledMoviesScreen(navController: NavController) {
    val vm: TheatreOwnerViewModel = viewModel()
    
    LaunchedEffect(Unit) {
        vm.loadMyShowtimeRequests()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scheduled Movies", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepCharcoal)
            )
        },
        containerColor = DeepCharcoal
    ) { padding ->
        if (vm.myShowtimeRequests.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Movie, null, tint = TextSecondary, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("No movies scheduled yet", color = TextSecondary, fontSize = 16.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(vm.myShowtimeRequests) { req ->
                    SummaryRequestCard(req)
                }
            }
        }
    }
}

@Composable
private fun SummaryRequestCard(req: ShowtimeRequest) {
    val (statusColor, statusIcon, statusLabel) = when (req.status.lowercase()) {
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
                Icon(statusIcon, null, tint = statusColor, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(statusLabel, color = statusColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Text(req.movieName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                InfoPill("${req.date}  |  ${req.time}")
                InfoPill(req.screenName)
            }
            if (req.status.lowercase() == "rejected" && req.rejectedReason.isNotBlank()) {
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
