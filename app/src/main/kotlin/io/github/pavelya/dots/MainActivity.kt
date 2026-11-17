package io.github.pavelya.dots

import android.media.AudioManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.pavelya.dots.ui.DotsApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // The phone's volume keys set the game's volume, also when no sound is playing.
        volumeControlStream = AudioManager.STREAM_MUSIC
        setContent { DotsApp() }
    }
}
