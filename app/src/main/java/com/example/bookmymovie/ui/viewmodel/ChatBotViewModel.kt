package com.example.bookmymovie.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookmymovie.model.ChatMessage
import com.example.bookmymovie.model.CinemaShowtime
import com.example.bookmymovie.firebase.FirebaseMovieRepository
import com.example.bookmymovie.firebase.UserRepository
import com.example.bookmymovie.utils.MarkdownUtils
import com.google.firebase.database.DataSnapshot
import kotlinx.coroutines.tasks.await
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

data class NavigationRequest(
    val route: String,
    val placeId: String = "",
    val showtime: CinemaShowtime? = null
)

class ChatBotViewModel : ViewModel() {

    // ── Booking Flow State ────────────────────────────────────────────────────
    var navigationRequest by mutableStateOf<NavigationRequest?>(null)
    var isBookingFlowActive by mutableStateOf(false)
    var bookingStep by mutableStateOf("ASK_THEATRE")
    var bookingMovieId by mutableStateOf<String?>(null)
    var bookingMovieName by mutableStateOf<String?>(null)
    var bookingMoviePoster by mutableStateOf<String?>(null)
    var bookingTheatreId by mutableStateOf<String?>(null)
    var bookingCity by mutableStateOf("Mumbai")
    var availableTheatresForMovie = mutableStateListOf<com.example.bookmymovie.model.Theatre>()

    companion object {
        private const val OPENROUTER_BASE_URL = "https://openrouter.ai/api/v1/chat/completions"
        private const val MODEL = "nvidia/nemotron-3-super-120b-a12b:free"
        
        fun getOpenRouterKey(context: Context): String {
            return try {
                context.assets.open("openrouter.key").bufferedReader().use { it.readText().trim() }
            } catch (e: Exception) {
                "" // Fallback if file is missing
            }
        }
    }

    // ── Current session ───────────────────────────────────────────────────────
    var currentSessionId: String = UUID.randomUUID().toString()
        private set

    val messages = mutableStateListOf<ChatMessage>()
    var isLoading by mutableStateOf(false)
    var isSpeaking by mutableStateOf(false)
    var isListening by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    // ── History (all sessions) ────────────────────────────────────────────────
    /** sessionId → sorted list of ChatMessages for that session */
    val allSessions = mutableStateMapOf<String, List<ChatMessage>>()
    var isLoadingHistory by mutableStateOf(false)

    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // ── Text-to-Speech ────────────────────────────────────────────────────────

    fun initTts(context: Context) {
        if (tts != null) return
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) { isSpeaking = true }
                    override fun onDone(utteranceId: String?) { isSpeaking = false }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) { isSpeaking = false }
                })
            }
        }
    }

    fun speakText(text: String) {
        // Strip markdown symbols so TTS reads clean text
        val clean = MarkdownUtils.stripForTTS(text)
        tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
        isSpeaking = true
    }

    fun stopSpeaking() {
        tts?.stop()
        isSpeaking = false
    }

    // ── Speech Recognition ────────────────────────────────────────────────────

    fun startListening(context: Context, onResult: (String) -> Unit) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            errorMessage = "Speech recognition not available on this device"
            return
        }
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { isListening = true }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { isListening = false }
                override fun onError(error: Int) {
                    isListening = false
                    errorMessage = "Voice recognition error. Please try again."
                }
                override fun onResults(results: Bundle?) {
                    isListening = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) onResult(matches[0])
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        isListening = false
    }

    // ── Session Management ────────────────────────────────────────────────────

    /** Clears the current chat and starts a fresh session. Old messages stay in Firebase. */
    fun startNewChat() {
        stopSpeaking()
        messages.clear()
        currentSessionId = UUID.randomUUID().toString()
        errorMessage = null
    }

    /**
     * Loads an old session so the user can continue chatting in it.
     * Sets [currentSessionId] to [sessionId] and populates [messages] from the cached allSessions map.
     * Any new messages sent will be appended to this session in Firebase.
     */
    fun resumeSession(sessionId: String) {
        stopSpeaking()
        currentSessionId = sessionId
        messages.clear()
        val sessionMessages = allSessions[sessionId]
        if (sessionMessages != null) {
            messages.addAll(sessionMessages)
        }
        errorMessage = null
    }

    // ── Firebase — current session ────────────────────────────────────────────

    fun loadHistory(userId: String) {
        if (userId.isEmpty()) return
        val ref = FirebaseDatabase.getInstance()
            .getReference("ai_chat_history")
            .child(userId)
        ref.orderByChild("timestamp").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val loaded = mutableListOf<ChatMessage>()
                for (child in snapshot.children) {
                    val msg = child.getValue(ChatMessage::class.java) ?: continue
                    // Only load messages belonging to the current session
                    if (msg.sessionId == currentSessionId) loaded.add(msg)
                }
                messages.clear()
                messages.addAll(loaded)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun saveMessageToFirebase(userId: String, message: ChatMessage) {
        if (userId.isEmpty()) return
        FirebaseDatabase.getInstance()
            .getReference("ai_chat_history")
            .child(userId)
            .child(message.id)
            .setValue(message)
    }

    private fun deleteMessageFromFirebase(userId: String, messageId: String) {
        if (userId.isEmpty()) return
        FirebaseDatabase.getInstance()
            .getReference("ai_chat_history")
            .child(userId)
            .child(messageId)
            .removeValue()
    }

    // ── Firebase — all sessions (for history screen) ──────────────────────────

    fun loadAllSessions(userId: String) {
        if (userId.isEmpty()) return
        isLoadingHistory = true
        val ref = FirebaseDatabase.getInstance()
            .getReference("ai_chat_history")
            .child(userId)
        ref.orderByChild("timestamp").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val allMessages = mutableListOf<ChatMessage>()
                for (child in snapshot.children) {
                    val msg = child.getValue(ChatMessage::class.java) ?: continue
                    allMessages.add(msg)
                }
                // Group by sessionId; treat empty sessionId as "Legacy"
                val grouped = allMessages
                    .groupBy { it.sessionId.ifEmpty { "legacy" } }
                    .mapValues { (_, msgs) -> msgs.sortedBy { it.timestamp } }
                // Sort sessions by most recent message
                val sorted = grouped.entries
                    .sortedByDescending { (_, msgs) -> msgs.maxOfOrNull { it.timestamp } ?: 0L }
                    .associate { it.key to it.value }
                allSessions.clear()
                allSessions.putAll(sorted)
                isLoadingHistory = false
            }
            override fun onCancelled(error: DatabaseError) {
                isLoadingHistory = false
            }
        })
    }

    // ── Edit Message ──────────────────────────────────────────────────────────

    /**
     * Removes a user message (and the immediately following AI response if any)
     * from the messages list and Firebase so the edited text can be resent inline.
     */
    fun editMessage(message: ChatMessage, userId: String) {
        val index = messages.indexOfFirst { it.id == message.id }
        if (index < 0) return

        val toDelete = mutableListOf(message)
        // If the next message is from the AI, remove it too
        if (index + 1 < messages.size && messages[index + 1].role == "model") {
            toDelete.add(messages[index + 1])
        }
        toDelete.forEach { msg ->
            messages.remove(msg)
            deleteMessageFromFirebase(userId, msg.id)
        }
    }

    // ── Send Message to OpenRouter ────────────────────────────────────────────

    fun sendMessage(userText: String, userId: String, context: Context) {
        if (userText.isBlank()) return
        errorMessage = null

        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = "user",
            text = userText.trim(),
            timestamp = System.currentTimeMillis(),
            sessionId = currentSessionId
        )
        messages.add(userMsg)
        saveMessageToFirebase(userId, userMsg)

        if (isBookingFlowActive) {
            handleBookingStep(userText, userId, context)
            return
        }

        val lowerText = userText.lowercase()
        if (lowerText.contains("book a ticket") || lowerText.contains("book a movie") || lowerText.contains("book ticket")) {
            startBookingFlow(userText, userId)
            return
        }

        isLoading = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Build the message history array (last 20 messages for context)
                val historyForApi = messages.takeLast(20)
                val messagesArray = JSONArray()
                val systemMsg = JSONObject().apply {
                    put("role", "system")
                    put("content",
                        "You are a helpful AI assistant for the BookMyMovie app. " +
                        "Help users find movies, get recommendations, understand showtimes, " +
                        "and answer any movie-related questions. " +
                        "Keep your answers concise and friendly. " +
                        "Do NOT use markdown formatting — no asterisks, no hyphens as lists, no arrows. " +
                        "Write in plain conversational text. Use numbered lists (1. 2. 3.) where needed."
                    )
                }
                messagesArray.put(systemMsg)
                for (msg in historyForApi) {
                    val role = if (msg.role == "model") "assistant" else "user"
                    messagesArray.put(JSONObject().apply {
                        put("role", role)
                        put("content", msg.text)
                    })
                }

                val body = JSONObject().apply {
                    put("model", MODEL)
                    put("messages", messagesArray)
                    put("max_tokens", 1024)
                    put("temperature", 0.7)
                }

                val request = Request.Builder()
                    .url(OPENROUTER_BASE_URL)
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .addHeader("Authorization", "Bearer ${getOpenRouterKey(context)}")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("HTTP-Referer", "https://bookmymovie.app")
                    .addHeader("X-Title", "BookMyMovie AI Assistant")
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && responseBody != null) {
                        try {
                            val json = JSONObject(responseBody)
                            val rawText = json
                                .getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content")
                                .trim()

                            // Strip markdown before storing and displaying
                            val cleanText = MarkdownUtils.stripMarkdown(rawText)

                            val aiMsg = ChatMessage(
                                id = UUID.randomUUID().toString(),
                                role = "model",
                                text = cleanText,
                                timestamp = System.currentTimeMillis(),
                                sessionId = currentSessionId
                            )
                            messages.add(aiMsg)
                            saveMessageToFirebase(userId, aiMsg)
                            speakText(cleanText)
                        } catch (parseEx: Exception) {
                            errorMessage = "Error parsing response: ${parseEx.message}"
                        }
                    } else {
                        val errSnippet = responseBody?.take(200) ?: "(empty body)"
                        errorMessage = "API error ${response.code}: $errSnippet"
                    }
                    isLoading = false
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    errorMessage = "Network error. Check your connection."
                    isLoading = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorMessage = "Something went wrong: ${e.message}"
                    isLoading = false
                }
            }
        }
    }

    private fun addSystemMessage(text: String, userId: String) {
        val aiMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = "model",
            text = text,
            timestamp = System.currentTimeMillis(),
            sessionId = currentSessionId
        )
        messages.add(aiMsg)
        saveMessageToFirebase(userId, aiMsg)
        speakText(text)
    }

    private fun startBookingFlow(userText: String, userId: String) {
        isLoading = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val lowerStr = userText.lowercase()
                var searchQuery = userText
                
                // Extract movie name
                val phrases = listOf("book a ticket for the ", "book a ticket for ", "book a movie ticket for the ", "book a movie ticket for ", "book ticket for ", "book ")
                for (phrase in phrases) {
                    if (lowerStr.contains(phrase)) {
                        val startIndex = lowerStr.indexOf(phrase) + phrase.length
                        searchQuery = userText.substring(startIndex).trim().removeSurrounding("\"").removeSurrounding("'")
                        break
                    }
                }
                
                val tmdbMovies = try {
                    com.example.bookmymovie.data.repository.MovieRepository.searchMovies(searchQuery)
                } catch(e: Exception) {
                    emptyList()
                }

                if (tmdbMovies.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        addSystemMessage("I couldn't find a matching movie for your request. Please specify the exact movie name.", userId)
                        isLoading = false
                    }
                    return@launch
                }
                
                // Fetch city
                val prefs = UserRepository.getUserPreferences()
                val city = prefs.lastCity.ifBlank { "Mumbai" }
                
                // Fetch all theatres
                val theatres = FirebaseMovieRepository.getTheatresInCity(city)
                
                val matchedTmdbMovie = tmdbMovies.first()
                val showingTheatres = if (theatres.isNotEmpty()) {
                    theatres.toMutableList()
                } else {
                    mutableListOf(com.example.bookmymovie.model.Theatre(theatreId = "dummy_1", name = "Connplex Cinemas", city = city))
                }
                
                withContext(Dispatchers.Main) {
                    bookingMovieId = matchedTmdbMovie.id.toString()
                    bookingMovieName = matchedTmdbMovie.title
                    bookingMoviePoster = matchedTmdbMovie.posterUrl // Use poster for the UI!
                    bookingCity = city
                    availableTheatresForMovie.clear()
                    availableTheatresForMovie.addAll(showingTheatres)
                    isBookingFlowActive = true
                    bookingStep = "ASK_THEATRE"
                    
                    val theatreNames = showingTheatres.take(3).mapIndexed { i, t -> "${i+1}. ${t.name}" }.joinToString(", ")
                    addSystemMessage("Showtimes are available for ${matchedTmdbMovie.title}! This movie is available at: $theatreNames. Which theatre would you like to book?", userId)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    addSystemMessage("Error checking availability: ${e.message}", userId)
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isLoading = false
                }
            }
        }
    }

    private fun handleBookingStep(userText: String, userId: String, context: Context) {
        isLoading = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (bookingStep == "ASK_THEATRE") {
                    val lowerStr = userText.lowercase()
                    val matchedTheatre = availableTheatresForMovie.find { lowerStr.contains(it.name.lowercase()) }
                        ?: availableTheatresForMovie.getOrNull(userText.trim().toIntOrNull()?.minus(1) ?: -1)
                        
                    withContext(Dispatchers.Main) {
                        if (matchedTheatre != null) {
                            bookingTheatreId = matchedTheatre.theatreId
                            bookingStep = "ASK_DATE"
                            addSystemMessage("Great, you selected ${matchedTheatre.name}. For which date to book the movie ticket for? (e.g. YYYY-MM-DD or Today)", userId)
                        } else {
                            addSystemMessage("I couldn't recognize that theatre. Please reply with the name or number of the theatre.", userId)
                        }
                        isLoading = false
                    }
                    return@launch
                }

                var dateStr = userText.trim()
                if (dateStr.lowercase().contains("today")) {
                    dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                } else if (dateStr.lowercase().contains("tomorrow")) {
                    val cal = java.util.Calendar.getInstance()
                    cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
                    dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(cal.time)
                } else {
                    // Simple parsing or fallback. Let's assume standard input format for test robustness.
                }
                
                val tId = bookingTheatreId ?: return@launch
                var foundPlaceId: String? = null
                var foundCinemaShowtime: CinemaShowtime? = null
                val dbRef = FirebaseDatabase.getInstance().getReference("showtimes")
                
                // Fetch all movies to help with title matching if needed
                val allMovies = FirebaseMovieRepository.getAllMovies()
                
                // Path is showtimes/{city}/{theatreId}
                val snapshot = dbRef.child(bookingCity).child(tId).get().await()
                val availableDates = mutableSetOf<String>()
                
                for (movieSnap in snapshot.children) {
                    val stMovieId = movieSnap.key ?: continue
                    val showtime = movieSnap.getValue(com.example.bookmymovie.model.Showtime::class.java) ?: continue
                    val stDate = showtime.date
                    
                    // Match by ID or Name
                    val bNameLower = bookingMovieName?.lowercase() ?: ""
                    val localMovie = allMovies.find { it.movieId == stMovieId }
                    val dbNameLower = localMovie?.title?.lowercase() ?: ""
                    
                    val isIdMatch = stMovieId == bookingMovieId
                    val isNameMatch = bNameLower.isNotEmpty() && dbNameLower.isNotEmpty() && (
                        bNameLower == dbNameLower || bNameLower.contains(dbNameLower) || dbNameLower.contains(bNameLower)
                    )
                    
                    if (isIdMatch || isNameMatch) {
                        availableDates.add(stDate)
                        
                        // If we find an exact match for the requested date and we haven't locked one in yet
                        if (stDate == dateStr && foundCinemaShowtime == null) {
                            val formatPrices = mapOf(showtime.format.ifEmpty { "2D" } to mapOf("silver" to showtime.price.toInt(), "gold" to (showtime.price + 50).toInt(), "platinum" to (showtime.price + 150).toInt()))
                            
                            foundPlaceId = tId
                            foundCinemaShowtime = CinemaShowtime(
                                showtimeId = movieSnap.key ?: java.util.UUID.randomUUID().toString(),
                                screenId = "screen_${showtime.screenNumber}",
                                screenName = "Screen ${showtime.screenNumber}",
                                screenType = showtime.format.ifEmpty { "2D" },
                                movieId = stMovieId,
                                movieName = bookingMovieName ?: localMovie?.title ?: "",
                                moviePoster = bookingMoviePoster ?: localMovie?.posterUrl ?: "",
                                date = stDate,
                                time = showtime.times.firstOrNull() ?: "01:00 PM",
                                language = "English",
                                formats = showtime.format.ifEmpty { "2D" },
                                formatPrices = formatPrices
                            )
                        }
                    }
                }
                
                withContext(Dispatchers.Main) {
                    if (foundCinemaShowtime != null && foundPlaceId != null) {
                        addSystemMessage("Great! Navigating you to the seat map.", userId)
                        navigationRequest = NavigationRequest(
                            route = "seat_selection", 
                            placeId = foundPlaceId, 
                            showtime = foundCinemaShowtime
                        )
                        isBookingFlowActive = false
                    } else {
                        if (availableDates.isNotEmpty()) {
                            val datesStr = availableDates.sorted().joinToString(", ")
                            addSystemMessage("Sorry, no showtimes were found for $dateStr. However, showtimes are available on these dates: $datesStr. Would you like to book the ticket for one of these dates?", userId)
                        } else {
                            addSystemMessage("Sorry, no showtimes were found at the selected theatre. Try another theatre or say 'cancel'.", userId)
                            isBookingFlowActive = false
                        }
                        
                        if (userText.lowercase() == "cancel" || userText.lowercase() == "no") {
                            isBookingFlowActive = false
                            addSystemMessage("Booking cancelled.", userId)
                        }
                    }
                }
            } catch(e: Exception) {
                withContext(Dispatchers.Main) {
                    addSystemMessage("Error finding showtimes: ${e.message}", userId)
                    isBookingFlowActive = false
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isLoading = false
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        tts?.shutdown()
        tts = null
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}
