package com.example.core.ai.live

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.LinkedBlockingQueue
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Handles real-time microphone capture at 16kHz 16-bit Mono PCM.
 */
class LiveAudioRecorder(
  private val onAudioChunkAvailable: (base64Chunk: String) -> Unit,
  private val onAmplitudeChanged: (amplitude: Float) -> Unit,
) {
  companion object {
    private const val TAG = "LiveAudioRecorder"
    const val SAMPLE_RATE_HZ = 16000
    private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    private const val CHUNK_SIZE_BYTES = 2048 // ~64ms of audio at 16kHz 16-bit
  }

  private var audioRecord: AudioRecord? = null
  private var recordingJob: Job? = null
  private var isMuted = false

  fun setMuted(muted: Boolean) {
    isMuted = muted
    if (muted) {
      onAmplitudeChanged(0f)
    }
  }

  @SuppressLint("MissingPermission")
  fun start(scope: CoroutineScope): Boolean {
    stop()
    try {
      val minBufferSize = AudioRecord.getMinBufferSize(
        SAMPLE_RATE_HZ,
        CHANNEL_CONFIG,
        AUDIO_FORMAT,
      )
      val bufferSize = maxOf(minBufferSize, CHUNK_SIZE_BYTES * 2)

      val record = AudioRecord(
        MediaRecorder.AudioSource.VOICE_COMMUNICATION,
        SAMPLE_RATE_HZ,
        CHANNEL_CONFIG,
        AUDIO_FORMAT,
        bufferSize,
      )

      if (record.state != AudioRecord.STATE_INITIALIZED) {
        Log.e(TAG, "AudioRecord initialization failed")
        record.release()
        return false
      }

      record.startRecording()
      audioRecord = record

      recordingJob = scope.launch(Dispatchers.IO) {
        val audioBuffer = ByteArray(CHUNK_SIZE_BYTES)
        while (isActive && audioRecord != null) {
          val readBytes = record.read(audioBuffer, 0, audioBuffer.size)
          if (readBytes > 0) {
            if (!isMuted) {
              val amplitude = calculateRmsAmplitude(audioBuffer, readBytes)
              onAmplitudeChanged(amplitude)

              val validBytes = if (readBytes == audioBuffer.size) {
                audioBuffer
              } else {
                audioBuffer.copyOf(readBytes)
              }
              val base64Chunk = Base64.encodeToString(validBytes, Base64.NO_WRAP)
              onAudioChunkAvailable(base64Chunk)
            } else {
              onAmplitudeChanged(0f)
            }
          }
        }
      }
      return true
    } catch (e: Throwable) {
      Log.e(TAG, "Error starting AudioRecord: ${e.message}", e)
      stop()
      return false
    }
  }

  fun stop() {
    recordingJob?.cancel()
    recordingJob = null
    try {
      audioRecord?.let {
        if (it.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
          it.stop()
        }
        it.release()
      }
    } catch (e: Throwable) {
      Log.w(TAG, "Error stopping AudioRecord: ${e.message}")
    } finally {
      audioRecord = null
      onAmplitudeChanged(0f)
    }
  }

  private fun calculateRmsAmplitude(buffer: ByteArray, length: Int): Float {
    if (length <= 0) return 0f
    var sum = 0.0
    val shortCount = length / 2
    val byteBuffer = ByteBuffer.wrap(buffer, 0, length).order(ByteOrder.LITTLE_ENDIAN)
    for (i in 0 until shortCount) {
      val sample = byteBuffer.short.toDouble()
      sum += sample * sample
    }
    val rms = sqrt(sum / shortCount)
    // Normalize 0..32767 to 0..1 with a reasonable dynamic range
    val normalized = (rms / 8000.0).toFloat().coerceIn(0f, 1f)
    return normalized
  }
}

/**
 * Handles real-time AI audio output at 24kHz 16-bit Mono PCM with instant interruption / barge-in.
 */
class LiveAudioPlayer(
  private val onAmplitudeChanged: (amplitude: Float) -> Unit,
  private val onPlaybackStateChanged: (isPlaying: Boolean) -> Unit,
) {
  companion object {
    private const val TAG = "LiveAudioPlayer"
    const val SAMPLE_RATE_HZ = 24000
    private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO
    private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
  }

  private var audioTrack: AudioTrack? = null
  private var playbackJob: Job? = null
  private val audioQueue = LinkedBlockingQueue<ByteArray>()
  private val _isPlaying = MutableStateFlow(false)
  val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

  fun start(scope: CoroutineScope): Boolean {
    stop()
    try {
      val minBufferSize = AudioTrack.getMinBufferSize(
        SAMPLE_RATE_HZ,
        CHANNEL_CONFIG,
        AUDIO_FORMAT,
      )
      val bufferSize = maxOf(minBufferSize, 8192)

      val track = AudioTrack.Builder()
        .setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        )
        .setAudioFormat(
          AudioFormat.Builder()
            .setEncoding(AUDIO_FORMAT)
            .setSampleRate(SAMPLE_RATE_HZ)
            .setChannelMask(CHANNEL_CONFIG)
            .build()
        )
        .setBufferSizeInBytes(bufferSize)
        .setTransferMode(AudioTrack.MODE_STREAM)
        .build()

      if (track.state != AudioTrack.STATE_INITIALIZED) {
        Log.e(TAG, "AudioTrack initialization failed")
        track.release()
        return false
      }

      track.play()
      audioTrack = track

      playbackJob = scope.launch(Dispatchers.IO) {
        while (isActive && audioTrack != null) {
          try {
            val chunk = audioQueue.poll(100, java.util.concurrent.TimeUnit.MILLISECONDS)
            if (chunk != null && chunk.isNotEmpty()) {
              _isPlaying.value = true
              onPlaybackStateChanged(true)

              val amplitude = calculateRmsAmplitude(chunk)
              onAmplitudeChanged(amplitude)

              track.write(chunk, 0, chunk.size)
            } else {
              if (_isPlaying.value && audioQueue.isEmpty()) {
                _isPlaying.value = false
                onPlaybackStateChanged(false)
                onAmplitudeChanged(0f)
              }
            }
          } catch (e: InterruptedException) {
            break
          }
        }
      }
      return true
    } catch (e: Throwable) {
      Log.e(TAG, "Error starting AudioTrack: ${e.message}", e)
      stop()
      return false
    }
  }

  fun enqueuePcmChunk(pcmData: ByteArray) {
    if (pcmData.isNotEmpty()) {
      audioQueue.offer(pcmData)
    }
  }

  /**
   * Interrupts and flushes current buffered speech immediately (barge-in).
   */
  fun interrupt() {
    audioQueue.clear()
    try {
      audioTrack?.let {
        if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
          it.pause()
          it.flush()
          it.play()
        }
      }
    } catch (e: Throwable) {
      Log.w(TAG, "Error flushing AudioTrack on interrupt: ${e.message}")
    } finally {
      _isPlaying.value = false
      onPlaybackStateChanged(false)
      onAmplitudeChanged(0f)
    }
  }

  fun stop() {
    playbackJob?.cancel()
    playbackJob = null
    audioQueue.clear()
    try {
      audioTrack?.let {
        if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
          it.stop()
        }
        it.release()
      }
    } catch (e: Throwable) {
      Log.w(TAG, "Error stopping AudioTrack: ${e.message}")
    } finally {
      audioTrack = null
      _isPlaying.value = false
      onPlaybackStateChanged(false)
      onAmplitudeChanged(0f)
    }
  }

  private fun calculateRmsAmplitude(buffer: ByteArray): Float {
    if (buffer.isEmpty()) return 0f
    var sum = 0.0
    val shortCount = buffer.size / 2
    val byteBuffer = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN)
    for (i in 0 until shortCount) {
      val sample = byteBuffer.short.toDouble()
      sum += sample * sample
    }
    val rms = sqrt(sum / shortCount)
    val normalized = (rms / 7000.0).toFloat().coerceIn(0f, 1f)
    return normalized
  }
}
