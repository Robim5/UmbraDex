package com.umbra.umbradex.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class UserProfile(
    val id: String,
    val username: String,
    val email: String,

    // Recursos
    val gold: Long = 0,
    val xp: Long = 0,
    val level: Int = 1,
    @SerialName("xp_for_next_level") val xpForNextLevel: Long = 60,

    // Equipamento
    @SerialName("equipped_pokemon_id") val equippedPokemonId: Int? = null,
    @SerialName("equipped_skin") val equippedSkin: String = "standard_male1",
    @SerialName("equipped_theme") val equippedTheme: String = "theme_default",
    @SerialName("equipped_badge") val equippedBadge: String = "start_badget",
    @SerialName("equipped_title") val equippedTitle: String = "Rookie",
    @SerialName("equipped_name_color") val equippedNameColor: JsonElement? = null, // JSONB array from database

    // Onboarding
    @SerialName("birth_date") val birthDate: String? = null,
    @SerialName("pokemon_knowledge") val pokemonKnowledge: String? = null,
    @SerialName("favorite_type") val favoriteType: String? = null,

    // Estatísticas
    @SerialName("total_time_seconds") val totalTimeSeconds: Long = 0,
    @SerialName("total_gold_earned") val totalGoldEarned: Long = 0,
    @SerialName("total_xp_earned") val totalXpEarned: Long = 0,
    @SerialName("pet_clicks") val petClicks: Int = 0,

    // Metadata
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("last_login") val lastLogin: String? = null
) {
    /**
     * Get the name colors as a list of hex strings.
     * Returns at least 2 colors (required for Brush.linearGradient).
     * If only one color is available, it's duplicated to ensure gradient works.
     */
    fun getNameColors(): List<String> {
        val colors = try {
            when {
                equippedNameColor == null -> listOf("#FFFFFF")
                equippedNameColor is JsonArray -> equippedNameColor.jsonArray.mapNotNull { 
                    try { it.jsonPrimitive.content } catch (e: Exception) { null }
                }.ifEmpty { listOf("#FFFFFF") }
                equippedNameColor is JsonPrimitive -> listOf(equippedNameColor.jsonPrimitive.content)
                else -> listOf("#FFFFFF")
            }
        } catch (e: Exception) {
            listOf("#FFFFFF")
        }
        
        // Brush.linearGradient requires at least 2 colors
        // If only one color, duplicate it to create a valid gradient
        return if (colors.size < 2) {
            listOf(colors.first(), colors.first())
        } else {
            colors
        }
    }
    
    /**
     * Get a string key for caching purposes (stable for Compose remember)
     */
    fun getNameColorKey(): String {
        return equippedNameColor?.toString() ?: "default"
    }
}