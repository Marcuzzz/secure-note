package com.example.securenote

import android.app.Application
import com.example.securenote.data.AppContainer
import com.example.securenote.data.DefaultAppContainer

class SecureNoteApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
