package com.example.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class AudioPlayerState(
  val isPlaying: Boolean = false,
  val isBuffering: Boolean = false,
  val currentPositionMs: Long = 0L,
  val durationMs: Long = 0L,
  val currentUrl: String? = null,
  val errorMessage: String? = null,
)

class KasaAudioPlayer(private val context: Context) {

  private var mediaPlayer: MediaPlayer? = null
  private var progressJob: Job? = null
  private val scope = CoroutineScope(Dispatchers.Main)

  private val _playerState = MutableStateFlow(AudioPlayerState())
  val playerState: StateFlow<AudioPlayerState> = _playerState.asStateFlow()

  fun play(url: String) {
    if (url.isBlank()) return

    // If already loaded same url, just toggle
    if (_playerState.value.currentUrl == url && mediaPlayer != null) {
      if (mediaPlayer?.isPlaying == true) {
        pause()
      } else {
        resume()
      }
      return
    }

    // Stop existing
    stop()

    _playerState.value = AudioPlayerState(
      isBuffering = true,
      currentUrl = url,
    )

    try {
      val player = MediaPlayer().apply {
        setAudioAttributes(
          AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .build()
        )
        setDataSource(url)
        setOnPreparedListener { mp ->
          val dur = mp.duration.toLong().coerceAtLeast(0L)
          _playerState.value = _playerState.value.copy(
            isPlaying = true,
            isBuffering = false,
            durationMs = dur,
          )
          mp.start()
          startProgressTracking()
        }
        setOnCompletionListener {
          _playerState.value = _playerState.value.copy(
            isPlaying = false,
            currentPositionMs = _playerState.value.durationMs,
          )
          stopProgressTracking()
        }
        setOnErrorListener { _, what, extra ->
          Log.e("KasaAudioPlayer", "MediaPlayer error: what=$what, extra=$extra")
          _playerState.value = _playerState.value.copy(
            isPlaying = false,
            isBuffering = false,
            errorMessage = "Playback failed. Please try again.",
          )
          stopProgressTracking()
          true
        }
        prepareAsync()
      }
      mediaPlayer = player
    } catch (e: Exception) {
      Log.e("KasaAudioPlayer", "Failed to initialize player: ${e.message}")
      _playerState.value = AudioPlayerState(
        errorMessage = "Could not stream audio: ${e.message}",
      )
    }
  }

  fun pause() {
    try {
      mediaPlayer?.let {
        if (it.isPlaying) {
          it.pause()
          _playerState.value = _playerState.value.copy(isPlaying = false)
        }
      }
    } catch (e: Exception) {
      Log.w("KasaAudioPlayer", "Error pausing: ${e.message}")
    }
  }

  fun resume() {
    try {
      mediaPlayer?.let {
        it.start()
        _playerState.value = _playerState.value.copy(isPlaying = true)
        startProgressTracking()
      }
    } catch (e: Exception) {
      Log.w("KasaAudioPlayer", "Error resuming: ${e.message}")
    }
  }

  fun seekTo(positionMs: Long) {
    try {
      mediaPlayer?.let {
        val target = positionMs.toInt().coerceIn(0, it.duration)
        it.seekTo(target)
        _playerState.value = _playerState.value.copy(currentPositionMs = positionMs)
      }
    } catch (e: Exception) {
      Log.w("KasaAudioPlayer", "Error seeking: ${e.message}")
    }
  }

  fun stop() {
    stopProgressTracking()
    try {
      mediaPlayer?.let {
        if (it.isPlaying) {
          it.stop()
        }
        it.reset()
        it.release()
      }
    } catch (e: Exception) {
      Log.w("KasaAudioPlayer", "Error stopping: ${e.message}")
    } finally {
      mediaPlayer = null
      _playerState.value = AudioPlayerState()
    }
  }

  fun release() {
    stop()
  }

  private fun startProgressTracking() {
    stopProgressTracking()
    progressJob = scope.launch {
      while (isActive) {
        val player = mediaPlayer
        if (player != null && player.isPlaying) {
          try {
            val cur = player.currentPosition.toLong()
            val dur = player.duration.toLong().coerceAtLeast(0L)
            _playerState.value = _playerState.value.copy(
              currentPositionMs = cur,
              durationMs = dur,
            )
          } catch (e: Exception) {
            // Ignored if released concurrently
          }
        }
        delay(250)
      }
    }
  }

  private fun stopProgressTracking() {
    progressJob?.cancel()
    progressJob = null
  }
}
