package com.umbra.umbradex.ui.missions

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.umbra.umbradex.data.model.Mission
import com.umbra.umbradex.data.model.MissionProgress
import com.umbra.umbradex.data.model.UserProfile
import com.umbra.umbradex.data.repository.ClaimResult
import com.umbra.umbradex.data.repository.MissionRepository
import com.umbra.umbradex.data.repository.UserRepository
import com.umbra.umbradex.data.supabase.UmbraSupabase
import com.umbra.umbradex.utils.MissionEvent
import com.umbra.umbradex.utils.PokemonDataEvents
import com.umbra.umbradex.utils.Resource
import com.umbra.umbradex.utils.TeamEvent
import com.umbra.umbradex.utils.ShopEvent
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow


data class MissionWithProgress(
    val mission: Mission,
    val progress: MissionProgress?,
    val progressPercentage: Float,
    val isCompleted: Boolean,
    val isLocked: Boolean,
    val canClaim: Boolean  // Verdadeiro quando currentValue >= requirementValue e status == "active"
)

data class MissionsUiState(
    val activeMissions: List<MissionWithProgress> = emptyList(),
    val completedMissions: List<MissionWithProgress> = emptyList(),
    val lockedMissions: List<MissionWithProgress> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedCategory: String? = null,
    val successMessage: String? = null,
    val claimingMissionId: Int? = null,  // Para animação de claim
    val userGold: Int = 0,
    val userXp: Int = 0,
    val userLevel: Int = 1,
    val lastClaimedReward: ClaimResult? = null  // Para mostrar animação de recompensa
)

class MissionsViewModel : ViewModel() {
    private val missionRepository = MissionRepository()
    private val userRepository = UserRepository()

    private val _uiState = MutableStateFlow(MissionsUiState())
    val uiState: StateFlow<MissionsUiState> = _uiState.asStateFlow()
    
    private var currentUserId: String? = null

    init {
        // Observar eventos de progresso de missões para atualização em tempo real
        observeMissionEvents()
    }
    
    /**
     * Observa eventos de outros ViewModels para atualizar missões em tempo real
     */
    private fun observeMissionEvents() {
        viewModelScope.launch {
            PokemonDataEvents.missionProgressChanged.collect { event ->
                when (event) {
                    is MissionEvent.ProgressUpdated -> {
                        // Sincronizar e recarregar missões
                        delay(300)
                        currentUserId?.let { userId ->
                            // Primeiro sincroniza com o servidor para garantir valores corretos
                            missionRepository.syncAllMissions(userId)
                            delay(200)
                            refreshMissionsQuietly(userId)
                        }
                    }
                    is MissionEvent.Claimed -> {
                        // Atualizar UI após claim
                        currentUserId?.let { userId ->
                            refreshMissionsQuietly(userId)
                        }
                    }
                }
            }
        }
        
        // Também observar eventos de Living Dex para atualizar tipos de missões
        viewModelScope.launch {
            PokemonDataEvents.livingDexChanged.collect { event ->
                when (event) {
                    is com.umbra.umbradex.utils.LivingDexEvent.Added -> {
                        // Atualizar missões de tipo e geração quando um Pokémon é adicionado
                        currentUserId?.let { userId ->
                            if (event.pokemonTypes.isNotEmpty()) {
                                missionRepository.updateProgressForPokemonTypes(userId, event.pokemonTypes, event.pokemonId)
                            }
                            delay(300)
                            missionRepository.syncAllMissions(userId)
                            refreshMissionsQuietly(userId)
                        }
                    }
                    else -> {}
                }
            }
        }
        
        // Observar eventos de equipas para atualizar missões de create_team
        viewModelScope.launch {
            PokemonDataEvents.teamChanged.collect { event ->
                when (event) {
                    is TeamEvent.Created -> {
                        currentUserId?.let { userId ->
                            delay(300)
                            missionRepository.syncAllMissions(userId)
                            refreshMissionsQuietly(userId)
                        }
                    }
                    else -> {}
                }
            }
        }
        
        // Observar eventos de compras na loja
        viewModelScope.launch {
            PokemonDataEvents.shopPurchase.collect { event ->
                when (event) {
                    is ShopEvent.ItemPurchased -> {
                        currentUserId?.let { userId ->
                            delay(300)
                            missionRepository.syncAllMissions(userId)
                            refreshMissionsQuietly(userId)
                        }
                    }
                }
            }
        }
        
        // Observar eventos de favoritos
        viewModelScope.launch {
            PokemonDataEvents.favoriteChanged.collect { event ->
                when (event) {
                    is com.umbra.umbradex.utils.FavoriteEvent.Changed -> {
                        currentUserId?.let { userId ->
                            delay(300)
                            missionRepository.syncAllMissions(userId)
                            refreshMissionsQuietly(userId)
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    fun loadMissions(userId: String) {
        currentUserId = userId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                // IMPORTANTE: Sincronizar todas as missões com valores reais antes de carregar
                // Isto garante que missões de equipas, favoritos, etc. têm o progresso correto
                missionRepository.syncAllMissions(userId)
                
                // Garantir que todas as missões raiz estão inicializadas
                missionRepository.ensureRootMissionsInitialized(userId)
                
                // Carregar perfil do usuário
                userRepository.getUserProfile().collect { profileResult ->
                    if (profileResult is Resource.Success) {
                        val profile = profileResult.data

                        // Carregar missões
                        missionRepository.getAllMissions().collect { missionsResult ->
                            if (missionsResult is Resource.Success) {
                                // Carregar progresso
                                missionRepository.getUserMissionProgress(userId).collect { progressResult ->
                                    if (progressResult is Resource.Success) {
                                        val allMissions = combineData(
                                            missionsResult.data,
                                            progressResult.data
                                        )
                                        
                                        // Separar missões por estado
                                        val (active, completed, locked) = separateMissions(allMissions)

                                        _uiState.value = _uiState.value.copy(
                                            activeMissions = active,
                                            completedMissions = completed,
                                            lockedMissions = locked,
                                            userGold = profile.gold.toInt(),
                                            userXp = profile.xp.toInt(),
                                            userLevel = profile.level,
                                            isLoading = false
                                        )
                                    }
                                }
                            } else if (missionsResult is Resource.Error) {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = missionsResult.message
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MissionsViewModel", "Error loading missions", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
    
    /**
     * Refresh silencioso (sem loading state) para atualizações em tempo real
     */
    private suspend fun refreshMissionsQuietly(userId: String) {
        try {
            // Carregar perfil atualizado
            val profile = try {
                UmbraSupabase.db.from("profiles")
                    .select(columns = Columns.ALL) {
                        filter { eq("id", userId) }
                    }
                    .decodeSingle<UserProfile>()
            } catch (e: Exception) {
                null
            }
            
            // Carregar missões
            val missions = UmbraSupabase.db.from("missions")
                .select()
                .decodeList<Mission>()
                .sortedBy { it.sortOrder }
            
            // Carregar progresso
            val progress = UmbraSupabase.db.from("missions_progress")
                .select() {
                    filter { eq("user_id", userId) }
                }
                .decodeList<MissionProgress>()
            
            val allMissions = combineData(missions, progress)
            val (active, completed, locked) = separateMissions(allMissions)
            
            _uiState.value = _uiState.value.copy(
                activeMissions = applyFilter(active),
                completedMissions = applyFilter(completed),
                lockedMissions = applyFilter(locked),
                userGold = profile?.gold?.toInt() ?: _uiState.value.userGold,
                userXp = profile?.xp?.toInt() ?: _uiState.value.userXp,
                userLevel = profile?.level ?: _uiState.value.userLevel
            )
        } catch (e: Exception) {
            Log.e("MissionsViewModel", "Error refreshing missions quietly", e)
        }
    }
    
    /**
     * Aplica o filtro de categoria atual
     */
    private fun applyFilter(missions: List<MissionWithProgress>): List<MissionWithProgress> {
        return if (_uiState.value.selectedCategory == null) {
            missions
        } else {
            missions.filter { it.mission.category == _uiState.value.selectedCategory }
        }
    }
    
    /**
     * Separa missões em ativas, completadas e bloqueadas
     */
    private fun separateMissions(
        allMissions: List<MissionWithProgress>
    ): Triple<List<MissionWithProgress>, List<MissionWithProgress>, List<MissionWithProgress>> {
        val active = allMissions.filter { !it.isCompleted && !it.isLocked }
        val completed = allMissions.filter { it.isCompleted }
        val locked = allMissions.filter { it.isLocked }
        return Triple(active, completed, locked)
    }

    private fun combineData(
        missions: List<Mission>,
        progressList: List<MissionProgress>
    ): List<MissionWithProgress> {
        val progressMap = progressList.associateBy { it.missionId }
        // Criar set de missões completas para verificar pré-requisitos
        val completedMissionIds = progressList
            .filter { it.status == "completed" }
            .map { it.missionId }
            .toSet()

        return missions.map { mission ->
            val progress = progressMap[mission.id]
            val currentValue = progress?.currentValue ?: 0
            
            // Determinar status correto:
            // 1. Se status é "completed" -> manter como completed
            // 2. Se não tem progresso E não tem pré-requisito -> é missão raiz -> ativa
            // 3. Se não tem progresso E tem pré-requisito completo -> ativa
            // 4. Se tem progresso "locked" MAS pré-requisito está completo -> corrigir para ativa
            // 5. Se tem progresso "active" -> manter como active
            // 6. Caso contrário -> locked
            val status = when {
                progress?.status == "completed" -> "completed"
                progress?.status == "active" -> "active"
                // Missão raiz (sem pré-requisito) é sempre ativa
                mission.prerequisiteMissionId == null -> "active"
                // Se o pré-requisito foi completado, a missão deveria ser ativa
                completedMissionIds.contains(mission.prerequisiteMissionId) -> "active"
                // Caso contrário, locked
                else -> "locked"
            }

            // Se requirementValue é 0, a missão está automaticamente completa (100%)
            val progressPercentage = when {
                mission.requirementValue <= 0 -> 1f
                else -> (currentValue.toFloat() / mission.requirementValue.toFloat()).coerceIn(0f, 1f)
            }

            val isCompleted = status == "completed"
            val isLocked = status == "locked"
            
            // canClaim é verdadeiro quando:
            // 1. Status é "active" (não locked ou completed)
            // 2. currentValue >= requirementValue
            val canClaim = status == "active" && currentValue >= mission.requirementValue

            MissionWithProgress(
                mission = mission,
                progress = progress,
                progressPercentage = progressPercentage,
                isCompleted = isCompleted,
                isLocked = isLocked,
                canClaim = canClaim
            )
        }
    }

    fun claimReward(userId: String, missionId: Int) {
        viewModelScope.launch {
            // Marcar que estamos a reclamar esta missão (para animação)
            _uiState.value = _uiState.value.copy(claimingMissionId = missionId)
            
            when (val result = missionRepository.claimMissionReward(userId, missionId)) {
                is Resource.Success -> {
                    val claimResult = result.data
                    
                    _uiState.value = _uiState.value.copy(
                        successMessage = "🎉 +${claimResult.goldReward} Gold, +${claimResult.xpReward} XP!",
                        lastClaimedReward = claimResult,
                        claimingMissionId = null
                    )
                    
                    // Notificar outros componentes sobre a recompensa
                    PokemonDataEvents.notifyMissionClaimed(missionId, claimResult.goldReward, claimResult.xpReward)
                    
                    // Notificar atualização do gold para atualização em tempo real na TopBar e Home
                    PokemonDataEvents.notifyGoldChanged(claimResult.goldReward)
                    
                    // Recarregar missões com animação
                    delay(300)  // Pequeno delay para animação
                    refreshMissionsQuietly(userId)
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = result.message,
                        claimingMissionId = null
                    )
                }
                else -> {}
            }
        }
    }

    fun filterByCategory(category: String?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        // Re-aplicar filtro às missões existentes
        currentUserId?.let { userId ->
            viewModelScope.launch {
                refreshMissionsQuietly(userId)
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            error = null,
            successMessage = null,
            lastClaimedReward = null
        )
    }
}