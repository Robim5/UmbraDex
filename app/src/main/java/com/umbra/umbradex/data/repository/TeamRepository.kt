package com.umbra.umbradex.data.repository

import android.util.Log
import com.umbra.umbradex.data.api.RetrofitClient
import com.umbra.umbradex.data.model.Team
import com.umbra.umbradex.data.supabase.UmbraSupabase
import com.umbra.umbradex.utils.Resource
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class TeamRepository {
    
    // API para buscar dados dos Pokémon
    private val pokeApi = RetrofitClient.api
    
    // Cache simples de dados de Pokémon (id -> name, types)
    private val pokemonDataCache = mutableMapOf<Int, PokemonBasicData>()
    
    private data class PokemonBasicData(
        val name: String,
        val types: List<String>
    )

    /**
     * Busca todas as equipas do utilizador com os seus Pokémon
     */
    suspend fun getUserTeams(): Flow<Resource<List<Team>>> = flow {
        emit(Resource.Loading)

        try {
            val userId = UmbraSupabase.client.auth.currentUserOrNull()?.id
                ?: throw Exception("User not logged in")

            // Buscar as equipas do utilizador
            val teamsResponse = UmbraSupabase.client
                .from("teams")
                .select(columns = Columns.ALL) {
                    filter {
                        eq("user_id", userId)
                    }
                    order("created_at", Order.ASCENDING)
                }
                .decodeList<Team>()

            // Para cada equipa, buscar os Pokémon dos slots
            val teamsWithPokemon = teamsResponse.map { team ->
                val pokemonResponse = UmbraSupabase.client
                    .from("team_slots")
                    .select(columns = Columns.ALL) {
                        filter {
                            eq("team_id", team.id)
                        }
                        order("slot_position", Order.ASCENDING)
                    }
                    .decodeList<Team.TeamPokemon>()
                
                // Enriquecer os Pokémon com dados da API (nome, tipos)
                val enrichedPokemon = pokemonResponse.map { pokemon ->
                    val basicData = getPokemonBasicData(pokemon.pokedexId)
                    Team.TeamPokemon(
                        pokedexId = pokemon.pokedexId,
                        slotPosition = pokemon.slotPosition,
                        level = pokemon.level,
                        name = basicData.name,
                        imageUrl = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/${pokemon.pokedexId}.png",
                        types = basicData.types
                    )
                }

                team.copy(pokemon = enrichedPokemon)
            }

            emit(Resource.Success(teamsWithPokemon))
        } catch (e: Exception) {
            emit(Resource.Error("Failed to load teams: ${e.message}", e))
        }
    }
    
    /**
     * Obtém dados básicos de um Pokémon (nome e tipos) usando a PokeAPI
     * Usa cache interno para evitar chamadas repetidas
     */
    private suspend fun getPokemonBasicData(pokemonId: Int): PokemonBasicData {
        // Verificar cache primeiro
        pokemonDataCache[pokemonId]?.let { return it }
        
        // Buscar da PokeAPI
        return try {
            val pokemonDto = pokeApi.getPokemonDetail(pokemonId)
            val name = pokemonDto.name.replaceFirstChar { it.uppercase() }
            val types = pokemonDto.types.map { slot -> 
                slot.type.name.replaceFirstChar { it.uppercase() } 
            }
            
            val data = PokemonBasicData(name, types)
            pokemonDataCache[pokemonId] = data
            data
        } catch (e: Exception) {
            Log.w("TeamRepository", "Failed to fetch Pokemon #$pokemonId from API: ${e.message}")
            // Fallback simples em caso de erro
            val data = PokemonBasicData("Pokemon #$pokemonId", emptyList())
            pokemonDataCache[pokemonId] = data
            data
        }
    }

    /**
     * Cria uma nova equipa
     * @return Resource indicando sucesso ou erro
     */
    suspend fun createTeam(
        name: String,
        region: String,
        gradientColors: List<String>
    ): Resource<Unit> {
        return try {
            // Validação do nome
            if (name.isBlank() || name.length > 50) {
                return Resource.Error("Nome da equipa deve ter entre 1 e 50 caracteres")
            }
            
            val userId = UmbraSupabase.client.auth.currentUserOrNull()?.id
                ?: return Resource.Error("Utilizador não autenticado")

            // Usa JsonArray para o campo gradient_colors (JSONB na BD)
            val teamData = buildJsonObject {
                put("user_id", userId)
                put("name", name)
                put("region", region)
                put("gradient_colors", kotlinx.serialization.json.JsonArray(
                    gradientColors.map { kotlinx.serialization.json.JsonPrimitive(it) }
                ))
            }

            UmbraSupabase.client
                .from("teams")
                .insert(teamData)
            
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("TeamRepository", "Failed to create team", e)
            Resource.Error("Falha ao criar equipa: ${e.message}", e)
        }
    }

    /**
     * Adiciona ou substitui um Pokémon num slot específico
     */
    suspend fun addOrReplacePokemonInTeam(
        teamId: String,
        slotIndex: Int,
        pokemonId: Int,
        pokemonName: String,
        pokemonImageUrl: String,
        pokemonTypes: List<String>,
        level: Int
    ) {
        // slot_position na BD é 1-6, mas slotIndex do UI é 0-5
        val dbSlotPosition = slotIndex + 1
        
        // Primeiro, verifica se já existe um Pokémon nesse slot
        val existingPokemon = UmbraSupabase.client
            .from("team_slots")
            .select(columns = Columns.ALL) {
                filter {
                    eq("team_id", teamId)
                    eq("slot_position", dbSlotPosition)
                }
            }
            .decodeSingleOrNull<Team.TeamPokemon>()

        if (existingPokemon != null) {
            // Atualizar o Pokémon existente
            val updateData = buildJsonObject {
                put("pokedex_id", pokemonId)
                put("level", level)
            }

            UmbraSupabase.client
                .from("team_slots")
                .update(updateData) {
                    filter {
                        eq("team_id", teamId)
                        eq("slot_position", dbSlotPosition)
                    }
                }
        } else {
            // Inserir novo Pokémon no slot
            val insertData = buildJsonObject {
                put("team_id", teamId)
                put("pokedex_id", pokemonId)
                put("slot_position", dbSlotPosition)
                put("level", level)
            }

            UmbraSupabase.client
                .from("team_slots")
                .insert(insertData)
        }
    }

    /**
     * Remove um Pokémon de um slot específico
     */
    suspend fun removePokemonFromSlot(teamId: String, slotIndex: Int) {
        // slot_position na BD é 1-6, mas slotIndex do UI é 0-5
        val dbSlotPosition = slotIndex + 1
        
        UmbraSupabase.client
            .from("team_slots")
            .delete {
                filter {
                    eq("team_id", teamId)
                    eq("slot_position", dbSlotPosition)
                }
            }
    }

    /**
     * Atualiza o nome de uma equipa
     */
    suspend fun updateTeamName(teamId: String, newName: String) {
        val updateData = buildJsonObject {
            put("name", newName)
        }

        UmbraSupabase.client
            .from("teams")
            .update(updateData) {
                filter {
                    eq("id", teamId)
                }
            }
    }

    /**
     * Elimina uma equipa (os Pokémon são eliminados automaticamente por CASCADE)
     */
    suspend fun deleteTeam(teamId: String) {
        UmbraSupabase.client
            .from("teams")
            .delete {
                filter {
                    eq("id", teamId)
                }
            }
    }
}