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
import com.example.bookmymovie.utils.MarkdownUtils
import com.google.firebase.database.DataSnapshot
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

class ChatBotViewModel : ViewModel() {

    companion object {
        private const val OPENROUTER_API_KEY = "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"
        private const val OPENROUTER_BASE_URL = "https://openrouter.ai/api/v1/chat/completions"
        private const val MODEL = "nvidia/nemotron-3-super-120b-a12b:free"
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
                    .addHeader("Authorization", "Bearer $OPENROUTER_API_KEY")
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

    override fun onCleared() {
        super.onCleared()
        tts?.shutdown()
        tts = null
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}
