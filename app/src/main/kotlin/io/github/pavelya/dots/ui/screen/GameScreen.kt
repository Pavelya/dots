package io.github.pavelya.dots.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import io.github.pavelya.dots.data.LevelEnd
import io.github.pavelya.dots.data.PlacingMethod
import io.github.pavelya.dots.game.GameUiState
import io.github.pavelya.dots.game.GameViewModel
import io.github.pavelya.dots.ui.board.BoardInput
import io.github.pavelya.dots.ui.theme.Palette
import kotlinx.coroutines.delay

/**
 * What only a campaign level or the daily puzzle adds to the board: the [title] in the top row,
 * the [mission] note's content ([MissionNoteContent]; null while the opening shows the mission
 * big) and, in a level, the [boosters] in the tool bar (A-05, with Undo among them, decision
 * 0023). The daily puzzle has no tool bar ([boosters] null). Free play and two players leave it out.
 */
class CampaignParts(
    val title: String,
    val mission: (@Composable () -> Unit)?,
    val boosters: (@Composable RowScope.() -> Unit)?,
)

/** How long the level opening shows the mission big over the board (motion.md, Level opening: 1200 ms, then 300 ms to shrink). */
internal const val OPENING_MILLIS = 1_500L

/**
 * How long after Result opens its note has settled and stamped the stars (motion.md: the note
 * slides up in 300 ms, the star reveal takes 1100 ms), when a level's end goes to `onLevelResult`.
 */
internal const val RESULT_SETTLED_MILLIS = 1_500L

/**
 * The game board, laid out as the canvas game frames (docs/design/canvas): see [GameBoard]. A
 * campaign level opens with its mission big over the board for a moment, then the mission sits in
 * the corner note. The pause icon, or Back, opens Pause over the board; when the game ends, Result
 * opens over the final board (docs/design/flows.md, "Around the board"). [sound] is the setting the
 * sound icon in Pause turns on or off through [onSound]. Dots are placed by the [placing] method,
 * and the board draws links between touching dots of one side when [links] is on.
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
    val style = Palette.current
    val level = ui.level
    val daily = ui.daily
    // Each new game of a level, and each daily attempt, opens with its mission; the first move, or a moment, ends it.
    var opening by rememberSaveable(ui.round) { mutableStateOf((level != null || daily != null) && ui.fresh) }
    LaunchedEffect(ui.round, ui.fresh) {
        if (!ui.fresh) {
            opening = false
        } else if (opening) {
            delay(OPENING_MILLIS)
            opening = false
        }
    }
    val guided = ui.guided && !guidesSkipped
    val guide = ui.guide?.takeIf { guided && !opening }
    val mission = ui.mission
    val campaign = when {
        level != null -> CampaignParts(
            title = stringResource(R.string.level_number, level.number),
            mission = if (opening || mission == null) null else missionNote(level, mission),
            boosters = { BoosterBar(boosterTools(ui, viewModel.human), style, viewModel::chooseBooster) },
        )
        daily != null -> CampaignParts(
            title = stringResource(R.string.daily_number, daily.number),
            mission = if (opening || mission == null) null else missionNote(daily.level, mission),
            boosters = null,
        )
        else -> null
    }
    BoxWithConstraints {
        GameBoard(
            ui = ui,
            human = viewModel.human,
            campaign = campaign,
            input = BoardInput(placing, viewModel::onTap, viewModel::aim, viewModel::release),
            onUndo = viewModel::undo,
            onPause = { paused = true },
            onStop = viewModel::stop,
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
        )
        if (opening) {
            val place = Modifier.align(Alignment.TopCenter).padding(top = maxHeight * 0.26f, start = 36.dp, end = 36.dp)
            if (level != null) LevelOpening(level, place) else if (daily != null) DailyOpening(daily, place)
        }
        val aim = ui.aim
        if (aim != null && !ui.isOver) {
            BoosterNote(
                aim = aim,
                method = placing,
                style = style,
                onCancel = viewModel::cancelBooster,
                onConfirm = viewModel::confirmBooster,
                modifier = Modifier.align(Alignment.BottomCenter).padding(start = 10.dp, end = 10.dp, bottom = 8.dp),
            )
        }
        if (paused && !ui.isOver) {
            PauseNote(
                sound = sound,
                onSound = onSound,
                onResume = { paused = false },
                onRestart = if (daily != null) null else ({ paused = false; viewModel.playAgain() }),
                onToMap = onToMap,
                onRules = onRules,
            )
        }
        val end = ui.levelEnd
        if (ui.showsResult && level != null && end != null) {
            LaunchedEffect(ui.round, end) {
                delay(RESULT_SETTLED_MILLIS)
                levelResult(level, end, ui.practice)
            }
        }
        if (ui.showsResult) {
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
            )
        }
    }
}

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
