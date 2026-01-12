package com.umbra.umbradex

import android.app.Application
import com.umbra.umbradex.utils.SoundManager

class UmbraDexApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize SoundManager for audio throughout the app
        SoundManager.initialize(this)
    }
    
    override fun onTerminate() {
        super.onTerminate()
        // Release SoundManager resources
        SoundManager.release()
    }
}