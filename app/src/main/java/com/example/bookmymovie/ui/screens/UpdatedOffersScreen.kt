package com.example.bookmymovie.ui.screens

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bookmymovie.model.Offer
import com.example.bookmymovie.model.OfferCategory
import com.example.bookmymovie.ui.components.OfferCardHorizontal
import com.example.bookmymovie.ui.components.OfferCard
import com.example.bookmymovie.ui.viewmodel.OffersViewModel

@Composable
private fun OfferCategoryCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(220.dp)
            .padding(end = 10.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun UpdatedOffersScreen(
    cityId: String = "1", // Default city ID, pass from navigation
    userId: String = "", // Pass current user ID
    viewModel: OffersViewModel = viewModel(),
    onOfferSelected: (Offer) -> Unit = {}
) {
    val carouselOffers by viewModel.carouselOffers.collectAsState()
    val personalizedOffers by viewModel.personalizedOffers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    var selectedCategory by remember { mutableStateOf<OfferCategory?>(null) }

    LaunchedEffect(key1 = cityId) {
        viewModel.loadHomeOffers(cityId, userId)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Exclusive Offers",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "View all real-time offers and apply coupons",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            errorMessage.isNotEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            else -> {
                val filteredCarouselOffers = if (selectedCategory == null) {
                    carouselOffers
                } else {
                    carouselOffers.filter { it.category == selectedCategory!!.name }
                }
                val filteredPersonalizedOffers = if (selectedCategory == null) {
                    personalizedOffers
                } else {
                    personalizedOffers.filter { it.category == selectedCategory!!.name }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    item {
                        Text(
                            text = "Offer Categories",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp)
                        ) {
                            OfferCategoryCard(
                                title = "Theatre-Specific",
                                subtitle = "Valid for a particular theatre/show",
                                isSelected = selectedCategory == OfferCategory.THEATRE_SPECIFIC,
                                onClick = {
                                    selectedCategory = if (selectedCategory == OfferCategory.THEATRE_SPECIFIC) null else OfferCategory.THEATRE_SPECIFIC
                                }
                            )
                            OfferCategoryCard(
                                title = "Movie-Specific",
                                subtitle = "Valid only for a particular movie",
                                isSelected = selectedCategory == OfferCategory.MOVIE_SPECIFIC,
                                onClick = {
                                    selectedCategory = if (selectedCategory == OfferCategory.MOVIE_SPECIFIC) null else OfferCategory.MOVIE_SPECIFIC
                                }
                            )
                            OfferCategoryCard(
                                title = "Platform-Wide",
                                subtitle = "Works across theatres/cities",
                                isSelected = selectedCategory == OfferCategory.PLATFORM_WIDE,
                                onClick = {
                                    selectedCategory = if (selectedCategory == OfferCategory.PLATFORM_WIDE) null else OfferCategory.PLATFORM_WIDE
                                }
                            )
                            OfferCategoryCard(
                                title = "Bank / Payment",
                                subtitle = "Stripe payment offers only",
                                isSelected = selectedCategory == OfferCategory.BANK_PAYMENT,
                                onClick = {
                                    selectedCategory = if (selectedCategory == OfferCategory.BANK_PAYMENT) null else OfferCategory.BANK_PAYMENT
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Carousel Section
                    item {
                        if (filteredCarouselOffers.isNotEmpty()) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Featured Offers",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(horizontal = 16.dp),
                                    content = {
                                        filteredCarouselOffers.forEach { offer ->
                                            OfferCardHorizontal(
                                                offer = offer,
                                                onClick = { onOfferSelected(it) },
                                                modifier = Modifier.padding(end = 12.dp)
                                            )
                                        }
                                    }
                                )

                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }

                    // Personalized Offers Section
                    if (filteredPersonalizedOffers.isNotEmpty()) {
                        item {
                            Text(
                                text = "Recommended for You",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                        }

                        items(filteredPersonalizedOffers) { offer ->
                            OfferCard(
                                offer = offer,
                                onClick = { onOfferSelected(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }

                    // Empty state
                    if (filteredCarouselOffers.isEmpty() && filteredPersonalizedOffers.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "No Offers Available",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "Check back soon for exciting offers!",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
