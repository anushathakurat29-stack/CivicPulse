package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = PolishPrimaryDark,
    secondary = PolishSecondaryDark,
    background = PolishBackgroundDark,
    surface = PolishSurfaceDark,
    onPrimary = Color(0xFF001D36),
    onSecondary = Color(0xFF00210E),
    onBackground = PolishOnBackgroundDark,
    onSurface = PolishOnSurfaceDark
  )

private val LightColorScheme =
  lightColorScheme(
    primary = PolishPrimaryLight,
    secondary = PolishSecondaryLight,
    background = PolishBackgroundLight,
    surface = PolishSurfaceLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = PolishOnBackgroundLight,
    onSurface = PolishOnSurfaceLight
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Set dynamicColor to false by default to ensure our highly polished curated brand colors are featured
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
