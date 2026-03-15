package com.example.bookmymovie.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.bookmymovie.navigation.Screen
import com.example.bookmymovie.ui.theme.PrimaryAccent

/**
 * A simple floating action button that navigates to the full-screen AI chat.
 */
@Composable
fun AiChatBotOverlay(
    userId: String,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // Pulsing glow ring
        val infiniteTransition = rememberInfiniteTransition(label = "glow")
        val glowAlpha by infiniteTransition.animateFloat(
            initialValue = 0.35f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                tween(1600, easing = FastOutSlowInEasing),
                RepeatMode.Restart
            ),
            label = "glow_alpha"
        )
        val glowSize by infiniteTransition.animateFloat(
            initialValue = 56f,
            targetValue = 80f,
            animationSpec = infiniteRepeatable(
                tween(1600, easing = FastOutSlowInEasing),
                RepeatMode.Restart
            ),
            label = "glow_size"
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(glowSize.dp)
                .clip(CircleShape)
                .background(PrimaryAccent.copy(alpha = glowAlpha))
        )

        FloatingActionButton(
            onClick = { navController.navigate(Screen.AiChat.route) },
            containerColor = PrimaryAccent,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .size(56.dp)
                .shadow(12.dp, CircleShape)
        ) {
            Icon(
                Icons.Default.SmartToy,
                contentDescription = "Open AI Assistant",
                modifier = Modifier.size(26.dp)
            )
        }
    }
}
