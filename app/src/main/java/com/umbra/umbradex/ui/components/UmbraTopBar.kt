package com.umbra.umbradex.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.umbra.umbradex.R
import com.umbra.umbradex.LocalNameColors
import com.umbra.umbradex.data.repository.UserRepository
import com.umbra.umbradex.ui.navigation.Screen
import com.umbra.umbradex.ui.theme.UmbraGold
import com.umbra.umbradex.ui.theme.UmbraPrimary
import com.umbra.umbradex.ui.theme.UmbraSurface
import com.umbra.umbradex.utils.InventoryEvent
import com.umbra.umbradex.utils.PokemonDataEvents
import com.umbra.umbradex.utils.ProfileEvent
import com.umbra.umbradex.utils.Resource
import androidx.compose.runtime.*
import kotlinx.coroutines.launch

@Composable
fun UmbraTopBar(
    navController: NavController,
    showSettings: Boolean = true,
    nameColors: List<String> = listOf("#6C63FF", "#9D4EDD", "#C77DFF") // Cores padrão se não fornecidas
) {
    val userRepository = UserRepository()
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<com.umbra.umbradex.data.model.UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Obter cores do nome do CompositionLocal global
    val globalNameColors = LocalNameColors.current
    
    // Estado local para cores do nome (pode ser atualizado por eventos)
    var currentNameColors by remember { mutableStateOf(nameColors) }
    
    // Usar as cores globais se disponíveis, senão usar as locais
    val effectiveNameColors = remember(globalNameColors, currentNameColors) {
        if (globalNameColors.isNotEmpty() && globalNameColors.first() != "#FFFFFF") {
            globalNameColors
        } else if (currentNameColors.isNotEmpty()) {
            currentNameColors
        } else {
            listOf("#FFFFFF", "#FFFFFF")
        }
    }
    
    // Estado local para gold e level (atualizado em tempo real)
    var currentGold by remember { mutableStateOf(0L) }
    var currentLevel by remember { mutableStateOf(1) }

    // Carregar perfil inicial
    LaunchedEffect(Unit) {
        scope.launch {
            userRepository.getUserProfile().collect { result ->
                when (result) {
                    is Resource.Success -> {
                        profile = result.data
                        currentGold = result.data.gold
                        currentLevel = result.data.level
                        isLoading = false
                    }
                    is Resource.Error -> {
                        isLoading = false
                    }
                    is Resource.Loading -> {
                        isLoading = true
                    }
                }
            }
        }
    }
    
    // Observar eventos de atualização do perfil
    LaunchedEffect(Unit) {
        PokemonDataEvents.profileUpdated.collect { event ->
            when (event) {
                is ProfileEvent.Updated -> {
                    event.newGold?.let { currentGold = it }
                    event.newLevel?.let { currentLevel = it }
                }
                is ProfileEvent.GoldChanged -> {
                    currentGold = (currentGold + event.amount).coerceAtLeast(0)
                }
                is ProfileEvent.LevelUp -> {
                    currentLevel = event.newLevel
                }
            }
        }
    }
    
    // Observar eventos de mudança de cor do nome
    LaunchedEffect(Unit) {
        PokemonDataEvents.inventoryChanged.collect { event ->
            when (event) {
                is InventoryEvent.NameColorEquipped -> {
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
    
    // Observar eventos de missões resgatadas (que dão gold e podem subir de nível)
    LaunchedEffect(Unit) {
        PokemonDataEvents.missionProgressChanged.collect { event ->
            when (event) {
                is com.umbra.umbradex.utils.MissionEvent.Claimed -> {
                    // Atualizar gold localmente
                    currentGold += event.goldReward
                    // Recarregar perfil para obter o nível atualizado
                    userRepository.getUserProfile().collect { result ->
                        if (result is Resource.Success) {
                            currentLevel = result.data.level
                            currentGold = result.data.gold
                        }
                    }
                }
                else -> {}
            }
        }
    }
    
    // Observar eventos de refresh completo (promocodes, etc.)
    LaunchedEffect(Unit) {
        PokemonDataEvents.refreshAll.collect {
            // Recarregar perfil completo
            userRepository.getUserProfile().collect { result ->
                if (result is Resource.Success) {
                    currentLevel = result.data.level
                    currentGold = result.data.gold
                    profile = result.data
                }
            }
        }
    }
    
    // Converter lista de cores hex para Brush
    val nameBrush = remember(effectiveNameColors) {
        Brush.horizontalGradient(
            colors = effectiveNameColors.map { hex ->
                try {
                    Color(android.graphics.Color.parseColor(hex))
                } catch (e: Exception) {
                    Color(0xFF9C27B0) // Fallback roxo padrão
                }
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isLoading) {
            // Durante loading, mostrar apenas o título
            Text(
                text = "UmbraDex",
                style = MaterialTheme.typography.headlineSmall.copy(
                    brush = nameBrush
                ),
                fontWeight = FontWeight.ExtraBold
            )
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        } else {
            // Nome da App com cor personalizada (mesmo estilo da Home)
            Text(
                text = "UmbraDex",
                style = MaterialTheme.typography.headlineSmall.copy(
                    brush = nameBrush
                ),
                fontWeight = FontWeight.ExtraBold
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Nível (mesmo estilo da Home)
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Lv $currentLevel",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                // Gold (mesmo estilo da Home com imagem money.png)
                Surface(
                    color = UmbraGold.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.money),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "$currentGold",
                            color = UmbraGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                // Settings (mesmo estilo da Home)
                if (showSettings) {
                    IconButton(onClick = {
                        navController.navigate(Screen.Settings.route)
                    }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Definições",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}