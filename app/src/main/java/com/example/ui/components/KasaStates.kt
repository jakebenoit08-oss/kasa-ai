package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.KasaSpacing

@Composable
fun KasaEmptyState(
  title: String,
  description: String,
  modifier: Modifier = Modifier,
  icon: ImageVector = Icons.Outlined.Inbox,
  actionButton: (@Composable () -> Unit)? = null,
  testTag: String = "empty_state",
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(KasaSpacing.large)
      .testTag(testTag),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      modifier = Modifier.size(KasaSpacing.iconHero),
      tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
    )
    Spacer(modifier = Modifier.height(KasaSpacing.medium))
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.SemiBold,
      color = MaterialTheme.colorScheme.onSurface,
      textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(KasaSpacing.small))
    Text(
      text = description,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
    if (actionButton != null) {
      Spacer(modifier = Modifier.height(KasaSpacing.large))
      actionButton()
    }
  }
}

@Composable
fun KasaErrorState(
  message: String,
  modifier: Modifier = Modifier,
  onRetry: (() -> Unit)? = null,
  testTag: String = "error_state",
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(KasaSpacing.large)
      .testTag(testTag),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Icon(
      imageVector = Icons.Outlined.ErrorOutline,
      contentDescription = null,
      modifier = Modifier.size(40.dp),
      tint = MaterialTheme.colorScheme.error,
    )
    Spacer(modifier = Modifier.height(KasaSpacing.medium))
    Text(
      text = message,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurface,
      textAlign = TextAlign.Center,
    )
    if (onRetry != null) {
      Spacer(modifier = Modifier.height(KasaSpacing.medium))
      KasaOutlinedButton(
        text = "Try Again",
        onClick = onRetry,
        testTag = "retry_button",
      )
    }
  }
}

@Composable
fun KasaLoadingState(
  modifier: Modifier = Modifier,
  message: String = "Loading...",
  testTag: String = "loading_state",
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(KasaSpacing.large)
      .testTag(testTag),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    CircularProgressIndicator(
      color = MaterialTheme.colorScheme.primary,
      strokeWidth = 3.dp,
    )
    Spacer(modifier = Modifier.height(KasaSpacing.medium))
    Text(
      text = message,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}
