package com.example.bookmymovie.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.bookmymovie.data.repository.StreamingRepository
import com.example.bookmymovie.ui.theme.*
import com.example.bookmymovie.ui.viewmodel.StreamingViewModel

// ─── Sealed class for access state ───────────────────────────────────────────

private sealed class AccessState {
    data object Loading : AccessState()
    data class Granted(val type: String, val ottPlatform: String) : AccessState()
    data class Denied(val message: String) : AccessState()
}

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

// ─── Helper: open OTT app or fallback to browser ─────────────────────────────

private fun openOttApp(platform: String, context: android.content.Context) {
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
// Player Screen — verifies access and redirects to OTT platform
// ═════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamPlayerScreen(
    navController: NavController,
    viewModel: StreamingViewModel,
    movieId: String
) {
    val context = LocalContext.current

    // Load movie detail
    LaunchedEffect(movieId) { viewModel.loadMovieDetail(movieId) }

    val movie = viewModel.selectedMovie

    // Access verification
    var accessState by remember { mutableStateOf<AccessState>(AccessState.Loading) }

    LaunchedEffect(movieId) {
        try {
            val libraryItem = StreamingRepository.isMovieInLibrary(movieId)
            if (libraryItem != null && !libraryItem.isExpired()) {
                accessState = AccessState.Granted(
                    type = libraryItem.type,
                    ottPlatform = libraryItem.ottPlatform.ifBlank { movie?.ottPlatform ?: "" }
                )
            } else {
                accessState = AccessState.Denied(
                    if (libraryItem?.isExpired() == true) "Your rental has expired. Please rent or buy again."
                    else "You need to rent or buy this movie to watch it."
                )
            }
        } catch (e: Exception) {
            accessState = AccessState.Denied("Failed to verify access: ${e.message}")
        }
    }

    // Auto-open OTT when access granted
    var hasLaunched by remember { mutableStateOf(false) }
    LaunchedEffect(accessState) {
        if (accessState is AccessState.Granted && !hasLaunched) {
            hasLaunched = true
            val platform = (accessState as AccessState.Granted).ottPlatform
            if (platform.isNotBlank()) {
                openOttApp(platform, context)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        movie?.title ?: "Player",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        containerColor = Color.Black
    ) { padding ->

        when (val state = accessState) {
            // ── Loading ──────────────────────────────────────────────────
            is AccessState.Loading -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = PrimaryAccent)
                        Spacer(Modifier.height(16.dp))
                        Text("Verifying access...", color = TextSecondary, fontSize = 14.sp)
                    }
                }
            }

            // ── Access denied ────────────────────────────────────────────
            is AccessState.Denied -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        Icon(Icons.Default.Lock, null, tint = PrimaryAccent, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Access Denied",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(state.message, color = TextSecondary, fontSize = 14.sp)
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = { navController.popBackStack() },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Go Back")
                        }
                    }
                }
            }

            // ── Access granted → OTT redirect ───────────────────────────
            is AccessState.Granted -> {
                val platform = state.ottPlatform
                val pColor = platformColor(platform)

                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        Icon(
                            Icons.Default.OpenInNew, null,
                            tint = pColor,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Opening ${platform.ifBlank { "streaming app" }}...",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "You have ${if (state.type == "buy") "purchased" else "rented"} this movie.",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(24.dp))

                        // Retry / re-open button
                        Button(
                            onClick = { openOttApp(platform, context) },
                            colors = ButtonDefaults.buttonColors(containerColor = pColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(0.7f)
                        ) {
                            Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Open ${platform.ifBlank { "App" }}", fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = { navController.popBackStack() },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                        ) {
                            Text("Go Back")
                        }
                    }
                }
            }
        }
    }
}
