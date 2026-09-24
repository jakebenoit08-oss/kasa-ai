package com.example.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
  val route: String,
  val title: String,
  val icon: ImageVector,
) {
  data object Splash : Screen("splash", "Splash", Icons.Outlined.Home)
  data object Onboarding : Screen("onboarding", "Onboarding", Icons.Outlined.Explore)
  data object Auth : Screen("auth", "Sign In", Icons.Outlined.AccountCircle)
  data object Home : Screen("home", "Home", Icons.Outlined.Home)
  data object Chat : Screen("chat", "Chat", Icons.AutoMirrored.Outlined.Chat)
  data object Live : Screen("live", "Live", Icons.Outlined.Mic)
  data object Create : Screen("create", "Music", Icons.Outlined.MusicNote)
  data object Study : Screen("study", "Study", Icons.Outlined.School)
  data object Settings : Screen("settings", "Settings", Icons.Outlined.Settings)

  companion object {
    val bottomNavItems = listOf(Home, Chat, Live, Create, Study)
  }
}
