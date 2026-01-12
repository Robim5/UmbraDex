package com.umbra.umbradex.ui.auth

import android.app.DatePickerDialog
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.umbra.umbradex.ui.components.UmbraButton
import com.umbra.umbradex.ui.components.UmbraTextField
import com.umbra.umbradex.ui.navigation.Screen
import com.umbra.umbradex.ui.theme.UmbraBackground
import com.umbra.umbradex.ui.theme.UmbraPrimary
import com.umbra.umbradex.ui.theme.UmbraSurface
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.umbra.umbradex.R
import com.umbra.umbradex.utils.getAvatarResourceId
import com.umbra.umbradex.utils.getTypeColor
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.viewmodel.compose.viewModel
import com.umbra.umbradex.utils.Resource
import java.util.Calendar

@Composable
fun OnboardingScreen(
    navController: NavController,
    viewModel: AuthViewModel
) {
    val authState by viewModel.authState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val onboardingData by viewModel.onboardingData.collectAsState()
    
    // Local state for onboarding steps
    var currentStep by remember { mutableStateOf(0) }
    var birthDate by remember { mutableStateOf("") }
    var pokemonKnowledge by remember { mutableStateOf("intermediate") }
    var favoriteType by remember { mutableStateOf("fire") }
    var selectedAvatar by remember { mutableStateOf("standard_male1") }
    var selectedStarter by remember { mutableStateOf(1) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Observar resultado de signup
    LaunchedEffect(authState) {
        when (authState) {
            is Resource.Success -> {
                // Clear any previous errors
                errorMessage = null
                // Navigate to Home after successful signup
                navController.navigate(Screen.Home.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
            is Resource.Error -> {
                errorMessage = (authState as Resource.Error).message
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1A2E),
                        Color(0xFF16213E)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(24.dp)
        ) {
            // Header com Progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (currentStep > 0) {
                            currentStep--
                        } else {
                            navController.popBackStack()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Voltar",
                        tint = Color.White
                    )
                }

                Text(
                    text = "Passo ${currentStep + 1} de 5",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = (currentStep + 1) / 5f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = Color(0xFF9C27B0),
                trackColor = Color.White.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Conteúdo dinâmico
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                when (currentStep) {
                    0 -> BirthDateStep(
                        birthDate = birthDate,
                        onBirthDateChange = { birthDate = it }
                    )
                    1 -> PokemonKnowledgeStep(
                        selected = pokemonKnowledge,
                        onSelected = { pokemonKnowledge = it }
                    )
                    2 -> FavoriteTypeStep(
                        selected = favoriteType,
                        onSelected = { favoriteType = it }
                    )
                    3 -> AvatarSelectionStep(
                        selected = selectedAvatar,
                        onSelected = { selectedAvatar = it }
                    )
                    4 -> StarterSelectionStep(
                        selected = selectedStarter,
                        onSelected = { selectedStarter = it }
                    )
                }
            }

            // Error message display
            errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFFF5252).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = error,
                        color = Color(0xFFFF5252),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Botão Next/Finish
            Button(
                onClick = {
                    errorMessage = null // Clear previous errors
                    if (currentStep < 4) {
                        currentStep++
                    } else {
                        // Última etapa - fazer signup
                        viewModel.updateOnboardingData {
                            copy(
                                birthDate = birthDate,
                                pokemonKnowledge = pokemonKnowledge,
                                favoriteType = favoriteType,
                                avatar = selectedAvatar,
                                starterId = selectedStarter
                            )
                        }
                        viewModel.signup()
                    }
                },
                enabled = !isLoading && when (currentStep) {
                    0 -> birthDate.isNotEmpty()
                    4 -> selectedStarter > 0
                    else -> true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF9C27B0),
                    disabledContainerColor = Color(0xFF9C27B0).copy(alpha = 0.4f)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                } else {
                    Text(
                        text = if (currentStep < 4) "Seguinte" else "Concluir",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun BirthDateStep(
    birthDate: String,
    onBirthDateChange: (String) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    
    // Parse existing date if available
    val initialYear = if (birthDate.isNotEmpty()) {
        birthDate.split("-").getOrNull(0)?.toIntOrNull() ?: (calendar.get(Calendar.YEAR) - 18)
    } else {
        calendar.get(Calendar.YEAR) - 18
    }
    val initialMonth = if (birthDate.isNotEmpty()) {
        (birthDate.split("-").getOrNull(1)?.toIntOrNull() ?: 1) - 1
    } else {
        0
    }
    val initialDay = if (birthDate.isNotEmpty()) {
        birthDate.split("-").getOrNull(2)?.toIntOrNull() ?: 1
    } else {
        1
    }
    
    // Format display date
    val displayDate = if (birthDate.isNotEmpty()) {
        try {
            val parts = birthDate.split("-")
            val day = parts.getOrNull(2)?.toIntOrNull() ?: 1
            val month = parts.getOrNull(1)?.toIntOrNull() ?: 1
            val year = parts.getOrNull(0)?.toIntOrNull() ?: 2000
            val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", 
                                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            "$day ${monthNames.getOrElse(month - 1) { "Jan" }} $year"
        } catch (e: Exception) {
            birthDate
        }
    } else {
        "Seleciona a tua data de nascimento"
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "🎂",
            fontSize = 64.sp
        )

        Text(
            text = "Quando nasceste?",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Precisamos disto para garantir que tens idade suficiente para jogar",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Date Picker Button
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            val formattedDate = String.format(
                                "%04d-%02d-%02d",
                                year,
                                month + 1,
                                dayOfMonth
                            )
                            onBirthDateChange(formattedDate)
                        },
                        initialYear,
                        initialMonth,
                        initialDay
                    ).apply {
                        datePicker.maxDate = System.currentTimeMillis()
                        // Set min date to 100 years ago
                        val minCal = Calendar.getInstance()
                        minCal.add(Calendar.YEAR, -100)
                        datePicker.minDate = minCal.timeInMillis
                    }.show()
                },
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.1f),
            border = androidx.compose.foundation.BorderStroke(
                2.dp,
                if (birthDate.isNotEmpty()) Color(0xFF9C27B0) else Color.White.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayDate,
                    fontSize = 18.sp,
                    color = if (birthDate.isNotEmpty()) Color.White else Color.White.copy(alpha = 0.5f)
                )
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Selecionar data",
                    tint = Color(0xFF9C27B0)
                )
            }
        }
        
        if (birthDate.isNotEmpty()) {
            Text(
                text = "✓ Data selecionada",
                fontSize = 14.sp,
                color = Color(0xFF4CAF50),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun PokemonKnowledgeStep(
    selected: String,
    onSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "🎓",
            fontSize = 64.sp
        )

        Text(
            text = "Qual é o teu conhecimento sobre Pokémon?",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        listOf(
            "beginner" to "Acabei de começar a minha jornada",
            "intermediate" to "Sei o básico",
            "expert" to "Gotta catch 'em all!"
        ).forEach { (level, description) ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelected(level) },
                shape = RoundedCornerShape(12.dp),
                color = if (selected == level)
                    Color(0xFF9C27B0).copy(alpha = 0.3f)
                else
                    Color.White.copy(alpha = 0.1f),
                border = androidx.compose.foundation.BorderStroke(
                    2.dp,
                    if (selected == level)
                        Color(0xFF9C27B0)
                    else
                        Color.White.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = level.replaceFirstChar { it.uppercase() },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = description,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun FavoriteTypeStep(
    selected: String,
    onSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "⚡",
            fontSize = 64.sp
        )

        Text(
            text = "Qual é o teu tipo favorito?",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // All 18 Pokémon types
        val types = listOf(
            "normal", "fire", "water", "electric", "grass", "ice",
            "fighting", "poison", "ground", "flying", "psychic", "bug",
            "rock", "ghost", "dragon", "dark", "steel", "fairy"
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp)
        ) {
            items(types) { type ->
                val typeColor = getTypeColor(type)
                Surface(
                    modifier = Modifier
                        .aspectRatio(1.3f)
                        .clickable { onSelected(type) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected == type)
                        typeColor.copy(alpha = 0.4f)
                    else
                        typeColor.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (selected == type) 3.dp else 1.dp,
                        color = if (selected == type)
                            typeColor
                        else
                            typeColor.copy(alpha = 0.5f)
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = type.replaceFirstChar { it.uppercase() },
                            fontWeight = if (selected == type) FontWeight.Bold else FontWeight.Medium,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AvatarSelectionStep(
    selected: String,
    onSelected: (String) -> Unit
) {
    var selectedGender by remember { mutableStateOf("male") }
    
    val maleAvatars = listOf(
        "standard_male1", "standard_male2", "standard_male3", "standard_male4", "standard_male5"
    )
    val femaleAvatars = listOf(
        "standard_female1", "standard_female2", "standard_female3", "standard_female4", "standard_female5"
    )
    
    val avatars = if (selectedGender == "male") maleAvatars else femaleAvatars

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "👤",
            fontSize = 64.sp
        )

        Text(
            text = "Escolhe o teu avatar",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))
        
        // Gender selector
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier
                    .clickable { selectedGender = "male" },
                shape = RoundedCornerShape(12.dp),
                color = if (selectedGender == "male") Color(0xFF9C27B0).copy(alpha = 0.3f) 
                        else Color.White.copy(alpha = 0.1f),
                border = androidx.compose.foundation.BorderStroke(
                    2.dp,
                    if (selectedGender == "male") Color(0xFF9C27B0) else Color.White.copy(alpha = 0.3f)
                )
            ) {
                Text(
                    text = "♂ Masculino",
                    color = Color.White,
                    fontWeight = if (selectedGender == "male") FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }
            
            Surface(
                modifier = Modifier
                    .clickable { selectedGender = "female" },
                shape = RoundedCornerShape(12.dp),
                color = if (selectedGender == "female") Color(0xFFE91E63).copy(alpha = 0.3f) 
                        else Color.White.copy(alpha = 0.1f),
                border = androidx.compose.foundation.BorderStroke(
                    2.dp,
                    if (selectedGender == "female") Color(0xFFE91E63) else Color.White.copy(alpha = 0.3f)
                )
            ) {
                Text(
                    text = "♀ Feminino",
                    color = Color.White,
                    fontWeight = if (selectedGender == "female") FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // First row: 3 avatars
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
        ) {
            avatars.take(3).forEach { avatar ->
                AvatarItem(
                    avatar = avatar,
                    isSelected = selected == avatar,
                    onSelected = onSelected
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Second row: 2 avatars centered
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
        ) {
            avatars.drop(3).forEach { avatar ->
                AvatarItem(
                    avatar = avatar,
                    isSelected = selected == avatar,
                    onSelected = onSelected
                )
            }
        }
    }
}

@Composable
private fun AvatarItem(
    avatar: String,
    isSelected: Boolean,
    onSelected: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .size(90.dp)
            .clickable { onSelected(avatar) },
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            3.dp,
            if (isSelected)
                Color(0xFF9C27B0)
            else
                Color.White.copy(alpha = 0.3f)
        )
    ) {
        Image(
            painter = painterResource(id = getAvatarResourceId(avatar)),
            contentDescription = avatar,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun StarterSelectionStep(
    selected: Int,
    onSelected: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "⭐",
            fontSize = 64.sp
        )

        Text(
            text = "Escolhe o teu inicial!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        val starters = listOf(
            1 to "Bulbasaur",
            4 to "Charmander",
            7 to "Squirtle"
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            starters.forEach { (id, name) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onSelected(id) }
                ) {
                    Surface(
                        modifier = Modifier.size(120.dp),
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(
                            4.dp,
                            if (selected == id)
                                Color(0xFF9C27B0)
                            else
                                Color.White.copy(alpha = 0.3f)
                        ),
                        color = Color.White.copy(alpha = 0.1f)
                    ) {
                        AsyncImage(
                            model = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$id.png",
                            contentDescription = name,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = name,
                        fontWeight = if (selected == id) FontWeight.Bold else FontWeight.Normal,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}