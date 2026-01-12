package com.umbra.umbradex.data.model

import kotlinx.serialization.Serializable

/**
 * Modelo detalhado de um Pokémon para a tela de detalhes.
 * Construído a partir de dados da PokeAPI + estado do utilizador.
 */
@Serializable
data class PokemonDetail(
    val id: Int,
    val name: String,
    val imageUrl: String,
    val shinyImageUrl: String? = null,
    val types: List<String>,
    val weight: Double,
    val height: Double,
    val description: String,
    val stats: List<PokemonStat>,
    val evolutions: List<EvolutionStep>,
    val isCaught: Boolean,
    val isFavorite: Boolean,
    val abilities: List<String> = emptyList(),
    val cryUrl: String? = null,
    val isLegendary: Boolean = false,
    val isMythical: Boolean = false
)

@Serializable
data class PokemonStat(
    val name: String,
    val value: Int,
    val max: Int = 255
)

@Serializable
data class EvolutionStep(
    val id: Int,
    val name: String,
    val imageUrl: String,
    val evolutionTrigger: String = ""
)