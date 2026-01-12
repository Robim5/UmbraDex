package com.umbra.umbradex.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserPokemon(
    val id: String? = null, // Made optional for queries that only select pokedex_id
    @SerialName("user_id") val userId: String? = null,
    @SerialName("pokedex_id") val pokedexId: Int,
    @SerialName("obtained_at") val obtainedAt: String? = null
)