package com.example.bookmymovie.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.bookmymovie.navigation.Screen
import com.example.bookmymovie.ui.theme.*
import com.example.bookmymovie.ui.viewmodel.StreamingViewModel
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.rememberPaymentSheet

// ─── Helper: OTT platform brand color ────────────────────────────────────────

private fun platformColor(platform: String): Color = when (platform.lowercase()) {
    "netflix"       -> Color(0xFFE50914)
    "prime video"   -> Color(0xFF00A8E1)
    "hotstar", "disney+ hotstar" -> Color(0xFF1F80E0)
    "zee5"          -> Color(0xFF8B3EB5)
    "sonyliv"       -> Color(0xFF070707)
    "jiocinema"     -> Color(0xFFE8308C)
    "mubi"          -> Color(0xFFDC1F26)
    "apple tv+"     -> Color(0xFF555555)
    else            -> Color(0xFFE50914)
}

// ─── Helper: open OTT platform app / website ─────────────────────────────────

private fun openOttPlatform(platform: String, context: android.content.Context) {
    val (pkg, web) = when (platform.lowercase()) {
        "netflix"       -> "com.netflix.mediaclient" to "https://www.netflix.com"
        "prime video"   -> "com.amazon.avod.thirdpartyclient" to "https://www.primevideo.com"
        "hotstar", "disney+ hotstar" -> "in.startv.hotstar" to "https://www.hotstar.com"
        "zee5"          -> "com.graymatrix.did" to "https://www.zee5.com"
        "sonyliv"       -> "com.sonyliv" to "https://www.sonyliv.com"
        "jiocinema"     -> "com.jio.media.ondemand" to "https://www.jiocinema.com"
        "mubi"          -> "com.mubi" to "https://mubi.com"
        "apple tv+"     -> "com.apple.atve.androidtv.appletv" to "https://tv.apple.com"
        else            -> "" to "https://www.google.com/search?q=${platform}+streaming"
    }
    try {
        if (pkg.isNotBlank()) {
            val intent = context.packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null) { context.startActivity(intent); return }
        }
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(web)))
    } catch (_: Exception) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(web)))
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Detail Screen
// ═════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamDetailScreen(
    navController: NavController,
    viewModel: StreamingViewModel,
    movieId: String
) {
    val context = LocalContext.current

    LaunchedEffect(movieId) { viewModel.loadMovieDetail(movieId) }

    val movie = viewModel.selectedMovie
    val ownedItem = viewModel.isMovieOwned
    val isCheckingOwnership = viewModel.isCheckingOwnership
    val isLoadingDetail = viewModel.isLoadingDetail
    val isPurchasing = viewModel.isPurchasing
    val purchaseSuccess = viewModel.purchaseSuccess
    val purchaseError = viewModel.purchaseError
    val hasAccess = ownedItem != null

    // Stripe payment state
    var paymentType by remember { mutableStateOf<String?>(null) } // "rent" or "buy"
    var paymentError by remember { mutableStateOf<String?>(null) }
    var isRequestingPayment by remember { mutableStateOf(false) }

    val paymentSheet = rememberPaymentSheet { result ->
        when (result) {
            is PaymentSheetResult.Completed -> {
                paymentError = null
                if (movie != null && paymentType != null) {
                    viewModel.completePurchase(
                        movie = movie,
                        type = paymentType!!,
                        paymentIntentId = "pi_${System.currentTimeMillis()}"
                    )
                }
                paymentType = null
            }
            is PaymentSheetResult.Canceled -> {
                paymentType = null
            }
            is PaymentSheetResult.Failed -> {
                paymentError = result.error.message ?: "Payment failed"
                paymentType = null
            }
        }
    }

    // Snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(purchaseSuccess) {
        if (purchaseSuccess) {
            snackbarHostState.showSnackbar(
                if (paymentType == "buy" || viewModel.isMovieOwned?.type == "buy")
                    "Movie purchased successfully!" else "Movie rented successfully!"
            )
            viewModel.resetPurchaseState()
        }
    }
    LaunchedEffect(purchaseError) {
        purchaseError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.resetPurchaseState()
        }
    }
    LaunchedEffect(paymentError) {
        paymentError?.let {
            snackbarHostState.showSnackbar(it)
            paymentError = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        movie?.title ?: "Movie Details",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = DeepCharcoal
    ) { padding ->

        if (movie == null || isLoadingDetail) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryAccent)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Banner / Inline Trailer ──────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            ) {
                // Poster / Banner image
                AsyncImage(
                    model = movie.bannerUrl.ifBlank { movie.posterUrl },
                    contentDescription = movie.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, DeepCharcoal),
                                startY = 100f
                            )
                        )
                )
                // Play trailer button — opens in YouTube app or browser
                if (movie.trailerUrl.isNotBlank()) {
                    FloatingActionButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(movie.trailerUrl))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.align(Alignment.Center).size(64.dp),
                        containerColor = PrimaryAccent.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(32.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, "Play Trailer", tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                }
            }

            // ── Content ──────────────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(12.dp))

                // Title
                Text(movie.title, color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))

                // Info chips
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (movie.rating > 0) {
                        InfoChip {
                            Icon(Icons.Default.Star, null, tint = StarYellow, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                String.format("%.1f", movie.rating),
                                color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    InfoChip { Text(movie.releaseYear.toString(), color = TextSecondary, fontSize = 12.sp) }
                    if (movie.duration.isNotBlank()) {
                        InfoChip { Text(movie.duration, color = TextSecondary, fontSize = 12.sp) }
                    }
                    InfoChip { Text(movie.language, color = TextSecondary, fontSize = 12.sp) }
                }
                Spacer(Modifier.height(12.dp))

                // Platform badge + genre
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val pColor = platformColor(movie.ottPlatform)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = pColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            movie.ottPlatform,
                            color = pColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Text(movie.genre, color = TextSecondary, fontSize = 13.sp)
                    if (movie.isExclusive) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = StarYellow.copy(alpha = 0.15f)
                        ) {
                            Text(
                                "EXCLUSIVE", color = StarYellow,
                                fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))

                // Synopsis
                if (movie.description.isNotBlank()) {
                    Text("Synopsis", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(movie.description, color = TextSecondary, fontSize = 14.sp, lineHeight = 20.sp)
                    Spacer(Modifier.height(16.dp))
                }

                // Director & cast
                if (movie.director.isNotBlank()) {
                    Text("Director", color = TextSecondary, fontSize = 12.sp)
                    Text(movie.director, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(10.dp))
                }
                if (movie.cast.isNotEmpty()) {
                    Text("Cast", color = TextSecondary, fontSize = 12.sp)
                    Text(movie.cast.joinToString(", "), color = TextPrimary, fontSize = 14.sp)
                    Spacer(Modifier.height(16.dp))
                }

                HorizontalDivider(color = DividerColor)
                Spacer(Modifier.height(16.dp))

                // ── Ownership / Purchase section ─────────────────────────
                if (isCheckingOwnership) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryAccent, modifier = Modifier.size(32.dp))
                    }
                } else if (hasAccess) {
                    // User owns / rented this movie
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1B5E20).copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    if (ownedItem?.type == "buy") "You own this movie" else "Rented",
                                    color = Color(0xFF4CAF50),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                if (ownedItem?.type == "rent") {
                                    val remaining = ownedItem.remainingDays()
                                    Text("$remaining days remaining", color = TextSecondary, fontSize = 12.sp)
                                } else {
                                    Text("Tap Watch Now to open on ${movie.ottPlatform}", color = TextSecondary, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    // Watch Now → open OTT platform
                    val pColor = platformColor(movie.ottPlatform)
                    Button(
                        onClick = { openOttPlatform(movie.ottPlatform, context) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = pColor)
                    ) {
                        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Watch Now on ${movie.ottPlatform}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                } else {
                    // ── Rent / Buy ───────────────────────────────────────
                    Text("Get This Movie", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(12.dp))

                    // Rent card
                    PurchaseOptionCard(
                        title = "Rent",
                        subtitle = "${movie.rentDurationDays} days access",
                        price = movie.rentPrice,
                        icon = Icons.Default.AccessTime,
                        accentColor = PrimaryAccent,
                        isLoading = isRequestingPayment && paymentType == "rent",
                        onClick = {
                            paymentType = "rent"
                            paymentError = null
                            isRequestingPayment = true
                            val data = hashMapOf("amount" to movie.rentPrice.toInt())
                            Firebase.functions
                                .getHttpsCallable("createPaymentIntent")
                                .call(data)
                                .addOnSuccessListener { result ->
                                    val secret = (result.data as? Map<*, *>)?.get("clientSecret") as? String
                                    isRequestingPayment = false
                                    if (secret != null) {
                                        paymentSheet.presentWithPaymentIntent(
                                            secret,
                                            PaymentSheet.Configuration(merchantDisplayName = "BookMyMovie Stream")
                                        )
                                    } else {
                                        paymentError = "Failed to initialize payment"
                                    }
                                }
                                .addOnFailureListener { e ->
                                    isRequestingPayment = false
                                    paymentError = e.message ?: "Payment setup failed"
                                }
                        }
                    )
                    Spacer(Modifier.height(10.dp))

                    // Buy card
                    PurchaseOptionCard(
                        title = "Buy",
                        subtitle = "Own forever",
                        price = movie.buyPrice,
                        icon = Icons.Default.ShoppingCart,
                        accentColor = Color(0xFF4CAF50),
                        isLoading = isRequestingPayment && paymentType == "buy",
                        onClick = {
                            paymentType = "buy"
                            paymentError = null
                            isRequestingPayment = true
                            val data = hashMapOf("amount" to movie.buyPrice.toInt())
                            Firebase.functions
                                .getHttpsCallable("createPaymentIntent")
                                .call(data)
                                .addOnSuccessListener { result ->
                                    val secret = (result.data as? Map<*, *>)?.get("clientSecret") as? String
                                    isRequestingPayment = false
                                    if (secret != null) {
                                        paymentSheet.presentWithPaymentIntent(
                                            secret,
                                            PaymentSheet.Configuration(merchantDisplayName = "BookMyMovie Stream")
                                        )
                                    } else {
                                        paymentError = "Failed to initialize payment"
                                    }
                                }
                                .addOnFailureListener { e ->
                                    isRequestingPayment = false
                                    paymentError = e.message ?: "Payment setup failed"
                                }
                        }
                    )

                    if (paymentError != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(paymentError!!, color = Color(0xFFF44336), fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
        }

    }
}

// ─── Reusable Components ─────────────────────────────────────────────────────

@Composable
private fun InfoChip(content: @Composable RowScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = SecondaryBackground
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
private fun PurchaseOptionCard(
    title: String,
    subtitle: String,
    price: Double,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accentColor.copy(alpha = 0.15f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(icon, null, tint = accentColor, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(subtitle, color = TextSecondary, fontSize = 12.sp)
            }
            Button(
                onClick = onClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                enabled = !isLoading,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("₹${price.toInt()}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}
