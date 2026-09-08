package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.KasaSpacing

@Composable
fun KasaInfoDialog(
  title: String,
  message: String,
  onDismiss: () -> Unit,
  confirmButtonText: String = "OK",
  testTag: String = "info_dialog",
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    },
    confirmButton = {
      KasaTextButton(
        text = confirmButtonText,
        onClick = onDismiss,
        testTag = "dialog_confirm_button",
      )
    },
    modifier = Modifier.testTag(testTag),
  )
}

@Composable
fun KasaEditNameDialog(
  currentName: String,
  onDismiss: () -> Unit,
  onSave: (String) -> Unit,
  testTag: String = "edit_name_dialog",
) {
  var name by remember { mutableStateOf(currentName) }
  var error by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = "Edit Profile Name",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = "Update your local display name for isolated session management.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(KasaSpacing.medium))
        KasaInputField(
          value = name,
          onValueChange = {
            name = it
            error = null
          },
          label = "Display Name",
          isError = error != null,
          errorMessage = error,
          testTag = "dialog_name_input",
        )
      }
    },
    confirmButton = {
      KasaPrimaryButton(
        text = "Save",
        onClick = {
          if (name.isBlank()) {
            error = "Name cannot be empty"
          } else {
            onSave(name.trim())
          }
        },
        testTag = "dialog_save_button",
      )
    },
    dismissButton = {
      KasaTextButton(
        text = "Cancel",
        onClick = onDismiss,
        testTag = "dialog_cancel_button",
      )
    },
    modifier = Modifier.testTag(testTag),
  )
}
