package io.github.pavelya.dots

import android.media.AudioManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import io.github.pavelya.dots.analytics.allowAnalytics
import io.github.pavelya.dots.ui.DotsApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // The phone's volume keys set the game's volume, also when no sound is playing.
        volumeControlStream = AudioManager.STREAM_MUSIC
        setContent { DotsApp() }
        // Each time the app opens (not when the phone turns): Google's consent form for ads, where
        // the law needs it (the EU, the EEA, the UK), over the loading screen. Analytics collects
        // nothing until it has the answer (C-03).
        if (savedInstanceState == null) {
            val app = application as DotsApplication
            lifecycleScope.launch {
                app.adsInEu = app.settings.settings.first().adsInEu
                app.ads.start(this@MainActivity) { allowAnalytics(app) }
            }
        }
    }
}
