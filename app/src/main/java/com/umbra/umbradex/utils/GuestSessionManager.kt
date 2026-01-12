package com.umbra.umbradex.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton que gere o estado de sessão de convidado (guest mode).
 * Quando o utilizador inicia sem conta, pode apenas ver a Pokédex.
 */
object GuestSessionManager {
    
    private val _isGuestMode = MutableStateFlow(false)
    val isGuestMode: StateFlow<Boolean> = _isGuestMode.asStateFlow()
    
    /**
     * Ativa o modo convidado - o utilizador entra sem conta
     */
    fun enableGuestMode() {
        _isGuestMode.value = true
    }
    
    /**
     * Desativa o modo convidado - quando o utilizador faz login/signup
     */
    fun disableGuestMode() {
        _isGuestMode.value = false
    }
    
    /**
     * Verifica se o modo convidado está ativo
     */
    fun isGuest(): Boolean = _isGuestMode.value
    
    /**
     * Limpa o estado (para logout ou reset)
     */
    fun clear() {
        _isGuestMode.value = false
    }
}
