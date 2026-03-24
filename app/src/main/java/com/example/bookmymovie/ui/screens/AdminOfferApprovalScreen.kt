package com.example.bookmymovie.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.bookmymovie.model.Offer
import com.example.bookmymovie.model.OfferApprovalStatus
import com.example.bookmymovie.ui.theme.*
import com.example.bookmymovie.ui.viewmodel.OfferAdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOfferApprovalScreen(
    navController: NavController,
    viewModel: OfferAdminViewModel = viewModel()
) {
    val pendingOffers by viewModel.pendingOffers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pending Offers", color = TextPrimary) },
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
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = PrimaryAccent)
            } else if (pendingOffers.isEmpty()) {
                Text(
                    text = "No pending offers to approve.",
                    color = TextSecondary,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(pendingOffers) { offer ->
                        AdminApprovalOfferCard(
                            offer = offer,
                            onApprove = { viewModel.updateOfferStatus(offer.id, OfferApprovalStatus.APPROVED) },
                            onReject = { viewModel.updateOfferStatus(offer.id, OfferApprovalStatus.REJECTED) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdminApprovalOfferCard(offer: Offer, onApprove: () -> Unit, onReject: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(offer.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(offer.description, color = TextSecondary, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Coupon: ${offer.couponCode}", color = PrimaryAccent, fontWeight = FontWeight.Bold)
            Text("Theatre: ${offer.theatreName}", color = TextSecondary, fontSize = 12.sp)
            
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onApprove,
                    colors = ButtonDefaults.buttonColors(containerColor = SafetyGreen),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Approve", color = TextPrimary)
                }
                OutlinedButton(
                    onClick = onReject,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reject")
                }
            }
        }
    }
}
