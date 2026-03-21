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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bookmymovie.model.ApprovalStatus
import com.example.bookmymovie.model.OfferApproval
import com.example.bookmymovie.ui.viewmodel.OfferAdminViewModel

@Composable
fun AdminOfferApprovalScreen(
    adminId: String = "",
    onCreateOfferClick: () -> Unit = {},
    viewModel: OfferAdminViewModel = viewModel()
) {
    val pendingApprovals by viewModel.pendingApprovals.collectAsState()
    val approvalHistory by viewModel.approvalHistory.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()

    var activeTab by remember { mutableStateOf(0) }
    var selectedApproval by remember { mutableStateOf<OfferApproval?>(null) }
    var showRejectionDialog by remember { mutableStateOf(false) }
    var rejectionReason by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadPendingApprovals()
        viewModel.loadApprovalHistory()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Manage Offers",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Review and approve theatre owner offers",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }

                    IconButton(onClick = onCreateOfferClick) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create Admin Offer",
                            tint = Color.White
                        )
                    }
                }
            }
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
                    text = { Text("Pending (${pendingApprovals.size})") }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("History") }
                )
            }

            // Messages
            if (errorMessage.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 12.sp
                    )
                }
            }

            if (successMessage.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.tertiaryContainer)
                        .padding(12.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text(
                        text = successMessage,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontSize = 12.sp
                    )
                }
            }

            // Content
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
                    // Pending approvals
                    if (pendingApprovals.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No pending offers for review")
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(12.dp)
                        ) {
                            items(pendingApprovals) { approval ->
                                OfferApprovalCard(
                                    approval = approval,
                                    onApprove = {
                                        viewModel.approveOffer(
                                            approval.id,
                                            approval.offerId,
                                            adminId
                                        )
                                    },
                                    onReject = {
                                        selectedApproval = approval
                                        showRejectionDialog = true
                                    },
                                    isProcessing = isProcessing
                                )

                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }

                else -> {
                    // Approval history
                    if (approvalHistory.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No approval history available")
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(12.dp)
                        ) {
                            items(approvalHistory) { approval ->
                                OfferApprovalHistoryCard(approval = approval)
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Rejection reason dialog
    if (showRejectionDialog && selectedApproval != null) {
        Dialog(onDismissRequest = { showRejectionDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Reject Offer",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = rejectionReason,
                        onValueChange = { rejectionReason = it },
                        placeholder = { Text("Enter rejection reason") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = { showRejectionDialog = false }
                        ) {
                            Text("Cancel")
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                if (rejectionReason.isNotEmpty() && selectedApproval != null) {
                                    viewModel.rejectOffer(
                                        selectedApproval!!.id,
                                        selectedApproval!!.offerId,
                                        adminId,
                                        rejectionReason
                                    )
                                    showRejectionDialog = false
                                    rejectionReason = ""
                                    selectedApproval = null
                                }
                            },
                            enabled = rejectionReason.isNotEmpty()
                        ) {
                            Text("Reject")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OfferApprovalCard(
    approval: OfferApproval,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    isProcessing: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
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
                        text = "From: ${approval.theatreOwnerName}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Theatre: ${approval.theatreName}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    Text("Type:", fontSize = 12.sp)
                    Text(
                        when (offer.type) {
                            "PERCENTAGE" -> "${offer.value.toInt()}% OFF"
                            "FIXED_AMOUNT" -> "₹${offer.value.toInt()} OFF"
                            else -> "Special Offer"
                        },
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                ) {
                    Text("Min Amount:", fontSize = 12.sp)
                    Text(
                        "₹${offer.minBookingAmount.toInt()}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    enabled = !isProcessing
                ) {
                    Icon(Icons.Filled.Close, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reject")
                }

                Button(
                    onClick = onApprove,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    enabled = !isProcessing
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.width(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Approve")
                    }
                }
            }
        }
    }
}

@Composable
fun OfferApprovalHistoryCard(
    approval: OfferApproval
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (approval.status == ApprovalStatus.APPROVED.name) {
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                        text = approval.theatreName,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .background(
                            color = if (approval.status == ApprovalStatus.APPROVED.name) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
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

            if (approval.rejectionReason.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Reason: ${approval.rejectionReason}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
