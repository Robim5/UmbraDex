package com.umbra.umbradex.ui.components

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.navigation.NavController
import com.umbra.umbradex.ui.navigation.Screen
import kotlin.math.abs

/**
 * Lista ordenada das telas principais para navegação por swipe.
 * Ordem: Home -> Pokédex -> Living Dex -> Shop -> Missions -> Items -> Teams
 */
val mainScreensOrder = listOf(
    Screen.Home.route,
    Screen.Pokedex.route,
    Screen.PokeLive.route,
    Screen.Shop.route,
    Screen.Missions.route,
    Screen.Inventory.route,
    Screen.Teams.route
)

/**
 * Extensão para adicionar navegação por swipe horizontal a qualquer Composable.
 * 
 * @param navController O NavController para navegação
 * @param currentRoute A rota atual da tela
 * @param isGuestMode Se o utilizador está em modo convidado
 * @param onLockedScreen Callback quando tenta navegar para uma tela bloqueada
 */
@Composable
fun Modifier.swipeNavigation(
    navController: NavController,
    currentRoute: String,
    isGuestMode: Boolean = false,
    onLockedScreen: () -> Unit = {}
): Modifier {
    var totalDrag by remember { mutableFloatStateOf(0f) }
    val swipeThreshold = 80f // Threshold reduzido para melhor sensibilidade
    
    // Telas que requerem conta
    val accountRequiredScreens = listOf(
        Screen.Home.route,
        Screen.PokeLive.route,
        Screen.Shop.route,
        Screen.Missions.route,
        Screen.Inventory.route,
        Screen.Teams.route
    )
    
    return this.pointerInput(currentRoute) {
        detectHorizontalDragGestures(
            onDragStart = { totalDrag = 0f },
            onDragEnd = {
                val currentIndex = mainScreensOrder.indexOf(currentRoute)
                
                if (currentIndex != -1) {
                    when {
                        // Swipe para a esquerda (voltar)
                        totalDrag < -swipeThreshold && currentIndex > 0 -> {
                            val prevRoute = mainScreensOrder[currentIndex - 1]
                            
                            // Verificar se a tela anterior requer conta
                            if (isGuestMode && prevRoute in accountRequiredScreens) {
                                onLockedScreen()
                            } else {
                                navController.navigate(prevRoute) {
                                    popUpTo(mainScreensOrder.first()) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                        // Swipe para a direita (avançar)
                        totalDrag > swipeThreshold && currentIndex < mainScreensOrder.size - 1 -> {
                            val nextRoute = mainScreensOrder[currentIndex + 1]
                            
                            // Verificar se a tela seguinte requer conta
                            if (isGuestMode && nextRoute in accountRequiredScreens) {
                                onLockedScreen()
                            } else {
                                navController.navigate(nextRoute) {
                                    popUpTo(mainScreensOrder.first()) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    }
                }
                
                totalDrag = 0f
            },
            onHorizontalDrag = { _, dragAmount ->
                totalDrag += dragAmount
            }
        )
    }
}

/**
 * Função helper para obter o índice da tela atual na ordem de navegação
 */
fun getScreenIndex(route: String): Int {
    return mainScreensOrder.indexOf(route)
}

/**
 * Verifica se é possível navegar para a esquerda (tela anterior)
 */
fun canSwipeLeft(currentRoute: String): Boolean {
    val index = mainScreensOrder.indexOf(currentRoute)
    return index > 0
}

/**
 * Verifica se é possível navegar para a direita (tela seguinte)
 */
fun canSwipeRight(currentRoute: String): Boolean {
    val index = mainScreensOrder.indexOf(currentRoute)
    return index >= 0 && index < mainScreensOrder.size - 1
}
