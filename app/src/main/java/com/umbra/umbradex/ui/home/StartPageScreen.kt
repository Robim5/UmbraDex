package com.umbra.umbradex.ui.home

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.umbra.umbradex.R
import com.umbra.umbradex.ui.components.AnimatedCircularChart
import com.umbra.umbradex.ui.components.BadgePlaceholderPreview
import com.umbra.umbradex.ui.components.UmbraBottomNav
import com.umbra.umbradex.ui.components.UmbraTopBar
import com.umbra.umbradex.ui.components.mainScreensOrder
import com.umbra.umbradex.ui.theme.*
import com.umbra.umbradex.utils.getAvatarResourceId
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.text.style.TextAlign
import com.umbra.umbradex.data.model.ShopItem
import com.umbra.umbradex.data.model.UserProfile
import com.umbra.umbradex.ui.navigation.Screen
import com.umbra.umbradex.utils.toBrush
import com.umbra.umbradex.utils.toColor
import kotlin.math.roundToInt
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.umbra.umbradex.utils.SoundManager

@Composable
fun StartPageScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val petClickCount by viewModel.petClickCount.collectAsState()
    
    // Estado para swipe navigation - mais sensível
    var totalDrag by remember { mutableFloatStateOf(0f) }
    val swipeThreshold = 80f // Reduzido para ser mais fácil de ativar

    Scaffold(
        bottomBar = { UmbraBottomNav(navController = navController) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { totalDrag = 0f },
                        onDragEnd = {
                            // Swipe para a direita vai para Pokédex (próxima tela)
                            if (totalDrag > swipeThreshold) {
                                navController.navigate(Screen.Pokedex.route) {
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
            when (val state = uiState) {
            is HomeUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Carregando perfil...", color = Color.Gray)
                    }
                }
            }

            is HomeUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = UmbraError,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(state.message, color = UmbraError, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadData() }) {
                            Text("Tentar Novamente")
                        }
                    }
                }
            }

            is HomeUiState.Success -> {
                StartPageContent(
                    state = state,
                    navController = navController,
                    petClickCount = petClickCount,
                    onPetClick = {
                        viewModel.onPetClick()
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        // Play pet sound using SoundManager
                        SoundManager.playGoodAnimalSound()
                    },
                    onSettingsClick = { navController.navigate(Screen.Settings.route) },
                    viewModel = viewModel
                )
            }
        }
        }
    }
}

@Composable
fun StartPageContent(
    state: HomeUiState.Success,
    navController: NavController,
    petClickCount: Int,
    onPetClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: HomeViewModel
) {
    val profile = state.profile
    val scrollState = rememberScrollState()
    var isStatsExpanded by remember { mutableStateOf(false) }
    
    // Estado local para cores do nome que pode ser atualizado por eventos
    var currentNameColors by remember(profile.getNameColorKey()) { 
        mutableStateOf(profile.getNameColors()) 
    }
    
    // Observar mudanças nas cores do nome
    LaunchedEffect(Unit) {
        com.umbra.umbradex.utils.PokemonDataEvents.inventoryChanged.collect { event ->
            when (event) {
                is com.umbra.umbradex.utils.InventoryEvent.NameColorEquipped -> {
                    val colors = if (event.colors.size < 2) {
                        listOf(event.colors.firstOrNull() ?: "#FFFFFF", event.colors.firstOrNull() ?: "#FFFFFF")
                    } else {
                        event.colors
                    }
                    currentNameColors = colors
                }
                else -> {}
            }
        }
    }

    // Animação de rotação do pet (360º após 3 cliques)
    val petRotation by animateFloatAsState(
        targetValue = if (petClickCount >= 3) 360f else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "petRotation"
    )

    // Mensagens aleatórias do Pokémon
    val petMessages = remember {
        listOf(
            "Pika pika! ⚡",
            "Olá, treinador!",
            "Pronto para aventuras! 🎮",
            "Vamos apanhar mais Pokémon!",
            "És o melhor treinador! 🏆"
        )
    }
    var currentPetMessage by remember { mutableStateOf("") }

    // Use as cores atualizadas em tempo real
    val nameColors = currentNameColors
    
    // Saudação baseada na hora do dia
    val greeting = remember {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Bom dia"
            hour < 18 -> "Boa tarde"
            else -> "Boa noite"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // --- FIXED HEADER with status bar padding ---
        // Usa UmbraTopBar unificado que tem atualização em tempo real
        UmbraTopBar(
            navController = navController,
            showSettings = true,
            nameColors = nameColors
        )
        
        // --- SCROLLABLE CONTENT ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 100.dp) // Extra space for navigation
        ) {
        
        // --- GREETING WITH NAME COLOR AND BADGE ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$greeting,",
                color = Color.Gray,
                fontSize = 16.sp
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Badge equipado à frente do nome
                if (profile.equippedBadge.isNotBlank()) {
                    val badgeResourceId = getAvatarResourceId(profile.equippedBadge)
                    if (badgeResourceId != 0) {
                        Image(
                            painter = painterResource(id = badgeResourceId),
                            contentDescription = "Insígnia",
                            modifier = Modifier.size(28.dp)
                        )
                    } else if (state.equippedBadgeItem != null) {
                        // Usar placeholder estilizado com informação do badge
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                        ) {
                            BadgePlaceholderPreview(
                                item = state.equippedBadgeItem,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        // Fallback: mostrar ícone de estrela
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Insígnia",
                            modifier = Modifier.size(28.dp),
                            tint = UmbraGold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                // Nome com cor equipada
                Text(
                    text = profile.username,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        brush = nameColors.toBrush()
                    ),
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- ÁREA DO TREINADOR (Avatar + Pet) ---
        TrainerSection(
            profile = profile,
            petRotation = petRotation,
            petClickCount = petClickCount,
            onPetClick = {
                onPetClick()
                currentPetMessage = petMessages.random()
            },
            petMessage = currentPetMessage
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- BARRA DE NÍVEL ---
        LevelProgressBar(profile = profile)

        Spacer(modifier = Modifier.height(24.dp))

            // --- SEÇÃO DE ESTATÍSTICAS (PERGAMINHO) ---
            StatsSection(
                state = state,
                isExpanded = isStatsExpanded,
                onToggleExpand = { isStatsExpanded = !isStatsExpanded },
                viewModel = viewModel
            )

            Spacer(modifier = Modifier.height(24.dp)) // Extra space at bottom
        }
    }
}

@Composable
fun TrainerSection(
    profile: UserProfile,
    petRotation: Float,
    petClickCount: Int,
    onPetClick: () -> Unit,
    petMessage: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        contentAlignment = Alignment.Center
    ) {
        // Fundo decorativo
        Box(
            modifier = Modifier
                .size(240.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    )
                )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar do Jogador (maior) - à esquerda/centro
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box {
                    val skinResourceId = getAvatarResourceId(profile.equippedSkin)
                    if (skinResourceId != 0) {
                        Image(
                            painter = painterResource(id = skinResourceId),
                            contentDescription = "Avatar do utilizador",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(140.dp)
                                .clip(CircleShape)
                                .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        )
                    } else {
                        // Fallback: mostrar ícone de pessoa com background
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), MaterialTheme.colorScheme.surface)
                                    )
                                )
                                .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Avatar do utilizador",
                                modifier = Modifier.size(80.dp),
                                tint = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Título equipado abaixo da skin
                Surface(
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = profile.equippedTitle,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))

            // Pokémon Pet (menor) - à direita
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clickable(onClick = onPetClick),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/${profile.equippedPokemonId ?: 1}.png",
                        contentDescription = "Companheiro",
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(petRotation)
                            .scale(if (petClickCount > 0 && petClickCount < 3) 1.1f else 1f)
                    )
                }

                // Mensagem do Pokémon
                if (petMessage.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = petMessage,
                            color = Color.White,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LevelProgressBar(profile: UserProfile) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Nível ${profile.level}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                "${profile.xp} / ${profile.xpForNextLevel} XP",
                color = Color.Gray,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        val progress = (profile.xp.toFloat() / profile.xpForNextLevel.toFloat()).coerceIn(0f, 1f)

        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
fun StatsSection(
    state: HomeUiState.Success,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    viewModel: HomeViewModel
) {
    val completionPercentage = state.pokedexCaught.toFloat() / state.pokedexTotal.toFloat()
    val rankTitle = viewModel.getRankTitle(completionPercentage)
    val rankColor = viewModel.getRankColor(completionPercentage)

    val iconRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "iconRotation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column {
            // Header (sempre visível)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Estatísticas de Coleção",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Rank: $rankTitle",
                        color = rankColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.rotate(iconRotation)
                )
            }

            // Conteúdo expansível
            androidx.compose.animation.AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Gráficos
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        AnimatedCircularChart(
                            value = state.pokedexCaught,
                            max = state.pokedexTotal,
                            label = "Pokédex",
                            color = MaterialTheme.colorScheme.primary,
                            size = 100.dp
                        )

                        AnimatedCircularChart(
                            value = state.missionsCompleted,
                            max = state.missionsTotal,
                            label = "Missões",
                            color = MaterialTheme.colorScheme.secondary,
                            size = 100.dp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Stats extras
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(
                            label = "Tempo Online",
                            value = formatTime(state.totalTimeSeconds)
                        )
                        StatItem(
                            label = "Gold Total",
                            value = "${state.profile.totalGoldEarned}"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Text(
            label,
            color = Color.Gray,
            fontSize = 12.sp
        )
    }
}

fun formatTime(seconds: Long): String {
    val hours = seconds / 3600
    return if (hours > 0) "${hours}h" else "${seconds / 60}m"
}