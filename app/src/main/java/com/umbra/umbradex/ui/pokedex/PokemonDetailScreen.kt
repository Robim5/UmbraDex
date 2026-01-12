package com.umbra.umbradex.ui.pokedex

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.umbra.umbradex.data.model.PokemonDetail
import com.umbra.umbradex.data.model.PokemonStat
import com.umbra.umbradex.ui.components.UmbraButton
import com.umbra.umbradex.ui.components.AccountRequiredDialog
import com.umbra.umbradex.ui.navigation.Screen
import com.umbra.umbradex.ui.theme.UmbraBackground
import com.umbra.umbradex.ui.theme.UmbraPrimary
import com.umbra.umbradex.ui.theme.UmbraSurface
import com.umbra.umbradex.utils.GuestSessionManager
import android.media.MediaPlayer
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import com.umbra.umbradex.ui.theme.*
import com.umbra.umbradex.utils.getTypeColor
import com.umbra.umbradex.data.model.EvolutionStep
import kotlinx.coroutines.delay
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.umbra.umbradex.utils.SoundManager



@Composable
fun PokemonDetailScreen(
    navController: NavController,
    pokemonId: Int,
    viewModel: PokemonDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val showShiny by viewModel.showShiny.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    
    // Estado de modo convidado
    val isGuestMode by GuestSessionManager.isGuestMode.collectAsState()
    var showAccountRequiredDialog by remember { mutableStateOf(false) }

    LaunchedEffect(pokemonId) {
        viewModel.loadPokemon(pokemonId)
    }
    
    // Haptic feedback and sound when action is successful
    LaunchedEffect(successMessage) {
        val message = successMessage
        if (message != null) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            
            // Play appropriate sound based on action
            when {
                message.contains("Living Dex", ignoreCase = true) -> {
                    SoundManager.playAddLivingSound()
                }
                message.contains("Partner", ignoreCase = true) || 
                message.contains("favorito", ignoreCase = true) -> {
                    SoundManager.playFavoriteSound()
                }
            }
            
            delay(3000)
            viewModel.clearMessages()
        }
    }
    
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            delay(3000)
            viewModel.clearMessages()
        }
    }

    // Dialog para quando o convidado tenta favoritar/catch
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
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is DetailUiState.Loading -> {
                    LoadingDetailState()
                }

                is DetailUiState.Error -> {
                    ErrorDetailState(
                        message = state.message,
                        onRetry = { viewModel.loadPokemon(pokemonId) },
                        onBack = { navController.popBackStack() }
                    )
                }

                is DetailUiState.Success -> {
                    val pokemon = state.data

                    var cryPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

                    // Carrega o som do Pokémon - liberta o anterior antes de criar novo
                    LaunchedEffect(pokemon.cryUrl) {
                        // Release previous player if exists
                        cryPlayer?.release()
                        cryPlayer = null
                        
                        pokemon.cryUrl?.let { url ->
                            try {
                                cryPlayer = MediaPlayer().apply {
                                    setDataSource(url)
                                    prepareAsync()
                                }
                            } catch (e: Exception) {
                                android.util.Log.w("PokemonDetail", "Failed to load cry: ${e.message}")
                                cryPlayer = null
                            }
                        }
                    }

                    // Cleanup on dispose
                    DisposableEffect(Unit) {
                        onDispose {
                            cryPlayer?.release()
                            cryPlayer = null
                        }
                    }

                    DetailContent(
                        pokemon = pokemon,
                        showShiny = showShiny,
                        isGuestMode = isGuestMode,
                        onBack = { navController.popBackStack() },
                        onToggleCatch = { 
                            if (isGuestMode) {
                                showAccountRequiredDialog = true
                            } else {
                                viewModel.toggleCatch()
                            }
                        },
                        onToggleFavorite = { 
                            if (isGuestMode) {
                                showAccountRequiredDialog = true
                            } else {
                                viewModel.toggleFavorite()
                            }
                        },
                        onToggleShiny = { viewModel.toggleShiny() },
                        onPlayCry = {
                            try {
                                cryPlayer?.let { player ->
                                    if (!player.isPlaying) {
                                        player.start()
                                    }
                                }
                            } catch (e: Exception) {
                                // Som falhou
                            }
                        }
                    )
                }
            }
            
            // Success Message Snackbar
            AnimatedVisibility(
                visible = successMessage != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp, start = 16.dp, end = 16.dp)
            ) {
                successMessage?.let { message ->
                    Snackbar(
                        containerColor = UmbraGreen,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Text(
                                text = message,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            
            // Error Message Snackbar
            AnimatedVisibility(
                visible = errorMessage != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp, start = 16.dp, end = 16.dp)
            ) {
                errorMessage?.let { message ->
                    Snackbar(
                        containerColor = Color(0xFFE53935),
                        contentColor = Color.White,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Text(
                                text = message,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailContent(
    pokemon: PokemonDetail,
    showShiny: Boolean,
    isGuestMode: Boolean = false,
    onBack: () -> Unit,
    onToggleCatch: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleShiny: () -> Unit,
    onPlayCry: () -> Unit
) {
    val scrollState = rememberScrollState()
    val dominantColor = getTypeColor(pokemon.types.firstOrNull() ?: "Normal")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // --- HEADER ---
        DetailHeader(
            pokemon = pokemon,
            showShiny = showShiny,
            dominantColor = dominantColor,
            isGuestMode = isGuestMode,
            onBack = onBack,
            onToggleFavorite = onToggleFavorite,
            onToggleShiny = onToggleShiny,
            onPlayCry = onPlayCry
        )

        // --- BODY CARD ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-32).dp),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Nome + ID
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = pokemon.name.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )

                    Text(
                        text = "#${pokemon.id.toString().padStart(4, '0')}",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Badges especiais
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (pokemon.isLegendary) {
                        Badge(text = "LENDÁRIO", color = UmbraGold)
                    }
                    if (pokemon.isMythical) {
                        Badge(text = "MÍTICO", color = MaterialTheme.colorScheme.secondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tipos
                TypeRow(types = pokemon.types)

                Spacer(modifier = Modifier.height(24.dp))

                // Descrição
                Text(
                    text = pokemon.description,
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 24.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Info Grid (Peso, Altura, Habilidades)
                InfoGrid(pokemon = pokemon)

                Spacer(modifier = Modifier.height(24.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(24.dp))

                // Stats
                Text(
                    "Estatísticas Base",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                pokemon.stats.forEach { stat ->
                    StatBar(stat = stat, color = dominantColor)
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(24.dp))

                // Evolution Chain
                if (pokemon.evolutions.size > 1) {
                    Text(
                        "Cadeia de Evolução",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    EvolutionChain(evolutions = pokemon.evolutions)

                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Botões de ação - esconde para convidados
                if (!isGuestMode) {
                    ActionButtons(
                        isCaught = pokemon.isCaught,
                        onToggleCatch = onToggleCatch
                    )
                } else {
                    // Mensagem para convidados
                    GuestActionPlaceholder(onToggleCatch = onToggleCatch)
                }

                Spacer(modifier = Modifier.height(80.dp)) // Espaço extra
            }
        }
    }
}

@Composable
fun GuestActionPlaceholder(onToggleCatch: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Bloqueado",
                tint = Color.Gray.copy(alpha = 0.6f),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Cria uma conta para adicionar à Living Dex",
                color = Color.Gray.copy(alpha = 0.7f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onToggleCatch,
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Criar Conta")
            }
        }
    }
}

@Composable
fun DetailHeader(
    pokemon: PokemonDetail,
    showShiny: Boolean,
    dominantColor: Color,
    isGuestMode: Boolean = false,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleShiny: () -> Unit,
    onPlayCry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
    ) {
        // Fundo gradiente
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            dominantColor.copy(alpha = 0.6f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )

        // Botões do topo
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBack,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.3f)
                )
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
            }

            Row {
                // Botão Shiny
                if (pokemon.shinyImageUrl != null) {
                    IconButton(
                        onClick = onToggleShiny,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (showShiny) UmbraGold else Color.Black.copy(alpha = 0.3f)
                        )
                    ) {
                        Icon(Icons.Default.Star, contentDescription = "Brilhante", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Botão Favorito - mostra cadeado se for convidado
                Box {
                    IconButton(
                        onClick = onToggleFavorite,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (isGuestMode) Color.Gray.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.3f)
                        )
                    ) {
                        Icon(
                            if (pokemon.isFavorite && !isGuestMode) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorito",
                            tint = if (isGuestMode) Color.Gray.copy(alpha = 0.5f) 
                                   else if (pokemon.isFavorite) Color(0xFFE91E63) 
                                   else Color.White
                        )
                    }
                    // Pequeno cadeado se for convidado
                    if (isGuestMode) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Bloqueado",
                            modifier = Modifier
                                .size(12.dp)
                                .align(Alignment.TopEnd),
                            tint = Color.Gray.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Botão Cry (som)
                if (pokemon.cryUrl != null) {
                    IconButton(
                        onClick = onPlayCry,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.3f)
                        )
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "Grito", tint = Color.White)
                    }
                }
            }
        }

        // Imagem do Pokémon
        val imageUrl = if (showShiny && pokemon.shinyImageUrl != null) {
            pokemon.shinyImageUrl
        } else {
            pokemon.imageUrl
        }

        val scale by animateFloatAsState(
            targetValue = if (showShiny) 1.2f else 1f,
            animationSpec = spring(stiffness = Spring.StiffnessLow),
            label = "imageScale"
        )

        AsyncImage(
            model = imageUrl,
            contentDescription = pokemon.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.Center)
                .scale(scale)
        )
    }
}

@Composable
fun TypeRow(types: List<String>) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        types.forEach { type ->
            Surface(
                color = getTypeColor(type).copy(alpha = 0.2f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, getTypeColor(type))
            ) {
                Text(
                    text = type.uppercase(),
                    color = getTypeColor(type),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun Badge(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun InfoGrid(pokemon: PokemonDetail) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        InfoItem(label = "Peso", value = "${pokemon.weight} kg", icon = Icons.Default.FitnessCenter)
        InfoItem(label = "Altura", value = "${pokemon.height} m", icon = Icons.Default.Height)
    }

    if (pokemon.abilities.isNotEmpty()) {
        Spacer(modifier = Modifier.height(16.dp))
        Column {
            Text("Habilidades", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                pokemon.abilities.joinToString(", "),
                color = Color.White,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun InfoItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(label, color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
fun StatBar(stat: PokemonStat, color: Color) {
    val animatedProgress by animateFloatAsState(
        targetValue = stat.value / stat.max.toFloat(),
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "statProgress"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stat.name,
            color = Color.Gray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(80.dp)
        )

        Text(
            text = stat.value.toString(),
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(40.dp)
        )

        LinearProgressIndicator(
            progress = animatedProgress,
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "${(animatedProgress * 100).toInt()}%",
            color = Color.Gray,
            fontSize = 10.sp,
            modifier = Modifier.width(40.dp)
        )
    }
}

@Composable
fun EvolutionChain(evolutions: List<com.umbra.umbradex.data.model.EvolutionStep>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        evolutions.forEachIndexed { index, evolution ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(100.dp)
            ) {
                AsyncImage(
                    model = evolution.imageUrl,
                    contentDescription = evolution.name,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        .padding(8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    evolution.name,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                if (evolution.evolutionTrigger.isNotBlank()) {
                    Text(
                        evolution.evolutionTrigger,
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Seta entre evoluções (exceto a última)
            if (index < evolutions.size - 1) {
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
@Composable
fun ActionButtons(
    isCaught: Boolean,
    onToggleCatch: () -> Unit
) {
    Button(
        onClick = onToggleCatch,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isCaught) Color(0xFF757575) else Color(0xFF4CAF50)
        ),
        shape = RoundedCornerShape(12.dp),
        enabled = !isCaught // Disable button if already caught
    ) {
        Icon(
            if (isCaught) Icons.Default.Check else Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            if (isCaught) "ADICIONADO À LIVING DEX" else "ADICIONAR À LIVING DEX",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}
@Composable
fun LoadingDetailState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Carregando detalhes...", color = Color.Gray)
        }
    }
}
@Composable
fun ErrorDetailState(message: String, onRetry: () -> Unit, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = UmbraError, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(message, color = Color.White, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onBack) {
                    Text("Voltar")
                }
                Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                    Text("Tentar Novamente")
                }
            }
        }
    }
}