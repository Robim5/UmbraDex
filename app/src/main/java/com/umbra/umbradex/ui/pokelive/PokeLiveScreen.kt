package com.umbra.umbradex.ui.pokelive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.umbra.umbradex.ui.components.LiveDexSlot
import com.umbra.umbradex.ui.components.UmbraBottomNav
import com.umbra.umbradex.ui.components.UmbraTopBar
import com.umbra.umbradex.ui.theme.UmbraBackground
import com.umbra.umbradex.ui.theme.UmbraPrimary
import com.umbra.umbradex.ui.theme.UmbraSurface
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import com.umbra.umbradex.ui.navigation.Screen
import com.umbra.umbradex.ui.theme.*

@Composable
fun PokeLiveScreen(
    navController: NavController,
    viewModel: PokeLiveViewModel = viewModel()
) {
    val boxState by viewModel.boxState.collectAsState()
    val statsState by viewModel.statsState.collectAsState()
    val currentBox by viewModel.currentBoxIndex.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()

    var isStatsExpanded by remember { mutableStateOf(false) }
    var showBoxSelector by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { UmbraTopBar(navController = navController) },
        bottomBar = { UmbraBottomNav(navController = navController) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header com controles - agora mais compacto (sem título duplicado)
            LiveDexControls(
                currentBox = currentBox,
                totalBoxes = 50,
                sortOrder = sortOrder,
                onPrevBox = { viewModel.prevBox() },
                onNextBox = { viewModel.nextBox() },
                onBoxSelectorClick = { showBoxSelector = true },
                onSortClick = { viewModel.toggleSort() }
            )

            // Grid de Pokémon (6x5 = 30 slots)
            Box(modifier = Modifier.weight(1f)) {
                when (val state = boxState) {
                    is BoxUiState.Loading -> {
                        LoadingBox()
                    }
                    is BoxUiState.Error -> {
                        ErrorBox(message = state.message)
                    }
                    is BoxUiState.Success -> {
                        PokemonGrid(
                            pokemonList = state.pokemonList,
                            onPokemonClick = { pokemon ->
                                navController.navigate(Screen.PokemonDetail.createRoute(pokemon.id))
                            }
                        )
                    }
                }
            }

            // Painel de estatísticas (expansível)
            LiveDexStatsPanel(
                state = statsState,
                isExpanded = isStatsExpanded,
                onToggleExpand = { isStatsExpanded = !isStatsExpanded }
            )
        }
    }

    // Modal de seleção rápida de box
    if (showBoxSelector) {
        BoxSelectorDialog(
            currentBox = currentBox,
            onBoxSelected = { boxNumber ->
                viewModel.goToBox(boxNumber)
                showBoxSelector = false
            },
            onDismiss = { showBoxSelector = false }
        )
    }
}

@Composable
fun LiveDexControls(
    currentBox: Int,
    totalBoxes: Int,
    sortOrder: SortOption,
    onPrevBox: () -> Unit,
    onNextBox: () -> Unit,
    onBoxSelectorClick: () -> Unit,
    onSortClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Controles de box e ordenação
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botão de ordenação
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                modifier = Modifier.clickable { onSortClick() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.SwapVert,
                        contentDescription = "Sort",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = when (sortOrder) {
                            SortOption.NUMBER -> "Nº"
                            SortOption.NAME_ASC -> "A-Z"
                            SortOption.NAME_DESC -> "Z-A"
                            SortOption.TYPE -> "Tipo"
                        },
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Texto "Living Dex" como subtítulo pequeno
            Text(
                text = "Living Dex",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            
            // Placeholder para alinhamento
            Spacer(modifier = Modifier.width(60.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Controles de navegação de box
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botão anterior
            IconButton(
                onClick = onPrevBox,
                enabled = currentBox > 1
            ) {
                Icon(
                    Icons.Default.ChevronLeft,
                    contentDescription = "Anterior",
                    tint = if (currentBox > 1) Color.White else Color.Gray
                )
            }

            // Indicador de box (clicável)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onBoxSelectorClick() }
            ) {
                Text(
                    text = "CAIXA $currentBox",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                val rangeStart = (currentBox - 1) * 30 + 1
                val rangeEnd = (currentBox * 30).coerceAtMost(1025)
                Text(
                    text = "#$rangeStart - #$rangeEnd",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                Text(
                    text = "Toca para selecionar caixa",
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            }

            // Botão seguinte
            IconButton(
                onClick = onNextBox,
                enabled = currentBox < totalBoxes
            ) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Seguinte",
                    tint = if (currentBox < totalBoxes) Color.White else Color.Gray
                )
            }
        }
    }
}

@Composable
fun LiveDexHeader(
    currentBox: Int,
    totalBoxes: Int,
    sortOrder: SortOption,
    onPrevBox: () -> Unit,
    onNextBox: () -> Unit,
    onBoxSelectorClick: () -> Unit,
    onSortClick: () -> Unit
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
            text = "Living Dex",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(12.dp))

        // Controles de box
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botão de ordenação
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                modifier = Modifier.clickable { onSortClick() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.SwapVert,
                        contentDescription = "Sort",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = when (sortOrder) {
                            SortOption.NUMBER -> "Nº"
                            SortOption.NAME_ASC -> "A-Z"
                            SortOption.NAME_DESC -> "Z-A"
                            SortOption.TYPE -> "Tipo"
                        },
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Controles de navegação de box
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botão anterior
            IconButton(
                onClick = onPrevBox,
                enabled = currentBox > 1
            ) {
                Icon(
                    Icons.Default.ChevronLeft,
                    contentDescription = "Anterior",
                    tint = if (currentBox > 1) Color.White else Color.Gray
                )
            }

            // Indicador de box (clicável)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onBoxSelectorClick() }
            ) {
                Text(
                    text = "CAIXA $currentBox",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                val rangeStart = (currentBox - 1) * 30 + 1
                val rangeEnd = (currentBox * 30).coerceAtMost(1025)
                Text(
                    text = "#$rangeStart - #$rangeEnd",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                Text(
                    text = "Toca para selecionar caixa",
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            }

            // Botão seguinte
            IconButton(
                onClick = onNextBox,
                enabled = currentBox < totalBoxes
            ) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Seguinte",
                    tint = if (currentBox < totalBoxes) Color.White else Color.Gray
                )
            }
        }
    }
}

@Composable
fun PokemonGrid(
    pokemonList: List<com.umbra.umbradex.data.model.Pokemon>,
    onPokemonClick: (com.umbra.umbradex.data.model.Pokemon) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(6), // Grid 6x5 = 30 slots
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            items = pokemonList,
            key = { it.id }
        ) { pokemon ->
            LiveDexSlot(
                pokemon = pokemon,
                onClick = { onPokemonClick(pokemon) }
            )
        }
    }
}

@Composable
fun BoxSelectorDialog(
    currentBox: Int,
    onBoxSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Selecionar Caixa", fontWeight = FontWeight.Bold)
        },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(400.dp)
            ) {
                items(50) { index ->
                    val boxNumber = index + 1
                    Surface(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clickable { onBoxSelected(boxNumber) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (boxNumber == currentBox)
                            MaterialTheme.colorScheme.primary
                        else
                            Color.White.copy(alpha = 0.1f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (boxNumber == currentBox) MaterialTheme.colorScheme.secondary else Color.Gray
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = boxNumber.toString(),
                                color = Color.White,
                                fontWeight = if (boxNumber == currentBox)
                                    FontWeight.Bold
                                else
                                    FontWeight.Normal,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        }
    )
}

@Composable
fun LoadingBox() {
    // Usa shimmer loading para Living Dex
    com.umbra.umbradex.ui.components.ShimmerLiveDexGrid()
}

@Composable
fun ErrorBox(message: String) {
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
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(message, color = Color.White)
        }
    }
}