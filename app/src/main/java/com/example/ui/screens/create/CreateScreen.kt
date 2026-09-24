package com.example.ui.screens.create

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.KasaBadge
import com.example.ui.theme.KasaSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateScreen(
  viewModel: CreateViewModel,
  onNavigateToSettings: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val uiState by viewModel.uiState.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(viewModel) {
    viewModel.setContextService(context)
    viewModel.selectMode(CreateStudioMode.MUSIC)
  }

  LaunchedEffect(uiState.saveStatusMessage) {
    uiState.saveStatusMessage?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      viewModel.clearSaveStatusMessage()
    }
  }

  LaunchedEffect(uiState.musicSaveStatusMessage) {
    uiState.musicSaveStatusMessage?.let { msg ->
      snackbarHostState.showSnackbar(msg)
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "KASA Music Studio",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.width(8.dp))
            KasaBadge(
              text = "Music Studio",
              containerColor = Color(0xFFD4AF37).copy(alpha = 0.2f),
              contentColor = Color(0xFF8A6D00),
            )
          }
        },
        actions = {
          IconButton(
            onClick = onNavigateToSettings,
            modifier = Modifier.testTag("create_settings_button"),
          ) {
            Icon(
              imageVector = Icons.Outlined.Settings,
              contentDescription = "Settings",
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background,
        ),
      )
    },
    snackbarHost = { SnackbarHost(snackbarHostState) },
    modifier = modifier.testTag("create_screen"),
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .widthIn(max = 680.dp)
          .padding(horizontal = KasaSpacing.medium),
        verticalArrangement = Arrangement.spacedBy(KasaSpacing.medium),
      ) {
        // ==============================================
        // MUSIC STUDIO
        // ==============================================
        if (uiState.activeSong != null && !uiState.isMusicGenerating) {
          item {
            MusicPlayerCard(
              song = uiState.activeSong!!,
              variations = uiState.activeSongVariations,
              isPlaying = uiState.isPlayingAudio,
              isBuffering = uiState.isBufferingAudio,
              currentPositionMs = uiState.playbackPositionMs,
              durationMs = uiState.playbackDurationMs,
              onTogglePlayPause = { viewModel.togglePlayPause() },
              onPlayVariation = { viewModel.playVariation(it) },
              onSelectVariation = { viewModel.selectVariation(it) },
              onSeek = { viewModel.seekAudio(it) },
              onShare = { viewModel.shareSong(context, uiState.activeSong!!) },
              onSave = {
                viewModel.shareSong(context, uiState.activeSong!!)
              },
              onGenerateAgain = { viewModel.clearActiveSong() },
              onClose = { viewModel.clearActiveSong() },
            )
          }
        } else {
          item {
            MusicStudioTabContent(
              uiState = uiState,
              musicStarterPrompts = viewModel.musicStarterPrompts,
              genreOptions = viewModel.genreOptions,
              moodOptions = viewModel.moodOptions,
              languageOptions = viewModel.languageOptions,
              onPromptChange = { viewModel.updateMusicPrompt(it) },
              onTitleChange = { viewModel.updateMusicTitle(it) },
              onGenreSelect = { viewModel.selectMusicGenre(it) },
              onMoodSelect = { viewModel.selectMusicMood(it) },
              onLanguageSelect = { viewModel.selectMusicLanguage(it) },
              onInstrumentalToggle = { viewModel.setInstrumental(it) },
              onSelectStarter = { viewModel.selectMusicStarter(it) },
              onGenerate = { viewModel.generateMusic(context) },
              onSelectHistorySong = { viewModel.playSong(it) },
              onDeleteHistorySong = { viewModel.deleteSong(it) },
              onDismissError = { viewModel.clearMusicErrorMessage() },
              onOpenUpgrade = { viewModel.showUpgradeDialog(true) },
            )
          }
        }

        item {
          Spacer(modifier = Modifier.height(KasaSpacing.large))
        }
      }

      if (uiState.showUpgradeDialog) {
        MusicUpgradeDialog(
          credits = uiState.musicCredits,
          isUpgrading = uiState.isUpgrading,
          checkoutReference = uiState.checkoutReference,
          billingMessage = uiState.billingMessage,
          onDismiss = { viewModel.showUpgradeDialog(false) },
          onSelectPlan = { planId -> viewModel.startCheckout(context, planId) },
          onVerifyPayment = { ref -> viewModel.verifyCheckout(ref) },
        )
      }
    }
  }
}
