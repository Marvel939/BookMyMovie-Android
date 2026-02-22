package com.example.bookmymovie.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.example.bookmymovie.MainActivity
import com.example.bookmymovie.navigation.Screen
import com.example.bookmymovie.ui.theme.*
import com.example.bookmymovie.ui.viewmodel.MovieViewModel
import com.example.bookmymovie.ui.viewmodel.NearbyTheatresViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    movieViewModel: MovieViewModel = viewModel()
) {
    val nearbyTheatresViewModel: NearbyTheatresViewModel =
        viewModel(LocalContext.current as MainActivity)
    var selectedItem by remember { mutableIntStateOf(0) }
    val items = listOf("Home", "Movies", "Offers", "Profile")
    val icons = listOf(Icons.Default.Home, Icons.Default.PlayArrow, Icons.Default.Star, Icons.Default.Person)

    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: ""
    val database = FirebaseDatabase.getInstance().getReference("users").child(userId)

    var userCity by remember { mutableStateOf("Loading...") }
    var isMenuExpanded by remember { mutableStateOf(false) }
    val cities = listOf("Mumbai", "Delhi", "Bangalore", "Hyderabad", "Ahmedabad", "Chennai", "Kolkata", "Pune")

    LaunchedEffect(userId) {
        database.child("city").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val city = snapshot.getValue(String::class.java)
                if (city != null) {
                    userCity = city
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box {
                        Column(modifier = Modifier.clickable { isMenuExpanded = true }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    userCity,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Icon(
                                    Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = PrimaryAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                "Select City",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                        DropdownMenu(
                            expanded = isMenuExpanded,
                            onDismissRequest = { isMenuExpanded = false },
                            modifier = Modifier.background(CardBackground)
                        ) {
                            cities.forEach { city ->
                                DropdownMenuItem(
                                    text = { Text(city, color = TextPrimary) },
                                    onClick = {
                                        userCity = city
                                        isMenuExpanded = false
                                        database.child("city").setValue(city)
                                        nearbyTheatresViewModel.filterTheatresByCity(city)
                                    }
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Search.route) }) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = TextPrimary)
                    }
                    Box {
                        IconButton(onClick = { navController.navigate(Screen.Notifications.route) }) {
                            Icon(
                                Icons.Outlined.Notifications,
                                contentDescription = "Notifications",
                                tint = TextPrimary
                            )
                        }
                        // Red notification badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 10.dp, end = 10.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(PrimaryAccent)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepCharcoal,
                    scrolledContainerColor = SecondaryBackground
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = SecondaryBackground,
                tonalElevation = 0.dp
            ) {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                icons[index],
                                contentDescription = item,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                item,
                                fontSize = 11.sp,
                                fontWeight = if (selectedItem == index) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        selected = selectedItem == index,
                        onClick = {
                            selectedItem = index
                            if (item == "Profile") {
                                navController.navigate(Screen.Profile.route)
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryAccent,
                            selectedTextColor = PrimaryAccent,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        },
        containerColor = DeepCharcoal
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = movieViewModel.isLoading,
            onRefresh = { movieViewModel.loadMovies() },
            modifier = Modifier.padding(padding)
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            if (movieViewModel.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryAccent)
                }
            } else if (movieViewModel.errorMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            movieViewModel.errorMessage ?: "Error loading movies",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { movieViewModel.loadMovies() },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text("Retry")
                        }
                    }
                }
            } else {
                BannerCarousel(movieViewModel.nowPlayingMovies, navController)
                Spacer(modifier = Modifier.height(28.dp))
                MovieSection("Now Showing", movieViewModel.nowPlayingMovies, navController)
                Spacer(modifier = Modifier.height(28.dp))
                ComingSoonSection("Coming Soon", movieViewModel.upcomingMovies, navController)
                Spacer(modifier = Modifier.height(28.dp))
                MovieSection("Popular", movieViewModel.popularMovies, navController)
                Spacer(modifier = Modifier.height(28.dp))
                MovieSection("Top Rated", movieViewModel.topRatedMovies, navController)
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
        }
    }
}

@Composable
fun BannerCarousel(movies: List<Movie>, navController: NavController) {
    if (movies.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { movies.size })

    LaunchedEffect(Unit) {
        while (true) {
            yield()
            delay(3500)
            pagerState.animateScrollToPage(
                page = (pagerState.currentPage + 1) % movies.size,
                animationSpec = tween(800, easing = FastOutSlowInEasing)
            )
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .padding(top = 16.dp),
            contentPadding = PaddingValues(horizontal = 24.dp),
            pageSpacing = 16.dp
        ) { page ->
            val movie = movies[page]
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { navController.navigate(Screen.MovieDetail.createRoute(movie.id)) },
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box {
                    AsyncImage(
                        model = movie.bannerUrl,
                        contentDescription = movie.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Dark gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.85f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(20.dp)
                    ) {
                        Text(
                            text = movie.title,
                            color = TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = movie.genre,
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { navController.navigate(Screen.MovieDetail.createRoute(movie.id)) },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.height(38.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp)
                        ) {
                            Text("Book Now", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Dots indicator
        Row(
            modifier = Modifier.height(8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(movies.size) { iteration ->
                val isSelected = pagerState.currentPage == iteration
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isSelected) PrimaryAccent else DividerColor)
                        .then(
                            if (isSelected) Modifier.width(20.dp).height(6.dp)
                            else Modifier.size(6.dp)
                        )
                )
            }
        }
    }
}

@Composable
fun MovieSection(title: String, movies: List<Movie>, navController: NavController) {
    Column {
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(movies) { movie ->
                MovieCard(movie) {
                    navController.navigate(Screen.MovieDetail.createRoute(movie.id))
                }
            }
        }
    }
}

@Composable
fun ComingSoonSection(title: String, movies: List<Movie>, navController: NavController) {
    Column {
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(movies) { movie ->
                ComingSoonCard(movie) {
                    navController.navigate(Screen.MovieDetail.createRoute(movie.id))
                }
            }
        }
    }
}

@Composable
fun MovieCard(movie: Movie, onClick: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "scale"
    )

    Column(
        modifier = Modifier
            .width(150.dp)
            .scale(scale)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
    ) {
        Card(
            shape = RoundedCornerShape(18.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            Box {
                AsyncImage(
                    model = movie.posterUrl,
                    contentDescription = movie.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Rating badge
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = DeepCharcoal.copy(alpha = 0.85f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = StarYellow,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = movie.rating,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = movie.title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = movie.genre,
            fontSize = 11.sp,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ComingSoonCard(movie: Movie, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(130.dp)
            .clickable { onClick() }
    ) {
        Card(
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            AsyncImage(
                model = movie.posterUrl,
                contentDescription = movie.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = movie.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = movie.releaseDate,
            fontSize = 11.sp,
            color = TextSecondary
        )
    }
}
