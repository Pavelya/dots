package io.github.pavelya.dots.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.ui.window.DialogProperties
import dots.campaign.Campaign
import dots.campaign.LevelRecord
import dots.campaign.Progress
import dots.campaign.Tutorial
import dots.log.GameLogEntry
import io.github.pavelya.dots.BuildConfig
import io.github.pavelya.dots.DotsApplication
import io.github.pavelya.dots.R
import io.github.pavelya.dots.data.AppSettings
import io.github.pavelya.dots.data.GameMode
import io.github.pavelya.dots.game.GameViewModel
import io.github.pavelya.dots.game.computerFor
import io.github.pavelya.dots.game.freeGame
import io.github.pavelya.dots.game.freeGameLike
import io.github.pavelya.dots.game.levelGame
import io.github.pavelya.dots.ui.screen.DeveloperScreen
import io.github.pavelya.dots.ui.screen.GameScreen
import io.github.pavelya.dots.ui.screen.LevelCard
import io.github.pavelya.dots.ui.screen.LoadingScreen
import io.github.pavelya.dots.ui.screen.MapActions
import io.github.pavelya.dots.ui.screen.MapScreen
import io.github.pavelya.dots.ui.screen.PlayAGameNote
import io.github.pavelya.dots.ui.screen.RulesScreen
import io.github.pavelya.dots.ui.screen.SettingsDetailsScreen
import io.github.pavelya.dots.ui.screen.SettingsPage
import io.github.pavelya.dots.ui.screen.SettingsScreen
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
    /**
     * Shown while the saved game and the progress are read; then the map, or Level 1 until it is
     * passed ([openingRoute]).
     */
    const val LOADING = "loading"

    /** Home. Back here leaves the app. */
    const val MAP = "map"

    /** Over the map: a level's card, with Start or Resume. */
    const val LEVEL = "level/{world}/{number}"
    fun level(world: Int, number: Int) = "level/$world/$number"

    /** Over the Level card: the boosters the player has. */
    const val BOOSTERS = "boosters"

    /** Over the map, after every 10th level: pick one of two rewards. */
    const val CHEST = "chest"

    /** Over the map: today's puzzle and the streak. */
    const val DAILY = "daily"

    /** Over the map: Free play or Two players. */
    const val PLAY = "play"

    /** The game in progress: the saved game of its mode, new or resumed. */
    const val GAME = "game/{mode}"
    fun game(mode: GameMode) = "game/${mode.key}"
    const val RULES = "rules"
    const val STATS = "stats"
    const val SHOP = "shop"

    /** An item from the Shop, on a sample board. */
    const val COSMETIC = "cosmetic"
    const val SETTINGS = "settings"

    /** A page Settings opens: Privacy, Credits and the others of [SettingsPage]. */
    const val SETTINGS_DETAILS = "settings/details/{page}"
    fun settingsDetails(page: SettingsPage) = "settings/details/${page.key}"

    /** The POC's test settings; opens from the version line in Settings. */
    const val DEVELOPER = "developer"

    val ALL = listOf(LOADING, MAP, LEVEL, BOOSTERS, CHEST, DAILY, PLAY, GAME, RULES, STATS, SHOP, COSMETIC, SETTINGS, SETTINGS_DETAILS, DEVELOPER)
}

@Composable
fun DotsApp(navController: NavHostController = rememberNavController()) {
    val app = LocalContext.current.applicationContext as DotsApplication
    val back = { navController.popBackStack(); Unit }
    // Leaving a game, or a note over the map, always lands on the map (flows.md, "Around the board").
    // A new player's first game opens straight from the loading screen, with no map below it yet.
    val toMap = {
        if (!navController.popBackStack(Routes.MAP, inclusive = false)) {
            navController.navigate(Routes.MAP) { popUpTo(navController.graph.id) { inclusive = true } }
        }
        Unit
    }
    val appScope = rememberCoroutineScope()
    // Play the tutorial (Rules) and Replay the tutorial (Settings): level 1 as practice, which
    // changes no progress, with its guided steps shown again. It replaces the saved game, as
    // Restart does, and the map stays under it.
    val playTutorial = {
        appScope.launch {
            app.games.loaded.first { it }
            app.settings.update { it.copy(tutorialSkipped = false) }
            val settings = app.settings.settings.first()
            app.games.start(levelGame(Tutorial.levels.first(), settings.threatHighlight, now = System.currentTimeMillis(), practice = true))
            navController.navigate(Routes.game(GameMode.CAMPAIGN)) {
                if (navController.isOnStack(Routes.MAP)) popUpTo(Routes.MAP) else popUpTo(navController.graph.id) { inclusive = true }
            }
        }
        Unit
    }
    val top by navController.currentBackStackEntryAsState()
    val starting = top == null || top?.destination?.route == Routes.LOADING
    DotsTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            // Paper under the system bars too, as on Android's own start-up screen.
            containerColor = if (starting) colorResource(R.color.paper) else MaterialTheme.colorScheme.background,
        ) { insets ->
            NavHost(
                navController = navController,
                startDestination = Routes.LOADING,
                modifier = Modifier.padding(insets),
            ) {
                composable(Routes.LOADING) {
                    val tips = stringArrayResource(R.array.loading_tips)
                    val tip = rememberSaveable { tips.indices.random() }
                    var drawn by remember { mutableStateOf(false) }
                    LoadingScreen(tips[tip], onDrawn = { drawn = true }, outside = ScaffoldDefaults.contentWindowInsets)
                    LaunchedEffect(Unit) {
                        val progress = app.progress.progress.first()
                        app.games.loaded.first { it }
                        val opening = openingRoute(progress, app.games.saved(GameMode.CAMPAIGN))
                        if (opening != Routes.MAP && app.games.saved(GameMode.CAMPAIGN) == null) {
                            val settings = app.settings.settings.first()
                            app.games.start(levelGame(Campaign.levels.first(), settings.threatHighlight, now = System.currentTimeMillis()))
                        }
                        snapshotFlow { drawn }.first { it }
                        navController.navigate(opening) { popUpTo(Routes.LOADING) { inclusive = true } }
                    }
                }
                composable(Routes.MAP) {
                    val map by app.progress.map.collectAsStateWithLifecycle()
                    MapScreen(
                        map,
                        MapActions(
                            onLevel = { navController.navigate(Routes.level(it.world, it.number)) },
                            onDaily = { navController.navigate(Routes.DAILY) },
                            onPlay = { navController.navigate(Routes.PLAY) },
                            onRules = { navController.navigate(Routes.RULES) },
                            onStats = { navController.navigate(Routes.STATS) },
                            onShop = { navController.navigate(Routes.SHOP) },
                            onSettings = { navController.navigate(Routes.SETTINGS) },
                        ),
                    )
                }
                dialog(
                    Routes.LEVEL,
                    arguments = listOf(navArgument("world") { type = NavType.IntType }, navArgument("number") { type = NavType.IntType }),
                    dialogProperties = DialogProperties(usePlatformDefaultWidth = false),
                ) { entry ->
                    val world = entry.arguments?.getInt("world") ?: 1
                    val number = entry.arguments?.getInt("number") ?: 1
                    val level = remember(world, number) { Campaign.levels.firstOrNull { it.world == world && it.number == number } }
                    val saved by app.games.saved.collectAsStateWithLifecycle()
                    val scope = rememberCoroutineScope()
                    if (level == null) {
                        LaunchedEffect(Unit) { back() }
                    } else {
                        LevelCard(
                            level = level,
                            saved = saved[GameMode.CAMPAIGN]?.levelCode == level.code,
                            // Resume plays the saved game of this level; Start writes a new one to the
                            // store before the board opens, as Play a game does.
                            onStart = {
                                scope.launch {
                                    app.games.loaded.first { it }
                                    if (app.games.saved(GameMode.CAMPAIGN)?.levelCode != level.code) {
                                        val settings = app.settings.settings.first()
                                        app.games.start(levelGame(level, settings.threatHighlight, now = System.currentTimeMillis()))
                                    }
                                    navController.navigate(Routes.game(GameMode.CAMPAIGN)) { popUpTo(Routes.MAP) }
                                }
                            },
                            onBooster = { navController.navigate(Routes.BOOSTERS) },
                            onClose = back,
                        )
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
                dialog(Routes.PLAY, dialogProperties = DialogProperties(usePlatformDefaultWidth = false)) {
                    val scope = rememberCoroutineScope()
                    val saved by app.games.saved.collectAsStateWithLifecycle()
                    val settings by app.settings.settings.collectAsStateWithLifecycle(initialValue = null)
                    // A card resumes its mode's saved game, or writes a new game to the store before
                    // the board opens: the board always plays the saved game of its mode, also after
                    // Android restarts the app. Free play's computer plays at the campaign's strength.
                    fun play(mode: GameMode, replace: Boolean) {
                        scope.launch {
                            app.games.loaded.first { it }
                            if (replace || app.games.saved(mode) == null) {
                                val chosen = app.settings.settings.first()
                                val strength = if (mode == GameMode.FREE_PLAY) Campaign.strengthReached(app.progress.progress.first()) else null
                                app.games.start(freeGame(chosen, strength, now = System.currentTimeMillis()))
                            }
                            navController.navigate(Routes.game(mode)) { popUpTo(Routes.MAP) }
                        }
                    }
                    settings?.let { chosen ->
                        PlayAGameNote(
                            saved = saved.keys,
                            settings = chosen,
                            onSettings = { change -> scope.launch { app.settings.update(change) } },
                            onPlay = ::play,
                            onClose = back,
                        )
                    }
                }
                composable(Routes.GAME, arguments = listOf(navArgument("mode") { type = NavType.StringType })) { entry ->
                    val mode = GameMode.of(entry.arguments?.getString("mode")) ?: GameMode.CAMPAIGN
                    val loaded by app.games.loaded.collectAsStateWithLifecycle()
                    if (loaded) {
                        val start = remember { app.games.saved(mode) }
                        if (start == null) {
                            // Nothing to resume (the app was restarted after the game ended).
                            LaunchedEffect(Unit) { toMap() }
                        } else {
                            val settings = app.settings.settings.collectAsStateWithLifecycle(initialValue = null)
                            val game = viewModel {
                                GameViewModel(
                                    start = start,
                                    nextGame = {
                                        val now = System.currentTimeMillis()
                                        val chosen = settings.value ?: AppSettings()
                                        val level = (start.record as? LevelRecord)?.level
                                        if (level != null) levelGame(level, chosen.threatHighlight, now) else freeGameLike(start, chosen.threatHighlight, now)
                                    },
                                    computerFor = ::computerFor,
                                    journal = app.games,
                                    campaign = app.progress,
                                    placing = { settings.value?.placing ?: AppSettings().placing },
                                )
                            }
                            val scope = rememberCoroutineScope()
                            GameScreen(
                                game,
                                onToMap = toMap,
                                onRules = { navController.navigate(Routes.RULES) },
                                sound = settings.value?.sound ?: AppSettings().sound,
                                onSound = { on -> scope.launch { app.settings.update { it.copy(sound = on) } } },
                                placing = settings.value?.placing ?: AppSettings().placing,
                                guidesSkipped = settings.value?.tutorialSkipped ?: false,
                                onSkipGuides = { scope.launch { app.settings.update { it.copy(tutorialSkipped = true) } } },
                                // Next goes back to the map, which opens the next level's card.
                                onNext = { next ->
                                    toMap()
                                    if (next != null) navController.navigate(Routes.level(next.world, next.number))
                                },
                            )
                        }
                    }
                }
                composable(Routes.RULES) {
                    RulesScreen(onBack = back, onPlayTutorial = playTutorial)
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
                    val settings by app.settings.settings.collectAsStateWithLifecycle(initialValue = null)
                    settings?.let {
                        SettingsScreen(
                            settings = it,
                            // Saved in the app's scope, so leaving Settings never cuts a change short.
                            // Each change applies to the stored settings, so quick changes never undo each other.
                            onChange = { change -> appScope.launch { app.settings.update(change) } },
                            version = BuildConfig.VERSION_NAME,
                            onPage = { navController.navigate(Routes.settingsDetails(it)) },
                            onDeveloperMenu = { navController.navigate(Routes.DEVELOPER) },
                            onReplayTutorial = playTutorial,
                            onBack = back,
                        )
                    }
                }
                composable(Routes.SETTINGS_DETAILS, arguments = listOf(navArgument("page") { type = NavType.StringType })) { entry ->
                    val page = SettingsPage.of(entry.arguments?.getString("page"))
                    if (page == null) {
                        LaunchedEffect(Unit) { back() }
                    } else {
                        SettingsDetailsScreen(page, onPage = { navController.navigate(Routes.settingsDetails(it)) }, onBack = back)
                    }
                }
                composable(Routes.DEVELOPER) {
                    val context = LocalContext.current
                    val scope = rememberCoroutineScope()
                    val loggedGames by app.games.loggedGames.collectAsStateWithLifecycle()
                    DeveloperScreen(
                        onProgress = { progress -> scope.launch { app.progress.replace(progress) } },
                        loggedGames = loggedGames,
                        onShareLog = { scope.launch { shareLog(context, app) } },
                        onBack = back,
                    )
                }
            }
        }
    }
}

/**
 * Where the app opens after loading (flows.md, First launch and Returning player). Until Level 1 is
 * passed, the board of Level 1: its saved game (also a practice one), or a new one when no campaign
 * game is [saved]; no map and no Level card, so a new player's first tap is a move. After that, or
 * when another level is saved, the map.
 */
internal fun openingRoute(progress: Progress, saved: GameLogEntry?): String {
    val first = Campaign.levels.first()
    return when {
        progress.isPassed(first) -> Routes.MAP
        saved == null || (saved.record as? LevelRecord)?.level?.code == first.code -> Routes.game(GameMode.CAMPAIGN)
        else -> Routes.MAP
    }
}

/**
 * The level of a saved campaign game, as its code (`1-7`); null for any other game. A practice
 * game is left out, so the level's card starts the level for real.
 */
private val GameLogEntry.levelCode: String? get() = (record as? LevelRecord)?.level?.code?.takeUnless { practice }

/** True when [route] is on the back stack. */
private fun NavHostController.isOnStack(route: String): Boolean = runCatching { getBackStackEntry(route) }.isSuccess

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
