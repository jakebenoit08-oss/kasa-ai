package com.example.ui.screens.create

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GeneratedSong
import com.example.ui.components.KasaBadge
import com.example.ui.theme.KasaSpacing

@Composable
fun MusicPlayerCard(
  song: GeneratedSong,
  variations: List<GeneratedSong> = emptyList(),
  isPlaying: Boolean,
  isBuffering: Boolean,
  currentPositionMs: Long,
  durationMs: Long,
  onTogglePlayPause: () -> Unit,
  onPlayVariation: (GeneratedSong) -> Unit = {},
  onSelectVariation: (GeneratedSong) -> Unit = {},
  onSeek: (Float) -> Unit,
  onShare: () -> Unit,
  onSave: () -> Unit,
  onGenerateAgain: () -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val resolvedDuration = if (durationMs > 0) durationMs else (song.duration * 1000).toLong()
  val sliderProgress = if (resolvedDuration > 0) {
    (currentPositionMs.toFloat() / resolvedDuration.toFloat()).coerceIn(0f, 1f)
  } else 0f

  val infiniteTransition = rememberInfiniteTransition(label = "vinyl_rotate")
  val rotationAngle by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 10000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "disc_angle"
  )

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("music_player_card"),
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface,
    ),
    border = CardDefaults.outlinedCardBorder(),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(KasaSpacing.medium),
    ) {
      // Top Bar in Card: Category & Close
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          KasaBadge(
            text = "KASA Audio Studio",
            containerColor = Color(0xFFD4AF37).copy(alpha = 0.2f),
            contentColor = Color(0xFF8A6D00),
          )
          if (variations.size > 1) {
            Spacer(modifier = Modifier.width(6.dp))
            KasaBadge(
              text = "2 Variations",
              containerColor = Color(0xFF1B2E1D),
              contentColor = Color(0xFF81C784),
            )
          } else if (song.genre != null) {
            Spacer(modifier = Modifier.width(6.dp))
            KasaBadge(
              text = song.genre,
              containerColor = MaterialTheme.colorScheme.primaryContainer,
              contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
          }
        }

        IconButton(
          onClick = onClose,
          modifier = Modifier
            .size(32.dp)
            .testTag("music_close_button"),
        ) {
          Icon(
            imageVector = Icons.Outlined.Close,
            contentDescription = "Close player",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Multiple Song Variations Section (Option 1 & Option 2)
      if (variations.size > 1) {
        Text(
          text = "Your songs are ready",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "AIMusicAPI generated 2 unique variations. Listen to both and choose your favorite.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(14.dp))

        variations.forEachIndexed { index, variation ->
          val isSelected = variation.id == song.id
          val isThisVariationPlaying = isSelected && isPlaying
          val optionLabel = "Option ${index + 1}"

          Card(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onSelectVariation(variation) }
              .testTag("music_variation_card_${index + 1}"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
              containerColor = if (isSelected) {
                Color(0xFFD4AF37).copy(alpha = 0.12f)
              } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
              },
            ),
            border = BorderStroke(
              width = if (isSelected) 2.dp else 1.dp,
              color = if (isSelected) Color(0xFFD4AF37) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            ),
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  KasaBadge(
                    text = optionLabel,
                    containerColor = if (isSelected) Color(0xFFD4AF37) else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isSelected) Color(0xFF1C1B1F) else MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  if (isSelected) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = Color(0xFFB8860B),
                        modifier = Modifier.size(14.dp),
                      )
                      Spacer(modifier = Modifier.width(4.dp))
                      Text(
                        text = "Selected",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB8860B),
                      )
                    }
                  }
                }

                if (!isSelected) {
                  TextButton(
                    onClick = { onSelectVariation(variation) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp).testTag("music_select_variation_${index + 1}"),
                  ) {
                    Text(
                      text = "Use this",
                      fontSize = 12.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = Color(0xFFD4AF37),
                    )
                  }
                }
              }

              Spacer(modifier = Modifier.height(8.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                // Artwork / Vinyl Icon Thumbnail
                Box(
                  modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                      Brush.linearGradient(
                        colors = if (index == 0) {
                          listOf(Color(0xFF1B2E1D), Color(0xFFD4AF37))
                        } else {
                          listOf(Color(0xFF2C2416), Color(0xFF9E782F))
                        }
                      )
                    ),
                  contentAlignment = Alignment.Center,
                ) {
                  Icon(
                    imageVector = Icons.Outlined.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                  )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title and Duration
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = variation.title.ifBlank { "Variation ${index + 1}" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Spacer(modifier = Modifier.height(2.dp))
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    Text(
                      text = formatSeconds(variation.duration),
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (variation.genre != null) {
                      Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                      Text(
                        text = variation.genre,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                  }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Independent Play/Pause Button for this Option
                IconButton(
                  onClick = { onPlayVariation(variation) },
                  modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                      if (isThisVariationPlaying) Color(0xFFD4AF37) else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .testTag("music_play_variation_${index + 1}"),
                ) {
                  Icon(
                    imageVector = if (isThisVariationPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    contentDescription = if (isThisVariationPlaying) "Pause $optionLabel" else "Play $optionLabel",
                    tint = if (isThisVariationPlaying) Color(0xFF1C1B1F) else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp),
                  )
                }
              }
            }
          }

          if (index < variations.size - 1) {
            Spacer(modifier = Modifier.height(10.dp))
          }
        }

        Spacer(modifier = Modifier.height(16.dp))
      }

      // Vinyl / Waveform Visualizer Banner for currently active/playing variation
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(150.dp)
          .clip(RoundedCornerShape(18.dp))
          .background(
            Brush.linearGradient(
              colors = listOf(
                Color(0xFF1B2E1D), // Dark Kasa Forest Green
                Color(0xFF2C2416), // Dark Warm Gold
                Color(0xFF1E1E24)
              )
            )
          ),
        contentAlignment = Alignment.Center,
      ) {
        // Rotating Vinyl Graphic
        Box(
          modifier = Modifier
            .size(110.dp)
            .rotate(if (isPlaying) rotationAngle else 0f)
            .clip(CircleShape)
            .background(
              Brush.radialGradient(
                colors = listOf(
                  Color(0xFF121212),
                  Color(0xFF2D2D2D),
                  Color(0xFF121212),
                  Color(0xFF3E3E3E),
                  Color(0xFF181818),
                )
              )
            )
            .border(2.dp, Color(0xFFD4AF37).copy(alpha = 0.6f), CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          // Center Label
          Box(
            modifier = Modifier
              .size(40.dp)
              .clip(CircleShape)
              .background(
                Brush.linearGradient(
                  listOf(Color(0xFFD4AF37), Color(0xFFE5A93C))
                )
              ),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Outlined.MusicNote,
              contentDescription = null,
              tint = Color(0xFF1C1B1F),
              modifier = Modifier.size(20.dp),
            )
          }
        }

        // Live Equalizer Visualizer Bars
        Row(
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 12.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          verticalAlignment = Alignment.Bottom,
        ) {
          val barHeights = listOf(14, 22, 10, 26, 18, 30, 16, 24, 12, 28, 20, 15)
          barHeights.forEachIndexed { index, heightDp ->
            val animatedHeight = if (isPlaying) {
              (heightDp * (0.5f + (index % 3) * 0.25f)).dp
            } else 6.dp

            Box(
              modifier = Modifier
                .width(4.dp)
                .height(animatedHeight)
                .clip(RoundedCornerShape(2.dp))
                .background(
                  if (isPlaying) Color(0xFFD4AF37) else Color.White.copy(alpha = 0.3f)
                )
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Song Title & Prompt Description
      Text(
        text = song.title.ifBlank { "Untitled Song" },
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = song.prompt,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )

      // Audio Progress Slider
      Spacer(modifier = Modifier.height(12.dp))
      Slider(
        value = sliderProgress,
        onValueChange = { onSeek(it) },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("music_progress_slider"),
        colors = SliderDefaults.colors(
          thumbColor = Color(0xFFD4AF37),
          activeTrackColor = Color(0xFFD4AF37),
          inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
      )

      // Time Display Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = formatMillis(currentPositionMs),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = formatMillis(resolvedDuration),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Main Play/Pause Control Button Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier = Modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(
              Brush.linearGradient(
                listOf(Color(0xFFD4AF37), Color(0xFFB8860B))
              )
            )
            .border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          IconButton(
            onClick = onTogglePlayPause,
            modifier = Modifier
              .size(60.dp)
              .testTag("music_play_pause_button"),
          ) {
            if (isBuffering) {
              CircularProgressIndicator(
                modifier = Modifier.size(26.dp),
                color = Color.White,
                strokeWidth = 3.dp,
              )
            } else {
              Icon(
                imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(34.dp),
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      // Bottom Action Buttons: Save, Share, Generate Again
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Button(
          onClick = onSave,
          modifier = Modifier
            .weight(1f)
            .height(44.dp)
            .testTag("music_save_button"),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
          ),
        ) {
          Icon(
            imageVector = Icons.Outlined.Download,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text("Save", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }

        OutlinedButton(
          onClick = onShare,
          modifier = Modifier
            .weight(1f)
            .height(44.dp)
            .testTag("music_share_button"),
          shape = RoundedCornerShape(12.dp),
        ) {
          Icon(
            imageVector = Icons.Outlined.Share,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text("Share", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }

        Button(
          onClick = onGenerateAgain,
          modifier = Modifier
            .weight(1.3f)
            .height(44.dp)
            .testTag("music_generate_again_button"),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
          ),
        ) {
          Icon(
            imageVector = Icons.Outlined.Refresh,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text("Create Another", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
      }
    }
  }
}

private fun formatSeconds(seconds: Float): String {
  val totalSeconds = seconds.toLong().coerceAtLeast(0)
  val minutes = totalSeconds / 60
  val remSeconds = totalSeconds % 60
  return "%02d:%02d".format(minutes, remSeconds)
}

private fun formatMillis(millis: Long): String {
  val totalSeconds = (millis / 1000).coerceAtLeast(0)
  val minutes = totalSeconds / 60
  val seconds = totalSeconds % 60
  return "%02d:%02d".format(minutes, seconds)
}
