package io.github.pavelya.dots.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.pavelya.dots.BuildConfig
import io.github.pavelya.dots.DotsApplication
import io.github.pavelya.dots.R
import io.github.pavelya.dots.data.AppSettings
import io.github.pavelya.dots.game.GameViewModel
import io.github.pavelya.dots.game.computerFor
import io.github.pavelya.dots.ui.screen.DeveloperScreen
import io.github.pavelya.dots.ui.screen.GameScreen
import io.github.pavelya.dots.ui.screen.LoadingScreen
import io.github.pavelya.dots.ui.screen.MapActions
import io.github.pavelya.dots.ui.screen.MapScreen
import io.github.pavelya.dots.ui.screen.PlayAGameNote
import io.github.pavelya.dots.ui.screen.RulesScreen
import io.github.pavelya.dots.ui.screen.SettingsScreen
import io.github.pavelya.dots.ui.screen.SettingsViewModel
import io.github.pavelya.dots.ui.screen.StandInNote
import io.github.pavelya.dots.ui.screen.StandInScreen
import io.github.pavelya.dots.ui.theme.DotsTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Every screen of the app, by route, as listed in docs/design/flows.md, "At launch". Screens Google
 * draws have no route, and Pause, Result, the booster preview and the tutorial are states of [GAME].
 */
object Routes {
    /** Shown while the saved game is read; then the map. */
    const val LOADING = "loading"

    /** Home. Back here leaves the app. */
    const val MAP = "map"

    /** Over the map: a level's card, with Start (A-03). */
    const val LEVEL = "level/{number}"
    fun level(number: Int) = "level/$number"

    /** Over the Level card: the boosters the player has. */
    const val BOOSTERS = "boosters"

    /** Over the map, after every 10th level: pick one of two rewards. */
    const val CHEST = "chest"

    /** Over the map: today's puzzle and the streak. */
    const val DAILY = "daily"

    /** Over the map: Free play or Two players. */
    const val PLAY = "play"

    /** The game in progress: the store's current game, new or resumed. */
    const val GAME = "game"
    const val RULES = "rules"
    const val STATS = "stats"
    const val SHOP = "shop"

    /** An item from the Shop, on a sample board. */
    const val COSMETIC = "cosmetic"
    const val SETTINGS = "settings"
    const val SETTINGS_DETAILS = "settings/details"

    /** The POC's test settings; opens from the version line in Settings. */
    const val DEVELOPER = "developer"

    val ALL = listOf(LOADING, MAP, LEVEL, BOOSTERS, CHEST, DAILY, PLAY, GAME, RULES, STATS, SHOP, COSMETIC, SETTINGS, SETTINGS_DETAILS, DEVELOPER)
}

@Composable
fun DotsApp(navController: NavHostController = rememberNavController()) {
    val app = LocalContext.current.applicationContext as DotsApplication
    val back = { navController.popBackStack(); Unit }
    // Leaving a game, or a note over the map, always lands on the map (flows.md, "Around the board").
    val toMap = { navController.popBackStack(Routes.MAP, inclusive = false); Unit }
    DotsTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { insets ->
            NavHost(
                navController = navController,
                startDestination = Routes.LOADING,
                modifier = Modifier.padding(insets),
            ) {
                composable(Routes.LOADING) {
                    val loaded by app.games.loaded.collectAsStateWithLifecycle()
                    LaunchedEffect(loaded) {
                        if (loaded) navController.navigate(Routes.MAP) { popUpTo(Routes.LOADING) { inclusive = true } }
                    }
                    LoadingScreen()
                }
                composable(Routes.MAP) {
                    MapScreen(
                        MapActions(
                            onLevel = { navController.navigate(Routes.level(it)) },
                            onDaily = { navController.navigate(Routes.DAILY) },
                            onPlay = { navController.navigate(Routes.PLAY) },
                            onRules = { navController.navigate(Routes.RULES) },
                            onStats = { navController.navigate(Routes.STATS) },
                            onShop = { navController.navigate(Routes.SHOP) },
                            onSettings = { navController.navigate(Routes.SETTINGS) },
                        ),
                    )
                }
                dialog(Routes.LEVEL, arguments = listOf(navArgument("number") { type = NavType.IntType })) { entry ->
                    val number = entry.arguments?.getInt("number") ?: 1
                    StandInNote(stringResource(R.string.map_level, number), "A-03", onClose = back) {
                        TextButton(onClick = { navController.navigate(Routes.BOOSTERS) }) { Text(stringResource(R.string.screen_boosters)) }
                    }
                }
                dialog(Routes.BOOSTERS) {
                    StandInNote(stringResource(R.string.screen_boosters), "A-05", onClose = back)
                }
                dialog(Routes.CHEST) {
                    StandInNote(stringResource(R.string.screen_chest), "A-04", onClose = toMap)
                }
                dialog(Routes.DAILY) {
                    StandInNote(stringResource(R.string.screen_daily), "A-07", onClose = back)
                }
                dialog(Routes.PLAY) {
                    val scope = rememberCoroutineScope()
                    val current by app.games.current.collectAsStateWithLifecycle()
                    // A card resumes the saved game of its kind, or writes a new game to the store
                    // before the board opens: the board always plays the store's current game, also
                    // after Android restarts the app. Until A-08, both kinds share one saved game.
                    fun play(vsComputer: Boolean) {
                        scope.launch {
                            app.games.loaded.first { it }
                            if (app.games.current.value?.vsComputer != vsComputer) {
                                val settings = app.settings.settings.first()
                                app.games.start(settings.newGame(vsComputer, now = System.currentTimeMillis()))
                            }
                            navController.navigate(Routes.GAME) { popUpTo(Routes.MAP) }
                        }
                    }
                    PlayAGameNote(
                        saved = current?.vsComputer,
                        onFreePlay = { play(vsComputer = true) },
                        onTwoPlayers = { play(vsComputer = false) },
                        onClose = back,
                    )
                }
                composable(Routes.GAME) {
                    val loaded by app.games.loaded.collectAsStateWithLifecycle()
                    if (loaded) {
                        val start = remember { app.games.current.value }
                        if (start == null) {
                            // Nothing to resume (the app was restarted after the game ended).
                            LaunchedEffect(Unit) { toMap() }
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
                            GameScreen(game, onToMap = toMap, onRules = { navController.navigate(Routes.RULES) })
                        }
                    }
                }
                composable(Routes.RULES) {
                    RulesScreen(back)
                }
                composable(Routes.STATS) {
                    StandInScreen(R.string.screen_stats, "A-11", onBack = back)
                }
                composable(Routes.SHOP) {
                    StandInScreen(R.string.screen_shop, "C-02", onBack = back) {
                        TextButton(onClick = { navController.navigate(Routes.COSMETIC) }) { Text(stringResource(R.string.shop_sample_item)) }
                    }
                }
                composable(Routes.COSMETIC) {
                    StandInScreen(R.string.screen_cosmetic, "C-02", onBack = back)
                }
                composable(Routes.SETTINGS) {
                    val model = viewModel { SettingsViewModel(app.settings) }
                    val settings by model.settings.collectAsStateWithLifecycle()
                    settings?.let {
                        SettingsScreen(
                            settings = it,
                            onChange = model::change,
                            version = BuildConfig.VERSION_NAME,
                            onDetails = { navController.navigate(Routes.SETTINGS_DETAILS) },
                            onDeveloperMenu = { navController.navigate(Routes.DEVELOPER) },
                            onBack = back,
                        )
                    }
                }
                composable(Routes.SETTINGS_DETAILS) {
                    StandInScreen(R.string.screen_settings_details, "A-09", onBack = back)
                }
                composable(Routes.DEVELOPER) {
                    val context = LocalContext.current
                    val scope = rememberCoroutineScope()
                    val model = viewModel { SettingsViewModel(app.settings) }
                    val settings by model.settings.collectAsStateWithLifecycle()
                    val loggedGames by app.games.loggedGames.collectAsStateWithLifecycle()
                    settings?.let {
                        DeveloperScreen(
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
