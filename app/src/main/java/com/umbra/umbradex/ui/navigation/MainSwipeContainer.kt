package com.umbra.umbradex.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.umbra.umbradex.ui.components.AccountRequiredDialog
import com.umbra.umbradex.ui.home.StartPageScreen
import com.umbra.umbradex.ui.inventory.InventoryScreen
import com.umbra.umbradex.ui.missions.MissionsScreen
import com.umbra.umbradex.ui.pokedex.PokedexScreen
import com.umbra.umbradex.ui.pokelive.PokeLiveScreen
import com.umbra.umbradex.ui.shop.ShopScreen
import com.umbra.umbradex.ui.teams.TeamsScreen
import com.umbra.umbradex.utils.GuestSessionManager
import kotlinx.coroutines.launch

/**
 * Container principal com navegação por swipe entre as telas principais.
 * Ordem: Home -> Pokédex -> Living Dex -> Shop -> Missions -> Items -> Teams
 * Swipe para a direita: avança | Swipe para a esquerda: volta
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainSwipeContainer(
    navController: NavController,
    currentUserId: String?,
    startPageIndex: Int = 0
) {
    val scope = rememberCoroutineScope()
    val isGuestMode by GuestSessionManager.isGuestMode.collectAsState()
    
    // Definição das páginas na ordem correta
    val pages = listOf(
        SwipePage("Home", Screen.Home.route, Icons.Default.Home, requiresAccount = true),
        SwipePage("Pokédex", Screen.Pokedex.route, Icons.Default.List, requiresAccount = false),
        SwipePage("Living", Screen.PokeLive.route, Icons.Default.Star, requiresAccount = true),
        SwipePage("Shop", Screen.Shop.route, Icons.Default.ShoppingCart, requiresAccount = true),
        SwipePage("Missions", Screen.Missions.route, Icons.Default.TaskAlt, requiresAccount = true),
        SwipePage("Items", Screen.Inventory.route, Icons.Default.Backpack, requiresAccount = true),
        SwipePage("Teams", Screen.Teams.route, Icons.Default.Groups, requiresAccount = true)
    )
    
    // Estado do pager
    val pagerState = rememberPagerState(
        initialPage = startPageIndex.coerceIn(0, pages.size - 1),
        pageCount = { pages.size }
    )
    
    // Dialog para conta requerida
    var showAccountRequiredDialog by remember { mutableStateOf(false) }
    
    // Monitorar a página atual para bloquear gestos em modo convidado
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            // Lógica adicional se necessário quando a página muda
        }
    }
    
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
        bottomBar = {
            SwipeNavigationBar(
                pages = pages,
                currentPage = pagerState.currentPage,
                isGuestMode = isGuestMode,
                onPageSelected = { index ->
                    val page = pages[index]
                    if (isGuestMode && page.requiresAccount) {
                        showAccountRequiredDialog = true
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(
                                page = index,
                                animationSpec = tween(
                                    durationMillis = 300,
                                    easing = FastOutSlowInEasing
                                )
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        // Em modo convidado, desativar o swipe para evitar que vejam páginas bloqueadas
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = !isGuestMode, // Desativar swipe em modo convidado
            beyondViewportPageCount = 1 // Pré-carregar páginas adjacentes
        ) { page ->
            val currentPage = pages[page]
            
            // Verificar se a página requer conta e estamos em modo convidado
            if (isGuestMode && currentPage.requiresAccount) {
                // Mostrar mensagem ou redirecionar para Pokedex
                LaunchedEffect(Unit) {
                    // Encontrar o índice da Pokedex
                    val pokedexIndex = pages.indexOfFirst { it.route == Screen.Pokedex.route }
                    if (pokedexIndex >= 0) {
                        pagerState.animateScrollToPage(pokedexIndex)
                    }
                }
            }
            
            // Renderizar a página apropriada
            Box(modifier = Modifier.fillMaxSize()) {
                when (currentPage.route) {
                    Screen.Home.route -> StartPageScreen(navController = navController)
                    Screen.Pokedex.route -> PokedexScreen(navController = navController)
                    Screen.PokeLive.route -> PokeLiveScreen(navController = navController)
                    Screen.Shop.route -> {
                        if (currentUserId != null) {
                            ShopScreen(navController = navController, userId = currentUserId)
                        }
                    }
                    Screen.Missions.route -> {
                        if (currentUserId != null) {
                            MissionsScreen(navController = navController, userId = currentUserId)
                        }
                    }
                    Screen.Inventory.route -> {
                        if (currentUserId != null) {
                            InventoryScreen(navController = navController, userId = currentUserId)
                        }
                    }
                    Screen.Teams.route -> TeamsScreen(navController = navController)
                }
            }
        }
    }
}

/**
 * Dados de uma página no sistema de swipe
 */
data class SwipePage(
    val name: String,
    val route: String,
    val icon: ImageVector,
    val requiresAccount: Boolean = false
)

/**
 * Barra de navegação inferior para o sistema de swipe
 */
@Composable
fun SwipeNavigationBar(
    pages: List<SwipePage>,
    currentPage: Int,
    isGuestMode: Boolean,
    onPageSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        pages.forEachIndexed { index, page ->
            val isSelected = index == currentPage
            val isLocked = isGuestMode && page.requiresAccount
            
            val lockedColor = Color.Gray.copy(alpha = 0.35f)
            val lockedIndicatorColor = Color.Gray.copy(alpha = 0.15f)
            
            // Animação de escala para o ícone selecionado
            val iconScale by animateFloatAsState(
                targetValue = if (isSelected && !isLocked) 1.2f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "iconScale"
            )
            
            // Animação de cor suave
            val iconColor by animateColorAsState(
                targetValue = when {
                    isLocked -> lockedColor
                    isSelected -> Color.White
                    else -> Color.White.copy(alpha = 0.6f)
                },
                animationSpec = tween(durationMillis = 300),
                label = "iconColor"
            )
            
            NavigationBarItem(
                selected = isSelected && !isLocked,
                onClick = { onPageSelected(index) },
                icon = {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.scale(iconScale)
                    ) {
                        Icon(
                            imageVector = page.icon,
                            contentDescription = page.name,
                            modifier = Modifier.size(24.dp),
                            tint = iconColor
                        )
                        if (isLocked) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Bloqueado",
                                modifier = Modifier
                                    .size(10.dp)
                                    .align(Alignment.TopEnd),
                                tint = lockedColor
                            )
                        }
                    }
                },
                label = null,
                alwaysShowLabel = false,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = if (isLocked) lockedColor else Color.White,
                    indicatorColor = if (isLocked) lockedIndicatorColor else MaterialTheme.colorScheme.primary,
                    unselectedIconColor = if (isLocked) lockedColor else Color.White.copy(alpha = 0.6f)
                )
            )
        }
    }
}
