package com.umbra.umbradex.data.api

import com.umbra.umbradex.data.model.PokemonApiDto
import retrofit2.http.GET
import retrofit2.http.Path
import com.umbra.umbradex.data.model.*
import retrofit2.http.Url

interface PokeApiService {
    @GET("pokemon/{id}")
    suspend fun getPokemonDetail(@Path("id") id: Int): PokemonApiDto

    @GET("pokemon-species/{id}")
    suspend fun getPokemonSpecies(@Path("id") id: Int): PokemonSpeciesDto

    // Usar URL dinâmico porque o evolution chain vem com URL completo
    @GET
    suspend fun getEvolutionChain(@Url url: String): EvolutionChainDto
}