package com.example.bookmymovie.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.bookmymovie.MainActivity
import com.example.bookmymovie.model.ChatMessage
import com.example.bookmymovie.navigation.Screen
import com.example.bookmymovie.ui.theme.*
import com.example.bookmymovie.ui.viewmodel.ChatBotViewModel
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatHistoryScreen(navController: NavController) {
    val chatBotViewModel: ChatBotViewModel = viewModel(LocalContext.current as MainActivity)
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    LaunchedEffect(Unit) {
        chatBotViewModel.loadAllSessions(userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Chat History", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SecondaryBackground)
            )
        },
        containerColor = DeepCharcoal
    ) { padding ->

        if (chatBotViewModel.isLoadingHistory) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryAccent)
            }
        } else if (chatBotViewModel.allSessions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(40.dp)) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        tint = TextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("No history yet", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Your chat sessions will appear here after you have a conversation.",
                        color = TextSecondary, fontSize = 14.sp,
                        textAlign = TextAlign.Center, lineHeight = 22.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(chatBotViewModel.allSessions.entries.toList()) { (sessionId, messages) ->
                    SessionCard(
                        sessionId = sessionId,
                        messages = messages,
                        onContinue = {
                            chatBotViewModel.resumeSession(sessionId)
                            navController.navigate(Screen.AiChat.route)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionCard(
    sessionId: String,
    messages: List<ChatMessage>,
    onContinue: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val sessionDate = messages.firstOrNull()?.timestamp?.let {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(it))
    } ?: "Unknown date"

    val firstUserMessage = messages.firstOrNull { it.role == "user" }?.text ?: "No messages"
    val exchanges = messages.count { it.role == "user" }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Session header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(listOf(PrimaryAccent.copy(alpha = 0.3f), Color.Transparent))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ChatBubble,
                        contentDescription = null,
                        tint = PrimaryAccent,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(sessionDate, color = TextSecondary, fontSize = 11.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        firstUserMessage,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "$exchanges",
                        color = PrimaryAccent,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (exchanges == 1) "question" else "questions",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }

            // Bottom action row: expand toggle + Continue Chat button
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = { expanded = !expanded },
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(
                        if (expanded) "Show less" else "View conversation",
                        color = PrimaryAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = PrimaryAccent,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Continue Chat button
                Button(
                    onClick = onContinue,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.Chat, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Continue", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Expanded message list
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    messages.forEach { msg ->
                        HistoryMessageRow(message = msg)
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryMessageRow(message: ChatMessage) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(PrimaryAccent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.SmartToy, null, tint = PrimaryAccent, modifier = Modifier.size(13.dp))
            }
            Spacer(Modifier.width(8.dp))
        }
        Surface(
            shape = RoundedCornerShape(
                topStart = if (isUser) 12.dp else 3.dp,
                topEnd = if (isUser) 3.dp else 12.dp,
                bottomStart = 12.dp,
                bottomEnd = 12.dp
            ),
            color = if (isUser) PrimaryAccent.copy(alpha = 0.2f) else SurfaceDark,
            modifier = Modifier.widthIn(max = 240.dp)
        ) {
            Text(
                text = message.text,
                color = if (isUser) SecondaryAccent else TextPrimary,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
            )
        }
        if (isUser) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(DividerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, null, tint = TextSecondary, modifier = Modifier.size(13.dp))
            }
        }
    }
}
