package com.example.bookmymovie.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.bookmymovie.ui.theme.CardBackground
import com.example.bookmymovie.ui.theme.DeepCharcoal
import com.example.bookmymovie.ui.theme.PrimaryAccent
import com.example.bookmymovie.ui.theme.TextPrimary
import com.example.bookmymovie.ui.theme.TextSecondary
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.getValue

data class RefundItem(
    val bookingId: String = "",
    val movieName: String = "",
    val refundedAmount: Int = 0,
    val nonRefundableAmount: Int = 0,
    val refundedAt: Long = 0L,
    val status: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyRefundsScreen(navController: NavController) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var refunds by remember { mutableStateOf<List<RefundItem>>(emptyList()) }

    LaunchedEffect(uid) {
        if (uid.isBlank()) return@LaunchedEffect
        FirebaseDatabase.getInstance().getReference("users").child(uid).child("refunds")
            .get()
            .addOnSuccessListener { snap ->
                refunds = snap.children.mapNotNull { it.getValue<RefundItem>() }
                    .sortedByDescending { it.refundedAt }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Refunds", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepCharcoal)
            )
        },
        containerColor = DeepCharcoal
    ) { padding ->
        if (refunds.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = TextSecondary)
                    Spacer(Modifier.height(8.dp))
                    Text("No refunds yet", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(refunds) { refund ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(refund.movieName.ifBlank { "Movie" }, color = TextPrimary, fontWeight = FontWeight.Bold)
                            Text("Booking ID: ${refund.bookingId}", color = TextSecondary, fontSize = 12.sp)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Refunded", color = TextSecondary, fontSize = 12.sp)
                                Text("₹${refund.refundedAmount}", color = PrimaryAccent, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Non-refundable", color = TextSecondary, fontSize = 12.sp)
                                Text("₹${refund.nonRefundableAmount}", color = TextSecondary)
                            }
                            Text("Status: ${refund.status.ifBlank { "succeeded" }}", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
