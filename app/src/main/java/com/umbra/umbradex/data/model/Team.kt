package com.umbra.umbradex.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Team(
    val id: String,
    @SerialName("user_id") val userId: String,
    val name: String,
    val region: String,
    @SerialName("gradient_colors") val gradientColors: List<String>, // Lista de cores hex para o gradiente
    @SerialName("created_at") val createdAt: String,
    val pokemon: List<TeamPokemon> = emptyList() // Máximo 6
) {
    /**
     * Representa um Pokémon num slot de equipa.
     * Corresponde à tabela 'team_slots' na base de dados.
     */
    @Serializable
    data class TeamPokemon(
        @SerialName("pokedex_id") val pokedexId: Int,
        @SerialName("slot_position") val slotPosition: Int, // 1 a 6 (database schema)
        val level: Int = 50,
        // Campos calculados/carregados da PokeAPI (não existem na BD)
        val name: String = "",
        @SerialName("image_url") val imageUrl: String = "",
        val types: List<String> = emptyList()
    ) {
        // Alias para compatibilidade
        val id: Int get() = pokedexId
        val pokemonId: Int get() = pokedexId
        val slotIndex: Int get() = slotPosition - 1 // Converte 1-6 para 0-5 para UI
    }

    //Verifica se a equipa está cheia (6 Pokémon)
    fun isFull(): Boolean = pokemon.size >= 6

    /**
     * Verifica se a equipa está vazia
     */
    fun isEmpty(): Boolean = pokemon.isEmpty()

    /**
     * Retorna o número de slots vazios
     */
    fun emptySlots(): Int = 6 - pokemon.size

    /**
     * Verifica se um Pokémon já está na equipa
     */
    fun hasPokemon(pokemonId: Int): Boolean {
        return pokemon.any { it.pokemonId == pokemonId }
    }

    /**
     * Retorna o Pokémon num slot específico (0-5)
     */
    fun getPokemonInSlot(slotIndex: Int): TeamPokemon? {
        return pokemon.find { it.slotIndex == slotIndex }
    }
}

/**
 * DTO para criar uma nova equipa
 */
@Serializable
data class CreateTeamDto(
    val name: String,
    val region: String,
    @SerialName("gradient_colors") val gradientColors: List<String>
)

/**
 * DTO para adicionar/substituir um Pokémon numa equipa
 */
@Serializable
data class AddPokemonToTeamDto(
    @SerialName("team_id") val teamId: String,
    @SerialName("pokemon_id") val pokemonId: Int,
    val level: Int,
    @SerialName("slot_index") val slotIndex: Int
)

/**
 * DTO para atualizar o nome de uma equipa
 */
@Serializable
data class UpdateTeamNameDto(
    @SerialName("team_id") val teamId: String,
    val name: String
)