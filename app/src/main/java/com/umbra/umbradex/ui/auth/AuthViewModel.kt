package com.umbra.umbradex.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.umbra.umbradex.data.supabase.UmbraSupabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import com.umbra.umbradex.data.repository.AuthRepository
import com.umbra.umbradex.data.repository.PokemonRepository
import com.umbra.umbradex.utils.Resource
import com.umbra.umbradex.utils.GuestSessionManager
import com.umbra.umbradex.utils.PokemonDataEvents
import com.umbra.umbradex.data.cache.PokemonCache
import kotlinx.coroutines.flow.asStateFlow

class AuthViewModel : ViewModel() {

    private val authRepository = AuthRepository()

    // Estados de autenticação
    private val _authState = MutableStateFlow<Resource<Boolean>>(Resource.Loading)
    val authState: StateFlow<Resource<Boolean>> = _authState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Estados para navegação
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    // Estado de modo convidado
    private val _isGuestMode = MutableStateFlow(false)
    val isGuestMode: StateFlow<Boolean> = _isGuestMode.asStateFlow()

    // Dados do onboarding
    private val _onboardingData = MutableStateFlow(OnboardingData())
    val onboardingData: StateFlow<OnboardingData> = _onboardingData.asStateFlow()

    init {
        checkAuthStatus()
        // Sincroniza com o GuestSessionManager
        _isGuestMode.value = GuestSessionManager.isGuest()
    }

    /**
     * Inicia a aplicação em modo convidado (sem conta)
     * O utilizador só pode aceder à Pokédex
     * NENHUM Pokemon deve aparecer como favorito ou na Living Dex
     */
    fun startAsGuest() {
        viewModelScope.launch {
            // CRÍTICO: Garantir que não existe sessão activa do Supabase
            // para não herdar favoritos/caught do utilizador anterior
            try {
                UmbraSupabase.auth.signOut()
            } catch (_: Exception) {
                // Se falhar, ignorar — vamos forçar limpeza de caches abaixo
            }
            
            // Limpar TODOS os caches ANTES de ativar modo convidado
            // Isto garante que nenhum Pokemon aparece como favorito/caught em modo convidado
            PokemonCache.clear()
            PokemonRepository.clearStaticCache()
            
            GuestSessionManager.enableGuestMode()
            _isGuestMode.value = true
            _isAuthenticated.value = false
            _currentUserId.value = null
            
            // Reset também os dados de onboarding para próxima conta
            _onboardingData.value = OnboardingData()
            
            // Reset estado de autenticação
            _authState.value = Resource.Loading
            
            // CRÍTICO: Notificar todos os ViewModels para recarregar dados limpos
            PokemonDataEvents.requestFullRefresh()
        }
    }

    /**
     * Sai do modo convidado (para criar conta ou fazer login)
     */
    fun exitGuestMode() {
        GuestSessionManager.disableGuestMode()
        _isGuestMode.value = false
    }

    private fun checkAuthStatus() {
        _isAuthenticated.value = authRepository.isUserLoggedIn()
        if (_isAuthenticated.value) {
            // Buscar ID do usuário atual
            viewModelScope.launch {
                val userId = authRepository.getCurrentUserId()
                _currentUserId.value = userId
            }
        }
    }

    // Login
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            
            // IMPORTANTE: Limpar caches ANTES do login para garantir dados frescos
            // Isto previne que dados de sessões anteriores apareçam
            PokemonCache.clear()
            PokemonRepository.clearStaticCache()
            
            // Também sair do modo convidado se estava ativo
            GuestSessionManager.disableGuestMode()
            _isGuestMode.value = false
            
            authRepository.login(email, password).collect { result ->
                _authState.value = result
                _isLoading.value = false

                if (result is Resource.Success) {
                    _isAuthenticated.value = true
                    _currentUserId.value = authRepository.getCurrentUserId()
                    // Reset onboarding data para próxima conta
                    _onboardingData.value = OnboardingData()
                    
                    // CRÍTICO: Notificar todos os ViewModels para recarregar dados com o novo user
                    PokemonDataEvents.requestFullRefresh()
                }
            }
        }
    }

    // Signup (após completar onboarding)
    fun signup() {
        viewModelScope.launch {
            _isLoading.value = true
            val data = _onboardingData.value
            
            Log.d("AuthViewModel", "Signup started with data: email=${data.email}, username=${data.username}, " +
                    "birthDate=${data.birthDate}, pokemonKnowledge=${data.pokemonKnowledge}, " +
                    "favoriteType=${data.favoriteType}, avatar=${data.avatar}, starterId=${data.starterId}")
            
            // Validate required data
            if (data.email.isEmpty() || data.password.isEmpty() || data.username.isEmpty()) {
                Log.e("AuthViewModel", "Signup validation failed: missing required fields")
                _authState.value = Resource.Error("Missing required fields. Please go back and fill in email, password, and username.")
                _isLoading.value = false
                return@launch
            }
            
            // IMPORTANTE: Limpar caches ANTES do signup para garantir dados frescos
            // Isto previne que dados de sessões anteriores contaminem a nova conta
            PokemonCache.clear()
            PokemonRepository.clearStaticCache()
            
            // Sair do modo convidado se estava ativo
            GuestSessionManager.disableGuestMode()
            _isGuestMode.value = false

            authRepository.signup(
                email = data.email,
                password = data.password,
                username = data.username,
                birthDate = data.birthDate,
                pokemonKnowledge = data.pokemonKnowledge,
                favoriteType = data.favoriteType,
                avatar = data.avatar,
                starterId = data.starterId
            ).collect { result ->
                Log.d("AuthViewModel", "Signup result: $result")
                _authState.value = result
                _isLoading.value = false

                if (result is Resource.Success) {
                    _isAuthenticated.value = true
                    _currentUserId.value = authRepository.getCurrentUserId()
                    Log.d("AuthViewModel", "Signup successful, userId: ${_currentUserId.value}")
                    // Reset onboarding data para próxima conta
                    _onboardingData.value = OnboardingData()
                    
                    // CRÍTICO: Notificar todos os ViewModels para recarregar dados com o novo user
                    PokemonDataEvents.requestFullRefresh()
                }
            }
        }
    }

    // Logout
    fun logout() {
        viewModelScope.launch {
            // CRÍTICO: Limpar todos os caches ANTES do logout para evitar dados stale
            // Isto garante que quando entrar como convidado, não há dados antigos
            PokemonCache.clear()
            PokemonRepository.clearStaticCache()
            
            authRepository.logout()
            _isAuthenticated.value = false
            _currentUserId.value = null
            _authState.value = Resource.Loading
            
            // Limpa o modo convidado (não ativa automaticamente - utilizador precisa escolher)
            GuestSessionManager.clear()
            _isGuestMode.value = false
            
            // Reset onboarding data para próxima conta
            _onboardingData.value = OnboardingData()
            
            // CRÍTICO: Notificar todos os ViewModels para limpar dados
            PokemonDataEvents.requestFullRefresh()
        }
    }

    // Atualizar dados do onboarding
    fun updateOnboardingData(update: OnboardingData.() -> OnboardingData) {
        _onboardingData.value = _onboardingData.value.update()
    }

    // Reset do estado
    fun resetAuthState() {
        _authState.value = Resource.Loading
    }
}

// Data class para dados do onboarding
data class OnboardingData(
    val email: String = "",
    val password: String = "",
    val username: String = "",
    val birthDate: String = "",
    val pokemonKnowledge: String = "intermediate",
    val favoriteType: String = "fire",
    val avatar: String = "standard_male1",
    val starterId: Int = 1,
    val currentStep: Int = 0
)