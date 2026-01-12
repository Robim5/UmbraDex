package com.umbra.umbradex.data.cache

import com.umbra.umbradex.data.model.Pokemon
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton cache global para dados de Pokémon.
 * Permite que todos os ViewModels compartilhem os mesmos dados em memória,
 * evitando carregamentos duplicados e melhorando a performance.
 */
object PokemonCache {
    
    // Lista completa de Pokémon em cache
    private val _pokemonList = MutableStateFlow<List<Pokemon>>(emptyList())
    val pokemonList: StateFlow<List<Pokemon>> = _pokemonList.asStateFlow()
    
    // Timestamp do último carregamento
    private var lastLoadTime: Long = 0L
    
    // Tempo de validade do cache (10 minutos)
    private const val CACHE_VALIDITY_MS = 10 * 60 * 1000L
    
    // Estado de carregamento
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Flag para indicar se o cache foi inicializado
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()
    
    /**
     * Verifica se o cache está válido
     */
    fun isCacheValid(): Boolean {
        return _pokemonList.value.isNotEmpty() && 
               System.currentTimeMillis() - lastLoadTime < CACHE_VALIDITY_MS
    }
    
    /**
     * Verifica se há dados em cache (mesmo que expirados)
     */
    fun hasData(): Boolean = _pokemonList.value.isNotEmpty()
    
    /**
     * Atualiza a lista de Pokémon em cache
     */
    fun updateCache(pokemon: List<Pokemon>) {
        _pokemonList.value = pokemon
        lastLoadTime = System.currentTimeMillis()
        _isInitialized.value = true
    }
    
    /**
     * Atualiza o estado de captura de um Pokémon específico
     */
    fun updateCaughtState(pokemonId: Int, isCaught: Boolean) {
        _pokemonList.value = _pokemonList.value.map { 
            if (it.id == pokemonId) it.copy(isCaught = isCaught) else it 
        }
    }
    
    /**
     * Atualiza o estado de favorito de um Pokémon específico
     */
    fun updateFavoriteState(pokemonId: Int, isFavorite: Boolean) {
        _pokemonList.value = _pokemonList.value.map { 
            if (it.id == pokemonId) it.copy(isFavorite = isFavorite) else it 
        }
    }
    
    /**
     * Atualiza os estados de caught/favorite em batch
     */
    fun updateStates(caughtIds: Set<Int>, favoriteIds: Set<Int>) {
        _pokemonList.value = _pokemonList.value.map { pokemon ->
            pokemon.copy(
                isCaught = caughtIds.contains(pokemon.id),
                isFavorite = favoriteIds.contains(pokemon.id)
            )
        }
    }
    
    /**
     * Define o estado de carregamento
     */
    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }
    
    /**
     * Invalida o cache forçando recarregamento
     */
    fun invalidate() {
        lastLoadTime = 0L
    }
    
    /**
     * Limpa completamente o cache
     */
    fun clear() {
        _pokemonList.value = emptyList()
        lastLoadTime = 0L
        _isInitialized.value = false
    }
    
    /**
     * Obtém um Pokémon específico do cache
     */
    fun getPokemon(id: Int): Pokemon? {
        return _pokemonList.value.find { it.id == id }
    }
    
    /**
     * Obtém Pokémon por range (para Living Dex boxes)
     */
    fun getPokemonRange(startId: Int, endId: Int): List<Pokemon> {
        return _pokemonList.value.filter { it.id in startId..endId }
    }
}
