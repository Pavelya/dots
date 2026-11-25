package io.github.pavelya.dots.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LifecycleStartEffect
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
import dots.campaign.Campaign
import dots.campaign.Level
import dots.campaign.LevelRecord
import dots.campaign.Progress
import dots.campaign.Tutorial
import dots.daily.Daily
import dots.daily.DailyRecord
import dots.engine.Player
import dots.log.GameLogEntry
import dots.log.Outcome
import io.github.pavelya.dots.BuildConfig
import io.github.pavelya.dots.DotsApplication
import io.github.pavelya.dots.R
import io.github.pavelya.dots.audio.Effect
import io.github.pavelya.dots.audio.LocalAudio
import io.github.pavelya.dots.audio.Track
import io.github.pavelya.dots.data.AppSettings
import io.github.pavelya.dots.data.GameMode
import io.github.pavelya.dots.data.LevelEnd
import io.github.pavelya.dots.data.PuzzleClock
import io.github.pavelya.dots.data.ReviewRule
import io.github.pavelya.dots.data.LookPart
import io.github.pavelya.dots.data.PieceStyleId
import io.github.pavelya.dots.data.RingEffectId
import io.github.pavelya.dots.data.ThemeId
import io.github.pavelya.dots.game.GameViewModel
import io.github.pavelya.dots.game.computerFor
import io.github.pavelya.dots.game.dailyGame
import io.github.pavelya.dots.game.freeGame
import io.github.pavelya.dots.game.freeGameLike
import io.github.pavelya.dots.game.levelGame
import io.github.pavelya.dots.haptics.LocalHaptics
import io.github.pavelya.dots.ui.motion.Capturing
import io.github.pavelya.dots.ui.motion.LocalReduceMotion
import io.github.pavelya.dots.ui.motion.NoteMotion
import io.github.pavelya.dots.ui.motion.animationsRemoved
import io.github.pavelya.dots.ui.motion.rememberNoteMotion
import io.github.pavelya.dots.ui.screen.BoostersNote
import io.github.pavelya.dots.ui.screen.ChestNote
import io.github.pavelya.dots.ui.screen.CosmeticPreviewScreen
import io.github.pavelya.dots.ui.screen.DailyAction
import io.github.pavelya.dots.ui.screen.DailyPuzzleNote
import io.github.pavelya.dots.ui.screen.DeveloperScreen
import io.github.pavelya.dots.ui.screen.GameScreen
import io.github.pavelya.dots.ui.screen.LevelCard
import io.github.pavelya.dots.ui.screen.LoadingScreen
import io.github.pavelya.dots.ui.screen.MapActions
import io.github.pavelya.dots.ui.screen.boostersLeft
import io.github.pavelya.dots.ui.screen.boostersWaiting
import io.github.pavelya.dots.ui.screen.MapScreen
import io.github.pavelya.dots.ui.screen.PlayAGameNote
import io.github.pavelya.dots.ui.screen.RulesScreen
import io.github.pavelya.dots.ui.screen.SettingsDetailsScreen
import io.github.pavelya.dots.ui.screen.SettingsPage
import io.github.pavelya.dots.ui.screen.SettingsScreen
import io.github.pavelya.dots.ui.screen.ShopScreen
import io.github.pavelya.dots.ui.screen.ShopTab
import io.github.pavelya.dots.ui.screen.ShopView
import io.github.pavelya.dots.ui.screen.tab
import io.github.pavelya.dots.ui.screen.StandInScreen
import io.github.pavelya.dots.ui.screen.StatsScreen
import io.github.pavelya.dots.ui.screen.ThemeScreen
import io.github.pavelya.dots.ui.screen.PiecesScreen
import io.github.pavelya.dots.ui.screen.RingsScreen
import io.github.pavelya.dots.ui.screen.Unlock
import io.github.pavelya.dots.ui.theme.DotsTheme
import io.github.pavelya.dots.ui.theme.GameTheme
import io.github.pavelya.dots.ads.AdPlaces
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    /** Over the Level card: the boosters the player has in that level, and what each does. */
    const val BOOSTERS = "boosters/{world}/{number}"
    fun boosters(world: Int, number: Int) = "boosters/$world/$number"

    /** Over the map: the chest after a level (every 10th), to pick one of two rewards. */
    const val CHEST = "chest/{world}/{number}"
    fun chest(after: Level) = "chest/${after.world}/${after.number}"

    /** Over the map: today's puzzle and the streak. */
    const val DAILY = "daily"

    /** Over the map: Free play or Two players. */
    const val PLAY = "play"

    /** The game in progress: the saved game of its mode, new or resumed. */
    const val GAME = "game/{mode}"
    fun game(mode: GameMode) = "game/${mode.key}"
    const val RULES = "rules"
    const val STATS = "stats"

    /**
     * The leaderboards and achievements of Google Play Games, which O-02 builds and opens from
     * Stats. Until then no screen opens it (decision 0037).
     */
    const val LEADERBOARDS = "leaderboards"
    /** The Shop, open at a tab: Themes from the map, or the tab a Settings page's link names. */
    const val SHOP = "shop?tab={tab}"
    fun shop(tab: ShopTab = ShopTab.THEMES) = "shop?tab=${tab.key}"

    /** An item from the Shop, on a sample board: its tab's kind and its key (`cosmetic/themes/star-chart`). */
    const val COSMETIC = "cosmetic/{kind}/{key}"
    fun cosmetic(part: LookPart) = "cosmetic/${part.tab.key}/${part.key}"
    const val SETTINGS = "settings"

    /** A page Settings opens: Privacy, Credits and the others of [SettingsPage]. */
    const val SETTINGS_DETAILS = "settings/details/{page}"
    fun settingsDetails(page: SettingsPage) = "settings/details/${page.key}"

    /** From Settings: the themes the player can choose, each on a sample board (flows.md, "Choosing a theme"). */
    const val THEME = "settings/theme"

    /** From Settings: the piece styles the player can choose, each on the Shop's small board (flows.md, "Choosing pieces and rings"). */
    const val PIECES = "settings/pieces"

    /** From Settings: the ring effects the player can choose, the same way. */
    const val RINGS = "settings/rings"

    /** The POC's test settings; opens from the version line in Settings. */
    const val DEVELOPER = "developer"

    val ALL = listOf(LOADING, MAP, LEVEL, BOOSTERS, CHEST, DAILY, PLAY, GAME, RULES, STATS, LEADERBOARDS, SHOP, COSMETIC, SETTINGS, SETTINGS_DETAILS, THEME, PIECES, RINGS, DEVELOPER)
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
    val activity = LocalActivity.current
    // Play the tutorial (Rules) and Replay the tutorial (Settings): level 1 as practice, which
    // changes no progress, with its guided steps shown again. It replaces the saved game, as
    // Restart does, and the map stays under it.
    val playTutorial = {
        appScope.launch {
            app.games.loaded.first { it }
            app.settings.update { it.copy(tutorialSkipped = false) }
            val settings = app.settings.settings.first()
            app.journal.start(levelGame(Tutorial.levels.first(), settings.threatHighlight, now = System.currentTimeMillis(), practice = true))
            navController.navigate(Routes.game(GameMode.CAMPAIGN)) {
                if (navController.isOnStack(Routes.MAP)) popUpTo(Routes.MAP) else popUpTo(navController.graph.id) { inclusive = true }
            }
        }
        Unit
    }
    // The developer menu's "a year ahead" moves the daily puzzle's clock; high-contrast pieces go to every board.
    val appSettings by app.settings.settings.collectAsStateWithLifecycle(initialValue = null)
    // The theme the game screen is drawn in; the map, the notes over it and the menu screens stay Notebook (decision 0028).
    val themeInUse by app.themes.inUse.collectAsStateWithLifecycle(initialValue = ThemeId.DEFAULT)
    // The piece style and ring effect the game screen draws in that theme's inks (docs/design/cosmetics.md).
    val piecesInUse by app.themes.pieces.collectAsStateWithLifecycle(initialValue = PieceStyleId.DEFAULT)
    val ringsInUse by app.themes.rings.collectAsStateWithLifecycle(initialValue = RingEffectId.DEFAULT)
    LaunchedEffect(appSettings?.dailyAhead) {
        appSettings?.let { app.clock.shiftDays = if (it.dailyAhead) PuzzleClock.YEAR_AHEAD_DAYS else 0 }
    }
    val top by navController.currentBackStackEntryAsState()
    val starting = top == null || top?.destination?.route == Routes.LOADING
    // The phone's "Remove animations" (motion.md, rule 5), read again each time the app comes back.
    val reduceMotion = remember { mutableStateOf(animationsRemoved(app)) }
    LifecycleResumeEffect(Unit) {
        reduceMotion.value = animationsRemoved(app)
        // Google Play's purchases again (Play Billing's advice): one bought or refunded elsewhere shows up.
        app.store.refresh()
        onPauseOrDispose {}
    }
    // The game's sound (docs/design/sound.md): the player's settings, whether the game is on the
    // screen, and the music of the screen shown; the game screen names its own ([gameTrack]).
    val audio = remember { app.audio }
    // The vibrations follow the Vibration setting; nothing vibrates until it has been read.
    val haptics = remember { app.haptics }
    LaunchedEffect(appSettings) {
        appSettings?.let {
            audio.settings(it.sound, it.musicVolume, it.effectsVolume)
            haptics.settings(it.vibration)
        }
    }
    LifecycleStartEffect(audio) {
        audio.onScreen(true)
        // Turning the phone draws the app again; the music carries on.
        onStopOrDispose { if (activity?.isChangingConfigurations != true) audio.onScreen(false) }
    }
    var gameTrack by remember { mutableStateOf<Track?>(null) }
    val playing = remember { mutableStateOf<Track?>(null) }
    val track = musicFor(top?.destination?.route, gameTrack, playing.value)
    LaunchedEffect(track) {
        playing.value = track
        audio.music(track)
    }
    // The level just passed, whose unlock the map plays after Next.
    var unlock by remember { mutableStateOf<Unlock?>(null) }
    CompositionLocalProvider(LocalReduceMotion provides reduceMotion.value, LocalAudio provides audio, LocalHaptics provides haptics) {
        DotsTheme(highContrast = appSettings?.highContrast ?: false) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                // Paper under the system bars too, as on Android's own start-up screen.
                // The game screen's paper is its theme's, under the system bars too.
                containerColor = when {
                    starting -> colorResource(R.color.paper)
                    top?.destination?.route == Routes.GAME -> GameTheme.of(themeInUse).palette(GameTheme.isDark).paper
                    else -> MaterialTheme.colorScheme.background
                },
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
                                app.journal.start(levelGame(Campaign.levels.first(), settings.threatHighlight, now = System.currentTimeMillis(), progress = progress))
                            }
                            snapshotFlow { drawn }.first { it }
                            navController.navigate(opening) { popUpTo(Routes.LOADING) { inclusive = true } }
                        }
                    }
                    composable(Routes.MAP) {
                        val map by app.progress.map.collectAsStateWithLifecycle()
                        val daily by app.daily.saved.collectAsStateWithLifecycle()
                        // A red dot on Shop while an item earned since the Shop last opened waits there.
                        val owned by app.owned.collectAsStateWithLifecycle()
                        val shownInShop by app.marks.shown.collectAsStateWithLifecycle(initialValue = null)
                        MapScreen(
                            map,
                            MapActions(
                                onLevel = { navController.navigate(Routes.level(it.world, it.number)) },
                                onDaily = { navController.navigate(Routes.DAILY) },
                                onPlay = { navController.navigate(Routes.PLAY) },
                                onRules = { navController.navigate(Routes.RULES) },
                                onStats = { navController.navigate(Routes.STATS) },
                                onShop = { navController.navigate(Routes.shop()) },
                                onSettings = { navController.navigate(Routes.SETTINGS) },
                                onChest = { navController.navigate(Routes.chest(it.after)) },
                            ),
                            streak = daily?.streak(app.clock.today())?.days ?: 0,
                            unlock = unlock,
                            onUnlocked = { unlock = null },
                            covered = top?.destination?.route != Routes.MAP,
                            shopNew = shownInShop?.let { (owned.earned - it).isNotEmpty() } ?: false,
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
                        val progress by app.progress.saved.collectAsStateWithLifecycle()
                        val scope = rememberCoroutineScope()
                        if (level == null) {
                            LaunchedEffect(Unit) { back() }
                        } else NoteWindow(back) { motion, close ->
                            val campaignGame = saved[GameMode.CAMPAIGN]
                            LevelCard(
                                level = level,
                                saved = campaignGame?.levelCode == level.code,
                                boosters = progress?.boostersFor(level) ?: level.boosters,
                                // Start asks first when it would end another level's unfinished game.
                                replaces = campaignGame?.takeIf { it.levelCode != level.code && it.record.actions.isNotEmpty() }?.levelOrPractice,
                                // Resume plays the saved game of this level; Start writes a new one to the
                                // store before the board opens, as Play a game does.
                                onStart = {
                                    scope.launch {
                                        app.games.loaded.first { it }
                                        if (app.games.saved(GameMode.CAMPAIGN)?.levelCode != level.code) {
                                            val settings = app.settings.settings.first()
                                            val latest = app.progress.progress.first()
                                            app.journal.start(levelGame(level, settings.threatHighlight, now = System.currentTimeMillis(), progress = latest))
                                        }
                                        navController.navigate(Routes.game(GameMode.CAMPAIGN)) { popUpTo(Routes.MAP) }
                                    }
                                },
                                onBooster = { navController.navigate(Routes.boosters(level.world, level.number)) },
                                onClose = close,
                                motion = motion,
                            )
                        }
                    }
                    dialog(
                        Routes.BOOSTERS,
                        arguments = listOf(navArgument("world") { type = NavType.IntType }, navArgument("number") { type = NavType.IntType }),
                        dialogProperties = DialogProperties(usePlatformDefaultWidth = false),
                    ) { entry ->
                        val world = entry.arguments?.getInt("world") ?: 1
                        val number = entry.arguments?.getInt("number") ?: 1
                        val level = remember(world, number) { Campaign.levels.firstOrNull { it.world == world && it.number == number } }
                        val progress by app.progress.saved.collectAsStateWithLifecycle()
                        val left = level?.let { boostersLeft(it, progress) }.orEmpty()
                        val waiting = level?.let { boostersWaiting(it, progress) }.orEmpty()
                        NoteWindow(back) { motion, close -> BoostersNote(left = left, waiting = waiting, onClose = close, motion = motion) }
                    }
                    dialog(
                        Routes.CHEST,
                        arguments = listOf(navArgument("world") { type = NavType.IntType }, navArgument("number") { type = NavType.IntType }),
                        dialogProperties = DialogProperties(usePlatformDefaultWidth = false),
                    ) { entry ->
                        val world = entry.arguments?.getInt("world") ?: 1
                        val number = entry.arguments?.getInt("number") ?: 1
                        val chest = remember(world, number) { Campaign.chests.firstOrNull { it.after.world == world && it.after.number == number } }
                        val progress by app.progress.saved.collectAsStateWithLifecycle()
                        var picked by remember { mutableStateOf(false) }
                        // After an ad kept both rewards: both marked Kept until the next level's card opens.
                        var kept by remember { mutableStateOf(false) }
                        var saving by remember { mutableStateOf<Job?>(null) }
                        val adInputs by app.offers.inputs.collectAsStateWithLifecycle()
                        val canOffer = remember(adInputs) { app.offers.canOffer() }
                        val adOffer = chest != null && AdPlaces.chest(chest) && canOffer
                        LaunchedEffect(chest) { if (chest != null && AdPlaces.chest(chest)) app.offers.prepare() }
                        // Saved, then the next level's card opens over the map, as after a pick (flows.md, "Around the board").
                        val openNext = {
                            appScope.launch {
                                val next = Campaign.map(app.progress.progress.first()).next
                                toMap()
                                if (next != null && unlock == null) navController.navigate(Routes.level(next.world, next.number))
                            }
                            Unit
                        }
                        when {
                            chest == null -> LaunchedEffect(Unit) { back() }
                            // Opened already (or not yet ready): nothing to pick.
                            !picked && progress?.isReady(chest) == false -> LaunchedEffect(Unit) { back() }
                            else -> NoteWindow(back) { motion, close ->
                                ChestNote(
                                    chest,
                                    // Saved in the app's scope; then the next level's card opens over the map
                                    // (flows.md, "Around the board"). When the level's unlock plays behind
                                    // the chest, the map opens the card once the unlock is over.
                                    onPick = { reward ->
                                        if (!picked) {
                                            picked = true
                                            audio.play(Effect.REWARD) // the chest opens
                                            appScope.launch {
                                                app.progress.open(chest, reward)
                                                val next = Campaign.map(app.progress.progress.first()).next
                                                toMap()
                                                if (next != null && unlock == null) navController.navigate(Routes.level(next.world, next.number))
                                            }
                                        }
                                    },
                                    // Once both are kept, the note stays until the next level's card opens.
                                    onClose = if (kept) ({}) else close,
                                    motion = motion,
                                    adOffer = adOffer && !picked,
                                    kept = kept,
                                    // The ad (money.md, "Rewarded ads"): both rewards once it is watched,
                                    // then a moment to see them marked Kept. Closed early, the chest stays open.
                                    onWatchAd = {
                                        activity?.let { over ->
                                            app.offers.watch(
                                                over,
                                                reward = {
                                                    if (!picked) {
                                                        picked = true
                                                        kept = true
                                                        saving = appScope.launch { app.progress.open(chest, chest.choices) }
                                                    }
                                                },
                                                closed = { rewarded ->
                                                    if (rewarded) {
                                                        audio.play(Effect.REWARD)
                                                        appScope.launch {
                                                            saving?.join()
                                                            delay(BOTH_KEPT_MILLIS)
                                                            openNext()
                                                        }
                                                    }
                                                },
                                            )
                                        }
                                    },
                                )
                            }
                        }
                    }
                    dialog(Routes.DAILY, dialogProperties = DialogProperties(usePlatformDefaultWidth = false)) {
                        val scope = rememberCoroutineScope()
                        val saved by app.games.saved.collectAsStateWithLifecycle()
                        val loaded by app.games.loaded.collectAsStateWithLifecycle()
                        val progress by app.daily.saved.collectAsStateWithLifecycle()
                        val daily = progress
                        if (loaded && daily != null) NoteWindow(back) { motion, close ->
                            val today = app.clock.today()
                            // An unfinished attempt is shown until it ends, even after midnight; then today's puzzle.
                            val unfinished = (saved[GameMode.DAILY]?.record as? DailyRecord)?.puzzle
                            val puzzle = unfinished ?: Daily.puzzle(today)
                            val action = when {
                                unfinished != null -> DailyAction.RESUME
                                puzzle != null && daily.isSolved(puzzle.day) -> DailyAction.VIEW_RESULT
                                else -> DailyAction.PLAY
                            }
                            DailyPuzzleNote(
                                puzzle = puzzle,
                                today = today,
                                action = action,
                                streak = daily.streak(today),
                                toNextPuzzle = app.clock::toNextPuzzle,
                                // Play writes a new attempt to the store before the board opens; Resume and
                                // View result open the board, which finds the attempt (dailyStart).
                                onAction = {
                                    scope.launch {
                                        if (action == DailyAction.PLAY && puzzle != null && app.games.saved(GameMode.DAILY) == null) {
                                            val settings = app.settings.settings.first()
                                            app.journal.start(dailyGame(puzzle, settings.threatHighlight, now = System.currentTimeMillis()))
                                        }
                                        navController.navigate(Routes.game(GameMode.DAILY)) { popUpTo(Routes.MAP) }
                                    }
                                },
                                onClose = close,
                                motion = motion,
                            )
                        }
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
                                    app.journal.start(freeGame(chosen, strength, now = System.currentTimeMillis()))
                                }
                                navController.navigate(Routes.game(mode)) { popUpTo(Routes.MAP) }
                            }
                        }
                        settings?.let { chosen ->
                            NoteWindow(back) { motion, close ->
                                PlayAGameNote(
                                    saved = saved,
                                    settings = chosen,
                                    onSettings = { change -> scope.launch { app.settings.update(change) } },
                                    onPlay = ::play,
                                    onClose = close,
                                    motion = motion,
                                )
                            }
                        }
                    }
                    composable(Routes.GAME, arguments = listOf(navArgument("mode") { type = NavType.StringType })) { entry ->
                        val mode = GameMode.of(entry.arguments?.getString("mode")) ?: GameMode.CAMPAIGN
                        val loaded by app.games.loaded.collectAsStateWithLifecycle()
                        if (loaded) {
                            // Kept by the screen, not the composition: once the game ends the store holds none, and
                            // a recreated screen (the phone turned) must find the same game, with its Result.
                            val start = viewModel { OpenedGame(app.games.saved(mode) ?: if (mode == GameMode.DAILY) solvedDaily(app) else null) }.start
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
                                            val puzzle = (start.record as? DailyRecord)?.puzzle
                                            when {
                                                level != null -> levelGame(level, chosen.threatHighlight, now, app.progress.current())
                                                // Retry plays the same day's puzzle again, also after midnight.
                                                puzzle != null -> dailyGame(puzzle, chosen.threatHighlight, now)
                                                else -> freeGameLike(start, chosen.threatHighlight, now)
                                            }
                                        },
                                        computerFor = ::computerFor,
                                        journal = app.journal,
                                        campaign = app.progress,
                                        placing = { settings.value?.placing ?: AppSettings().placing },
                                        progress = app.progress::current,
                                        daily = app.daily,
                                        analytics = app.analytics,
                                        // The computer answers once the player's capture has played (motion.md, rule 2).
                                        holdFor = { move ->
                                            val dots = move.captures.sumOf { it.dots.size }
                                            if (reduceMotion.value || dots == 0) 0L else Capturing.of(dots).length.toLong()
                                        },
                                    )
                                }
                                // The board's music: its level's, or for free play and the daily puzzle the world reached.
                                val shown by game.ui.collectAsStateWithLifecycle()
                                val map by app.progress.map.collectAsStateWithLifecycle()
                                val music = gameMusic(shown.level, shown.vsComputer, map)
                                LaunchedEffect(music) { gameTrack = music }
                                DisposableEffect(Unit) { onDispose { gameTrack = null } }
                                val scope = rememberCoroutineScope()
                                val context = LocalContext.current
                                // A rewarded ad's offer on the Result of a failed level from World 2 (money.md):
                                // an ad is made ready while the level is played, and offered only when ready.
                                val retryAd = shown.level?.let { AdPlaces.retry(it, shown.practice) }
                                LaunchedEffect(retryAd) { if (retryAd != null) app.offers.prepare() }
                                val adInputs by app.offers.inputs.collectAsStateWithLifecycle()
                                val canOffer = remember(adInputs, shown.round, shown.levelEnd) { app.offers.canOffer() }
                                val failed = shown.levelEnd?.result?.isPassed == false
                                DotsTheme(
                                    GameTheme.of(themeInUse),
                                    dark = GameTheme.isDark,
                                    highContrast = GameTheme.highContrast,
                                    pieces = piecesInUse,
                                    rings = ringsInUse,
                                ) {
                                    GameScreen(
                                        game,
                                        onToMap = toMap,
                                        onRules = { navController.navigate(Routes.RULES) },
                                        sound = settings.value?.sound ?: AppSettings().sound,
                                        onSound = { on -> scope.launch { app.settings.update { it.copy(sound = on) } } },
                                        placing = settings.value?.placing ?: AppSettings().placing,
                                        links = settings.value?.links ?: AppSettings().links,
                                        guidesSkipped = settings.value?.tutorialSkipped ?: false,
                                        onSkipGuides = { scope.launch { app.settings.update { it.copy(tutorialSkipped = true) } } },
                                        onShare = { text -> shareText(context, text) },
                                        toNextPuzzle = app.clock::toNextPuzzle,
                                        // Google Play's review prompt, after a level won with three stars, at most once a month.
                                        onLevelResult = { level, end, practice ->
                                            activity?.let { over -> appScope.launch { askForReview(app, over, level, end, practice, System.currentTimeMillis()) } }
                                        },
                                        adOffer = retryAd?.takeIf { failed && canOffer },
                                        // The booster waits for the retry once the ad is watched; closed early, the offer stays.
                                        onWatchAd = { activity?.let { over -> app.offers.watch(over, reward = game::adWatched) } },
                                        // Next goes back to the map, which plays the level's unlock and opens the
                                        // next level's card. A chest the level made ready opens at once, and the
                                        // unlock plays behind it; the card then waits until the chest closes.
                                        onNext = { end ->
                                            toMap()
                                            val next = end?.next
                                            val passed = game.ui.value.level
                                            val played = if (end != null && passed != null) {
                                                Unlock(passed, end.starsBefore, end.result.stars, next, end.opened.worlds.isNotEmpty())
                                            } else {
                                                null
                                            }
                                            if (played != null && played.plays) unlock = played
                                            when {
                                                end?.chest != null -> navController.navigate(Routes.chest(end.chest.after))
                                                played != null && played.plays -> Unit
                                                next != null -> navController.navigate(Routes.level(next.world, next.number))
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }
                    composable(Routes.RULES) {
                        RulesScreen(onBack = back, onPlayTutorial = playTutorial)
                    }
                    composable(Routes.STATS) {
                        val map by app.progress.map.collectAsStateWithLifecycle()
                        val stats by app.stats.stats.collectAsStateWithLifecycle()
                        val daily by app.daily.saved.collectAsStateWithLifecycle()
                        val shown = map
                        val totals = stats
                        if (shown != null && totals != null) {
                            StatsScreen(
                                map = shown,
                                stats = totals,
                                longestStreak = daily?.longestStreak ?: 0,
                                // No Leaderboards or Achievements until O-02 builds them (decision 0037).
                                onBack = back,
                            )
                        }
                    }
                    composable(Routes.LEADERBOARDS) {
                        StandInScreen(R.string.screen_leaderboards, "O-02", onBack = back)
                    }
                    composable(Routes.SHOP, arguments = listOf(navArgument("tab") { type = NavType.StringType; defaultValue = ShopTab.THEMES.key })) { entry ->
                        var tab by rememberSaveable { mutableStateOf(ShopTab.of(entry.arguments?.getString("tab")).key) }
                        val view = shopView(app, themeInUse, piecesInUse, ringsInUse)
                        // Opening the Shop asks Google Play again, and clears the map's red dot.
                        LaunchedEffect(Unit) { app.store.refresh() }
                        val earned = view?.owned?.earned
                        LaunchedEffect(earned) { if (earned != null) appScope.launch { app.marks.shopOpened(earned) } }
                        view?.let {
                            ShopScreen(
                                view = it,
                                tab = ShopTab.of(tab),
                                onTab = { chosen -> tab = chosen.key },
                                onItem = { part -> navController.navigate(Routes.cosmetic(part)) },
                                onRestore = { app.store.refresh() },
                                onBack = back,
                            )
                        }
                    }
                    composable(
                        Routes.COSMETIC,
                        arguments = listOf(navArgument("kind") { type = NavType.StringType }, navArgument("key") { type = NavType.StringType }),
                    ) { entry ->
                        val part = lookPart(entry.arguments?.getString("kind"), entry.arguments?.getString("key"))
                        val view = shopView(app, themeInUse, piecesInUse, ringsInUse)
                        if (part == null) {
                            LaunchedEffect(Unit) { back() }
                        } else {
                            // The New mark goes once the player has seen the preview: it shows here, then not on the tile.
                            DisposableEffect(part) {
                                onDispose { part.cosmetic?.let { appScope.launch { app.marks.previewOpened(it) } } }
                            }
                            view?.let {
                                CosmeticPreviewScreen(
                                    part = part,
                                    view = it,
                                    onBuy = { part.cosmetic?.let { item -> activity?.let { over -> app.store.buy(over, item) } } },
                                    // Saved in the app's scope, like a setting; the Theme, Pieces and Rings pages read the same value.
                                    onUse = {
                                        appScope.launch {
                                            when (part) {
                                                is ThemeId -> app.themes.use(part)
                                                is PieceStyleId -> app.themes.use(part)
                                                is RingEffectId -> app.themes.use(part)
                                            }
                                        }
                                    },
                                    onBack = back,
                                )
                            }
                        }
                    }
                    composable(Routes.SETTINGS) {
                        val settings by app.settings.settings.collectAsStateWithLifecycle(initialValue = null)
                        // What the player owns decides the Theme, Pieces and Rings rows; read it so they follow a purchase or a refund.
                        val owned by app.owned.collectAsStateWithLifecycle()
                        val context = LocalContext.current
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
                                onFeedback = { sendFeedback(context) },
                                onBack = back,
                                // Only when there is more than one theme to choose from (decision 0029).
                                theme = themeInUse.takeIf { owned.let { app.themes.choices.size > 1 } },
                                onTheme = { navController.navigate(Routes.THEME) },
                                // Each only when more than Standard can be chosen (flows.md, "Choosing pieces and rings").
                                pieces = piecesInUse.takeIf { owned.let { app.themes.pieceChoices.size > 1 } },
                                onPieces = { navController.navigate(Routes.PIECES) },
                                rings = ringsInUse.takeIf { owned.let { app.themes.ringChoices.size > 1 } },
                                onRings = { navController.navigate(Routes.RINGS) },
                            )
                        }
                    }
                    composable(Routes.SETTINGS_DETAILS, arguments = listOf(navArgument("page") { type = NavType.StringType })) { entry ->
                        val page = SettingsPage.of(entry.arguments?.getString("page"))
                        if (page == null) {
                            LaunchedEffect(Unit) { back() }
                        } else {
                            val ads by app.ads.state.collectAsStateWithLifecycle()
                            val context = LocalContext.current
                            SettingsDetailsScreen(
                                page,
                                onPage = { navController.navigate(Routes.settingsDetails(it)) },
                                onBack = back,
                                onPrivacyPolicy = { openPrivacyPolicy(context) },
                                adChoices = ads.choices,
                                onAdChoices = { activity?.let { app.ads.choices(it) } },
                                showAdOffers = appSettings?.showAdOffers ?: AppSettings().showAdOffers,
                                // Saved in the app's scope, like every setting, so Back never cuts the change short.
                                onShowAdOffers = { on -> appScope.launch { app.settings.update { it.copy(showAdOffers = on) } } },
                            )
                        }
                    }
                    composable(Routes.THEME) {
                        val owned by app.owned.collectAsStateWithLifecycle()
                        ThemeScreen(
                            choices = owned.let { app.themes.choices },
                            inUse = themeInUse,
                            // Saved in the app's scope, like a setting, so Back never cuts the change short.
                            onUse = { theme -> appScope.launch { app.themes.use(theme) } },
                            debugNote = app.debugBuild,
                            // A player who lacks a theme gets a link to the Shop's Themes.
                            onShop = { navController.navigate(Routes.shop(ShopTab.THEMES)) },
                            onBack = back,
                        )
                    }
                    composable(Routes.PIECES) {
                        val owned by app.owned.collectAsStateWithLifecycle()
                        PiecesScreen(
                            theme = themeInUse,
                            choices = owned.let { app.themes.pieceChoices },
                            inUse = piecesInUse,
                            // Saved in the app's scope, like a setting, so Back never cuts the change short.
                            onUse = { style -> appScope.launch { app.themes.use(style) } },
                            debugNote = app.debugBuild,
                            onShop = { navController.navigate(Routes.shop(ShopTab.PIECES)) },
                            onBack = back,
                        )
                    }
                    composable(Routes.RINGS) {
                        val owned by app.owned.collectAsStateWithLifecycle()
                        RingsScreen(
                            theme = themeInUse,
                            choices = owned.let { app.themes.ringChoices },
                            inUse = ringsInUse,
                            onUse = { effect -> appScope.launch { app.themes.use(effect) } },
                            debugNote = app.debugBuild,
                            onShop = { navController.navigate(Routes.shop(ShopTab.RINGS)) },
                            onBack = back,
                        )
                    }
                    composable(Routes.DEVELOPER) {
                        val context = LocalContext.current
                        val scope = rememberCoroutineScope()
                        val loggedGames by app.games.loggedGames.collectAsStateWithLifecycle()
                        DeveloperScreen(
                            onProgress = { progress -> scope.launch { app.progress.replace(progress) } },
                            dailyAhead = appSettings?.dailyAhead ?: false,
                            onDailyAhead = { on -> appScope.launch { app.settings.update { it.copy(dailyAhead = on) } } },
                            onClearDaily = { appScope.launch { app.daily.clear() } },
                            ads = app.debugBuild,
                            adsInEu = appSettings?.adsInEu ?: false,
                            onAdsInEu = { on -> appScope.launch { app.settings.update { it.copy(adsInEu = on) } } },
                            onForgetConsent = { app.ads.forgetConsent() },
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

/**
 * What the Shop and its preview show (docs/design/money.md): Google Play's answers, what the player
 * owns and their stars, the looks in use, and the earned items marked New. Null until the progress
 * and the marks are read.
 */
@Composable
private fun shopView(app: DotsApplication, theme: ThemeId, pieces: PieceStyleId, rings: RingEffectId): ShopView? {
    val store by app.store.state.collectAsStateWithLifecycle()
    val owned by app.owned.collectAsStateWithLifecycle()
    val progress by app.progress.saved.collectAsStateWithLifecycle()
    val seen by app.marks.seen.collectAsStateWithLifecycle(initialValue = null)
    val stars = progress?.total ?: return null
    val opened = seen ?: return null
    return ShopView(store, owned, stars, theme, pieces, rings, fresh = owned.earned - opened)
}

/** The Shop item a preview's route names: a theme, piece style or ring effect by its tab and key. */
internal fun lookPart(kind: String?, key: String?): LookPart? = when (kind) {
    ShopTab.THEMES.key -> ThemeId.of(key)
    ShopTab.PIECES.key -> PieceStyleId.of(key)
    ShopTab.RINGS.key -> RingEffectId.of(key)
    else -> null
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

/** The level of a saved campaign game, as its code, a practice game's included; null for any other game. */
private val GameLogEntry.levelOrPractice: String? get() = (record as? LevelRecord)?.level?.code

/**
 * The game a game screen opened with, or null when there was nothing to play. It lives as long as
 * the screen on the back stack, so turning the phone keeps it, also after the game has ended; it
 * goes when the app is closed.
 */
private class OpenedGame(val start: GameLogEntry?) : ViewModel()

/**
 * Today's solved puzzle as a finished game, for View result: the attempt that solved it, replayed
 * to its final board. Null when today's puzzle is not solved (or there is none).
 */
private fun solvedDaily(app: DotsApplication): GameLogEntry? {
    val puzzle = Daily.puzzle(app.clock.today()) ?: return null
    val solution = app.daily.saved.value?.solution(puzzle.day) ?: return null
    val record = DailyRecord(puzzle, solution.actions)
    val end = runCatching { record.replay() }.getOrNull() ?: return null
    return GameLogEntry.start(record, puzzle.strength.text, threatHighlight = false, now = solution.startedAt)
        .copy(record = record, outcome = Outcome.FINISHED, red = end.score(Player.RED), blue = end.score(Player.BLUE))
}

/** Opens Android's share screen with [text]: the daily puzzle's result. */
private fun shareText(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    context.startActivity(Intent.createChooser(send, context.getString(R.string.daily_share_title)))
}

/**
 * Asks Google Play for its review prompt over [activity], now that [level] has ended with [end],
 * if [ReviewRule] allows it at [now]: a level won with three stars, not practice and not the
 * tutorial, and no ask in the last month. Any other end leaves the month's ask unspent. Returns
 * true when it asked; Google decides whether the prompt shows.
 */
internal suspend fun askForReview(app: DotsApplication, activity: Activity, level: Level, end: LevelEnd, practice: Boolean, now: Long): Boolean {
    if (!ReviewRule.earns(level, end.result, practice) || !app.settings.askReview(now)) return false
    app.reviews.show(activity)
    return true
}

/**
 * Opens the phone's email app with a message to the game's support address, which names the
 * build and the phone under the player's words. Without an email app, says where to write.
 */
private fun sendFeedback(context: Context) {
    try {
        context.startActivity(feedbackEmail(context))
    } catch (_: ActivityNotFoundException) {
        val address = context.getString(R.string.feedback_address)
        Toast.makeText(context, context.getString(R.string.feedback_no_email, address), Toast.LENGTH_LONG).show()
    }
}

/**
 * Opens the privacy policy (`site/privacy/`, on GitHub Pages) in the phone's browser. Without a
 * browser, says where the policy is.
 */
private fun openPrivacyPolicy(context: Context) {
    try {
        context.startActivity(privacyPolicyPage(context))
    } catch (_: ActivityNotFoundException) {
        val address = context.getString(R.string.privacy_policy_url)
        Toast.makeText(context, context.getString(R.string.privacy_no_browser, address), Toast.LENGTH_LONG).show()
    }
}

/** The privacy policy of [openPrivacyPolicy], for the phone's browser. */
internal fun privacyPolicyPage(context: Context): Intent =
    Intent(Intent.ACTION_VIEW, context.getString(R.string.privacy_policy_url).toUri()).addCategory(Intent.CATEGORY_BROWSABLE)

/** The feedback email of [sendFeedback], for Android's email apps (`mailto:`). */
internal fun feedbackEmail(context: Context): Intent = Intent(Intent.ACTION_SENDTO, "mailto:".toUri())
    .putExtra(Intent.EXTRA_EMAIL, arrayOf(context.getString(R.string.feedback_address)))
    .putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.feedback_subject))
    .putExtra(Intent.EXTRA_TEXT, context.getString(R.string.feedback_details, BuildConfig.VERSION_NAME, Build.VERSION.RELEASE, Build.MODEL))

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

/**
 * A note over the map in its own window (a dialog route): it slides up as it opens, and [content]'s
 * close, Back or a tap around it slides it down before the window goes ([back]). The note dims the
 * map itself as it moves, so the window neither dims nor animates (motion.md, "Level opening,
 * notes and cards"). A button that opens another screen leaves at once.
 */
@Composable
private fun NoteWindow(back: () -> Unit, content: @Composable (motion: NoteMotion, close: () -> Unit) -> Unit) {
    val window = (LocalView.current.parent as? DialogWindowProvider)?.window
    SideEffect {
        window?.setDimAmount(0f)
        window?.setWindowAnimations(0)
    }
    var open by remember { mutableStateOf(true) }
    val motion = rememberNoteMotion(open)
    val close = { open = false }
    BackHandler(enabled = open, onBack = close)
    LaunchedEffect(motion) {
        snapshotFlow { motion.gone }.first { it }
        back()
    }
    content(motion, close)
}

/** How long the chest shows both rewards marked Kept, after the ad, before the next level's card opens. */
private const val BOTH_KEPT_MILLIS = 1_200L
