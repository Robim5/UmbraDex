package com.umbra.umbradex.data.repository

import com.umbra.umbradex.data.model.ShopItem
import com.umbra.umbradex.data.model.Title
import com.umbra.umbradex.data.supabase.UmbraSupabase
import com.umbra.umbradex.utils.PokemonDataEvents
import com.umbra.umbradex.utils.Resource
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

data class InventoryItem(
    val item: ShopItem,
    val acquiredAt: String
)

// Item de título para exibição no inventário
data class TitleInventoryItem(
    val title: Title,
    val isUnlocked: Boolean
)

class InventoryRepository {

    // Buscar todos os itens do inventário do usuário
    suspend fun getUserInventory(userId: String): Flow<Resource<List<InventoryItem>>> = flow {
        emit(Resource.Loading)
        try {
            // 1. Buscar IDs dos itens no inventário
            val inventoryRecords = UmbraSupabase.client.from("inventory")
                .select(Columns.list("item_id", "category", "acquired_at")) {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<Map<String, String>>()

            // 2. Buscar detalhes dos itens na shop_items
            val itemIds = inventoryRecords.mapNotNull { it["item_id"] }

            if (itemIds.isEmpty()) {
                emit(Resource.Success(emptyList()))
                return@flow
            }

            // Buscar TODOS os shop_items e filtrar no código
            // Isto porque o trigger pode guardar 'standard_male1' mas o item name é 'Standard Male 1'
            val allShopItems = UmbraSupabase.client.from("shop_items")
                .select()
                .decodeList<ShopItem>()
            
            // 3. Fazer matching flexível: por name, asset_url (sem .png), ou item_id conhecido
            val inventoryItems = mutableListOf<InventoryItem>()
            
            for (record in inventoryRecords) {
                val itemId = record["item_id"] ?: continue
                val category = record["category"] ?: ""
                
                // Tentar encontrar o item por diferentes critérios
                val shopItem = allShopItems.find { item ->
                    // Match exato por nome
                    item.name == itemId ||
                    // Match por asset_url sem extensão
                    item.assetUrl?.removeSuffix(".png") == itemId ||
                    // Match para theme_default -> Classic Purple
                    (itemId == "theme_default" && item.name == "Classic Purple") ||
                    // Match para start_badget -> Starter Badge
                    (itemId == "start_badget" && item.name == "Starter Badge") ||
                    // Match para standard_male1 -> Standard Male 1 (etc)
                    (item.assetUrl == "$itemId.png" && item.type == category) ||
                    // Match para name_color padrão (Trainer White) via cores
                    (itemId == "name_color_default" && item.name == "Trainer White")
                }
                
                if (shopItem != null) {
                    inventoryItems.add(
                        InventoryItem(
                            item = shopItem,
                            acquiredAt = record["acquired_at"] ?: ""
                        )
                    )
                }
            }

            emit(Resource.Success(inventoryItems))
        } catch (e: Exception) {
            emit(Resource.Error("Failed to load inventory: ${e.message}", e))
        }
    }

    // Equipar um item
    suspend fun equipItem(
        userId: String,
        itemName: String,
        category: String
    ): Resource<String> {
        return try {
            // Primeiro, buscar os detalhes do item para temas e cores
            val itemDetails = getItemDetails(itemName)
            val item = if (itemDetails is Resource.Success) itemDetails.data else null
            
            // Usar buildJsonObject para evitar erro de serialização com Map<String, Any?>
            val updateData = buildJsonObject {
                when (category) {
                    "skin" -> {
                        // Para skin, salvar o asset_url (sem .png) para consistência
                        val skinValue = item?.assetUrl?.removeSuffix(".png") ?: itemName
                        put("equipped_skin", skinValue)
                    }
                    "theme" -> {
                        // Para theme, salvar as cores como JSON array string
                        if (item != null && item.colors != null && item.colors.isNotEmpty()) {
                            val colorsJson = item.colors.joinToString(",", "[", "]") { "\"$it\"" }
                            put("equipped_theme", colorsJson)
                        } else {
                            put("equipped_theme", "theme_default")
                        }
                    }
                    "badge" -> {
                        // Para badge, salvar o asset_url (sem .png) para consistência
                        val badgeValue = item?.assetUrl?.removeSuffix(".png") ?: itemName
                        put("equipped_badge", badgeValue)
                    }
                    "name_color" -> {
                        // Para name_color, criar um JsonArray com as cores
                        if (item != null && item.colors != null) {
                            putJsonArray("equipped_name_color") {
                                item.colors.forEach { add(JsonPrimitive(it)) }
                            }
                        } else {
                            putJsonArray("equipped_name_color") {
                                add(JsonPrimitive("#FFFFFF"))
                            }
                        }
                    }
                    "title" -> {
                        put("equipped_title", itemName)
                    }
                    else -> {
                        // Categoria inválida - não adiciona nada
                    }
                }
            }
            
            // Verificar se a categoria é válida
            if (category !in listOf("skin", "theme", "badge", "name_color", "title")) {
                return Resource.Error("Invalid category")
            }

            UmbraSupabase.client.from("profiles")
                .update(updateData) {
                    filter {
                        eq("id", userId)
                    }
                }

            // Emitir evento para atualizar UI em tempo real
            when (category) {
                "skin" -> {
                    val skinValue = item?.assetUrl?.removeSuffix(".png") ?: itemName
                    PokemonDataEvents.notifySkinEquipped(skinValue)
                }
                "badge" -> {
                    val badgeValue = item?.assetUrl?.removeSuffix(".png") ?: itemName
                    PokemonDataEvents.notifyBadgeEquipped(badgeValue)
                }
                "theme" -> {
                    val colors = item?.colors ?: emptyList()
                    PokemonDataEvents.notifyThemeEquipped(itemName, colors)
                }
                "name_color" -> {
                    val colors = item?.colors ?: listOf("#FFFFFF")
                    PokemonDataEvents.notifyNameColorEquipped(itemName, colors)
                }
                "title" -> {
                    PokemonDataEvents.notifyTitleEquipped(itemName)
                }
            }

            Resource.Success("Item equipped successfully!")
        } catch (e: Exception) {
            Resource.Error("Failed to equip item: ${e.message}", e)
        }
    }

    // Buscar detalhes de um item específico (para preview)
    suspend fun getItemDetails(itemName: String): Resource<ShopItem> {
        return try {
            // Tentar buscar por nome exato primeiro
            val items = UmbraSupabase.client.from("shop_items")
                .select()
                .decodeList<ShopItem>()
            
            // Procurar com matching flexível
            val item = items.find { shopItem ->
                shopItem.name == itemName ||
                shopItem.assetUrl?.removeSuffix(".png") == itemName ||
                (itemName == "theme_default" && shopItem.name == "Classic Purple") ||
                (itemName == "start_badget" && shopItem.name == "Starter Badge") ||
                (itemName == "name_color_default" && shopItem.name == "Trainer White") ||
                shopItem.assetUrl == "$itemName.png"
            }
            
            if (item != null) {
                Resource.Success(item)
            } else {
                Resource.Error("Item not found: $itemName")
            }
        } catch (e: Exception) {
            Resource.Error("Failed to load item: ${e.message}", e)
        }
    }
    
    // Buscar tema por cores (para quando o tema está guardado como JSON de cores)
    suspend fun getThemeByColors(colors: List<String>): ShopItem? {
        return try {
            val items = UmbraSupabase.client.from("shop_items")
                .select() {
                    filter {
                        eq("type", "theme")
                    }
                }
                .decodeList<ShopItem>()
            
            // Encontrar tema com cores correspondentes
            items.find { item ->
                item.colors != null && item.colors.sorted() == colors.sorted()
            }
        } catch (e: Exception) {
            null
        }
    }
    
    // Buscar item por cores (para identificar name_color equipado)
    suspend fun getItemByColors(colors: List<String>): ShopItem? {
        return try {
            val items = UmbraSupabase.client.from("shop_items")
                .select() {
                    filter {
                        eq("type", "name_color")
                    }
                }
                .decodeList<ShopItem>()
            
            // Remover duplicados das cores para matching
            val uniqueColors = colors.distinct().sorted()
            
            // Encontrar item com cores correspondentes
            items.find { item ->
                if (item.colors == null) return@find false
                
                val itemUniqueColors = item.colors.distinct().sorted()
                
                // Match exato ou match com cores únicas
                itemUniqueColors == uniqueColors ||
                item.colors.sorted() == colors.sorted() ||
                // Match especial para cor branca padrão
                (uniqueColors == listOf("#FFFFFF") && itemUniqueColors == listOf("#FFFFFF"))
            }
        } catch (e: Exception) {
            null
        }
    }
    
    // Buscar todos os títulos disponíveis pelo nível do usuário
    suspend fun getTitlesByLevel(userLevel: Int): Resource<List<TitleInventoryItem>> {
        return try {
            val allTitles = UmbraSupabase.client.from("titles")
                .select()
                .decodeList<Title>()
            
            // Marcar quais estão desbloqueados baseado no nível
            val titleItems = allTitles.map { title ->
                TitleInventoryItem(
                    title = title,
                    isUnlocked = userLevel >= title.minLevel
                )
            }.sortedBy { it.title.minLevel }
            
            Resource.Success(titleItems)
        } catch (e: Exception) {
            Resource.Error("Failed to load titles: ${e.message}", e)
        }
    }
    
    // Buscar apenas títulos desbloqueados (para o inventário)
    suspend fun getUnlockedTitles(userLevel: Int): List<Title> {
        return try {
            UmbraSupabase.client.from("titles")
                .select() {
                    filter {
                        lte("min_level", userLevel)
                    }
                }
                .decodeList<Title>()
                .sortedByDescending { it.minLevel }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    // Buscar um título específico pelo nome
    suspend fun getTitleByName(titleName: String): Title? {
        return try {
            UmbraSupabase.client.from("titles")
                .select() {
                    filter {
                        eq("title_text", titleName)
                    }
                }
                .decodeSingleOrNull<Title>()
        } catch (e: Exception) {
            null
        }
    }
    
    // Obter o item padrão de name_color (Trainer White) para novo usuário
    suspend fun getDefaultNameColor(): ShopItem? {
        return try {
            UmbraSupabase.client.from("shop_items")
                .select() {
                    filter {
                        eq("type", "name_color")
                        eq("name", "Trainer White")
                    }
                }
                .decodeSingleOrNull<ShopItem>()
        } catch (e: Exception) {
            null
        }
    }
    
    // Adicionar item default de name_color ao inventário se não existir
    suspend fun ensureDefaultNameColorInInventory(userId: String): Boolean {
        return try {
            // Verificar se já tem algum name_color no inventário
            val existingColors = UmbraSupabase.client.from("inventory")
                .select(Columns.list("item_id")) {
                    filter {
                        eq("user_id", userId)
                        eq("category", "name_color")
                    }
                }
                .decodeList<Map<String, String>>()
            
            if (existingColors.isEmpty()) {
                // Adicionar Trainer White ao inventário
                UmbraSupabase.client.from("inventory")
                    .insert(mapOf(
                        "user_id" to userId,
                        "item_id" to "name_color_default",
                        "category" to "name_color"
                    ))
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}