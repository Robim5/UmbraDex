package com.umbra.umbradex.ui.missions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.umbra.umbradex.ui.components.UmbraBottomNav
import com.umbra.umbradex.ui.theme.UmbraBackground
import com.umbra.umbradex.ui.theme.UmbraPrimary
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import com.umbra.umbradex.utils.RarityUtils
import androidx.compose.foundation.lazy.LazyRow
import kotlinx.coroutines.delay
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.umbra.umbradex.utils.SoundManager

@Composable
fun MissionsScreen(
    navController: NavController,
    viewModel: MissionsViewModel = viewModel(),
    userId: String
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLockedMissions by remember { mutableStateOf(false) }
    var showCompletedMissions by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    
    // Success/Error message states
    var showSuccessMessage by remember { mutableStateOf<String?>(null) }
    var showErrorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(userId) {
        viewModel.loadMissions(userId)
    }
    
    // Observe success messages with haptic feedback and sound
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            // Play get quest sound when mission reward is claimed
            SoundManager.playGetQuestSound()
            showSuccessMessage = message
            delay(3000)
            showSuccessMessage = null
            viewModel.clearMessages()
        }
    }
    
    // Observe error messages
    LaunchedEffect(uiState.error) {
        uiState.error?.let { message ->
            showErrorMessage = message
            delay(4000)
            showErrorMessage = null
            viewModel.clearMessages()
        }
    }

    // Filtrar missões por categoria
    val filteredActiveMissions = remember(uiState.activeMissions, uiState.selectedCategory) {
        if (uiState.selectedCategory == null) {
            uiState.activeMissions
        } else {
            uiState.activeMissions.filter { it.mission.category == uiState.selectedCategory }
        }
    }
    
    val filteredCompletedMissions = remember(uiState.completedMissions, uiState.selectedCategory) {
        if (uiState.selectedCategory == null) {
            uiState.completedMissions
        } else {
            uiState.completedMissions.filter { it.mission.category == uiState.selectedCategory }
        }
    }
    
    val filteredLockedMissions = remember(uiState.lockedMissions, uiState.selectedCategory) {
        if (uiState.selectedCategory == null) {
            uiState.lockedMissions
        } else {
            uiState.lockedMissions.filter { it.mission.category == uiState.selectedCategory }
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
                    text = "Missões",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        },
        bottomBar = { UmbraBottomNav(navController = navController) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Filtros
                MissionFilters(
                    selectedCategory = uiState.selectedCategory,
                    onCategorySelected = { category -> viewModel.filterByCategory(category) }
                )

                // Lista de missões
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Missões Ativas (principais) - Mostrar primeiro as que podem ser resgatadas
                        val sortedActiveMissions = filteredActiveMissions.sortedByDescending { it.canClaim }
                        
                        items(
                            items = sortedActiveMissions,
                            key = { mission -> "active_${mission.mission.id}" }
                        ) { missionData ->
                            AnimatedMissionCard(
                                missionData = missionData,
                                isClaiming = uiState.claimingMissionId == missionData.mission.id,
                                onClaimReward = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.claimReward(userId, missionData.mission.id)
                                }
                            )
                        }
                        
                        // Seção de Missões Completadas (collapsible)
                        if (filteredCompletedMissions.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                CollapsibleSection(
                                    title = "Missões Completas",
                                    count = filteredCompletedMissions.size,
                                    isExpanded = showCompletedMissions,
                                    onToggle = { showCompletedMissions = !showCompletedMissions },
                                    icon = Icons.Default.Check,
                                    iconColor = Color(0xFF4CAF50),
                                    containerColor = Color(0xFF1B5E20).copy(alpha = 0.3f)
                                )
                            }
                            
                            // Show completed missions if expanded
                            if (showCompletedMissions) {
                                items(
                                    items = filteredCompletedMissions,
                                    key = { mission -> "completed_${mission.mission.id}" }
                                ) { missionData ->
                                    AnimatedMissionCard(
                                        missionData = missionData,
                                        isClaiming = false,
                                        onClaimReward = { }
                                    )
                                }
                            }
                        }
                        
                        // Seção de Missões Bloqueadas (collapsible)
                        if (filteredLockedMissions.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                CollapsibleSection(
                                    title = "Missões Bloqueadas",
                                    count = filteredLockedMissions.size,
                                    isExpanded = showLockedMissions,
                                    onToggle = { showLockedMissions = !showLockedMissions },
                                    icon = Icons.Default.Lock,
                                    iconColor = Color.Gray,
                                    containerColor = Color(0xFF424242).copy(alpha = 0.5f)
                                )
                            }
                            
                            // Show locked missions if expanded
                            if (showLockedMissions) {
                                items(
                                    items = filteredLockedMissions,
                                    key = { mission -> "locked_${mission.mission.id}" }
                                ) { missionData ->
                                    AnimatedMissionCard(
                                        missionData = missionData,
                                        isClaiming = false,
                                        onClaimReward = { }
                                    )
                                }
                            }
                        }
                        
                        // Space at bottom for snackbar
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        
            // Success Snackbar
            AnimatedVisibility(
                visible = showSuccessMessage != null,
                enter = slideInVertically(initialOffsetY = { offset -> offset }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { offset -> offset }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp, start = 16.dp, end = 16.dp)
            ) {
                showSuccessMessage?.let { message ->
                    Snackbar(
                        containerColor = Color(0xFF4CAF50),
                        contentColor = Color.White,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFD700)
                            )
                            Text(
                                text = message,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            // Error Snackbar
            AnimatedVisibility(
                visible = showErrorMessage != null,
                enter = slideInVertically(initialOffsetY = { offset -> offset }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { offset -> offset }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp, start = 16.dp, end = 16.dp)
            ) {
                showErrorMessage?.let { message ->
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
                                imageVector = Icons.Default.Warning,
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
fun CollapsibleSection(
    title: String,
    count: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    containerColor: Color
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        shape = RoundedCornerShape(12.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor
                )
                Text(
                    text = "$title ($count)",
                    color = iconColor,
                    fontWeight = FontWeight.Bold
                )
            }
            Icon(
                if (isExpanded) Icons.Default.KeyboardArrowUp 
                else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = iconColor
            )
        }
    }
}

@Composable
fun MissionsHeader(
    gold: Int,
    xp: Int,
    level: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🏆 Missões",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Level badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF9C27B0).copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF9C27B0))
                    ) {
                        Text(
                            text = "LV $level",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF9C27B0)
                        )
                    }

                    // Gold
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("💰", fontSize = 18.sp)
                        Text(
                            text = gold.toString(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MissionFilters(
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit
) {
    val categories = listOf(
        "Todas" to null,
        "📚 Coleção" to "collection",
        "⚡ Tipos" to "type",
        "🌍 Gerações" to "generation",
        "💰 Ouro" to "gold",
        "🛒 Loja" to "shop",
        "👕 Estilo" to "customization",
        "👥 Equipas" to "team",
        "⬆️ Nível" to "level"
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
fun AnimatedMissionCard(
    missionData: MissionWithProgress,
    isClaiming: Boolean,
    onClaimReward: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mission = missionData.mission
    val rarityColor = RarityUtils.getColor(mission.rarity)
    var expanded by remember { mutableStateOf(false) }

    // Animação de glow quando pode resgatar
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    
    // Animação de scale quando está a reclamar
    val claimScale by animateFloatAsState(
        targetValue = if (isClaiming) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "claimScale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(claimScale)
            .shadow(
                elevation = if (missionData.canClaim) 8.dp else 4.dp,
                shape = RoundedCornerShape(16.dp)
            )
            .then(
                if (missionData.canClaim) {
                    Modifier.border(
                        width = 2.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                Color(0xFFFFD700).copy(alpha = glowAlpha),
                                Color(0xFFFFA500).copy(alpha = glowAlpha)
                            )
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                } else Modifier
            )
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                missionData.isCompleted -> Color(0xFF1B5E20).copy(alpha = 0.3f)
                missionData.isLocked -> Color(0xFF424242)
                missionData.canClaim -> Color(0xFF2D2D00).copy(alpha = 0.8f)  // Amarelo escuro para destacar
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header da missão
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ícone de status
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                missionData.isCompleted -> Color(0xFF4CAF50)
                                missionData.isLocked -> Color(0xFF757575)
                                missionData.canClaim -> Color(0xFFFFD700)
                                else -> rarityColor.copy(alpha = 0.3f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            missionData.isCompleted -> Icons.Default.Check
                            missionData.isLocked -> Icons.Default.Lock
                            missionData.canClaim -> Icons.Default.Star
                            else -> Icons.Default.FavoriteBorder
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Título e descrição
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = mission.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            missionData.isLocked -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            missionData.canClaim -> Color(0xFFFFD700)  // Título dourado quando pode resgatar
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = mission.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = if (expanded) Int.MAX_VALUE else 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Badge de raridade
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = rarityColor.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, rarityColor)
                ) {
                    Text(
                        text = mission.rarity.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = rarityColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Barra de progresso
            if (!missionData.isCompleted) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = when {
                                missionData.isLocked -> "🔒 Locked"
                                missionData.canClaim -> "✅ Ready to claim!"
                                else -> "${missionData.progress?.currentValue ?: 0} / ${mission.requirementValue}"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (missionData.canClaim) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface
                        )

                        if (!missionData.isLocked) {
                            Text(
                                text = "${(missionData.progressPercentage * 100).toInt()}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (missionData.canClaim) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }

                    // Progress bar animada
                    AnimatedProgressBar(
                        progress = if (missionData.isLocked) 0f else missionData.progressPercentage,
                        color = when {
                            missionData.canClaim -> Color(0xFFFFD700)
                            missionData.isLocked -> Color(0xFF757575)
                            else -> rarityColor
                        },
                        backgroundColor = Color(0xFF424242)
                    )
                }
            }

            // Recompensas e botão
            AnimatedVisibility(
                visible = expanded || missionData.canClaim || missionData.isCompleted,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))

                    // Recompensas
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        RewardChip(
                            icon = "💰",
                            value = mission.goldReward.toString(),
                            label = "Ouro"
                        )

                        if (mission.xpReward > 0) {
                            RewardChip(
                                icon = "⭐",
                                value = mission.xpReward.toString(),
                                label = "XP"
                            )
                        }
                    }

                    // Botão de claim (apenas quando pode reclamar)
                    if (missionData.canClaim) {
                        Button(
                            onClick = onClaimReward,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            enabled = !isClaiming,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFD700),
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isClaiming) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.Black,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Reclamar Recompensa",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else if (missionData.isCompleted) {
                        // Missão completada
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            color = Color(0xFF4CAF50).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4CAF50))
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF4CAF50)
                                    )
                                    Text(
                                        text = "Completa e Reclamada",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4CAF50)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedProgressBar(
    progress: Float,
    color: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "progress"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .fillMaxHeight()
                .clip(RoundedCornerShape(4.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(color, color.copy(alpha = 0.7f))
                    )
                )
        )
    }
}

@Composable
fun RewardChip(
    icon: String,
    value: String,
    label: String
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = icon, fontSize = 16.sp)
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
