package io.github.pavelya.dots.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dots.campaign.Level
import dots.campaign.MissionStatus
import dots.engine.Booster
import dots.engine.GameState
import dots.engine.Player
import io.github.pavelya.dots.R
import io.github.pavelya.dots.audio.Effect
import io.github.pavelya.dots.data.LevelEnd
import io.github.pavelya.dots.data.PlacingMethod
import io.github.pavelya.dots.game.BoosterAim
import io.github.pavelya.dots.game.GameUiState
import io.github.pavelya.dots.game.GameViewModel
import io.github.pavelya.dots.ui.board.BoardInput
import io.github.pavelya.dots.ui.motion.Beats
import io.github.pavelya.dots.ui.motion.Cues
import io.github.pavelya.dots.ui.motion.EndOfGame
import io.github.pavelya.dots.ui.motion.LocalReduceMotion
import io.github.pavelya.dots.ui.motion.OpeningNote
import io.github.pavelya.dots.ui.motion.PlayCues
import io.github.pavelya.dots.ui.motion.Timeline
import io.github.pavelya.dots.ui.motion.playOut
import io.github.pavelya.dots.ui.motion.rememberBoosterTimeline
import io.github.pavelya.dots.ui.motion.rememberMomentTimeline
import io.github.pavelya.dots.ui.motion.rememberNoteMotion
import io.github.pavelya.dots.ui.motion.rememberStopTimeline
import io.github.pavelya.dots.ui.motion.rememberTimeline
import io.github.pavelya.dots.ui.motion.slideUp
import io.github.pavelya.dots.ui.theme.Palette
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/**
 * What only a campaign level or the daily puzzle adds to the board: the [title] in the top row,
 * the [mission] note's content ([MissionNoteContent]; null while the opening shows the mission
 * big) and, in a level, the [boosters] in the tool bar (A-05, with Undo among them, decision
 * 0023). The daily puzzle has no tool bar ([boosters] null). Free play and two players leave it out.
 * While a capture plays, the note shows the mission as it was before it ([missionBefore]) until the
 * points land. A level's [world] adds its board touches beside the opponent ([BoardTouches]); the
 * daily puzzle belongs to no world. While the level opening hands over to the mission note, the
 * note shows through it as far as [missionShown] says, from 0 to 1.
 */
class CampaignParts(
    val title: String,
    val mission: (@Composable () -> Unit)?,
    val boosters: (@Composable RowScope.() -> Unit)?,
    val missionBefore: (@Composable () -> Unit)? = null,
    val world: Int? = null,
    val missionShown: () -> Float = { 1f },
)

/**
 * How long after Result opens its note has settled and stamped the stars (motion.md: the note
 * slides up in 300 ms, the star reveal takes 1100 ms), when a level's end goes to `onLevelResult`.
 */
internal const val RESULT_SETTLED_MILLIS = 1_500L

/**
 * The clock of a game's end (motion.md, "End of game"): it waits for the last move to play out,
 * then runs while the tools leave and the [areas] light up, and stops when the result note is to
 * come up. A game already over when the screen opens, and every game with animations removed,
 * starts at the end.
 */
@Composable
private fun rememberEndTimeline(ui: GameUiState, moment: Timeline): Timeline {
    val times = remember(ui.captureOrder) { EndOfGame(ui.captureOrder.size) }
    val reduce = LocalReduceMotion.current
    val overAtStart = remember(ui.round) { ui.isOver }
    val clock = remember(ui.round) { Animatable(if (overAtStart) times.length else 0f) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(ui.round, ui.isOver, reduce) {
        when {
            !ui.isOver -> clock.snapTo(0f)
            reduce || overAtStart -> clock.snapTo(times.length)
            else -> {
                val playOut = ui.moment?.playOut ?: 0f
                snapshotFlow { moment.now >= playOut }.first { it }
                clock.animateTo(times.length, tween(times.length.toInt(), easing = LinearEasing))
            }
        }
    }
    return remember(clock, times) { Timeline(clock, times.length, scope) }
}

/**
 * The game board, laid out as the canvas game frames (docs/design/canvas): see [GameBoard]. A
 * campaign level opens with its mission big over the board for a moment, then the mission shrinks
 * into the corner note (motion.md, "Level opening, notes and cards"); a touch anywhere ends the
 * opening at once and still reaches what it touched. The pause icon, or Back, opens Pause over the
 * board; when the game ends, Result opens over the final board (docs/design/flows.md, "Around the
 * board"). Pause and the booster preview slide up as they open and down as they close. [sound] is
 * the setting the sound icon in Pause turns on or off through [onSound]. Dots are placed by the
 * [placing] method, and the board draws links between touching dots of one side when [links] is on.
 *
 * Result's buttons: Retry and Play again start the next game here; Map goes to [onToMap]; Next, in
 * the campaign, goes to [onNext] with the level's end: the chest it made ready, which opens first,
 * and the level whose card the map should open. Once a level's Result has settled, its end goes to
 * [onLevelResult] with the level and whether it was practice; it does not if the player left first.
 *
 * In the campaign the tool bar holds the five boosters (A-05). A tap on one opens its preview, a
 * note over the player's line and the tool bar; on the board the player chooses the booster's
 * point by the same [placing] method as a dot. Confirm applies it, Cancel or Back closes it.
 *
 * In an open-ended game (decision 0025) Stop opens Stop's note in the same place: Stop here stops,
 * Cancel or Back closes it, and the game goes on.
 *
 * In the tutorial's levels (A-06) the guided steps show once the level opening has gone, unless
 * [guidesSkipped]. Skip hides them and calls [onSkipGuides], which saves that. In a practice game
 * of the tutorial, Next plays its next level here, and after the last one goes to the map.
 *
 * The daily puzzle (A-07) opens with its mission, as a level does, and has no tool bar and no
 * Restart in Pause: a failed attempt has Retry on Result. Its Result shows the streak and the time
 * to the next puzzle ([toNextPuzzle]), and Share hands the share text to [onShare].
 */
@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onToMap: () -> Unit,
    onRules: () -> Unit,
    sound: Boolean = true,
    onSound: (Boolean) -> Unit = {},
    onNext: (LevelEnd?) -> Unit = { onToMap() },
    placing: PlacingMethod = PlacingMethod.TWO_TAPS,
    links: Boolean = false,
    guidesSkipped: Boolean = false,
    onSkipGuides: () -> Unit = {},
    onShare: (String) -> Unit = {},
    toNextPuzzle: () -> Long = { 0L },
    onLevelResult: (level: Level, end: LevelEnd, practice: Boolean) -> Unit = { _, _, _ -> },
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val levelResult by rememberUpdatedState(onLevelResult)
    val names = Names(ui.vsComputer, viewModel.human)
    // Saved, so Pause is still open after Rules, opened from it, closes.
    var paused by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = !ui.isOver) { paused = !paused }
    // Registered later, so it comes first: Back closes an open booster preview.
    BackHandler(enabled = ui.aim != null && !paused) { viewModel.cancelBooster() }
    BackHandler(enabled = ui.askingStop && !paused) { viewModel.cancelStop() }
    val style = Palette.current
    val level = ui.level
    val daily = ui.daily
    // Each new game of a level, and each daily attempt, opens with its mission; a touch, the first move, or a moment, ends it.
    var opening by rememberSaveable(ui.round) { mutableStateOf((level != null || daily != null) && ui.fresh) }
    val opens = rememberTimeline(ui.round.takeIf { opening }, OpeningNote.LENGTH)
    LaunchedEffect(ui.round, ui.fresh) { if (!ui.fresh) opening = false }
    LaunchedEffect(opens) {
        snapshotFlow { opens.running }.first { !it }
        opening = false
    }
    // The big note shows until the clock ends; the corner note from the start of the shrink.
    val openingShown by remember(opens, ui.round) { derivedStateOf { opening && opens.running } }
    val holding by remember(opens, ui.round) { derivedStateOf { opening && opens.now < OpeningNote.HOLD } }
    val missionShown = { if (openingShown) OpeningNote.handOver(opens.now) else 1f }
    val guided = ui.guided && !guidesSkipped
    val guide = ui.guide?.takeIf { guided && !opening }
    val mission = ui.mission
    val campaign = when {
        level != null -> CampaignParts(
            title = stringResource(R.string.level_number, level.number),
            mission = if (holding || mission == null) null else missionNote(level, mission),
            boosters = { BoosterBar(boosterTools(ui, viewModel.human), style, viewModel::chooseBooster) },
            missionBefore = ui.moment?.missionBefore?.takeIf { !holding }?.let { missionNote(level, it) },
            world = level.world,
            missionShown = missionShown,
        )
        daily != null -> CampaignParts(
            title = stringResource(R.string.daily_number, daily.number),
            mission = if (holding || mission == null) null else missionNote(daily.level, mission),
            boosters = null,
            missionBefore = ui.moment?.missionBefore?.takeIf { !holding }?.let { missionNote(daily.level, it) },
            missionShown = missionShown,
        )
        else -> null
    }
    val moment = rememberMomentTimeline(ui.moment)
    val boosting = rememberBoosterTimeline(ui.booster)
    val stopping = rememberStopTimeline(ui.stopMoment)
    val ending = rememberEndTimeline(ui, moment)
    // Result comes up once the end has played; a tap meanwhile jumps to it.
    val ended by remember(ending) { derivedStateOf { !ending.running } }
    // The end and Result play their sounds only for a game that ended on this screen, not one opened over.
    val endedHere = remember(ui.round) { !ui.isOver }
    GameSounds(ui, viewModel, moment, boosting, stopping, ending, endedHere)
    // The notes over the board: each stays drawn while it slides down, the booster preview with its last aim.
    val pauseOpen = paused && !ui.isOver
    val pausing = rememberNoteMotion(pauseOpen)
    val aimOpen = ui.aim?.takeIf { !ui.isOver }
    val aiming = rememberNoteMotion(aimOpen != null)
    val lastAim = remember { arrayOfNulls<BoosterAim>(1) }
    if (aimOpen != null) lastAim[0] = aimOpen
    val askOpen = ui.askingStop && !ui.isOver
    val asking = rememberNoteMotion(askOpen)
    // Stop's note keeps what it said while it slides down: after Stop here the other side is to move.
    val lastOpen = remember { IntArray(1) }
    val lastOther = remember { arrayOfNulls<Player>(1) }
    if (askOpen) {
        lastOpen[0] = ui.openDots.size
        lastOther[0] = ui.state.toMove.opponent
    }
    // A touch anywhere ends the opening at once; the touch still reaches what it touched.
    val endOpening = Modifier.pointerInput(ui.round) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.changes.any { it.changedToDown() }) opening = false
            }
        }
    }
    BoxWithConstraints(endOpening) {
        GameBoard(
            ui = ui,
            human = viewModel.human,
            campaign = campaign,
            input = BoardInput(placing, viewModel::onTap, viewModel::aim, viewModel::release),
            onUndo = viewModel::undo,
            onPause = { paused = true },
            onStop = viewModel::askStop,
            onDone = viewModel::done,
            // Behind Pause, the board is out of reach, also for TalkBack.
            modifier = if (paused && !ui.isOver) Modifier.clearAndSetSemantics {} else Modifier,
            guided = guided,
            guide = guide,
            onSkipGuide = {
                viewModel.skipGuides()
                onSkipGuides()
            },
            links = links,
            moment = moment,
            ending = ending,
            boosting = boosting,
            stopping = stopping,
        )
        if (ui.isOver && !ended) {
            Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { ending.finish() } })
        }
        if (openingShown) {
            val top = maxHeight * 0.26f
            // It shrinks into the corner note's place: their top right corners meet, at the corner note's width and slant.
            val shrinking = Modifier.graphicsLayer {
                val p = OpeningNote.shrink(opens.now)
                if (p > 0f) {
                    val scale = 1f + (OPENING_CORNER_WIDTH.toPx() / size.width - 1f) * p
                    transformOrigin = TransformOrigin(1f, 0f)
                    scaleX = scale
                    scaleY = scale
                    translationX = (OPENING_SIDE - OPENING_CORNER_END).toPx() * p
                    translationY = (OPENING_CORNER_TOP - top).toPx() * p
                    rotationZ = OPENING_TURN * p
                    alpha = 1f - OpeningNote.handOver(opens.now)
                }
            }
            val place = Modifier.align(Alignment.TopCenter).padding(top = top, start = OPENING_SIDE, end = OPENING_SIDE).then(shrinking)
            if (level != null) LevelOpening(level, place) else if (daily != null) DailyOpening(daily, place)
        }
        val aim = aimOpen ?: lastAim[0]
        if (aim != null && !aiming.gone) {
            BoosterNote(
                aim = aim,
                method = placing,
                style = style,
                onCancel = viewModel::cancelBooster,
                onConfirm = viewModel::confirmBooster,
                modifier = Modifier.align(Alignment.BottomCenter).slideUp { aiming.up }.padding(start = 10.dp, end = 10.dp, bottom = 8.dp),
                motion = aiming,
            )
        }
        if (askOpen || !asking.gone) {
            StopNote(
                open = lastOpen[0],
                other = lastOther[0]?.takeUnless { ui.vsComputer }?.let { playerName(it) },
                style = style,
                onCancel = viewModel::cancelStop,
                onStop = viewModel::stop,
                modifier = Modifier.align(Alignment.BottomCenter).slideUp { asking.up }.padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
                motion = asking,
            )
        }
        if (pauseOpen || !pausing.gone) {
            PauseNote(
                sound = sound,
                onSound = onSound,
                onResume = { paused = false },
                onRestart = if (daily != null) null else ({ paused = false; viewModel.playAgain() }),
                onToMap = onToMap,
                onRules = onRules,
                motion = pausing,
            )
        }
        val end = ui.levelEnd
        val showsResult = ui.showsResult && ended
        if (showsResult && level != null && end != null) {
            LaunchedEffect(ui.round, end) {
                delay(RESULT_SETTLED_MILLIS)
                levelResult(level, end, ui.practice)
            }
        }
        if (showsResult) {
            ResultNote(
                ui = ui,
                names = names,
                onNext = {
                    val next = ui.levelEnd?.next
                    when {
                        !ui.practice -> onNext(ui.levelEnd)
                        next != null -> viewModel.practiceNext()
                        else -> onToMap()
                    }
                },
                onAgain = { paused = false; viewModel.playAgain() },
                onAgainWithHint = { paused = false; viewModel.retryWith(Booster.HINT) },
                onMap = onToMap,
                onShare = onShare,
                toNextPuzzle = toNextPuzzle,
                sounds = endedHere,
            )
        }
    }
}

/**
 * The game's sounds (docs/design/sound.md) and vibrations (motion.md, rule 6): each dot's [moment],
 * each booster's effect ([boosting]) and each stop ([stopping]) as it plays, and at the end of a
 * game that [endedHere] the captured areas lighting up ([ending]), unless animations are removed. Each moment is heard and
 * felt once, also when the screen is drawn again after Rules ([GameViewModel.heard]).
 */
@Composable
private fun GameSounds(ui: GameUiState, viewModel: GameViewModel, moment: Timeline, boosting: Timeline, stopping: Timeline, ending: Timeline, endedHere: Boolean) {
    val human = viewModel.human.takeIf { ui.vsComputer }
    val firstTime = { id: Int -> (id > viewModel.heard).also { if (it) viewModel.heard = id } }
    // The dots run low once a game (sound.md, sfx_dots_low).
    val warned = remember(ui.round) { BooleanArray(1) }
    val landed = ui.moment
    PlayCues(landed?.id, moment, play = { landed != null && firstTime(landed.id) }, beats = { landed?.let(Beats::moment).orEmpty() }) {
        val cues = landed?.let { Cues.moment(it, ui.state, human, warned[0]) }.orEmpty()
        if (cues.any { it.effect == Effect.DOTS_LOW }) warned[0] = true
        cues
    }
    val used = ui.booster
    PlayCues(used?.id, boosting, play = { used != null && firstTime(used.id) }, beats = { used?.let(Beats::booster).orEmpty() }) {
        used?.let(Cues::booster).orEmpty()
    }
    // A stop, by either side: the pen's full stop and a light vibration (sound.md, "Stop and Done").
    val stop = ui.stopMoment
    PlayCues(stop?.id, stopping, play = { stop != null && firstTime(stop.id) }, beats = { Beats.stop() }) { Cues.stop() }
    val reduce = LocalReduceMotion.current
    val endStarted by remember(ending) { derivedStateOf { ending.now > 0f } }
    PlayCues(ui.round.takeIf { ui.isOver && endedHere && !reduce && endStarted }, ending) { Cues.ending(EndOfGame(ui.captureOrder.size)) }
}

/** The level opening's note: 36 dp from each side of the screen, turned −1.5° ([LevelOpening]). */
private val OPENING_SIDE = 36.dp

/** Where it shrinks to: the corner note's top right corner and width, and its turn beyond the opening's (GameBoard's MissionNote). */
private val OPENING_CORNER_TOP = 6.dp
private val OPENING_CORNER_END = 12.dp
private val OPENING_CORNER_WIDTH = 136.dp
private const val OPENING_TURN = 3.5f

/** The mission note's content for [level] where its mission stands at [status]. */
internal fun missionNote(level: Level, status: MissionStatus): @Composable () -> Unit = { MissionNoteContent(level, status) }

const val BOARD_TAG = "board"

/** How players are called: "You" and "Computer" against the computer, colours between two people. */
internal class Names(val vsComputer: Boolean, val human: Player) {
    @Composable
    fun of(player: Player): String = when {
        !vsComputer -> playerName(player)
        player == human -> stringResource(R.string.player_you)
        else -> stringResource(R.string.player_computer)
    }
}

/** The end of a free game in one sentence, for TalkBack: "You win, 8 to 3!". */
@Composable
internal fun outcome(state: GameState, names: Names): String {
    val winner = state.winner ?: return stringResource(R.string.game_draw, state.score(Player.RED))
    val score = state.score(winner)
    val other = state.score(winner.opponent)
    return when {
        !names.vsComputer -> stringResource(R.string.game_won, playerName(winner), score, other)
        winner == names.human -> stringResource(R.string.game_you_win, score, other)
        else -> stringResource(R.string.game_computer_wins, score, other)
    }
}

/** The state of the game in one sentence, for TalkBack: the board's description. */
@Composable
internal fun status(ui: GameUiState, names: Names): String {
    val state = ui.state
    val level = ui.level ?: ui.daily?.level
    val mission = ui.mission
    if (ui.isOver && level != null && mission != null) {
        return if (ui.levelEnd?.result?.isPassed ?: level.isPassed(state)) stringResource(R.string.mission_done) else failureText(level, mission)
    }
    if (ui.isOver) return outcome(state, names)
    if (ui.thinking) return stringResource(R.string.game_thinking)
    val left = state.dotsLeft(state.toMove)
    val mover = state.toMove
    return when {
        names.vsComputer && mover == names.human && left != null ->
            pluralStringResource(R.plurals.game_your_turn_dots, left, left)
        names.vsComputer && mover == names.human -> stringResource(R.string.game_your_turn)
        left != null -> pluralStringResource(R.plurals.game_to_move_dots, left, names.of(mover), left)
        else -> stringResource(R.string.game_to_move, names.of(mover))
    }
}

@Composable
fun playerName(player: Player): String =
    stringResource(if (player == Player.RED) R.string.player_red else R.string.player_blue)
