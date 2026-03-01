package com.example.bookmymovie.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.bookmymovie.model.StreamingMovie
import com.example.bookmymovie.model.StreamingTransaction
import com.example.bookmymovie.navigation.Screen
import com.example.bookmymovie.ui.theme.*
import com.example.bookmymovie.ui.viewmodel.AdminStreamingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminStreamingCatalogScreen(navController: NavController) {
    val vm: AdminStreamingViewModel = viewModel()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 1) vm.loadTransactions()
    }

    // Snackbar for messages
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(vm.message) {
        vm.message?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearMessage()
        }
    }
    LaunchedEffect(vm.errorMessage) {
        vm.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearError()
        }
    }

    // Delete confirmation dialog
    showDeleteDialog?.let { movieId ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            containerColor = CardBackground,
            title = { Text("Delete Movie?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently remove this movie from the streaming catalog.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = { vm.deleteMovie(movieId); showDeleteDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text("Cancel", color = TextSecondary) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Streaming Catalog", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepCharcoal)
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = {
                        vm.clearForm()
                        navController.navigate(Screen.AdminAddStreamingMovie.route)
                    },
                    containerColor = PrimaryAccent
                ) {
                    Icon(Icons.Default.Add, "Add Streaming Movie", tint = Color.White)
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = DeepCharcoal
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = CardBackground,
                contentColor = PrimaryAccent,
                divider = { HorizontalDivider(color = DividerColor) }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Movies", color = if (selectedTab == 0) PrimaryAccent else TextSecondary) },
                    icon = { Icon(Icons.Default.Movie, null, tint = if (selectedTab == 0) PrimaryAccent else TextSecondary) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Transactions", color = if (selectedTab == 1) PrimaryAccent else TextSecondary) },
                    icon = { Icon(Icons.Default.Receipt, null, tint = if (selectedTab == 1) PrimaryAccent else TextSecondary) }
                )
            }

            when (selectedTab) {
                0 -> StreamingMoviesTab(
                    movies = vm.movies,
                    isLoading = vm.isLoading,
                    onEdit = { movie ->
                        vm.prepareForEdit(movie)
                        navController.navigate(Screen.AdminAddStreamingMovie.route)
                    },
                    onDelete = { showDeleteDialog = it },
                    onToggleActive = { movieId, isActive -> vm.toggleActive(movieId, isActive) }
                )
                1 -> TransactionsTab(vm.transactions)
            }
        }
    }
}

// ─── Movies Tab ──────────────────────────────────────────────────────────────

@Composable
private fun StreamingMoviesTab(
    movies: List<StreamingMovie>,
    isLoading: Boolean,
    onEdit: (StreamingMovie) -> Unit,
    onDelete: (String) -> Unit,
    onToggleActive: (String, Boolean) -> Unit
) {
    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrimaryAccent)
        }
        return
    }

    if (movies.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Movie, null, tint = TextSecondary, modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(16.dp))
                Text("No streaming movies added", color = TextSecondary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text("Tap + to add a movie to the catalog", color = TextSecondary, fontSize = 13.sp)
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(DeepCharcoal),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(movies, key = { it.movieId }) { movie ->
            StreamingMovieCard(
                movie = movie,
                onEdit = { onEdit(movie) },
                onDelete = { onDelete(movie.movieId) },
                onToggleActive = { onToggleActive(movie.movieId, !movie.isActive) }
            )
        }
    }
}

@Composable
private fun StreamingMovieCard(
    movie: StreamingMovie,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleActive: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(12.dp)) {
            // Poster
            AsyncImage(
                model = movie.posterUrl,
                contentDescription = movie.title,
                modifier = Modifier
                    .width(80.dp)
                    .height(120.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        movie.title,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (!movie.isActive) {
                        Spacer(Modifier.width(6.dp))
                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFF44336).copy(alpha = 0.15f)) {
                            Text("Inactive", color = Color(0xFFF44336), fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))

                // Platform badge
                Surface(shape = RoundedCornerShape(4.dp), color = PrimaryAccent.copy(alpha = 0.15f)) {
                    Text(movie.ottPlatform, color = PrimaryAccent, fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                }
                Spacer(Modifier.height(6.dp))

                Text("${movie.genre} • ${movie.releaseYear}", color = TextSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))

                Row {
                    Text("Rent: ₹${movie.rentPrice}", color = TextSecondary, fontSize = 12.sp)
                    Spacer(Modifier.width(12.dp))
                    Text("Buy: ₹${movie.buyPrice}", color = TextSecondary, fontSize = 12.sp)
                }
                Spacer(Modifier.height(8.dp))

                // Action buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, "Edit", tint = PrimaryAccent, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onToggleActive, modifier = Modifier.size(32.dp)) {
                        Icon(
                            if (movie.isActive) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            if (movie.isActive) "Deactivate" else "Activate",
                            tint = if (movie.isActive) TextSecondary else Color(0xFF4CAF50),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFF44336), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

// ─── Transactions Tab ────────────────────────────────────────────────────────

@Composable
private fun TransactionsTab(transactions: List<StreamingTransaction>) {
    if (transactions.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Receipt, null, tint = TextSecondary, modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(16.dp))
                Text("No transactions yet", color = TextSecondary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text("Transactions appear when users rent/buy movies", color = TextSecondary, fontSize = 13.sp)
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(DeepCharcoal),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(transactions, key = { it.transactionId }) { tx ->
            TransactionCard(tx)
        }
    }
}

@Composable
private fun TransactionCard(tx: StreamingTransaction) {
    val statusColor = when (tx.status) {
        "completed" -> Color(0xFF4CAF50)
        "failed" -> Color(0xFFF44336)
        else -> Color(0xFFFF9800)
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    tx.movieTitle,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Surface(shape = RoundedCornerShape(4.dp), color = statusColor.copy(alpha = 0.15f)) {
                    Text(
                        tx.status.replaceFirstChar { it.uppercaseChar() },
                        color = statusColor,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = RoundedCornerShape(20.dp), color = SecondaryBackground) {
                    Text(
                        tx.type.replaceFirstChar { it.uppercaseChar() },
                        color = TextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Surface(shape = RoundedCornerShape(20.dp), color = SecondaryBackground) {
                    Text(
                        "₹${tx.amount}",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("User: ${tx.userId}", color = TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
