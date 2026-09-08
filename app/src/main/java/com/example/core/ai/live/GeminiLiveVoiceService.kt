package com.example.core.ai.live

import android.util.Base64
import android.util.Log
import com.example.core.config.AppConfig
import com.example.core.error.AppError
import com.example.core.result.AppResult
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Real-time voice implementation powered by Google Gemini Multimodal Live API.
 * Communicates bidirectionally over WebSockets with 16kHz audio input and 24kHz audio output.
 */
class GeminiLiveVoiceService(
  private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
    .readTimeout(0, TimeUnit.MILLISECONDS) // Unlimited for WebSockets
    .connectTimeout(15, TimeUnit.SECONDS)
    .pingInterval(20, TimeUnit.SECONDS)
    .build(),
) : LiveVoiceService {

  companion object {
    private const val TAG = "GeminiLiveVoiceService"
  }

  private val moshi: Moshi = Moshi.Builder()
    .addLast(KotlinJsonAdapterFactory())
    .build()

  private val clientMessageAdapter = moshi.adapter(LiveClientMessage::class.java)
  private val serverMessageAdapter = moshi.adapter(LiveServerMessage::class.java)

  private val _sessionState = MutableStateFlow<LiveSessionState>(LiveSessionState.Idle)
  override val sessionState: StateFlow<LiveSessionState> = _sessionState.asStateFlow()

  private val _transcript = MutableStateFlow<List<LiveTranscriptItem>>(emptyList())
  override val transcript: StateFlow<List<LiveTranscriptItem>> = _transcript.asStateFlow()

  private val _isMicrophoneMuted = MutableStateFlow(false)
  override val isMicrophoneMuted: StateFlow<Boolean> = _isMicrophoneMuted.asStateFlow()

  private var activeScope: CoroutineScope? = null
  private var activeWebSocket: WebSocket? = null
  private var activeSessionToken: String? = null
  private var audioRecorder: LiveAudioRecorder? = null
  private var audioPlayer: LiveAudioPlayer? = null
  private var currentUserId: String? = null
  private var currentAssistantTurnId: String? = null
  private val assistantTurnStringBuilder = StringBuilder()

  override fun isVoiceEngineAvailable(): Boolean {
    return AppConfig.isGeminiConfigured()
  }

  override fun resetStateToIdle() {
    _sessionState.value = LiveSessionState.Idle
  }

  override suspend fun startSession(
    userId: String,
    voiceName: String,
    systemInstruction: String?,
  ): AppResult<Unit> {
    if (userId.isBlank()) {
      return AppResult.Error(AppError.ValidationError("User ID is required to start a Live session"))
    }

    val apiKey = AppConfig.getGeminiApiKey().trim()
    if (apiKey.isBlank()) {
      _sessionState.value = LiveSessionState.Error(
        message = "Gemini API key is not configured in AI Studio Secrets. Add GEMINI_API_KEY to start Live mode.",
        canRetry = false,
      )
      return AppResult.Error(AppError.ConfigurationError("Gemini API key is missing in environment"))
    }

    // Clean up any existing session before starting
    cleanupResources(sendCloseFrame = true)

    val sessionToken = UUID.randomUUID().toString()
    activeSessionToken = sessionToken
    currentUserId = userId
    _transcript.value = emptyList()
    _sessionState.value = LiveSessionState.Connecting

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    activeScope = scope

    // Initialize Audio Player for 24kHz AI speech
    val player = LiveAudioPlayer(
      onAmplitudeChanged = { amplitude ->
        val currentState = _sessionState.value
        if (currentState is LiveSessionState.Speaking || (audioPlayer?.isPlaying?.value == true)) {
          _sessionState.value = LiveSessionState.Speaking(amplitude)
        }
      },
      onPlaybackStateChanged = { isPlaying ->
        if (!isPlaying && _sessionState.value is LiveSessionState.Speaking) {
          _sessionState.value = LiveSessionState.Listening(0f)
        }
      },
    )
    audioPlayer = player
    val playerReady = player.start(scope)
    if (!playerReady) {
      Log.w(TAG, "AudioPlayer failed to initialize hardware")
    }

    // Initialize Audio Recorder for 16kHz User mic
    val recorder = LiveAudioRecorder(
      onAudioChunkAvailable = { base64Chunk ->
        sendRealtimeAudioChunk(base64Chunk)
      },
      onAmplitudeChanged = { amplitude ->
        val currentState = _sessionState.value
        if (currentState is LiveSessionState.Listening && (audioPlayer?.isPlaying?.value != true)) {
          _sessionState.value = LiveSessionState.Listening(amplitude)
        }
      },
    )
    audioRecorder = recorder

    // Connect WebSocket
    val wsUrl = "${AppConfig.GEMINI_LIVE_WS_URL}?key=$apiKey"
    val request = Request.Builder()
      .url(wsUrl)
      .build()

    val webSocketListener = object : WebSocketListener() {
      override fun onOpen(webSocket: WebSocket, response: Response) {
        if (activeSessionToken != sessionToken) {
          webSocket.close(1000, "Old session aborted")
          return
        }
        Log.i(TAG, "Gemini Live WebSocket connected for session $sessionToken")
        activeWebSocket = webSocket

        // Send Initial Setup Message
        val effectiveInstruction = systemInstruction ?: AppConfig.KASA_LIVE_SYSTEM_INSTRUCTION
        val setupPayload = LiveClientMessage(
          setup = LiveSetupMessage(
            model = AppConfig.GEMINI_LIVE_DEFAULT_MODEL,
            generationConfig = LiveGenerationConfig(
              responseModalities = listOf("AUDIO"),
              speechConfig = LiveSpeechConfig(
                voiceConfig = LiveVoiceConfig(
                  prebuiltVoiceConfig = LivePrebuiltVoiceConfig(voiceName = voiceName)
                )
              ),
              temperature = 0.7f,
            ),
            systemInstruction = LiveContent(
              parts = listOf(LivePart(text = effectiveInstruction))
            ),
          )
        )

        try {
          val json = clientMessageAdapter.toJson(setupPayload)
          webSocket.send(json)
          Log.d(TAG, "Setup message sent to Live API: model=${AppConfig.GEMINI_LIVE_DEFAULT_MODEL}, voice=$voiceName")
        } catch (e: Exception) {
          Log.e(TAG, "Failed to serialize and send setup payload", e)
          if (activeSessionToken == sessionToken) {
            _sessionState.value = LiveSessionState.Error("Failed to initiate Live session handshake: ${e.message}")
          }
        }
      }

      override fun onMessage(webSocket: WebSocket, text: String) {
        if (activeSessionToken != sessionToken) return
        handleServerMessage(text, sessionToken)
      }

      override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        if (activeSessionToken != sessionToken) return
        handleServerMessage(bytes.utf8(), sessionToken)
      }

      override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        Log.i(TAG, "Gemini Live WebSocket closing: $code / $reason")
        webSocket.close(1000, null)
      }

      override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        if (activeSessionToken != sessionToken) return
        Log.i(TAG, "Gemini Live WebSocket closed: $code / $reason")
        val currentState = _sessionState.value
        if (currentState !is LiveSessionState.Ended && currentState !is LiveSessionState.Error) {
          _sessionState.value = LiveSessionState.Ended("Live session closed")
        }
      }

      override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        if (activeSessionToken != sessionToken) return
        val statusCode = response?.code
        val errorBody = try { response?.body?.string() ?: "" } catch (_: Exception) { "" }
        Log.e(TAG, "Gemini Live WebSocket failure (code=$statusCode): ${t.message}. Body=$errorBody", t)

        val friendlyMessage = when {
          statusCode == 400 -> "Invalid Live API request configuration."
          statusCode == 401 || statusCode == 403 -> "Invalid or unauthorized Gemini API key. Check AI Studio Secrets."
          statusCode == 429 -> "Gemini Live quota limit reached. Please wait a moment and try again."
          statusCode == 404 -> "The requested Live model is not available for this API tier."
          statusCode != null && statusCode >= 500 -> "Google AI Live server temporarily unavailable. Please try again."
          t.message?.contains("Unable to resolve host", ignoreCase = true) == true ->
            "No internet connection. Please check your network."
          else -> t.message ?: "Connection to KASA Live failed."
        }

        _sessionState.value = LiveSessionState.Error(
          message = friendlyMessage,
          canRetry = true,
        )
        audioRecorder?.stop()
        audioPlayer?.interrupt()
      }
    }

    try {
      activeWebSocket = okHttpClient.newWebSocket(request, webSocketListener)
      return AppResult.Success(Unit)
    } catch (e: Exception) {
      _sessionState.value = LiveSessionState.Error("Could not connect to Live voice server: ${e.message}")
      return AppResult.Error(AppError.NetworkError("WebSocket connection error", e))
    }
  }

  private fun handleServerMessage(json: String, sessionToken: String) {
    if (activeSessionToken != sessionToken) return
    try {
      val serverMessage = serverMessageAdapter.fromJson(json) ?: return

      if (serverMessage.setupComplete != null) {
        Log.i(TAG, "Setup complete confirmed by Gemini Live API")
        // Start microphone recording now that the server is ready
        activeScope?.let { scope ->
          val started = audioRecorder?.start(scope) == true
          if (started) {
            _sessionState.value = LiveSessionState.Listening(0f)
          } else {
            _sessionState.value = LiveSessionState.Error("Microphone hardware failed to start. Verify microphone permissions.")
          }
        }
        return
      }

      val content = serverMessage.serverContent
      if (content != null) {
        // Handle User Interruption / Barge-in
        if (content.interrupted == true) {
          Log.d(TAG, "Server signaled user interruption / barge-in. Flushing audio output.")
          audioPlayer?.interrupt()
          currentAssistantTurnId = null
          assistantTurnStringBuilder.clear()
          _sessionState.value = LiveSessionState.Listening(0f)
          return
        }

        val modelTurn = content.modelTurn
        if (modelTurn != null && modelTurn.parts.isNotEmpty()) {
          for (part in modelTurn.parts) {
            // Text transcript part
            if (!part.text.isNullOrBlank()) {
              appendAssistantTranscript(part.text)
            }

            // Audio PCM chunk part
            val inlineData = part.inlineData
            if (inlineData != null && inlineData.data.isNotBlank()) {
              try {
                val pcmBytes = Base64.decode(inlineData.data, Base64.DEFAULT)
                audioPlayer?.enqueuePcmChunk(pcmBytes)
                _sessionState.value = LiveSessionState.Speaking(0.3f)
              } catch (e: Exception) {
                Log.w(TAG, "Failed to decode base64 audio chunk", e)
              }
            }
          }
        }

        if (content.turnComplete == true) {
          currentAssistantTurnId = null
          assistantTurnStringBuilder.clear()
          if (audioPlayer?.isPlaying?.value != true) {
            _sessionState.value = LiveSessionState.Listening(0f)
          }
        }
      }
    } catch (e: Exception) {
      Log.w(TAG, "Error parsing server message: ${e.message}")
    }
  }

  private fun sendRealtimeAudioChunk(base64Chunk: String) {
    val ws = activeWebSocket ?: return
    if (_sessionState.value is LiveSessionState.Ended || _sessionState.value is LiveSessionState.Error) return

    val message = LiveClientMessage(
      realtimeInput = LiveRealtimeInput(
        mediaChunks = listOf(
          LiveBlob(
            mimeType = "audio/pcm;rate=${LiveAudioRecorder.SAMPLE_RATE_HZ}",
            data = base64Chunk,
          )
        )
      )
    )

    try {
      val json = clientMessageAdapter.toJson(message)
      ws.send(json)
    } catch (e: Exception) {
      Log.w(TAG, "Failed to send realtime audio chunk: ${e.message}")
    }
  }

  private fun appendAssistantTranscript(textChunk: String) {
    assistantTurnStringBuilder.append(textChunk)
    val fullText = assistantTurnStringBuilder.toString()

    val turnId = currentAssistantTurnId ?: UUID.randomUUID().toString().also {
      currentAssistantTurnId = it
    }

    val currentList = _transcript.value.toMutableList()
    val existingIndex = currentList.indexOfFirst { it.id == turnId }
    if (existingIndex >= 0) {
      currentList[existingIndex] = LiveTranscriptItem(
        id = turnId,
        isUser = false,
        text = fullText,
        timestamp = currentList[existingIndex].timestamp,
      )
    } else {
      currentList.add(
        LiveTranscriptItem(
          id = turnId,
          isUser = false,
          text = fullText,
        )
      )
    }
    _transcript.value = currentList
  }

  override fun setMicrophoneMuted(muted: Boolean) {
    _isMicrophoneMuted.value = muted
    audioRecorder?.setMuted(muted)
  }

  private fun cleanupResources(sendCloseFrame: Boolean) {
    activeSessionToken = null
    try {
      if (sendCloseFrame) {
        activeWebSocket?.close(1000, "Live session terminated")
      }
    } catch (e: Exception) {
      Log.w(TAG, "Error closing websocket: ${e.message}")
    } finally {
      activeWebSocket = null
    }

    audioRecorder?.stop()
    audioRecorder = null

    audioPlayer?.stop()
    audioPlayer = null

    activeScope?.cancel()
    activeScope = null

    currentAssistantTurnId = null
    assistantTurnStringBuilder.clear()
  }

  override suspend fun stopSession(): AppResult<Unit> {
    cleanupResources(sendCloseFrame = true)
    if (_sessionState.value !is LiveSessionState.Idle) {
      _sessionState.value = LiveSessionState.Ended("Live session ended")
    }
    return AppResult.Success(Unit)
  }
}
