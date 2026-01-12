package com.umbra.umbradex.data.repository

import android.util.Log
import com.umbra.umbradex.data.supabase.UmbraSupabase
import com.umbra.umbradex.utils.Resource
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.exceptions.UnknownRestException
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AuthRepository {
    private val auth = UmbraSupabase.auth
    private val db = UmbraSupabase.db

    companion object {
        private const val TAG = "AuthRepository"
    }

    // Faz login e devolve estado
    suspend fun login(email: String, password: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading) // indica carregamento
        try {
            Log.d(TAG, "Attempting login for: $email") // tenta iniciar sessao para este email
            
            auth.signInWith(Email) { // auth call com email e pass
                this.email = email
                this.password = password
            }
            
            val userId = auth.currentUserOrNull()?.id // id do utilizador se existir
            Log.d(TAG, "Login successful for: $email, userId: $userId")
            emit(Resource.Success(true))
            
        } catch (e: Exception) {
            Log.e(TAG, "Login error: ${e.javaClass.simpleName} - ${e.message}", e)
            val errorMessage = parseAuthError(e)
            emit(Resource.Error(errorMessage))
        }
    }.flowOn(Dispatchers.IO)
    
    // Regista e prepara perfil do utilizador
    suspend fun signup(
        email: String,
        password: String,
        username: String,
        birthDate: String,
        pokemonKnowledge: String,
        favoriteType: String,
        avatar: String,
        starterId: Int
    ): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading) // indica registo em progresso
        
        Log.d(TAG, "=== SIGNUP STARTED ===") // inicio do registo
        Log.d(TAG, "Email: $email, Username: $username, StarterId: $starterId")
        Log.d(TAG, "BirthDate: $birthDate, PokemonKnowledge: $pokemonKnowledge")
        Log.d(TAG, "FavoriteType: $favoriteType, Avatar: $avatar")
        
        try {
            // Step 1: Create user with Supabase Auth
            Log.d(TAG, "Step 1: Creating user with Supabase Auth...") // passo criar conta
            Log.d(TAG, "Email: $email, Password length: ${password.length}")
            
            // Build metadata object with all user data
            val userMetadata = buildJsonObject { // metadados para trigger bd
                put("username", username)
                put("starter_id", starterId)
                put("birth_date", birthDate)
                put("pokemon_knowledge", pokemonKnowledge)
                put("favorite_type", favoriteType)
                put("avatar", avatar)
            }
            
            // Log the exact JSON being sent
            Log.d(TAG, "User metadata JSON: $userMetadata") // mostra metadados enviados
            
            try {
                auth.signUpWith(Email) { // cria conta no supabase
                    this.email = email
                    this.password = password
                    // Pass metadata that the trigger/function expects
                    this.data = userMetadata
                }
                Log.d(TAG, "User created successfully!")
            } catch (signupException: Exception) { // trata erro de registo
                // Log detailed error information
                Log.e(TAG, "Signup call failed!")
                Log.e(TAG, "Exception type: ${signupException.javaClass.name}")
                Log.e(TAG, "Exception message: ${signupException.message}")
                Log.e(TAG, "Exception cause: ${signupException.cause}")
                
                // Try to extract more details from exceptions
                if (signupException is AuthRestException) {
                    Log.e(TAG, "AuthRestException - Error message: ${signupException.message}")
                    Log.e(TAG, "This usually indicates a database trigger/function issue")
                } else if (signupException is UnknownRestException) {
                    try {
                        // Try to get status code using reflection
                        val statusCodeField = signupException.javaClass.superclass?.getDeclaredField("statusCode")
                            ?: signupException.javaClass.getDeclaredField("statusCode")
                        statusCodeField?.isAccessible = true
                        val statusCode = statusCodeField?.get(signupException) as? Int
                        if (statusCode != null) {
                            Log.e(TAG, "HTTP Status Code: $statusCode")
                        }
                        
                        // Try to get response object and extract body from it
                        val responseField = signupException.javaClass.superclass?.getDeclaredField("response")
                            ?: signupException.javaClass.getDeclaredField("response")
                        responseField?.isAccessible = true
                        val response = responseField?.get(signupException)
                        if (response != null) {
                            Log.e(TAG, "Response object: $response")
                            val responseClass = response.javaClass
                            
                            // Try to extract Supabase error code from headers
                            try {
                                val headersField = responseClass.getDeclaredField("headers")
                                headersField.isAccessible = true
                                val headers = headersField.get(response)
                                if (headers != null) {
                                    // Try to get x-sb-error-code header
                                    try {
                                        val getMethod = headers.javaClass.getMethod("get", String::class.java)
                                        val errorCode = getMethod.invoke(headers, "x-sb-error-code") as? String
                                        if (errorCode != null && errorCode.isNotBlank()) {
                                            Log.e(TAG, "Supabase Error Code: $errorCode")
                                        }
                                    } catch (e: Exception) {
                                        // Headers might use different method - try getAll
                                        try {
                                            val getAllMethod = headers.javaClass.getMethod("getAll", String::class.java)
                                            val errorCodes = getAllMethod.invoke(headers, "x-sb-error-code") as? List<*>
                                            errorCodes?.firstOrNull()?.let { code ->
                                                Log.e(TAG, "Supabase Error Code: $code")
                                            }
                                        } catch (e2: Exception) {
                                            // Headers extraction failed
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                // Headers extraction failed
                            }
                            
                            // Try to get body from HttpResponse
                            // Note: bodyAsString is a suspend function, so we can't call it via reflection
                            // Try to access body content through properties or other means
                            try {
                                // Try to get body property directly
                                val bodyField = responseClass.getDeclaredField("body")
                                bodyField.isAccessible = true
                                val bodyObj = bodyField.get(response)
                                if (bodyObj != null) {
                                    Log.e(TAG, "Response body object: $bodyObj")
                                    // Try to get string representation
                                    val bodyStr = bodyObj.toString()
                                    if (bodyStr.isNotBlank() && bodyStr != "null") {
                                        Log.e(TAG, "Response body (from field): $bodyStr")
                                    }
                                }
                            } catch (e: Exception) {
                                // Try alternative: get call property and then body
                                try {
                                    val callField = responseClass.getDeclaredField("call")
                                    callField.isAccessible = true
                                    val call = callField.get(response)
                                    Log.e(TAG, "Response call object: $call")
                                } catch (e2: Exception) {
                                    Log.w(TAG, "Could not extract body from response (this is normal for Ktor responses)")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not extract additional error details: ${e.message}")
                    }
                }
                
                signupException.cause?.let { cause ->
                    Log.e(TAG, "Cause type: ${cause.javaClass.name}")
                    Log.e(TAG, "Cause message: ${cause.message}")
                }
                throw signupException
            }
            
            // Step 2: Sign in to get session
            Log.d(TAG, "Step 2: Signing in to establish session...") // passo inicio sessao
            try {
                auth.signInWith(Email) { // tenta auto login apos registo
                    this.email = email
                    this.password = password
                }
            } catch (signInError: Exception) {
                Log.w(TAG, "Auto sign-in failed (email confirmation may be required): ${signInError.message}")
                // User might need to confirm email first - this is OK
            }
            
            // Step 3: Check if we have a session
            val userId = auth.currentUserOrNull()?.id // verifica sessao apos registo
            Log.d(TAG, "User ID after signup: $userId")
            
            // Note: Profile creation is handled by the database trigger 'handle_new_user'
            // which fires on auth.users INSERT and uses the metadata we passed during signup.
            // We don't need to create the profile manually here - the trigger handles:
            // - Creating the profile in 'profiles' table
            // - Adding initial inventory items (avatar, theme, badge)
            // - Adding the starter Pokémon to 'user_pokemons'
            // - Activating the first 10 missions
            
            if (userId != null) {
                Log.d(TAG, "User created successfully. Profile will be set up by database trigger.") // perfil criado pelo trigger bd
            } else {
                Log.w(TAG, "No user session after signup - email confirmation might be required")
            }
            
            Log.d(TAG, "=== SIGNUP COMPLETED SUCCESSFULLY ===")
            emit(Resource.Success(true)) // emite sucesso de registo
            
        } catch (e: Exception) {
            Log.e(TAG, "========================================")
            Log.e(TAG, "SIGNUP ERROR DETAILS:")
            Log.e(TAG, "Exception class: ${e.javaClass.name}")
            Log.e(TAG, "Exception message: ${e.message}")
            Log.e(TAG, "Exception cause: ${e.cause?.message}")
            Log.e(TAG, "Exception toString: ${e.toString()}")
            
            // Log all declared fields for UnknownRestException
            if (e is UnknownRestException) {
                try {
                    Log.e(TAG, "UnknownRestException fields:")
                    e.javaClass.declaredFields.forEach { field ->
                        try {
                            field.isAccessible = true
                            val value = field.get(e)
                            Log.e(TAG, "  ${field.name}: $value")
                        } catch (ex: Exception) {
                            Log.w(TAG, "  Could not access field ${field.name}: ${ex.message}")
                        }
                    }
                    // Also check superclass fields
                    e.javaClass.superclass?.declaredFields?.forEach { field ->
                        try {
                            field.isAccessible = true
                            val value = field.get(e)
                            Log.e(TAG, "  ${field.name} (super): $value")
                        } catch (ex: Exception) {
                            // Ignore
                        }
                    }
                } catch (ex: Exception) {
                    Log.w(TAG, "Could not inspect exception fields: ${ex.message}")
                }
            }
            
            Log.e(TAG, "Full stack trace:", e)
            Log.e(TAG, "========================================")
            val errorMessage = parseAuthError(e)
            Log.e(TAG, "Parsed error message: $errorMessage")
            emit(Resource.Error(errorMessage))
        }
    }.flowOn(Dispatchers.IO)
    
    // Interpreta erros e devolve mensagem amigavel
    private fun parseAuthError(e: Exception): String {
        val message = e.message ?: ""
        val cause = e.cause?.message ?: ""
        val fullError = "$message | Cause: $cause"
        
        Log.e(TAG, "Parsing error - Full error: $fullError")
        Log.e(TAG, "Exception class: ${e.javaClass.name}")
        
        // Try to extract HTTP status code from UnknownRestException
        var httpStatusCode: Int? = null
        var responseBody: String? = null
        
        if (e is UnknownRestException) {
            try {
                // Try to get status code using reflection
                val statusCodeField = e.javaClass.superclass?.getDeclaredField("statusCode")
                    ?: e.javaClass.getDeclaredField("statusCode")
                statusCodeField?.isAccessible = true
                httpStatusCode = statusCodeField?.get(e) as? Int
                
                // Try to get response object and extract body from it
                val responseField = e.javaClass.superclass?.getDeclaredField("response")
                    ?: e.javaClass.getDeclaredField("response")
                responseField?.isAccessible = true
                val response = responseField?.get(e)
                if (response != null) {
                    val responseClass = response.javaClass
                    // Try to get body from HttpResponse
                    // Note: bodyAsString is a suspend function in Ktor, so we can't call it via reflection
                    // Try to access body content through properties
                    try {
                        // Try to get body property directly
                        val bodyField = responseClass.getDeclaredField("body")
                        bodyField.isAccessible = true
                        val bodyObj = bodyField.get(response)
                        if (bodyObj != null) {
                            // Try to get string representation
                            val bodyStr = bodyObj.toString()
                            if (bodyStr.isNotBlank() && bodyStr != "null") {
                                responseBody = bodyStr
                            }
                        }
                    } catch (e1: Exception) {
                        // Try alternative: get call property
                        try {
                            val callField = responseClass.getDeclaredField("call")
                            callField.isAccessible = true
                            val call = callField.get(response)
                            // The call object might have more info, but body extraction is limited
                            // without suspend function support
                        } catch (e2: Exception) {
                            // Ignore - body extraction failed (normal for Ktor responses)
                        }
                    }
                }
                
                if (httpStatusCode != null) {
                    Log.e(TAG, "Extracted HTTP Status Code: $httpStatusCode")
                }
                if (responseBody != null && responseBody.isNotBlank()) {
                    Log.e(TAG, "Extracted Response Body: $responseBody")
                }
            } catch (reflectionError: Exception) {
                Log.w(TAG, "Could not extract error details via reflection: ${reflectionError.message}")
            }
            
            // Try to extract status code from message (format: "statusCode: 400" or "400" in message)
            if (httpStatusCode == null) {
                val statusCodeRegex = Regex("""(?:status|code|http)[\s:=]*(\d{3})""", RegexOption.IGNORE_CASE)
                val match = statusCodeRegex.find(message)
                httpStatusCode = match?.groupValues?.get(1)?.toIntOrNull()
            }
        }
        
        // Check for HTTP status codes in message
        if (httpStatusCode == null) {
            val statusCodeRegex = Regex("""\b([45]\d{2})\b""")
            val match = statusCodeRegex.find(message)
            httpStatusCode = match?.groupValues?.get(1)?.toIntOrNull()
        }
        
        // Handle based on HTTP status code first
        httpStatusCode?.let { code ->
            return when (code) {
                400 -> {
                    // Bad Request - check response body for specific error
                    when {
                        responseBody?.contains("already registered", ignoreCase = true) == true ||
                        responseBody?.contains("already exists", ignoreCase = true) == true ||
                        message.contains("already registered", ignoreCase = true) ->
                            "Este email já está registado. Tenta fazer login."
                        responseBody?.contains("password", ignoreCase = true) == true ||
                        message.contains("password", ignoreCase = true) ->
                            "A password deve ter pelo menos 6 caracteres"
                        responseBody?.contains("email", ignoreCase = true) == true ||
                        message.contains("email", ignoreCase = true) ->
                            "Por favor insere um email válido"
                        else -> "Dados inválidos. Verifica os campos preenchidos."
                    }
                }
                401 -> "Email ou password incorretos"
                403 -> "Acesso negado. Verifica as tuas credenciais."
                409 -> "Este email já está registado. Tenta fazer login."
                422 -> {
                    // Unprocessable Entity - validation error
                    when {
                        responseBody?.contains("password", ignoreCase = true) == true ->
                            "A password deve ter pelo menos 6 caracteres"
                        responseBody?.contains("email", ignoreCase = true) == true ->
                            "Por favor insere um email válido"
                        else -> "Dados inválidos. Verifica os campos preenchidos."
                    }
                }
                429 -> "Muitas tentativas. Aguarda uns minutos."
                500, 502, 503, 504 -> {
                    // Server error - check if it's an unexpected_failure (usually database trigger issue)
                    when {
                        message.contains("unexpected_failure", ignoreCase = true) ||
                        responseBody?.contains("unexpected_failure", ignoreCase = true) == true ->
                            "Erro no servidor ao criar conta. Verifica se o email já existe ou contacta o suporte."
                        else -> "Servidor indisponível. Tenta mais tarde."
                    }
                }
                else -> "Erro do servidor (código $code). Tenta novamente."
            }
        }
        
        return when {
            // Database error saving new user (usually trigger/function issue)
            message.contains("Database error saving new user", ignoreCase = true) ||
            message.contains("unexpected_failure", ignoreCase = true) ||
            (e is AuthRestException && message.contains("unexpected_failure", ignoreCase = true)) ->
                "Erro no servidor ao criar conta. Verifica se o email já existe ou contacta o suporte."
            
            // Email already registered
            message.contains("already registered", ignoreCase = true) ||
            message.contains("already been registered", ignoreCase = true) ||
            message.contains("already exists", ignoreCase = true) || 
            message.contains("User already registered", ignoreCase = true) ||
            responseBody?.contains("already registered", ignoreCase = true) == true ||
            responseBody?.contains("already exists", ignoreCase = true) == true -> 
                "Este email já está registado. Tenta fazer login."
            
            // Invalid credentials
            message.contains("Invalid login credentials", ignoreCase = true) ||
            message.contains("invalid credentials", ignoreCase = true) -> 
                "Email ou password incorretos"
            
            // Email confirmation needed
            message.contains("Email not confirmed", ignoreCase = true) ||
            message.contains("email not confirmed", ignoreCase = true) -> 
                "Por favor confirma o teu email antes de fazer login"
            
            // Password validation
            message.contains("Password should be", ignoreCase = true) ||
            message.contains("password", ignoreCase = true) && message.contains("6", ignoreCase = true) ||
            responseBody?.contains("password", ignoreCase = true) == true ->
                "A password deve ter pelo menos 6 caracteres"
            
            // Email validation
            message.contains("valid email", ignoreCase = true) ||
            message.contains("invalid email", ignoreCase = true) ||
            responseBody?.contains("email", ignoreCase = true) == true ->
                "Por favor insere um email válido"
            
            // Rate limiting
            message.contains("rate limit", ignoreCase = true) ||
            message.contains("too many", ignoreCase = true) ||
            responseBody?.contains("rate limit", ignoreCase = true) == true ->
                "Muitas tentativas. Aguarda uns minutos."
            
            // Network errors
            message.contains("network", ignoreCase = true) ||
            message.contains("Unable to resolve host", ignoreCase = true) ||
            message.contains("connection", ignoreCase = true) ||
            message.contains("timeout", ignoreCase = true) ||
            message.contains("SocketTimeout", ignoreCase = true) ||
            message.contains("UnknownHost", ignoreCase = true) ||
            cause.contains("network", ignoreCase = true) ||
            cause.contains("connection", ignoreCase = true) ||
            cause.contains("Unable to resolve", ignoreCase = true) ->
                "Erro de conexão. Verifica a tua internet."
            
            // SSL errors
            message.contains("SSLHandshake", ignoreCase = true) ||
            message.contains("SSL", ignoreCase = true) ||
            message.contains("certificate", ignoreCase = true) ->
                "Erro de segurança. Atualiza a app ou verifica a hora do dispositivo."
            
            // Server errors (check for status codes in message)
            message.contains("500", ignoreCase = true) ||
            message.contains("502", ignoreCase = true) ||
            message.contains("503", ignoreCase = true) ||
            message.contains("server", ignoreCase = true) ->
                "Servidor indisponível. Tenta mais tarde."
            
            // UnknownRestException - try to provide more helpful message
            e is UnknownRestException -> {
                if (responseBody != null && responseBody.isNotBlank()) {
                    // Try to parse JSON error message from response body
                    try {
                        val errorMessage = responseBody.substringAfter("\"message\":\"").substringBefore("\"")
                        if (errorMessage.isNotBlank() && errorMessage.length < 200) {
                            return errorMessage
                        }
                    } catch (ex: Exception) {
                        // Ignore parsing errors
                    }
                }
                "Erro de comunicação com o servidor. Tenta novamente."
            }
            
            // Serialization/parsing errors
            message.contains("serialization", ignoreCase = true) ||
            message.contains("parse", ignoreCase = true) ||
            message.contains("json", ignoreCase = true) ||
            message.contains("decode", ignoreCase = true) ->
                "Erro de comunicação com o servidor. Tenta novamente."
            
            // Unknown error with more details
            else -> {
                val shortMessage = if (message.length > 100) message.take(100) + "..." else message
                if (shortMessage.isNotBlank() && !shortMessage.contains("Unknown Error")) {
                    "Erro: $shortMessage"
                } else {
                    "Erro de comunicação com o servidor. Tenta novamente."
                }
            }
        }
    }
    
    // Cria perfil e itens iniciais do utilizador
    private suspend fun createUserProfile(
        userId: String,
        email: String,
        username: String,
        birthDate: String,
        pokemonKnowledge: String,
        favoriteType: String,
        avatar: String,
        starterId: Int
    ) {
        // Create profile
        val profile = ProfileInsert( // prepara dados do perfil
            id = userId,
            email = email,
            username = username,
            birthDate = birthDate,
            pokemonKnowledge = pokemonKnowledge,
            favoriteType = favoriteType,
            equippedSkin = avatar,
            equippedPokemonId = starterId,
            gold = 100,
            xp = 0,
            level = 1,
            xpForNextLevel = 60
        )
        
        db.from("profiles").insert(profile) // insere perfil na base de dados
        Log.d(TAG, "Profile inserted for user: $userId")
        
        // Add initial inventory items (Note: this is also done by database trigger)
        try {
            val initialItems = listOf( // items iniciais do jogador
                InventoryInsert(userId = userId, itemId = avatar, category = "skin"),
                InventoryInsert(userId = userId, itemId = "theme_default", category = "theme"),
                InventoryInsert(userId = userId, itemId = "start_badget", category = "badge") // "badget" matches database schema
            )
            db.from("inventory").insert(initialItems) // insere items no inventario
            Log.d(TAG, "Initial inventory items added")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to add inventory items (non-critical): ${e.message}")
        }
        
        // Add starter Pokemon to Living Dex
        try {
            val starterPokemon = UserPokemonInsert( // prepara starter pokemon
                userId = userId,
                pokedexId = starterId
            )
            db.from("user_pokemons").insert(starterPokemon) // adiciona starter ao living dex
            Log.d(TAG, "Starter Pokemon added to Living Dex: $starterId")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to add starter Pokemon to Living Dex (non-critical): ${e.message}")
        }
        
        // Add starter Pokemon to Favorites (so it shows as favorited in Pokedex)
        try {
            db.from("favorites").insert(
                buildJsonObject {
                    put("user_id", userId)
                    put("pokedex_id", starterId)
                }
            ) // marca starter como favorito
            Log.d(TAG, "Starter Pokemon added to Favorites: $starterId")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to add starter to Favorites (non-critical): ${e.message}")
        }
    }

    // Termina sessao do utilizador
    suspend fun logout(): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading) // inicia processo de logout
        try {
            auth.signOut()
            Log.d(TAG, "Logout successful")
            emit(Resource.Success(true)) // logout bem sucedido
        } catch (e: Exception) {
            Log.e(TAG, "Logout failed", e)
            emit(Resource.Error("Erro ao sair: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    // Verifica se ha sessao ativa
    fun isUserLoggedIn(): Boolean {
        val isLoggedIn = auth.currentUserOrNull() != null
        Log.d(TAG, "isUserLoggedIn: $isLoggedIn")
        return isLoggedIn
    }
    
    // Devolve id do utilizador ou null
    fun getCurrentUserId(): String? {
        val userId = auth.currentUserOrNull()?.id
        Log.d(TAG, "getCurrentUserId: $userId")
        return userId
    }
}

// Data classes for database inserts
@Serializable
data class ProfileInsert(
    val id: String,
    val email: String,
    val username: String,
    @kotlinx.serialization.SerialName("birth_date")
    val birthDate: String,
    @kotlinx.serialization.SerialName("pokemon_knowledge")
    val pokemonKnowledge: String,
    @kotlinx.serialization.SerialName("favorite_type")
    val favoriteType: String,
    @kotlinx.serialization.SerialName("equipped_skin")
    val equippedSkin: String,
    @kotlinx.serialization.SerialName("equipped_pokemon_id")
    val equippedPokemonId: Int,
    val gold: Long = 100,
    val xp: Long = 0,
    val level: Int = 1,
    @kotlinx.serialization.SerialName("xp_for_next_level")
    val xpForNextLevel: Long = 60
)

@Serializable
data class InventoryInsert(
    @kotlinx.serialization.SerialName("user_id")
    val userId: String,
    @kotlinx.serialization.SerialName("item_id")
    val itemId: String,
    val category: String
)

@Serializable
data class UserPokemonInsert(
    @kotlinx.serialization.SerialName("user_id")
    val userId: String,
    @kotlinx.serialization.SerialName("pokedex_id")
    val pokedexId: Int
)
