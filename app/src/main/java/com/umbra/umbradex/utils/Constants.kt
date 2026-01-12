package com.umbra.umbradex.utils

object Constants {
    const val BASE_URL_POKEAPI = "https://pokeapi.co/api/v2/"

    // Limites de níveis
    const val MAX_LEVEL = 100
    const val MAX_TEAMS = 22
    const val TEAM_SIZE = 6
    
    // Fórmula de XP: XP_NECESSARIO = BASE_XP + ((nivel - 1) * XP_INCREMENT)
    const val BASE_XP_FOR_LEVEL = 60
    const val XP_INCREMENT_PER_LEVEL = 10

    // Preços da loja (common, rare, epic, legendary)
    const val PRICE_COMMON = 300
    const val PRICE_RARE = 900
    const val PRICE_EPIC = 1600
    const val PRICE_LEGENDARY = 3500

    // Recompensas de missões
    const val MISSION_GOLD_COMMON = 20
    const val MISSION_GOLD_RARE = 50
    const val MISSION_GOLD_EPIC = 120
    const val MISSION_GOLD_LEGENDARY = 350

    const val MISSION_XP_COMMON = 50
    const val MISSION_XP_RARE = 100
    const val MISSION_XP_EPIC = 200
    const val MISSION_XP_LEGENDARY = 300

    // XP por adicionar Pokémon
    const val XP_PER_POKEMON = 10

    // Recompensas de nível
    const val GOLD_PER_LEVEL = 5
    const val GOLD_EVERY_5_LEVELS = 50
    const val GOLD_EVERY_10_LEVELS = 150

    // Total de Pokémon (Gen 1-9)
    const val TOTAL_POKEMON = 1025
    
    // Pokémon por box na Living Dex
    const val POKEMON_PER_BOX = 30
    
    // Cálculo de boxes necessárias
    val TOTAL_BOXES: Int get() = (TOTAL_POKEMON + POKEMON_PER_BOX - 1) / POKEMON_PER_BOX // 35 boxes
    
    /**
     * Calcula o XP necessário para passar de um nível para o próximo
     */
    fun xpForLevel(level: Int): Int {
        return BASE_XP_FOR_LEVEL + ((level - 1) * XP_INCREMENT_PER_LEVEL)
    }
}