package com.umbra.umbradex.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.umbra.umbradex.data.model.ShopItem
import com.umbra.umbradex.data.model.Title
import com.umbra.umbradex.data.model.UserProfile
import com.umbra.umbradex.data.repository.UserRepository
import com.umbra.umbradex.data.supabase.UmbraSupabase
import com.umbra.umbradex.utils.Resource
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.umbra.umbradex.data.repository.InventoryRepository
import com.umbra.umbradex.data.repository.InventoryItem
import com.umbra.umbradex.data.repository.TitleInventoryItem
import kotlinx.coroutines.flow.StateFlow

data class EquippedItems(
    val skin: ShopItem? = null,
    val theme: ShopItem? = null,
    val badge: ShopItem? = null,
    val nameColor: ShopItem? = null,
    val title: String = "Rookie",
    val titleDetails: Title? = null,
    val partnerPokemonId: Int? = null
)

data class InventoryUiState(
    val inventoryItems: List<InventoryItem> = emptyList(),
    val titleItems: List<TitleInventoryItem> = emptyList(),
    val equippedItems: EquippedItems = EquippedItems(),
    val userLevel: Int = 1,
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedCategory: String = "skin",
    val successMessage: String? = null
)

class InventoryViewModel : ViewModel() {
    private val inventoryRepository = InventoryRepository()
    private val userRepository = UserRepository()

    private val _uiState = MutableStateFlow(InventoryUiState())
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

    fun loadInventory(userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            // Carregar perfil do usuário para saber o que está equipado e o nível
            userRepository.getUserProfile().collect { profileResult ->
                if (profileResult is Resource.Success) {
                    val profile = profileResult.data
                    val userLevel = profile.level

                    // Garantir que o usuário tem a cor padrão no inventário
                    inventoryRepository.ensureDefaultNameColorInInventory(userId)

                    // Carregar detalhes dos itens equipados
                    val equippedSkin = if (profile.equippedSkin.isNotEmpty()) {
                        inventoryRepository.getItemDetails(profile.equippedSkin).let {
                            if (it is Resource.Success) it.data else null
                        }
                    } else null

                    // Para theme, pode ser um nome ou um JSON de cores
                    val equippedTheme = if (profile.equippedTheme.isNotEmpty()) {
                        val themeStr = profile.equippedTheme
                        if (themeStr.startsWith("[")) {
                            // É um JSON de cores - encontrar tema por cores
                            try {
                                val colors = themeStr.removeSurrounding("[", "]")
                                    .replace("\"", "")
                                    .split(",")
                                    .map { it.trim() }
                                    .filter { it.startsWith("#") }
                                inventoryRepository.getThemeByColors(colors)
                            } catch (e: Exception) {
                                null
                            }
                        } else {
                            // É um nome de tema
                            inventoryRepository.getItemDetails(themeStr).let {
                                if (it is Resource.Success) it.data else null
                            }
                        }
                    } else null

                    val equippedBadge = if (profile.equippedBadge.isNotEmpty()) {
                        inventoryRepository.getItemDetails(profile.equippedBadge).let {
                            if (it is Resource.Success) it.data else null
                        }
                    } else null
                    
                    // For name_color, find item whose colors match the equipped colors
                    val equippedColorHexes = profile.getNameColors()
                    var equippedNameColor = inventoryRepository.getItemByColors(equippedColorHexes)
                    
                    // Fallback: Se não encontrar o item de cor e a cor for branca, criar um item placeholder
                    if (equippedNameColor == null && equippedColorHexes.all { it == "#FFFFFF" }) {
                        equippedNameColor = ShopItem(
                            id = 0,
                            type = "name_color",
                            name = "Trainer White",
                            rarity = "common",
                            price = 0,
                            colors = listOf("#FFFFFF"),
                            description = "Classic white"
                        )
                    }
                    
                    // Buscar detalhes do título equipado
                    val equippedTitleDetails = inventoryRepository.getTitleByName(profile.equippedTitle)
                    
                    // Carregar títulos desbloqueados por nível
                    val titleItems = when (val titlesResult = inventoryRepository.getTitlesByLevel(userLevel)) {
                        is Resource.Success -> titlesResult.data
                        else -> emptyList()
                    }

                    // Carregar inventário de itens comprados
                    inventoryRepository.getUserInventory(userId).collect { inventoryResult ->
                        when (inventoryResult) {
                            is Resource.Success -> {
                                // Adicionar item de cor padrão se não existir na lista
                                var items = inventoryResult.data.toMutableList()
                                
                                // Verificar se já tem Trainer White
                                val hasDefaultColor = items.any { 
                                    it.item.type == "name_color" && it.item.name == "Trainer White" 
                                }
                                
                                if (!hasDefaultColor) {
                                    // Criar item padrão Trainer White
                                    val defaultColorItem = ShopItem(
                                        id = 0,
                                        type = "name_color",
                                        name = "Trainer White",
                                        rarity = "common",
                                        price = 0,
                                        colors = listOf("#FFFFFF"),
                                        description = "Classic white"
                                    )
                                    items.add(0, InventoryItem(defaultColorItem, "default"))
                                }
                                
                                _uiState.value = _uiState.value.copy(
                                    inventoryItems = items,
                                    titleItems = titleItems,
                                    userLevel = userLevel,
                                    equippedItems = EquippedItems(
                                        skin = equippedSkin,
                                        theme = equippedTheme,
                                        badge = equippedBadge,
                                        nameColor = equippedNameColor,
                                        title = profile.equippedTitle,
                                        titleDetails = equippedTitleDetails,
                                        partnerPokemonId = profile.equippedPokemonId
                                    ),
                                    isLoading = false
                                )
                            }
                            is Resource.Error -> {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = inventoryResult.message
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

    fun equipItem(userId: String, itemName: String, category: String) {
        viewModelScope.launch {
            when (val result = inventoryRepository.equipItem(userId, itemName, category)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        successMessage = result.data
                    )
                    // Recarregar inventário para atualizar estado
                    loadInventory(userId)
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(error = result.message)
                }
                else -> {}
            }
        }
    }
    
    // Equipar título (usa o mesmo método mas com categoria 'title')
    fun equipTitle(userId: String, titleName: String) {
        equipItem(userId, titleName, "title")
    }

    fun selectCategory(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            error = null,
            successMessage = null
        )
    }
}