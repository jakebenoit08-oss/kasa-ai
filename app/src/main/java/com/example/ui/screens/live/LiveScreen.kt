package com.example.ui.screens.live

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.core.ai.live.LiveSessionState
import com.example.core.ai.live.LiveVoiceOption
import com.example.ui.components.KasaBadge
import com.example.ui.components.KasaPrimaryButton
import com.example.ui.theme.KasaSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveScreen(
  viewModel: LiveViewModel,
  onNavigateToChat: () -> Unit,
  onNavigateToSettings: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val state by viewModel.uiState.collectAsState()

  // Permission Launcher
  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
  ) { isGranted ->
    viewModel.updateMicrophonePermission(isGranted)
    if (isGranted) {
      viewModel.startLiveSession()
    }
  }

  // Check initial permission
  LaunchedEffect(Unit) {
    val isGranted = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.RECORD_AUDIO,
    ) == PackageManager.PERMISSION_GRANTED
    viewModel.updateMicrophonePermission(isGranted)
  }

  val onStartRequested = {
    val isGranted = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.RECORD_AUDIO,
    ) == PackageManager.PERMISSION_GRANTED
    if (isGranted) {
      viewModel.startLiveSession()
    } else {
      permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "KASA LIVE",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.width(8.dp))
            val isLiveActive = state.sessionState is LiveSessionState.Listening ||
                state.sessionState is LiveSessionState.Speaking ||
                state.sessionState is LiveSessionState.Thinking
            KasaBadge(
              text = if (isLiveActive) "Live Voice" else "Real-Time AI",
              containerColor = if (isLiveActive) {
                MaterialTheme.colorScheme.primaryContainer
              } else {
                MaterialTheme.colorScheme.surfaceVariant
              },
              contentColor = if (isLiveActive) {
                MaterialTheme.colorScheme.onPrimaryContainer
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant
              },
            )
          }
        },
        actions = {
          IconButton(
            onClick = { viewModel.setVoiceSelectionSheetVisible(true) },
            modifier = Modifier.testTag("live_voice_selector_button"),
          ) {
            Icon(
              imageVector = Icons.Outlined.RecordVoiceOver,
              contentDescription = "Select Voice",
              tint = MaterialTheme.colorScheme.onSurface,
            )
          }
          IconButton(
            onClick = onNavigateToSettings,
            modifier = Modifier.testTag("live_settings_button"),
          ) {
            Icon(
              imageVector = Icons.Outlined.Settings,
              contentDescription = "Settings",
              tint = MaterialTheme.colorScheme.onSurface,
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background,
        ),
      )
    },
    modifier = modifier.testTag("live_screen"),
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = KasaSpacing.medium),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      // Top Error Banner
      AnimatedVisibility(
        visible = state.errorMessage != null,
        enter = fadeIn(),
        exit = fadeOut(),
      ) {
        if (state.errorMessage != null) {
          LiveErrorBanner(
            errorMessage = state.errorMessage!!,
            onRetry = onStartRequested,
            onDismiss = { viewModel.dismissError() },
          )
        }
      }

      Spacer(modifier = Modifier.height(KasaSpacing.small))

      // Status Header
      LiveStatusHeader(state = state.sessionState)

      // Central Interactive Voice Visualizer Node
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth(),
        contentAlignment = Alignment.Center,
      ) {
        LiveVoiceOrb(
          state = state.sessionState,
          isMuted = state.isMuted,
          onOrbClicked = {
            when (state.sessionState) {
              is LiveSessionState.Idle, is LiveSessionState.Ended, is LiveSessionState.Error -> {
                onStartRequested()
              }
              else -> {
                viewModel.stopLiveSession()
              }
            }
          },
        )
      }

      // Live Subtitle Transcript Container
      if (state.transcript.isNotEmpty()) {
        LiveTranscriptCard(
          transcript = state.transcript,
          modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .padding(bottom = KasaSpacing.small),
        )
      } else {
        LiveGuidanceTips(
          sessionState = state.sessionState,
          modifier = Modifier.padding(bottom = KasaSpacing.medium),
        )
      }

      // Bottom Control Panel
      LiveBottomControls(
        sessionState = state.sessionState,
        selectedVoice = state.selectedVoice,
        isMuted = state.isMuted,
        onToggleMute = { viewModel.toggleMute() },
        onStartSession = onStartRequested,
        onStopSession = { viewModel.stopLiveSession() },
        onOpenVoiceSelector = { viewModel.setVoiceSelectionSheetVisible(true) },
        onNavigateToChat = onNavigateToChat,
      )

      Spacer(modifier = Modifier.height(KasaSpacing.small))
    }

    // Voice Selection Bottom Sheet
    if (state.showVoiceSelectionSheet) {
      LiveVoiceSelectionBottomSheet(
        selectedVoice = state.selectedVoice,
        availableVoices = state.availableVoices,
        onSelectVoice = { voice ->
          viewModel.selectVoice(voice)
        },
        onDismiss = { viewModel.setVoiceSelectionSheetVisible(false) },
      )
    }

    // Permission Rationale Dialog
    if (state.showPermissionRationale && !state.hasMicrophonePermission) {
      MicrophonePermissionDialog(
        onRequestPermission = {
          viewModel.setPermissionRationaleVisible(false)
          permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        },
        onDismiss = { viewModel.setPermissionRationaleVisible(false) },
      )
    }
  }
}

@Composable
private fun LiveStatusHeader(state: LiveSessionState) {
  val (headline, subtitle) = when (state) {
    is LiveSessionState.Idle -> "KASA Live" to "Tap the microphone to start talking in real time"
    is LiveSessionState.Connecting -> "Connecting..." to "Establishing real-time bidirectional audio stream"
    is LiveSessionState.Listening -> "KASA is listening" to "Speak naturally • English, Twi, Ga, Ewe, Pidgin"
    is LiveSessionState.Thinking -> "KASA is thinking..." to "Processing speech and preparing voice response"
    is LiveSessionState.Speaking -> "KASA is speaking" to "Speaking aloud • Speak anytime to interrupt"
    is LiveSessionState.Ended -> "Live session ended" to "Tap below to start another live conversation"
    is LiveSessionState.Error -> "Connection Issue" to (state.message)
  }

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.padding(horizontal = KasaSpacing.medium),
  ) {
    Text(
      text = headline,
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onBackground,
      textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = subtitle,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
      maxLines = 2,
    )
  }
}

@Composable
private fun LiveVoiceOrb(
  state: LiveSessionState,
  isMuted: Boolean,
  onOrbClicked: () -> Unit,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 1.0f,
    targetValue = 1.15f,
    animationSpec = infiniteRepeatable(
      animation = tween(1200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "pulse_scale",
  )

  // Real-time amplitude from live audio hardware (0f..1f)
  val rawAmplitude = when (state) {
    is LiveSessionState.Listening -> state.micAmplitude
    is LiveSessionState.Speaking -> state.speakerAmplitude
    else -> 0f
  }

  val dynamicScale by animateFloatAsState(
    targetValue = 1.0f + (rawAmplitude * 0.45f),
    animationSpec = tween(100),
    label = "amplitude_scale",
  )

  val isActive = state is LiveSessionState.Listening ||
      state is LiveSessionState.Speaking ||
      state is LiveSessionState.Thinking

  val primaryColor = MaterialTheme.colorScheme.primary
  val secondaryColor = MaterialTheme.colorScheme.tertiary
  val errorColor = MaterialTheme.colorScheme.error

  Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier.size(240.dp),
  ) {
    // Outer dynamic halo 2 (reacts to real sound amplitude)
    if (isActive) {
      Box(
        modifier = Modifier
          .size(220.dp)
          .scale(if (rawAmplitude > 0.05f) dynamicScale else pulseScale)
          .clip(CircleShape)
          .background(
            Brush.radialGradient(
              colors = listOf(
                primaryColor.copy(alpha = 0.25f),
                Color.Transparent,
              )
            )
          )
      )
    }

    // Outer dynamic halo 1
    if (isActive) {
      Box(
        modifier = Modifier
          .size(170.dp)
          .scale(dynamicScale)
          .clip(CircleShape)
          .background(
            Brush.radialGradient(
              colors = listOf(
                secondaryColor.copy(alpha = 0.35f),
                Color.Transparent,
              )
            )
          )
      )
    }

    // Center interactive Button
    Surface(
      onClick = onOrbClicked,
      shape = CircleShape,
      color = when {
        isMuted -> MaterialTheme.colorScheme.errorContainer
        state is LiveSessionState.Speaking -> MaterialTheme.colorScheme.primaryContainer
        state is LiveSessionState.Listening -> MaterialTheme.colorScheme.primary
        state is LiveSessionState.Connecting || state is LiveSessionState.Thinking -> MaterialTheme.colorScheme.surfaceVariant
        state is LiveSessionState.Error -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.primary
      },
      tonalElevation = 6.dp,
      shadowElevation = 8.dp,
      modifier = Modifier
        .size(120.dp)
        .testTag("live_voice_main_button"),
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize(),
      ) {
        when {
          state is LiveSessionState.Connecting || state is LiveSessionState.Thinking -> {
            CircularProgressIndicator(
              modifier = Modifier.size(44.dp),
              strokeWidth = 3.dp,
              color = MaterialTheme.colorScheme.primary,
            )
          }
          state is LiveSessionState.Speaking -> {
            Icon(
              imageVector = Icons.Filled.GraphicEq,
              contentDescription = "KASA Speaking",
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.size(48.dp),
            )
          }
          isMuted -> {
            Icon(
              imageVector = Icons.Filled.MicOff,
              contentDescription = "Microphone Muted",
              tint = MaterialTheme.colorScheme.onErrorContainer,
              modifier = Modifier.size(48.dp),
            )
          }
          state is LiveSessionState.Listening -> {
            Icon(
              imageVector = Icons.Filled.Mic,
              contentDescription = "Microphone Active",
              tint = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.size(48.dp),
            )
          }
          state is LiveSessionState.Error -> {
            Icon(
              imageVector = Icons.Outlined.ErrorOutline,
              contentDescription = "Error State",
              tint = errorColor,
              modifier = Modifier.size(48.dp),
            )
          }
          else -> {
            Icon(
              imageVector = Icons.Filled.Mic,
              contentDescription = "Start Live Voice",
              tint = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.size(48.dp),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun LiveTranscriptCard(
  transcript: List<com.example.core.ai.live.LiveTranscriptItem>,
  modifier: Modifier = Modifier,
) {
  val listState = rememberLazyListState()

  LaunchedEffect(transcript.size, transcript.lastOrNull()?.text) {
    if (transcript.isNotEmpty()) {
      listState.animateScrollToItem(transcript.size - 1)
    }
  }

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface,
    ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = modifier.testTag("live_transcript_card"),
  ) {
    LazyColumn(
      state = listState,
      modifier = Modifier
        .fillMaxSize()
        .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      items(transcript, key = { it.id }) { item ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = if (item.isUser) Arrangement.End else Arrangement.Start,
        ) {
          Text(
            text = (if (item.isUser) "You: " else "KASA: ") + item.text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (item.isUser) FontWeight.Medium else FontWeight.SemiBold,
            color = if (item.isUser) {
              MaterialTheme.colorScheme.onSurfaceVariant
            } else {
              MaterialTheme.colorScheme.primary
            },
            modifier = Modifier.padding(horizontal = 4.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun LiveGuidanceTips(
  sessionState: LiveSessionState,
  modifier: Modifier = Modifier,
) {
  val tipText = when (sessionState) {
    is LiveSessionState.Listening -> "💡 You can interrupt KASA at any moment while it speaks."
    is LiveSessionState.Speaking -> "💡 Speak naturally to interrupt and take the floor."
    else -> "💡 Powered by Google Gemini Real-Time Voice with African cultural grounding."
  }

  Text(
    text = tipText,
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    textAlign = TextAlign.Center,
    modifier = modifier,
  )
}

@Composable
private fun LiveBottomControls(
  sessionState: LiveSessionState,
  selectedVoice: LiveVoiceOption,
  isMuted: Boolean,
  onToggleMute: () -> Unit,
  onStartSession: () -> Unit,
  onStopSession: () -> Unit,
  onOpenVoiceSelector: () -> Unit,
  onNavigateToChat: () -> Unit,
) {
  val isLiveActive = sessionState is LiveSessionState.Listening ||
      sessionState is LiveSessionState.Speaking ||
      sessionState is LiveSessionState.Thinking ||
      sessionState is LiveSessionState.Connecting

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = KasaSpacing.small),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    // Voice Selector Chip
    Surface(
      onClick = onOpenVoiceSelector,
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surfaceVariant,
      modifier = Modifier.testTag("live_voice_chip"),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
      ) {
        Icon(
          imageVector = Icons.Outlined.RecordVoiceOver,
          contentDescription = null,
          modifier = Modifier.size(16.dp),
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = selectedVoice.displayName,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    // Center Action: Start/Stop Session or Mute
    if (isLiveActive) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        // Mute Microphone Toggle
        IconButton(
          onClick = onToggleMute,
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(
              if (isMuted) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
            )
            .testTag("live_mute_toggle_button"),
        ) {
          Icon(
            imageVector = if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
            contentDescription = if (isMuted) "Unmute Microphone" else "Mute Microphone",
            tint = if (isMuted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
          )
        }

        // End Live Session Button
        IconButton(
          onClick = onStopSession,
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.error)
            .testTag("live_end_session_button"),
        ) {
          Icon(
            imageVector = Icons.Filled.CallEnd,
            contentDescription = "End Live Session",
            tint = MaterialTheme.colorScheme.onError,
            modifier = Modifier.size(22.dp),
          )
        }
      }
    } else {
      KasaPrimaryButton(
        text = "Start Live",
        onClick = onStartSession,
        leadingIcon = {
          Icon(Icons.Filled.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
        },
        testTag = "live_start_session_button",
      )
    }

    // Go to Text Chat Button
    OutlinedButton(
      onClick = onNavigateToChat,
      shape = RoundedCornerShape(20.dp),
      modifier = Modifier.testTag("live_go_to_chat_button"),
    ) {
      Text("Chat", style = MaterialTheme.typography.labelMedium)
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LiveVoiceSelectionBottomSheet(
  selectedVoice: LiveVoiceOption,
  availableVoices: List<LiveVoiceOption>,
  onSelectVoice: (LiveVoiceOption) -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState()

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = KasaSpacing.medium)
        .padding(bottom = KasaSpacing.large),
    ) {
      Text(
        text = "Select KASA Voice",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "Choose a voice persona for real-time live conversations.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Spacer(modifier = Modifier.height(KasaSpacing.medium))

      availableVoices.forEach { voice ->
        val isSelected = voice.id == selectedVoice.id
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
              MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            } else {
              MaterialTheme.colorScheme.surface
            }
          ),
          border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
          ),
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onSelectVoice(voice) }
            .testTag("voice_option_${voice.id}"),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = voice.displayName,
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.width(8.dp))
                KasaBadge(
                  text = voice.gender,
                  containerColor = MaterialTheme.colorScheme.surfaceVariant,
                  contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = voice.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }

            if (isSelected) {
              Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun MicrophonePermissionDialog(
  onRequestPermission: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Filled.Mic,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(32.dp),
      )
    },
    title = {
      Text("Microphone Access Required", textAlign = TextAlign.Center)
    },
    text = {
      Text(
        "KASA Live needs access to your microphone to enable real-time bidirectional voice conversation. Your audio is streamed directly to the AI model during your active session.",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
      )
    },
    confirmButton = {
      TextButton(
        onClick = onRequestPermission,
        modifier = Modifier.testTag("confirm_permission_button"),
      ) {
        Text("Grant Permission", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Not Now")
      }
    },
  )
}

@Composable
private fun LiveErrorBanner(
  errorMessage: String,
  onRetry: () -> Unit,
  onDismiss: () -> Unit,
) {
  Surface(
    color = MaterialTheme.colorScheme.errorContainer,
    contentColor = MaterialTheme.colorScheme.onErrorContainer,
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp)
      .clip(RoundedCornerShape(12.dp)),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = Icons.Outlined.ErrorOutline,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.error,
        modifier = Modifier.size(18.dp),
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = errorMessage,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.weight(1f),
      )
      TextButton(onClick = onRetry) {
        Text("Retry", fontWeight = FontWeight.Bold)
      }
      TextButton(onClick = onDismiss) {
        Text("Dismiss")
      }
    }
  }
}
