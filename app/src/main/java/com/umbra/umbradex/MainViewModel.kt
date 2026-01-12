package com.umbra.umbradex

// Em: app/src/main/java/com/umbra/umbradex/MainViewModel.kt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.umbra.umbradex.data.repository.InventoryRepository
import com.umbra.umbradex.data.repository.UserRepository
import com.umbra.umbradex.utils.InventoryEvent
import com.umbra.umbradex.utils.PokemonDataEvents
import com.umbra.umbradex.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// A classe MainViewModel herda de ViewModel
class MainViewModel(
    // Instanciamos o repositório aqui. Numa app maior, usaríamos injeção de dependências.
    private val userRepository: UserRepository = UserRepository(),
    private val inventoryRepository: InventoryRepository = InventoryRepository()
) : ViewModel() {

    // StateFlow mutável para controlar as cores do tema
    private val _themeColors = MutableStateFlow<List<String>?>(null)
    val themeColors: StateFlow<List<String>?> = _themeColors.asStateFlow()
    
    // StateFlow para as cores do nome (para o Header)
    private val _nameColors = MutableStateFlow<List<String>>(listOf("#FFFFFF", "#FFFFFF"))
    val nameColors: StateFlow<List<String>> = _nameColors.asStateFlow()

    init {
        loadInitialTheme()
        observeInventoryEvents()
    }
    
    /**
     * Carrega o tema inicial do perfil do utilizador
     */
    private fun loadInitialTheme() {
        viewModelScope.launch {
            userRepository.getUserProfile()
                .catch { e ->
                    // Se houver erro, usa tema padrão
                    _themeColors.value = null
                    _nameColors.value = listOf("#FFFFFF", "#FFFFFF")
                }
                .collect { result ->
                    when (result) {
                        is Resource.Success -> {
                            val profile = result.data
                            
                            // Processar tema
                            val themeStr = profile.equippedTheme
                            _themeColors.value = parseThemeColors(themeStr)
                            
                            // Processar cores do nome
                            _nameColors.value = profile.getNameColors()
                        }
                        is Resource.Error, is Resource.Loading -> {
                            // Manter valores atuais ou padrão
                        }
                    }
                }
        }
    }
    
    /**
     * Observa eventos de mudança no inventário para atualizar tema em tempo real
     */
    private fun observeInventoryEvents() {
        viewModelScope.launch {
            PokemonDataEvents.inventoryChanged.collect { event ->
                when (event) {
                    is InventoryEvent.ThemeEquipped -> {
                        // Atualizar tema com transição suave
                        if (event.themeColors.isNotEmpty()) {
                            _themeColors.value = event.themeColors
                        } else {
                            // Se cores vazias, tentar buscar pelo nome do tema
                            loadThemeColorsByName(event.themeName)
                        }
                    }
                    is InventoryEvent.NameColorEquipped -> {
                        // Atualizar cores do nome
                        val colors = if (event.colors.size < 2) {
                            listOf(event.colors.firstOrNull() ?: "#FFFFFF", event.colors.firstOrNull() ?: "#FFFFFF")
                        } else {
                            event.colors
                        }
                        _nameColors.value = colors
                    }
                    is InventoryEvent.RefreshNeeded -> {
                        // Recarregar tudo do perfil
                        loadInitialTheme()
                    }
                    else -> {
                        // Outros eventos não afetam o tema
                    }
                }
            }
        }
    }
    
    /**
     * Busca as cores do tema pelo nome do item
     */
    private fun loadThemeColorsByName(themeName: String) {
        viewModelScope.launch {
            // Verificar nomes especiais
            if (themeName == "Classic Purple" || themeName == "theme_default") {
                _themeColors.value = null // Usa tema padrão
                return@launch
            }
            
            // Buscar detalhes do tema
            when (val result = inventoryRepository.getItemDetails(themeName)) {
                is Resource.Success -> {
                    val item = result.data
                    if (item.colors != null && item.colors.isNotEmpty()) {
                        _themeColors.value = item.colors
                    } else {
                        // Fallback: recarregar do perfil
                        reloadThemeFromProfile()
                    }
                }
                else -> {
                    // Fallback: recarregar do perfil
                    reloadThemeFromProfile()
                }
            }
        }
    }
    
    /**
     * Recarrega o tema do perfil (para quando as cores não são passadas diretamente)
     */
    private fun reloadThemeFromProfile() {
        viewModelScope.launch {
            userRepository.getUserProfile().collect { result ->
                if (result is Resource.Success) {
                    val themeStr = result.data.equippedTheme
                    _themeColors.value = parseThemeColors(themeStr)
                }
            }
        }
    }
    
    /**
     * Parse das cores do tema a partir da string guardada no perfil
     */
    private fun parseThemeColors(themeStr: String?): List<String>? {
        if (themeStr.isNullOrBlank() || themeStr == "[]" || themeStr == "theme_default" || themeStr == "Classic Purple") {
            return null // Usar tema padrão
        }
        
        return try {
            // Se for um JSON array
            if (themeStr.startsWith("[")) {
                val colors = themeStr.removeSurrounding("[", "]")
                    .replace("\"", "")
                    .split(",")
                    .map { it.trim() }
                    .filter { it.startsWith("#") }
                
                if (colors.size >= 2) colors else null
            } else {
                // Se for só o nome do tema, precisamos buscar as cores
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
