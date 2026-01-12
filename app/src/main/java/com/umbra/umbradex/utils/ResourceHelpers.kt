package com.umbra.umbradex.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.umbra.umbradex.R

/**
 * Mapeia nomes de assets da base de dados para resource IDs do drawable.
 * 
 * A base de dados guarda nomes como "shop_common_greenhair.png" mas precisamos
 * de os converter para R.drawable.xxx
 * 
 * Retorna 0 se o recurso não existir (permite fallback visual no UI)
 */
@Composable
fun getAvatarResourceId(assetName: String): Int {
    val context = LocalContext.current
    
    // Limpar o nome do asset (remover .png, espaços, etc.)
    val cleanName = assetName
        .removeSuffix(".png")
        .removeSuffix(".jpg")
        .removeSuffix(".webp")
        .replace("-", "_")
        .replace(" ", "_")
        .lowercase()
        .trim()

    // Tenta encontrar o ID pelo nome limpo
    val resourceId = context.resources.getIdentifier(
        cleanName,
        "drawable",
        context.packageName
    )

    // Retorna o ID ou 0 (permite que o UI faça fallback visual apropriado)
    return resourceId
}

/**
 * Verifica se um drawable resource existe
 */
@Composable
fun hasDrawableResource(assetName: String): Boolean {
    return getAvatarResourceId(assetName) != 0
}

/**
 * Retorna um drawable fallback baseado no tipo de item
 */
fun getFallbackDrawable(itemType: String): Int {
    return when (itemType) {
        "skin" -> R.drawable.default_person
        "badge" -> R.drawable.ic_launcher_foreground
        else -> R.drawable.ic_launcher_foreground
    }
}