package com.lunexa.gameshelf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lunexa.gameshelf.data.AppSettings
import com.lunexa.gameshelf.ui.navigation.GameShelfRoot
import com.lunexa.gameshelf.ui.theme.GameShelfTheme
import com.lunexa.gameshelf.ui.theme.GsBackground

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as GameShelfApp).container
        setContent {
            val settings: AppSettings? by container.settings.settings
                .collectAsStateWithLifecycle(initialValue = null)
            val current = settings
            if (current == null) {
                // Пока настройки читаются (доли секунды) — фон без мигания темы
                Box(Modifier.fillMaxSize().background(GsBackground))
            } else {
                GameShelfTheme(mode = current.themeMode, accent = Color(current.accent)) {
                    GameShelfRoot()
                }
            }
        }
    }
}
