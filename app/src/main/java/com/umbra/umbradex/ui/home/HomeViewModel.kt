package com.umbra.umbradex.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.umbra.umbradex.data.model.ShopItem
import com.umbra.umbradex.data.model.UserProfile
import com.umbra.umbradex.data.repository.DataRepository
import com.umbra.umbradex.data.repository.InventoryRepository
import com.umbra.umbradex.data.repository.UserRepository
import com.umbra.umbradex.utils.FavoriteEvent
import com.umbra.umbradex.utils.InventoryEvent
import com.umbra.umbradex.utils.LivingDexEvent
import com.umbra.umbradex.utils.MissionEvent
import com.umbra.umbradex.utils.PokemonDataEvents
import com.umbra.umbradex.utils.ProfileEvent
import com.umbra.umbradex.utils.Resource
import com.umbra.umbradex.utils.SoundManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val userRepository = UserRepository()
    private val dataRepository = DataRepository()
    private val inventoryRepository = InventoryRepository()

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState

    // Estado para tracking de cliques no pet
    private val _petClickCount = MutableStateFlow(0)
    val petClickCount: StateFlow<Int> = _petClickCount
    
    // Track the last known level to detect new titles
    private var lastKnownLevel: Int = 0

    init {
        loadData()
        observeDataEvents()
    }
    
    /**
     * Observa eventos de alteração de dados de outros ViewModels
     * para atualizar a UI em tempo real
     */
    private fun observeDataEvents() {
        // Observar mudanças nos favoritos (novo Partner)
        viewModelScope.launch {
            PokemonDataEvents.favoriteChanged.collect { event ->
                when (event) {
                    is FavoriteEvent.Changed -> {
                        // Recarrega os dados para mostrar o novo Partner
                        loadData()
                    }
                    is FavoriteEvent.Removed -> {
                        // Recarrega os dados para atualizar o Partner atual
                        loadData()
                    }
                }
            }
        }
        
        // Observar mudanças na Living Dex para atualizar estatísticas
        viewModelScope.launch {
            PokemonDataEvents.livingDexChanged.collect { event ->
                // Atualiza as estatísticas quando um Pokémon é adicionado/removido
                updateLivingDexStats(event)
            }
        }
        
        // Observar eventos de missões para atualizar XP/Gold/Level
        viewModelScope.launch {
            PokemonDataEvents.missionProgressChanged.collect { event ->
                when (event) {
                    is MissionEvent.Claimed -> {
                        // Recarregar perfil quando recompensa é resgatada
                        android.util.Log.d("HomeViewModel", "Mission claimed, reloading profile data")
                        loadData()
                    }
                    is MissionEvent.ProgressUpdated -> {
                        // Atualizar contagem de missões completadas
                        refreshMissionStats()
                    }
                }
            }
        }
        
        // Observar pedidos de refresh completo
        viewModelScope.launch {
            PokemonDataEvents.refreshAll.collect {
                loadData()
            }
        }
        
        // Observar mudanças no inventário (skin, badge, title equipados)
        viewModelScope.launch {
            PokemonDataEvents.inventoryChanged.collect { event ->
                val currentState = _uiState.value
                if (currentState is HomeUiState.Success) {
                    when (event) {
                        is InventoryEvent.SkinEquipped -> {
                            val updatedProfile = currentState.profile.copy(equippedSkin = event.skinName)
                            _uiState.value = currentState.copy(profile = updatedProfile)
                        }
                        is InventoryEvent.BadgeEquipped -> {
                            val updatedProfile = currentState.profile.copy(equippedBadge = event.badgeName)
                            // Também recarregar o item do badge para preview
                            loadBadgeDetails(event.badgeName)
                        }
                        is InventoryEvent.TitleEquipped -> {
                            val updatedProfile = currentState.profile.copy(equippedTitle = event.titleName)
                            _uiState.value = currentState.copy(profile = updatedProfile)
                        }
                        is InventoryEvent.RefreshNeeded -> {
                            loadData()
                        }
                        else -> {}
                    }
                }
            }
        }
        
        // Observar atualizações do perfil (gold, level) para atualizar em tempo real
        viewModelScope.launch {
            PokemonDataEvents.profileUpdated.collect { event ->
                val currentState = _uiState.value
                if (currentState is HomeUiState.Success) {
                    when (event) {
                        is ProfileEvent.Updated -> {
                            // Atualizar gold e/ou level
                            val updatedProfile = currentState.profile.copy(
                                gold = event.newGold ?: currentState.profile.gold,
                                level = event.newLevel ?: currentState.profile.level
                            )
                            _uiState.value = currentState.copy(profile = updatedProfile)
                        }
                        is ProfileEvent.GoldChanged -> {
                            // Atualizar gold incrementalmente
                            val newGold = (currentState.profile.gold + event.amount).coerceAtLeast(0)
                            val updatedProfile = currentState.profile.copy(gold = newGold)
                            _uiState.value = currentState.copy(profile = updatedProfile)
                        }
                        is ProfileEvent.LevelUp -> {
                            // Atualizar level
                            val updatedProfile = currentState.profile.copy(level = event.newLevel)
                            _uiState.value = currentState.copy(profile = updatedProfile)
                            
                            // Play title sound if a new title was unlocked
                            if (event.newTitle != null) {
                                SoundManager.playGetTitleSound()
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Carrega os detalhes do badge para preview
     */
    private fun loadBadgeDetails(badgeName: String) {
        viewModelScope.launch {
            val badgeResult = inventoryRepository.getItemDetails(badgeName)
            if (badgeResult is Resource.Success) {
                val currentState = _uiState.value
                if (currentState is HomeUiState.Success) {
                    val updatedProfile = currentState.profile.copy(equippedBadge = badgeName)
                    _uiState.value = currentState.copy(
                        profile = updatedProfile,
                        equippedBadgeItem = badgeResult.data
                    )
                }
            }
        }
    }
    
    /**
     * Atualiza apenas as estatísticas de missões sem recarregar tudo
     */
    private fun refreshMissionStats() {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is HomeUiState.Success) {
                dataRepository.getMissionStats().collect { res ->
                    if (res is Resource.Success) {
                        _uiState.value = currentState.copy(
                            missionsCompleted = (res.data["completed"] as? Number)?.toInt() ?: currentState.missionsCompleted,
                            missionsTotal = (res.data["total"] as? Number)?.toInt() ?: currentState.missionsTotal
                        )
                    }
                }
            }
        }
    }
    
    /**
     * Atualiza as estatísticas da Living Dex sem recarregar tudo
     */
    private fun updateLivingDexStats(event: LivingDexEvent) {
        val currentState = _uiState.value
        if (currentState is HomeUiState.Success) {
            val delta = when (event) {
                is LivingDexEvent.Added -> 1
                is LivingDexEvent.Removed -> -1
            }
            _uiState.value = currentState.copy(
                pokedexCaught = (currentState.pokedexCaught + delta).coerceIn(0, currentState.pokedexTotal)
            )
        }
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading

            try {
                var userProfile: UserProfile? = null
                var livingDexStats: Map<String, Any> = mapOf(
                    "total_caught" to 1,
                    "total_possible" to 1025
                )
                var missionStats: Map<String, Int> = mapOf(
                    "completed" to 0,
                    "total" to 200
                )
                var totalTime: Long = 0L

                // Carregar Perfil
                userRepository.getUserProfile().collect { res ->
                    when (res) {
                        is Resource.Success -> userProfile = res.data
                        is Resource.Error -> {
                            android.util.Log.e("HomeViewModel", "Profile load error: ${res.message}")
                        }
                        else -> {}
                    }
                }

                // Carregar Stats Living Dex
                dataRepository.getLivingDexStats().collect { res ->
                    if (res is Resource.Success) livingDexStats = res.data
                }

                // Carregar Stats Missões
                dataRepository.getMissionStats().collect { res ->
                    if (res is Resource.Success) missionStats = res.data
                }

                // Carregar Tempo Total
                dataRepository.getTotalTimeOnline().collect { res ->
                    if (res is Resource.Success) totalTime = res.data
                }

                if (userProfile != null) {
                    // Load badge details for display
                    var equippedBadgeItem: ShopItem? = null
                    if (userProfile!!.equippedBadge.isNotEmpty()) {
                        val badgeResult = inventoryRepository.getItemDetails(userProfile!!.equippedBadge)
                        if (badgeResult is Resource.Success) {
                            equippedBadgeItem = badgeResult.data
                        }
                    }
                    
                    // Check if user leveled up and got new titles
                    val currentLevel = userProfile!!.level
                    if (lastKnownLevel > 0 && currentLevel > lastKnownLevel) {
                        // User leveled up - check if there are new titles unlocked
                        val titlesResult = inventoryRepository.getTitlesByLevel(currentLevel)
                        if (titlesResult is Resource.Success) {
                            val newTitles = titlesResult.data.filter { 
                                it.isUnlocked && it.title.minLevel > lastKnownLevel && it.title.minLevel <= currentLevel 
                            }
                            if (newTitles.isNotEmpty()) {
                                // Play title sound for new unlocked title(s)
                                SoundManager.playGetTitleSound()
                                android.util.Log.d("HomeViewModel", "New title(s) unlocked: ${newTitles.map { it.title.titleText }}")
                            }
                        }
                    }
                    lastKnownLevel = currentLevel
                    
                    // Use Number type casting to handle both Int and Long from database
                    _uiState.value = HomeUiState.Success(
                        profile = userProfile!!,
                        equippedBadgeItem = equippedBadgeItem,
                        pokedexCaught = (livingDexStats["total_caught"] as? Number)?.toInt() ?: 1,
                        pokedexTotal = (livingDexStats["total_possible"] as? Number)?.toInt() ?: 1025,
                        missionsCompleted = (missionStats["completed"] as? Number)?.toInt() ?: 0,
                        missionsTotal = (missionStats["total"] as? Number)?.toInt() ?: 200,
                        totalTimeSeconds = totalTime
                    )
                } else {
                    _uiState.value = HomeUiState.Error("Não foi possível carregar o perfil. Tenta fazer login novamente.")
                }

            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Load data error", e)
                _uiState.value = HomeUiState.Error(e.message ?: "Erro desconhecido")
            }
        }
    }

    fun onPetClick() {
        _petClickCount.value += 1

        // Incrementa no backend
        viewModelScope.launch {
            userRepository.incrementPetClicks()
        }

        // Reset após 3 cliques (para a rotação)
        if (_petClickCount.value >= 3) {
            viewModelScope.launch {
                kotlinx.coroutines.delay(1000) // Espera a animação
                _petClickCount.value = 0
            }
        }
    }

    fun getRankTitle(completionPercentage: Float): String {
        return when {
            completionPercentage >= 0.85f -> "Imperador Pokémon"
            completionPercentage >= 0.50f -> "Campeão"
            completionPercentage >= 0.25f -> "Gym King"
            completionPercentage >= 0.10f -> "Sonhador"
            else -> "Explorador do Novo Mundo"
        }
    }

    fun getRankColor(completionPercentage: Float): androidx.compose.ui.graphics.Color {
        return when {
            completionPercentage >= 0.85f -> androidx.compose.ui.graphics.Color(0xFFFFD700) // Gold
            completionPercentage >= 0.50f -> androidx.compose.ui.graphics.Color(0xFFC0C0C0) // Silver
            completionPercentage >= 0.25f -> androidx.compose.ui.graphics.Color(0xFFCD7F32) // Bronze
            else -> androidx.compose.ui.graphics.Color.Gray
        }
    }
}

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(
        val profile: UserProfile,
        val equippedBadgeItem: ShopItem? = null,
        val pokedexCaught: Int,
        val pokedexTotal: Int,
        val missionsCompleted: Int,
        val missionsTotal: Int,
        val totalTimeSeconds: Long
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}