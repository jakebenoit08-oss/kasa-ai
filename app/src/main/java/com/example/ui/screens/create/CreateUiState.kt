package com.example.ui.screens.create

import com.example.data.model.GeneratedImage
import com.example.data.model.GeneratedSong
import com.example.data.model.UserMusicCredits
import com.example.data.model.UserProfile

enum class CreateStudioMode(val label: String, val iconEmoji: String) {
  IMAGE("Image", "🖼️"),
  MUSIC("Music", "🎵"),
}

enum class AspectRatioOption(val value: String, val label: String, val iconRatio: Float) {
  SQUARE("1:1", "Square (1:1)", 1.0f),
  LANDSCAPE("16:9", "Landscape (16:9)", 1.777f),
  PORTRAIT("9:16", "Portrait (9:16)", 0.5625f),
  STANDARD("4:3", "Standard (4:3)", 1.333f),
}

data class StarterPrompt(
  val title: String,
  val prompt: String,
  val tag: String,
)

data class MusicStarterPrompt(
  val title: String,
  val prompt: String,
  val genre: String,
)

data class CreateUiState(
  // Studio Mode
  val selectedMode: CreateStudioMode = CreateStudioMode.IMAGE,

  // Image Generation State
  val prompt: String = "",
  val selectedAspectRatio: AspectRatioOption = AspectRatioOption.SQUARE,
  val isGenerating: Boolean = false,
  val activeImage: GeneratedImage? = null,
  val history: List<GeneratedImage> = emptyList(),
  val errorMessage: String? = null,
  val saveStatusMessage: String? = null,
  val activeUser: UserProfile? = null,

  // Music Generation State
  val musicPrompt: String = "",
  val musicTitle: String = "",
  val musicGenre: String = "Ghanaian Afrobeats",
  val musicLanguage: String = "English",
  val musicMood: String = "Uplifting",
  val isInstrumental: Boolean = false,
  val isMusicGenerating: Boolean = false,
  val musicStatusMessage: String = "",
  val activeSong: GeneratedSong? = null,
  val musicErrorMessage: String? = null,
  val musicSaveStatusMessage: String? = null,
  val musicCredits: UserMusicCredits? = null,
  val musicHistory: List<GeneratedSong> = emptyList(),
  val isPlayingAudio: Boolean = false,
  val isBufferingAudio: Boolean = false,
  val playbackPositionMs: Long = 0L,
  val playbackDurationMs: Long = 0L,
) {
  val isPromptValid: Boolean
    get() = prompt.trim().isNotBlank()

  val isMusicPromptValid: Boolean
    get() = musicPrompt.trim().isNotBlank()
}

