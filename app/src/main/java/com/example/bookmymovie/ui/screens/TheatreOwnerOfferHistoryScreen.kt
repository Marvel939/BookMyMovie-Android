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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bookmymovie.model.ApprovalStatus
import com.example.bookmymovie.model.OfferApproval
import com.example.bookmymovie.ui.viewmodel.TheatreOwnerCreateOfferViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TheatreOwnerOfferHistoryScreen(
    theatreOwnerId: String,
    onBackClick: () -> Unit = {},
    onCreateNewOffer: () -> Unit = {},
    viewModel: TheatreOwnerCreateOfferViewModel = viewModel()
) {
    val offers by viewModel.theatreOwnerOffers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var activeTab by remember { mutableStateOf(0) }

    LaunchedEffect(key1 = theatreOwnerId) {
        viewModel.loadTheatreOwnerOffers(theatreOwnerId)
    }

    val pendingOffers = offers.filter { it.status == ApprovalStatus.PENDING.name }
    val approvedOffers = offers.filter { it.status == ApprovalStatus.APPROVED.name }
    val rejectedOffers = offers.filter { it.status == ApprovalStatus.REJECTED.name }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("My Offers") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab selection
            TabRow(
                selectedTabIndex = activeTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("Pending (${pendingOffers.size})") }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("Approved (${approvedOffers.size})") }
                )
                Tab(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    text = { Text("Rejected (${rejectedOffers.size})") }
                )
            }

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                activeTab == 0 -> {
                    // Pending tab
                    if (pendingOffers.isEmpty()) {
                        EmptyOfferState("No pending offers")
                    } else {
                        LazyColumn(contentPadding = PaddingValues(12.dp)) {
                            items(pendingOffers) { approval ->
                                OfferHistoryCard(approval = approval)
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }

                activeTab == 1 -> {
                    // Approved tab
                    if (approvedOffers.isEmpty()) {
                        EmptyOfferState("No approved offers yet")
                    } else {
                        LazyColumn(contentPadding = PaddingValues(12.dp)) {
                            items(approvedOffers) { approval ->
                                OfferHistoryCard(
                                    approval = approval,
                                    statusColor = MaterialTheme.colorScheme.tertiary
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }

                else -> {
                    // Rejected tab
                    if (rejectedOffers.isEmpty()) {
                        EmptyOfferState("No rejected offers")
                    } else {
                        LazyColumn(contentPadding = PaddingValues(12.dp)) {
                            items(rejectedOffers) { approval ->
                                OfferHistoryCard(
                                    approval = approval,
                                    statusColor = MaterialTheme.colorScheme.error,
                                    showReason = true
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OfferHistoryCard(
    approval: OfferApproval,
    statusColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    showReason: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = approval.offer?.title ?: "Unknown Offer",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = approval.offer?.description ?: "",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }

                Box(
                    modifier = Modifier
                        .background(
                            color = statusColor,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = approval.status,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Offer details
            approval.offer?.let { offer ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Discount:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        when (offer.type) {
                            "PERCENTAGE" -> "${offer.value.toInt()}% OFF"
                            "FIXED_AMOUNT" -> "₹${offer.value.toInt()} OFF"
                            else -> "Special Offer"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Min Amount:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "₹${offer.minBookingAmount.toInt()}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Valid Until:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        formatDate(offer.validUntil),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Rejection reason if rejected
            if (showReason && approval.rejectionReason.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "Rejection Reason",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = approval.rejectionReason,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyOfferState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Check back soon!",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    return try {
        val sdf = java.text.SimpleDateFormat("dd MMM, yyyy", java.util.Locale.getDefault())
        sdf.format(java.util.Date(timestamp))
    } catch (e: Exception) {
        "--"
    }
}
