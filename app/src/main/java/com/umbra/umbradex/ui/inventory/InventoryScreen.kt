package com.umbra.umbradex.ui.inventory

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.umbra.umbradex.data.model.ShopItem
import com.umbra.umbradex.data.model.UserProfile
import com.umbra.umbradex.ui.components.UmbraBottomNav
import com.umbra.umbradex.utils.getAvatarResourceId
import com.umbra.umbradex.utils.toBrush
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import coil.compose.AsyncImage
import com.umbra.umbradex.utils.RarityUtils
import com.umbra.umbradex.ui.components.UmbraTopBar
import com.umbra.umbradex.ui.theme.UmbraBackground
import com.umbra.umbradex.ui.components.SkinPlaceholderPreview
import com.umbra.umbradex.ui.components.BadgePlaceholderPreview
import com.umbra.umbradex.ui.components.ThemeColorPreview
import com.umbra.umbradex.ui.components.NameColorPreviewEnhanced
import com.umbra.umbradex.data.model.Title
import com.umbra.umbradex.data.repository.TitleInventoryItem
import com.umbra.umbradex.utils.SoundManager

@Composable
fun InventoryScreen(
    navController: NavController,
    viewModel: InventoryViewModel = viewModel(),
    userId: String
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(userId) {
        viewModel.loadInventory(userId)
    }

    // Filtrar itens por categoria selecionada (exceto títulos que têm tratamento especial)
    val filteredItems = remember(uiState.inventoryItems, uiState.selectedCategory) {
        if (uiState.selectedCategory == "title") {
            emptyList() // Títulos são tratados separadamente
        } else {
            uiState.inventoryItems.filter { it.item.type == uiState.selectedCategory }
        }
    }
    
    // Filtrar títulos desbloqueados
    val unlockedTitles = remember(uiState.titleItems) {
        uiState.titleItems.filter { it.isUnlocked }
    }
    
    // Mostrar feedback quando item é equipado and play sound
    val successMessage = uiState.successMessage
    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            // Play equip sound when item is equipped
            SoundManager.playEquipSound()
            kotlinx.coroutines.delay(2000)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = { 
            // Custom header with status bar padding
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                Text(
                    text = "🎒 Inventário",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        },
        bottomBar = { UmbraBottomNav(navController = navController) },
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            // Snackbar para feedback de sucesso
            uiState.successMessage?.let { message ->
                androidx.compose.material3.Snackbar(
                    modifier = Modifier.padding(16.dp),
                    containerColor = Color(0xFF4CAF50)
                ) {
                    Text(message, color = Color.White)
                }
            }
            // Snackbar para erro
            uiState.error?.let { error ->
                androidx.compose.material3.Snackbar(
                    modifier = Modifier.padding(16.dp),
                    containerColor = Color(0xFFE53935)
                ) {
                    Text(error, color = Color.White)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Preview do equipamento atual
            EquippedItemsPreview(
                equippedItems = uiState.equippedItems
            )

            // Filtros de categoria
            CategoryFilters(
                selectedCategory = uiState.selectedCategory,
                onCategorySelected = { viewModel.selectCategory(it) }
            )

            // Grid de itens
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.selectedCategory == "title") {
                // Mostrar títulos desbloqueados por nível
                if (unlockedTitles.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Ainda não tens títulos desbloqueados!\nContinua a subir de nível!",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(unlockedTitles) { titleItem ->
                            TitleItemCard(
                                titleItem = titleItem,
                                isEquipped = titleItem.title.titleText == uiState.equippedItems.title,
                                userLevel = uiState.userLevel,
                                onClick = {
                                    if (titleItem.isUnlocked) {
                                        viewModel.equipTitle(userId, titleItem.title.titleText)
                                    }
                                }
                            )
                        }
                    }
                }
            } else if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Ainda não tens itens nesta categoria!",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredItems) { inventoryItem ->
                        InventoryItemCard(
                            item = inventoryItem.item,
                            isEquipped = isItemEquipped(inventoryItem.item, uiState.equippedItems),
                            onClick = {
                                viewModel.equipItem(userId, inventoryItem.item.name, inventoryItem.item.type)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InventoryHeader() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "🎒 Inventory",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun EquippedItemsPreview(equippedItems: EquippedItems) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Atualmente Equipado",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Skin
                EquippedItemSlot(
                    label = "Aparência",
                    item = equippedItems.skin,
                    icon = "👤"
                )

                // Badge
                EquippedItemSlot(
                    label = "Insígnia",
                    item = equippedItems.badge,
                    icon = "🏆"
                )

                // Theme
                EquippedItemSlot(
                    label = "Tema",
                    item = equippedItems.theme,
                    icon = "🎨"
                )
                
                // Name Color
                EquippedColorSlot(
                    label = "Cor",
                    item = equippedItems.nameColor,
                    icon = "🌈"
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))

            // Title e Partner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "📜 Título",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = equippedItems.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "🐾 Partner",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = if (equippedItems.partnerPokemonId != null)
                            "#${equippedItems.partnerPokemonId}"
                        else "None",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun EquippedItemSlot(
    label: String,
    item: ShopItem?,
    icon: String
) {
    val rarityColor = if (item != null) RarityUtils.getColor(item.rarity) else Color(0xFF616161)
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "$icon $label",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, rarityColor, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            when {
                item == null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF2A2A2A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "?",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                    }
                }
                item.type == "theme" -> {
                    ThemeColorPreview(
                        colors = item.colors,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                item.type == "skin" -> {
                    val resourceId = if (item.assetUrl != null) {
                        getAvatarResourceId(item.assetUrl)
                    } else 0
                    
                    if (resourceId != 0) {
                        Image(
                            painter = painterResource(id = resourceId),
                            contentDescription = item.name,
                            modifier = Modifier.size(48.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        SkinPlaceholderPreview(item = item, modifier = Modifier.fillMaxSize())
                    }
                }
                item.type == "badge" -> {
                    val resourceId = if (item.assetUrl != null) {
                        getAvatarResourceId(item.assetUrl)
                    } else 0
                    
                    if (resourceId != 0) {
                        Image(
                            painter = painterResource(id = resourceId),
                            contentDescription = item.name,
                            modifier = Modifier.size(48.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        BadgePlaceholderPreview(item = item, modifier = Modifier.fillMaxSize())
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF2A2A2A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Item",
                            modifier = Modifier.size(32.dp),
                            tint = rarityColor
                        )
                    }
                }
            }
        }

        Text(
            text = item?.name ?: "Nenhum",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
fun EquippedColorSlot(
    label: String,
    item: ShopItem?,
    icon: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "$icon $label",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (item?.colors != null && item.colors.isNotEmpty()) {
                        try {
                            Color(android.graphics.Color.parseColor(item.colors.first()))
                        } catch (e: Exception) {
                            Color(0xFF616161)
                        }
                    } else {
                        Color(0xFF616161)
                    }
                )
                .border(
                    2.dp,
                    if (item != null) RarityUtils.getColor(item.rarity) else Color(0xFF616161),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            // Just show the color as background
        }

        Text(
            text = item?.name ?: "Nenhum",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
fun CategoryFilters(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    val categories = listOf(
        "👤 Skins" to "skin",
        "🎨 Temas" to "theme",
        "🏆 Insígnias" to "badge",
        "🌈 Cores" to "name_color",
        "📜 Títulos" to "title"
    )

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { (label, category) ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                label = { Text(label) }
            )
        }
    }
}

@Composable
fun InventoryItemCard(
    item: ShopItem,
    isEquipped: Boolean,
    onClick: () -> Unit
) {
    val rarityColor = RarityUtils.getColor(item.rarity)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.75f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEquipped) Color(0xFF1B5E20).copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
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
                // Badge de raridade e equipped
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
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

                    if (isEquipped) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF4CAF50)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Equipado",
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(4.dp),
                                tint = Color.White
                            )
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
                    when (item.type) {
                        "theme" -> {
                            ThemeColorPreview(
                                colors = item.colors,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        "name_color" -> {
                            NameColorPreviewEnhanced(
                                colors = item.colors,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        "skin" -> {
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
                                SkinPlaceholderPreview(
                                    item = item,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        "badge" -> {
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
                                BadgePlaceholderPreview(
                                    item = item,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        else -> {
                            val resourceId = if (item.assetUrl != null) {
                                getAvatarResourceId(item.assetUrl)
                            } else 0
                            
                            if (resourceId != 0) {
                                Image(
                                    painter = painterResource(id = resourceId),
                                    contentDescription = item.name,
                                    modifier = Modifier.size(64.dp),
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

                Spacer(modifier = Modifier.weight(1f))

                // Status
                if (isEquipped) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF4CAF50).copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4CAF50))
                    ) {
                        Text(
                            text = "EQUIPADO",
                            modifier = Modifier.padding(vertical = 6.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Button(
                        onClick = onClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2196F3)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Equipar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Helper para verificar se item está equipado
fun isItemEquipped(item: ShopItem, equipped: EquippedItems): Boolean {
    return when (item.type) {
        "skin" -> item.name == equipped.skin?.name
        "theme" -> item.name == equipped.theme?.name
        "badge" -> item.name == equipped.badge?.name
        "name_color" -> item.name == equipped.nameColor?.name
        else -> false
    }
}

@Composable
fun TitleItemCard(
    titleItem: TitleInventoryItem,
    isEquipped: Boolean,
    userLevel: Int,
    onClick: () -> Unit
) {
    val title = titleItem.title
    val rarityColor = RarityUtils.getColor(title.rarity)
    val isUnlocked = titleItem.isUnlocked
    
    // Criar brush para o texto do título se tiver cores
    val titleBrush = remember(title.colors) {
        if (title.colors != null && title.colors.size >= 2) {
            try {
                Brush.linearGradient(
                    colors = title.colors.map { Color(android.graphics.Color.parseColor(it)) }
                )
            } catch (e: Exception) {
                Brush.linearGradient(colors = listOf(Color.White, Color.White))
            }
        } else if (title.colors != null && title.colors.size == 1) {
            try {
                val color = Color(android.graphics.Color.parseColor(title.colors.first()))
                Brush.linearGradient(colors = listOf(color, color))
            } catch (e: Exception) {
                Brush.linearGradient(colors = listOf(Color.White, Color.White))
            }
        } else {
            Brush.linearGradient(colors = listOf(Color.White, Color.White))
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .clickable(enabled = isUnlocked, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isEquipped -> Color(0xFF1B5E20).copy(alpha = 0.3f)
                !isUnlocked -> Color(0xFF424242).copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Indicador de raridade
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = rarityColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = title.rarity.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = rarityColor
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Ícone de título
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isUnlocked) {
                                Brush.linearGradient(
                                    colors = listOf(
                                        rarityColor.copy(alpha = 0.3f),
                                        rarityColor.copy(alpha = 0.1f)
                                    )
                                )
                            } else {
                                Brush.linearGradient(
                                    colors = listOf(Color.Gray.copy(alpha = 0.3f), Color.Gray.copy(alpha = 0.1f))
                                )
                            }
                        )
                        .border(2.dp, if (isUnlocked) rarityColor else Color.Gray, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "📜",
                        fontSize = 32.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Nome do título com gradiente
                Text(
                    text = title.titleText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    style = if (isUnlocked) {
                        MaterialTheme.typography.titleMedium.copy(
                            brush = titleBrush
                        )
                    } else {
                        MaterialTheme.typography.titleMedium.copy(
                            color = Color.Gray
                        )
                    }
                )

                // Nível necessário
                Text(
                    text = "Level ${title.minLevel}",
                    fontSize = 12.sp,
                    color = if (isUnlocked) Color(0xFF4CAF50) else Color.Gray
                )

                Spacer(modifier = Modifier.weight(1f))

                // Status
                when {
                    isEquipped -> {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF4CAF50).copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4CAF50))
                        ) {
                            Text(
                                text = "EQUIPADO",
                                modifier = Modifier.padding(vertical = 6.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    isUnlocked -> {
                        Button(
                            onClick = onClick,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2196F3)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Equipar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    else -> {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Gray.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "🔒 Bloqueado",
                                modifier = Modifier.padding(vertical = 6.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}