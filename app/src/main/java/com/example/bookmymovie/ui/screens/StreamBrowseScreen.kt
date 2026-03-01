package com.example.bookmymovie.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.bookmymovie.navigation.Screen
import com.example.bookmymovie.ui.theme.*
import com.example.bookmymovie.ui.viewmodel.StreamingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamBrowseScreen(
    navController: NavController,
    streamingViewModel: StreamingViewModel = viewModel()
) {
    var showSearch by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (showSearch) {
                        OutlinedTextField(
                            value = streamingViewModel.searchQuery,
                            onValueChange = { streamingViewModel.updateSearchQuery(it) },
                            placeholder = { Text("Search movies...", color = TextSecondary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryAccent,
                                unfocusedBorderColor = Color.Transparent,
                                cursorColor = PrimaryAccent,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LiveTv, null, tint = PrimaryAccent, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Stream", fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { showSearch = !showSearch; if (!showSearch) streamingViewModel.updateSearchQuery("") }) {
                        Icon(
                            if (showSearch) Icons.Default.Close else Icons.Default.Search,
                            null, tint = TextPrimary
                        )
                    }
                    IconButton(onClick = { navController.navigate(Screen.MyLibrary.route) }) {
                        Icon(Icons.Default.VideoLibrary, "My Library", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepCharcoal)
            )
        },
        containerColor = DeepCharcoal
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Platform filter chips
            PlatformFilterRow(
                platforms = streamingViewModel.availablePlatforms,
                selected = streamingViewModel.selectedPlatform,
                onSelect = { streamingViewModel.setFilter(platform = it) }
            )

            // Genre filter chips
            if (streamingViewModel.availableGenres.isNotEmpty()) {
                GenreFilterRow(
                    genres = streamingViewModel.availableGenres,
                    selected = streamingViewModel.selectedGenre,
                    onSelect = { streamingViewModel.setFilter(genre = it) }
                )
            }

            // Active filters indicator
            if (streamingViewModel.selectedPlatform != null || streamingViewModel.selectedGenre != null || streamingViewModel.searchQuery.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${streamingViewModel.filteredMovies.size} movies found",
                        color = TextSecondary, fontSize = 12.sp
                    )
                    TextButton(onClick = { streamingViewModel.clearFilters() }) {
                        Text("Clear Filters", color = PrimaryAccent, fontSize = 12.sp)
                    }
                }
            }

            // Movie grid
            if (streamingViewModel.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryAccent)
                }
            } else if (streamingViewModel.filteredMovies.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.MovieFilter, null, tint = TextSecondary, modifier = Modifier.size(56.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("No movies found", color = TextSecondary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Text("Try adjusting your filters", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(streamingViewModel.filteredMovies, key = { it.movieId }) { movie ->
                        StreamBrowseCard(movie = movie) {
                            navController.navigate(Screen.StreamDetail.createRoute(movie.movieId))
                        }
                    }
                }
            }
        }
    }
}

// ─── Platform Filter Row ─────────────────────────────────────────────────────

@Composable
private fun PlatformFilterRow(
    platforms: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text("All", fontSize = 12.sp) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = PrimaryAccent,
                selectedLabelColor = Color.White,
                containerColor = CardBackground,
                labelColor = TextSecondary
            ),
            border = FilterChipDefaults.filterChipBorder(
                borderColor = DividerColor,
                selectedBorderColor = PrimaryAccent,
                enabled = true,
                selected = selected == null
            )
        )
        platforms.forEach { platform ->
            FilterChip(
                selected = selected == platform,
                onClick = { onSelect(if (selected == platform) null else platform) },
                label = { Text(platform, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PrimaryAccent,
                    selectedLabelColor = Color.White,
                    containerColor = CardBackground,
                    labelColor = TextSecondary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = DividerColor,
                    selectedBorderColor = PrimaryAccent,
                    enabled = true,
                    selected = selected == platform
                )
            )
        }
    }
}

// ─── Genre Filter Row ────────────────────────────────────────────────────────

@Composable
private fun GenreFilterRow(
    genres: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        genres.forEach { genre ->
            FilterChip(
                selected = selected == genre,
                onClick = { onSelect(if (selected == genre) null else genre) },
                label = { Text(genre, fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PrimaryAccent.copy(alpha = 0.8f),
                    selectedLabelColor = Color.White,
                    containerColor = SecondaryBackground,
                    labelColor = TextSecondary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = Color.Transparent,
                    selectedBorderColor = PrimaryAccent,
                    enabled = true,
                    selected = selected == genre
                )
            )
        }
    }
}

// ─── Browse Card ─────────────────────────────────────────────────────────────

@Composable
private fun StreamBrowseCard(movie: StreamingMovie, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(14.dp))
        ) {
            AsyncImage(
                model = movie.posterUrl,
                contentDescription = movie.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Platform badge
            Surface(
                shape = RoundedCornerShape(bottomEnd = 14.dp),
                color = PrimaryAccent.copy(alpha = 0.9f),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Text(
                    movie.ottPlatform,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Rating badge
            if (movie.rating > 0) {
                Surface(
                    shape = RoundedCornerShape(bottomStart = 14.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Star, null, tint = StarYellow, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(2.dp))
                        Text(
                            String.format("%.1f", movie.rating),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Price badge
            Surface(
                shape = RoundedCornerShape(topStart = 14.dp),
                color = Color.Black.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                Text(
                    "₹${movie.rentPrice.toInt()}",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Exclusive badge
            if (movie.isExclusive) {
                Surface(
                    shape = RoundedCornerShape(topEnd = 14.dp),
                    color = StarYellow.copy(alpha = 0.9f),
                    modifier = Modifier.align(Alignment.BottomStart)
                ) {
                    Text(
                        "EXCLUSIVE",
                        color = Color.Black,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            movie.title,
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            "${movie.genre} • ${movie.releaseYear}",
            color = TextSecondary,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
