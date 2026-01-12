package com.umbra.umbradex.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.umbra.umbradex.ui.auth.AuthViewModel
import com.umbra.umbradex.ui.auth.LoginScreen
import com.umbra.umbradex.ui.auth.OnboardingScreen
import com.umbra.umbradex.ui.auth.SignUpScreen
import com.umbra.umbradex.ui.home.StartPageScreen
import com.umbra.umbradex.ui.pokedex.PokemonDetailScreen
import com.umbra.umbradex.ui.navigation.Screen
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import com.umbra.umbradex.ui.pokedex.PokedexScreen
import com.umbra.umbradex.ui.pokelive.PokeLiveScreen
import com.umbra.umbradex.ui.shop.ShopScreen
import com.umbra.umbradex.ui.missions.MissionsScreen
import com.umbra.umbradex.ui.inventory.InventoryScreen
import com.umbra.umbradex.ui.teams.TeamsScreen
import com.umbra.umbradex.ui.settings.SettingsScreen
import com.umbra.umbradex.utils.GuestSessionManager
import com.umbra.umbradex.utils.SoundManager

// Configurações de animação suaves
private const val ANIMATION_DURATION = 350

private fun enterTransition(): EnterTransition {
    return fadeIn(
        animationSpec = tween(
            durationMillis = ANIMATION_DURATION,
            easing = FastOutSlowInEasing
        )
    ) + scaleIn(
        initialScale = 0.92f,
        animationSpec = tween(
            durationMillis = ANIMATION_DURATION,
            easing = FastOutSlowInEasing
        )
    )
}

private fun exitTransition(): ExitTransition {
    return fadeOut(
        animationSpec = tween(
            durationMillis = ANIMATION_DURATION,
            easing = FastOutSlowInEasing
        )
    ) + scaleOut(
        targetScale = 1.08f,
        animationSpec = tween(
            durationMillis = ANIMATION_DURATION,
            easing = FastOutSlowInEasing
        )
    )
}

private fun popEnterTransition(): EnterTransition {
    return fadeIn(
        animationSpec = tween(
            durationMillis = ANIMATION_DURATION,
            easing = FastOutSlowInEasing
        )
    ) + scaleIn(
        initialScale = 1.08f,
        animationSpec = tween(
            durationMillis = ANIMATION_DURATION,
            easing = FastOutSlowInEasing
        )
    )
}

private fun popExitTransition(): ExitTransition {
    return fadeOut(
        animationSpec = tween(
            durationMillis = ANIMATION_DURATION,
            easing = FastOutSlowInEasing
        )
    ) + scaleOut(
        targetScale = 0.92f,
        animationSpec = tween(
            durationMillis = ANIMATION_DURATION,
            easing = FastOutSlowInEasing
        )
    )
}

// Transições de slide para navegação horizontal (ex: detalhes)
private fun slideEnterTransition(): EnterTransition {
    return slideInHorizontally(
        initialOffsetX = { fullWidth -> fullWidth },
        animationSpec = tween(
            durationMillis = ANIMATION_DURATION,
            easing = FastOutSlowInEasing
        )
    ) + fadeIn(
        animationSpec = tween(
            durationMillis = ANIMATION_DURATION,
            easing = FastOutSlowInEasing
        )
    )
}

private fun slideExitTransition(): ExitTransition {
    return slideOutHorizontally(
        targetOffsetX = { fullWidth -> -fullWidth / 4 },
        animationSpec = tween(
            durationMillis = ANIMATION_DURATION,
            easing = FastOutSlowInEasing
        )
    ) + fadeOut(
        animationSpec = tween(
            durationMillis = ANIMATION_DURATION,
            easing = FastOutSlowInEasing
        )
    )
}

private fun slidePopEnterTransition(): EnterTransition {
    return slideInHorizontally(
        initialOffsetX = { fullWidth -> -fullWidth / 4 },
        animationSpec = tween(
            durationMillis = ANIMATION_DURATION,
            easing = FastOutSlowInEasing
        )
    ) + fadeIn(
        animationSpec = tween(
            durationMillis = ANIMATION_DURATION,
            easing = FastOutSlowInEasing
        )
    )
}

private fun slidePopExitTransition(): ExitTransition {
    return slideOutHorizontally(
        targetOffsetX = { fullWidth -> fullWidth },
        animationSpec = tween(
            durationMillis = ANIMATION_DURATION,
            easing = FastOutSlowInEasing
        )
    ) + fadeOut(
        animationSpec = tween(
            durationMillis = ANIMATION_DURATION,
            easing = FastOutSlowInEasing
        )
    )
}

@Composable
fun UmbraNavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val isAuthenticated by authViewModel.isAuthenticated.collectAsState()
    val currentUserId by authViewModel.currentUserId.collectAsState()
    val isGuestMode by GuestSessionManager.isGuestMode.collectAsState()

    // Determinar rota inicial
    // Se está autenticado -> Home
    // Se está em modo convidado -> Pokedex
    // Senão -> Login
    val startDestination = when {
        isAuthenticated -> Screen.Home.route
        isGuestMode -> Screen.Pokedex.route
        else -> Screen.Login.route
    }
    
    // Auth routes where background music should NOT play
    val authRoutes = listOf(
        Screen.Login.route,
        Screen.SignUp.route,
        Screen.Onboarding.route
    )
    
    // Handle background music based on navigation
    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            val currentRoute = destination.route ?: ""
            
            // Check if current route is an auth route
            val isOnAuthScreen = authRoutes.any { currentRoute.startsWith(it) }
            
            if (isOnAuthScreen) {
                // Stop background music on auth screens
                SoundManager.stopBackgroundMusic()
            } else {
                // Start background music on main app screens
                SoundManager.startBackgroundMusic(context)
            }
        }
        navController.addOnDestinationChangedListener(listener)
        
        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { enterTransition() },
        exitTransition = { exitTransition() },
        popEnterTransition = { popEnterTransition() },
        popExitTransition = { popExitTransition() }
    ) {

        // ==================== AUTH SCREENS ====================

        composable(Screen.Login.route) {
            LoginScreen(
                navController = navController,
                viewModel = authViewModel
            )
        }

        composable(Screen.SignUp.route) {
            SignUpScreen(
                navController = navController,
                viewModel = authViewModel
            )
        }

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                navController = navController,
                viewModel = authViewModel
            )
        }

        // ==================== MAIN APP SCREENS ====================

        composable(Screen.Home.route) {
            StartPageScreen(navController = navController)
        }

        composable(Screen.Pokedex.route) {
            PokedexScreen(navController = navController)
        }

        composable(
            route = Screen.PokemonDetail.route,
            arguments = listOf(navArgument("pokemonId") { type = NavType.IntType }),
            // Transições especiais para detalhes - slide horizontal
            enterTransition = { slideEnterTransition() },
            exitTransition = { slideExitTransition() },
            popEnterTransition = { slidePopEnterTransition() },
            popExitTransition = { slidePopExitTransition() }
        ) { backStackEntry ->
            val pokemonId = backStackEntry.arguments?.getInt("pokemonId") ?: 1
            PokemonDetailScreen(
                navController = navController,
                pokemonId = pokemonId
            )
        }

        composable(Screen.PokeLive.route) {
            PokeLiveScreen(navController = navController)
        }

        composable(Screen.Shop.route) {
            // Proteção: redirecionar para login se não autenticado
            val userId = currentUserId
            if (userId.isNullOrEmpty()) {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Shop.route) { inclusive = true }
                    }
                }
            } else {
                ShopScreen(
                    navController = navController,
                    userId = userId
                )
            }
        }

        composable(Screen.Missions.route) {
            val userId = currentUserId
            if (userId.isNullOrEmpty()) {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Missions.route) { inclusive = true }
                    }
                }
            } else {
                MissionsScreen(
                    navController = navController,
                    userId = userId
                )
            }
        }

        composable(Screen.Inventory.route) {
            val userId = currentUserId
            if (userId.isNullOrEmpty()) {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Inventory.route) { inclusive = true }
                    }
                }
            } else {
                InventoryScreen(
                    navController = navController,
                    userId = userId
                )
            }
        }

        composable(Screen.Teams.route) {
            TeamsScreen(navController = navController)
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                navController = navController,
                userId = currentUserId ?: "",
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }

    // Observar autenticação e redirecionar
    LaunchedEffect(isAuthenticated, isGuestMode) {
        val currentRoute = navController.currentDestination?.route
        
        // Se não está autenticado E não está em modo convidado E não está em rotas de auth
        if (!isAuthenticated && !isGuestMode && currentRoute !in authRoutes) {
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }
}