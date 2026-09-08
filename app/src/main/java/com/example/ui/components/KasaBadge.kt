package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.KasaSpacing

@Composable
fun KasaBadge(
  text: String,
  modifier: Modifier = Modifier,
  containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
  contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(KasaSpacing.chipCornerRadius))
      .background(containerColor)
      .padding(horizontal = 8.dp, vertical = 4.dp),
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall,
      color = contentColor,
      fontWeight = FontWeight.SemiBold,
    )
  }
}
