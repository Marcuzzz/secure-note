package com.example.securenote.ui

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.securenote.SecureNoteApp
import com.example.securenote.ui.nav.SecureNoteNavHost
import com.example.securenote.ui.theme.SecureNoteTheme
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Prevent screenshots and content from appearing in recents.
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        setContent {
            SecureNoteTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SecureNoteNavHost()
                }
            }
        }

        // Auto-lock when the app goes to the background.
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                // no-op; the STOPPED transition below handles locking
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) {
            (application as SecureNoteApp).container.session.lock()
        }
    }
}
