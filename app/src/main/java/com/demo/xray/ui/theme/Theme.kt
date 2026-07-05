package com.demo.xray.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * A deliberately chosen blue/slate palette rather than the Material baseline purple, so the app
 * has its own visual identity when [dynamicColor] is off (the default - see [XRayTheme]).
 */
private val LightColors =
    lightColorScheme(
        primary = Color(0xFF2A5C9A),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFD3E4FF),
        onPrimaryContainer = Color(0xFF001C38),
        secondary = Color(0xFF4C6377),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFD3E5F4),
        onSecondaryContainer = Color(0xFF0D1F2C),
        tertiary = Color(0xFF6B5A2E),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFF3E1B8),
        onTertiaryContainer = Color(0xFF241A00),
        error = Color(0xFFB3261E),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFF9DEDC),
        onErrorContainer = Color(0xFF410E0B),
        background = Color(0xFFF7F9FC),
        onBackground = Color(0xFF1A1C1E),
        surface = Color(0xFFF7F9FC),
        onSurface = Color(0xFF1A1C1E),
        surfaceVariant = Color(0xFFDDE3EA),
        onSurfaceVariant = Color(0xFF41474D),
        outline = Color(0xFF71787E),
        outlineVariant = Color(0xFFC1C7CE),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF1F4F8),
        surfaceContainer = Color(0xFFEBEEF3),
        surfaceContainerHigh = Color(0xFFE5E9EE),
        surfaceContainerHighest = Color(0xFFDFE3E9),
        inverseSurface = Color(0xFF2E3133),
        inverseOnSurface = Color(0xFFEFF1F4),
        inversePrimary = Color(0xFFA0C9FF),
        scrim = Color(0xFF000000),
    )

/** Deep blue-charcoal rather than near-black, so dark mode reads as intentional, not flat/empty. */
private val DarkColors =
    darkColorScheme(
        primary = Color(0xFFA0C9FF),
        onPrimary = Color(0xFF00325A),
        primaryContainer = Color(0xFF17497D),
        onPrimaryContainer = Color(0xFFD3E4FF),
        secondary = Color(0xFFB4CCE5),
        onSecondary = Color(0xFF1D3346),
        secondaryContainer = Color(0xFF34495D),
        onSecondaryContainer = Color(0xFFD3E5F4),
        tertiary = Color(0xFFDBC38F),
        onTertiary = Color(0xFF3B2E05),
        tertiaryContainer = Color(0xFF54431A),
        onTertiaryContainer = Color(0xFFF3E1B8),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF10141B),
        onBackground = Color(0xFFE2E5EA),
        surface = Color(0xFF10141B),
        onSurface = Color(0xFFE2E5EA),
        surfaceVariant = Color(0xFF41474D),
        onSurfaceVariant = Color(0xFFC1C7CE),
        outline = Color(0xFF8B9198),
        outlineVariant = Color(0xFF41474D),
        surfaceContainerLowest = Color(0xFF0A0D13),
        surfaceContainerLow = Color(0xFF181C24),
        surfaceContainer = Color(0xFF1C2129),
        surfaceContainerHigh = Color(0xFF272C34),
        surfaceContainerHighest = Color(0xFF32373F),
        inverseSurface = Color(0xFFE2E5EA),
        inverseOnSurface = Color(0xFF2E3133),
        inversePrimary = Color(0xFF2A5C9A),
        scrim = Color(0xFF000000),
    )

@Composable
fun XRayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> DarkColors
            else -> LightColors
        }

    MaterialTheme(colorScheme = colorScheme, content = content)
}
