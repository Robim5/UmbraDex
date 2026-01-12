package com.umbra.umbradex.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Mission(
    val id: Int,
    val title: String,
    val description: String,
    val rarity: String, // common, rare, epic, legendary
    val category: String, // collection, type, generation, etc
    @SerialName("requirement_type") val requirementType: String,
    @SerialName("requirement_value") val requirementValue: Int,
    @SerialName("gold_reward") val goldReward: Int,
    @SerialName("xp_reward") val xpReward: Int,
    @SerialName("prerequisite_mission_id") val prerequisiteMissionId: Int? = null,
    @SerialName("sort_order") val sortOrder: Int = 0
)

@Serializable
data class MissionProgress(
    val id: String, // UUID
    @SerialName("user_id") val userId: String,
    @SerialName("mission_id") val missionId: Int,
    @SerialName("current_value") val currentValue: Int,
    val status: String, // locked, active, pending, completed
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("claimed_at") val claimedAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)