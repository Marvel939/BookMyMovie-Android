package com.example.bookmymovie.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
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

    companion object {
        private const val OPENROUTER_BASE_URL = "https://openrouter.ai/api/v1/chat/completions"
        private const val MODEL = "nvidia/nemotron-3-super-120b-a12b:free"  // Auto-routes to available free model
        private const val TAG = "ChatBotViewModel"
        
        fun getOpenRouterKey(context: Context): String {
            return try {
                val key = context.assets.open("openrouter.key").bufferedReader().use { it.readText().trim() }
                Log.d(TAG, "API Key loaded successfully: ${key.take(20)}...")
                key
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load API key file: ${e.message}", e)
                ""
            }
        }
    }

    private val TAG = "ChatBotViewModel"

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

        Log.d(TAG, "===== MESSAGE SENT: $userText =====")

        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = "user",
            text = userText.trim(),
            timestamp = System.currentTimeMillis(),
            sessionId = currentSessionId
        )
        messages.add(userMsg)
        saveMessageToFirebase(userId, userMsg)

        isLoading = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "1. Starting API request...")
                val apiKey = getOpenRouterKey(context)
                Log.d(TAG, "2. API Key: ${if(apiKey.isEmpty()) "EMPTY!" else "OK"}")
                if(apiKey.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        errorMessage = "ERROR: API key not found. Check openrouter.key file in assets folder."
                        Log.e(TAG, "API key is empty!")
                        isLoading = false
                    }
                    return@launch
                }

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

        Log.d(TAG, "Sending request to OpenRouter API with model: $MODEL")
                
                val request = Request.Builder()
                    .url(OPENROUTER_BASE_URL)
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("HTTP-Referer", "https://bookmymovie.app")
                    .addHeader("X-Title", "BookMyMovie AI Assistant")
                    .build()

                Log.d(TAG, "3. Making HTTP call...")
                var response: okhttp3.Response? = null
                var responseBody: String? = null
                
                try {
                    response = httpClient.newCall(request).execute()
                    Log.d(TAG, "4. Got response code: ${response.code}")
                    responseBody = response.body?.string()
                    Log.d(TAG, "5. Response body length: ${responseBody?.length ?: 0}")
                    Log.d(TAG, "7. About to process response in withContext")
                } catch(e: Exception) {
                    Log.e(TAG, "8. Exception during API call: ${e.message}", e)
                    withContext(Dispatchers.Main) {
                        errorMessage = "Network error: ${e.message}"
                        isLoading = false
                    }
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    Log.d(TAG, "6. Processing response on Main thread")
                    when {
                        response?.code == 401 -> {
                            errorMessage = "Authorization failed. Your API key may be invalid or expired. Please check your openrouter.key file."
                            Log.e(TAG, "401 Unauthorized - check API key")
                        }
                        response?.code == 404 -> {
                            errorMessage = "API model not available (404). The AI model is not available on OpenRouter. Please check your model configuration."
                            Log.e(TAG, "404 Not Found - Model endpoint unavailable")
                        }
                        response?.code == 429 -> {
                            errorMessage = "Rate limit reached. Please try again in a few minutes."
                            Log.e(TAG, "429 Too Many Requests")
                        }
                        response?.code == 500 -> {
                            errorMessage = "Server error (500). The API service is having issues. Try again later."
                            Log.e(TAG, "500 Server Error")
                        }
                        response?.isSuccessful == true && responseBody != null -> {
                            try {
                                Log.d(TAG, "9. Parsing JSON response...")
                                val json = JSONObject(responseBody)
                                Log.d(TAG, "10. Got JSONObject")
                                val rawText = json
                                    .getJSONArray("choices")
                                    .getJSONObject(0)
                                    .getJSONObject("message")
                                    .getString("content")
                                    .trim()

                                Log.d(TAG, "Successfully parsed AI response")

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
                                Log.d(TAG, "14. Message added to list and saved to Firebase")
                                saveMessageToFirebase(userId, aiMsg)
                                speakText(cleanText)
                            } catch (parseEx: Exception) {
                                errorMessage = "Failed to parse API response: ${parseEx.message}"
                                Log.e(TAG, "Error parsing response", parseEx)
                            }
                        }
                        else -> {
                            Log.d(TAG, "11. Response code was: ${response?.code}")
                            Log.d(TAG, "12. Is successful: ${response?.isSuccessful}")
                            Log.d(TAG, "13. Response body null: ${responseBody == null}")
                            val errSnippet = responseBody?.take(300) ?: "(empty response)"
                            errorMessage = "API Error ${response?.code}: $errSnippet"
                            Log.e(TAG, "API call failed with code ${response?.code}: $errSnippet")
                        }
                    }
                    isLoading = false
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    errorMessage = "Network error: ${e.message}. Check your internet connection."
                    Log.e(TAG, "Network error", e)
                    isLoading = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorMessage = "Error: ${e.message}"
                    Log.e(TAG, "Unexpected error", e)
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



    override fun onCleared() {
        super.onCleared()
        tts?.shutdown()
        tts = null
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}
