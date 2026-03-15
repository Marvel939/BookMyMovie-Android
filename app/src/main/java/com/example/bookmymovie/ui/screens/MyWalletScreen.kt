package com.example.bookmymovie.ui.screens

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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.runtime.mutableDoubleStateOf
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

data class WalletTransactionItem(
    val txId: String = "",
    val type: String = "",
    val amount: Int = 0,
    val movieName: String = "",
    val bookingId: String = "",
    val createdAt: Long = 0L,
    val note: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyWalletScreen(navController: NavController) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var balance by remember { mutableDoubleStateOf(0.0) }
    var txs by remember { mutableStateOf<List<WalletTransactionItem>>(emptyList()) }

    LaunchedEffect(uid) {
        if (uid.isBlank()) return@LaunchedEffect
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(uid)
        userRef.child("walletBalance").get().addOnSuccessListener {
            balance = (it.getValue(Double::class.java) ?: 0.0)
        }
        userRef.child("wallet_transactions").get().addOnSuccessListener { snap ->
            txs = snap.children.mapNotNull { it.getValue<WalletTransactionItem>() }
                .sortedByDescending { it.createdAt }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Wallet", color = TextPrimary, fontWeight = FontWeight.Bold) },
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
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = PrimaryAccent)
                        Spacer(Modifier.height(0.dp))
                        Text("  Wallet Balance", color = TextSecondary)
                    }
                    Text("₹${"%.2f".format(balance)}", color = PrimaryAccent, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }

            Spacer(Modifier.height(14.dp))

            if (txs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No wallet transactions yet", color = TextSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(txs) { tx ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = CardBackground),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    if (tx.type == "credit") "Refund Credit" else "Wallet Debit",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (tx.movieName.isNotBlank()) {
                                    Text("Movie: ${tx.movieName}", color = TextSecondary, fontSize = 12.sp)
                                }
                                if (tx.bookingId.isNotBlank()) {
                                    Text("Booking ID: ${tx.bookingId}", color = TextSecondary, fontSize = 12.sp)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(tx.note.ifBlank { "Wallet transaction" }, color = TextSecondary, fontSize = 12.sp)
                                    Text(
                                        text = (if (tx.type == "credit") "+" else "-") + "₹${tx.amount}",
                                        color = if (tx.type == "credit") PrimaryAccent else TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
