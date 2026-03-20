package com.example.bookmymovie.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.bookmymovie.MainActivity
import com.example.bookmymovie.model.ChatMessage
import com.example.bookmymovie.navigation.Screen
import com.example.bookmymovie.ui.theme.*
import com.example.bookmymovie.ui.viewmodel.ChatBotViewModel
import com.example.bookmymovie.ui.viewmodel.BookingViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    navController: NavController
) {
    // Activity-scoped ViewModel so session persists across navigation
    val chatBotViewModel: ChatBotViewModel = viewModel(LocalContext.current as MainActivity)
    val bookingViewModel: BookingViewModel = viewModel(LocalContext.current as MainActivity)
    val context = LocalContext.current
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var showNewChatDialog by remember { mutableStateOf(false) }

    // Mic permission
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            chatBotViewModel.startListening(context) { recognized -> 
                chatBotViewModel.sendMessage(recognized, userId, context)
            }
        }
    }

    LaunchedEffect(chatBotViewModel.navigationRequest) {
        val req = chatBotViewModel.navigationRequest
        if (req != null) {
            if (req.route == "seat_selection" && req.showtime != null) {
                bookingViewModel.currentPlaceId = req.placeId
                bookingViewModel.selectedShowtime = req.showtime
                bookingViewModel.selectedFormat = req.showtime.formats.split(",").firstOrNull()?.trim() ?: "2D"
                bookingViewModel.selectedLanguage = req.showtime.language.split(",").firstOrNull()?.trim() ?: "English"
                navController.navigate(Screen.SeatSelection.route)
            }
            chatBotViewModel.navigationRequest = null
        }
    }

    // Init TTS and load current session history on first open
    LaunchedEffect(Unit) {
        chatBotViewModel.initTts(context)
        if (chatBotViewModel.messages.isEmpty()) {
            chatBotViewModel.loadHistory(userId)
        }
    }

    // Auto-scroll to the latest message
    LaunchedEffect(chatBotViewModel.messages.size) {
        if (chatBotViewModel.messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(chatBotViewModel.messages.size - 1)
            }
        }
    }

    // ── New Chat Confirmation Dialog ──────────────────────────────────────────
    if (showNewChatDialog) {
        AlertDialog(
            onDismissRequest = { showNewChatDialog = false },
            containerColor = CardBackground,
            title = {
                Text("Start New Chat?", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Your current conversation will be saved in History. A fresh chat will start.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showNewChatDialog = false
                    chatBotViewModel.startNewChat()
                }) {
                    Text("New Chat", color = PrimaryAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewChatDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(listOf(PrimaryAccent, Color(0xFF8B5E2A)))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.SmartToy, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("AI Assistant", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            Text("Powered by NVIDIA Nemotron", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        chatBotViewModel.stopSpeaking()
                        navController.popBackStack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    // Stop TTS
                    AnimatedVisibility(visible = chatBotViewModel.isSpeaking) {
                        IconButton(onClick = { chatBotViewModel.stopSpeaking() }) {
                            Icon(Icons.Default.VolumeOff, "Stop speaking", tint = PrimaryAccent)
                        }
                    }
                    // History button
                    IconButton(onClick = { navController.navigate(Screen.ChatHistory.route) }) {
                        Icon(Icons.Default.History, "Chat history", tint = TextPrimary)
                    }
                    // New Chat button
                    IconButton(onClick = { showNewChatDialog = true }) {
                        Icon(Icons.Default.AddComment, "New chat", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SecondaryBackground
                )
            )
        },
        bottomBar = {
            Surface(color = SecondaryBackground, shadowElevation = 8.dp) {
                Column {
                    chatBotViewModel.errorMessage?.let { err ->
                        Text(
                            text = err,
                            color = ErrorRose,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ErrorRose.copy(alpha = 0.12f))
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = inputText,
                            onValueChange = {
                                inputText = it
                                chatBotViewModel.errorMessage = null
                            },
                            placeholder = { Text("Ask me anything...", color = TextSecondary, fontSize = 14.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(24.dp)),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = CardBackground,
                                unfocusedContainerColor = CardBackground,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = PrimaryAccent
                            ),
                            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                            maxLines = 4
                        )
                        Spacer(Modifier.width(8.dp))

                        // Mic
                        val micColor = if (chatBotViewModel.isListening) PrimaryAccent else TextSecondary
                        IconButton(
                            onClick = {
                                if (chatBotViewModel.isListening) {
                                    chatBotViewModel.stopListening()
                                } else {
                                    val hasPerm = ContextCompat.checkSelfPermission(
                                        context, Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (hasPerm) chatBotViewModel.startListening(context) { recognized -> 
                                        chatBotViewModel.sendMessage(recognized, userId, context)
                                    }
                                    else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            modifier = Modifier.size(46.dp).clip(CircleShape).background(micColor.copy(alpha = 0.12f))
                        ) {
                            Icon(
                                if (chatBotViewModel.isListening) Icons.Default.MicOff else Icons.Default.Mic,
                                null, tint = micColor, modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.width(6.dp))

                        // Send
                        val canSend = inputText.isNotBlank() && !chatBotViewModel.isLoading
                        IconButton(
                            onClick = {
                                if (canSend) {
                                    chatBotViewModel.sendMessage(inputText.trim(), userId, context)
                                    inputText = ""
                                }
                            },
                            enabled = canSend,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(if (canSend) PrimaryAccent else PrimaryAccent.copy(alpha = 0.3f))
                        ) {
                            Icon(Icons.Default.Send, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        },
        containerColor = DeepCharcoal
    ) { padding ->

        if (chatBotViewModel.messages.isEmpty() && !chatBotViewModel.isLoading) {
            // ── Empty State ───────────────────────────────────────────────────
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(40.dp)
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "idle")
                    val glowAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f, targetValue = 0.8f,
                        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
                        label = "glow"
                    )
                    Box(
                        modifier = Modifier.size(100.dp).clip(CircleShape)
                            .background(PrimaryAccent.copy(alpha = glowAlpha * 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.SmartToy, null,
                            tint = PrimaryAccent.copy(alpha = glowAlpha),
                            modifier = Modifier.size(56.dp)
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "Hi! I'm your AI Movie Assistant.",
                        color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Ask me about movies, showtimes, recommendations, cast and more. Type or use voice!",
                        color = TextSecondary, fontSize = 14.sp,
                        textAlign = TextAlign.Center, lineHeight = 22.sp
                    )
                    Spacer(Modifier.height(32.dp))
                    listOf("Recommend a movie for tonight", "Top rated films this year", "Best action movies").forEach { suggestion ->
                        SuggestionChip(
                            onClick = { chatBotViewModel.sendMessage(suggestion, userId, context) },
                            label = { Text(suggestion, color = TextPrimary, fontSize = 13.sp) },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = CardBackground),
                            border = SuggestionChipDefaults.suggestionChipBorder(enabled = true, borderColor = DividerColor)
                        )
                    }
                }
            }
        } else {
            // ── Messages List ─────────────────────────────────────────────────
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(chatBotViewModel.messages, key = { it.id }) { msg ->
                    FullScreenChatBubble(
                        message = msg,
                        onResend = { oldMsg, newText ->
                            chatBotViewModel.editMessage(oldMsg, userId)
                            chatBotViewModel.sendMessage(newText, userId, context)
                        }
                    )
                }
                if (chatBotViewModel.isLoading) {
                    item { FullScreenTypingIndicator() }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
fun FullScreenChatBubble(
    message: ChatMessage,
    onResend: (oldMessage: ChatMessage, newText: String) -> Unit
) {
    val isUser = message.role == "user"
    var isEditing by remember { mutableStateOf(false) }
    var editText by remember(message.id) { mutableStateOf(message.text) }
    val clipboardManager = LocalClipboardManager.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        // AI avatar (left side)
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(PrimaryAccent, Color(0xFF8B5E2A)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.SmartToy, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
        }

        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 284.dp)
        ) {
            if (isEditing) {
                // ── Inline Edit Mode ──────────────────────────────────────────
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CardBackground,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        TextField(
                            value = editText,
                            onValueChange = { editText = it },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = SurfaceDark,
                                unfocusedContainerColor = SurfaceDark,
                                focusedIndicatorColor = PrimaryAccent,
                                unfocusedIndicatorColor = DividerColor,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = PrimaryAccent
                            ),
                            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                            shape = RoundedCornerShape(10.dp),
                            maxLines = 6,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    isEditing = false
                                    editText = message.text   // reset
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("Cancel", color = TextSecondary, fontSize = 13.sp)
                            }
                            Spacer(Modifier.width(6.dp))
                            Button(
                                onClick = {
                                    if (editText.isNotBlank()) {
                                        isEditing = false
                                        onResend(message, editText.trim())
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Send, null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Send", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                // ── Normal Bubble ─────────────────────────────────────────────
                Surface(
                    shape = RoundedCornerShape(
                        topStart = if (isUser) 18.dp else 4.dp,
                        topEnd = if (isUser) 4.dp else 18.dp,
                        bottomStart = 18.dp,
                        bottomEnd = 18.dp
                    ),
                    color = if (isUser) PrimaryAccent.copy(alpha = 0.9f) else CardBackground
                ) {
                    Text(
                        text = message.text,
                        color = if (isUser) Color.White else TextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }

                // ── Action buttons row (Edit + Copy) ──────────────────────────
                Spacer(Modifier.height(3.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Edit button — user messages only
                    if (isUser) {
                        TextButton(
                            onClick = { isEditing = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Icon(Icons.Default.Edit, null, tint = TextSecondary, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("Edit", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                    // Copy button — all messages
                    TextButton(
                        onClick = { clipboardManager.setText(AnnotatedString(message.text)) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, null, tint = TextSecondary, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("Copy", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }

        // User avatar (right side)
        if (isUser) {
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(CardBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun FullScreenTypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(PrimaryAccent, Color(0xFF8B5E2A)))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.SmartToy, null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Surface(
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
            color = CardBackground
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    val offsetY by infiniteTransition.animateFloat(
                        initialValue = 0f, targetValue = -7f,
                        animationSpec = infiniteRepeatable(
                            tween(400, delayMillis = index * 130, easing = FastOutSlowInEasing),
                            RepeatMode.Reverse
                        ),
                        label = "dot_$index"
                    )
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .offset(y = offsetY.dp)
                            .clip(CircleShape)
                            .background(TextSecondary)
                    )
                }
            }
        }
    }
}
