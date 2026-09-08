package com.example.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight

@Composable
fun KasaBottomBar(
  currentRoute: String?,
  onNavigate: (Screen) -> Unit,
  modifier: Modifier = Modifier,
) {
  NavigationBar(
    modifier = modifier.testTag("kasa_bottom_navigation"),
    containerColor = MaterialTheme.colorScheme.surface,
    contentColor = MaterialTheme.colorScheme.onSurface,
    windowInsets = WindowInsets.navigationBars,
  ) {
    Screen.bottomNavItems.forEach { screen ->
      val isSelected = currentRoute == screen.route
      NavigationBarItem(
        selected = isSelected,
        onClick = { onNavigate(screen) },
        icon = {
          Icon(
            imageVector = screen.icon,
            contentDescription = screen.title,
          )
        },
        label = {
          Text(
            text = screen.title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
          )
        },
        colors = NavigationBarItemDefaults.colors(
          selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
          selectedTextColor = MaterialTheme.colorScheme.primary,
          indicatorColor = MaterialTheme.colorScheme.primaryContainer,
          unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
          unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        ),
        modifier = Modifier.testTag("nav_item_${screen.route}"),
      )
    }
  }
}
