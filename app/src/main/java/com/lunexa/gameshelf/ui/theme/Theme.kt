package com.lunexa.gameshelf.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// Палитра GameShelf
val GsBackground = Color(0xFF0D0F17)
val GsCard = Color(0xFF191D2A)
val GsAccent = Color(0xFF8B5CF6)
val GsAccentLight = Color(0xFFA78BFA)
val GsText = Color(0xFFFFFFFF)
val GsTextSecondary = Color(0xFF9CA3AF)

enum class ThemeMode(val title: String) { SYSTEM("Системная"), DARK("Тёмная"), LIGHT("Светлая") }

/** Варианты акцентного цвета (первый — фирменный). */
val AccentOptions: List<Long> = listOf(
    0xFF8B5CF6, 0xFF6366F1, 0xFF3B82F6, 0xFF06B6D4,
    0xFF10B981, 0xFFF59E0B, 0xFFEF4444, 0xFFEC4899
)

/** true, если сейчас активна тёмная тема (для мелких декоративных решений). */
val LocalIsDark = staticCompositionLocalOf { true }

private val GsTypography = Typography().let { t ->
    t.copy(
        headlineLarge = t.headlineLarge.copy(fontWeight = FontWeight.Bold),
        headlineMedium = t.headlineMedium.copy(fontWeight = FontWeight.Bold),
        headlineSmall = t.headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        titleSmall = t.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = t.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp)
    )
}

@Composable
fun GameShelfTheme(
    mode: ThemeMode,
    accent: Color,
    content: @Composable () -> Unit
) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val accentLight = lerp(accent, Color.White, 0.35f)

    val scheme = if (dark) {
        darkColorScheme(
            primary = accent,
            onPrimary = Color.White,
            primaryContainer = accent.copy(alpha = 0.22f).compositeOver(GsBackground),
            onPrimaryContainer = accentLight,
            secondary = accentLight,
            onSecondary = GsBackground,
            secondaryContainer = accent.copy(alpha = 0.16f).compositeOver(GsCard),
            onSecondaryContainer = Color.White,
            tertiary = Color(0xFFFBBF24),
            background = GsBackground,
            onBackground = GsText,
            surface = GsBackground,
            onSurface = GsText,
            surfaceVariant = GsCard,
            onSurfaceVariant = GsTextSecondary,
            surfaceContainerLowest = Color(0xFF0A0C12),
            surfaceContainerLow = Color(0xFF141824),
            surfaceContainer = GsCard,
            surfaceContainerHigh = Color(0xFF212637),
            surfaceContainerHighest = Color(0xFF2A3044),
            outline = Color(0xFF353B52),
            outlineVariant = Color(0xFF262B3D),
            error = Color(0xFFF87171),
            onError = Color.White,
            inverseSurface = Color(0xFFE5E7EB),
            inverseOnSurface = GsBackground
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = Color.White,
            primaryContainer = accent.copy(alpha = 0.14f).compositeOver(Color.White),
            onPrimaryContainer = lerp(accent, Color.Black, 0.35f),
            secondary = lerp(accent, Color.Black, 0.15f),
            onSecondary = Color.White,
            secondaryContainer = accent.copy(alpha = 0.10f).compositeOver(Color.White),
            onSecondaryContainer = Color(0xFF111827),
            tertiary = Color(0xFFD97706),
            background = Color(0xFFF4F4F8),
            onBackground = Color(0xFF111827),
            surface = Color(0xFFF4F4F8),
            onSurface = Color(0xFF111827),
            surfaceVariant = Color.White,
            onSurfaceVariant = Color(0xFF6B7280),
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = Color(0xFFFAFAFC),
            surfaceContainer = Color.White,
            surfaceContainerHigh = Color(0xFFEDEDF3),
            surfaceContainerHighest = Color(0xFFE4E4EC),
            outline = Color(0xFFD1D5DB),
            outlineVariant = Color(0xFFE5E7EB),
            error = Color(0xFFDC2626),
            onError = Color.White
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalIsDark provides dark) {
        MaterialTheme(colorScheme = scheme, typography = GsTypography, content = content)
    }
}
