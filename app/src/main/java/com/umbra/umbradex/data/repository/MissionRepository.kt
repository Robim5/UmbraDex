package com.umbra.umbradex.data.repository

import android.util.Log
import com.umbra.umbradex.data.model.Mission
import com.umbra.umbradex.data.model.MissionProgress
import com.umbra.umbradex.data.supabase.UmbraSupabase
import com.umbra.umbradex.utils.Resource
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class MissionRepository {
    
    private val db = UmbraSupabase.db
    private val auth = UmbraSupabase.auth

    // Buscar todas as missões disponíveis
    suspend fun getAllMissions(): Flow<Resource<List<Mission>>> = flow {
        emit(Resource.Loading)
        try {
            val missions = UmbraSupabase.client.from("missions")
                .select()
                .decodeList<Mission>()
                .sortedBy { it.sortOrder }

            emit(Resource.Success(missions))
        } catch (e: Exception) {
            emit(Resource.Error("Failed to load missions: ${e.message}", e))
        }
    }

    // Buscar progresso do usuário em todas as missões
    suspend fun getUserMissionProgress(userId: String): Flow<Resource<List<MissionProgress>>> = flow {
        emit(Resource.Loading)
        try {
            val progress = UmbraSupabase.client.from("missions_progress")
                .select() {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<MissionProgress>()

            emit(Resource.Success(progress))
        } catch (e: Exception) {
            emit(Resource.Error("Failed to load progress: ${e.message}", e))
        }
    }
    
    /**
     * Buscar progresso atual (non-flow version para uso rápido)
     */
    suspend fun getUserMissionProgressSync(userId: String): List<MissionProgress> {
        return try {
            UmbraSupabase.client.from("missions_progress")
                .select() {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<MissionProgress>()
        } catch (e: Exception) {
            Log.e("MissionRepository", "Failed to get progress: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Sincroniza completamente as missões com o servidor.
     * Esta é a função PRINCIPAL que deve ser chamada quando há ações que afetam missões.
     * Chama a RPC sync_user_stats_and_missions que:
     * 1. Recalcula todos os user_global_stats com base nos dados reais
     * 2. Atualiza todas as missões com os valores corretos
     */
    suspend fun syncAllMissions(userId: String): Boolean {
        return try {
            Log.d("MissionRepository", "Syncing all missions for user $userId")
            
            // Usar a RPC do servidor que faz sync completo
            db.rpc("sync_user_stats_and_missions", buildJsonObject {
                put("p_user_id", userId)
            })
            
            Log.d("MissionRepository", "Successfully synced all missions via server RPC")
            true
        } catch (e: Exception) {
            Log.e("MissionRepository", "Failed to sync missions via RPC: ${e.message}")
            // Fallback para sync local
            try {
                syncAllMissionsLocally(userId)
                true
            } catch (e2: Exception) {
                Log.e("MissionRepository", "Local sync also failed: ${e2.message}")
                false
            }
        }
    }
    
    /**
     * Sync local de missões - fallback quando a RPC não está disponível
     */
    private suspend fun syncAllMissionsLocally(userId: String) {
        Log.d("MissionRepository", "Performing local sync for missions")
        
        // Buscar todas as missões
        val allMissions = db.from("missions")
            .select()
            .decodeList<Mission>()
        
        // Buscar progresso existente
        val existingProgress = db.from("missions_progress")
            .select() {
                filter { eq("user_id", userId) }
            }
            .decodeList<MissionProgress>()
        
        val progressMap = existingProgress.associateBy { it.missionId }
        val completedMissionIds = existingProgress
            .filter { it.status == "completed" }
            .map { it.missionId }
            .toSet()
        
        // Atualizar cada missão com o valor global correto
        for (mission in allMissions) {
            val progress = progressMap[mission.id]
            val globalValue = getGlobalProgressForRequirement(userId, mission.requirementType)
            val correctValue = minOf(globalValue, mission.requirementValue)
            
            // Determinar se a missão deveria ser ativa
            val shouldBeActive = mission.prerequisiteMissionId == null || 
                                 completedMissionIds.contains(mission.prerequisiteMissionId)
            
            when {
                // Missão já completada - não tocar
                progress?.status == "completed" -> continue
                
                // Missão ativa - atualizar valor
                progress?.status == "active" -> {
                    if (progress.currentValue != correctValue) {
                        db.from("missions_progress")
                            .update({
                                set("current_value", correctValue)
                                set("updated_at", "now()")
                            }) {
                                filter {
                                    eq("user_id", userId)
                                    eq("mission_id", mission.id)
                                }
                            }
                        Log.d("MissionRepository", "Updated ${mission.title}: ${progress.currentValue} -> $correctValue")
                    }
                }
                
                // Missão locked mas deveria ser ativa - corrigir
                progress?.status == "locked" && shouldBeActive -> {
                    db.from("missions_progress")
                        .update({
                            set("status", "active")
                            set("current_value", correctValue)
                            set("updated_at", "now()")
                        }) {
                            filter {
                                eq("user_id", userId)
                                eq("mission_id", mission.id)
                            }
                        }
                    Log.d("MissionRepository", "Unlocked ${mission.title} with progress $correctValue")
                }
                
                // Sem progresso mas deveria ter - criar
                progress == null && shouldBeActive -> {
                    db.from("missions_progress")
                        .insert(buildJsonObject {
                            put("user_id", userId)
                            put("mission_id", mission.id)
                            put("current_value", correctValue)
                            put("status", "active")
                        })
                    Log.d("MissionRepository", "Created progress for ${mission.title} with value $correctValue")
                }
            }
        }
    }

    /**
     * Reclamar recompensa de uma missão que está pronta para ser reclamada.
     * Usa a nova função claim_mission_reward_v2 que mantém o progresso contínuo
     */
    suspend fun claimMissionReward(userId: String, missionId: Int): Resource<ClaimResult> {
        return try {
            Log.d("MissionRepository", "Claiming reward for mission $missionId using v2")
            
            // Tentar usar a nova função RPC v2 que mantém progresso contínuo
            try {
                val result = db.rpc("claim_mission_reward_v2", buildJsonObject {
                    put("p_user_id", userId)
                    put("p_mission_id", missionId)
                })
                
                // Parsear o resultado JSON
                val resultJson = result.decodeAs<kotlinx.serialization.json.JsonObject>()
                val goldReward = resultJson["gold_reward"]?.toString()?.toIntOrNull() ?: 0
                val xpReward = resultJson["xp_reward"]?.toString()?.toIntOrNull() ?: 0
                val nextMissionId = resultJson["next_mission_id"]?.toString()?.toIntOrNull()
                
                Log.d("MissionRepository", "Successfully claimed ${goldReward} gold and ${xpReward} XP via v2")
                
                return Resource.Success(ClaimResult(
                    goldReward = goldReward,
                    xpReward = xpReward,
                    nextMissionId = nextMissionId
                ))
            } catch (e: Exception) {
                Log.w("MissionRepository", "claim_mission_reward_v2 failed, falling back to v1: ${e.message}")
            }
            
            // Fallback para o método antigo caso a v2 não esteja disponível
            val mission = db.from("missions")
                .select() {
                    filter { eq("id", missionId) }
                }
                .decodeSingle<Mission>()
            
            val progressList = db.from("missions_progress")
                .select() {
                    filter {
                        eq("user_id", userId)
                        eq("mission_id", missionId)
                    }
                }
                .decodeList<MissionProgress>()
            
            val progress = progressList.firstOrNull()
            
            if (progress == null) {
                if (mission.prerequisiteMissionId != null) {
                    return Resource.Error("Mission is locked - prerequisite not completed")
                }
                Log.w("MissionRepository", "Creating progress for root mission $missionId")
            } else {
                if (progress.status != "active") {
                    return Resource.Error("Mission is not active (status: ${progress.status})")
                }
                
                if (progress.currentValue < mission.requirementValue) {
                    return Resource.Error("Mission not completed yet (${progress.currentValue}/${mission.requirementValue})")
                }
            }
            
            db.rpc("add_gold", buildJsonObject {
                put("p_user_id", userId)
                put("p_amount", mission.goldReward)
            })
            
            db.rpc("add_xp_and_level_up", buildJsonObject {
                put("p_user_id", userId)
                put("p_xp_amount", mission.xpReward)
            })
            
            if (progress != null) {
                db.from("missions_progress")
                    .update({
                        set("status", "completed")
                        set("completed_at", "now()")
                        set("updated_at", "now()")
                    }) {
                        filter {
                            eq("user_id", userId)
                            eq("mission_id", missionId)
                        }
                    }
            } else {
                db.from("missions_progress")
                    .insert(buildJsonObject {
                        put("user_id", userId)
                        put("mission_id", missionId)
                        put("current_value", mission.requirementValue)
                        put("status", "completed")
                    })
            }
            
            activateNextMission(userId, missionId)
            
            Log.d("MissionRepository", "Successfully claimed ${mission.goldReward} gold and ${mission.xpReward} XP")
            
            Resource.Success(ClaimResult(
                goldReward = mission.goldReward,
                xpReward = mission.xpReward,
                nextMissionId = getNextMissionId(missionId)
            ))
        } catch (e: Exception) {
            Log.e("MissionRepository", "Failed to claim reward: ${e.message}", e)
            Resource.Error("Failed to claim reward: ${e.message}", e)
        }
    }
    
    /**
     * Ativar a próxima missão na cadeia após completar uma.
     * IMPORTANTE: Agora busca o progresso global para inicializar a nova missão
     * com o valor correto (progresso contínuo).
     */
    private suspend fun activateNextMission(userId: String, completedMissionId: Int) {
        try {
            // Buscar missão que tem esta como pré-requisito
            val nextMissions = db.from("missions")
                .select() {
                    filter {
                        eq("prerequisite_mission_id", completedMissionId)
                    }
                }
                .decodeList<Mission>()
            
            for (nextMission in nextMissions) {
                // Buscar o progresso global para este tipo de requisito
                val globalValue = getGlobalProgressForRequirement(userId, nextMission.requirementType)
                
                // Verificar se já existe progresso para esta missão
                val existingProgress = try {
                    db.from("missions_progress")
                        .select() {
                            filter {
                                eq("user_id", userId)
                                eq("mission_id", nextMission.id)
                            }
                        }
                        .decodeList<MissionProgress>()
                } catch (e: Exception) {
                    emptyList()
                }
                
                val initialValue = minOf(globalValue, nextMission.requirementValue)
                
                if (existingProgress.isEmpty()) {
                    // Criar novo progresso como 'active' COM O VALOR GLOBAL
                    db.from("missions_progress")
                        .insert(buildJsonObject {
                            put("user_id", userId)
                            put("mission_id", nextMission.id)
                            put("current_value", initialValue)
                            put("status", "active")
                        })
                    Log.d("MissionRepository", "Activated next mission: ${nextMission.title} with progress $initialValue/${nextMission.requirementValue}")
                } else if (existingProgress.first().status == "locked") {
                    // Atualizar de locked para active COM O VALOR GLOBAL
                    db.from("missions_progress")
                        .update({
                            set("status", "active")
                            set("current_value", initialValue)
                            set("updated_at", "now()")
                        }) {
                            filter {
                                eq("user_id", userId)
                                eq("mission_id", nextMission.id)
                            }
                        }
                    Log.d("MissionRepository", "Unlocked mission: ${nextMission.title} with progress $initialValue/${nextMission.requirementValue}")
                }
            }
        } catch (e: Exception) {
            Log.e("MissionRepository", "Failed to activate next mission: ${e.message}")
        }
    }
    
    /**
     * Buscar o progresso global atual para um tipo de requisito.
     * Usa uma RPC do servidor ou calcula manualmente contando registros.
     */
    private suspend fun getGlobalProgressForRequirement(userId: String, requirementType: String): Int {
        return try {
            // Para nível e gold, buscar diretamente do perfil
            if (requirementType == "reach_level") {
                val profile = db.from("profiles")
                    .select(columns = Columns.list("level")) {
                        filter { eq("id", userId) }
                    }
                    .decodeSingleOrNull<ProfileLevel>()
                return profile?.level ?: 0
            }
            
            if (requirementType == "earn_gold") {
                val profile = db.from("profiles")
                    .select(columns = Columns.list("total_gold_earned")) {
                        filter { eq("id", userId) }
                    }
                    .decodeSingleOrNull<ProfileGold>()
                return profile?.totalGoldEarned ?: 0
            }
            
            // Tentar buscar da tabela user_global_stats
            val statsResult = try {
                // Determinar qual coluna buscar
                val columnName = when {
                    requirementType == "collect_pokemon" -> "total_pokemon_collected"
                    requirementType == "favorite_count" -> "total_favorites"
                    requirementType == "create_team" -> "total_teams"
                    requirementType == "shop_buy" -> "total_shop_purchases"
                    requirementType == "own_skins" -> "total_skins"
                    requirementType == "own_badges" -> "total_badges"
                    requirementType == "own_themes" -> "total_themes"
                    requirementType == "own_name_colors" -> "total_name_colors"
                    requirementType.startsWith("collect_type_") -> "type_${requirementType.removePrefix("collect_type_")}"
                    requirementType.startsWith("collect_gen_") -> "gen_${requirementType.removePrefix("collect_gen_")}"
                    else -> null
                } ?: return 0
                
                // Buscar valor específico
                val result = db.from("user_global_stats")
                    .select(columns = Columns.list(columnName)) {
                        filter { eq("user_id", userId) }
                    }
                    .decodeSingleOrNull<kotlinx.serialization.json.JsonObject>()
                
                result?.get(columnName)?.toString()?.toIntOrNull() ?: 0
            } catch (e: Exception) {
                Log.w("MissionRepository", "Could not get from user_global_stats: ${e.message}")
                -1 // Flag to use fallback
            }
            
            // Se conseguiu buscar da user_global_stats, retornar
            if (statsResult >= 0) {
                return statsResult
            }
            
            // Fallback: contar registros manualmente (sem usar count(*))
            when {
                requirementType == "collect_pokemon" -> {
                    db.from("user_pokemons")
                        .select(columns = Columns.list("id")) {
                            filter { eq("user_id", userId) }
                        }
                        .decodeList<UuidIdOnly>()
                        .size
                }
                requirementType == "favorite_count" -> {
                    db.from("favorites")
                        .select(columns = Columns.list("id")) {
                            filter { eq("user_id", userId) }
                        }
                        .decodeList<UuidIdOnly>()
                        .size
                }
                requirementType == "create_team" -> {
                    db.from("teams")
                        .select(columns = Columns.list("id")) {
                            filter { eq("user_id", userId) }
                        }
                        .decodeList<UuidIdOnly>()
                        .size
                }
                requirementType == "shop_buy" -> {
                    db.from("inventory")
                        .select(columns = Columns.list("id")) {
                            filter { eq("user_id", userId) }
                        }
                        .decodeList<UuidIdOnly>()
                        .size
                }
                requirementType == "own_skins" -> {
                    db.from("inventory")
                        .select(columns = Columns.list("id")) {
                            filter { 
                                eq("user_id", userId) 
                                eq("category", "skin")
                            }
                        }
                        .decodeList<UuidIdOnly>()
                        .size
                }
                requirementType == "own_badges" -> {
                    db.from("inventory")
                        .select(columns = Columns.list("id")) {
                            filter { 
                                eq("user_id", userId)
                                eq("category", "badge")
                            }
                        }
                        .decodeList<UuidIdOnly>()
                        .size
                }
                requirementType == "own_themes" -> {
                    db.from("inventory")
                        .select(columns = Columns.list("id")) {
                            filter { 
                                eq("user_id", userId)
                                eq("category", "theme")
                            }
                        }
                        .decodeList<UuidIdOnly>()
                        .size
                }
                requirementType == "own_name_colors" -> {
                    db.from("inventory")
                        .select(columns = Columns.list("id")) {
                            filter { 
                                eq("user_id", userId)
                                eq("category", "name_color")
                            }
                        }
                        .decodeList<UuidIdOnly>()
                        .size
                }
                requirementType == "own_titles" -> {
                    db.from("inventory")
                        .select(columns = Columns.list("id")) {
                            filter { 
                                eq("user_id", userId)
                                eq("category", "title")
                            }
                        }
                        .decodeList<UuidIdOnly>()
                        .size
                }
                else -> 0
            }
        } catch (e: Exception) {
            Log.e("MissionRepository", "Error getting global progress for $requirementType: ${e.message}")
            0
        }
    }
    
    /**
     * Buscar o ID da próxima missão na cadeia
     */
    private suspend fun getNextMissionId(missionId: Int): Int? {
        return try {
            val nextMissions = db.from("missions")
                .select() {
                    filter {
                        eq("prerequisite_mission_id", missionId)
                    }
                }
                .decodeList<Mission>()
            
            nextMissions.firstOrNull()?.id
        } catch (e: Exception) {
            Log.e("MissionRepository", "Error getting next mission id: ${e.message}")
            null
        }
    }
    
    /**
     * Atualizar progresso de missões por tipo de Pokémon adicionado.
     * Chamado quando um Pokémon é adicionado à Living Dex.
     * Também atualiza missões de geração baseado no pokemonId.
     * 
     * NOTA: Esta função chama uma RPC no servidor para atualizar os stats globais
     * e as missões de tipo de forma atômica.
     */
    suspend fun updateProgressForPokemonTypes(userId: String, pokemonTypes: List<String>, pokemonId: Int = 0) {
        try {
            Log.d("MissionRepository", "Updating progress for types: $pokemonTypes, pokemonId: $pokemonId")
            
            // Tentar usar a RPC do servidor para atualizar tipos (mais confiável)
            if (pokemonTypes.isNotEmpty()) {
                try {
                    db.rpc("update_type_progress", buildJsonObject {
                        put("p_user_id", userId)
                        // Converter lista para array JSON
                        put("p_pokemon_types", kotlinx.serialization.json.JsonArray(
                            pokemonTypes.map { kotlinx.serialization.json.JsonPrimitive(it.lowercase()) }
                        ))
                    })
                    Log.d("MissionRepository", "Type progress updated via RPC for types: $pokemonTypes")
                } catch (e: Exception) {
                    Log.w("MissionRepository", "update_type_progress RPC failed, using fallback: ${e.message}")
                    // Fallback: atualizar localmente
                    pokemonTypes.forEach { type ->
                        val requirementType = "collect_type_${type.lowercase()}"
                        updateProgressByRequirementType(userId, requirementType, 1)
                    }
                }
            }
            
            // Se temos o pokemonId, atualizar missões de geração
            // NOTA: A geração já é atualizada pelo trigger do servidor (on_pokemon_added_v2)
            // mas fazemos aqui também como backup
            if (pokemonId > 0) {
                val generation = getGenerationFromPokedexId(pokemonId)
                if (generation > 0) {
                    val genRequirementType = "collect_gen_$generation"
                    updateProgressByRequirementType(userId, genRequirementType, 1)
                    Log.d("MissionRepository", "Updated generation $generation progress for pokemon $pokemonId")
                }
            }
        } catch (e: Exception) {
            Log.e("MissionRepository", "Failed to update type progress: ${e.message}")
        }
    }
    
    /**
     * Determinar a geração de um Pokémon baseado no seu ID da Pokédex
     */
    private fun getGenerationFromPokedexId(pokedexId: Int): Int {
        return when (pokedexId) {
            in 1..151 -> 1
            in 152..251 -> 2
            in 252..386 -> 3
            in 387..493 -> 4
            in 494..649 -> 5
            in 650..721 -> 6
            in 722..809 -> 7
            in 810..905 -> 8
            in 906..1025 -> 9
            else -> 0
        }
    }
    
    /**
     * Atualizar progresso de missões por tipo de requisito.
     * Esta função é usada para missões que não são tracked automaticamente pelo banco de dados.
     */
    suspend fun updateProgressByRequirementType(userId: String, requirementType: String, increment: Int = 1) {
        try {
            Log.d("MissionRepository", "Updating progress for requirement: $requirementType by $increment")
            
            // Buscar todas as missões ativas com este tipo de requisito
            val allMissions = db.from("missions")
                .select() {
                    filter {
                        eq("requirement_type", requirementType)
                    }
                }
                .decodeList<Mission>()
            
            val allProgress = db.from("missions_progress")
                .select() {
                    filter {
                        eq("user_id", userId)
                        eq("status", "active")
                    }
                }
                .decodeList<MissionProgress>()
            
            val progressMap = allProgress.associateBy { it.missionId }
            
            // Atualizar cada missão ativa deste tipo
            allMissions.forEach { mission ->
                val progress = progressMap[mission.id]
                if (progress != null && progress.status == "active") {
                    val newValue = minOf(progress.currentValue + increment, mission.requirementValue)
                    
                    db.from("missions_progress")
                        .update({
                            set("current_value", newValue)
                            set("updated_at", "now()")
                        }) {
                            filter {
                                eq("user_id", userId)
                                eq("mission_id", mission.id)
                            }
                        }
                    
                    Log.d("MissionRepository", "Updated ${mission.title}: $newValue/${mission.requirementValue}")
                }
            }
        } catch (e: Exception) {
            Log.e("MissionRepository", "Failed to update progress: ${e.message}")
        }
    }

    // Atualizar progresso de uma missão manualmente (se necessário)
    suspend fun updateMissionProgress(
        userId: String,
        missionId: Int,
        currentValue: Int
    ): Resource<String> {
        return try {
            UmbraSupabase.client.from("missions_progress")
                .update({
                    set("current_value", currentValue)
                    set("updated_at", "now()")
                }) {
                    filter {
                        eq("user_id", userId)
                        eq("mission_id", missionId)
                    }
                }

            Resource.Success("Progress updated!")
        } catch (e: Exception) {
            Resource.Error("Failed to update progress: ${e.message}", e)
        }
    }

    // Inicializar missões para um novo usuário
    suspend fun initializeMissionsForUser(userId: String): Resource<String> {
        return try {
            // Buscar todas as missões
            val allMissions = UmbraSupabase.client.from("missions")
                .select()
                .decodeList<Mission>()

            // Filtrar apenas as que não têm pré-requisito (primeiras da cadeia)
            val initialMissions = allMissions.filter { it.prerequisiteMissionId == null }

            // Criar progresso inicial para cada missão
            initialMissions.forEach { mission ->
                UmbraSupabase.client.from("missions_progress")
                    .insert(
                        mapOf(
                            "user_id" to userId,
                            "mission_id" to mission.id,
                            "current_value" to 0,
                            "status" to "active"
                        )
                    )
            }

            Resource.Success("Missions initialized!")
        } catch (e: Exception) {
            Resource.Error("Failed to initialize missions: ${e.message}", e)
        }
    }
    
    /**
     * Verificar e inicializar missões raiz em falta.
     * Esta função também sincroniza o progresso com os valores globais reais.
     */
    suspend fun ensureRootMissionsInitialized(userId: String) {
        try {
            // Primeiro tentar sincronizar usando a RPC do servidor (se disponível)
            try {
                db.rpc("sync_user_missions", buildJsonObject {
                    put("p_user_id", userId)
                })
                Log.d("MissionRepository", "Synced missions via server RPC")
            } catch (e: Exception) {
                Log.w("MissionRepository", "sync_user_missions RPC not available, using local sync: ${e.message}")
            }
            
            // Buscar todas as missões
            val allMissions = UmbraSupabase.client.from("missions")
                .select()
                .decodeList<Mission>()

            // Buscar progresso existente
            val existingProgress = UmbraSupabase.client.from("missions_progress")
                .select() {
                    filter { eq("user_id", userId) }
                }
                .decodeList<MissionProgress>()
            
            val existingMissionIds = existingProgress.map { it.missionId }.toSet()
            val completedMissionIds = existingProgress
                .filter { it.status == "completed" }
                .map { it.missionId }
                .toSet()
            
            // Encontrar missões que estão locked mas cujo pré-requisito foi completado
            val lockedButShouldBeActive = existingProgress.filter { progress ->
                progress.status == "locked" && allMissions.any { mission ->
                    mission.id == progress.missionId && 
                    mission.prerequisiteMissionId in completedMissionIds
                }
            }
            
            // Atualizar missões locked para active COM progresso global
            if (lockedButShouldBeActive.isNotEmpty()) {
                Log.d("MissionRepository", "Unlocking ${lockedButShouldBeActive.size} missions that should be active")
                for (progress in lockedButShouldBeActive) {
                    try {
                        val mission = allMissions.find { it.id == progress.missionId }
                        if (mission != null) {
                            val globalValue = getGlobalProgressForRequirement(userId, mission.requirementType)
                            val initialValue = minOf(globalValue, mission.requirementValue)
                            
                            UmbraSupabase.client.from("missions_progress")
                                .update({
                                    set("status", "active")
                                    set("current_value", initialValue)
                                    set("updated_at", "now()")
                                }) {
                                    filter {
                                        eq("user_id", userId)
                                        eq("mission_id", progress.missionId)
                                    }
                                }
                            Log.d("MissionRepository", "Unlocked mission id: ${progress.missionId} with progress $initialValue")
                        }
                    } catch (e: Exception) {
                        Log.w("MissionRepository", "Could not unlock mission ${progress.missionId}: ${e.message}")
                    }
                }
            }

            // Encontrar missões raiz (sem pré-requisito) que não têm progresso
            val rootMissionsWithoutProgress = allMissions.filter { mission ->
                mission.prerequisiteMissionId == null && mission.id !in existingMissionIds
            }
            
            // Encontrar missões cujo pré-requisito foi completado mas ainda não têm progresso
            val unlockedMissionsWithoutProgress = allMissions.filter { mission ->
                mission.prerequisiteMissionId != null && 
                mission.prerequisiteMissionId in completedMissionIds &&
                mission.id !in existingMissionIds
            }
            
            val missionsToCreate = rootMissionsWithoutProgress + unlockedMissionsWithoutProgress
            
            if (missionsToCreate.isNotEmpty()) {
                Log.d("MissionRepository", "Initializing ${missionsToCreate.size} missing mission progress entries")
                
                for (mission in missionsToCreate) {
                    try {
                        val globalValue = getGlobalProgressForRequirement(userId, mission.requirementType)
                        val initialValue = minOf(globalValue, mission.requirementValue)
                        
                        UmbraSupabase.client.from("missions_progress")
                            .insert(buildJsonObject {
                                put("user_id", userId)
                                put("mission_id", mission.id)
                                put("current_value", initialValue)
                                put("status", "active")
                            })
                        Log.d("MissionRepository", "Initialized progress for mission: ${mission.title} with value $initialValue")
                    } catch (e: Exception) {
                        Log.w("MissionRepository", "Could not create progress for ${mission.title}: ${e.message}")
                    }
                }
            }
            
            // Atualizar progresso de missões ativas com os valores globais reais
            val activeMissions = existingProgress.filter { it.status == "active" }
            for (progress in activeMissions) {
                try {
                    val mission = allMissions.find { it.id == progress.missionId }
                    if (mission != null) {
                        val globalValue = getGlobalProgressForRequirement(userId, mission.requirementType)
                        val correctValue = minOf(globalValue, mission.requirementValue)
                        
                        // Só atualizar se o valor mudou
                        if (correctValue != progress.currentValue) {
                            UmbraSupabase.client.from("missions_progress")
                                .update({
                                    set("current_value", correctValue)
                                    set("updated_at", "now()")
                                }) {
                                    filter {
                                        eq("user_id", userId)
                                        eq("mission_id", progress.missionId)
                                    }
                                }
                            Log.d("MissionRepository", "Synced mission ${mission.title}: ${progress.currentValue} -> $correctValue")
                        }
                    }
                } catch (e: Exception) {
                    Log.w("MissionRepository", "Could not sync mission ${progress.missionId}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("MissionRepository", "Failed to ensure missions initialized: ${e.message}")
        }
    }
}

/**
 * Resultado do claim de uma missão
 */
data class ClaimResult(
    val goldReward: Int,
    val xpReward: Int,
    val nextMissionId: Int? = null
)

// Helper data classes para deserialização
@Serializable
data class IdOnly(val id: Int = 0)

// Helper para contar registros com UUID como id
@Serializable
data class UuidIdOnly(val id: String = "")

@Serializable
data class ProfileLevel(val level: Int = 0)

@Serializable
data class ProfileGold(
    @kotlinx.serialization.SerialName("total_gold_earned")
    val totalGoldEarned: Int = 0
)