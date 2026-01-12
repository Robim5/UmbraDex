package com.umbra.umbradex.ui.teams

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.umbra.umbradex.data.model.Team
import com.umbra.umbradex.data.repository.TeamRepository
import com.umbra.umbradex.data.repository.UserRepository
import com.umbra.umbradex.utils.PokemonDataEvents
import com.umbra.umbradex.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.widget.Toast
import com.umbra.umbradex.data.model.Pokemon
import com.umbra.umbradex.data.repository.PokemonRepository
import com.umbra.umbradex.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.net.URL


class TeamsViewModel(
    private val teamRepository: TeamRepository = TeamRepository()
) : ViewModel() {

    // Estados das equipas
    private val _teams = MutableStateFlow<List<Team>>(emptyList())
    val teams: StateFlow<List<Team>> = _teams.asStateFlow()

    // Estados de UI
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadTeams()
    }

    /**
     * Carrega todas as equipas do utilizador
     */
    private fun loadTeams() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            teamRepository.getUserTeams().collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        _teams.value = resource.data.sortedBy { it.createdAt }
                        _isLoading.value = false
                    }
                    is Resource.Error -> {
                        _error.value = resource.message
                        _isLoading.value = false
                    }
                    is Resource.Loading -> {
                        _isLoading.value = true
                    }
                }
            }
        }
    }

    /**
     * Cria uma nova equipa com cores gradiente aleatórias
     */
    fun createTeam(name: String, region: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                // Gerar cores aleatórias para o gradiente
                val gradientColors = generateRandomGradient()

                val result = teamRepository.createTeam(
                    name = name,
                    region = region,
                    gradientColors = gradientColors
                )
                
                when (result) {
                    is Resource.Success -> {
                        loadTeams() // Recarrega as equipas
                        // Notificar sistema de missões sobre criação de equipa
                        PokemonDataEvents.notifyTeamCreated()
                    }
                    is Resource.Error -> {
                        _error.value = result.message
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Falha ao criar equipa"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Adiciona ou substitui um Pokémon num slot específico da equipa
     */
    fun addOrReplacePokemonInTeam(
        teamId: String,
        slotIndex: Int,
        pokemon: Pokemon,
        level: Int
    ) {
        viewModelScope.launch {
            try {
                teamRepository.addOrReplacePokemonInTeam(
                    teamId = teamId,
                    slotIndex = slotIndex,
                    pokemonId = pokemon.id,
                    pokemonName = pokemon.name,
                    pokemonImageUrl = pokemon.imageUrl,
                    pokemonTypes = pokemon.types,
                    level = level
                )

                loadTeams() // Recarrega as equipas
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to add Pokémon to team"
            }
        }
    }

    /**
     * Remove um Pokémon de um slot específico
     */
    fun removePokemonFromSlot(teamId: String, slotIndex: Int) {
        viewModelScope.launch {
            try {
                teamRepository.removePokemonFromSlot(
                    teamId = teamId,
                    slotIndex = slotIndex
                )

                loadTeams() // Recarrega as equipas
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to remove Pokémon from team"
            }
        }
    }

    /**
     * Atualiza o nome de uma equipa
     */
    fun updateTeamName(teamId: String, newName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                teamRepository.updateTeamName(
                    teamId = teamId,
                    newName = newName
                )

                loadTeams() // Recarrega as equipas
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to update team name"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Elimina uma equipa
     */
    fun deleteTeam(teamId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                teamRepository.deleteTeam(teamId)
                loadTeams() // Recarrega as equipas
                // Notificar sistema de missões sobre eliminação de equipa
                PokemonDataEvents.notifyTeamDeleted()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to delete team"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Faz download do cartão da equipa como imagem PNG
     */
    fun downloadTeamCard(context: Context, team: Team) {
        viewModelScope.launch {
            try {
                Toast.makeText(
                    context,
                    "A gerar cartão da equipa...",
                    Toast.LENGTH_SHORT
                ).show()

                // Gerar bitmap do cartão da equipa
                val bitmap = withContext(Dispatchers.IO) {
                    generateTeamCardBitmap(team)
                }
                
                // Sanitizar o nome do ficheiro
                val sanitizedName = team.name.replace(Regex("[^a-zA-Z0-9]"), "_")
                val fileName = "UmbraDex_Team_${sanitizedName}_${System.currentTimeMillis()}"
                
                // Guardar na galeria
                ImageUtils.saveBitmapToGallery(context, bitmap, fileName)

            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Erro ao descarregar cartão da equipa: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    /**
     * Gera um Bitmap bonito do cartão da equipa
     */
    private suspend fun generateTeamCardBitmap(team: Team): Bitmap {
        val cardWidth = 1080 // Full HD width
        val cardHeight = 1920 // Aspect ratio for social media stories
        
        val bitmap = Bitmap.createBitmap(cardWidth, cardHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Parse gradient colors
        val gradientColors = team.gradientColors.map { hexColor ->
            try {
                android.graphics.Color.parseColor(hexColor)
            } catch (e: Exception) {
                android.graphics.Color.parseColor("#667eea")
            }
        }.toIntArray()
        
        // Background gradient
        val gradient = LinearGradient(
            0f, 0f, cardWidth.toFloat(), cardHeight.toFloat(),
            gradientColors.getOrElse(0) { android.graphics.Color.parseColor("#667eea") },
            gradientColors.getOrElse(1) { android.graphics.Color.parseColor("#764ba2") },
            Shader.TileMode.CLAMP
        )
        
        val bgPaint = Paint().apply {
            shader = gradient
        }
        canvas.drawRect(0f, 0f, cardWidth.toFloat(), cardHeight.toFloat(), bgPaint)
        
        // Title paint
        val titlePaint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 72f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        
        // Subtitle paint
        val subtitlePaint = Paint().apply {
            color = android.graphics.Color.argb(200, 255, 255, 255)
            textSize = 36f
            isAntiAlias = true
        }
        
        // Draw team name
        val teamNameX = 60f
        var currentY = 120f
        canvas.drawText(team.name, teamNameX, currentY, titlePaint)
        
        // Draw region
        currentY += 60f
        canvas.drawText("📍 ${team.region.uppercase()}", teamNameX, currentY, subtitlePaint)
        
        // Draw separator line
        currentY += 60f
        val linePaint = Paint().apply {
            color = android.graphics.Color.argb(100, 255, 255, 255)
            strokeWidth = 2f
        }
        canvas.drawLine(60f, currentY, cardWidth - 60f, currentY, linePaint)
        
        // Draw Pokémon slots
        currentY += 40f
        val slotHeight = 260f
        val slotPadding = 20f
        
        for (slotIndex in 0..5) {
            val pokemon = team.pokemon.find { it.slotIndex == slotIndex }
            val slotTop = currentY + (slotIndex * (slotHeight + slotPadding))
            val slotRect = RectF(60f, slotTop, cardWidth - 60f, slotTop + slotHeight)
            
            if (pokemon != null && pokemon.types.isNotEmpty()) {
                // Slot com gradiente baseado no tipo do Pokémon
                val typeColors = getTypeGradientColorsForBitmap(pokemon.types)
                val typeGradient = LinearGradient(
                    slotRect.left, slotRect.top,
                    slotRect.right, slotRect.bottom,
                    typeColors.first,
                    typeColors.second,
                    Shader.TileMode.CLAMP
                )
                val slotPaint = Paint().apply {
                    shader = typeGradient
                    isAntiAlias = true
                }
                canvas.drawRoundRect(slotRect, 24f, 24f, slotPaint)
            } else if (pokemon != null) {
                // Pokémon sem tipos definidos - usar cor semi-transparente
                val slotPaint = Paint().apply {
                    color = android.graphics.Color.argb(100, 255, 255, 255)
                    isAntiAlias = true
                }
                canvas.drawRoundRect(slotRect, 24f, 24f, slotPaint)
            } else {
                // Slot vazio
                val slotPaint = Paint().apply {
                    color = android.graphics.Color.argb(30, 255, 255, 255)
                    isAntiAlias = true
                }
                canvas.drawRoundRect(slotRect, 24f, 24f, slotPaint)
            }
            
            if (pokemon != null) {
                // Draw Pokémon info
                val pokemonNamePaint = Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 48f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }
                
                val pokemonIdPaint = Paint().apply {
                    color = android.graphics.Color.argb(220, 255, 255, 255)
                    textSize = 32f
                    isAntiAlias = true
                }
                
                // Pokemon name
                val displayName = pokemon.name.ifBlank { "Pokemon #${pokemon.pokemonId}" }
                    .replaceFirstChar { it.uppercase() }
                canvas.drawText(displayName, 220f, slotTop + 80f, pokemonNamePaint)
                
                // Pokemon number
                val formattedId = "#${pokemon.pokemonId.toString().padStart(3, '0')}"
                canvas.drawText(formattedId, 220f, slotTop + 130f, pokemonIdPaint)
                
                // Level badge
                val levelPaint = Paint().apply {
                    color = android.graphics.Color.argb(180, 0, 0, 0)
                    isAntiAlias = true
                }
                val levelRect = RectF(cardWidth - 200f, slotTop + 40f, cardWidth - 80f, slotTop + 100f)
                canvas.drawRoundRect(levelRect, 16f, 16f, levelPaint)
                
                val levelTextPaint = Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 36f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("Lv.${pokemon.level}", cardWidth - 140f, slotTop + 80f, levelTextPaint)
                
                // Try to load and draw Pokémon image
                try {
                    val imageUrl = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/${pokemon.pokemonId}.png"
                    val pokemonBitmap = withContext(Dispatchers.IO) {
                        loadBitmapFromUrl(imageUrl)
                    }
                    pokemonBitmap?.let {
                        val scaledBitmap = Bitmap.createScaledBitmap(it, 160, 160, true)
                        canvas.drawBitmap(scaledBitmap, 80f, slotTop + 50f, null)
                    }
                } catch (e: Exception) {
                    // Ignore image loading errors
                }
                
                // Draw types if available
                if (pokemon.types.isNotEmpty()) {
                    val typePaint = Paint().apply {
                        color = android.graphics.Color.argb(150, 0, 0, 0)
                        isAntiAlias = true
                    }
                    val typeTextPaint = Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 24f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        isAntiAlias = true
                    }
                    
                    var typeX = 220f
                    pokemon.types.take(2).forEach { type ->
                        val typeWidth = typeTextPaint.measureText(type.uppercase()) + 24f
                        val typeRect = RectF(typeX, slotTop + 150f, typeX + typeWidth, slotTop + 190f)
                        canvas.drawRoundRect(typeRect, 8f, 8f, typePaint)
                        canvas.drawText(type.uppercase(), typeX + 12f, slotTop + 178f, typeTextPaint)
                        typeX += typeWidth + 12f
                    }
                }
            } else {
                // Empty slot
                val emptyTextPaint = Paint().apply {
                    color = android.graphics.Color.argb(120, 255, 255, 255)
                    textSize = 36f
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("Slot Vazio ${slotIndex + 1}", cardWidth / 2f, slotTop + slotHeight / 2f + 12f, emptyTextPaint)
            }
        }
        
        // Footer with UmbraDex branding
        val footerPaint = Paint().apply {
            color = android.graphics.Color.argb(180, 255, 255, 255)
            textSize = 28f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Criado com UmbraDex", cardWidth / 2f, cardHeight - 60f, footerPaint)
        
        return bitmap
    }
    
    /**
     * Retorna cores de gradiente baseadas no tipo do Pokémon para usar no Bitmap
     */
    private fun getTypeGradientColorsForBitmap(types: List<String>): Pair<Int, Int> {
        val typeColorMap = mapOf(
            "normal" to Pair(0xFFCDCDBE.toInt(), 0xFF8A8A6E.toInt()),
            "fire" to Pair(0xFFFFB366.toInt(), 0xFFCC5500.toInt()),
            "water" to Pair(0xFF99BBFF.toInt(), 0xFF3366CC.toInt()),
            "electric" to Pair(0xFFFFE066.toInt(), 0xFFCCAA00.toInt()),
            "grass" to Pair(0xFFA3D977.toInt(), 0xFF4E8C2A.toInt()),
            "ice" to Pair(0xFFBBEEEE.toInt(), 0xFF66B2B2.toInt()),
            "fighting" to Pair(0xFFE06060.toInt(), 0xFF8C1818.toInt()),
            "poison" to Pair(0xFFCC77CC.toInt(), 0xFF702070.toInt()),
            "ground" to Pair(0xFFF0D898.toInt(), 0xFFB08828.toInt()),
            "flying" to Pair(0xFFCCBBFF.toInt(), 0xFF7766CC.toInt()),
            "psychic" to Pair(0xFFFF99AA.toInt(), 0xFFCC3366.toInt()),
            "bug" to Pair(0xFFCCDD55.toInt(), 0xFF808C10.toInt()),
            "rock" to Pair(0xFFDDCC77.toInt(), 0xFF8C7C18.toInt()),
            "ghost" to Pair(0xFF9988BB.toInt(), 0xFF4C3C6C.toInt()),
            "dragon" to Pair(0xFF9977FF.toInt(), 0xFF4C18CC.toInt()),
            "dark" to Pair(0xFF998877.toInt(), 0xFF4C3C30.toInt()),
            "steel" to Pair(0xFFD8D8E8.toInt(), 0xFF8888A8.toInt()),
            "fairy" to Pair(0xFFFFBBCC.toInt(), 0xFFCC7788.toInt())
        )
        
        return when {
            types.isEmpty() -> Pair(0xFF888888.toInt(), 0xFF444444.toInt())
            types.size == 1 -> {
                typeColorMap[types[0].lowercase()] ?: Pair(0xFF888888.toInt(), 0xFF444444.toInt())
            }
            else -> {
                // Para 2 tipos, pegar a cor principal de cada
                val color1 = getTypeMainColor(types[0])
                val color2 = getTypeMainColor(types[1])
                Pair(color1, color2)
            }
        }
    }
    
    /**
     * Retorna a cor principal de um tipo
     */
    private fun getTypeMainColor(type: String): Int {
        return when (type.lowercase()) {
            "normal" -> 0xFFA8A878.toInt()
            "fire" -> 0xFFF08030.toInt()
            "water" -> 0xFF6890F0.toInt()
            "electric" -> 0xFFF8D030.toInt()
            "grass" -> 0xFF78C850.toInt()
            "ice" -> 0xFF98D8D8.toInt()
            "fighting" -> 0xFFC03028.toInt()
            "poison" -> 0xFFA040A0.toInt()
            "ground" -> 0xFFE0C068.toInt()
            "flying" -> 0xFFA890F0.toInt()
            "psychic" -> 0xFFF85888.toInt()
            "bug" -> 0xFFA8B820.toInt()
            "rock" -> 0xFFB8A038.toInt()
            "ghost" -> 0xFF705898.toInt()
            "dragon" -> 0xFF7038F8.toInt()
            "dark" -> 0xFF705848.toInt()
            "steel" -> 0xFFB8B8D0.toInt()
            "fairy" -> 0xFFEE99AC.toInt()
            else -> 0xFF888888.toInt()
        }
    }
    
    /**
     * Carrega um Bitmap a partir de uma URL
     */
    private fun loadBitmapFromUrl(urlString: String): Bitmap? {
        return try {
            val url = URL(urlString)
            val connection = url.openConnection()
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            val inputStream = connection.getInputStream()
            android.graphics.BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Limpa a mensagem de erro
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Recarrega os dados
     */
    fun refresh() {
        loadTeams()
    }

    /**
     * Gera um gradiente de 2 cores aleatórias em formato hex
     */
    private fun generateRandomGradient(): List<String> {
        val gradients = listOf(
            // Gradientes predefinidos bonitos
            listOf("#667eea", "#764ba2"), // Roxo
            listOf("#f093fb", "#f5576c"), // Rosa
            listOf("#4facfe", "#00f2fe"), // Azul
            listOf("#43e97b", "#38f9d7"), // Verde
            listOf("#fa709a", "#fee140"), // Laranja-Rosa
            listOf("#30cfd0", "#330867"), // Azul-Roxo
            listOf("#a8edea", "#fed6e3"), // Pastel
            listOf("#ff9a9e", "#fecfef"), // Rosa suave
            listOf("#fbc2eb", "#a6c1ee"), // Lavanda
            listOf("#fdcbf1", "#e6dee9"), // Rosa claro
            listOf("#a1c4fd", "#c2e9fb"), // Azul claro
            listOf("#ffecd2", "#fcb69f"), // Pêssego
            listOf("#ff6e7f", "#bfe9ff"), // Rosa-Azul
            listOf("#e0c3fc", "#8ec5fc"), // Roxo-Azul
            listOf("#f093fb", "#f5576c"), // Magenta
            listOf("#4facfe", "#00f2fe"), // Ciano
            listOf("#43e97b", "#38f9d7"), // Turquesa
            listOf("#fa709a", "#fee140"), // Sunset
            listOf("#30cfd0", "#330867"), // Deep Blue
            listOf("#a8edea", "#fed6e3")  // Cotton Candy
        )

        return gradients[Random.nextInt(gradients.size)]
    }
}