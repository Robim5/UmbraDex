package com.umbra.umbradex.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush

object RarityUtils {
    // Cores oficiais das raridades
    val Common = Color(0xFFB0BEC5)     // Cinza Aço
    val Rare = Color(0xFF42A5F5)       // Azul Brilhante
    val Epic = Color(0xFFAB47BC)       // Roxo Místico
    val Legendary = Color(0xFFFFD700)  // Dourado

    fun getColor(rarity: String): Color {
        return when (rarity.lowercase()) {
            "common" -> Common
            "rare" -> Rare
            "epic" -> Epic
            "legendary" -> Legendary
            else -> Color.White
        }
    }
}

// Extensão para converter "#FFFFFF" em Color(0xFFFFFFFF)
fun String.toColor(): Color {
    return try {
        Color(android.graphics.Color.parseColor(this))
    } catch (e: Exception) {
        Color.White
    }
}

// Extensão para criar Brush (Gradiente) a partir de lista de hex strings
fun List<String>.toBrush(): Brush {
    val colors = if (this.isEmpty()) listOf(Color.White, Color.White)
    else this.map { it.toColor() }

    return if (colors.size == 1) {
        Brush.linearGradient(listOf(colors[0], colors[0]))
    } else {
        Brush.linearGradient(colors)
    }
}

fun getTypeColor(type: String): Color {
    return when (type.lowercase()) {
        "normal" -> Color(0xFFA8A878)
        "fire" -> Color(0xFFF08030)
        "water" -> Color(0xFF6890F0)
        "electric" -> Color(0xFFF8D030)
        "grass" -> Color(0xFF78C850)
        "ice" -> Color(0xFF98D8D8)
        "fighting" -> Color(0xFFC03028)
        "poison" -> Color(0xFFA040A0)
        "ground" -> Color(0xFFE0C068)
        "flying" -> Color(0xFFA890F0)
        "psychic" -> Color(0xFFF85888)
        "bug" -> Color(0xFFA8B820)
        "rock" -> Color(0xFFB8A038)
        "ghost" -> Color(0xFF705898)
        "dragon" -> Color(0xFF7038F8)
        "dark" -> Color(0xFF705848)
        "steel" -> Color(0xFFB8B8D0)
        "fairy" -> Color(0xFFEE99AC)
        else -> Color.Gray
    }
}

/**
 * Retorna um par de cores para criar um gradiente bonito baseado no tipo.
 * Usa cores mais claras e escuras do mesmo tipo para um efeito mais elegante.
 */
fun getTypeGradientColors(type: String): Pair<Color, Color> {
    return when (type.lowercase()) {
        "normal" -> Pair(Color(0xFFCDCDBE), Color(0xFF8A8A6E))
        "fire" -> Pair(Color(0xFFFFB366), Color(0xFFCC5500))
        "water" -> Pair(Color(0xFF99BBFF), Color(0xFF3366CC))
        "electric" -> Pair(Color(0xFFFFE066), Color(0xFFCCAA00))
        "grass" -> Pair(Color(0xFFA3D977), Color(0xFF4E8C2A))
        "ice" -> Pair(Color(0xFFBBEEEE), Color(0xFF66B2B2))
        "fighting" -> Pair(Color(0xFFE06060), Color(0xFF8C1818))
        "poison" -> Pair(Color(0xFFCC77CC), Color(0xFF702070))
        "ground" -> Pair(Color(0xFFF0D898), Color(0xFFB08828))
        "flying" -> Pair(Color(0xFFCCBBFF), Color(0xFF7766CC))
        "psychic" -> Pair(Color(0xFFFF99AA), Color(0xFFCC3366))
        "bug" -> Pair(Color(0xFFCCDD55), Color(0xFF808C10))
        "rock" -> Pair(Color(0xFFDDCC77), Color(0xFF8C7C18))
        "ghost" -> Pair(Color(0xFF9988BB), Color(0xFF4C3C6C))
        "dragon" -> Pair(Color(0xFF9977FF), Color(0xFF4C18CC))
        "dark" -> Pair(Color(0xFF998877), Color(0xFF4C3C30))
        "steel" -> Pair(Color(0xFFD8D8E8), Color(0xFF8888A8))
        "fairy" -> Pair(Color(0xFFFFBBCC), Color(0xFFCC7788))
        else -> Pair(Color(0xFFCCCCCC), Color(0xFF888888))
    }
}

/**
 * Cria um Brush (gradiente) a partir de uma lista de tipos de Pokémon.
 * Se tiver 2 tipos, cria um gradiente diagonal bonito entre eles.
 * Se tiver 1 tipo, cria um gradiente com variações da mesma cor.
 */
fun createTypeGradientBrush(types: List<String>): Brush {
    return when {
        types.isEmpty() -> Brush.linearGradient(listOf(Color(0xFF888888), Color(0xFF444444)))
        types.size == 1 -> {
            val (light, dark) = getTypeGradientColors(types[0])
            Brush.linearGradient(
                colors = listOf(light, dark),
                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            )
        }
        else -> {
            // Gradiente diagonal com ambos os tipos
            val color1 = getTypeColor(types[0])
            val color2 = getTypeColor(types[1])
            Brush.linearGradient(
                colors = listOf(color1.copy(alpha = 0.9f), color2.copy(alpha = 0.9f)),
                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            )
        }
    }
}