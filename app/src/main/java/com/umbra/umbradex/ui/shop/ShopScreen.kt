package com.umbra.umbradex.ui.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.umbra.umbradex.ui.components.ShopItemCard
import com.umbra.umbradex.ui.components.UmbraBottomNav
import com.umbra.umbradex.ui.components.UmbraTopBar
import com.umbra.umbradex.ui.theme.UmbraBackground
import com.umbra.umbradex.ui.theme.UmbraGold
import com.umbra.umbradex.ui.theme.UmbraPrimary
import com.umbra.umbradex.ui.theme.UmbraSurface
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.umbra.umbradex.data.model.ShopItem
import com.umbra.umbradex.utils.RarityUtils
import com.umbra.umbradex.utils.getAvatarResourceId
import com.umbra.umbradex.R
import com.umbra.umbradex.ui.components.SkinPlaceholderPreview
import com.umbra.umbradex.ui.components.BadgePlaceholderPreview
import com.umbra.umbradex.ui.components.ThemeColorPreview
import com.umbra.umbradex.ui.components.NameColorPreviewEnhanced
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.umbra.umbradex.utils.SoundManager

@Composable
fun ShopScreen(
    navController: NavController,
    viewModel: ShopViewModel = viewModel(),
    userId: String
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current

    var showPurchaseDialog by remember { mutableStateOf<ShopItem?>(null) }
    var showInsufficientFundsDialog by remember { mutableStateOf(false) }
    var showLockedDialog by remember { mutableStateOf<ShopItem?>(null) }
    
    // Recarregar dados quando o ecrã é mostrado
    LaunchedEffect(Unit) {
        viewModel.refresh()
    }
    
    // Haptic feedback and sound when purchase is successful
    LaunchedEffect(uiState.purchaseSuccess) {
        if (uiState.purchaseSuccess != null) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            // Play buy sound when purchase is successful
            SoundManager.playBuySomethingSound()
        }
    }

    // Filtrar itens
    val filteredItems = remember(
        uiState.items,
        uiState.selectedCategory,
        uiState.selectedRarity
    ) {
        uiState.items
            .filter { item ->
                (uiState.selectedCategory == null || item.type == uiState.selectedCategory) &&
                        (uiState.selectedRarity == null || item.rarity == uiState.selectedRarity)
            }
    }

    Scaffold(
        topBar = { UmbraTopBar(navController = navController) },
        bottomBar = { UmbraBottomNav(navController = navController) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Filtros
                ShopFilters(
                    selectedCategory = uiState.selectedCategory,
                    selectedRarity = uiState.selectedRarity,
                    onCategorySelected = { viewModel.filterByCategory(it) },
                    onRaritySelected = { viewModel.filterByRarity(it) }
                )

                // Grid de itens
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredItems) { item ->
                            ShopItemCard(
                                item = item,
                                isOwned = uiState.ownedItems.contains(item.name),
                                isEquipped = isItemEquipped(item, uiState),
                                userLevel = uiState.userLevel,
                                userGold = uiState.userGold,
                                onClick = {
                                    when {
                                        uiState.ownedItems.contains(item.name) -> {
                                            // Já possui
                                        }
                                        item.minLevel > uiState.userLevel -> {
                                            showLockedDialog = item
                                        }
                                        item.price > uiState.userGold -> {
                                            showInsufficientFundsDialog = true
                                        }
                                        else -> {
                                            showPurchaseDialog = item
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Dialogs
            showPurchaseDialog?.let { item ->
                PurchaseConfirmDialog(
                    item = item,
                    currentGold = uiState.userGold,
                    onConfirm = {
                        viewModel.purchaseItem(item, userId)
                        showPurchaseDialog = null
                    },
                    onDismiss = { showPurchaseDialog = null }
                )
            }

            if (showInsufficientFundsDialog) {
                InsufficientFundsDialog(
                    onDismiss = { showInsufficientFundsDialog = false }
                )
            }

            showLockedDialog?.let { item ->
                ItemLockedDialog(
                    item = item,
                    userLevel = uiState.userLevel,
                    onDismiss = { showLockedDialog = null }
                )
            }

            // Snackbar de sucesso
            uiState.purchaseSuccess?.let { message ->
                LaunchedEffect(message) {
                    kotlinx.coroutines.delay(2000)
                    viewModel.clearMessages()
                }
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    containerColor = Color(0xFF4CAF50)
                ) {
                    Text(message, color = Color.White)
                }
            }

            // Snackbar de erro
            uiState.error?.let { error ->
                LaunchedEffect(error) {
                    kotlinx.coroutines.delay(2000)
                    viewModel.clearMessages()
                }
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    containerColor = Color(0xFFF44336)
                ) {
                    Text(error, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun ShopHeader(gold: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "✨ Loja",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "💰",
                    fontSize = 24.sp
                )
                Text(
                    text = gold.toString(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD700)
                )
            }
        }
    }
}

@Composable
fun ShopFilters(
    selectedCategory: String?,
    selectedRarity: String?,
    onCategorySelected: (String?) -> Unit,
    onRaritySelected: (String?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Filtro de categoria
        Text("Categoria:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) },
                label = { Text("All") }
            )
            FilterChip(
                selected = selectedCategory == "skin",
                onClick = { onCategorySelected("skin") },
                label = { Text("Skins") }
            )
            FilterChip(
                selected = selectedCategory == "theme",
                onClick = { onCategorySelected("theme") },
                label = { Text("Themes") }
            )
            FilterChip(
                selected = selectedCategory == "badge",
                onClick = { onCategorySelected("badge") },
                label = { Text("Badges") }
            )
            FilterChip(
                selected = selectedCategory == "name_color",
                onClick = { onCategorySelected("name_color") },
                label = { Text("Colors") }
            )
        }

        // Filtro de raridade
        Text("Raridade:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FilterChip(
                selected = selectedRarity == null,
                onClick = { onRaritySelected(null) },
                label = { Text("Tudo") }
            )
            FilterChip(
                selected = selectedRarity == "common",
                onClick = { onRaritySelected("common") },
                label = { Text("Comum") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = RarityUtils.Common.copy(alpha = 0.2f)
                )
            )
            FilterChip(
                selected = selectedRarity == "rare",
                onClick = { onRaritySelected("rare") },
                label = { Text("Raro") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = RarityUtils.Rare.copy(alpha = 0.2f)
                )
            )
            FilterChip(
                selected = selectedRarity == "epic",
                onClick = { onRaritySelected("epic") },
                label = { Text("Épico") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = RarityUtils.Epic.copy(alpha = 0.2f)
                )
            )
            FilterChip(
                selected = selectedRarity == "legendary",
                onClick = { onRaritySelected("legendary") },
                label = { Text("Lendário") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = RarityUtils.Legendary.copy(alpha = 0.2f)
                )
            )
        }
    }
}

@Composable
fun ShopItemCard(
    item: ShopItem,
    isOwned: Boolean,
    isEquipped: Boolean,
    userLevel: Int,
    userGold: Int,
    onClick: () -> Unit
) {
    val isLocked = item.minLevel > userLevel
    val canAfford = userGold >= item.price
    val rarityColor = RarityUtils.getColor(item.rarity)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.75f)
            .clickable(enabled = !isOwned) { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOwned) Color(0xFF2E2E2E) else MaterialTheme.colorScheme.surface
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Badges no topo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Badge de raridade
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = rarityColor.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, rarityColor)
                    ) {
                        Text(
                            text = item.rarity.uppercase(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = rarityColor
                        )
                    }

                    // Badge de OWNED ou EQUIPPED
                    when {
                        isEquipped -> {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF4CAF50)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Equipped",
                                    modifier = Modifier
                                        .size(24.dp)
                                        .padding(4.dp),
                                    tint = Color.White
                                )
                            }
                        }
                        isOwned -> {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF4CAF50).copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4CAF50))
                            ) {
                                Text(
                                    text = "ADQUIRIDO",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Imagem do item
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(2.dp, rarityColor, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Conteúdo do preview baseado no tipo
                    when (item.type) {
                        "theme" -> {
                            // Themes mostram as cores em círculos
                            ThemeColorPreview(
                                colors = item.colors,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        "name_color" -> {
                            // Name colors mostram "Aa" com gradiente
                            NameColorPreviewEnhanced(
                                colors = item.colors,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        "badge" -> {
                            // Badges - tentar carregar imagem, senão placeholder estilizado
                            val resourceId = if (item.assetUrl != null) {
                                getAvatarResourceId(item.assetUrl)
                            } else 0
                            
                            if (resourceId != 0) {
                                Image(
                                    painter = painterResource(id = resourceId),
                                    contentDescription = item.name,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                // Placeholder visual estilizado para badges
                                BadgePlaceholderPreview(
                                    item = item,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        "skin" -> {
                            // Skins - tentar carregar imagem, senão placeholder estilizado
                            val resourceId = if (item.assetUrl != null) {
                                getAvatarResourceId(item.assetUrl)
                            } else 0
                            
                            if (resourceId != 0) {
                                Image(
                                    painter = painterResource(id = resourceId),
                                    contentDescription = item.name,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(4.dp),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                // Placeholder visual estilizado para skins
                                SkinPlaceholderPreview(
                                    item = item,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        else -> {
                            // Fallback genérico
                            val resourceId = if (item.assetUrl != null) {
                                getAvatarResourceId(item.assetUrl)
                            } else 0
                            
                            if (resourceId != 0) {
                                Image(
                                    painter = painterResource(id = resourceId),
                                    contentDescription = item.name,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFF2A2A2A)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Item",
                                        modifier = Modifier.size(40.dp),
                                        tint = rarityColor
                                    )
                                }
                            }
                        }
                    }

                    // Overlay de locked
                    if (isLocked) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = Color.Black.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Locked",
                                    modifier = Modifier.size(32.dp),
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Nome
                Text(
                    text = item.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Descrição
                if (item.description != null) {
                    Text(
                        text = item.description,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Preço ou status
                when {
                    isOwned -> {
                        Text(
                            text = "No Inventário",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                    }
                    isLocked -> {
                        Text(
                            text = "🔒 Level ${item.minLevel}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF9800)
                        )
                    }
                    else -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "💰 ${item.price}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (canAfford) Color(0xFFFFD700) else Color(0xFFF44336)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Dialogs
@Composable
fun PurchaseConfirmDialog(
    item: ShopItem,
    currentGold: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val remainingGold = currentGold - item.price

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Confirmar Compra", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tens a certeza que queres comprar:")
                Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold,
                    color = RarityUtils.getColor(item.rarity)
                )
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Ouro Atual:")
                    Text("💰 $currentGold", fontWeight = FontWeight.Bold)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Preço:")
                    Text("💰 ${item.price}", fontWeight = FontWeight.Bold, color = Color(0xFFF44336))
                }
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Após Compra:", fontWeight = FontWeight.Bold)
                    Text(
                        "💰 $remainingGold",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Text("Comprar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun InsufficientFundsDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Text("💸", fontSize = 48.sp)
        },
        title = {
            Text("Ouro Insuficiente", fontWeight = FontWeight.Bold)
        },
        text = {
            Text(
                "Não tens ouro suficiente para comprar este item. Completa missões para ganhar mais ouro!",
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF44336)
                )
            ) {
                Text("Entendido!")
            }
        }
    )
}

@Composable
fun ItemLockedDialog(
    item: ShopItem,
    userLevel: Int,
    onDismiss: () -> Unit
) {
    val levelsNeeded = item.minLevel - userLevel

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Text("🔒", fontSize = 48.sp)
        },
        title = {
            Text("Item Bloqueado", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Este item requer nível ${item.minLevel}",
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Precisas de mais $levelsNeeded níve${if (levelsNeeded > 1) "is" else "l"}!",
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF9800)
                )
            ) {
                Text("Entendido!")
            }
        }
    )
}

// Helper para verificar se o item está equipado
fun isItemEquipped(item: ShopItem, uiState: ShopUiState): Boolean {
    return when (item.type) {
        "skin" -> item.name == uiState.equippedSkin
        "theme" -> item.name == uiState.equippedTheme
        "badge" -> item.name == uiState.equippedBadge
        "title" -> item.name == uiState.equippedTitle
        else -> false
    }
}