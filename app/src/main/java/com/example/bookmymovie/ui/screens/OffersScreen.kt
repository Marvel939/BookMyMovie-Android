package com.example.bookmymovie.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.bookmymovie.model.Offer
import com.example.bookmymovie.model.OfferCategory
import com.example.bookmymovie.ui.theme.CardBackground
import com.example.bookmymovie.ui.theme.DeepCharcoal
import com.example.bookmymovie.ui.theme.PrimaryAccent
import com.example.bookmymovie.ui.theme.TextPrimary
import com.example.bookmymovie.ui.theme.TextSecondary
import com.example.bookmymovie.ui.viewmodel.OffersViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OffersScreen(
    navController: NavController,
    offersViewModel: OffersViewModel = viewModel()
) {
    val filteredOffers by offersViewModel.filteredOffers.collectAsState()
    val selectedCategory by offersViewModel.selectedCategory.collectAsState()
    val isLoading by offersViewModel.isLoading.collectAsState()

    val categories = listOf(
        OfferCategory.THEATRE_SPECIFIC to "Theatre Offers",
        OfferCategory.MOVIE_SPECIFIC to "Movie Offers",
        OfferCategory.PLATFORM_WIDE to "Platform Offers"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Offers", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepCharcoal)
            )
        },
        containerColor = DeepCharcoal
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ScrollableTabRow(
                selectedTabIndex = categories.indexOfFirst { it.first == selectedCategory },
                containerColor = CardBackground,
                contentColor = PrimaryAccent,
                edgePadding = 8.dp
            ) {
                categories.forEach { pair ->
                    Tab(
                        selected = selectedCategory == pair.first,
                        onClick = { offersViewModel.selectCategory(pair.first) },
                        text = {
                            Text(
                                text = pair.second,
                                color = if (selectedCategory == pair.first) PrimaryAccent else TextSecondary,
                                fontWeight = if (selectedCategory == pair.first) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryAccent)
                }
            } else if (filteredOffers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No offers available in this category.", color = TextSecondary)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredOffers) { offer ->
                        SimplifiedOfferCard(offer)
                    }
                }
            }
        }
    }
}

@Composable
fun SimplifiedOfferCard(offer: Offer) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.LocalOffer,
                contentDescription = null,
                tint = PrimaryAccent,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = offer.title,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = offer.description,
                    color = TextSecondary,
                    fontSize = 14.sp
                )
                if (offer.discountPercentage > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Get ${offer.discountPercentage}% OFF (Up to ₹${offer.maxDiscountAmount})",
                        color = PrimaryAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
