package com.umbra.umbradex.ui.pokedex

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.umbra.umbradex.data.model.EvolutionStep
import com.umbra.umbradex.data.model.PokemonDetail
import com.umbra.umbradex.data.model.PokemonStat
import com.umbra.umbradex.data.repository.DataRepository
import com.umbra.umbradex.data.repository.PokemonRepository
import com.umbra.umbradex.utils.PokemonDataEvents
import com.umbra.umbradex.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PokemonDetailViewModel : ViewModel() {
    private val repo = PokemonRepository()

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _showShiny = MutableStateFlow(false)
    val showShiny = _showShiny.asStateFlow()
    
    // Messages for user feedback
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage = _successMessage.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun loadPokemon(id: Int) {
        viewModelScope.launch {
            _uiState.value = DetailUiState.Loading

            repo.getPokemonFullDetails(id).collect { result ->
                _uiState.value = when (result) {
                    is Resource.Success -> DetailUiState.Success(result.data)
                    is Resource.Error -> DetailUiState.Error(result.message)
                    is Resource.Loading -> DetailUiState.Loading
                }
            }
        }
    }

    fun toggleShiny() {
        _showShiny.value = !_showShiny.value
    }

    fun toggleCatch() {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is DetailUiState.Success) {
                val pokemon = currentState.data
                
                // Don't allow uncatching (Living Dex is permanent)
                if (pokemon.isCaught) {
                    _errorMessage.value = "Pokémon já está na Living Dex!"
                    return@launch
                }

                try {
                    Log.d("PokemonDetailVM", "Adding Pokemon ${pokemon.id} to Living Dex")
                    
                    repo.addToLivingDex(pokemon.id).collect { result ->
                        when (result) {
                            is Resource.Success -> {
                                Log.d("PokemonDetailVM", "Successfully added to Living Dex!")
                                _uiState.value = DetailUiState.Success(
                                    pokemon.copy(isCaught = true)
                                )
                                _successMessage.value = "${pokemon.name.replaceFirstChar { it.uppercase() }} adicionado à Living Dex! (+10 XP)"
                                
                                // Notificar outros ViewModels sobre a mudança (incluindo tipos para missões)
                                PokemonDataEvents.notifyLivingDexAdded(pokemon.id, pokemon.types)
                            }
                            is Resource.Error -> {
                                Log.e("PokemonDetailVM", "Add to dex failed: ${result.message}")
                                _errorMessage.value = "Falha ao adicionar: ${result.message}"
                            }
                            else -> {}
                        }
                    }
                } catch (e: Exception) {
                    Log.e("PokemonDetailVM", "Toggle catch error", e)
                    _errorMessage.value = "Erro: ${e.message}"
                }
            }
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is DetailUiState.Success) {
                val pokemon = currentState.data

                try {
                    if (pokemon.isFavorite) {
                        // Remove from favorites
                        Log.d("PokemonDetailVM", "Removing Pokemon ${pokemon.id} from favorites")
                        repo.removeFavorite(pokemon.id).collect { result ->
                            when (result) {
                                is Resource.Success -> {
                                    Log.d("PokemonDetailVM", "Successfully removed from favorites!")
                                    _uiState.value = DetailUiState.Success(
                                        pokemon.copy(isFavorite = false)
                                    )
                                    _successMessage.value = "${pokemon.name.replaceFirstChar { it.uppercase() }} removido dos favoritos"
                                    
                                    // Notificar outros ViewModels sobre a mudança
                                    PokemonDataEvents.notifyFavoriteRemoved(pokemon.id)
                                }
                                is Resource.Error -> {
                                    Log.e("PokemonDetailVM", "Remove favorite failed: ${result.message}")
                                    _errorMessage.value = "Falha ao remover favorito: ${result.message}"
                                }
                                else -> {}
                            }
                        }
                    } else {
                        // Add to favorites and set as equipped partner
                        Log.d("PokemonDetailVM", "Adding Pokemon ${pokemon.id} to favorites and setting as partner")
                        repo.addFavorite(pokemon.id).collect { result ->
                            when (result) {
                                is Resource.Success -> {
                                    Log.d("PokemonDetailVM", "Successfully set as partner!")
                                    _uiState.value = DetailUiState.Success(
                                        pokemon.copy(isFavorite = true)
                                    )
                                    _successMessage.value = "${pokemon.name.replaceFirstChar { it.uppercase() }} é agora o teu Partner!"
                                    
                                    // Notificar outros ViewModels sobre a mudança
                                    PokemonDataEvents.notifyFavoriteChanged(pokemon.id)
                                }
                                is Resource.Error -> {
                                    Log.e("PokemonDetailVM", "Add favorite failed: ${result.message}")
                                    _errorMessage.value = "Falha ao definir partner: ${result.message}"
                                }
                                else -> {}
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("PokemonDetailVM", "Toggle favorite error", e)
                    _errorMessage.value = "Erro: ${e.message}"
                }
            }
        }
    }
    
    fun clearMessages() {
        _successMessage.value = null
        _errorMessage.value = null
    }
}

sealed class DetailUiState {
    object Loading : DetailUiState()
    data class Success(val data: PokemonDetail) : DetailUiState()
    data class Error(val message: String) : DetailUiState()
}