package com.umbra.umbradex.ui.teams

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.umbra.umbradex.data.model.Pokemon

@Composable
fun PokemonLevelDialog(
    pokemon: Pokemon,
    onDismiss: () -> Unit,
    onConfirm: (level: Int) -> Unit
) {
    var levelText by remember { mutableStateOf("1") }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Definir Nível do Pokémon") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Imagem do Pokémon
                AsyncImage(
                    model = pokemon.imageUrl,
                    contentDescription = pokemon.name,
                    modifier = Modifier.size(100.dp)
                )

                // Nome do Pokémon
                Text(
                    text = pokemon.capitalizedName(),
                    style = MaterialTheme.typography.titleMedium
                )

                // Campo de nível
                OutlinedTextField(
                    value = levelText,
                    onValueChange = { newValue ->
                        // Permite apenas números
                        if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                            levelText = newValue
                            showError = false

                            // Valida o nível (1-100)
                            val level = newValue.toIntOrNull()
                            if (level != null && (level < 1 || level > 100)) {
                                showError = true
                            }
                        }
                    },
                    label = { Text("Nível") },
                    placeholder = { Text("1-100") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    isError = showError,
                    supportingText = {
                        if (showError) {
                            Text(
                                text = "O nível deve ser entre 1 e 100",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val level = levelText.toIntOrNull()
                    if (level != null && level in 1..100) {
                        onConfirm(level)
                    } else {
                        showError = true
                    }
                },
                enabled = levelText.isNotBlank() && !showError
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}