package com.umbra.umbradex.ui.teams

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Picture
import android.view.View
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.umbra.umbradex.data.model.Team
import com.umbra.umbradex.ui.components.UmbraBottomNav
import com.umbra.umbradex.ui.theme.UmbraBackground
import com.umbra.umbradex.ui.theme.UmbraPrimary
import com.umbra.umbradex.ui.navigation.Screen
import kotlinx.coroutines.launch
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.umbra.umbradex.data.model.Pokemon
import com.umbra.umbradex.utils.ImageUtils
import com.umbra.umbradex.utils.SoundManager
import com.umbra.umbradex.utils.toBrush
import com.umbra.umbradex.utils.createTypeGradientBrush
import com.umbra.umbradex.utils.getTypeColor
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamsScreen(
    navController: NavController,
    viewModel: TeamsViewModel = viewModel()
) {
    val context = LocalContext.current

    // Estados do ViewModel
    val teams by viewModel.teams.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    // Estados locais para diálogos
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedTeamForOptions by remember { mutableStateOf<Team?>(null) }
    var selectedTeamForEdit by remember { mutableStateOf<Team?>(null) }
    var selectedTeamForDelete by remember { mutableStateOf<Team?>(null) }
    var showPokemonSelector by remember { mutableStateOf(false) }
    var selectedTeamForPokemon by remember { mutableStateOf<Team?>(null) }
    var selectedSlotIndex by remember { mutableStateOf<Int?>(null) }
    var showLevelDialog by remember { mutableStateOf(false) }
    var selectedPokemonForLevel by remember { mutableStateOf<Pokemon?>(null) }
    
    // Estado de expansão das equipas - mantém o estado por ID da equipa
    // Usar rememberSaveable para persistir mesmo com recomposições
    val expandedTeams = remember { mutableStateMapOf<String, Boolean>() }
    
    // Estado para swipe navigation - mais sensível
    var totalDrag by remember { mutableFloatStateOf(0f) }
    val swipeThreshold = 80f // Reduzido para ser mais fácil de ativar

    Scaffold(
        topBar = {
            // Custom header with status bar padding
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                Text(
                    text = "As Minhas Equipas",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        },
        bottomBar = { UmbraBottomNav(navController = navController) },
        floatingActionButton = {
            if (teams.size < 22) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Criar Equipa")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { totalDrag = 0f },
                        onDragEnd = {
                            // Swipe para a esquerda volta para Items (tela anterior)
                            if (totalDrag < -swipeThreshold) {
                                navController.navigate(Screen.Inventory.route) {
                                    popUpTo(Screen.Home.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                            totalDrag = 0f
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            totalDrag += dragAmount
                        }
                    )
                }
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = error!!,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.refresh() }) {
                            Text("Tentar Novamente")
                        }
                    }
                }
                teams.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Groups,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Ainda não tens equipas",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Cria a tua primeira equipa!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(1),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(teams) { team ->
                            // Obter estado de expansão para esta equipa (default: true para novas)
                            val isExpanded = expandedTeams.getOrPut(team.id) { true }
                            
                            TeamCard(
                                team = team,
                                isExpanded = isExpanded,
                                onExpandToggle = { 
                                    expandedTeams[team.id] = !isExpanded
                                },
                                onClick = {},
                                onLongClick = {
                                    selectedTeamForOptions = team
                                },
                                onPokemonSlotClick = { slotIndex ->
                                    selectedTeamForPokemon = team
                                    selectedSlotIndex = slotIndex
                                    showPokemonSelector = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Diálogo de criação de equipa
    if (showCreateDialog) {
        CreateTeamDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, region ->
                viewModel.createTeam(name, region)
                // Play create team sound
                SoundManager.playCreateTeamSound()
                showCreateDialog = false
            }
        )
    }

    // Menu de opções da equipa
    if (selectedTeamForOptions != null) {
        TeamOptionsMenu(
            teamName = selectedTeamForOptions!!.name,
            onDismiss = { selectedTeamForOptions = null },
            onEdit = {
                selectedTeamForEdit = selectedTeamForOptions
                selectedTeamForOptions = null
            },
            onDownload = {
                viewModel.downloadTeamCard(context, selectedTeamForOptions!!)
                selectedTeamForOptions = null
            },
            onDelete = {
                selectedTeamForDelete = selectedTeamForOptions
                selectedTeamForOptions = null
            }
        )
    }

    // Diálogo de edição do nome
    if (selectedTeamForEdit != null) {
        EditTeamNameDialog(
            currentName = selectedTeamForEdit!!.name,
            onDismiss = { selectedTeamForEdit = null },
            onConfirm = { newName ->
                viewModel.updateTeamName(selectedTeamForEdit!!.id, newName)
                selectedTeamForEdit = null
            }
        )
    }

    // Diálogo de confirmação de eliminação
    if (selectedTeamForDelete != null) {
        DeleteConfirmationDialog(
            teamName = selectedTeamForDelete!!.name,
            onDismiss = { selectedTeamForDelete = null },
            onConfirm = {
                viewModel.deleteTeam(selectedTeamForDelete!!.id)
                selectedTeamForDelete = null
            }
        )
    }

    // Seletor de Pokémon
    if (showPokemonSelector && selectedTeamForPokemon != null && selectedSlotIndex != null) {
        PokemonSelectorDialog(
            onDismiss = { showPokemonSelector = false },
            onPokemonSelected = { pokemon ->
                selectedPokemonForLevel = pokemon
                showPokemonSelector = false
                showLevelDialog = true
            },
            excludedPokemonIds = emptySet()
        )
    }

    // Diálogo de nível do Pokémon
    if (showLevelDialog && selectedPokemonForLevel != null && selectedTeamForPokemon != null && selectedSlotIndex != null) {
        PokemonLevelDialog(
            pokemon = selectedPokemonForLevel!!,
            onDismiss = {
                showLevelDialog = false
                selectedPokemonForLevel = null
            },
            onConfirm = { level ->
                selectedTeamForPokemon?.let { team ->
                    selectedSlotIndex?.let { slot ->
                        viewModel.addOrReplacePokemonInTeam(
                            teamId = team.id,
                            slotIndex = slot,
                            pokemon = selectedPokemonForLevel!!,
                            level = level
                        )
                    }
                }
                showLevelDialog = false
                selectedPokemonForLevel = null
                selectedTeamForPokemon = null
                selectedSlotIndex = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TeamCard(
    team: Team,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onPokemonSlotClick: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = team.gradientColors.toBrush()
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Cabeçalho da equipa (clicável para expandir/colapsar)
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left side - Team info (clickable to expand/collapse)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onExpandToggle() }
                    ) {
                        Text(
                            text = team.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = team.region,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                    
                    // Right side - Counter and expand arrow
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Pokémon counter
                        Text(
                            text = "${team.pokemon.size}/6",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        
                        // Expand/Collapse arrow
                        IconButton(
                            onClick = { onExpandToggle() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp 
                                              else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Lista vertical de Pokémon (animada)
                androidx.compose.animation.AnimatedVisibility(
                    visible = isExpanded,
                    enter = androidx.compose.animation.expandVertically() + 
                            androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.shrinkVertically() + 
                           androidx.compose.animation.fadeOut()
                ) {
                    Column(
                        modifier = Modifier.padding(top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (slotIndex in 0..5) {
                            PokemonSlotVertical(
                                pokemon = team.pokemon.find { it.slotIndex == slotIndex },
                                slotIndex = slotIndex,
                                onClick = { onPokemonSlotClick(slotIndex) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PokemonSlotVertical(
    pokemon: Team.TeamPokemon?,
    slotIndex: Int,
    onClick: () -> Unit
) {
    // Determinar o gradiente baseado nos tipos do Pokémon
    val backgroundBrush = if (pokemon != null && pokemon.types.isNotEmpty()) {
        createTypeGradientBrush(pokemon.types)
    } else {
        Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.3f), Color.White.copy(alpha = 0.2f))
        )
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (pokemon != null) 6.dp else 2.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundBrush)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (pokemon != null) {
                    // Left: Pokemon image in circle with glow effect
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Sempre usar URL do PokeAPI se imageUrl estiver vazia ou null
                        val imageUrl = pokemon.imageUrl.takeIf { it.isNotBlank() }
                            ?: "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/${pokemon.pokemonId}.png"
                        
                        // Pokemon image with glowing border
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.3f))
                                .border(
                                    width = 2.dp,
                                    color = Color.White.copy(alpha = 0.8f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = pokemon.name,
                                modifier = Modifier
                                    .size(48.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                        
                        // Nome e número do Pokemon
                        Column {
                            // Nome do Pokemon - usar nome real, não "Pokemon #numero"
                            val pokemonName = pokemon.name.takeIf { it.isNotBlank() }
                                ?: "Desconhecido"
                            
                            Text(
                                text = pokemonName.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            
                            // Número do Pokémon no Pokédex
                            Text(
                                text = "#${pokemon.pokemonId.toString().padStart(3, '0')}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            
                            // Type badges (small)
                            if (pokemon.types.isNotEmpty()) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    pokemon.types.take(2).forEach { type ->
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color.Black.copy(alpha = 0.3f)
                                        ) {
                                            Text(
                                                text = type.uppercase(),
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Right: Level badge with nice styling
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.4f),
                        modifier = Modifier.border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "LV",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "${pokemon.level}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                } else {
                    // Empty slot - more attractive design
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.1f))
                                .border(
                                    width = 2.dp,
                                    color = Color.White.copy(alpha = 0.3f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Adicionar Pokémon",
                                modifier = Modifier.size(24.dp),
                                tint = Color.White.copy(alpha = 0.6f)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Slot Vazio ${slotIndex + 1}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "Toca para adicionar Pokémon",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTeamDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, region: String) -> Unit
) {
    var teamName by remember { mutableStateOf("") }
    var selectedRegion by remember { mutableStateOf("Kanto") }
    var expanded by remember { mutableStateOf(false) }

    val regions = listOf(
        "Kanto", "Johto", "Hoenn", "Sinnoh", "Unova",
        "Kalos", "Alola", "Galar", "Paldea"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Criar Nova Equipa") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = teamName,
                    onValueChange = { teamName = it },
                    label = { Text("Nome da Equipa") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedRegion,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Região") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        regions.forEach { region ->
                            DropdownMenuItem(
                                text = { Text(region) },
                                onClick = {
                                    selectedRegion = region
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (teamName.isNotBlank()) {
                        onConfirm(teamName, selectedRegion)
                    }
                },
                enabled = teamName.isNotBlank()
            ) {
                Text("Criar")
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
fun TeamOptionsMenu(
    teamName: String,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Opções da Equipa") },
        text = {
            Column {
                ListItem(
                    headlineContent = { Text("Editar Nome") },
                    leadingContent = {
                        Icon(Icons.Default.Edit, contentDescription = null)
                    },
                    modifier = Modifier.clickable {
                        onEdit()
                        onDismiss()
                    }
                )

                ListItem(
                    headlineContent = { Text("Transferir Cartão") },
                    leadingContent = {
                        Icon(Icons.Default.Download, contentDescription = null)
                    },
                    modifier = Modifier.clickable {
                        onDownload()
                        onDismiss()
                    }
                )

                HorizontalDivider()

                ListItem(
                    headlineContent = { Text("Apagar Equipa") },
                    leadingContent = {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color.Red
                        )
                    },
                    modifier = Modifier.clickable {
                        onDelete()
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun EditTeamNameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Nome da Equipa") },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Nome da Equipa") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newName.isNotBlank() && newName != currentName) {
                        onConfirm(newName)
                    }
                },
                enabled = newName.isNotBlank() && newName != currentName
            ) {
                Text("Guardar")
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
fun DeleteConfirmationDialog(
    teamName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = Color.Red,
                modifier = Modifier.size(48.dp)
            )
        },
        title = { Text("Apagar Equipa?") },
        text = {
            Text("Tens a certeza que queres apagar \"$teamName\"? Esta ação não pode ser desfeita.")
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red
                )
            ) {
                Text("Apagar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}