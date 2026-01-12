package com.umbra.umbradex.data.repository

import android.util.Log
import com.umbra.umbradex.data.api.RetrofitClient
import com.umbra.umbradex.data.model.Pokemon
import com.umbra.umbradex.data.model.UserPokemon
import com.umbra.umbradex.data.supabase.UmbraSupabase
import com.umbra.umbradex.utils.Resource
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import com.umbra.umbradex.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.supervisorScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

// Serializable data class for favorites insert (UserPokemonInsert is in AuthRepository)
@Serializable
data class FavoriteInsert(
    @SerialName("user_id") val userId: String,
    @SerialName("pokedex_id") val pokedexId: Int
)




class PokemonRepository {

    private val db = UmbraSupabase.db
    private val auth = UmbraSupabase.auth
    private val pokeApi = RetrofitClient.api
    
    companion object {
        // Cache estático para poder ser limpo externamente (ex: ao fazer logout ou entrar como guest)
        private var cachedPokemonList: List<Pokemon>? = null
        private var lastCacheTime: Long = 0
        private val cacheValidityMs = 5 * 60 * 1000 // 5 minutos
        
        /**
         * Limpa o cache estático do repositório.
         * Deve ser chamado quando o utilizador faz logout ou entra como convidado.
         */
        fun clearStaticCache() {
            cachedPokemonList = null
            lastCacheTime = 0
        }
    }

    // ========================================
    // MÉTODO OTIMIZADO: getAllPokemon
    // Usa carregamento paralelo por batches
    // ========================================
    suspend fun getAllPokemon(limit: Int = 1025): Flow<Resource<List<Pokemon>>> = flow {
        emit(Resource.Loading)
        try {
            // CRÍTICO: Em modo convidado, sempre retornar lista vazia de caught/favorites
            // para garantir que nenhum Pokemon aparece como capturado/favorito
            val userId = auth.currentUserOrNull()?.id
            
            // Verificar cache apenas se não estamos em modo convidado
            val now = System.currentTimeMillis()
            if (userId != null) {
                cachedPokemonList?.let { cached ->
                    if (now - lastCacheTime < cacheValidityMs) {
                        // Atualizar apenas os estados de caught/favorite do cache
                        val caughtIds = getCaughtIds(userId)
                        val favoriteIds = getFavoriteIds(userId)
                        val updated = cached.map { pokemon ->
                            pokemon.copy(
                                isCaught = caughtIds.contains(pokemon.id),
                                isFavorite = favoriteIds.contains(pokemon.id)
                            )
                        }
                        emit(Resource.Success(updated))
                        return@flow
                    }
                }
            }
            
            // Buscar IDs de caught/favoritos (será vazio se userId == null)
            val caughtIds = getCaughtIds(userId)
            val favoriteIds = getFavoriteIds(userId)

            // Carregar Pokémon em batches paralelos para melhor performance
            // e emitir resultados parciais para a UI mostrar rapidamente
            val batchSize = 40
            val emitEveryBatches = 3      // emite a cada N batches
            val minEmitThreshold = 80     // só emite parcial depois deste mínimo
            val pokemonList = mutableListOf<Pokemon>()
            var batchIndex = 0
            
            supervisorScope {
                for (batchStart in 1..limit step batchSize) {
                    val batchEnd = minOf(batchStart + batchSize - 1, limit)
                    batchIndex++
                    
                    val batchResults = (batchStart..batchEnd).map { id ->
                        async(Dispatchers.IO) {
                            try {
                                val dto = pokeApi.getPokemonDetail(id)
                                Pokemon(
                                    id = dto.id,
                                    name = dto.name,
                                    imageUrl = dto.sprites.other?.officialArtwork?.frontDefault
                                        ?: dto.sprites.frontDefault ?: "",
                                    types = dto.types.map { it.type.name.replaceFirstChar { c -> c.uppercase() } },
                                    height = dto.height / 10.0,
                                    weight = dto.weight / 10.0,
                                    isCaught = caughtIds.contains(dto.id),
                                    isFavorite = favoriteIds.contains(dto.id)
                                )
                            } catch (e: Exception) {
                                Log.w("PokemonRepository", "Failed to load Pokemon #$id: ${e.message}")
                                null
                            }
                        }
                    }.awaitAll().filterNotNull()
                    
                    pokemonList.addAll(batchResults)
                    
                    // Emitir resultados parciais para que a UI mostre a Pokédex mais cedo
                    val shouldEmitPartial = pokemonList.size >= minEmitThreshold &&
                        (batchIndex % emitEveryBatches == 0 || batchEnd == limit)
                    if (shouldEmitPartial) {
                        val partialSorted = pokemonList.sortedBy { it.id }
                        emit(Resource.Success(partialSorted))
                    }
                }
            }
            
            // Ordenar por ID
            val sortedList = pokemonList.sortedBy { it.id }
            
            // Atualizar cache
            cachedPokemonList = sortedList
            lastCacheTime = now

            emit(Resource.Success(sortedList))
        } catch (e: Exception) {
            Log.e("PokemonRepository", "Failed to load Pokémon list", e)
            emit(Resource.Error("Falha ao carregar Pokémon: ${e.message}"))
        }
    }
    
    private suspend fun getCaughtIds(userId: String?): List<Int> {
        return if (userId != null) {
            try {
                db.from("user_pokemons")
                    .select(columns = Columns.list("pokedex_id")) {
                        filter { eq("user_id", userId) }
                    }
                    .decodeList<UserPokemon>()
                    .map { it.pokedexId }
            } catch (e: Exception) {
                emptyList()
            }
        } else emptyList()
    }
    
    private suspend fun getFavoriteIds(userId: String?): List<Int> {
        return if (userId != null) {
            try {
                db.from("favorites")
                    .select(columns = Columns.list("pokedex_id")) {
                        filter { eq("user_id", userId) }
                    }
                    .decodeList<Map<String, Int>>()
                    .map { it["pokedex_id"] ?: 0 }
            } catch (e: Exception) {
                emptyList()
            }
        } else emptyList()
    }
    
    /**
     * Invalida o cache para forçar recarregamento
     */
    fun invalidateCache() {
        clearStaticCache()
    }

    // ========================================
    // NOVO MÉTODO: getPokemonFullDetails
    // ========================================
    suspend fun getPokemonFullDetails(pokemonId: Int): Flow<Resource<PokemonDetail>> = flow {
        emit(Resource.Loading)
        try {
            val userId = auth.currentUserOrNull()?.id

            // 1. Buscar dados base
            val pokemonDto = pokeApi.getPokemonDetail(pokemonId)

            // 2. Buscar species
            val speciesDto = pokeApi.getPokemonSpecies(pokemonId)

            // 3. Buscar evolution chain
            val evolutionChainDto = pokeApi.getEvolutionChain(speciesDto.evolutionChain.url)

            // 4. Descrição
            val description = speciesDto.flavorTextEntries
                .firstOrNull { it.language.name == "en" }
                ?.flavorText
                ?.replace("\n", " ")
                ?.replace("\u000c", " ")
                ?: "No description available."

            // 5. Stats
            val stats = pokemonDto.stats.map { statDto ->
                PokemonStat(
                    name = when (statDto.stat.name) {
                        "hp" -> "HP"
                        "attack" -> "ATK"
                        "defense" -> "DEF"
                        "special-attack" -> "SP.ATK"
                        "special-defense" -> "SP.DEF"
                        "speed" -> "SPEED"
                        else -> statDto.stat.name.uppercase()
                    },
                    value = statDto.baseStat,
                    max = 255
                )
            }

            // 6. Evolution chain
            val evolutions = parseEvolutionChain(evolutionChainDto.chain)

            // 7. Verificar caught/favorite
            var isCaught = false
            var isFavorite = false

            if (userId != null) {
                isCaught = db.from("user_pokemons")
                    .select(columns = Columns.list("id")) {
                        filter {
                            eq("user_id", userId)
                            eq("pokedex_id", pokemonId)
                        }
                    }
                    .decodeList<Map<String, String>>()
                    .isNotEmpty()

                isFavorite = db.from("favorites")
                    .select(columns = Columns.list("user_id")) {
                        filter {
                            eq("user_id", userId)
                            eq("pokedex_id", pokemonId)
                        }
                    }
                    .decodeList<Map<String, String>>()
                    .isNotEmpty()
            }

            // 8. Construir PokemonDetail
            val detail = PokemonDetail(
                id = pokemonDto.id,
                name = pokemonDto.name,
                imageUrl = pokemonDto.sprites.other?.officialArtwork?.frontDefault ?: "",
                shinyImageUrl = pokemonDto.sprites.other?.officialArtwork?.frontShiny,
                types = pokemonDto.types.map { it.type.name.replaceFirstChar { c -> c.uppercase() } },
                weight = pokemonDto.weight / 10.0,
                height = pokemonDto.height / 10.0,
                description = description,
                stats = stats,
                evolutions = evolutions,
                isCaught = isCaught,
                isFavorite = isFavorite,
                abilities = pokemonDto.abilities.map {
                    it.ability.name.replaceFirstChar { c -> c.uppercase() }
                },
                cryUrl = pokemonDto.cries?.latest,
                isLegendary = speciesDto.isLegendary,
                isMythical = speciesDto.isMythical
            )

            emit(Resource.Success(detail))

        } catch (e: Exception) {
            emit(Resource.Error("Failed to load details: ${e.message}"))
        }
    }

    private fun parseEvolutionChain(chain: ChainLink): List<EvolutionStep> {
        val evolutions = mutableListOf<EvolutionStep>()

        fun traverse(link: ChainLink) {
            val speciesId = link.species.url.split("/").dropLast(1).last().toIntOrNull() ?: 0

            val evolutionTrigger = if (link.evolutionDetails.isNotEmpty()) {
                val detail = link.evolutionDetails.first()
                when {
                    detail.minLevel != null -> "Lv. ${detail.minLevel}"
                    detail.item != null -> detail.item.name.replaceFirstChar { it.uppercase() }
                    detail.trigger.name == "trade" -> "Trade"
                    else -> ""
                }
            } else ""

            evolutions.add(
                EvolutionStep(
                    id = speciesId,
                    name = link.species.name.replaceFirstChar { it.uppercase() },
                    imageUrl = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$speciesId.png",
                    evolutionTrigger = evolutionTrigger
                )
            )

            link.evolvesTo.forEach { traverse(it) }
        }

        traverse(chain)
        return evolutions
    }

    // ========================================
    // MÉTODOS EXISTENTES (mantém os que já tens)
    // ========================================
    suspend fun addToLivingDex(pokedexId: Int): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading)
        try {
            val userId = auth.currentUserOrNull()?.id
            Log.d("PokemonRepository", "addToLivingDex - userId: $userId, pokedexId: $pokedexId")
            
            if (userId == null) {
                Log.e("PokemonRepository", "addToLivingDex - User not logged in!")
                emit(Resource.Error("User not logged in"))
                return@flow
            }

            db.from("user_pokemons").insert(
                UserPokemonInsert(
                    userId = userId,
                    pokedexId = pokedexId
                )
            )
            
            // Invalidate cache so the next getAllPokemon refresh shows the new state
            invalidateCache()
            
            Log.d("PokemonRepository", "addToLivingDex - Success!")
            emit(Resource.Success(true))
        } catch (e: Exception) {
            Log.e("PokemonRepository", "addToLivingDex failed: ${e.message}", e)
            emit(Resource.Error("Failed to add Pokémon: ${e.message}"))
        }
    }

    suspend fun removeFromLivingDex(pokedexId: Int): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading)
        try {
            val userId = auth.currentUserOrNull()?.id
            Log.d("PokemonRepository", "removeFromLivingDex - userId: $userId, pokedexId: $pokedexId")
            
            if (userId == null) {
                Log.e("PokemonRepository", "removeFromLivingDex - User not logged in!")
                emit(Resource.Error("User not logged in"))
                return@flow
            }

            db.from("user_pokemons").delete {
                filter {
                    eq("user_id", userId)
                    eq("pokedex_id", pokedexId)
                }
            }
            
            // Invalidate cache
            invalidateCache()
            
            Log.d("PokemonRepository", "removeFromLivingDex - Success!")
            emit(Resource.Success(true))
        } catch (e: Exception) {
            Log.e("PokemonRepository", "removeFromLivingDex failed: ${e.message}", e)
            emit(Resource.Error("Failed to remove Pokémon: ${e.message}"))
        }
    }

    suspend fun addFavorite(pokedexId: Int): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading)
        try {
            val userId = auth.currentUserOrNull()?.id
            Log.d("PokemonRepository", "addFavorite - userId: $userId, pokedexId: $pokedexId")
            
            if (userId == null) {
                Log.e("PokemonRepository", "addFavorite - User not logged in!")
                emit(Resource.Error("User not logged in"))
                return@flow
            }

            // Check if this Pokemon is already a favorite
            val existingFavorite = db.from("favorites")
                .select(columns = Columns.list("pokedex_id")) {
                    filter {
                        eq("user_id", userId)
                        eq("pokedex_id", pokedexId)
                    }
                }
                .decodeList<Map<String, Int>>()
            
            if (existingFavorite.isEmpty()) {
                // Add to favorites (multiple favorites allowed now)
                db.from("favorites").insert(
                    FavoriteInsert(
                        userId = userId,
                        pokedexId = pokedexId
                    )
                )
                Log.d("PokemonRepository", "addFavorite - Added to favorites")
            } else {
                Log.d("PokemonRepository", "addFavorite - Already in favorites, just updating equipped pet")
            }
            
            // Always set this Pokemon as the equipped pet on the profile (most recent favorite)
            db.from("profiles").update({
                set("equipped_pokemon_id", pokedexId)
            }) {
                filter {
                    eq("id", userId)
                }
            }
            
            // Invalidate cache
            invalidateCache()
            
            Log.d("PokemonRepository", "addFavorite - Success! Set as equipped pet.")
            emit(Resource.Success(true))
        } catch (e: Exception) {
            Log.e("PokemonRepository", "addFavorite failed: ${e.message}", e)
            emit(Resource.Error("Failed to add favorite: ${e.message}"))
        }
    }

    suspend fun removeFavorite(pokedexId: Int): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading)
        try {
            val userId = auth.currentUserOrNull()?.id
            Log.d("PokemonRepository", "removeFavorite - userId: $userId, pokedexId: $pokedexId")
            
            if (userId == null) {
                Log.e("PokemonRepository", "removeFavorite - User not logged in!")
                emit(Resource.Error("User not logged in"))
                return@flow
            }

            // Remove from favorites
            db.from("favorites").delete {
                filter {
                    eq("user_id", userId)
                    eq("pokedex_id", pokedexId)
                }
            }
            
            // Check if this was the equipped pet, if so set the most recent favorite as the new pet
            // or null if no favorites remain
            // Check if this was the equipped pet, if so set the most recent favorite as the new pet
            // or null if no favorites remain
            val remainingFavorites = try {
                db.from("favorites")
                    .select(columns = Columns.list("pokedex_id, created_at")) {
                        filter { eq("user_id", userId) }
                        order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                        limit(1)
                    }
                    .decodeList<Map<String, Any>>()
            } catch (e: Exception) {
                Log.w("PokemonRepository", "Error fetching remaining favorites: ${e.message}")
                emptyList()
            }
            
            val newEquippedId: Int? = try {
                val pokedexIdValue = remainingFavorites.firstOrNull()?.get("pokedex_id")
                when (pokedexIdValue) {
                    is Int -> pokedexIdValue
                    is Number -> pokedexIdValue.toInt()
                    is String -> pokedexIdValue.toIntOrNull()
                    else -> null
                }
            } catch (e: Exception) {
                Log.w("PokemonRepository", "Error parsing pokedex_id: ${e.message}")
                null
            }
            
            // Update equipped_pokemon_id to the most recent favorite (or null if none)
            try {
                if (newEquippedId != null) {
                    db.from("profiles").update({
                        set("equipped_pokemon_id", newEquippedId)
                    }) {
                        filter { eq("id", userId) }
                    }
                    Log.d("PokemonRepository", "Updated equipped_pokemon_id to: $newEquippedId")
                } else {
                    // No more favorites - set to null (will need to handle this in UI)
                    db.from("profiles").update({
                        set("equipped_pokemon_id", null as Int?)
                    }) {
                        filter { eq("id", userId) }
                    }
                    Log.d("PokemonRepository", "Set equipped_pokemon_id to null (no favorites remaining)")
                }
            } catch (e: Exception) {
                Log.e("PokemonRepository", "Error updating equipped_pokemon_id: ${e.message}", e)
                // Não falhar a operação se apenas a atualização do equipped falhar
            }
            
            // Invalidate cache
            invalidateCache()
            
            Log.d("PokemonRepository", "removeFavorite - Success! New equipped: $newEquippedId")
            emit(Resource.Success(true))
        } catch (e: Exception) {
            Log.e("PokemonRepository", "removeFavorite failed: ${e.message}", e)
            emit(Resource.Error("Failed to remove favorite: ${e.message}"))
        }
    }

    suspend fun getUserLivingDex(): Flow<Resource<List<UserPokemon>>> = flow {
        emit(Resource.Loading)
        try {
            val userId = auth.currentUserOrNull()?.id
                ?: throw Exception("User not logged in")

            val userPokemons = db.from("user_pokemons")
                .select()
                {
                    filter { eq("user_id", userId) }
                }
                .decodeList<UserPokemon>()

            emit(Resource.Success(userPokemons))
        } catch (e: Exception) {
            emit(Resource.Error("Failed to load Living Dex: ${e.message}"))
        }
    }
}