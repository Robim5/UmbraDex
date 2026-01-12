package com.umbra.umbradex.data.api

import com.umbra.umbradex.utils.Constants
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // Criação preguiçosa (lazy) do serviço
    val api: PokeApiService by lazy {
        Retrofit.Builder()
            .baseUrl(Constants.BASE_URL_POKEAPI) // Certifica-te que tens isto em utils/Constants.kt
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PokeApiService::class.java)
    }
}