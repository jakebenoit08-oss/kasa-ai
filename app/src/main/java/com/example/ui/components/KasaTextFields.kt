package com.example.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.ui.theme.KasaSpacing

@Composable
fun KasaInputField(
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  modifier: Modifier = Modifier,
  placeholder: String = "",
  isError: Boolean = false,
  errorMessage: String? = null,
  singleLine: Boolean = true,
  testTag: String = "kasa_input_field",
) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    label = { Text(label) },
    placeholder = if (placeholder.isNotEmpty()) {
      { Text(placeholder) }
    } else null,
    isError = isError,
    supportingText = if (errorMessage != null) {
      { Text(errorMessage, color = MaterialTheme.colorScheme.error) }
    } else null,
    singleLine = singleLine,
    shape = RoundedCornerShape(KasaSpacing.inputCornerRadius),
    modifier = modifier
      .fillMaxWidth()
      .testTag(testTag),
    colors = OutlinedTextFieldDefaults.colors(
      focusedBorderColor = MaterialTheme.colorScheme.primary,
      unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    ),
  )
}

@Composable
fun KasaMessageComposer(
  value: String,
  onValueChange: (String) -> Unit,
  onSend: () -> Unit,
  onAttachmentClick: () -> Unit,
  modifier: Modifier = Modifier,
  placeholder: String = "Type a message...",
  enabled: Boolean = true,
  testTag: String = "message_composer",
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = KasaSpacing.medium, vertical = KasaSpacing.small)
      .testTag(testTag),
    verticalAlignment = Alignment.Bottom,
  ) {
    IconButton(
      onClick = onAttachmentClick,
      modifier = Modifier
        .size(KasaSpacing.touchTargetMinimum)
        .testTag("attachment_button"),
      enabled = enabled,
    ) {
      Icon(
        imageVector = Icons.Outlined.AttachFile,
        contentDescription = "Attach file (Phase 0 Placeholder)",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    Spacer(modifier = Modifier.width(4.dp))

    OutlinedTextField(
      value = value,
      onValueChange = onValueChange,
      placeholder = {
        Text(
          placeholder,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
      },
      modifier = Modifier
        .weight(1f)
        .defaultMinSize(minHeight = KasaSpacing.touchTargetMinimum)
        .testTag("message_input_field"),
      shape = RoundedCornerShape(24.dp),
      maxLines = 4,
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
      keyboardActions = KeyboardActions(
        onSend = {
          if (value.isNotBlank() && enabled) {
            onSend()
          }
        }
      ),
      colors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
      ),
    )

    Spacer(modifier = Modifier.width(8.dp))

    val canSend = value.isNotBlank() && enabled
    IconButton(
      onClick = {
        if (canSend) onSend()
      },
      enabled = canSend,
      modifier = Modifier
        .size(KasaSpacing.touchTargetMinimum)
        .testTag("send_button"),
      colors = IconButtonDefaults.iconButtonColors(
        containerColor = if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (canSend) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
      ),
    ) {
      Icon(
        imageVector = Icons.AutoMirrored.Filled.Send,
        contentDescription = "Send Message",
      )
    }
  }
}
