package com.umbra.umbradex.data.supabase

import android.content.Context
import android.util.Log
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json

object UmbraSupabase {

    private const val TAG = "UmbraSupabase"
    private const val SUPABASE_URL = "https://fgwcqwrohktipjtccclc.supabase.co"
    private const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZnd2Nxd3JvaGt0aXBqdGNjY2xjIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjYzMjkyMTEsImV4cCI6MjA4MTkwNTIxMX0.ByVz0SU3LRmmlbuT9XQTgCWZjz0HlRtdADUVrYWayPs"

    private var _context: Context? = null

    fun initialize(context: Context) {
        _context = context.applicationContext
        Log.d(TAG, "UmbraSupabase initialized with context")
    }

    private val _client by lazy {
        try {
            Log.d(TAG, "Initializing Supabase client...")
            Log.d(TAG, "URL: $SUPABASE_URL")
            
            createSupabaseClient(
                supabaseUrl = SUPABASE_URL,
                supabaseKey = SUPABASE_KEY
            ) {
                // Configurações de serialização JSON usando KotlinXSerializer
                defaultSerializer = KotlinXSerializer(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    coerceInputValues = true
                    encodeDefaults = true
                })
                
                install(Auth) {
                    // Enable auto-refresh of session tokens
                    autoLoadFromStorage = true
                    autoSaveToStorage = true
                    alwaysAutoRefresh = true
                }
                install(Postgrest)
                install(Storage)
            }.also {
                Log.d(TAG, "Supabase client initialized successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Supabase client", e)
            throw e
        }
    }

    // Accessors
    val client get() = _client
    val auth get() = _client.auth
    val db get() = _client.postgrest
    val storage get() = _client.storage
}