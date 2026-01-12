package com.umbra.umbradex.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Pokemon(
    val id: Int,
    val name: String,
    val imageUrl: String,
    val types: List<String>,      // Ex: ["Fire", "Flying"]
    val height: Double,
    val weight: Double,

    // Estados do Jogador (Cruciais para a UI gamificada)
    val isCaught: Boolean = false,
    val isFavorite: Boolean = false,
    val caughtAt: String? = null  // Data de captura para o detalhe
) {
    // Helper para formatar o ID (ex: #001)
    fun formattedId(): String = "#${id.toString().padStart(3, '0')}"

    // Helper para formatar o nome (ex: bulbasaur -> Bulbasaur)
    fun capitalizedName(): String = name.replaceFirstChar { it.uppercase() }
}