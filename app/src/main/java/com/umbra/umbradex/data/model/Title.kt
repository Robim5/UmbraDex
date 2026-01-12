package com.umbra.umbradex.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Title(
    val id: Int,
    @SerialName("title_text") val titleText: String,
    @SerialName("min_level") val minLevel: Int,
    val rarity: String = "common",
    val colors: List<String>? = null,
    @SerialName("created_at") val createdAt: String? = null
)
