package com.umbra.umbradex.ui.pokedex

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.umbra.umbradex.ui.components.PokemonCard
import com.umbra.umbradex.ui.components.UmbraBottomNav
import com.umbra.umbradex.ui.components.UmbraTextField
import com.umbra.umbradex.ui.components.AccountRequiredDialog
import com.umbra.umbradex.ui.theme.UmbraBackground
import com.umbra.umbradex.ui.theme.UmbraPrimary
import com.umbra.umbradex.utils.GuestSessionManager
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.style.TextAlign
import com.umbra.umbradex.ui.navigation.Screen
import com.umbra.umbradex.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokedexScreen(
    navController: NavController,
    viewModel: PokedexViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchText by viewModel.searchText.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()
    val selectedGeneration by viewModel.selectedGeneration.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val showOnlyFavorites by viewModel.showOnlyFavorites.collectAsState()
    val showOnlyCaught by viewModel.showOnlyCaught.collectAsState()

    // Estado de modo convidado
    val isGuestMode by GuestSessionManager.isGuestMode.collectAsState()
    var showAccountRequiredDialog by remember { mutableStateOf(false) }

    var showFilters by remember { mutableStateOf(false) }

    // Dialog para quando o convidado tenta usar filtros que requerem conta
    if (showAccountRequiredDialog) {
        AccountRequiredDialog(
            onDismiss = { showAccountRequiredDialog = false },
            onCreateAccount = {
                showAccountRequiredDialog = false
                GuestSessionManager.disableGuestMode()
                navController.navigate(Screen.SignUp.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        )
    }

    Scaffold(
        bottomBar = { UmbraBottomNav(navController = navController) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            PokedexTopBar(
                searchText = searchText,
                onSearchChange = viewModel::onSearchTextChange,
                hasActiveFilters = selectedType != null || selectedGeneration != null ||
                        showOnlyFavorites || showOnlyCaught || sortOrder != SortOrder.NUMBER,
                onFilterClick = { showFilters = !showFilters }
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Painel de filtros expansível
            AnimatedVisibility(
                visible = showFilters,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                FilterPanel(
                    selectedType = selectedType,
                    onTypeChange = viewModel::setTypeFilter,
                    selectedGeneration = selectedGeneration,
                    onGenerationChange = viewModel::setGenerationFilter,
                    sortOrder = sortOrder,
                    onSortOrderChange = viewModel::setSortOrder,
                    showOnlyFavorites = showOnlyFavorites,
                    onToggleFavorites = {
                        if (isGuestMode) {
                            showAccountRequiredDialog = true
                        } else {
                            viewModel.toggleFavoritesOnly()
                        }
                    },
                    showOnlyCaught = showOnlyCaught,
                    onToggleCaught = {
                        if (isGuestMode) {
                            showAccountRequiredDialog = true
                        } else {
                            viewModel.toggleCaughtOnly()
                        }
                    },
                    onClearAll = viewModel::clearAllFilters,
                    isGuestMode = isGuestMode
                )
            }

            // Conteúdo
            Box(modifier = Modifier.fillMaxSize()) {
                when (val state = uiState) {
                    is PokedexUiState.Loading -> {
                        LoadingGrid()
                    }

                    is PokedexUiState.Error -> {
                        ErrorState(
                            message = state.message,
                            onRetry = { viewModel.loadPokedex() }
                        )
                    }

                    is PokedexUiState.Success -> {
                        if (state.pokemonList.isEmpty()) {
                            EmptyState(
                                onClearFilters = viewModel::clearAllFilters
                            )
                        } else {
                            PokemonGrid(
                                pokemonList = state.pokemonList,
                                filteredCount = state.filteredCount,
                                totalCount = state.totalCount,
                                onPokemonClick = { pokemon ->
                                    navController.navigate(Screen.PokemonDetail.createRoute(pokemon.id))
                                },
                                onFavoriteClick = { pokemonId ->
                                    viewModel.toggleFavorite(pokemonId)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PokedexTopBar(
    searchText: String,
    onSearchChange: (String) -> Unit,
    hasActiveFilters: Boolean,
    onFilterClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        // Título centrado
        Text(
            text = "Pokédex",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Barra de pesquisa e filtro lado a lado
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botão de filtro à esquerda
            FilledIconButton(
                onClick = onFilterClick,
                modifier = Modifier.size(48.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (hasActiveFilters) MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Box {
                    Icon(
                        Icons.Default.FilterList,
                        contentDescription = "Filtros",
                        tint = if (hasActiveFilters) MaterialTheme.colorScheme.secondary else Color.White
                    )
                    if (hasActiveFilters) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(MaterialTheme.colorScheme.secondary, CircleShape)
                                .align(Alignment.TopEnd)
                        )
                    }
                }
            }
            
            // Barra de pesquisa
            UmbraTextField(
                value = searchText,
                onValueChange = onSearchChange,
                label = "Pesquisar Pokémon...",
                icon = Icons.Default.Search,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun FilterPanel(
    selectedType: String?,
    onTypeChange: (String?) -> Unit,
    selectedGeneration: Int?,
    onGenerationChange: (Int?) -> Unit,
    sortOrder: SortOrder,
    onSortOrderChange: (SortOrder) -> Unit,
    showOnlyFavorites: Boolean,
    onToggleFavorites: () -> Unit,
    showOnlyCaught: Boolean,
    onToggleCaught: () -> Unit,
    onClearAll: () -> Unit,
    isGuestMode: Boolean = false
) {
    val types = listOf(
        "Normal", "Fire", "Water", "Electric", "Grass", "Ice",
        "Fighting", "Poison", "Ground", "Flying", "Psychic", "Bug",
        "Rock", "Ghost", "Dragon", "Dark", "Steel", "Fairy"
    )

    val generations = (1..9).toList()
    
    // Cores para elementos bloqueados
    val lockedColor = Color.Gray.copy(alpha = 0.4f)
    
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 400.dp) // Limitar altura para permitir scroll
            .verticalScroll(scrollState)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Filtros",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            TextButton(onClick = onClearAll) {
                Text("Limpar Tudo", color = MaterialTheme.colorScheme.secondary)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        
        // Toggles (Favoritos e Capturados) - NO TOPO
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Chip Favoritos
                FilterChip(
                    selected = showOnlyFavorites && !isGuestMode,
                    onClick = onToggleFavorites,
                    label = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "❤️ Favoritos", 
                                fontSize = 12.sp,
                                color = if (isGuestMode) lockedColor else Color.Unspecified
                            )
                            if (isGuestMode) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = "Bloqueado",
                                    modifier = Modifier.size(12.dp),
                                    tint = lockedColor
                                )
                            }
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = if (isGuestMode) lockedColor else Color(0xFFE91E63),
                        selectedLabelColor = Color.White
                    )
                )

                // Chip Capturados
                FilterChip(
                    selected = showOnlyCaught && !isGuestMode,
                    onClick = onToggleCaught,
                    label = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "✓ Capturados", 
                                fontSize = 12.sp,
                                color = if (isGuestMode) lockedColor else Color.Unspecified
                            )
                            if (isGuestMode) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = "Bloqueado",
                                    modifier = Modifier.size(12.dp),
                                    tint = lockedColor
                                )
                            }
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = if (isGuestMode) lockedColor else Color(0xFF4CAF50),
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Ordenação em grid 2x2
        Text("Ordenar por", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = sortOrder == SortOrder.NUMBER,
                    onClick = { onSortOrderChange(SortOrder.NUMBER) },
                    label = { Text("📋 Número", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = UmbraGold,
                        selectedLabelColor = Color.Black
                    )
                )
                FilterChip(
                    selected = sortOrder == SortOrder.NAME_ASC,
                    onClick = { onSortOrderChange(SortOrder.NAME_ASC) },
                    label = { Text("🔤 A-Z", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = UmbraGold,
                        selectedLabelColor = Color.Black
                    )
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = sortOrder == SortOrder.NAME_DESC,
                    onClick = { onSortOrderChange(SortOrder.NAME_DESC) },
                    label = { Text("🔤 Z-A", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = UmbraGold,
                        selectedLabelColor = Color.Black
                    )
                )
                FilterChip(
                    selected = sortOrder == SortOrder.TYPE,
                    onClick = { onSortOrderChange(SortOrder.TYPE) },
                    label = { Text("🏷️ Tipo", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = UmbraGold,
                        selectedLabelColor = Color.Black
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        // Geração em grid 3x3
        Text("Geração", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (rowStart in generations.indices step 3) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (i in 0..2) {
                        val gen = generations.getOrNull(rowStart + i)
                        if (gen != null) {
                            FilterChip(
                                selected = selectedGeneration == gen,
                                onClick = { onGenerationChange(if (selectedGeneration == gen) null else gen) },
                                label = { Text("Gen $gen", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tipos em grid 3x6
        Text("Tipo", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (rowStart in types.indices step 3) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (i in 0..2) {
                        val type = types.getOrNull(rowStart + i)
                        if (type != null) {
                            FilterChip(
                                selected = selectedType == type,
                                onClick = { onTypeChange(if (selectedType == type) null else type) },
                                label = { Text(type, fontSize = 10.sp) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PokemonGrid(
    pokemonList: List<com.umbra.umbradex.data.model.Pokemon>,
    filteredCount: Int,
    totalCount: Int,
    onPokemonClick: (com.umbra.umbradex.data.model.Pokemon) -> Unit,
    onFavoriteClick: (Int) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            items = pokemonList,
            key = { it.id }
        ) { pokemon ->
            PokemonCard(
                pokemon = pokemon,
                onClick = { onPokemonClick(pokemon) }
            )
        }
    }
}

@Composable
fun LoadingGrid() {
    // Usa shimmer loading para uma experiência visual mais agradável
    com.umbra.umbradex.ui.components.ShimmerLoadingGrid(
        itemCount = 18,
        columns = 3
    )
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = UmbraError,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                message,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Tentar Novamente")
            }
        }
    }
}

@Composable
fun EmptyState(onClearFilters: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Nenhum Pokémon encontrado",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Tenta ajustar os filtros",
                color = Color.Gray,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onClearFilters,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Limpar Filtros")
            }
        }
    }
}

// FlowRow personalizado (se não tiver no Compose)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints) }

        var xPos = 0
        var yPos = 0
        var maxHeight = 0

        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.forEach { placeable ->
                if (xPos + placeable.width > constraints.maxWidth) {
                    xPos = 0
                    yPos += maxHeight + 8.dp.roundToPx()
                    maxHeight = 0
                }

                placeable.place(xPos, yPos)
                xPos += placeable.width + 8.dp.roundToPx()
                maxHeight = maxOf(maxHeight, placeable.height)
            }
        }
    }
}