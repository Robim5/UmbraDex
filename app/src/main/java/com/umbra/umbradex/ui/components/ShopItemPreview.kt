package com.umbra.umbradex.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.umbra.umbradex.data.model.ShopItem
import com.umbra.umbradex.utils.RarityUtils
import com.umbra.umbradex.utils.toColor
import kotlin.math.cos
import kotlin.math.sin

/**
 * Preview visual para skins quando não há imagem disponível.
 * Mostra um avatar estilizado com cores da raridade.
 */
@Composable
fun SkinPlaceholderPreview(
    item: ShopItem,
    modifier: Modifier = Modifier
) {
    val rarityColor = RarityUtils.getColor(item.rarity)
    
    // Cores baseadas na raridade para o gradiente
    val gradientColors = when (item.rarity.lowercase()) {
        "common" -> listOf(Color(0xFF78909C), Color(0xFF546E7A))
        "rare" -> listOf(Color(0xFF42A5F5), Color(0xFF1976D2))
        "epic" -> listOf(Color(0xFFAB47BC), Color(0xFF7B1FA2))
        "legendary" -> listOf(Color(0xFFFFD700), Color(0xFFFF8F00))
        else -> listOf(Color(0xFF616161), Color(0xFF424242))
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        gradientColors[0].copy(alpha = 0.3f),
                        gradientColors[1].copy(alpha = 0.6f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Avatar estilizado
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.verticalGradient(gradientColors)
                )
                .border(2.dp, rarityColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Skin",
                modifier = Modifier.size(36.dp),
                tint = Color.White
            )
        }
        
        // Efeito de brilho para legendary
        if (item.rarity.lowercase() == "legendary") {
            SparkleEffect(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFFFFD700)
            )
        }
    }
}

/**
 * Preview visual para badges quando não há imagem disponível.
 * Mostra uma medalha estilizada com estrelas baseadas na raridade.
 */
@Composable
fun BadgePlaceholderPreview(
    item: ShopItem,
    modifier: Modifier = Modifier
) {
    val rarityColor = RarityUtils.getColor(item.rarity)
    
    // Número de pontas da estrela baseado na raridade
    val starPoints = when (item.rarity.lowercase()) {
        "common" -> 4
        "rare" -> 5
        "epic" -> 6
        "legendary" -> 8
        else -> 5
    }
    
    val gradientColors = when (item.rarity.lowercase()) {
        "common" -> listOf(Color(0xFFB0BEC5), Color(0xFF78909C))
        "rare" -> listOf(Color(0xFF64B5F6), Color(0xFF1976D2))
        "epic" -> listOf(Color(0xFFCE93D8), Color(0xFF7B1FA2))
        "legendary" -> listOf(Color(0xFFFFF176), Color(0xFFFFD700), Color(0xFFFF8F00))
        else -> listOf(Color(0xFF9E9E9E), Color(0xFF616161))
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A)),
        contentAlignment = Alignment.Center
    ) {
        // Círculo de fundo
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            gradientColors.first().copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    )
                )
        )
        
        // Desenho da estrela/badge
        Canvas(
            modifier = Modifier.size(48.dp)
        ) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val outerRadius = size.minDimension / 2 * 0.9f
            val innerRadius = outerRadius * 0.5f
            
            val path = Path()
            val angleStep = Math.PI / starPoints
            
            for (i in 0 until starPoints * 2) {
                val radius = if (i % 2 == 0) outerRadius else innerRadius
                val angle = i * angleStep - Math.PI / 2
                val x = centerX + (radius * cos(angle)).toFloat()
                val y = centerY + (radius * sin(angle)).toFloat()
                
                if (i == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }
            path.close()
            
            // Preencher com gradiente
            drawPath(
                path = path,
                brush = Brush.verticalGradient(gradientColors),
                style = Fill
            )
            
            // Contorno
            drawPath(
                path = path,
                color = rarityColor,
                style = Stroke(width = 2.dp.toPx())
            )
        }
        
        // Efeito de brilho para legendary
        if (item.rarity.lowercase() == "legendary") {
            SparkleEffect(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFFFFD700)
            )
        }
    }
}

/**
 * Preview para themes - mostra as cores em círculos sobrepostos
 */
@Composable
fun ThemeColorPreview(
    colors: List<String>?,
    modifier: Modifier = Modifier
) {
    val palette = colors?.take(4) ?: listOf("#6366F1", "#8B5CF6")
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A)),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            palette.take(3).forEachIndexed { index, hex ->
                Box(
                    modifier = Modifier
                        .offset(x = (-12 * index).dp)
                        .size(if (index == 1) 40.dp else 32.dp)
                        .clip(CircleShape)
                        .background(hex.toColor())
                        .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                )
            }
        }
    }
}

/**
 * Preview para name colors - mostra "Aa" com o gradiente aplicado
 */
@Composable
fun NameColorPreviewEnhanced(
    colors: List<String>?,
    modifier: Modifier = Modifier
) {
    val colorList = colors?.mapNotNull { 
        try { it.toColor() } catch (e: Exception) { null }
    } ?: listOf(Color.White)
    
    val brush = if (colorList.size >= 2) {
        Brush.linearGradient(colorList)
    } else if (colorList.isNotEmpty()) {
        Brush.linearGradient(listOf(colorList[0], colorList[0]))
    } else {
        Brush.linearGradient(listOf(Color.White, Color.White))
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Aa",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            style = androidx.compose.ui.text.TextStyle(
                brush = brush
            )
        )
    }
}

/**
 * Efeito de sparkle/brilho para itens legendary
 */
@Composable
fun SparkleEffect(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFFFD700)
) {
    Canvas(modifier = modifier) {
        val sparklePositions = listOf(
            Offset(size.width * 0.2f, size.height * 0.2f),
            Offset(size.width * 0.8f, size.height * 0.3f),
            Offset(size.width * 0.15f, size.height * 0.7f),
            Offset(size.width * 0.85f, size.height * 0.75f)
        )
        
        sparklePositions.forEach { pos ->
            // Desenha pequena estrela de 4 pontas
            val size = 6.dp.toPx()
            val path = Path().apply {
                moveTo(pos.x, pos.y - size)
                lineTo(pos.x + size * 0.3f, pos.y - size * 0.3f)
                lineTo(pos.x + size, pos.y)
                lineTo(pos.x + size * 0.3f, pos.y + size * 0.3f)
                lineTo(pos.x, pos.y + size)
                lineTo(pos.x - size * 0.3f, pos.y + size * 0.3f)
                lineTo(pos.x - size, pos.y)
                lineTo(pos.x - size * 0.3f, pos.y - size * 0.3f)
                close()
            }
            drawPath(path, color.copy(alpha = 0.6f))
        }
    }
}
