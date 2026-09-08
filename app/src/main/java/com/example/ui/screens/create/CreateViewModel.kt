package com.example.ui.screens.create

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.ai.image.GeminiImageGenerationService
import com.example.core.ai.image.ImageGenerationService
import com.example.core.ai.music.KasaMusicGenerationService
import com.example.core.ai.music.MusicGenerationService
import com.example.core.audio.KasaAudioPlayer
import com.example.core.config.AppConfig
import com.example.core.result.AppResult
import com.example.data.local.KasaDatabase
import com.example.data.model.GeneratedImage
import com.example.data.model.GeneratedSong
import com.example.data.model.UserProfile
import com.example.data.repository.GeneratedImageRepository
import com.example.data.repository.MusicRepository
import com.example.data.repository.MusicRepositoryImpl
import com.example.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class CreateViewModel(
  private val userRepository: UserRepository,
  private val generatedImageRepository: GeneratedImageRepository,
  private val imageGenerationService: ImageGenerationService? = null,
  private val musicRepository: MusicRepository? = null,
  private val musicGenerationService: MusicGenerationService? = null,
  private val applicationContext: Context? = null,
) : ViewModel() {

  private val _uiState = MutableStateFlow(CreateUiState())
  val uiState: StateFlow<CreateUiState> = _uiState.asStateFlow()

  private var activeService: ImageGenerationService? = imageGenerationService
  private var activeMusicRepo: MusicRepository? = musicRepository
  private var activeMusicService: MusicGenerationService? = musicGenerationService
  private var audioPlayer: KasaAudioPlayer? = null

  // Reflectively tested field: MUST remain named 'isRequestInFlight'
  private var isRequestInFlight = false
  private var isMusicRequestInFlight = false

  val starterPrompts = listOf(
    StarterPrompt(
      title = "Accra Sunset",
      prompt = "Cinematic sunset over Accra city with the historic Jamestown lighthouse in warm golden light",
      tag = "Landscapes",
    ),
    StarterPrompt(
      title = "Futuristic Africa",
      prompt = "Futuristic African eco-city with lush botanical sky towers and solar architecture in sunlight",
      tag = "Sci-Fi & Future",
    ),
    StarterPrompt(
      title = "Cape Coast",
      prompt = "Golden hour ocean view over the rocky coastline of Cape Coast with rolling Atlantic waves",
      tag = "Nature",
    ),
    StarterPrompt(
      title = "Authentic Cuisine",
      prompt = "A delicious bowl of steaming Ghanaian Jollof rice, grilled tilapia, and sweet fried plantain",
      tag = "Culture & Food",
    ),
    StarterPrompt(
      title = "Geometric Patterns",
      prompt = "Minimalist mobile wallpaper featuring clean geometric African textile patterns with gold and indigo tones",
      tag = "Wallpapers",
    ),
  )

  val musicStarterPrompts = listOf(
    MusicStarterPrompt(
      title = "Ghanaian Afrobeats",
      prompt = "An upbeat, energetic Ghanaian Afrobeats summer anthem with heavy percussion, log drums, and catchy melodic hooks",
      genre = "Ghanaian Afrobeats",
    ),
    MusicStarterPrompt(
      title = "Highlife Classic",
      prompt = "Warm vintage Highlife groove featuring bright brass horns, rhythmic rhythm guitars, and sweet soulful harmonies",
      genre = "Highlife",
    ),
    MusicStarterPrompt(
      title = "Gospel Praise",
      prompt = "An uplifting contemporary Gospel praise track with heartfelt acoustic piano, choir vocal harmonies, and celebration",
      genre = "Gospel",
    ),
    MusicStarterPrompt(
      title = "Worship",
      prompt = "A deep, contemplative worship ballad with ambient electric guitar pads, gentle strings, and moving vocals",
      genre = "Worship",
    ),
    MusicStarterPrompt(
      title = "Love & Romance",
      prompt = "A smooth romantic R&B and Afropop fusion song with acoustic guitar plucks and warm heartfelt melodies",
      genre = "Love",
    ),
    MusicStarterPrompt(
      title = "Motivation",
      prompt = "An inspiring and triumphant motivational anthem with driving percussion, dynamic synth swells, and bold confidence",
      genre = "Motivation",
    ),
    MusicStarterPrompt(
      title = "Hip-hop",
      prompt = "A hard-hitting modern hip-hop beat with 808 bass, crisp hi-hats, and rhythmic bounce",
      genre = "Hip-hop",
    ),
    MusicStarterPrompt(
      title = "Chill Groove",
      prompt = "A calming lofi chillout track with mellow keys, soothing vinyl crackle, and laid-back downtempo drums",
      genre = "Chill",
    ),
  )

  val genreOptions = listOf(
    "Ghanaian Afrobeats",
    "Highlife",
    "Gospel",
    "Worship",
    "Love",
    "Motivation",
    "Hip-hop",
    "Chill",
  )

  val moodOptions = listOf(
    "Uplifting",
    "Energetic",
    "Soulful",
    "Romantic",
    "Reflective",
    "Celebratory",
  )

  val languageOptions = listOf(
    "English",
    "Twi",
    "Fante",
    "Ga",
    "Ewe",
  )

  init {
    if (applicationContext != null) {
      val appContext = applicationContext.applicationContext
      if (activeService == null) {
        activeService = GeminiImageGenerationService(
          context = appContext,
          imageRepository = generatedImageRepository,
        )
      }
      if (activeMusicRepo == null) {
        try {
          val db = KasaDatabase.getDatabase(appContext)
          activeMusicRepo = MusicRepositoryImpl(db.generatedSongDao())
        } catch (e: Exception) {
          Log.e("CreateViewModel", "Error creating MusicRepository: ${e.message}", e)
        }
      }
      if (activeMusicService == null && activeMusicRepo != null) {
        activeMusicService = KasaMusicGenerationService(
          context = appContext,
          musicRepository = activeMusicRepo!!,
          baseUrl = AppConfig.getMusicBackendUrl(appContext),
        )
      }
    }

    viewModelScope.launch {
      userRepository.getCurrentUser().collectLatest { user ->
        _uiState.update { it.copy(activeUser = user) }
        loadHistoryForUser(user.id)
        loadMusicHistoryForUser(user.id)
      }
    }
  }

  fun setContextService(context: Context) {
    val appContext = context.applicationContext
    if (activeService == null) {
      activeService = GeminiImageGenerationService(
        context = appContext,
        imageRepository = generatedImageRepository,
      )
    }

    if (activeMusicRepo == null) {
      try {
        val db = KasaDatabase.getDatabase(appContext)
        val repo = MusicRepositoryImpl(db.generatedSongDao())
        activeMusicRepo = repo
        _uiState.value.activeUser?.id?.let { loadMusicHistoryForUser(it) }
      } catch (e: Exception) {
        Log.e("CreateViewModel", "Error creating MusicRepository: ${e.message}", e)
      }
    }

    if (activeMusicRepo != null) {
      val backendUrl = AppConfig.getMusicBackendUrl(appContext)
      // Create or refresh service with current configured backend URL
      activeMusicService = KasaMusicGenerationService(
        context = appContext,
        musicRepository = activeMusicRepo!!,
        baseUrl = backendUrl,
      )
      refreshCredits()
    }

    if (audioPlayer == null) {
      audioPlayer = KasaAudioPlayer(appContext).also { player ->
        viewModelScope.launch {
          player.playerState.collectLatest { pState ->
            _uiState.update { current ->
              current.copy(
                isPlayingAudio = pState.isPlaying,
                isBufferingAudio = pState.isBuffering,
                playbackPositionMs = pState.currentPositionMs,
                playbackDurationMs = pState.durationMs,
                musicErrorMessage = pState.errorMessage ?: current.musicErrorMessage,
              )
            }
          }
        }
      }
    }
  }

  // ==========================================
  // Mode Selection
  // ==========================================

  fun selectMode(mode: CreateStudioMode) {
    _uiState.update { it.copy(selectedMode = mode) }
    if (mode == CreateStudioMode.MUSIC) {
      refreshCredits()
    }
  }

  // ==========================================
  // Image Generation
  // ==========================================

  private fun loadHistoryForUser(userId: String) {
    viewModelScope.launch {
      generatedImageRepository.getImagesForUser(userId).collectLatest { images ->
        _uiState.update { it.copy(history = images) }
      }
    }
  }

  fun updatePrompt(newPrompt: String) {
    _uiState.update { it.copy(prompt = newPrompt, errorMessage = null) }
  }

  fun selectAspectRatio(option: AspectRatioOption) {
    _uiState.update { it.copy(selectedAspectRatio = option) }
  }

  fun selectStarterPrompt(prompt: StarterPrompt) {
    _uiState.update { it.copy(prompt = prompt.prompt, errorMessage = null) }
  }

  fun generateImage() {
    val state = _uiState.value
    if (state.isGenerating || isRequestInFlight) return

    val cleanPrompt = state.prompt.trim()
    if (cleanPrompt.isBlank()) {
      _uiState.update { it.copy(errorMessage = "Please enter an image prompt description.") }
      return
    }

    val userId = state.activeUser?.id ?: "usr_default_kasa"
    val service = activeService

    if (service == null) {
      _uiState.update { it.copy(errorMessage = "Image generation service is initializing. Please try again.") }
      return
    }

    isRequestInFlight = true
    _uiState.update { it.copy(isGenerating = true, errorMessage = null) }

    viewModelScope.launch {
      try {
        val result = service.generateImage(
          userId = userId,
          prompt = cleanPrompt,
          aspectRatio = state.selectedAspectRatio.value,
        )

        when (result) {
          is AppResult.Success -> {
            _uiState.update {
              it.copy(
                isGenerating = false,
                activeImage = result.data,
                errorMessage = null,
              )
            }
          }
          is AppResult.Error -> {
            _uiState.update {
              it.copy(
                isGenerating = false,
                errorMessage = result.error.message,
              )
            }
          }
          is AppResult.Loading -> {
            _uiState.update { it.copy(isGenerating = true) }
          }
        }
      } finally {
        isRequestInFlight = false
      }
    }
  }

  fun regenerateImage() {
    val currentActive = _uiState.value.activeImage
    if (currentActive != null) {
      _uiState.update { it.copy(prompt = currentActive.prompt) }
    }
    generateImage()
  }

  fun selectHistoryImage(image: GeneratedImage) {
    _uiState.update { it.copy(activeImage = image, errorMessage = null) }
  }

  fun clearActiveImage() {
    _uiState.update { it.copy(activeImage = null) }
  }

  fun deleteImage(id: String) {
    viewModelScope.launch {
      generatedImageRepository.deleteImage(id)
      if (_uiState.value.activeImage?.id == id) {
        _uiState.update { it.copy(activeImage = null) }
      }
    }
  }

  fun saveImageToDevice(context: Context, image: GeneratedImage) {
    viewModelScope.launch {
      val success = withContext(Dispatchers.IO) {
        try {
          val sourceFile = File(image.imagePath)
          if (!sourceFile.exists()) return@withContext false

          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
              put(MediaStore.Images.Media.DISPLAY_NAME, "KASA_${System.currentTimeMillis()}.jpg")
              put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
              put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/KasaAI")
              put(MediaStore.Images.Media.IS_PENDING, 1)
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
              resolver.openOutputStream(uri)?.use { out ->
                FileInputStream(sourceFile).use { input ->
                  input.copyTo(out)
                }
              }
              contentValues.clear()
              contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
              resolver.update(uri, contentValues, null, null)
              true
            } else {
              false
            }
          } else {
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val kasaDir = File(picturesDir, "KasaAI")
            if (!kasaDir.exists()) kasaDir.mkdirs()
            val destFile = File(kasaDir, "KASA_${System.currentTimeMillis()}.jpg")
            FileInputStream(sourceFile).use { input ->
              FileOutputStream(destFile).use { output ->
                input.copyTo(output)
              }
            }
            true
          }
        } catch (e: Exception) {
          false
        }
      }

      if (success) {
        _uiState.update { it.copy(saveStatusMessage = "Image saved to Pictures/KasaAI successfully.") }
      } else {
        _uiState.update { it.copy(errorMessage = "Could not save image to device storage.") }
      }
    }
  }

  fun shareImage(context: Context, image: GeneratedImage) {
    try {
      val file = File(image.imagePath)
      if (!file.exists()) {
        _uiState.update { it.copy(errorMessage = "Image file not found on device.") }
        return
      }

      val authority = "${context.packageName}.fileprovider"
      val contentUri: Uri = FileProvider.getUriForFile(context, authority, file)

      val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = image.mimeType
        putExtra(Intent.EXTRA_STREAM, contentUri)
        putExtra(Intent.EXTRA_TEXT, "${image.prompt} — Created with KASA AI")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }

      val chooser = Intent.createChooser(shareIntent, "Share Image")
      chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(chooser)
    } catch (e: Exception) {
      _uiState.update { it.copy(errorMessage = "Failed to open sharing interface.") }
    }
  }

  fun clearErrorMessage() {
    _uiState.update { it.copy(errorMessage = null) }
  }

  fun clearSaveStatusMessage() {
    _uiState.update { it.copy(saveStatusMessage = null) }
  }

  // ==========================================
  // Music Generation
  // ==========================================

  private fun loadMusicHistoryForUser(userId: String) {
    val repo = activeMusicRepo ?: return
    viewModelScope.launch {
      repo.getSongsForUser(userId).collectLatest { songs ->
        _uiState.update { it.copy(musicHistory = songs) }
      }
    }
  }

  fun refreshCredits(targetUserId: String? = null) {
    val uid = targetUserId ?: _uiState.value.activeUser?.id ?: "usr_default_kasa"
    val service = activeMusicService ?: return
    viewModelScope.launch {
      val res = service.getCredits(uid)
      if (res is AppResult.Success) {
        _uiState.update { it.copy(musicCredits = res.data) }
      }
    }
  }

  fun updateMusicPrompt(newPrompt: String) {
    _uiState.update { it.copy(musicPrompt = newPrompt, musicErrorMessage = null) }
  }

  fun updateMusicTitle(newTitle: String) {
    _uiState.update { it.copy(musicTitle = newTitle) }
  }

  fun selectMusicGenre(genre: String) {
    _uiState.update { it.copy(musicGenre = genre) }
  }

  fun selectMusicMood(mood: String) {
    _uiState.update { it.copy(musicMood = mood) }
  }

  fun selectMusicLanguage(language: String) {
    _uiState.update { it.copy(musicLanguage = language) }
  }

  fun setInstrumental(instrumental: Boolean) {
    _uiState.update { it.copy(isInstrumental = instrumental) }
  }

  fun selectMusicStarter(starter: MusicStarterPrompt) {
    _uiState.update {
      it.copy(
        musicPrompt = starter.prompt,
        musicGenre = starter.genre,
        musicErrorMessage = null,
      )
    }
  }

  fun generateMusic(context: Context? = null) {
    val state = _uiState.value
    if (state.isMusicGenerating || isMusicRequestInFlight) return

    val cleanPrompt = state.musicPrompt.trim()
    if (cleanPrompt.isBlank()) {
      _uiState.update { it.copy(musicErrorMessage = "Please describe the song you want to create.") }
      return
    }

    val userId = state.activeUser?.id ?: "usr_default_kasa"

    // Auto-recover service if null using provided context or applicationContext
    val targetContext = context?.applicationContext ?: applicationContext
    if (activeMusicService == null && targetContext != null) {
      if (activeMusicRepo == null) {
        try {
          val db = KasaDatabase.getDatabase(targetContext)
          activeMusicRepo = MusicRepositoryImpl(db.generatedSongDao())
        } catch (e: Exception) {
          Log.e("CreateViewModel", "Error acquiring database: ${e.message}")
        }
      }
      if (activeMusicRepo != null) {
        activeMusicService = KasaMusicGenerationService(
          context = targetContext,
          musicRepository = activeMusicRepo!!,
          baseUrl = AppConfig.getMusicBackendUrl(targetContext),
        )
      }
    }

    val service = activeMusicService

    if (service == null) {
      _uiState.update {
        it.copy(
          musicErrorMessage = "Music service is not ready. Please verify your backend connection."
        )
      }
      return
    }

    isMusicRequestInFlight = true
    _uiState.update {
      it.copy(
        isMusicGenerating = true,
        musicErrorMessage = null,
        musicStatusMessage = "Creating your song...",
      )
    }

    viewModelScope.launch {
      try {
        val result = service.generateMusic(
          userId = userId,
          prompt = cleanPrompt,
          genre = state.musicGenre,
          mood = state.musicMood,
          language = state.musicLanguage,
          title = state.musicTitle.takeIf { it.isNotBlank() },
          isInstrumental = state.isInstrumental,
          onStatusUpdate = { status ->
            _uiState.update { it.copy(musicStatusMessage = status) }
          }
        )

        when (result) {
          is AppResult.Success -> {
            val song = result.data
            activeMusicRepo?.saveSong(song)
            _uiState.update {
              it.copy(
                isMusicGenerating = false,
                activeSong = song,
                musicErrorMessage = null,
                musicStatusMessage = "Song ready 🎵",
              )
            }
            refreshCredits(userId)
          }
          is AppResult.Error -> {
            _uiState.update {
              it.copy(
                isMusicGenerating = false,
                musicErrorMessage = result.error.message,
                musicStatusMessage = "",
              )
            }
            refreshCredits(userId)
          }
          is AppResult.Loading -> {
            _uiState.update { it.copy(isMusicGenerating = true) }
          }
        }
      } finally {
        isMusicRequestInFlight = false
      }
    }
  }

  fun togglePlayPause() {
    val state = _uiState.value
    val song = state.activeSong ?: return
    if (state.isPlayingAudio) {
      audioPlayer?.pause()
    } else {
      audioPlayer?.play(song.audioUrl)
    }
  }

  fun seekAudio(targetFraction: Float) {
    val dur = _uiState.value.playbackDurationMs
    if (dur > 0) {
      val pos = (dur * targetFraction.coerceIn(0f, 1f)).toLong()
      audioPlayer?.seekTo(pos)
    }
  }

  fun playSong(song: GeneratedSong) {
    _uiState.update {
      it.copy(
        activeSong = song,
        musicErrorMessage = null,
        musicStatusMessage = "Song ready 🎵",
      )
    }
    audioPlayer?.play(song.audioUrl)
  }

  fun clearActiveSong() {
    audioPlayer?.stop()
    _uiState.update { it.copy(activeSong = null, musicStatusMessage = "") }
  }

  fun deleteSong(id: String) {
    val repo = activeMusicRepo ?: return
    viewModelScope.launch {
      repo.deleteSong(id)
      if (_uiState.value.activeSong?.id == id) {
        audioPlayer?.stop()
        _uiState.update { it.copy(activeSong = null) }
      }
    }
  }

  fun shareSong(context: Context, song: GeneratedSong) {
    try {
      val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Song: ${song.title}")
        putExtra(
          Intent.EXTRA_TEXT,
          "🎵 Listen to '${song.title}' created with KASA Create Studio!\n\nAudio: ${song.audioUrl}\n\nPrompt: \"${song.prompt}\""
        )
      }
      val chooser = Intent.createChooser(shareIntent, "Share Song")
      chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(chooser)
    } catch (e: Exception) {
      _uiState.update { it.copy(musicErrorMessage = "Failed to share song.") }
    }
  }

  fun clearMusicErrorMessage() {
    _uiState.update { it.copy(musicErrorMessage = null) }
  }

  override fun onCleared() {
    super.onCleared()
    audioPlayer?.release()
    audioPlayer = null
  }
}
