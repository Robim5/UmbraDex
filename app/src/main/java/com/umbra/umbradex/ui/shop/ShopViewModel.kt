package com.umbra.umbradex.ui.shop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.umbra.umbradex.data.model.ShopItem
import com.umbra.umbradex.data.model.UserProfile
import com.umbra.umbradex.data.repository.ShopRepository
import com.umbra.umbradex.data.repository.UserRepository
import com.umbra.umbradex.utils.MissionEvent
import com.umbra.umbradex.utils.PokemonDataEvents
import com.umbra.umbradex.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow

data class ShopUiState(
    val items: List<ShopItem> = emptyList(),
    val ownedItems: List<String> = emptyList(),
    val userGold: Int = 0,
    val userLevel: Int = 1,
    val equippedSkin: String = "",
    val equippedTheme: String = "",
    val equippedBadge: String = "",
    val equippedTitle: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedCategory: String? = null,
    val selectedRarity: String? = null,
    val purchaseSuccess: String? = null
)

class ShopViewModel : ViewModel() {
    private val shopRepository = ShopRepository()
    private val userRepository = UserRepository()

    private val _uiState = MutableStateFlow(ShopUiState())
    val uiState: StateFlow<ShopUiState> = _uiState.asStateFlow()

    init {
        loadShopData()
        observeDataEvents()
    }
    
    /**
     * Observa eventos de alteração de dados para atualizar a loja em tempo real
     */
    private fun observeDataEvents() {
        // Observar quando missões são resgatadas (XP/Gold/Level podem mudar)
        viewModelScope.launch {
            PokemonDataEvents.missionProgressChanged.collect { event ->
                when (event) {
                    is MissionEvent.Claimed -> {
                        // Recarregar dados quando recompensa é resgatada
                        refreshUserData()
                    }
                    else -> {}
                }
            }
        }
        
        // Observar pedidos de refresh completo
        viewModelScope.launch {
            PokemonDataEvents.refreshAll.collect {
                loadShopData()
            }
        }
    }
    
    /**
     * Recarrega apenas os dados do utilizador (gold, level, etc.)
     * Mais leve que recarregar toda a loja
     */
    private fun refreshUserData() {
        viewModelScope.launch {
            userRepository.getUserProfile().collect { profileResult ->
                if (profileResult is Resource.Success) {
                    val profile = profileResult.data
                    _uiState.value = _uiState.value.copy(
                        userGold = profile.gold.toInt(),
                        userLevel = profile.level,
                        equippedSkin = profile.equippedSkin,
                        equippedTheme = profile.equippedTheme,
                        equippedBadge = profile.equippedBadge,
                        equippedTitle = profile.equippedTitle
                    )
                }
            }
        }
    }
    
    /**
     * Função pública para recarregar os dados da loja
     * Chamada quando o ecrã é visitado
     */
    fun refresh() {
        loadShopData()
    }

    private fun loadShopData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            // Carregar perfil do user
            userRepository.getUserProfile().collect { profileResult ->
                if (profileResult is Resource.Success) {
                    val profile = profileResult.data

                    // Carregar itens da loja
                    shopRepository.getAvailableItems().collect { itemsResult ->
                        when (itemsResult) {
                            is Resource.Success -> {
                                // Carregar inventário
                                shopRepository.getUserInventory(profile.id).collect { inventoryResult ->
                                    if (inventoryResult is Resource.Success) {
                                        _uiState.value = _uiState.value.copy(
                                            items = itemsResult.data,
                                            ownedItems = inventoryResult.data,
                                            userGold = profile.gold.toInt(),
                                            userLevel = profile.level,
                                            equippedSkin = profile.equippedSkin,
                                            equippedTheme = profile.equippedTheme,
                                            equippedBadge = profile.equippedBadge,
                                            equippedTitle = profile.equippedTitle,
                                            isLoading = false
                                        )
                                    }
                                }
                            }
                            is Resource.Error -> {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = itemsResult.message
                                )
                            }
                            is Resource.Loading -> {
                                _uiState.value = _uiState.value.copy(isLoading = true)
                            }
                        }
                    }
                }
            }
        }
    }

    fun purchaseItem(item: ShopItem, userId: String) {
        viewModelScope.launch {
            val currentGold = _uiState.value.userGold

            when (val result = shopRepository.purchaseItem(userId, item, currentGold)) {
                is Resource.Success -> {
                    val newGold = currentGold - item.price
                    _uiState.value = _uiState.value.copy(
                        purchaseSuccess = result.data,
                        userGold = newGold,
                        ownedItems = _uiState.value.ownedItems + item.name
                    )
                    // Notificar sistema de missões sobre a compra e atualizar gold em tempo real
                    PokemonDataEvents.notifyShopPurchase(item.type, item.price)
                    // Notificar atualização do perfil com o novo gold
                    PokemonDataEvents.notifyProfileUpdated(newGold = newGold.toLong())
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(error = result.message)
                }
                else -> {}
            }
        }
    }

    fun filterByCategory(category: String?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun filterByRarity(rarity: String?) {
        _uiState.value = _uiState.value.copy(selectedRarity = rarity)
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            error = null,
            purchaseSuccess = null
        )
    }
}