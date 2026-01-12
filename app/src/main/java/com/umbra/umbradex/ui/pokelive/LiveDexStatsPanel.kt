package com.umbra.umbradex.ui.pokelive

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.umbra.umbradex.ui.components.AnimatedCircularChart
import com.umbra.umbradex.ui.theme.UmbraAccent
import com.umbra.umbradex.ui.theme.UmbraPrimary
import com.umbra.umbradex.ui.theme.UmbraSurface
import com.umbra.umbradex.ui.theme.UmbraSurfaceHighlight
import com.umbra.umbradex.ui.theme.*

@Composable
fun LiveDexStatsPanel(
    state: StatsUiState,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "rotation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column {
            // Header (sempre visível)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Estatísticas de Coleção",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expandir",
                    tint = Color.Gray,
                    modifier = Modifier.rotate(rotation)
                )
            }

            // Conteúdo expansível
            AnimatedVisibility(visible = isExpanded) {
                if (state is StatsUiState.Success) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Gráficos circulares
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            AnimatedCircularChart(
                                value = state.totalCaught,
                                max = 1025,
                                label = "Apanhados",
                                color = MaterialTheme.colorScheme.secondary,
                                size = 100.dp
                            )

                            AnimatedCircularChart(
                                value = state.totalMissing,
                                max = 1025,
                                label = "Em Falta",
                                color = Color.Gray,
                                size = 100.dp
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Top 3 tipos
                        Text(
                            text = "Tipos Favoritos",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        state.topTypes.forEach { (type, count) ->
                            TypeStatRow(type, count, state.totalCaught)
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TypeStatRow(type: String, count: Int, total: Int) {
    val progress = if (total > 0) count.toFloat() / total.toFloat() else 0f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = type,
            color = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier.width(60.dp)
        )

        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Text(
            text = count.toString(),
            color = Color.White,
            fontSize = 12.sp,
            modifier = Modifier
                .padding(start = 8.dp)
                .width(30.dp)
        )
    }
}