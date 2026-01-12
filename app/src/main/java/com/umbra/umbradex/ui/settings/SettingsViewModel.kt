package com.umbra.umbradex.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.umbra.umbradex.data.model.UserProfile
import com.umbra.umbradex.data.repository.PromoCodeReward
import com.umbra.umbradex.data.repository.UserRepository
import com.umbra.umbradex.utils.PokemonDataEvents
import com.umbra.umbradex.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val profile: UserProfile? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val accountDeleted: Boolean = false,
    val promoReward: PromoCodeReward? = null
)

class SettingsViewModel : ViewModel() {
    private val userRepository = UserRepository()

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun loadUserProfile(userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            userRepository.getUserProfile().collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            profile = result.data,
                            isLoading = false
                        )
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            error = result.message,
                            isLoading = false
                        )
                    }
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true)
                    }
                }
            }
        }
    }
    
    fun updateUsername(newUsername: String, userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            when (val result = userRepository.updateUsernameSync(newUsername)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Nome atualizado com sucesso!",
                        isLoading = false
                    )
                    // Reload profile to get updated data
                    loadUserProfile(userId)
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = result.message,
                        isLoading = false
                    )
                }
                else -> {}
            }
        }
    }
    
    fun updateEmail(newEmail: String, userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            when (val result = userRepository.updateEmail(newEmail)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Email atualizado! Verifica a tua caixa de correio para confirmar.",
                        isLoading = false
                    )
                    loadUserProfile(userId)
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = result.message,
                        isLoading = false
                    )
                }
                else -> {}
            }
        }
    }
    
    fun updatePassword(newPassword: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            when (val result = userRepository.updatePassword(newPassword)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Palavra-passe atualizada com sucesso!",
                        isLoading = false
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = result.message,
                        isLoading = false
                    )
                }
                else -> {}
            }
        }
    }
    
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            error = null,
            successMessage = null,
            promoReward = null
        )
    }
    
    fun verifyPasswordAndDeleteAccount(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            // First verify password
            when (val verifyResult = userRepository.verifyPassword(email, password)) {
                is Resource.Success -> {
                    // Password verified, now delete account
                    when (val deleteResult = userRepository.deleteAccount()) {
                        is Resource.Success -> {
                            _uiState.value = _uiState.value.copy(
                                successMessage = "Conta apagada com sucesso!",
                                accountDeleted = true,
                                isLoading = false
                            )
                            onSuccess()
                        }
                        is Resource.Error -> {
                            _uiState.value = _uiState.value.copy(
                                error = deleteResult.message,
                                isLoading = false
                            )
                        }
                        else -> {}
                    }
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = verifyResult.message,
                        isLoading = false
                    )
                }
                else -> {}
            }
        }
    }
    
    fun redeemPromoCode(code: String, userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            when (val result = userRepository.redeemPromoCode(code)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        successMessage = result.data.message,
                        promoReward = result.data,
                        isLoading = false
                    )
                    
                    // Emitir evento para atualizar gold em tempo real em toda a app
                    PokemonDataEvents.notifyGoldChanged(result.data.gold)
                    
                    // Solicitar refresh completo para atualizar nível e XP
                    PokemonDataEvents.requestFullRefresh()
                    
                    // Reload profile to get updated gold/xp/level
                    loadUserProfile(userId)
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = result.message,
                        isLoading = false
                    )
                }
                else -> {}
            }
        }
    }
}