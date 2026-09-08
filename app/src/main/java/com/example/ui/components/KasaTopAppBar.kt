package com.example.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KasaTopAppBar(
  title: String,
  modifier: Modifier = Modifier,
  showBackButton: Boolean = false,
  onBackClick: () -> Unit = {},
  showSettingsButton: Boolean = true,
  onSettingsClick: () -> Unit = {},
  badgeText: String? = null,
  testTag: String = "kasa_top_app_bar",
) {
  TopAppBar(
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
        )
        if (badgeText != null) {
          Spacer(modifier = Modifier.width(8.dp))
          KasaBadge(text = badgeText)
        }
      }
    },
    navigationIcon = {
      if (showBackButton) {
        IconButton(
          onClick = onBackClick,
          modifier = Modifier.testTag("nav_back_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = "Back",
          )
        }
      }
    },
    actions = {
      if (showSettingsButton) {
        IconButton(
          onClick = onSettingsClick,
          modifier = Modifier.testTag("nav_settings_button"),
        ) {
          Icon(
            imageVector = Icons.Outlined.Settings,
            contentDescription = "Settings",
          )
        }
      }
    },
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = MaterialTheme.colorScheme.background,
      titleContentColor = MaterialTheme.colorScheme.onBackground,
      actionIconContentColor = MaterialTheme.colorScheme.onBackground,
    ),
    modifier = modifier.testTag(testTag),
  )
}
