package com.umbra.umbradex.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class ShopItem(
    val id: Int,
    val type: String, // 'skin', 'theme', 'badge', 'name_color', 'title'
    val name: String,
    val rarity: String, // 'common', 'rare', 'epic', 'legendary'
    val price: Int,
    @SerialName("asset_url") val assetUrl: String? = null,
    val colors: List<String>? = null, // Para themes (lista de hex colors)
    @SerialName("min_level") val minLevel: Int = 0,
    val description: String? = null,
    @SerialName("is_available") val isAvailable: Boolean = true,
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("created_at") val createdAt: String? = null
)