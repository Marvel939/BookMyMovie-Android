package com.example.bookmymovie.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.bookmymovie.data.api.TmdbPersonDetail
import com.example.bookmymovie.data.repository.MovieRepository
import com.example.bookmymovie.navigation.Screen
import com.example.bookmymovie.ui.theme.*

@Composable
fun PersonDetailScreen(navController: NavController, personId: String?) {
    var person by remember { mutableStateOf<TmdbPersonDetail?>(null) }
    var knownForMovies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(personId) {
        isLoading = true
        val id = personId?.toIntOrNull()
        if (id != null) {
            val result = MovieRepository.fetchPersonDetail(id)
            person = result
            if (result != null) {
                knownForMovies = MovieRepository.personMoviesToMovieList(result)
            }
        }
        isLoading = false
    }

    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepCharcoal),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = PrimaryAccent)
        }
        return
    }

    val currentPerson = person ?: run {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepCharcoal),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Person not found", color = TextSecondary, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { navController.popBackStack() },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("Go Back")
                }
            }
        }
        return
    }

    val imageBaseUrl = "https://image.tmdb.org/t/p/w500"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepCharcoal)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Profile header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (currentPerson.profilePath != null) {
                        AsyncImage(
                            model = "$imageBaseUrl${currentPerson.profilePath}",
                            contentDescription = currentPerson.name,
                            modifier = Modifier
                                .size(160.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .clip(CircleShape)
                                .background(CardBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = currentPerson.name,
                                tint = TextSecondary,
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = currentPerson.name,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (!currentPerson.knownForDepartment.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentPerson.knownForDepartment,
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Info chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                if (!currentPerson.birthday.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = CardBackground
                    ) {
                        Text(
                            text = "Born: ${currentPerson.birthday}",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                if (!currentPerson.deathday.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = CardBackground
                    ) {
                        Text(
                            text = "Died: ${currentPerson.deathday}",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }

            if (!currentPerson.placeOfBirth.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = currentPerson.placeOfBirth,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(1.dp)
                    .background(DividerColor)
            )

            // Biography
            if (!currentPerson.biography.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(20.dp))
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text(
                        text = "Biography",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = currentPerson.biography,
                        fontSize = 14.sp,
                        color = TextSecondary,
                        lineHeight = 22.sp
                    )
                }
            }

            // Known For Movies
            if (knownForMovies.isNotEmpty()) {
                Spacer(modifier = Modifier.height(28.dp))
                Text(
                    text = "Known For",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(knownForMovies) { movie ->
                        Column(
                            modifier = Modifier
                                .width(120.dp)
                                .clickable {
                                    navController.navigate(Screen.MovieDetail.createRoute(movie.id))
                                }
                        ) {
                            AsyncImage(
                                model = movie.posterUrl,
                                contentDescription = movie.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(170.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = movie.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Back button
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .padding(top = 40.dp, start = 8.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = TextPrimary
            )
        }
    }
}
