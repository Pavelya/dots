package io.github.pavelya.dots.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.pavelya.dots.DotsApplication
import io.github.pavelya.dots.ui.screen.RulesScreen
import io.github.pavelya.dots.ui.screen.SettingsScreen
import io.github.pavelya.dots.ui.screen.SettingsViewModel
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.pavelya.dots.game.GameViewModel
import io.github.pavelya.dots.game.computer
import io.github.pavelya.dots.ui.screen.GameScreen
import kotlin.random.Random
import io.github.pavelya.dots.ui.screen.HomeScreen
import io.github.pavelya.dots.ui.theme.DotsTheme

/** Every screen of the app, by route. */
object Routes {
    const val HOME = "home"
    const val PLAY_COMPUTER = "game/computer"
    const val TWO_PLAYERS = "game/two-players"
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
                    HomeScreen(
                        onPlayComputer = { navController.navigate(Routes.PLAY_COMPUTER) },
                        onTwoPlayers = { navController.navigate(Routes.TWO_PLAYERS) },
                        onRules = { navController.navigate(Routes.RULES) },
                        onSettings = { navController.navigate(Routes.SETTINGS) },
                    )
                }
                val back = { navController.popBackStack(); Unit }
                composable(Routes.PLAY_COMPUTER) {
                    val settings by app.settings.settings.collectAsStateWithLifecycle(initialValue = null)
                    settings?.let { chosen ->
                        val game = viewModel {
                            GameViewModel(
                                newSettings = { chosen.gameSettings(seed = Random.nextLong()) },
                                newComputer = { chosen.difficulty.computer() },
                                showThreats = chosen.threatHighlight,
                            )
                        }
                        GameScreen(game, back)
                    }
                }
                composable(Routes.TWO_PLAYERS) {
                    val settings by app.settings.settings.collectAsStateWithLifecycle(initialValue = null)
                    settings?.let { chosen ->
                        val game = viewModel {
                            GameViewModel(newSettings = { chosen.gameSettings(seed = Random.nextLong()) }, showThreats = chosen.threatHighlight)
                        }
                        GameScreen(game, back)
                    }
                }
                composable(Routes.RULES) {
                    RulesScreen(back)
                }
                composable(Routes.SETTINGS) {
                    val model = viewModel { SettingsViewModel(app.settings) }
                    val settings by model.settings.collectAsStateWithLifecycle()
                    settings?.let { SettingsScreen(it, model::change, back) }
                }
            }
        }
    }
}
