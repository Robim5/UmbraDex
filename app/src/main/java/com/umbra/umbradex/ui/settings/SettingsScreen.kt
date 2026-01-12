package com.umbra.umbradex.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.umbra.umbradex.data.repository.UserRepository
import com.umbra.umbradex.utils.Resource
import com.umbra.umbradex.utils.SoundManager
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    navController: NavController,
    userId: String,
    viewModel: SettingsViewModel = viewModel(),
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showUsernameDialog by remember { mutableStateOf(false) }
    var showEmailDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showDeletePasswordDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showPromoCodeDialog by remember { mutableStateOf(false) }
    var showPromoSuccessDialog by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()

    LaunchedEffect(userId) {
        viewModel.loadUserProfile(userId)
    }
    
    // Show snackbar for messages
    LaunchedEffect(uiState.successMessage, uiState.error) {
        if (uiState.successMessage != null || uiState.error != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearMessages()
        }
    }
    
    // Show promo success dialog when reward is received
    LaunchedEffect(uiState.promoReward) {
        if (uiState.promoReward != null) {
            showPromoSuccessDialog = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(scrollState)
    ) {
        // Header
        SettingsHeader(onBackClick = { navController.popBackStack() })
        
        // Success/Error Message
        if (uiState.successMessage != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF4CAF50).copy(alpha = 0.2f)
            ) {
                Text(
                    text = uiState.successMessage!!,
                    modifier = Modifier.padding(16.dp),
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        if (uiState.error != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF44336).copy(alpha = 0.2f)
            ) {
                Text(
                    text = uiState.error!!,
                    modifier = Modifier.padding(16.dp),
                    color = Color(0xFFF44336),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Profile Section
        if (uiState.profile != null) {
            ProfileSection(
                username = uiState.profile!!.username,
                email = uiState.profile!!.email,
                level = uiState.profile!!.level,
                gold = uiState.profile!!.gold.toInt()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Account Settings Section
        SettingsSection(title = "Conta") {
            SettingsItem(
                icon = Icons.Default.Person,
                title = "Alterar Nome",
                subtitle = uiState.profile?.username ?: "Muda o teu nome de utilizador",
                onClick = { showUsernameDialog = true }
            )
            SettingsItem(
                icon = Icons.Default.Email,
                title = "Alterar Email",
                subtitle = uiState.profile?.email ?: "",
                onClick = { showEmailDialog = true }
            )
            SettingsItem(
                icon = Icons.Default.Lock,
                title = "Alterar Palavra-passe",
                subtitle = "Atualiza a tua palavra-passe",
                onClick = { showPasswordDialog = true }
            )
            SettingsItem(
                icon = Icons.Default.DeleteForever,
                title = "Apagar Conta",
                subtitle = "Eliminar permanentemente a conta",
                onClick = { showDeleteConfirmDialog = true },
                tintColor = Color(0xFFF44336)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // App Settings Section
        SettingsSection(title = "Aplicação") {
            var musicEnabled by remember { mutableStateOf(SoundManager.isBackgroundMusicEnabled()) }
            var effectsEnabled by remember { mutableStateOf(SoundManager.isSoundEffectsEnabled()) }
            
            SettingsToggleItem(
                icon = Icons.Default.MusicNote,
                title = "Música de Fundo",
                subtitle = if (musicEnabled) "A música está a tocar" else "A música está silenciada",
                isChecked = musicEnabled,
                onToggle = { enabled ->
                    musicEnabled = enabled
                    SoundManager.setBackgroundMusicEnabled(enabled)
                    if (enabled) {
                        SoundManager.startBackgroundMusic()
                    }
                }
            )
            SettingsToggleItem(
                icon = Icons.Default.VolumeUp,
                title = "Efeitos Sonoros",
                subtitle = if (effectsEnabled) "Os efeitos estão ativos" else "Os efeitos estão silenciados",
                isChecked = effectsEnabled,
                onToggle = { enabled ->
                    effectsEnabled = enabled
                    SoundManager.setSoundEffectsEnabled(enabled)
                }
            )
            SettingsItem(
                icon = Icons.Default.Info,
                title = "Sobre",
                subtitle = "Versão 1.0.0",
                onClick = { showAboutDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Social Settings Section
        SettingsSection(title = "Social") {
            SettingsItem(
                icon = Icons.Default.PlayArrow,
                title = "YouTube",
                subtitle = "Visita o nosso canal",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://youtube.com/@umbraflowpromax?si=ddaqvsu4EdAyXjeo"))
                    context.startActivity(intent)
                }
            )
            SettingsItem(
                icon = Icons.Default.Forum,
                title = "Discord",
                subtitle = "Junta-te à nossa comunidade",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.gg/mNmk8s6qXG"))
                    context.startActivity(intent)
                }
            )
            SettingsItem(
                icon = Icons.Default.CardGiftcard,
                title = "Códigos Promocionais",
                subtitle = "Resgata códigos e recebe recompensas",
                onClick = { showPromoCodeDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Logout Button
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clickable { showLogoutDialog = true },
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFF44336).copy(alpha = 0.1f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF44336))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = "Terminar Sessão",
                    tint = Color(0xFFF44336)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Terminar Sessão",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF44336)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
    
    // Edit Username Dialog
    if (showUsernameDialog) {
        EditTextDialog(
            title = "Alterar Nome",
            currentValue = uiState.profile?.username ?: "",
            label = "Nome de utilizador",
            onDismiss = { showUsernameDialog = false },
            onConfirm = { newUsername ->
                viewModel.updateUsername(newUsername, userId)
                showUsernameDialog = false
            }
        )
    }
    
    // Edit Email Dialog
    if (showEmailDialog) {
        EditTextDialog(
            title = "Alterar Email",
            currentValue = uiState.profile?.email ?: "",
            label = "Email",
            onDismiss = { showEmailDialog = false },
            onConfirm = { newEmail ->
                viewModel.updateEmail(newEmail, userId)
                showEmailDialog = false
            }
        )
    }
    
    // Change Password Dialog
    if (showPasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showPasswordDialog = false },
            onConfirm = { newPassword ->
                viewModel.updatePassword(newPassword)
                showPasswordDialog = false
            }
        )
    }
    
    // Delete Account - Step 1: Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFF44336),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text("Tens a certeza?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Esta ação é irreversível! Todos os teus dados, Pokémon, equipas e progresso serão permanentemente apagados.",
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        showDeletePasswordDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF44336)
                    )
                ) {
                    Text("Sim, quero apagar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
    
    // Delete Account - Step 2: Password Verification Dialog
    if (showDeletePasswordDialog) {
        DeleteAccountPasswordDialog(
            email = uiState.profile?.email ?: "",
            isLoading = uiState.isLoading,
            onDismiss = { showDeletePasswordDialog = false },
            onConfirm = { password ->
                viewModel.verifyPasswordAndDeleteAccount(
                    email = uiState.profile?.email ?: "",
                    password = password,
                    onSuccess = {
                        showDeletePasswordDialog = false
                        onLogout()
                    }
                )
            }
        )
    }
    
    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            icon = {
                Text("🎮", fontSize = 48.sp)
            },
            title = {
                Text("Sobre o UmbraDex", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "O UmbraDex nasceu da paixão da empresa Umbra que é uma grande fã de Pokémon que sonha em criar uma Pokédex perfeita.",
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Mais do que um simples tracker, o UmbraDex é uma experiência gamificada onde podes construir a tua Living Dex, completar missões, personalizar o teu perfil e muito mais!",
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Obrigado por fazeres parte desta obra de arte!",
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Versão 3.5.0",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Fechar")
                }
            }
        )
    }
    
    // Promo Code Dialog
    if (showPromoCodeDialog) {
        PromoCodeDialog(
            isLoading = uiState.isLoading,
            onDismiss = { showPromoCodeDialog = false },
            onConfirm = { code ->
                viewModel.redeemPromoCode(code, userId)
                showPromoCodeDialog = false
            }
        )
    }
    
    // Promo Success Dialog
    if (showPromoSuccessDialog && uiState.promoReward != null) {
        AlertDialog(
            onDismissRequest = { 
                showPromoSuccessDialog = false
                viewModel.clearMessages()
            },
            icon = {
                Text("🎉", fontSize = 48.sp)
            },
            title = {
                Text("Código Resgatado!", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Parabéns! Recebeste:",
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("💰", fontSize = 32.sp)
                            Text(
                                text = "+${uiState.promoReward!!.gold} Gold",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD700)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("⭐", fontSize = 32.sp)
                            Text(
                                text = "+${uiState.promoReward!!.xp} XP",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF9C27B0)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { 
                    showPromoSuccessDialog = false
                    viewModel.clearMessages()
                }) {
                    Text("Fantástico!")
                }
            }
        )
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = null,
                    tint = Color(0xFFF44336)
                )
            },
            title = {
                Text("Terminar Sessão", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Tens a certeza que queres sair?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF44336)
                    )
                ) {
                    Text("Sair")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun EditTextDialog(
    title: String,
    currentValue: String,
    label: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var textValue by remember { mutableStateOf(currentValue) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = textValue,
                onValueChange = { textValue = it },
                label = { Text(label) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(textValue) },
                enabled = textValue.isNotBlank() && textValue != currentValue
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    
    val passwordsMatch = newPassword == confirmPassword && newPassword.length >= 6
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Alterar Palavra-passe", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("Nova palavra-passe") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (passwordVisible) 
                        androidx.compose.ui.text.input.VisualTransformation.None 
                    else 
                        androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Esconder palavra-passe" else "Mostrar palavra-passe"
                            )
                        }
                    }
                )
                
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirmar palavra-passe") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (passwordVisible) 
                        androidx.compose.ui.text.input.VisualTransformation.None 
                    else 
                        androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    isError = confirmPassword.isNotEmpty() && newPassword != confirmPassword
                )
                
                if (newPassword.isNotEmpty() && newPassword.length < 6) {
                    Text(
                        text = "A palavra-passe deve ter pelo menos 6 caracteres",
                        color = Color(0xFFF44336),
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(newPassword) },
                enabled = passwordsMatch
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun DeleteAccountPasswordDialog(
    email: String,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        icon = {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = Color(0xFFF44336),
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text("Verificação de Segurança", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Para confirmar a eliminação da conta, introduz a tua palavra-passe:",
                    fontSize = 14.sp
                )
                
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Palavra-passe") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isLoading,
                    visualTransformation = if (passwordVisible) 
                        androidx.compose.ui.text.input.VisualTransformation.None 
                    else 
                        androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Esconder" else "Mostrar"
                            )
                        }
                    }
                )
                
                if (isLoading) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color(0xFFF44336)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("A apagar conta...", fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(password) },
                enabled = password.isNotEmpty() && !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF44336)
                )
            ) {
                Text("Apagar Conta")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun PromoCodeDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var code by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        icon = {
            Text("🎁", fontSize = 48.sp)
        },
        title = {
            Text("Código Promocional", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Introduz o teu código para receber recompensas especiais!",
                    fontSize = 14.sp
                )
                
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    label = { Text("Código") },
                    placeholder = { Text("Ex: BETA123UAU") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isLoading
                )
                
                if (isLoading) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("A verificar código...", fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(code) },
                enabled = code.isNotEmpty() && !isLoading
            ) {
                Text("Resgatar")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun SettingsHeader(onBackClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Voltar"
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "⚙️ Definições",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ProfileSection(
    username: String,
    email: String,
    level: Int,
    gold: Int
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = username,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = email,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                StatChip(label = "Nível", value = level.toString())
                StatChip(label = "Ouro", value = gold.toString())
            }
        }
    }
}

@Composable
fun StatChip(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    tintColor: Color? = null
) {
    val iconColor = tintColor ?: MaterialTheme.colorScheme.primary
    val titleColor = tintColor ?: MaterialTheme.colorScheme.onSurface
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = titleColor
            )
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

@Composable
fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!isChecked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        Switch(
            checked = isChecked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        )
    }
}