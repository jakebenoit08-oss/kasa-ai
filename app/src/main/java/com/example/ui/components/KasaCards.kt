package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.KasaSpacing

@Composable
fun KasaCard(
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
  testTag: String = "kasa_card",
  content: @Composable ColumnScope.() -> Unit,
) {
  val baseModifier = modifier
    .testTag(testTag)
    .then(
      if (onClick != null) {
        Modifier.clickable(onClick = onClick)
      } else {
        Modifier
      }
    )

  Card(
    modifier = baseModifier,
    shape = RoundedCornerShape(KasaSpacing.cardCornerRadius),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
      contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
  ) {
    Column(
      modifier = Modifier.padding(KasaSpacing.medium),
      content = content,
    )
  }
}

@Composable
fun KasaOutlinedCard(
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
  testTag: String = "kasa_outlined_card",
  content: @Composable ColumnScope.() -> Unit,
) {
  val baseModifier = modifier
    .testTag(testTag)
    .then(
      if (onClick != null) {
        Modifier.clickable(onClick = onClick)
      } else {
        Modifier
      }
    )

  OutlinedCard(
    modifier = baseModifier,
    shape = RoundedCornerShape(KasaSpacing.cardCornerRadius),
    colors = CardDefaults.outlinedCardColors(
      containerColor = MaterialTheme.colorScheme.surface,
      contentColor = MaterialTheme.colorScheme.onSurface,
    ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(
      modifier = Modifier.padding(KasaSpacing.medium),
      content = content,
    )
  }
}
