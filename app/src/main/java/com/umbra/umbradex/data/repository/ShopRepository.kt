package com.umbra.umbradex.data.repository

import android.util.Log
import com.umbra.umbradex.data.model.ShopItem
import com.umbra.umbradex.data.supabase.UmbraSupabase
import com.umbra.umbradex.utils.Resource
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put




class ShopRepository {

    // Buscar todos os itens disponíveis na loja
    suspend fun getAvailableItems(): Flow<Resource<List<ShopItem>>> = flow {
        emit(Resource.Loading)
        try {
            val items = UmbraSupabase.client.from("shop_items")
                .select()
                .decodeList<ShopItem>()
                .filter { it.isAvailable }
                // Filter out standard/default items (given at signup, not in shop)
                .filter { item -> 
                    !item.name.startsWith("Standard ", ignoreCase = true) && 
                    item.name != "Classic Purple" && // Default theme
                    item.name != "Starter Badge" && // Default badge
                    item.name != "Trainer White" && // Default name color
                    item.name != "Rookie" && // Default title
                    item.price > 0 // Only show items with a price
                }
                .sortedBy { it.sortOrder }

            emit(Resource.Success(items))
        } catch (e: Exception) {
            emit(Resource.Error("Failed to load shop items: ${e.message}", e))
        }
    }

    // Verificar se o user já possui um item
    suspend fun userOwnsItem(userId: String, itemName: String, category: String): Boolean {
        return try {
            val result = UmbraSupabase.client.from("inventory")
                .select(Columns.list("id")) {
                    filter {
                        eq("user_id", userId)
                        eq("item_id", itemName)
                        eq("category", category)
                    }
                }
                .decodeList<Map<String, Any>>()

            result.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    // Comprar item (gastar gold + adicionar ao inventário)
    // NOTA: Usa transação segura - se falhar ao adicionar ao inventário, devolve o gold
    suspend fun purchaseItem(
        userId: String,
        item: ShopItem,
        currentGold: Int
    ): Resource<String> {
        return try {
            // 1. Verificar se tem gold suficiente
            if (currentGold < item.price) {
                return Resource.Error("Insufficient gold")
            }

            // 2. Verificar se já possui
            if (userOwnsItem(userId, item.name, item.type)) {
                return Resource.Error("Item already owned")
            }

            // 3. Gastar gold usando RPC
            Log.d("ShopRepository", "Spending ${item.price} gold for user $userId")
            UmbraSupabase.client.postgrest.rpc(
                function = "spend_gold",
                parameters = buildJsonObject {
                    put("p_user_id", userId)
                    put("p_amount", item.price)
                }
            )

            // 4. Adicionar ao inventário (com rollback se falhar)
            try {
                UmbraSupabase.client.from("inventory").insert(
                    mapOf(
                        "user_id" to userId,
                        "item_id" to item.name,
                        "category" to item.type
                    )
                )
            } catch (inventoryError: Exception) {
                // ROLLBACK: Devolver o gold ao utilizador
                Log.e("ShopRepository", "Inventory insert failed, rolling back gold: ${inventoryError.message}")
                try {
                    UmbraSupabase.client.postgrest.rpc(
                        function = "add_gold",
                        parameters = buildJsonObject {
                            put("p_user_id", userId)
                            put("p_amount", item.price)
                        }
                    )
                } catch (rollbackError: Exception) {
                    Log.e("ShopRepository", "CRITICAL: Rollback failed! User lost ${item.price} gold: ${rollbackError.message}")
                }
                return Resource.Error("Purchase failed. Gold refunded.")
            }

            Resource.Success("Item purchased successfully!")
        } catch (e: Exception) {
            if (e.message?.contains("Insufficient gold") == true) {
                Resource.Error("Not enough gold!")
            } else {
                Resource.Error("Purchase failed: ${e.message}", e)
            }
        }
    }

    // Buscar itens do inventário do user
    suspend fun getUserInventory(userId: String): Flow<Resource<List<String>>> = flow {
        emit(Resource.Loading)
        try {
            val inventory = UmbraSupabase.client.from("inventory")
                .select(Columns.list("item_id")) {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<Map<String, String>>()
                .mapNotNull { it["item_id"] }

            emit(Resource.Success(inventory))
        } catch (e: Exception) {
            emit(Resource.Error("Failed to load inventory: ${e.message}", e))
        }
    }
}