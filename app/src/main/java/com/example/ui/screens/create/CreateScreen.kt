package com.example.ui.screens.create

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.GeneratedImage
import com.example.ui.components.KasaBadge
import com.example.ui.components.KasaCard
import com.example.ui.theme.KasaSpacing
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
              text = "KASA Create Studio",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.width(8.dp))
            KasaBadge(
              text = if (uiState.selectedMode == CreateStudioMode.IMAGE) "Image Studio" else "Music Studio",
              containerColor = if (uiState.selectedMode == CreateStudioMode.IMAGE) {
                MaterialTheme.colorScheme.primaryContainer
              } else {
                Color(0xFFD4AF37).copy(alpha = 0.2f)
              },
              contentColor = if (uiState.selectedMode == CreateStudioMode.IMAGE) {
                MaterialTheme.colorScheme.onPrimaryContainer
              } else {
                Color(0xFF8A6D00)
              },
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
        item {
          Spacer(modifier = Modifier.height(KasaSpacing.small))
          // Studio Mode Selector: [ 🖼️ Image ] [ 🎵 Music ]
          StudioModeSelector(
            selectedMode = uiState.selectedMode,
            onSelectMode = { viewModel.selectMode(it) },
          )
        }

        // ==============================================
        // MODE: IMAGE STUDIO
        // ==============================================
        if (uiState.selectedMode == CreateStudioMode.IMAGE) {
          if (uiState.activeImage != null && !uiState.isGenerating) {
            item {
              ImagePreviewView(
                image = uiState.activeImage!!,
                isGenerating = uiState.isGenerating,
                onBack = { viewModel.clearActiveImage() },
                onRegenerate = { viewModel.regenerateImage() },
                onSave = { viewModel.saveImageToDevice(context, uiState.activeImage!!) },
                onShare = { viewModel.shareImage(context, uiState.activeImage!!) },
                onDelete = { viewModel.deleteImage(uiState.activeImage!!.id) },
              )
            }
          } else {
            item {
              StudioHeaderCard()
            }

            if (uiState.errorMessage != null) {
              item {
                ErrorBanner(
                  message = uiState.errorMessage!!,
                  onDismiss = { viewModel.clearErrorMessage() },
                )
              }
            }

            if (uiState.isGenerating) {
              item {
                GeneratingIndicatorCard(prompt = uiState.prompt)
              }
            } else {
              item {
                PromptComposerCard(
                  prompt = uiState.prompt,
                  selectedAspectRatio = uiState.selectedAspectRatio,
                  isGenerating = uiState.isGenerating,
                  onPromptChange = { viewModel.updatePrompt(it) },
                  onAspectRatioChange = { viewModel.selectAspectRatio(it) },
                  onGenerate = { viewModel.generateImage() },
                )
              }

              item {
                StarterIdeasSection(
                  starterPrompts = viewModel.starterPrompts,
                  onSelectPrompt = { viewModel.selectStarterPrompt(it) },
                )
              }

              if (uiState.history.isNotEmpty()) {
                item {
                  HistorySection(
                    history = uiState.history,
                    onSelectImage = { viewModel.selectHistoryImage(it) },
                  )
                }
              }
            }
          }
        } else {
          // ==============================================
          // MODE: MUSIC STUDIO
          // ==============================================
          if (uiState.activeSong != null && !uiState.isMusicGenerating) {
            item {
              MusicPlayerCard(
                song = uiState.activeSong!!,
                isPlaying = uiState.isPlayingAudio,
                isBuffering = uiState.isBufferingAudio,
                currentPositionMs = uiState.playbackPositionMs,
                durationMs = uiState.playbackDurationMs,
                onTogglePlayPause = { viewModel.togglePlayPause() },
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
              )
            }
          }
        }

        item {
          Spacer(modifier = Modifier.height(KasaSpacing.large))
        }
      }
    }
  }
}

@Composable
private fun StudioModeSelector(
  selectedMode: CreateStudioMode,
  onSelectMode: (CreateStudioMode) -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
      .padding(4.dp)
      .testTag("create_mode_selector"),
    horizontalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    CreateStudioMode.values().forEach { mode ->
      val isSelected = selectedMode == mode
      val tag = if (mode == CreateStudioMode.IMAGE) "create_tab_image" else "create_tab_music"
      Box(
        modifier = Modifier
          .weight(1f)
          .height(44.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(
            if (isSelected) {
              if (mode == CreateStudioMode.MUSIC) Color(0xFFD4AF37)
              else MaterialTheme.colorScheme.primary
            } else Color.Transparent
          )
          .clickable { onSelectMode(mode) }
          .testTag(tag),
        contentAlignment = Alignment.Center,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center,
        ) {
          Text(
            text = mode.iconEmoji,
            fontSize = 16.sp,
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = mode.label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) {
              if (mode == CreateStudioMode.MUSIC) Color(0xFF1C1B1F)
              else MaterialTheme.colorScheme.onPrimary
            } else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@Composable
private fun StudioHeaderCard() {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("create_header_card"),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    ),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(KasaSpacing.medium),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier = Modifier
          .size(48.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Outlined.AutoAwesome,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(26.dp),
        )
      }
      Spacer(modifier = Modifier.width(16.dp))
      Column {
        Text(
          text = "KASA Create Studio",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = "Turn your imagination into high-quality AI images.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun PromptComposerCard(
  prompt: String,
  selectedAspectRatio: AspectRatioOption,
  isGenerating: Boolean,
  onPromptChange: (String) -> Unit,
  onAspectRatioChange: (AspectRatioOption) -> Unit,
  onGenerate: () -> Unit,
) {
  KasaCard(testTag = "create_composer_card") {
    Column(modifier = Modifier.fillMaxWidth()) {
      Text(
        text = "Image Description",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Spacer(modifier = Modifier.height(8.dp))

      OutlinedTextField(
        value = prompt,
        onValueChange = onPromptChange,
        placeholder = {
          Text(
            text = "Describe the image you want to create... (e.g., 'Cinematic sunset over Accra with Jamestown lighthouse')",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
          )
        },
        trailingIcon = {
          if (prompt.isNotBlank()) {
            IconButton(onClick = { onPromptChange("") }) {
              Icon(
                imageVector = Icons.Outlined.Clear,
                contentDescription = "Clear prompt",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        },
        modifier = Modifier
          .fillMaxWidth()
          .height(110.dp)
          .testTag("create_prompt_input"),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = MaterialTheme.colorScheme.primary,
          unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        ),
      )

      Spacer(modifier = Modifier.height(12.dp))

      Text(
        text = "Aspect Ratio",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(6.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        AspectRatioOption.values().forEach { option ->
          FilterChip(
            selected = selectedAspectRatio == option,
            onClick = { onAspectRatioChange(option) },
            label = {
              Text(
                text = option.value,
                style = MaterialTheme.typography.labelSmall,
              )
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
            modifier = Modifier.testTag("create_chip_${option.value}"),
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      Button(
        onClick = onGenerate,
        enabled = prompt.trim().isNotBlank() && !isGenerating,
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("create_generate_button"),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
      ) {
        Icon(
          imageVector = Icons.Outlined.AutoAwesome,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Generate Image",
          fontWeight = FontWeight.SemiBold,
        )
      }
    }
  }
}

@Composable
private fun GeneratingIndicatorCard(prompt: String) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("create_generating_indicator"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    ),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(KasaSpacing.large),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      CircularProgressIndicator(
        modifier = Modifier.size(48.dp),
        color = MaterialTheme.colorScheme.primary,
        strokeWidth = 3.dp,
      )
      Spacer(modifier = Modifier.height(16.dp))
      Text(
        text = "Creating your image...",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = prompt,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun StarterIdeasSection(
  starterPrompts: List<StarterPrompt>,
  onSelectPrompt: (StarterPrompt) -> Unit,
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Text(
      text = "Creative Starter Ideas",
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(modifier = Modifier.height(8.dp))

    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      contentPadding = PaddingValues(horizontal = 2.dp),
    ) {
      items(starterPrompts) { item ->
        Card(
          modifier = Modifier
            .width(220.dp)
            .clickable { onSelectPrompt(item) }
            .testTag("create_starter_${item.title.replace(" ", "_")}"),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
          ),
          border = CardDefaults.outlinedCardBorder(),
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            KasaBadge(
              text = item.tag,
              containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
              contentColor = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = item.title,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = item.prompt,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun HistorySection(
  history: List<GeneratedImage>,
  onSelectImage: (GeneratedImage) -> Unit,
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = Icons.Outlined.History,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(18.dp),
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = "Your Studio Creations",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Spacer(modifier = Modifier.weight(1f))
      Text(
        text = "${history.size} items",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Spacer(modifier = Modifier.height(10.dp))

    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      contentPadding = PaddingValues(horizontal = 2.dp),
    ) {
      items(history, key = { it.id }) { item ->
        val fileExists = remember(item.imagePath) { File(item.imagePath).exists() }
        Card(
          modifier = Modifier
            .width(140.dp)
            .clickable { onSelectImage(item) }
            .testTag("create_history_item_${item.id}"),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
          ),
          border = CardDefaults.outlinedCardBorder(),
        ) {
          Column {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant),
              contentAlignment = Alignment.Center,
            ) {
              if (fileExists) {
                AsyncImage(
                  model = ImageRequest.Builder(LocalContext.current)
                    .data(File(item.imagePath))
                    .crossfade(true)
                    .build(),
                  contentDescription = item.prompt,
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize(),
                )
              } else {
                Icon(
                  imageVector = Icons.Outlined.Image,
                  contentDescription = "Missing file",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                  modifier = Modifier.size(32.dp),
                )
              }
            }
            Text(
              text = item.prompt,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.padding(8.dp),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ImagePreviewView(
  image: GeneratedImage,
  isGenerating: Boolean = false,
  onBack: () -> Unit,
  onRegenerate: () -> Unit,
  onSave: () -> Unit,
  onShare: () -> Unit,
  onDelete: () -> Unit,
) {
  val context = LocalContext.current
  val file = remember(image.imagePath) { File(image.imagePath) }
  val fileExists = file.exists()
  val dateFormat = remember { SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()) }

  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(KasaSpacing.medium),
  ) {
    // Back Navigation Row
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(
        onClick = onBack,
        modifier = Modifier.testTag("create_preview_back_button"),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
          contentDescription = "Back to Studio",
        )
      }
      Text(
        text = "Creation Preview",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
    }

    // Main Image View
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("create_preview_image_card"),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
      ),
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(1.0f)
          .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
      ) {
        if (fileExists) {
          AsyncImage(
            model = ImageRequest.Builder(context)
              .data(file)
              .crossfade(true)
              .build(),
            contentDescription = image.prompt,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
          )
        } else {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              imageVector = Icons.Outlined.ErrorOutline,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(40.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "Image file is no longer on device storage.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }

    // Prompt & Metadata Info Card
    KasaCard(testTag = "create_preview_meta_card") {
      Column(modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          KasaBadge(
            text = "Ratio ${image.aspectRatio}",
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
          )
          Text(
            text = dateFormat.format(Date(image.createdAt)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = "Prompt",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = image.prompt,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
    }

    // Actions Row
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Button(
        onClick = onSave,
        enabled = fileExists,
        modifier = Modifier
          .weight(1f)
          .height(48.dp)
          .testTag("create_save_button"),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
      ) {
        Icon(imageVector = Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Save", fontWeight = FontWeight.SemiBold)
      }

      OutlinedButton(
        onClick = onShare,
        enabled = fileExists,
        modifier = Modifier
          .weight(1f)
          .height(48.dp)
          .testTag("create_share_button"),
        shape = RoundedCornerShape(12.dp),
      ) {
        Icon(imageVector = Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Share", fontWeight = FontWeight.SemiBold)
      }

      IconButton(
        onClick = onDelete,
        modifier = Modifier
          .size(48.dp)
          .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
          .testTag("create_delete_button"),
      ) {
        Icon(
          imageVector = Icons.Outlined.Delete,
          contentDescription = "Delete image",
          tint = MaterialTheme.colorScheme.error,
        )
      }
    }

    OutlinedButton(
      onClick = onRegenerate,
      enabled = !isGenerating,
      modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)
        .testTag("create_regenerate_button"),
      shape = RoundedCornerShape(12.dp),
    ) {
      Icon(imageVector = Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Text("Regenerate Variation", fontWeight = FontWeight.SemiBold)
    }
  }
}

@Composable
private fun ErrorBanner(
  message: String,
  onDismiss: () -> Unit,
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("create_error_banner"),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
    ),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = Icons.Outlined.ErrorOutline,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.error,
        modifier = Modifier.size(22.dp),
      )
      Spacer(modifier = Modifier.width(10.dp))
      Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier.weight(1f),
      )
      IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
        Icon(
          imageVector = Icons.Outlined.Clear,
          contentDescription = "Dismiss",
          tint = MaterialTheme.colorScheme.onErrorContainer,
          modifier = Modifier.size(16.dp),
        )
      }
    }
  }
}
