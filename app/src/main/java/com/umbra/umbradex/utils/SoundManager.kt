package com.umbra.umbradex.utils

import android.content.Context
import android.media.MediaPlayer
import android.media.SoundPool
import android.media.AudioAttributes
import android.util.Log
import com.umbra.umbradex.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Singleton manager for all app sounds.
 * Handles background music and sound effects.
 */
object SoundManager {
    private const val TAG = "SoundManager"
    
    // Background music player
    private var backgroundPlayer: MediaPlayer? = null
    private var isBackgroundMusicEnabled = true
    private var isSoundEffectsEnabled = true
    private var isInitialized = false
    
    // SoundPool for sound effects (better for short sounds)
    private var soundPool: SoundPool? = null
    
    // Sound effect IDs
    private var soundGetQuest: Int = 0
    private var soundEquipSome: Int = 0
    private var soundGetTitle: Int = 0
    private var soundGoodAnimal: Int = 0
    private var soundBuySomething: Int = 0
    private var soundAddLiving: Int = 0
    private var soundFavorite: Int = 0
    private var soundCreateTeam: Int = 0
    
    // Track if sounds are loaded
    private var soundsLoaded = false
    
    // Track current stream for resetting
    private var currentStreamId: Int = 0
    
    // Application context reference
    private var appContext: Context? = null
    
    /**
     * Initialize the SoundManager with application context.
     * Should be called once in Application.onCreate()
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        
        appContext = context.applicationContext
        
        // Create SoundPool for sound effects
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        
        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(audioAttributes)
            .build()
        
        soundPool?.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) {
                soundsLoaded = true
                Log.d(TAG, "Sound loaded successfully")
            }
        }
        
        // Load sound effects
        loadSoundEffects(context)
        
        isInitialized = true
        Log.d(TAG, "SoundManager initialized")
    }
    
    /**
     * Load all sound effects into SoundPool
     */
    private fun loadSoundEffects(context: Context) {
        try {
            soundPool?.let { pool ->
                soundGetQuest = pool.load(context, R.raw.getquest, 1)
                soundEquipSome = pool.load(context, R.raw.equipsome, 1)
                soundGetTitle = pool.load(context, R.raw.gettitle, 1)
                soundGoodAnimal = pool.load(context, R.raw.goodanimal, 1)
                soundBuySomething = pool.load(context, R.raw.buysomethings, 1)
                soundAddLiving = pool.load(context, R.raw.addliving, 1)
                soundFavorite = pool.load(context, R.raw.favorite, 1)
                soundCreateTeam = pool.load(context, R.raw.createteam, 1)
                Log.d(TAG, "All sound effects queued for loading")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading sound effects", e)
        }
    }
    
    /**
     * Start background music in loop.
     * Call this when entering main app screens (not auth screens).
     */
    fun startBackgroundMusic(context: Context? = null) {
        if (!isBackgroundMusicEnabled) return
        
        val ctx = context ?: appContext ?: return
        
        // If already playing, do nothing
        if (backgroundPlayer?.isPlaying == true) return
        
        try {
            // Release any existing player
            stopBackgroundMusic()
            
            backgroundPlayer = MediaPlayer.create(ctx, R.raw.background)?.apply {
                isLooping = true
                setVolume(0.6f, 0.6f) // Background volume
                start()
                Log.d(TAG, "Background music started")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting background music", e)
        }
    }
    
    /**
     * Stop background music.
     * Call this when entering auth screens or leaving app.
     */
    fun stopBackgroundMusic() {
        try {
            backgroundPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
                Log.d(TAG, "Background music stopped")
            }
            backgroundPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping background music", e)
        }
    }
    
    /**
     * Pause background music (e.g., when app goes to background)
     */
    fun pauseBackgroundMusic() {
        try {
            backgroundPlayer?.let { player ->
                if (player.isPlaying) {
                    player.pause()
                    Log.d(TAG, "Background music paused")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing background music", e)
        }
    }
    
    /**
     * Resume background music (e.g., when app comes to foreground)
     */
    fun resumeBackgroundMusic() {
        if (!isBackgroundMusicEnabled) return
        
        try {
            backgroundPlayer?.let { player ->
                if (!player.isPlaying) {
                    player.start()
                    Log.d(TAG, "Background music resumed")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming background music", e)
        }
    }
    
    /**
     * Enable or disable background music
     */
    fun setBackgroundMusicEnabled(enabled: Boolean) {
        isBackgroundMusicEnabled = enabled
        if (!enabled) {
            stopBackgroundMusic()
        }
    }
    
    /**
     * Check if background music is enabled
     */
    fun isBackgroundMusicEnabled(): Boolean = isBackgroundMusicEnabled
    
    /**
     * Enable or disable sound effects
     */
    fun setSoundEffectsEnabled(enabled: Boolean) {
        isSoundEffectsEnabled = enabled
    }
    
    /**
     * Check if sound effects are enabled
     */
    fun isSoundEffectsEnabled(): Boolean = isSoundEffectsEnabled
    
    // ==================== SOUND EFFECT METHODS ====================
    
    /**
     * Play sound when claiming a mission reward.
     * Resets if called multiple times quickly.
     */
    fun playGetQuestSound() {
        playSound(soundGetQuest)
    }
    
    /**
     * Play sound when equipping an item in inventory.
     */
    fun playEquipSound() {
        playSound(soundEquipSome)
    }
    
    /**
     * Play sound when user levels up and gets a new title.
     */
    fun playGetTitleSound() {
        playSound(soundGetTitle)
    }
    
    /**
     * Play sound when clicking on the equipped pet.
     */
    fun playGoodAnimalSound() {
        playSound(soundGoodAnimal)
    }
    
    /**
     * Play sound when buying something in the shop.
     */
    fun playBuySomethingSound() {
        playSound(soundBuySomething)
    }
    
    /**
     * Play sound when adding a Pokemon to the Living Dex.
     */
    fun playAddLivingSound() {
        playSound(soundAddLiving)
    }
    
    /**
     * Play sound when favoriting a Pokemon.
     */
    fun playFavoriteSound() {
        playSound(soundFavorite)
    }
    
    /**
     * Play sound when creating a team.
     */
    fun playCreateTeamSound() {
        playSound(soundCreateTeam)
    }
    
    /**
     * Generic method to play a sound effect.
     * Stops any previous instance of the same sound to prevent overlapping.
     */
    private fun playSound(soundId: Int) {
        if (!isSoundEffectsEnabled) return
        
        if (soundId == 0) {
            Log.w(TAG, "Sound not loaded yet")
            return
        }
        
        try {
            soundPool?.let { pool ->
                // Stop the current stream if it exists (prevents sound overlap)
                if (currentStreamId != 0) {
                    pool.stop(currentStreamId)
                }
                
                // Play new sound and track the stream ID
                currentStreamId = pool.play(
                    soundId,
                    1.0f, // left volume
                    1.0f, // right volume
                    1,    // priority
                    0,    // loop (0 = no loop)
                    1.0f  // rate
                )
                Log.d(TAG, "Playing sound: $soundId, stream: $currentStreamId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing sound", e)
        }
    }
    
    /**
     * Release all resources.
     * Call this in Application.onTerminate() or Activity.onDestroy()
     */
    fun release() {
        try {
            stopBackgroundMusic()
            soundPool?.release()
            soundPool = null
            isInitialized = false
            soundsLoaded = false
            appContext = null
            Log.d(TAG, "SoundManager released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing SoundManager", e)
        }
    }
}
