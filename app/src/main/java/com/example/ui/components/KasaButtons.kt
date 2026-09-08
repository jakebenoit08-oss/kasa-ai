package com.example.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.KasaSpacing

@Composable
fun KasaPrimaryButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  loading: Boolean = false,
  leadingIcon: (@Composable () -> Unit)? = null,
  testTag: String = "primary_button",
) {
  Button(
    onClick = onClick,
    enabled = enabled && !loading,
    modifier = modifier
      .defaultMinSize(minHeight = KasaSpacing.touchTargetMinimum)
      .testTag(testTag),
    shape = RoundedCornerShape(KasaSpacing.buttonCornerRadius),
    colors = ButtonDefaults.buttonColors(
      containerColor = MaterialTheme.colorScheme.primary,
      contentColor = MaterialTheme.colorScheme.onPrimary,
    ),
    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
  ) {
    if (loading) {
      CircularProgressIndicator(
        modifier = Modifier.size(18.dp),
        color = MaterialTheme.colorScheme.onPrimary,
        strokeWidth = 2.dp,
      )
    } else {
      Row(verticalAlignment = Alignment.CenterVertically) {
        if (leadingIcon != null) {
          leadingIcon()
          Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
          text = text,
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
        )
      }
    }
  }
}

@Composable
fun KasaOutlinedButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  leadingIcon: (@Composable () -> Unit)? = null,
  testTag: String = "outlined_button",
) {
  OutlinedButton(
    onClick = onClick,
    enabled = enabled,
    modifier = modifier
      .defaultMinSize(minHeight = KasaSpacing.touchTargetMinimum)
      .testTag(testTag),
    shape = RoundedCornerShape(KasaSpacing.buttonCornerRadius),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      if (leadingIcon != null) {
        leadingIcon()
        Spacer(modifier = Modifier.width(8.dp))
      }
      Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Medium,
      )
    }
  }
}

@Composable
fun KasaTextButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  testTag: String = "text_button",
) {
  TextButton(
    onClick = onClick,
    enabled = enabled,
    modifier = modifier
      .defaultMinSize(minHeight = KasaSpacing.touchTargetMinimum)
      .testTag(testTag),
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelLarge,
      fontWeight = FontWeight.Medium,
    )
  }
}
