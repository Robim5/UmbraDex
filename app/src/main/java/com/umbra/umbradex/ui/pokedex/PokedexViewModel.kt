package com.umbra.umbradex.ui.pokedex

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.umbra.umbradex.data.cache.PokemonCache
import com.umbra.umbradex.data.model.Pokemon
import com.umbra.umbradex.data.repository.PokemonRepository
import com.umbra.umbradex.utils.LivingDexEvent
import com.umbra.umbradex.utils.FavoriteEvent
import com.umbra.umbradex.utils.GuestSessionManager
import com.umbra.umbradex.utils.PokemonDataEvents
import com.umbra.umbradex.utils.Resource
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class PokedexViewModel : ViewModel() {
    private val repository = PokemonRepository()

    // Usa cache global para evitar recarregamentos
    private var allPokemon: List<Pokemon>
        get() = PokemonCache.pokemonList.value
        set(value) = PokemonCache.updateCache(value)

    // Estados da UI
    private val _uiState = MutableStateFlow<PokedexUiState>(PokedexUiState.Loading)
    val uiState = _uiState.asStateFlow()

    // Filtros
    private val _searchText = MutableStateFlow("")
    val searchText = _searchText.asStateFlow()

    private val _selectedType = MutableStateFlow<String?>(null)
    val selectedType = _selectedType.asStateFlow()

    private val _selectedGeneration = MutableStateFlow<Int?>(null)
    val selectedGeneration = _selectedGeneration.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.NUMBER)
    val sortOrder = _sortOrder.asStateFlow()

    private val _showOnlyFavorites = MutableStateFlow(false)
    val showOnlyFavorites = _showOnlyFavorites.asStateFlow()

    private val _showOnlyCaught = MutableStateFlow(false)
    val showOnlyCaught = _showOnlyCaught.asStateFlow()

    // Paginação
    private val _currentPage = MutableStateFlow(1)
    private val itemsPerPage = 50

    init {
        loadPokedex()
        setupFilters()
        observeDataEvents()
        observeGuestMode()
    }
    
    /**
     * Observa mudanças no modo convidado.
     * Quando o utilizador entra como convidado, força reload para limpar favoritos/caught.
     */
    private fun observeGuestMode() {
        viewModelScope.launch {
            GuestSessionManager.isGuestMode.collect { isGuest ->
                if (isGuest) {
                    // CRÍTICO: Forçar reload quando entra em modo convidado
                    // para garantir que NENHUM Pokemon aparece como favorito/caught
                    PokemonCache.clear() // Limpar cache global
                    repository.invalidateCache() // Limpar cache do repositório
                    loadPokedex() // Recarregar dados frescos
                }
            }
        }
    }
    
    /**
     * Observa eventos de alteração de dados de outros ViewModels
     * para atualizar a UI em tempo real
     */
    private fun observeDataEvents() {
        // Observar mudanças na Living Dex
        viewModelScope.launch {
            PokemonDataEvents.livingDexChanged.collect { event ->
                when (event) {
                    is LivingDexEvent.Added -> {
                        updatePokemonCaughtState(event.pokemonId, true)
                    }
                    is LivingDexEvent.Removed -> {
                        updatePokemonCaughtState(event.pokemonId, false)
                    }
                }
            }
        }
        
        // Observar mudanças nos favoritos
        viewModelScope.launch {
            PokemonDataEvents.favoriteChanged.collect { event ->
                when (event) {
                    is FavoriteEvent.Changed -> {
                        // Marca o novo favorito (múltiplos favoritos permitidos)
                        updateAllFavoritesState(event.pokemonId)
                    }
                    is FavoriteEvent.Removed -> {
                        // Remove o favorito da lista local
                        updatePokemonFavoriteState(event.pokemonId, false)
                    }
                }
            }
        }
        
        // Observar pedidos de refresh completo
        viewModelScope.launch {
            PokemonDataEvents.refreshAll.collect {
                // CRÍTICO: Quando há um refresh completo (ex: login/signup/logout),
                // limpar cache e forçar recarregamento com dados frescos
                PokemonCache.clear()
                repository.invalidateCache()
                loadPokedex()
            }
        }
    }
    
    /**
     * Atualiza o estado de capturado de um Pokémon localmente
     */
    private fun updatePokemonCaughtState(pokemonId: Int, isCaught: Boolean) {
        PokemonCache.updateCaughtState(pokemonId, isCaught)
        applyFilters(getCurrentFilterState())
    }
    
    /**
     * Atualiza o estado de favorito - agora suporta múltiplos favoritos
     * Apenas marca o novo favorito como true, não remove os anteriores
     */
    private fun updateAllFavoritesState(newFavoriteId: Int) {
        PokemonCache.updateFavoriteState(newFavoriteId, true)
        applyFilters(getCurrentFilterState())
    }

    private fun setupFilters() {
        viewModelScope.launch {
            combine(
                searchText,
                selectedType,
                selectedGeneration,
                sortOrder,
                showOnlyFavorites,
                showOnlyCaught
            ) { flows: Array<*> ->
                FilterState(
                    searchText = flows[0] as String,
                    selectedType = flows[1] as String?,
                    selectedGeneration = flows[2] as Int?,
                    sortOrder = flows[3] as SortOrder,
                    showOnlyFavorites = flows[4] as Boolean,
                    showOnlyCaught = flows[5] as Boolean
                )
            }
                .debounce(300)
                .collect { filterState ->
                    applyFilters(filterState)
                }
        }
    }

    fun loadPokedex() {
        viewModelScope.launch {
            // Se estamos em modo convidado, garantir que não usamos cache com dados de utilizador
            val isGuest = GuestSessionManager.isGuest()
            
            // Verificar se o cache global tem dados válidos
            // Em modo convidado, sempre recarregar para garantir dados limpos
            if (!isGuest && PokemonCache.isCacheValid()) {
                // Usar dados do cache imediatamente
                applyFilters(getCurrentFilterState())
                return@launch
            }
            
            // Se há dados em cache (mesmo expirados), mostrar enquanto carrega
            // Mas em modo convidado, não usar cache antigo
            if (!isGuest && PokemonCache.hasData()) {
                applyFilters(getCurrentFilterState())
            } else {
                _uiState.value = PokedexUiState.Loading
            }

            // Carrega 1025 Pokémon (Gen 1-9)
            repository.getAllPokemon(limit = 1025).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        PokemonCache.updateCache(result.data)
                        applyFilters(getCurrentFilterState())
                    }
                    is Resource.Error -> {
                        // Só mostrar erro se não há dados em cache
                        if (!PokemonCache.hasData()) {
                            _uiState.value = PokedexUiState.Error(result.message)
                        }
                    }
                    is Resource.Loading -> {
                        if (!PokemonCache.hasData()) {
                            _uiState.value = PokedexUiState.Loading
                        }
                    }
                }
            }
        }
    }

    private fun applyFilters(filterState: FilterState) {
        if (allPokemon.isEmpty()) return

        var filtered = allPokemon

        // Filtro de pesquisa
        if (filterState.searchText.isNotBlank()) {
            filtered = filtered.filter {
                it.name.contains(filterState.searchText, ignoreCase = true) ||
                        it.id.toString() == filterState.searchText ||
                        it.formattedId().contains(filterState.searchText)
            }
        }

        // Filtro de tipo
        filterState.selectedType?.let { type ->
            filtered = filtered.filter { pokemon ->
                pokemon.types.any { it.equals(type, ignoreCase = true) }
            }
        }

        // Filtro de geração
        filterState.selectedGeneration?.let { gen ->
            val range = getGenerationRange(gen)
            filtered = filtered.filter { it.id in range }
        }

        // Filtro de favoritos
        if (filterState.showOnlyFavorites) {
            filtered = filtered.filter { it.isFavorite }
        }

        // Filtro de capturados
        if (filterState.showOnlyCaught) {
            filtered = filtered.filter { it.isCaught }
        }

        // Ordenação
        filtered = when (filterState.sortOrder) {
            SortOrder.NUMBER -> filtered.sortedBy { it.id }
            SortOrder.NAME_ASC -> filtered.sortedBy { it.name }
            SortOrder.NAME_DESC -> filtered.sortedByDescending { it.name }
            SortOrder.TYPE -> filtered.sortedBy { it.types.firstOrNull() ?: "" }
        }

        _uiState.value = PokedexUiState.Success(
            pokemonList = filtered,
            totalCount = allPokemon.size,
            filteredCount = filtered.size
        )
    }

    private fun getGenerationRange(gen: Int): IntRange {
        return when (gen) {
            1 -> 1..151
            2 -> 152..251
            3 -> 252..386
            4 -> 387..493
            5 -> 494..649
            6 -> 650..721
            7 -> 722..809
            8 -> 810..905
            9 -> 906..1025
            else -> 1..1025
        }
    }

    private fun getCurrentFilterState(): FilterState {
        return FilterState(
            searchText = _searchText.value,
            selectedType = _selectedType.value,
            selectedGeneration = _selectedGeneration.value,
            sortOrder = _sortOrder.value,
            showOnlyFavorites = _showOnlyFavorites.value,
            showOnlyCaught = _showOnlyCaught.value
        )
    }

    // Funções públicas para atualizar filtros
    fun onSearchTextChange(text: String) {
        _searchText.value = text
    }

    fun setTypeFilter(type: String?) {
        _selectedType.value = type
    }

    fun setGenerationFilter(gen: Int?) {
        _selectedGeneration.value = gen
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun toggleFavoritesOnly() {
        _showOnlyFavorites.value = !_showOnlyFavorites.value
    }

    fun toggleCaughtOnly() {
        _showOnlyCaught.value = !_showOnlyCaught.value
    }

    fun clearAllFilters() {
        _searchText.value = ""
        _selectedType.value = null
        _selectedGeneration.value = null
        _sortOrder.value = SortOrder.NUMBER
        _showOnlyFavorites.value = false
        _showOnlyCaught.value = false
    }

    // Toggle favorito - coleta o Flow para garantir que a operação é executada
    fun toggleFavorite(pokemonId: Int) {
        viewModelScope.launch {
            val pokemon = allPokemon.find { it.id == pokemonId } ?: return@launch

            try {
                if (pokemon.isFavorite) {
                    repository.removeFavorite(pokemonId).collect { result ->
                        when (result) {
                            is Resource.Success -> {
                                // Atualiza localmente para UI responsiva
                                updatePokemonFavoriteState(pokemonId, false)
                                // Notifica outros ViewModels sobre a remoção
                                PokemonDataEvents.notifyFavoriteRemoved(pokemonId)
                            }
                            is Resource.Error -> {
                                android.util.Log.e("PokedexViewModel", "Failed to remove favorite: ${result.message}")
                            }
                            else -> {}
                        }
                    }
                } else {
                    repository.addFavorite(pokemonId).collect { result ->
                        when (result) {
                            is Resource.Success -> {
                                updateAllFavoritesState(pokemonId)
                                // Notifica outros ViewModels sobre a mudança
                                PokemonDataEvents.notifyFavoriteChanged(pokemonId)
                            }
                            is Resource.Error -> {
                                android.util.Log.e("PokedexViewModel", "Failed to add favorite: ${result.message}")
                            }
                            else -> {}
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("PokedexViewModel", "Toggle favorite error", e)
            }
        }
    }
    
    // Atualiza o estado de favorito localmente sem recarregar tudo
    private fun updatePokemonFavoriteState(pokemonId: Int, isFavorite: Boolean) {
        PokemonCache.updateFavoriteState(pokemonId, isFavorite)
        applyFilters(getCurrentFilterState())
    }
}

data class FilterState(
    val searchText: String,
    val selectedType: String?,
    val selectedGeneration: Int?,
    val sortOrder: SortOrder,
    val showOnlyFavorites: Boolean,
    val showOnlyCaught: Boolean
)

enum class SortOrder {
    NUMBER,
    NAME_ASC,
    NAME_DESC,
    TYPE
}

sealed class PokedexUiState {
    object Loading : PokedexUiState()
    data class Success(
        val pokemonList: List<Pokemon>,
        val totalCount: Int,
        val filteredCount: Int
    ) : PokedexUiState()
    data class Error(val message: String) : PokedexUiState()
}