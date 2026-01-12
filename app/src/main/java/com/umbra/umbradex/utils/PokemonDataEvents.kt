package com.umbra.umbradex.utils

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Singleton object que permite comunicação entre ViewModels
 * para atualizações em tempo real de dados de Pokémon.
 * 
 * Quando um Pokémon é adicionado à Living Dex ou definido como favorito,
 * este objeto emite eventos que outros ViewModels podem observar
 * para atualizar a sua UI imediatamente.
 */
object PokemonDataEvents {
    
    // Evento emitido quando um Pokémon é adicionado/removido da Living Dex
    private val _livingDexChanged = MutableSharedFlow<LivingDexEvent>(replay = 0)
    val livingDexChanged = _livingDexChanged.asSharedFlow()
    
    // Evento emitido quando o Pokémon favorito/partner é alterado
    private val _favoriteChanged = MutableSharedFlow<FavoriteEvent>(replay = 0)
    val favoriteChanged = _favoriteChanged.asSharedFlow()
    
    // Evento genérico para refresh completo (usado em casos especiais)
    private val _refreshAll = MutableSharedFlow<Unit>(replay = 0)
    val refreshAll = _refreshAll.asSharedFlow()
    
    // Evento emitido quando missões precisam ser atualizadas
    private val _missionProgressChanged = MutableSharedFlow<MissionEvent>(replay = 0)
    val missionProgressChanged = _missionProgressChanged.asSharedFlow()
    
    // Evento emitido quando uma equipa é criada/eliminada
    private val _teamChanged = MutableSharedFlow<TeamEvent>(replay = 0)
    val teamChanged = _teamChanged.asSharedFlow()
    
    // Evento emitido quando um item é comprado na loja
    private val _shopPurchase = MutableSharedFlow<ShopEvent>(replay = 0)
    val shopPurchase = _shopPurchase.asSharedFlow()
    
    // Evento emitido quando o perfil do utilizador é atualizado (gold, level, etc.)
    private val _profileUpdated = MutableSharedFlow<ProfileEvent>(replay = 0)
    val profileUpdated = _profileUpdated.asSharedFlow()
    
    // Evento emitido quando um item é equipado no inventário
    private val _inventoryChanged = MutableSharedFlow<InventoryEvent>(replay = 1)
    val inventoryChanged = _inventoryChanged.asSharedFlow()
    
    /**
     * Emite um evento indicando que um Pokémon foi adicionado à Living Dex
     */
    suspend fun notifyLivingDexAdded(pokemonId: Int, pokemonTypes: List<String> = emptyList()) {
        _livingDexChanged.emit(LivingDexEvent.Added(pokemonId, pokemonTypes))
        // Também notificar o sistema de missões
        _missionProgressChanged.emit(MissionEvent.ProgressUpdated)
    }
    
    /**
     * Emite um evento indicando que um Pokémon foi removido da Living Dex
     */
    suspend fun notifyLivingDexRemoved(pokemonId: Int) {
        _livingDexChanged.emit(LivingDexEvent.Removed(pokemonId))
    }
    
    /**
     * Emite um evento indicando que o Pokémon favorito/partner foi alterado
     */
    suspend fun notifyFavoriteChanged(pokemonId: Int) {
        _favoriteChanged.emit(FavoriteEvent.Changed(pokemonId))
        _missionProgressChanged.emit(MissionEvent.ProgressUpdated)
    }
    
    /**
     * Emite um evento indicando que um Pokémon foi removido dos favoritos
     */
    suspend fun notifyFavoriteRemoved(pokemonId: Int) {
        _favoriteChanged.emit(FavoriteEvent.Removed(pokemonId))
    }
    
    /**
     * Emite um evento indicando que uma missão foi completada e recompensa resgatada
     */
    suspend fun notifyMissionClaimed(missionId: Int, goldReward: Int, xpReward: Int) {
        _missionProgressChanged.emit(MissionEvent.Claimed(missionId, goldReward, xpReward))
    }
    
    /**
     * Emite um evento indicando que o progresso de missões foi atualizado
     */
    suspend fun notifyMissionProgressUpdated() {
        _missionProgressChanged.emit(MissionEvent.ProgressUpdated)
    }
    
    /**
     * Emite um evento indicando que uma equipa foi criada
     */
    suspend fun notifyTeamCreated() {
        _teamChanged.emit(TeamEvent.Created)
        _missionProgressChanged.emit(MissionEvent.ProgressUpdated)
    }
    
    /**
     * Emite um evento indicando que uma equipa foi eliminada
     */
    suspend fun notifyTeamDeleted() {
        _teamChanged.emit(TeamEvent.Deleted)
    }
    
    /**
     * Emite um evento indicando que um item foi comprado na loja
     */
    suspend fun notifyShopPurchase(category: String, goldSpent: Int = 0) {
        _shopPurchase.emit(ShopEvent.ItemPurchased(category))
        _missionProgressChanged.emit(MissionEvent.ProgressUpdated)
        // Notificar que o gold mudou
        if (goldSpent > 0) {
            _profileUpdated.emit(ProfileEvent.GoldChanged(-goldSpent))
        }
    }
    
    /**
     * Emite um evento indicando que o perfil foi atualizado
     */
    suspend fun notifyProfileUpdated(newGold: Long? = null, newLevel: Int? = null) {
        _profileUpdated.emit(ProfileEvent.Updated(newGold, newLevel))
    }
    
    /**
     * Emite um evento indicando que o gold mudou
     */
    suspend fun notifyGoldChanged(amount: Int) {
        _profileUpdated.emit(ProfileEvent.GoldChanged(amount))
    }
    
    /**
     * Emite um evento indicando que o utilizador subiu de nível
     * @param newLevel O novo nível alcançado
     * @param newTitle O novo título desbloqueado, se houver (triggers title sound)
     */
    suspend fun notifyLevelUp(newLevel: Int, newTitle: String? = null) {
        _profileUpdated.emit(ProfileEvent.LevelUp(newLevel, newTitle))
    }
    
    /**
     * Solicita um refresh completo de todos os dados
     */
    suspend fun requestFullRefresh() {
        _refreshAll.emit(Unit)
    }
    
    /**
     * Emite um evento indicando que uma skin foi equipada
     */
    suspend fun notifySkinEquipped(skinName: String) {
        _inventoryChanged.emit(InventoryEvent.SkinEquipped(skinName))
    }
    
    /**
     * Emite um evento indicando que um badge foi equipado
     */
    suspend fun notifyBadgeEquipped(badgeName: String) {
        _inventoryChanged.emit(InventoryEvent.BadgeEquipped(badgeName))
    }
    
    /**
     * Emite um evento indicando que um tema foi equipado
     */
    suspend fun notifyThemeEquipped(themeName: String, themeColors: List<String>) {
        _inventoryChanged.emit(InventoryEvent.ThemeEquipped(themeName, themeColors))
    }
    
    /**
     * Emite um evento indicando que uma cor de nome foi equipada
     */
    suspend fun notifyNameColorEquipped(colorName: String, colors: List<String>) {
        _inventoryChanged.emit(InventoryEvent.NameColorEquipped(colorName, colors))
    }
    
    /**
     * Emite um evento indicando que um título foi equipado
     */
    suspend fun notifyTitleEquipped(titleName: String) {
        _inventoryChanged.emit(InventoryEvent.TitleEquipped(titleName))
    }
    
    /**
     * Emite um evento indicando que o inventário precisa ser recarregado
     */
    suspend fun notifyInventoryRefreshNeeded() {
        _inventoryChanged.emit(InventoryEvent.RefreshNeeded)
    }
}

/**
 * Eventos relacionados com a Living Dex
 */
sealed class LivingDexEvent {
    data class Added(val pokemonId: Int, val pokemonTypes: List<String> = emptyList()) : LivingDexEvent()
    data class Removed(val pokemonId: Int) : LivingDexEvent()
}

/**
 * Eventos relacionados com favoritos
 */
sealed class FavoriteEvent {
    data class Changed(val pokemonId: Int) : FavoriteEvent()
    data class Removed(val pokemonId: Int) : FavoriteEvent()
}

/**
 * Eventos relacionados com missões
 */
sealed class MissionEvent {
    object ProgressUpdated : MissionEvent()
    data class Claimed(val missionId: Int, val goldReward: Int, val xpReward: Int) : MissionEvent()
}

/**
 * Eventos relacionados com equipas
 */
sealed class TeamEvent {
    object Created : TeamEvent()
    object Deleted : TeamEvent()
}

/**
 * Eventos relacionados com compras na loja
 */
sealed class ShopEvent {
    data class ItemPurchased(val category: String) : ShopEvent()
}

/**
 * Eventos relacionados com o perfil do utilizador
 */
sealed class ProfileEvent {
    data class Updated(val newGold: Long? = null, val newLevel: Int? = null) : ProfileEvent()
    data class GoldChanged(val amount: Int) : ProfileEvent()
    data class LevelUp(val newLevel: Int, val newTitle: String? = null) : ProfileEvent()
}

/**
 * Eventos relacionados com itens equipados (inventário)
 */
sealed class InventoryEvent {
    data class SkinEquipped(val skinName: String) : InventoryEvent()
    data class BadgeEquipped(val badgeName: String) : InventoryEvent()
    data class ThemeEquipped(val themeName: String, val themeColors: List<String>) : InventoryEvent()
    data class NameColorEquipped(val colorName: String, val colors: List<String>) : InventoryEvent()
    data class TitleEquipped(val titleName: String) : InventoryEvent()
    object RefreshNeeded : InventoryEvent()
}
