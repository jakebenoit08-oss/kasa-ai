package com.example.ui.screens.splash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.KasaBrandHeader

@Composable
fun SplashScreen(
  viewModel: SplashViewModel,
  onNavigateTo: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val destination by viewModel.destination.collectAsStateWithLifecycle()

  LaunchedEffect(destination) {
    if (destination is SplashDestination.Navigate) {
      onNavigateTo((destination as SplashDestination.Navigate).route)
    }
  }

  Surface(
    modifier = modifier
      .fillMaxSize()
      .testTag("splash_screen"),
    color = MaterialTheme.colorScheme.background
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(24.dp),
      contentAlignment = Alignment.Center
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        KasaBrandHeader(
          logoSize = 96.dp,
          showTagline = true,
          isAnimated = true
        )
      }
    }
  }
}
