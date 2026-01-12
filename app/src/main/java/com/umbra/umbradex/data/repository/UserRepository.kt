package com.umbra.umbradex.data.repository

import android.util.Log
import com.umbra.umbradex.data.model.UserProfile
import com.umbra.umbradex.data.supabase.UmbraSupabase
import com.umbra.umbradex.utils.Resource
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class UserRepository {

    private val db = UmbraSupabase.db
    private val auth = UmbraSupabase.auth

    // Obter perfil do user atual
    fun getUserProfile(): Flow<Resource<UserProfile>> = flow {
        emit(Resource.Loading)
        try {
            val userId = UmbraSupabase.auth.currentUserOrNull()?.id
                ?: throw Exception("User not logged in")

            val profile = db.from("profiles")
                .select(columns = Columns.ALL) {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingle<UserProfile>()

            emit(Resource.Success(profile))
        } catch (e: Exception) {
            emit(Resource.Error("Failed to load profile: ${e.message}"))
        }
    }

    // Atualizar nome de utilizador
    fun updateUsername(newUsername: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading)
        try {
            val userId = auth.currentUserOrNull()?.id
                ?: throw Exception("User not logged in")

            db.from("profiles").update({
                set("username", newUsername)
            }) {
                filter {
                    eq("id", userId)
                }
            }

            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error("Failed to update username: ${e.message}"))
        }
    }

    // Equipar Pokémon como pet
    fun equipPokemon(pokedexId: Int): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading)
        try {
            val userId = auth.currentUserOrNull()?.id
                ?: throw Exception("User not logged in")

            db.from("profiles").update({
                set("equipped_pokemon_id", pokedexId)
            }) {
                filter {
                    eq("id", userId)
                }
            }

            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error("Failed to equip pokemon: ${e.message}"))
        }
    }

    // Incrementar clicks no pet (para achievement)
    fun incrementPetClicks(): Flow<Resource<Boolean>> = flow {
        try {
            val userId = auth.currentUserOrNull()?.id
                ?: throw Exception("User not logged in")

            // Usa RPC para incrementar atomicamente
            db.rpc("increment_pet_clicks", buildJsonObject {
                put("p_user_id", userId)
            })

            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error("Failed to increment clicks: ${e.message}"))
        }
    }

    // Adicionar Gold
    fun addGold(amount: Int): Flow<Resource<Boolean>> = flow {
        try {
            val userId = auth.currentUserOrNull()?.id
                ?: throw Exception("User not logged in")

            db.rpc("add_gold", buildJsonObject {
                put("p_user_id", userId)
                put("p_amount", amount)
            })

            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error("Failed to add gold: ${e.message}"))
        }
    }

    // Subtrair Gold (para compras)
    fun spendGold(amount: Int): Flow<Resource<Boolean>> = flow {
        try {
            val userId = auth.currentUserOrNull()?.id
                ?: throw Exception("User not logged in")

            db.rpc("spend_gold", buildJsonObject {
                put("p_user_id", userId)
                put("p_amount", amount)
            })

            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error("Failed to spend gold: ${e.message}"))
        }
    }

    // Adicionar XP (level up automático via trigger)
    fun addXP(amount: Int): Flow<Resource<Boolean>> = flow {
        try {
            val userId = auth.currentUserOrNull()?.id
                ?: throw Exception("User not logged in")

            db.rpc("add_xp_and_level_up", buildJsonObject {
                put("p_user_id", userId)
                put("p_xp_amount", amount)
            })

            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error("Failed to add XP: ${e.message}"))
        }
    }
    
    // Atualizar email do utilizador
    suspend fun updateEmail(newEmail: String): Resource<Boolean> {
        return try {
            val userId = auth.currentUserOrNull()?.id
                ?: throw Exception("Utilizador não autenticado")
            
            // Validate email format (basic check using Android's built-in validator)
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                return Resource.Error("O email introduzido não é válido. Verifica o formato (ex: exemplo@gmail.com)")
            }
            
            // Update email in Supabase Auth
            UmbraSupabase.client.auth.updateUser {
                email = newEmail
            }
            
            // Also update in profiles table
            db.from("profiles").update({
                set("email", newEmail)
            }) {
                filter {
                    eq("id", userId)
                }
            }
            
            Resource.Success(true)
        } catch (e: Exception) {
            val errorMessage = parseEmailError(e.message ?: "")
            Resource.Error(errorMessage)
        }
    }
    
    // Converter erros de email para mensagens amigáveis
    private fun parseEmailError(error: String): String {
        return when {
            error.contains("email_address_invalid", ignoreCase = true) -> 
                "Não é possível alterar o email porque a conta foi criada com um email inválido. Por favor, cria uma nova conta com um email válido."
            error.contains("email_exists", ignoreCase = true) || error.contains("already registered", ignoreCase = true) -> 
                "Este email já está registado noutra conta"
            error.contains("rate_limit", ignoreCase = true) -> 
                "Demasiadas tentativas. Aguarda um momento e tenta novamente"
            error.contains("network", ignoreCase = true) || error.contains("connection", ignoreCase = true) -> 
                "Erro de ligação. Verifica a tua internet e tenta novamente"
            error.contains("same_email", ignoreCase = true) -> 
                "O novo email é igual ao atual"
            else -> "Não foi possível atualizar o email. Tenta novamente mais tarde"
        }
    }
    
    // Atualizar password do utilizador
    suspend fun updatePassword(newPassword: String): Resource<Boolean> {
        return try {
            // Validate password length
            if (newPassword.length < 6) {
                return Resource.Error("A palavra-passe deve ter pelo menos 6 caracteres")
            }
            
            // Get current user email before updating password
            val currentUser = auth.currentUserOrNull()
            val currentEmail = currentUser?.email
                ?: throw Exception("Utilizador não autenticado")
            
            // Update password in Supabase Auth
            UmbraSupabase.client.auth.updateUser {
                password = newPassword
            }
            
            // CRÍTICO: Re-autenticar com a nova password para atualizar a sessão
            // Isto garante que quando tentar apagar a conta, a password atualizada funciona
            try {
                UmbraSupabase.client.auth.signInWith(Email) {
                    this.email = currentEmail
                    this.password = newPassword
                }
                Log.d("UserRepository", "Password updated and user re-authenticated successfully")
            } catch (reAuthError: Exception) {
                // Se a re-autenticação falhar, logar mas não falhar a operação
                // A password foi atualizada, apenas a sessão pode precisar de refresh manual
                Log.w("UserRepository", "Password updated but re-authentication failed: ${reAuthError.message}")
            }
            
            Resource.Success(true)
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("weak_password", ignoreCase = true) == true -> 
                    "A palavra-passe é demasiado fraca. Usa letras, números e símbolos"
                e.message?.contains("same_password", ignoreCase = true) == true -> 
                    "A nova palavra-passe não pode ser igual à anterior"
                e.message?.contains("rate_limit", ignoreCase = true) == true -> 
                    "Demasiadas tentativas. Aguarda um momento e tenta novamente"
                else -> "Não foi possível atualizar a palavra-passe. Tenta novamente"
            }
            Resource.Error(errorMessage)
        }
    }
    
    // Atualizar username (versão suspend)
    suspend fun updateUsernameSync(newUsername: String): Resource<Boolean> {
        return try {
            val userId = auth.currentUserOrNull()?.id
                ?: throw Exception("Utilizador não autenticado")

            db.from("profiles").update({
                set("username", newUsername)
            }) {
                filter {
                    eq("id", userId)
                }
            }

            Resource.Success(true)
        } catch (e: Exception) {
            Resource.Error("Failed to update username: ${e.message}")
        }
    }
    
    // Verificar password do utilizador (re-autenticação)
    suspend fun verifyPassword(email: String, password: String): Resource<Boolean> {
        return try {
            // Re-authenticate user with email and password
            UmbraSupabase.client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            Resource.Success(true)
        } catch (e: Exception) {
            Resource.Error("Palavra-passe incorreta")
        }
    }
    
    // Apagar conta do utilizador
    suspend fun deleteAccount(): Resource<Boolean> {
        return try {
            val userId = auth.currentUserOrNull()?.id
                ?: throw Exception("User not logged in")
            
            // Delete user data from profiles table (cascade will handle related data)
            db.from("profiles").delete {
                filter {
                    eq("id", userId)
                }
            }
            
            // Sign out the user
            UmbraSupabase.client.auth.signOut()
            
            Resource.Success(true)
        } catch (e: Exception) {
            Resource.Error("Falha ao apagar conta: ${e.message}")
        }
    }
    
    // Resgatar código promocional
    suspend fun redeemPromoCode(code: String): Resource<PromoCodeReward> {
        return try {
            val userId = auth.currentUserOrNull()?.id
                ?: throw Exception("User not logged in")
            
            // Verify the code (hardcoded for now, but can be extended to check database)
            val upperCode = code.uppercase()
            
            when (upperCode) {
                "BETA123UAU" -> {
                    // Give MASSIVE rewards for beta testers: 10000 gold + 5000 XP
                    val goldReward = 10000
                    val xpReward = 5000
                    
                    db.rpc("add_gold", buildJsonObject {
                        put("p_user_id", userId)
                        put("p_amount", goldReward)
                    })
                    
                    db.rpc("add_xp_and_level_up", buildJsonObject {
                        put("p_user_id", userId)
                        put("p_xp_amount", xpReward)
                    })
                    
                    Resource.Success(PromoCodeReward(gold = goldReward, xp = xpReward, message = "Código BETA123UAU resgatado com sucesso!"))
                }
                else -> {
                    Resource.Error("Código inválido ou já utilizado")
                }
            }
        } catch (e: Exception) {
            Resource.Error("Falha ao resgatar código: ${e.message}")
        }
    }
}

// Data class para recompensas de códigos promocionais
data class PromoCodeReward(
    val gold: Int,
    val xp: Int,
    val message: String
)