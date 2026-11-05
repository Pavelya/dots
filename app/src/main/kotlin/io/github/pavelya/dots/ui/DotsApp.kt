package io.github.pavelya.dots.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.pavelya.dots.DotsApplication
import io.github.pavelya.dots.R
import io.github.pavelya.dots.data.AppSettings
import io.github.pavelya.dots.game.GameViewModel
import io.github.pavelya.dots.game.computerFor
import io.github.pavelya.dots.ui.screen.GameScreen
import io.github.pavelya.dots.ui.screen.HomeScreen
import io.github.pavelya.dots.ui.screen.RulesScreen
import io.github.pavelya.dots.ui.screen.SettingsScreen
import io.github.pavelya.dots.ui.screen.SettingsViewModel
import io.github.pavelya.dots.ui.theme.DotsTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Every screen of the app, by route. */
object Routes {
    const val HOME = "home"

    /** The game in progress: the store's current game, new or resumed. */
    const val GAME = "game"
    const val RULES = "rules"
    const val SETTINGS = "settings"
}

@Composable
fun DotsApp(navController: NavHostController = rememberNavController()) {
    val app = LocalContext.current.applicationContext as DotsApplication
    DotsTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { insets ->
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier.padding(insets),
            ) {
                composable(Routes.HOME) {
                    val scope = rememberCoroutineScope()
                    val current by app.games.current.collectAsStateWithLifecycle()
                    // A new game is written to the store before its screen opens, so the screen
                    // always plays the store's current game, also after Android restarts the app.
                    fun startNew(vsComputer: Boolean) {
                        scope.launch {
                            val settings = app.settings.settings.first()
                            app.games.start(settings.newGame(vsComputer, now = System.currentTimeMillis()))
                            navController.navigate(Routes.GAME)
                        }
                    }
                    HomeScreen(
                        canContinue = current != null,
                        onContinue = { navController.navigate(Routes.GAME) },
                        onPlayComputer = { startNew(vsComputer = true) },
                        onTwoPlayers = { startNew(vsComputer = false) },
                        onRules = { navController.navigate(Routes.RULES) },
                        onSettings = { navController.navigate(Routes.SETTINGS) },
                    )
                }
                val back = { navController.popBackStack(); Unit }
                composable(Routes.GAME) {
                    val loaded by app.games.loaded.collectAsStateWithLifecycle()
                    if (loaded) {
                        val start = remember { app.games.current.value }
                        if (start == null) {
                            // Nothing to resume (the app was restarted after the game ended).
                            LaunchedEffect(Unit) { navController.popBackStack() }
                        } else {
                            val settings = app.settings.settings.collectAsStateWithLifecycle(initialValue = null)
                            val game = viewModel {
                                GameViewModel(
                                    start = start,
                                    nextGame = {
                                        (settings.value ?: AppSettings()).newGame(start.vsComputer, now = System.currentTimeMillis())
                                    },
                                    computerFor = ::computerFor,
                                    journal = app.games,
                                )
                            }
                            GameScreen(game, back)
                        }
                    }
                }
                composable(Routes.RULES) {
                    RulesScreen(back)
                }
                composable(Routes.SETTINGS) {
                    val context = LocalContext.current
                    val scope = rememberCoroutineScope()
                    val model = viewModel { SettingsViewModel(app.settings) }
                    val settings by model.settings.collectAsStateWithLifecycle()
                    val loggedGames by app.games.loggedGames.collectAsStateWithLifecycle()
                    settings?.let {
                        SettingsScreen(
                            settings = it,
                            onChange = model::change,
                            loggedGames = loggedGames,
                            onShareLog = { scope.launch { shareLog(context, app) } },
                            onBack = back,
                        )
                    }
                }
            }
        }
    }
}

/** Opens the share sheet with the game log file, for the tester to send it (docs/poc/test-protocol.md). */
private suspend fun shareLog(context: Context, app: DotsApplication) {
    val uri = withContext(Dispatchers.IO) {
        FileProvider.getUriForFile(context, "${context.packageName}.logs", app.games.logFile)
    }
    val send = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.log_share_subject))
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    send.clipData = ClipData.newRawUri(null, uri)
    context.startActivity(Intent.createChooser(send, context.getString(R.string.log_share)))
}
