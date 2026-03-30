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
import androidx.compose.material.icons.filled.LocalOffer
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
import com.example.bookmymovie.model.Offer
import com.example.bookmymovie.ui.theme.*
import com.example.bookmymovie.ui.viewmodel.TheatreOwnerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TheatreOwnerOffersScreen(navController: NavController) {
    val vm: TheatreOwnerViewModel = viewModel()
    
    LaunchedEffect(Unit) {
        vm.loadOwnerOffers()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Offers", fontWeight = FontWeight.Bold, color = TextPrimary) },
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
        if (vm.ownerOffers.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.LocalOffer, null, tint = TextSecondary, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("No offers created yet", color = TextSecondary, fontSize = 16.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(vm.ownerOffers) { offer ->
                    OwnerOfferCard(offer)
                }
            }
        }
    }
}

@Composable
private fun OwnerOfferCard(offer: Offer) {
    val statusColor = when (offer.approvalStatus.lowercase()) {
        "approved" -> Color(0xFF4CAF50)
        "rejected" -> Color(0xFFF44336)
        else -> Color(0xFFFF9800)
    }
    
    val statusIcon = when (offer.approvalStatus.lowercase()) {
        "approved" -> Icons.Default.CheckCircle
        "rejected" -> Icons.Default.Warning
        else -> Icons.Default.HourglassEmpty
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
                Text(
                    offer.approvalStatus.replaceFirstChar { it.uppercase() },
                    color = statusColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(Modifier.weight(1f))
            }
            
            Spacer(Modifier.height(12.dp))
            
            Text(
                offer.title,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                offer.description,
                color = TextSecondary,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            
            Spacer(Modifier.height(8.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PrimaryAccent.copy(alpha = 0.1f)
                ) {
                    Text(
                        "Code: ${offer.couponCode}",
                        color = PrimaryAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "${offer.theatreName}",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            if (offer.discountPercentage > 0 || offer.discountAmount > 0) {
                Spacer(Modifier.height(8.dp))
                val discountText = if (offer.discountType.lowercase() == "percentage") {
                    "${offer.discountPercentage}% OFF"
                } else {
                    "₹${offer.discountAmount} OFF"
                }
                Text(
                    discountText,
                    color = PrimaryAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}
