package com.umbra.umbradex.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.umbra.umbradex.data.model.ShopItem
import com.umbra.umbradex.ui.theme.UmbraGold
import com.umbra.umbradex.ui.theme.UmbraPrimary
import com.umbra.umbradex.ui.theme.UmbraSurface
import com.umbra.umbradex.utils.RarityUtils
import com.umbra.umbradex.utils.getAvatarResourceId
import com.umbra.umbradex.utils.toBrush
import com.umbra.umbradex.utils.toColor

@Composable
fun ShopItemCard(
    item: ShopItem,
    userGold: Long,
    userLevel: Int,
    onBuyClick: () -> Unit
) {
    val rarityColor = RarityUtils.getColor(item.rarity)

    // Regras do PDF: Bloqueio por Nível
    val isLevelLocked = userLevel < item.minLevel
    val canAfford = userGold >= item.price

    // Design do Cartão
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(6.dp)
            .border(1.dp, rarityColor.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- 1. VISUAL PREVIEW (O Recheio) ---
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(2.dp, rarityColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                when (item.type) {
                    "theme" -> ThemePreview(item.colors)
                    "name_color" -> NameColorPreview(item.colors)
                    "skin" -> SkinPreview(item)
                    "badge" -> BadgePreview(item)
                    else -> AssetPreview(item.assetUrl ?: "", item)
                }

                // CADEADO (Overlay) se bloqueado
                if (isLevelLocked) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Lock, contentDescription = "Bloqueado", tint = Color.White)
                            Text("Lvl ${item.minLevel}", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- 2. DETALHES ---
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                maxLines = 1,
                fontWeight = FontWeight.Bold
            )

            // Raridade (Badge Pequeno)
            Surface(
                color = rarityColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Text(
                    text = item.rarity.uppercase(),
                    color = rarityColor,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- 3. BOTÃO DE COMPRA ---
            Button(
                onClick = onBuyClick,
                enabled = !isLevelLocked && canAfford,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (canAfford) UmbraGold else Color.Red.copy(alpha = 0.6f),
                    disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                if (isLevelLocked) {
                    Text("BLOQUEADO", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                } else if (!canAfford) {
                    Text("${item.price} G", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f))
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Black)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${item.price} G", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }
        }
    }
}

// --- SUB-COMPONENTES DE PREVIEW ---

@Composable
fun SkinPreview(item: ShopItem) {
    val rarityColor = RarityUtils.getColor(item.rarity)
    val resId = if (item.assetUrl != null) getAvatarResourceId(item.assetUrl) else 0
    
    if (resId != 0) {
        Image(
            painter = painterResource(id = resId),
            contentDescription = item.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().padding(4.dp)
        )
    } else {
        // Placeholder estilizado para skins
        SkinPlaceholderPreview(item = item, modifier = Modifier.fillMaxSize())
    }
}

@Composable
fun BadgePreview(item: ShopItem) {
    val rarityColor = RarityUtils.getColor(item.rarity)
    val resId = if (item.assetUrl != null) getAvatarResourceId(item.assetUrl) else 0
    
    if (resId != 0) {
        Image(
            painter = painterResource(id = resId),
            contentDescription = item.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().padding(8.dp)
        )
    } else {
        // Placeholder estilizado para badges
        BadgePlaceholderPreview(item = item, modifier = Modifier.fillMaxSize())
    }
}

@Composable
fun AssetPreview(assetName: String, item: ShopItem) {
    val resId = getAvatarResourceId(assetName)
    val rarityColor = RarityUtils.getColor(item.rarity)
    
    if (resId != 0) {
        Image(
            painter = painterResource(id = resId),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().padding(4.dp)
        )
    } else {
        // Fallback visual baseado no tipo
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            rarityColor.copy(alpha = 0.3f),
                            Color(0xFF1A1A1A)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null, 
                tint = rarityColor,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

@Composable
fun ThemePreview(colors: List<String>?) {
    val palette = colors ?: listOf("#6366F1", "#8B5CF6")
    
    Box(
        modifier = Modifier
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
                        .offset(x = (-8 * index).dp)
                        .size(if (index == 1) 36.dp else 28.dp)
                        .clip(CircleShape)
                        .background(hex.toColor())
                        .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                )
            }
        }
    }
}

@Composable
fun NameColorPreview(colors: List<String>?) {
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
        modifier = Modifier
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