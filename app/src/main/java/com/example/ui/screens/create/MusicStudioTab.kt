package com.example.ui.screens.create

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GeneratedSong
import com.example.data.model.UserMusicCredits
import com.example.ui.components.KasaBadge
import com.example.ui.components.KasaCard
import com.example.ui.theme.KasaSpacing

@Composable
fun MusicStudioTabContent(
  uiState: CreateUiState,
  musicStarterPrompts: List<MusicStarterPrompt>,
  genreOptions: List<String>,
  moodOptions: List<String>,
  languageOptions: List<String>,
  onPromptChange: (String) -> Unit,
  onTitleChange: (String) -> Unit,
  onGenreSelect: (String) -> Unit,
  onMoodSelect: (String) -> Unit,
  onLanguageSelect: (String) -> Unit,
  onInstrumentalToggle: (Boolean) -> Unit,
  onSelectStarter: (MusicStarterPrompt) -> Unit,
  onGenerate: () -> Unit,
  onSelectHistorySong: (GeneratedSong) -> Unit,
  onDeleteHistorySong: (String) -> Unit,
  onDismissError: () -> Unit,
  onOpenUpgrade: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(KasaSpacing.medium),
  ) {
    // 1. Music Header Card with Credit Pill & Upgrade Action
    MusicHeaderCard(
      credits = uiState.musicCredits,
      onOpenUpgrade = onOpenUpgrade,
    )

    // 2. Error Banner (if any)
    if (uiState.musicErrorMessage != null) {
      MusicErrorBanner(
        message = uiState.musicErrorMessage,
        onDismiss = onDismissError,
      )
    }

    // 3. Generating Progress Card OR Composer
    if (uiState.isMusicGenerating) {
      MusicGeneratingIndicatorCard(
        statusMessage = uiState.musicStatusMessage,
        prompt = uiState.musicPrompt,
      )
    } else {
      // Composer Form Card
      MusicComposerCard(
        prompt = uiState.musicPrompt,
        title = uiState.musicTitle,
        genre = uiState.musicGenre,
        mood = uiState.musicMood,
        language = uiState.musicLanguage,
        isInstrumental = uiState.isInstrumental,
        credits = uiState.musicCredits,
        isGenerating = uiState.isMusicGenerating,
        genreOptions = genreOptions,
        moodOptions = moodOptions,
        languageOptions = languageOptions,
        onPromptChange = onPromptChange,
        onTitleChange = onTitleChange,
        onGenreSelect = onGenreSelect,
        onMoodSelect = onMoodSelect,
        onLanguageSelect = onLanguageSelect,
        onInstrumentalToggle = onInstrumentalToggle,
        onGenerate = onGenerate,
        onOpenUpgrade = onOpenUpgrade,
      )

      // Quick Starter Ideas
      MusicStarterIdeasSection(
        starters = musicStarterPrompts,
        onSelect = onSelectStarter,
      )

      // Music History Section
      if (uiState.musicHistory.isNotEmpty()) {
        MusicHistorySection(
          history = uiState.musicHistory,
          onSelectSong = onSelectHistorySong,
          onDeleteSong = onDeleteHistorySong,
        )
      }
    }
  }
}

@Composable
private fun MusicHeaderCard(
  credits: UserMusicCredits?,
  onOpenUpgrade: () -> Unit,
) {
  val isOwner = credits?.isOwner == true

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("music_header_card"),
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
          .background(Color(0xFFD4AF37).copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Outlined.MusicNote,
          contentDescription = null,
          tint = Color(0xFFD4AF37),
          modifier = Modifier.size(26.dp),
        )
      }
      Spacer(modifier = Modifier.width(14.dp))
      Column(modifier = Modifier.weight(1f)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = "KASA Music",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
          )

          // Allowance Pill / Badge
          val allowanceText = when {
            isOwner -> "Owner Access"
            credits == null -> "AI Music"
            credits.isUnlimitedDev -> "Unlimited (Dev)"
            credits.remaining <= 0 -> "0 left this cycle"
            else -> "${credits.remaining}/${credits.limit} left"
          }
          val allowanceColor = when {
            isOwner -> Color(0xFFD4AF37)
            credits?.remaining ?: 1 > 0 -> Color(0xFF2E7D32)
            else -> MaterialTheme.colorScheme.error
          }

          KasaBadge(
            text = allowanceText,
            containerColor = allowanceColor.copy(alpha = 0.15f),
            contentColor = allowanceColor,
          )
        }
        Text(
          text = if (isOwner) "Developer & Owner Access • Unlimited" else "Turn your idea into a song.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      if (!isOwner) {
        OutlinedButton(
          onClick = onOpenUpgrade,
          shape = RoundedCornerShape(12.dp),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          modifier = Modifier.testTag("music_upgrade_button"),
        ) {
          Text(
            text = if (credits?.tier == "plus" || credits?.tier == "pro") "Manage" else "Upgrade",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }
  }
}

@Composable
private fun MusicComposerCard(
  prompt: String,
  title: String,
  genre: String,
  mood: String,
  language: String,
  isInstrumental: Boolean,
  credits: UserMusicCredits?,
  isGenerating: Boolean,
  genreOptions: List<String>,
  moodOptions: List<String>,
  languageOptions: List<String>,
  onPromptChange: (String) -> Unit,
  onTitleChange: (String) -> Unit,
  onGenreSelect: (String) -> Unit,
  onMoodSelect: (String) -> Unit,
  onLanguageSelect: (String) -> Unit,
  onInstrumentalToggle: (Boolean) -> Unit,
  onGenerate: () -> Unit,
  onOpenUpgrade: () -> Unit = {},
) {
  var showAdvancedControls by remember { mutableStateOf(false) }

  KasaCard(testTag = "music_composer_card") {
    Column(modifier = Modifier.fillMaxWidth()) {
      Text(
        text = "What should your song be about?",
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
            text = "Describe your song... (e.g., 'An energetic Afrobeats anthem celebrating love and triumph in Accra')",
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
          .height(105.dp)
          .testTag("music_prompt_input"),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = Color(0xFFD4AF37),
          unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        ),
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Genre Chips Row
      Text(
        text = "Style / Genre",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(6.dp))

      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp),
      ) {
        items(genreOptions) { g ->
          FilterChip(
            selected = genre == g,
            onClick = { onGenreSelect(g) },
            label = { Text(text = g, fontSize = 12.sp) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = Color(0xFFD4AF37).copy(alpha = 0.25f),
              selectedLabelColor = Color(0xFF8A6D00),
            ),
            modifier = Modifier.testTag("music_chip_genre_${g.replace(" ", "_")}"),
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Toggle for Optional Controls (Title, Instrumental, Mood, Language)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { showAdvancedControls = !showAdvancedControls }
          .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          imageVector = Icons.Outlined.Tune,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = if (showAdvancedControls) "Hide Optional Controls" else "Customize Style, Mood & Language",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.weight(1f))
        Icon(
          imageVector = if (showAdvancedControls) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(18.dp),
        )
      }

      AnimatedVisibility(
        visible = showAdvancedControls,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          // Song Title (Optional)
          OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            placeholder = { Text("Song Title (optional)", fontSize = 13.sp) },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("music_title_input"),
            shape = RoundedCornerShape(10.dp),
          )

          // Instrumental Switch
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column {
              Text(
                text = "Instrumental Track",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
              )
              Text(
                text = if (isInstrumental) "No lyrics, instruments only" else "Includes AI vocal performance",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Switch(
              checked = isInstrumental,
              onCheckedChange = onInstrumentalToggle,
              colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFFD4AF37),
                checkedTrackColor = Color(0xFFD4AF37).copy(alpha = 0.5f),
              ),
              modifier = Modifier.testTag("music_instrumental_switch"),
            )
          }

          // Mood Selection
          Column {
            Text(
              text = "Mood",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              items(moodOptions) { m ->
                FilterChip(
                  selected = mood == m,
                  onClick = { onMoodSelect(m) },
                  label = { Text(m, fontSize = 11.sp) },
                )
              }
            }
          }

          // Language Selection (English, Twi, Fante, Ga, Ewe)
          Column {
            Text(
              text = "Vocal Language",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              items(languageOptions) { lang ->
                FilterChip(
                  selected = language == lang,
                  onClick = { onLanguageSelect(lang) },
                  label = { Text(lang, fontSize = 11.sp) },
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Allowance Check & Generate Button
      val isOwner = credits?.isOwner == true
      val hasAvailableAllowance = isOwner || credits == null || credits.isUnlimitedDev || credits.remaining > 0

      if (!hasAvailableAllowance) {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenUpgrade)
            .testTag("music_allowance_exhausted_banner"),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(
            containerColor = Color(0xFFD4AF37).copy(alpha = 0.15f),
          ),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Cycle Allowance Exhausted",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8A6D00),
              )
              Text(
                text = "Upgrade to Plus (GH₵49) or Pro (GH₵99) for more generations.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Button(
              onClick = onOpenUpgrade,
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFD4AF37),
                contentColor = Color(0xFF1C1B1F),
              ),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            ) {
              Text("Upgrade", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
          }
        }
        Spacer(modifier = Modifier.height(10.dp))
      }

      Button(
        onClick = {
          if (hasAvailableAllowance) {
            onGenerate()
          } else {
            onOpenUpgrade()
          }
        },
        enabled = prompt.trim().isNotBlank() && !isGenerating,
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("music_generate_button"),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = if (hasAvailableAllowance) Color(0xFFD4AF37) else MaterialTheme.colorScheme.surfaceVariant,
          contentColor = if (hasAvailableAllowance) Color(0xFF1C1B1F) else MaterialTheme.colorScheme.onSurfaceVariant,
        ),
      ) {
        Icon(
          imageVector = Icons.Outlined.MusicNote,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (hasAvailableAllowance) "Generate Song" else "Upgrade to Continue Generating",
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}

@Composable
private fun MusicGeneratingIndicatorCard(statusMessage: String, prompt: String) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("music_generating_indicator"),
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
        color = Color(0xFFD4AF37),
        strokeWidth = 3.dp,
      )
      Spacer(modifier = Modifier.height(16.dp))
      Text(
        text = statusMessage.ifBlank { "Creating your song..." },
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
      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = "This usually takes 30–60 seconds.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
      )
    }
  }
}

@Composable
private fun MusicStarterIdeasSection(
  starters: List<MusicStarterPrompt>,
  onSelect: (MusicStarterPrompt) -> Unit,
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Text(
      text = "Song Inspiration & Themes",
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(modifier = Modifier.height(8.dp))

    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      contentPadding = PaddingValues(horizontal = 2.dp),
    ) {
      items(starters) { item ->
        Card(
          modifier = Modifier
            .width(220.dp)
            .clickable { onSelect(item) }
            .testTag("music_starter_${item.title.replace(" ", "_")}"),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
          ),
          border = CardDefaults.outlinedCardBorder(),
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            KasaBadge(
              text = item.genre,
              containerColor = Color(0xFFD4AF37).copy(alpha = 0.15f),
              contentColor = Color(0xFF8A6D00),
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
private fun MusicHistorySection(
  history: List<GeneratedSong>,
  onSelectSong: (GeneratedSong) -> Unit,
  onDeleteSong: (String) -> Unit,
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = Icons.Outlined.History,
        contentDescription = null,
        tint = Color(0xFFD4AF37),
        modifier = Modifier.size(18.dp),
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = "Your Generated Tracks",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Spacer(modifier = Modifier.weight(1f))
      Text(
        text = "${history.size} tracks",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Spacer(modifier = Modifier.height(10.dp))

    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      contentPadding = PaddingValues(horizontal = 2.dp),
    ) {
      items(history, key = { it.id }) { song ->
        Card(
          modifier = Modifier
            .width(180.dp)
            .clickable { onSelectSong(song) }
            .testTag("music_history_item_${song.id}"),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
          ),
          border = CardDefaults.outlinedCardBorder(),
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(Color(0xFFD4AF37).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Outlined.PlayArrow,
                  contentDescription = "Play track",
                  tint = Color(0xFF8A6D00),
                  modifier = Modifier.size(20.dp),
                )
              }

              IconButton(
                onClick = { onDeleteSong(song.id) },
                modifier = Modifier.size(28.dp),
              ) {
                Icon(
                  imageVector = Icons.Outlined.Delete,
                  contentDescription = "Delete",
                  tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                  modifier = Modifier.size(16.dp),
                )
              }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = song.title.ifBlank { "Untitled Song" },
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = song.genre ?: "Track",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = song.prompt,
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
private fun MusicErrorBanner(
  message: String,
  onDismiss: () -> Unit,
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("music_error_banner"),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
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
