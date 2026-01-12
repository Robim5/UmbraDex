package com.umbra.umbradex.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.umbra.umbradex.data.model.Mission
import com.umbra.umbradex.data.model.MissionProgress
import com.umbra.umbradex.ui.theme.UmbraAccent
import com.umbra.umbradex.ui.theme.UmbraGold
import com.umbra.umbradex.ui.theme.UmbraPrimary
import com.umbra.umbradex.ui.theme.UmbraSurface
import com.umbra.umbradex.utils.RarityUtils

@Composable
fun MissionCard(
    mission: Mission,
    progress: MissionProgress?,
    onClaim: () -> Unit
) {
    // Calcular percentagem (0.0 a 1.0)
    val current = progress?.currentValue ?: 0
    val target = mission.requirementValue
    val percentage = (current.toFloat() / target.toFloat()).coerceIn(0f, 1f)
    val isCompleted = percentage >= 1f
    val isClaimed = progress?.status == "completed"

    // Se já foi reclamado e estamos na aba ativa, não mostramos (lógica do ecrã principal)
    // Mas o componente deve saber renderizar os 3 estados: Em curso, Pronto a reclamar, Reclamado.

    val rarityColor = RarityUtils.getColor(mission.rarity)

    // Animação da barra
    val animatedProgress by animateFloatAsState(targetValue = percentage, label = "progress")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(1.dp, if (isCompleted && !isClaimed) MaterialTheme.colorScheme.secondary else Color.Transparent, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // --- CABEÇALHO (Título e Raridade) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = mission.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Surface(
                    color = rarityColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = mission.rarity.uppercase(),
                        color = rarityColor,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = mission.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- BARRA DE PROGRESSO ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${(percentage * 100).toInt()}%",
                    color = if (isCompleted) MaterialTheme.colorScheme.secondary else Color.Gray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Text(
                    text = "$current / $target",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            LinearProgressIndicator(
                progress = animatedProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (isCompleted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                trackColor = Color.Black.copy(alpha = 0.3f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- RODAPÉ (Recompensas e Botão) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Recompensas
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RewardBadge(text = "${mission.goldReward} G", color = UmbraGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    RewardBadge(text = "${mission.xpReward} XP", color = MaterialTheme.colorScheme.primary)
                }

                // Botão de Ação
                if (isCompleted && !isClaimed) {
                    Button(
                        onClick = onClaim,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("RECLAMAR", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                    }
                } else if (isClaimed) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Text(" Feito", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun RewardBadge(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}