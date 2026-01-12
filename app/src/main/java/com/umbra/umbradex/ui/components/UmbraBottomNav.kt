package com.umbra.umbradex.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.umbra.umbradex.ui.navigation.Screen
import com.umbra.umbradex.ui.theme.UmbraAccent
import com.umbra.umbradex.ui.theme.UmbraPrimary
import com.umbra.umbradex.ui.theme.UmbraSurface
import com.umbra.umbradex.utils.GuestSessionManager
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

// Define os itens do menu com indicação se requer conta
data class BottomNavItem(
    val name: String,
    val route: String,
    val icon: ImageVector,
    val requiresAccount: Boolean = false
)

@Composable
fun UmbraBottomNav(navController: NavController) {
    // Haptic feedback
    val haptic = LocalHapticFeedback.current
    
    // 7 itens: Home, Pokédex, Living Dex, Shop, Missions, Inventory, Teams
    val items = listOf(
        BottomNavItem("Início", Screen.Home.route, Icons.Default.Home, requiresAccount = true),
        BottomNavItem("Pokédex", Screen.Pokedex.route, Icons.Default.List, requiresAccount = false),
        BottomNavItem("Living", Screen.PokeLive.route, Icons.Default.Star, requiresAccount = true),
        BottomNavItem("Loja", Screen.Shop.route, Icons.Default.ShoppingCart, requiresAccount = true),
        BottomNavItem("Missões", Screen.Missions.route, Icons.Default.TaskAlt, requiresAccount = true),
        BottomNavItem("Itens", Screen.Inventory.route, Icons.Default.Backpack, requiresAccount = true),
        BottomNavItem("Equipas", Screen.Teams.route, Icons.Default.Groups, requiresAccount = true)
    )

    // Observa o estado de convidado
    val isGuestMode by GuestSessionManager.isGuestMode.collectAsState()

    // Estado para mostrar o dialog
    var showAccountRequiredDialog by remember { mutableStateOf(false) }

    // Deteta em que ecrã estamos para pintar o ícone
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Só mostramos a barra se NÃO estivermos no Login/Onboarding
    val showBottomBar = currentRoute !in listOf(
        Screen.Login.route, 
        Screen.SignUp.route, 
        Screen.Onboarding.route
    )

    // Dialog para quando o utilizador convidado tenta aceder a funcionalidades bloqueadas
    if (showAccountRequiredDialog) {
        AccountRequiredDialog(
            onDismiss = { showAccountRequiredDialog = false },
            onCreateAccount = {
                showAccountRequiredDialog = false
                // Sai do modo convidado e navega para signup
                GuestSessionManager.disableGuestMode()
                navController.navigate(Screen.SignUp.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        )
    }

    if (showBottomBar) {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.route
                val isLocked = isGuestMode && item.requiresAccount

                // Cores para itens bloqueados - cinzento claro/branco
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
                    onClick = {
                        // Haptic feedback ao clicar
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        
                        if (isLocked) {
                            // Mostra o dialog se estiver bloqueado
                            showAccountRequiredDialog = true
                        } else if (currentRoute != item.route) {
                            val popUpRoute = if (isGuestMode) Screen.Pokedex.route else Screen.Home.route
                            navController.navigate(item.route) {
                                popUpTo(popUpRoute) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    icon = {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.scale(iconScale)
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.name,
                                modifier = Modifier.size(24.dp),
                                tint = iconColor
                            )
                            // Mostra um pequeno cadeado no canto se estiver bloqueado
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
                    // Remove labels - apenas ícones para visual mais limpo
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
}