package io.github.pavelya.dots.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.pavelya.dots.R
import io.github.pavelya.dots.ui.screen.HomeScreen
import io.github.pavelya.dots.ui.screen.PlaceholderScreen
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
                    PlaceholderScreen(R.string.home_play_computer, R.string.placeholder_game, back)
                }
                composable(Routes.TWO_PLAYERS) {
                    PlaceholderScreen(R.string.home_two_players, R.string.placeholder_game, back)
                }
                composable(Routes.RULES) {
                    PlaceholderScreen(R.string.home_rules, R.string.placeholder_rules, back)
                }
                composable(Routes.SETTINGS) {
                    PlaceholderScreen(R.string.home_settings, R.string.placeholder_settings, back)
                }
            }
        }
    }
}
