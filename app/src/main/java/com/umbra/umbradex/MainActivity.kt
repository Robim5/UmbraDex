// Em: app/src/main/java/com/umbra/umbradex/MainActivity.kt
package com.umbra.umbradex // Certifica-te que o package está correto

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.umbra.umbradex.ui.theme.UmbraDexTheme // Certifica-te que tens os imports corretos
import com.umbra.umbradex.ui.navigation.UmbraNavGraph // E este também
import com.umbra.umbradex.data.supabase.UmbraSupabase
import com.umbra.umbradex.utils.SoundManager

// CompositionLocal para passar as cores do nome globalmente
val LocalNameColors = compositionLocalOf { listOf("#FFFFFF", "#FFFFFF") }

class MainActivity : ComponentActivity() {

    // Cria ou obtém o ViewModel existente
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Instalar Splash Screen ANTES de super.onCreate()
        val splashScreen = installSplashScreen()
        
        super.onCreate(savedInstanceState)
        
        // Initialize Supabase with application context for session persistence
        UmbraSupabase.initialize(applicationContext)

        setContent {
            // Recolhe o StateFlow como estado do Compose
            val themeColors by viewModel.themeColors.collectAsState()
            val nameColors by viewModel.nameColors.collectAsState()

            // Fornecer as cores do nome globalmente
            CompositionLocalProvider(LocalNameColors provides nameColors) {
                UmbraDexTheme(themeColors = themeColors) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val navController = rememberNavController()
                        UmbraNavGraph(navController = navController)
                    }
                }
            }
        }
    }
    
    override fun onPause() {
        super.onPause()
        // Pause background music when app goes to background
        SoundManager.pauseBackgroundMusic()
    }
    
    override fun onResume() {
        super.onResume()
        // Resume background music when app comes back to foreground
        SoundManager.resumeBackgroundMusic()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Stop background music when activity is destroyed
        SoundManager.stopBackgroundMusic()
    }
}
