package com.umbra.umbradex.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Função auxiliar para criar cores a partir de HEX seguro
fun fromHex(hex: String, default: Color): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        default
    }
}

// Função para criar cor de surface baseada na primary (mais escura)
private fun createSurfaceColor(primary: Color): Color {
    // Escurecer a cor primária para criar um surface sutil
    return primary.copy(alpha = 0.15f).compositeOver(UmbraSurface)
}

// Função para criar cor de surfaceVariant baseada na primary
private fun createSurfaceVariantColor(primary: Color): Color {
    return primary.copy(alpha = 0.08f).compositeOver(UmbraSurfaceHighlight)
}

// Extensão para compor cores
private fun Color.compositeOver(background: Color): Color {
    val fg = this
    val bg = background
    val alpha = fg.alpha + bg.alpha * (1f - fg.alpha)
    val red = (fg.red * fg.alpha + bg.red * bg.alpha * (1f - fg.alpha)) / alpha
    val green = (fg.green * fg.alpha + bg.green * bg.alpha * (1f - fg.alpha)) / alpha
    val blue = (fg.blue * fg.alpha + bg.blue * bg.alpha * (1f - fg.alpha)) / alpha
    return Color(red = red, green = green, blue = blue, alpha = alpha)
}

@Composable
fun UmbraDexTheme(
    // Recebe a paleta de cores do perfil (se existir)
    themeColors: List<String>? = null,
    content: @Composable () -> Unit
) {
    // 1. Definir as cores base (Roxo Default ou Tema Personalizado)
    val targetPrimaryColor = if (!themeColors.isNullOrEmpty()) fromHex(themeColors[0], UmbraPrimary) else UmbraPrimary
    val targetSecondaryColor = if (themeColors != null && themeColors.size > 1) fromHex(themeColors[1], UmbraAccent) else UmbraAccent
    
    // Criar cores de surface dinâmicas baseadas na primary
    val targetSurfaceColor = createSurfaceColor(targetPrimaryColor)
    val targetSurfaceVariantColor = createSurfaceVariantColor(targetPrimaryColor)
    
    // 2. Animar transições de cor para uma mudança suave
    val primaryColor by animateColorAsState(
        targetValue = targetPrimaryColor,
        animationSpec = tween(durationMillis = 500),
        label = "primaryColor"
    )
    
    val secondaryColor by animateColorAsState(
        targetValue = targetSecondaryColor,
        animationSpec = tween(durationMillis = 500),
        label = "secondaryColor"
    )
    
    val surfaceColor by animateColorAsState(
        targetValue = targetSurfaceColor,
        animationSpec = tween(durationMillis = 500),
        label = "surfaceColor"
    )
    
    val surfaceVariantColor by animateColorAsState(
        targetValue = targetSurfaceVariantColor,
        animationSpec = tween(durationMillis = 500),
        label = "surfaceVariantColor"
    )

    // 3. Criar o esquema de cores dinâmico
    val colorScheme = darkColorScheme(
        primary = primaryColor,
        secondary = secondaryColor,
        tertiary = UmbraGold,
        background = UmbraBackground, // Background mantém escuro
        surface = surfaceColor, // Surface agora é dinâmico!
        surfaceVariant = surfaceVariantColor, // SurfaceVariant dinâmico
        onPrimary = Color.White,
        onSecondary = Color.White,
        onBackground = Color.White,
        onSurface = Color.White,
        onSurfaceVariant = Color.White.copy(alpha = 0.7f),
        outline = primaryColor.copy(alpha = 0.5f), // Outline baseado na primary
        outlineVariant = primaryColor.copy(alpha = 0.3f)
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = UmbraBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}