package com.umbra.umbradex.ui.navigation

sealed class Screen(val route: String) {
    // Auth
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object Onboarding : Screen("onboarding")

    // Main App
    object Home : Screen("home")
    object Pokedex : Screen("pokedex")
    object PokemonDetail : Screen("pokemon_detail/{pokemonId}") {
        fun createRoute(pokemonId: Int) = "pokemon_detail/$pokemonId"
    }
    object PokeLive : Screen("pokelive")
    object Shop : Screen("shop")
    object Missions : Screen("missions")
    object Inventory : Screen("inventory")
    object Teams : Screen("teams")
    object Settings : Screen("settings")
}