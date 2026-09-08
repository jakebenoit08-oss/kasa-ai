package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

val KasaDarkColorScheme = darkColorScheme(
  primary = KasaPrimaryDark,
  onPrimary = KasaOnPrimaryDark,
  primaryContainer = KasaPrimaryContainerDark,
  onPrimaryContainer = KasaOnPrimaryContainerDark,
  secondary = KasaSecondaryDark,
  onSecondary = KasaOnSecondaryDark,
  secondaryContainer = KasaSecondaryContainerDark,
  onSecondaryContainer = KasaOnSecondaryContainerDark,
  tertiary = KasaTertiaryDark,
  onTertiary = KasaOnTertiaryDark,
  tertiaryContainer = KasaTertiaryContainerDark,
  onTertiaryContainer = KasaOnTertiaryContainerDark,
  background = KasaDarkBackground,
  onBackground = KasaDarkOnBackground,
  surface = KasaDarkSurface,
  onSurface = KasaDarkOnSurface,
  surfaceVariant = KasaDarkSurfaceVariant,
  onSurfaceVariant = KasaDarkOnSurfaceVariant,
  outline = KasaDarkOutline,
  outlineVariant = KasaDarkOutlineVariant,
  error = KasaError,
)

val KasaLightColorScheme = lightColorScheme(
  primary = KasaPrimaryLight,
  onPrimary = KasaOnPrimaryLight,
  primaryContainer = KasaPrimaryContainerLight,
  onPrimaryContainer = KasaOnPrimaryContainerLight,
  secondary = KasaSecondaryLight,
  onSecondary = KasaOnSecondaryLight,
  secondaryContainer = KasaSecondaryContainerLight,
  onSecondaryContainer = KasaOnSecondaryContainerLight,
  tertiary = KasaTertiaryLight,
  onTertiary = KasaOnTertiaryLight,
  tertiaryContainer = KasaTertiaryContainerLight,
  onTertiaryContainer = KasaOnTertiaryContainerLight,
  background = KasaLightBackground,
  onBackground = KasaLightOnBackground,
  surface = KasaLightSurface,
  onSurface = KasaLightOnSurface,
  surfaceVariant = KasaLightSurfaceVariant,
  onSurfaceVariant = KasaLightOnSurfaceVariant,
  outline = KasaLightOutline,
  outlineVariant = KasaLightOutlineVariant,
  error = KasaError,
)

enum class ThemeMode {
  SYSTEM,
  LIGHT,
  DARK,
}

@Composable
fun KasaTheme(
  themeMode: ThemeMode = ThemeMode.SYSTEM,
  dynamicColor: Boolean = false, // Prefer KASA brand identity by default
  content: @Composable () -> Unit,
) {
  val isDark = when (themeMode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
  }

  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    isDark -> KasaDarkColorScheme
    else -> KasaLightColorScheme
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content,
  )
}
