package io.github.pavelya.dots.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dots.campaign.Mission
import dots.campaign.Strength
import dots.campaign.Tutorial
import dots.engine.Booster
import dots.engine.GameState
import dots.engine.Player
import dots.engine.Pos
import io.github.pavelya.dots.R
import io.github.pavelya.dots.game.Bubble
import io.github.pavelya.dots.game.GameUiState
import io.github.pavelya.dots.game.OpponentLook
import io.github.pavelya.dots.game.bossCountdown
import io.github.pavelya.dots.ui.board.AreaGlow
import io.github.pavelya.dots.ui.board.BoardInput
import io.github.pavelya.dots.ui.board.BoardMarks
import io.github.pavelya.dots.ui.board.BoardStyle
import io.github.pavelya.dots.ui.board.PlayableBoard
import io.github.pavelya.dots.ui.board.bounds
import io.github.pavelya.dots.ui.board.drawPiece
import io.github.pavelya.dots.ui.motion.BombEffect
import io.github.pavelya.dots.ui.motion.BoosterTimes
import io.github.pavelya.dots.ui.motion.DoubleDotEffect
import io.github.pavelya.dots.ui.motion.Ease
import io.github.pavelya.dots.ui.motion.EndOfGame
import io.github.pavelya.dots.ui.motion.Placing
import io.github.pavelya.dots.ui.motion.RunningOut
import io.github.pavelya.dots.ui.motion.StopTimes
import io.github.pavelya.dots.ui.motion.Timeline
import io.github.pavelya.dots.ui.motion.UndoEffect
import io.github.pavelya.dots.ui.motion.capturing
import io.github.pavelya.dots.ui.motion.phase
import io.github.pavelya.dots.ui.motion.rememberBoosterTimeline
import io.github.pavelya.dots.ui.motion.rememberChangeTimeline
import io.github.pavelya.dots.ui.motion.rememberMomentTimeline
import io.github.pavelya.dots.ui.motion.rememberStopTimeline
import io.github.pavelya.dots.ui.theme.BoardLook
import io.github.pavelya.dots.ui.theme.Palette
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.delay

/**
 * The game board as the canvas game frames lay it out (docs/design/canvas, `game-start`,
 * `game-threat`, `game-big-capture`; the wireframes' "Game board: the parts" and "Two players"):
 *
 * - **Top row:** the pause icon (left), the game's name, and in the campaign and the daily puzzle
 *   the mission note (right).
 * - **The opponent above the board:** against the computer, its face taped in like a photo, with a
 *   speech bubble; between two people, Blue's piece in the photo. Under it the opponent's piece,
 *   name, tally and score. On a boss level with a double move, the boss's counter sits right above
 *   the opponent's line ([BossCounter], the wireframes' "Boss turn"); it takes its room before the
 *   photo. The boss's two dots of that turn are numbered 1 and 2 on the board until the player's
 *   next dot.
 * - **The board,** edge to edge, at the largest whole-dp spacing that fits ([boardSpacing]). Dots
 *   are placed by the [input]'s method ([PlayableBoard]).
 * - **The player below the board:** piece, name, tally and score, and the dots left.
 * - **The tool bar:** the campaign's five boosters ([BoosterBar], A-05), or Undo in free play (decision 0023). Two
 *   players and the daily puzzle have none. An open-ended game (decision 0025) has Stop at the left and free play's
 *   Undo at the right, and "no limit" where the dots left would be. Stop opens Stop's note ([onStop], [StopNote]),
 *   which circles the player's dots that can still be captured. After a stop, a "stopped" chip sits beside the
 *   stopper's name, the stopper's open dots stay circled, and the collector's row takes the tool bar's place: Undo
 *   in free play, what to do, and Done ([CollectRow]). When the player stopped, the computer collects by itself.
 * - **The tutorial** (the wireframes' "Tutorial level"): a [guided] level has no tool bar and
 *   keeps room just above the board for the step note ([guide], [TutorialNote]), which shows in
 *   place of the mission note; the board circles the point to tap, with a hand pointing at it.
 *   Skip calls [onSkipGuide]. When there is no room above the board, the note lies over its top
 *   rows.
 *
 * The board draws the links between touching dots of one side when [links] is on (the player's
 * setting), and always in the tutorial's linking level while its steps are guided: that level
 * teaches them.
 *
 * In a booster preview the board shows where the booster can act and what it would do ([BoardMarks]);
 * after a Hint its point stays circled until the dot lands, and after a Double dot a chip beside
 * the player's name counts the dots still to place this turn.
 *
 * A pen stroke under a name marks the side to move. Between two people, a banner names the side to
 * play at each change of turn. Spare height goes to the opponent ([opponentFit]); when there is too
 * little for the full photo, the board touches go and the photo gets smaller, then moves beside the
 * opponent's line without its bubble, and on a small phone the line moves into the top row in place
 * of the game's name (game-screens.md, "When the photo doesn't fit"; the canvas frames `fit-*`).
 *
 * Every ink, size and text style comes from the theme ([Palette], [BoardStyle], [NoteText]); the
 * opponent's face is a picture ([Faces]).
 *
 * Motion (docs/design/motion.md): the [moment] clock plays the last dot's moment on the board. In a
 * capture the captor's tally and score, and the mission note, take the points when the "+N" lands,
 * then swell; the opponent's face changes on its beat; a big capture shakes the board. A touch on
 * the board finishes the moment. From 5 dots left the count shows the last dots, and at 3 the
 * mission note swings once. At the end of a game the [ending] clock, when there is one, plays the
 * pause icon and the tool bar leaving and the captured areas lighting up, and the face keeps its
 * look until the result note comes.
 *
 * The [boosting] clock plays the booster just used (motion.md, "Boosters"): its count swells, the
 * board plays its effect, and a bomb shakes the board. While Undo rubs out a turn the scores, the
 * dots left and the face stay as they were until that part is gone, and a booster it gives back
 * flies to its button. After a Double dot the chip grows in, counts down and leaves. A touch on the
 * board finishes a bomb and an Undo.
 *
 * The [stopping] clock plays a stop (motion.md, "Stop and Done"): the tool bar leaves, the chip
 * grows in, the stopper's open dots get their rings, and the collector's row slides up. The rings
 * of Stop's note grow in as it opens and shrink away after Cancel.
 */
@Composable
internal fun GameBoard(
    ui: GameUiState,
    human: Player,
    campaign: CampaignParts?,
    input: BoardInput,
    onUndo: () -> Unit,
    onPause: () -> Unit,
    modifier: Modifier = Modifier,
    onStop: () -> Unit = {},
    onDone: () -> Unit = {},
    guided: Boolean = false,
    guide: Tutorial.Step? = null,
    onSkipGuide: () -> Unit = {},
    links: Boolean = false,
    moment: Timeline = rememberMomentTimeline(ui.moment),
    ending: Timeline? = null,
    boosting: Timeline = rememberBoosterTimeline(ui.booster),
    stopping: Timeline = rememberStopTimeline(ui.stopMoment),
) {
    val style = Palette.current
    val boardStyle = BoardStyle.current
    val state = ui.state
    val landed = ui.moment
    val capture = landed?.capturing
    val spots = remember { Spots() }
    // What the moment holds back until its beat: the points, the mission note and the face.
    val pointsIn by remember(landed, moment) { derivedStateOf { capture == null || moment.now >= capture.score } }
    val faceIn by remember(landed, moment) { derivedStateOf { capture == null || moment.now >= capture.face } }
    val endTimes = remember(ui.captureOrder) { EndOfGame(ui.captureOrder.size) }
    val toolsGone by remember(ui.isOver, ending) { derivedStateOf { ui.isOver && (ending == null || ending.now >= EndOfGame.TOOLS_OUT) } }
    val endFace by remember(ui.isOver, ending, endTimes) { derivedStateOf { ui.isOver && ending != null && ending.now < endTimes.noteStart } }
    // What Undo holds back until its part of the turn is gone: the answer's, then the player's own.
    val used = ui.booster
    val undone = used?.undone
    val answerGone by remember(used, boosting) { derivedStateOf { undone == null || boosting.now >= UndoEffect.ANSWER + UndoEffect.RUB } }
    val ownGone by remember(used, boosting) { derivedStateOf { undone == null || boosting.now >= UndoEffect.SETTLED } }
    val givenBackLanded by remember(used, boosting) { derivedStateOf { undone?.givenBack == null || boosting.now >= UndoEffect.FLIGHT_TO } }
    val shownScore = { player: Player ->
        val held = landed?.takeIf { !pointsIn && it.by == player }?.points ?: 0
        val undoing = undone?.takeIf { !(if (player == used?.by) ownGone else answerGone) }
        undoing?.before?.score(player) ?: (state.score(player) - held)
    }
    val scoreSwell = { player: Player ->
        val captured = if (capture != null && landed?.by == player) capture.scoreScale(moment.now) else 1f
        val lit = if (ending != null && ui.isOver) {
            ui.captureOrder.withIndex().maxOfOrNull { (i, area) -> if (area.by == player) 1f + 0.3f * endTimes.glow(ending.now, i) else 1f } ?: 1f
        } else {
            1f
        }
        max(captured, lit)
    }
    val look = when {
        endFace -> ui.opponentInPlay ?: ui.opponent
        !faceIn -> landed?.opponentBefore ?: ui.opponent
        undone != null && !ownGone -> undone.opponentBefore ?: ui.opponent
        else -> ui.opponent
    }
    val toolsOut = { if (ending != null && ui.isOver) endTimes.toolsOut(ending.now) else if (ui.isOver) 1f else 0f }
    val shownState = if (undone != null && !ownGone) undone.before else state
    val left = state.dotsLeft(if (ui.vsComputer) human else Player.RED)
    val missionOpen = ui.mission?.isDecided != true
    val nudge = rememberChangeTimeline(left, RunningOut.NUDGE) { old, new -> missionOpen && new == RunningOut.NUDGE_AT && old != null && old > RunningOut.NUDGE_AT }
    val names = Names(ui.vsComputer, human)
    val below = if (ui.vsComputer) human else Player.RED
    val above = below.opponent
    // The daily puzzle has no tool bar: no boosters and no undo (decision 0023).
    val toolBar = !guided && (if (campaign != null) campaign.boosters != null else ui.vsComputer || ui.openEnded)
    val turnBanner = turnBanner(ui)
    val chipLeaving by remember(landed, moment) { derivedStateOf { landed?.doubleDots == 1 && moment.now < DoubleDotEffect.CHIP_OUT } }
    val toolMotion = remember(spots, used, boosting) {
        ToolMotion(
            spots,
            scale = { booster ->
                when {
                    used == null -> 1f
                    booster == used.booster -> BoosterTimes.countScale(boosting.now)
                    booster == undone?.givenBack -> UndoEffect.countScale(boosting.now)
                    else -> 1f
                }
            },
            held = { booster -> booster == undone?.givenBack && !givenBackLanded },
        )
    }
    // The dashed rings round the dots that can still be captured: on Stop's note, and after a stop.
    val noteRings = rememberChangeTimeline(ui.askingStop, StopTimes.RINGS)
    val lastOpen = remember(ui.round) { arrayOf<List<Pos>>(emptyList()) }
    if (ui.askingStop) lastOpen[0] = ui.openDots
    val stop = ui.stopMoment
    // A stop made on Stop's note keeps the note's rings; the computer's stop draws them in.
    val ringsGrow = stop != null && ui.vsComputer && stop.by != human
    val marks = boardMarks(ui).let { base ->
        when {
            state.isCleanup -> base.copy(open = ui.openDots, openSize = { if (ringsGrow) StopTimes.ringsIn(stopping.now) else 1f })
            ui.askingStop -> base.copy(open = ui.openDots, openSize = { StopTimes.ringsIn(noteRings.now) })
            else -> base.copy(open = lastOpen[0], openSize = { StopTimes.ringsOut(noteRings.now) })
        }
    }
    val stopper = state.stopper?.takeUnless { ui.isOver }
    val numbers = BossNumbers(rememberTextMeasurer(), NoteText.small.copy(color = style.card, fontWeight = FontWeight.ExtraBold, lineHeight = TextUnit.Unspecified))
    val chipSize = { if (stop != null) StopTimes.chip(stopping.now) else 1f }
    val stoppedChip = @Composable { player: Player ->
        if (stopper == player) StoppedChip(names.of(player), style, Modifier.graphicsLayer { val size = chipSize(); scaleX = size; scaleY = size })
    }
    BoxWithConstraints(modifier.fillMaxSize().background(style.paper).onGloballyPositioned { spots.screen = it }) {
        val bottom = PLAYER_LINE + if (toolBar) TOOL_BAR else 0.dp
        val spacing = boardSpacing(maxWidth, maxHeight - TOP_ROW - bottom, state.width, state.height)
        val boardHeight = spacing * state.height
        val spare = maxHeight - TOP_ROW - bottom - boardHeight
        // The tutorial's note takes the space just above the board, when there is that much.
        val guideRoom = if (guided && spare >= GUIDE_ROOM) GUIDE_ROOM else 0.dp
        val room = spare - guideRoom
        val countdown = if (ui.vsComputer && !ui.isOver) bossCountdown(state, above) else null
        val fit = opponentFit(room, countdown != null)
        // On a small phone the opponent's line moves into the top row, and the mission note narrows to make room.
        val compact = fit == OpponentFit.TopRow
        // The boss's counter sits right above the opponent's line; with the line in the top row, above the board when it fits.
        val counter = countdown?.takeIf { !compact || room >= COUNTER + COUNTER_GAP }
        val boss = Faces.boss(ui.level)
        val opponentLine = @Composable { inTopRow: Boolean ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SideLine(above, names.of(above), shownScore(above), toMove = isToMove(ui, above), style, inTopRow, swell = { scoreSwell(above) }, onTally = { spots.tally[above] = it })
                stoppedChip(above)
            }
        }
        Column(Modifier.fillMaxSize()) {
            val title = campaign?.title ?: stringResource(if (ui.vsComputer) R.string.play_free else R.string.play_two)
            TopRow(showPause = !toolsGone, style = style, onPause = onPause, leaving = toolsOut) {
                if (compact) {
                    Box(Modifier.align(Alignment.CenterStart).padding(start = 52.dp)) { opponentLine(true) }
                } else {
                    Text(
                        title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = style.graphite,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                when (fit) {
                    is OpponentFit.Above -> {
                        // The world's board touches sit in the paper beside the full photo; they are the first to go.
                        val touches = if (fit.full) Modifier.boardTouches(campaign?.world, style) else Modifier
                        Box(Modifier.fillMaxWidth().then(touches), contentAlignment = Alignment.TopCenter) {
                            Photo(fit.photo, above, look, ui.strength, boss, style)
                        }
                        Spacer(Modifier.height(8.dp))
                        if (counter != null) {
                            BossCounter(counter, style.ink(above), style)
                            Spacer(Modifier.height(COUNTER_GAP))
                        }
                        opponentLine(false)
                        Spacer(Modifier.height(12.dp))
                    }
                    OpponentFit.Beside -> Box(Modifier.fillMaxWidth().height(BESIDE_ROOM)) {
                        SmallPhoto(above, look, ui.strength, boss, style, Modifier.offset(x = 16.dp, y = 6.dp))
                        // With a counter it takes the row's top and the line sits under it; without, the line is level with the photo.
                        Column(Modifier.padding(start = 16.dp + SMALL_PHOTO + 14.dp, top = if (counter != null) 0.dp else 6.dp + (SMALL_PHOTO - 40.dp) / 2)) {
                            if (counter != null) {
                                BossCounter(counter, style.ink(above), style)
                                Spacer(Modifier.height(COUNTER_GAP))
                            }
                            opponentLine(false)
                        }
                    }
                    OpponentFit.TopRow -> if (counter != null) {
                        BossCounter(counter, style.ink(above), style)
                        Spacer(Modifier.height(COUNTER_GAP))
                    }
                }
                Spacer(Modifier.height(guideRoom))
            }
            PlayableBoard(
                state = state,
                style = boardStyle,
                description = stringResource(R.string.board_description, state.width, state.height, status(ui, names)),
                input = input,
                preview = ui.preview,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(spacing * state.width, boardHeight)
                    .onGloballyPositioned { spots.board = it }
                    // A big capture and a bomb shake the board sideways (motion.md, "Big capture", "Boosters").
                    .graphicsLayer {
                        val bomb = if (used?.booster == Booster.BOMB) BombEffect.shake(boosting.now) else 0f
                        translationX = ((capture?.shake(moment.now) ?: 0f) + bomb).dp.toPx()
                    }
                    .bossNumbers(ui.bossDots, state, spacing, style.ink(above), numbers)
                    .testTag(BOARD_TAG),
                lastMove = ui.lastMove,
                dangers = ui.dangers,
                marked = ui.level?.mission?.let(::missionDot)?.takeUnless { ui.isOver },
                pointer = guide?.point,
                marks = marks,
                showLinks = links || (guided && ui.level?.let(Tutorial::lesson) == Tutorial.Lesson.LINKING),
                moment = landed,
                time = { moment.now },
                edgeMarks = ui.edgeMarks,
                glow = if (ending != null && ui.isOver) AreaGlow(ui.captureOrder, endTimes) { ending.now } else null,
                booster = used,
                boosterTime = { boosting.now },
                onTouch = {
                    moment.finish()
                    if (used != null && BoosterTimes.tapFinishes(used.booster)) boosting.finish()
                },
            )
            Row(
                modifier = Modifier.fillMaxWidth().height(PLAYER_LINE).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SideLine(below, names.of(below), shownScore(below), toMove = isToMove(ui, below), style, swell = { scoreSwell(below) }, onTally = { spots.tally[below] = it })
                // The chip grows in with the booster, swells as it turns to ×1, and shrinks away after the second dot.
                val chip = ui.doubleDots ?: 1.takeIf { chipLeaving }
                chip?.let {
                    val scale = Modifier.graphicsLayer {
                        val size = when {
                            landed?.doubleDots == 2 -> DoubleDotEffect.chipSwell(moment.now)
                            landed?.doubleDots == 1 -> DoubleDotEffect.chipOut(moment.now)
                            used?.booster == Booster.DOUBLE_DOT -> DoubleDotEffect.chipIn(boosting.now)
                            else -> 1f
                        }
                        scaleX = size
                        scaleY = size
                    }
                    DoubleDotChip(it, style, Modifier.padding(start = 10.dp).then(scale))
                }
                if (stopper == below) Spacer(Modifier.width(12.dp))
                stoppedChip(below)
                Spacer(Modifier.weight(1f))
                val dotsLeft = shownState.dotsLeft(below)
                when {
                    dotsLeft != null -> DotsLeft(dotsLeft, below, style)
                    // An open-ended game has no limit (canvas frame `game-free-open`).
                    ui.openEnded -> Text(stringResource(R.string.length_open_detail), style = NoteText.label, color = style.graphite)
                }
            }
            if (toolBar) {
                // Five boosters in the campaign, and the tools of an open-ended game, fit with less room around them
                // (draw.py, `toolbar` and `free_toolbar`).
                val tight = campaign != null || ui.openEnded
                val side = if (tight) 12.dp else 18.dp
                val gap = if (tight) 10.dp else 14.dp
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(TOOL_BAR)
                        .graphicsLayer { translationY = toolsOut() * size.height }
                        .padding(start = side, end = side, bottom = 8.dp),
                ) {
                    val tools = Modifier.fillMaxWidth().height(56.dp)
                    // At the end the tools slide down and leave, and Result takes their place (motion.md, End of game).
                    // A booster preview's note, and Stop's, covers them (canvas frames `game-undo`, `game-free-stop`).
                    when {
                        toolsGone || ui.aim != null || ui.askingStop -> Unit
                        campaign != null -> Row(tools, horizontalArrangement = Arrangement.spacedBy(gap)) {
                            CompositionLocalProvider(LocalToolMotion provides toolMotion) { campaign.boosters?.invoke(this) }
                        }
                        ui.openEnded && state.stopper != null -> AfterStop(ui, human, names, style, stopping, gap, onUndo, onDone, onStop, tools)
                        ui.openEnded -> OpenEndedTools(ui, enabled = true, style, gap, onUndo, onStop, tools)
                        else -> Row(tools, horizontalArrangement = Arrangement.spacedBy(gap)) {
                            FreeTool(stringResource(R.string.game_undo), Doodles.UNDO, ui.canUndo && !ui.thinking, style, onUndo, Modifier.weight(1f))
                            repeat(3) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
        if (guide == null) {
            // The note ticks its boxes when the capture's points land, then swells; at 3 dots left it swings once.
            // As the level opening shrinks into it, it shows through.
            val content = if (pointsIn) campaign?.mission else campaign?.missionBefore ?: campaign?.mission
            content?.let {
                val swing = Modifier.graphicsLayer {
                    val size = if (capture != null) capture.noteScale(moment.now) else 1f
                    scaleX = size
                    scaleY = size
                    rotationZ = RunningOut.nudge(nudge.now)
                    alpha = campaign?.missionShown?.invoke() ?: 1f
                }
                MissionNote(style, Modifier.align(Alignment.TopEnd).then(swing), if (compact) 120.dp else 136.dp, it)
            }
        }
        if (guide != null) {
            // Its bottom 4 dp above the board; with no room there, over the board's top rows.
            val place = if (guideRoom > 0.dp) {
                Modifier.align(Alignment.BottomCenter).padding(bottom = bottom + boardHeight + 4.dp)
            } else {
                Modifier.align(Alignment.TopCenter).padding(top = TOP_ROW + spare + 6.dp)
            }
            TutorialNote(guide, input.method, style, onSkipGuide, place.padding(horizontal = 14.dp))
        }
        turnBanner?.let { TurnBanner(it, collects = state.isCleanup, style, Modifier.offset(y = maxHeight * 0.38f)) }
        CaptureOverlay(landed, moment, state.width, state.height, spots, style)
        GivenBackOverlay(used, boosting, state.width, state.height, spots)
    }
}

/** The top row, under the status bar. */
private val TOP_ROW = 56.dp

/** The player's line under the board. */
private val PLAYER_LINE = 56.dp

/** The tool bar: 56 dp buttons and a little paper under them. */
private val TOOL_BAR = 64.dp

/** The room the tutorial's note takes above the board: a label and two lines of handwriting. */
private val GUIDE_ROOM = 96.dp

/** The opponent's line above the board, when it fits there. */
private val OPPONENT_LINE = 52.dp

/** The boss's counter above the opponent ([BossCounter]). */
private val COUNTER = 24.dp

/** The paper between the boss's counter and the opponent's line under it. */
private val COUNTER_GAP = 4.dp

/** The full photo: 124 dp with a 108 dp face. */
private val FULL_PHOTO = 124.dp

/** The smallest photo above the opponent's line: 72 dp with a 56 dp face. */
private val SMALLEST_PHOTO = 72.dp

/** The room the photo above the line needs besides the photo itself: 8 dp of tape above it, 8 dp of paper under it, and the line. */
private val AROUND_PHOTO = 8.dp + 8.dp + OPPONENT_LINE

/** The photo beside the opponent's line: 62 dp with a 56 dp face. */
private val SMALL_PHOTO = 62.dp

/** The row of the photo beside the line: 6 dp of tape above the photo, 8 dp of paper under it. */
private val BESIDE_ROOM = 6.dp + SMALL_PHOTO + 8.dp

/**
 * How the opponent sits in the [room] between the top row and the board (game-screens.md, "When the
 * photo doesn't fit"; the canvas frames `fit-*`). The steps, from the most room to the least: the
 * full photo above the opponent's line; a smaller photo, down to 72 dp; a 62 dp photo beside the
 * line; the line in the top row. A boss's [counter] sits right above the line and takes its 28 dp
 * before the photo above it; beside the photo it costs nothing.
 */
internal sealed interface OpponentFit {
    /** The photo, [photo] dp across, above the opponent's line, with the speech bubble beside it. */
    data class Above(val photo: Dp) : OpponentFit {
        /** The full photo, which alone keeps the world's board touches beside it. */
        val full: Boolean get() = photo >= FULL_PHOTO
    }

    /** The small photo at the left, the opponent's line beside it, and no speech bubble. */
    data object Beside : OpponentFit

    /** No photo: the opponent's line in the top row. */
    data object TopRow : OpponentFit
}

internal fun opponentFit(room: Dp, counter: Boolean): OpponentFit {
    val left = room - if (counter) COUNTER + COUNTER_GAP else 0.dp
    return when {
        left >= AROUND_PHOTO + FULL_PHOTO -> OpponentFit.Above(FULL_PHOTO)
        left >= AROUND_PHOTO + SMALLEST_PHOTO -> OpponentFit.Above(floor((left - AROUND_PHOTO).value).dp)
        room >= BESIDE_ROOM -> OpponentFit.Beside
        else -> OpponentFit.TopRow
    }
}

/**
 * The largest whole-dp spacing at which a [columns] x [rows] board fits [width] and [height]: 24 dp
 * on a 360 x 800 dp phone, 21 dp on 320 x 568 (docs/design/flows.md, Wireframes, the layout rules).
 */
internal fun boardSpacing(width: Dp, height: Dp, columns: Int, rows: Int): Dp =
    floor(min(width.value / columns, height.value / rows)).coerceAtLeast(1f).dp

private fun isToMove(ui: GameUiState, player: Player): Boolean = !ui.isOver && ui.state.toMove == player

/** The dot a mission is about, circled on the board: the one to rescue or to capture. */
private fun missionDot(mission: Mission): Pos? = when (mission) {
    is Mission.Rescue -> mission.dot
    is Mission.Target -> mission.dot
    else -> null
}

/**
 * Between two people, the side whose turn has just begun, for as long as the banner shows
 * ([BANNER_MILLIS]); null otherwise. The first turn of a game has no banner.
 */
@Composable
private fun turnBanner(ui: GameUiState): Player? {
    if (ui.vsComputer) return null
    val toMove = ui.state.toMove
    var lastSeen by rememberSaveable { mutableStateOf(toMove) }
    var banner by remember { mutableStateOf<Player?>(null) }
    LaunchedEffect(toMove, ui.state.isOver) {
        if (ui.state.isOver) {
            banner = null
        } else if (toMove != lastSeen) {
            lastSeen = toMove
            banner = toMove
            delay(BANNER_MILLIS)
            banner = null
        }
    }
    return banner
}

/** How long the turn banner shows. */
internal const val BANNER_MILLIS = 1_200L

/** What the boosters add to the board: the open preview's marks and a Hint's point. */
private fun boardMarks(ui: GameUiState): BoardMarks {
    val aim = ui.aim ?: return if (ui.hint == null) BoardMarks.NONE else BoardMarks(hint = ui.hint)
    return BoardMarks(
        targets = aim.targets,
        point = aim.point,
        wall = aim.point.takeIf { aim.booster == Booster.WALL },
        plus = aim.plus,
        gone = aim.gone,
        hint = ui.hint,
    )
}

/** After a Double dot, beside the player's name: "×2", then "×1" (motion.md, Boosters: Double dot). */
@Composable
private fun DoubleDotChip(left: Int, style: Palette, modifier: Modifier) {
    val description = pluralStringResource(R.plurals.double_dot_left, left, left)
    Text(
        "×$left",
        style = NoteText.label,
        color = style.red,
        modifier = modifier
            .border(1.5.dp, style.red, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp)
            .clearAndSetSemantics {
                contentDescription = description
                liveRegion = LiveRegionMode.Polite
            },
    )
}

/**
 * The boss's counter above the opponent on a boss level with a double move (the wireframes' "Boss
 * turn" and its counting states): the boss's [turns] until it places two dots, in its [ink]. TalkBack
 * reads it, and again as it changes.
 */
@Composable
private fun BossCounter(turns: Int, ink: Color, style: Palette) {
    val text = when (turns) {
        0 -> stringResource(R.string.boss_counter_now)
        1 -> stringResource(R.string.boss_counter_next)
        else -> pluralStringResource(R.plurals.boss_counter_in, turns, turns)
    }
    val description = when (turns) {
        0 -> stringResource(R.string.boss_counter_now_description)
        1 -> stringResource(R.string.boss_counter_next_description)
        else -> pluralStringResource(R.plurals.boss_counter_in_description, turns, turns)
    }
    val shape = RoundedCornerShape(COUNTER / 2)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(COUNTER)
            .background(style.card, shape)
            .border(2.dp, ink, shape)
            .padding(horizontal = 12.dp)
            .clearAndSetSemantics {
                contentDescription = description
                liveRegion = LiveRegionMode.Polite
            },
    ) {
        Text(text, style = HAND.copy(fontSize = 18.sp), color = ink, maxLines = 1)
    }
}

/** How the boss's dots are numbered: the [measurer] and the [text] style of the numbers. */
private class BossNumbers(val measurer: TextMeasurer, val text: TextStyle)

/**
 * The boss's [dots] of its turn of two, numbered 1 and 2 as the wireframes' "Boss turn" draws them:
 * a disc in the boss's [ink], sized to the board's [spacing], straight above each dot, or below it
 * when the point above holds a dot. Straight above, not to one side: the two dots are often
 * neighbours, and a number to one side would sit over the other dot.
 */
private fun Modifier.bossNumbers(dots: List<Pos>, state: GameState, spacing: Dp, ink: Color, numbers: BossNumbers): Modifier =
    if (dots.isEmpty()) {
        this
    } else {
        drawWithContent {
            drawContent()
            val s = spacing.toPx()
            val style = numbers.text.copy(fontSize = (0.46f * s).toSp())
            dots.forEachIndexed { i, dot ->
                val above = Pos(dot.x, dot.y - 1)
                val below = state.inBounds(above.x, above.y) && state.ownerAt(above) != null
                val disc = Offset((dot.x + 0.5f) * s, (dot.y + 0.5f + if (below) 0.72f else -0.72f) * s)
                drawCircle(ink, 0.375f * s, disc)
                val layout = numbers.measurer.measure((i + 1).toString(), style)
                drawText(layout, topLeft = disc - Offset(layout.size.width / 2f, layout.size.height / 2f))
            }
        }
    }

/**
 * The top row: the pause icon and the [content], the game's name or, on a small phone, the
 * opponent's line. At the end of a game the icon slides up off the screen as [leaving] goes to 1.
 */
@Composable
private fun TopRow(showPause: Boolean, style: Palette, onPause: () -> Unit, leaving: () -> Float = { 0f }, content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxWidth().height(TOP_ROW)) {
        if (showPause) {
            val label = stringResource(R.string.pause)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .graphicsLayer { translationY = -leaving() * TOP_ROW.toPx() }
                    .padding(start = 4.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onPause)
                    .semantics { contentDescription = label },
            ) {
                Icon(remember(style) { Doodles.icon(Doodles.PAUSE, style.pencil) }, contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(30.dp))
            }
        }
        content()
    }
}

/**
 * The opponent taped in like a photo, [size] dp across: 124 dp with a 108 dp face when there is
 * room, down to 72 dp (illustrations.md, "Where each picture goes"), and its speech bubble beside
 * it: the computer's face for its [look] and [strength], or the [boss]'s own. Between two people,
 * the photo holds Blue's piece. A smaller photo keeps its 8 dp border and its tape, scaled with it,
 * so the tape never covers the face; its right edge stays put, and so does the bubble.
 */
@Composable
private fun Photo(size: Dp, player: Player, look: OpponentLook?, strength: Strength?, boss: BossFaces?, style: Palette) {
    val scale = size / FULL_PHOTO
    // The canvas places the full photo 64 dp from the left of a 360 dp screen and the bubble at 198 dp.
    Box(Modifier.width(296.dp).height(8.dp + size)) {
        Box(Modifier.offset(x = 32.dp + FULL_PHOTO - size, y = 8.dp).rotate(-3f).testTag(OPPONENT_PHOTO_TAG)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(size)
                    .shadow(4.dp, RectangleShape, clip = false)
                    .background(style.card)
                    .padding(8.dp),
            ) {
                PhotoContent(player, look, strength, boss, style, size - 16.dp)
            }
            Tape(style, Modifier.offset(x = 36.dp * scale, y = (-9).dp).rotate(4f).size(width = 52.dp * scale, height = 16.dp))
        }
        look?.bubble?.let { SpeechBubble(bubbleText(it), style, Modifier.offset(x = 166.dp, y = 8.dp + 32.dp * scale)) }
    }
}

/**
 * The photo beside the opponent's line when the one above it doesn't fit: 62 dp with a 56 dp face,
 * a 3 dp border and thinner tape (game-screens.md, "When the photo doesn't fit"). No speech bubble.
 */
@Composable
private fun SmallPhoto(player: Player, look: OpponentLook?, strength: Strength?, boss: BossFaces?, style: Palette, modifier: Modifier) {
    Box(modifier.rotate(-3f).testTag(OPPONENT_PHOTO_TAG)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(SMALL_PHOTO)
                .shadow(3.dp, RectangleShape, clip = false)
                .background(style.card)
                .padding(3.dp),
        ) {
            PhotoContent(player, look, strength, boss, style, SMALL_PHOTO - 6.dp)
        }
        Tape(style, Modifier.offset(x = 15.dp, y = (-6).dp).rotate(4f).size(width = 32.dp, height = 9.dp))
    }
}

/** What a photo holds, [face] dp across: the computer's face, or between two people Blue's piece at about half that. */
@Composable
private fun PhotoContent(player: Player, look: OpponentLook?, strength: Strength?, boss: BossFaces?, style: Palette, face: Dp) {
    if (look != null) {
        Face(Faces.of(look.mood, strength, boss), face)
    } else {
        Piece(player, style, Modifier.size(face * 56f / 108f))
    }
}

/** The opponent's photo above the board, in each of its sizes; tests find it by this tag. */
const val OPPONENT_PHOTO_TAG = "opponent photo"

@Composable
private fun bubbleText(bubble: Bubble): String = stringResource(
    when (bubble) {
        Bubble.GOOD_LUCK -> R.string.bubble_good_luck
        Bubble.YOUR_MOVE -> R.string.bubble_your_move
        Bubble.HEY -> R.string.bubble_hey
        Bubble.OUCH -> R.string.bubble_ouch
        Bubble.WELL_PLAYED -> R.string.bubble_well_played
        Bubble.AGAIN -> R.string.bubble_again
        Bubble.TAKE_IT_BACK -> R.string.bubble_take_it_back
        Bubble.STOPPED -> R.string.bubble_stopped
    },
)

/** Short handwritten words: the theme's display style at 24 sp. */
internal val HAND: TextStyle
    @Composable @ReadOnlyComposable
    get() = NoteText.display.copy(fontSize = 24.sp, lineHeight = TextUnit.Unspecified)

@Composable
private fun SpeechBubble(text: String, style: Palette, modifier: Modifier) {
    val shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 3.dp)
    Text(
        text,
        style = HAND,
        color = style.blue,
        maxLines = 1,
        modifier = modifier
            .background(style.card, shape)
            .border(1.5.dp, style.blue, shape)
            .padding(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 4.dp),
    )
}

/** A strip of tape on a note or a photo. */
@Composable
internal fun Tape(style: Palette, modifier: Modifier) {
    Box(modifier.background(style.tape))
}

/**
 * A side's line: its piece, its name in its ink, its tally and its score. A pen stroke under the
 * name marks the side to move. TalkBack reads it as one sentence and announces changes. A [compact]
 * line, for the top row of a small phone, is smaller and has no tally.
 */
@Composable
private fun SideLine(
    player: Player,
    name: String,
    score: Int,
    toMove: Boolean,
    style: Palette,
    compact: Boolean = false,
    swell: () -> Float = { 1f },
    onTally: (LayoutCoordinates) -> Unit = {},
) {
    val ink = style.ink(player)
    // The pen stroke is drawn left to right under the side whose turn begins (motion.md, "Placing a dot").
    val stroke = rememberChangeTimeline(toMove, Placing.PEN_STROKE) { old, new -> !old && new }
    val description = stringResource(if (toMove) R.string.side_to_move else R.string.side_waiting, name, score)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 8.dp),
        modifier = Modifier
            .height(40.dp)
            .clearAndSetSemantics {
                contentDescription = description
                liveRegion = LiveRegionMode.Polite
            },
    ) {
        // A 12 dp dot, as on the board at 24 dp spacing; on a small phone 6/7 of that.
        Piece(player, style, Modifier.size(pieceBox(if (compact) 72f / 7 else 12f)))
        Text(
            name,
            style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = ink,
            maxLines = 1,
            modifier = if (toMove) Modifier.penStroke(ink) { phase(stroke.now, 0f, Placing.PEN_STROKE, Ease.OUT) } else Modifier,
        )
        if (!compact) Tally(score, ink, Modifier.onGloballyPositioned(onTally))
        Text(
            score.toString(),
            style = if (compact) NoteText.score.copy(fontSize = 18.sp) else NoteText.score,
            color = ink,
            modifier = Modifier
                .then(if (compact) Modifier.onGloballyPositioned(onTally) else Modifier)
                .graphicsLayer {
                    val size = swell()
                    scaleX = size
                    scaleY = size
                },
        )
    }
}


/**
 * A side's piece in the theme's shape (pieces.md, "Beyond the board"). Both sides are drawn at one
 * dot size, at which the wider of the two fills the box: in Notebook the dot size is 6/7 of the
 * box, so a diamond of 7/6 of the dot size fills it. Each piece is centred by its own outline, so
 * a comet's head sits low and to the right, its tail reaching up and to the left.
 */
@Composable
internal fun Piece(player: Player, style: Palette, modifier: Modifier) {
    val ink = style.ink(player)
    val look = BoardStyle.current.look
    val piece = if (player == Player.RED) look.red else look.blue
    Canvas(modifier) {
        val dot = size.minDimension / look.pieceReach()
        drawPiece(piece, center - piece.bounds(dot).center, dot, ink)
    }
}

/** How wide a box holds both sides' pieces, in dot sizes: 7/6 in Notebook, for its diamond. */
private fun BoardLook.pieceReach(): Float = maxOf(red.bounds(1f).maxDimension, blue.bounds(1f).maxDimension)

/** The box for a side's piece drawn at a dot size of [dot] dp (the score row's 12 dp). */
@Composable
@ReadOnlyComposable
private fun pieceBox(dot: Float): Dp = (dot * BoardStyle.current.look.pieceReach()).dp

/** The canvas's pen stroke under a name: a slightly wavy line, 4 dp under the text, drawn as far as [drawn] says. */
private fun Modifier.penStroke(ink: Color, drawn: () -> Float = { 1f }): Modifier = drawWithContent {
    drawContent()
    val w = size.width + 10.dp.toPx()
    val left = -4.dp.toPx()
    val top = size.height - 1.dp.toPx()
    val unit = 1.dp.toPx()
    val path = Path().apply {
        moveTo(left + 2 * unit, top + 5 * unit)
        quadraticTo(left + w * 0.4f, top + unit, left + w * 0.7f, top + 4 * unit)
        quadraticTo(left + w, top + 7 * unit, left + w - 2 * unit, top + 3 * unit)
    }
    val part = drawn()
    if (part <= 0f) return@drawWithContent
    val shown = if (part >= 1f) {
        path
    } else {
        val measure = PathMeasure()
        measure.setPath(path, false)
        Path().also { measure.getSegment(0f, measure.length * part, it, true) }
    }
    drawPath(shown, ink, style = Stroke(width = 2.2f * unit, cap = StrokeCap.Round))
}

/** The most marks a tally draws; the score beside it gives the rest. Keeps the line on a 320 dp phone. */
private const val TALLY_MARKS = 15

/** A score as ink tally marks, in fives (docs/design/canvas/draw.py, `tally`). */
@Composable
private fun Tally(score: Int, ink: Color, modifier: Modifier = Modifier) {
    val marks = score.coerceIn(0, TALLY_MARKS)
    val groups = ceil(marks / 5f).toInt().coerceAtLeast(1)
    Canvas(modifier.size(width = 24.dp * groups, height = 18.dp)) {
        val u = 1.dp.toPx()
        val path = Path()
        for (i in 0 until marks) {
            val group = i / 5
            val k = i % 5
            val x0 = group * 24f
            if (k < 4) {
                val x = x0 + 2 + k * 4.5f
                val y = 2 + (k % 2) * 0.6f
                val bend = if (k % 2 == 0) 0.6f else -0.6f
                path.moveTo(x * u, y * u)
                path.quadraticTo((x + bend) * u, (y + 7) * u, x * u, (y + 14 - k % 2) * u)
            } else {
                path.moveTo((x0 - 1) * u, 14 * u)
                path.quadraticTo((x0 + 9) * u, 9 * u, (x0 + 19) * u, 3 * u)
            }
        }
        drawPath(path, ink, style = Stroke(width = 1.8f * u, cap = StrokeCap.Round))
    }
}

/** The hand-cut shape of a tool in the tool bar (the canvas's `toolbar`). */
internal val TOOL_SHAPE = RoundedCornerShape(topStart = 14.dp, topEnd = 18.dp, bottomEnd = 12.dp, bottomStart = 20.dp)

/**
 * An open-ended game's tool bar (canvas frame `game-free-open`; draw.py, `free_toolbar`): Stop at
 * the left, and in free play Undo at the right, where the campaign keeps its Undo, in a row of five
 * places. Stop opens Stop's note ([onStop]); it never stops on one tap.
 */
@Composable
private fun OpenEndedTools(ui: GameUiState, enabled: Boolean, style: Palette, gap: Dp, onUndo: () -> Unit, onStop: () -> Unit, modifier: Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(gap)) {
        FreeTool(stringResource(R.string.game_stop), Doodles.FLAG, enabled && ui.canStop && !ui.thinking, style, onStop, Modifier.weight(1f))
        repeat(3) { Spacer(Modifier.weight(1f)) }
        if (ui.vsComputer) FreeTool(stringResource(R.string.game_undo), Doodles.UNDO, enabled && ui.canUndo && !ui.thinking, style, onUndo, Modifier.weight(1f)) else Spacer(Modifier.weight(1f))
    }
}

/**
 * The tool bar's place after a stop (motion.md, "Stop and Done"): the collector's row, which slides
 * up as the [stopping] clock plays; when the computer stopped, the player's tool bar first slides
 * down and away. When the player stopped against the computer, only the line "The computer captures
 * what is left." comes up: the computer collects by itself.
 */
@Composable
private fun AfterStop(
    ui: GameUiState,
    human: Player,
    names: Names,
    style: Palette,
    stopping: Timeline,
    gap: Dp,
    onUndo: () -> Unit,
    onDone: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier,
) {
    val stop = ui.stopMoment
    val computerStopped = ui.vsComputer && stop != null && stop.by != human
    val barLeaving by remember(stopping, computerStopped) { derivedStateOf { computerStopped && stopping.now < StopTimes.TOOLS_OUT } }
    if (barLeaving) {
        OpenEndedTools(ui, enabled = false, style, gap, onUndo, onStop, modifier.graphicsLayer { translationY = StopTimes.toolsOut(stopping.now) * size.height })
    }
    val comingUp = modifier.graphicsLayer { translationY = (1f - if (stop != null) StopTimes.row(stopping.now) else 1f) * size.height }
    val collector = ui.state.stopper?.opponent ?: return
    if (ui.vsComputer && collector != human) {
        Box(comingUp, contentAlignment = Alignment.CenterStart) {
            Text(stringResource(R.string.game_computer_collects), style = NoteText.label, color = style.pencil)
        }
    } else {
        val line = if (ui.vsComputer) stringResource(R.string.game_collect) else stringResource(R.string.game_collect_named, names.of(collector))
        CollectRow(line, onUndo.takeIf { ui.vsComputer }, undoEnabled = ui.canUndo && !ui.thinking, style, onDone, comingUp)
    }
}

/** A 2 dp outline in [ink], inside the edge, dashed for a tool that cannot be used now. */
internal fun Modifier.toolOutline(ink: Color, dashed: Boolean, shape: Shape): Modifier = drawBehind {
    val width = 2.dp.toPx()
    val effect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())) else null
    inset(width / 2) {
        drawOutline(shape.createOutline(size, layoutDirection, this), ink, style = Stroke(width = width, pathEffect = effect))
    }
}

/** The campaign's mission note, taped in the top right corner; [MissionNoteContent] fills it. */
@Composable
private fun MissionNote(style: Palette, modifier: Modifier, width: Dp, content: @Composable () -> Unit) {
    Box(modifier.padding(top = 6.dp, end = 12.dp).rotate(2f)) {
        Box(
            Modifier
                .width(width)
                .shadow(4.dp, RectangleShape, clip = false)
                .background(style.card)
                .padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 9.dp),
        ) { content() }
        Tape(style, Modifier.offset(x = 44.dp, y = (-8).dp).rotate(-4f).size(width = 44.dp, height = 16.dp))
    }
}

/** Between two people, across the screen at each change of turn: the side to play now, or after a stop the side that collects. */
@Composable
private fun TurnBanner(player: Player, collects: Boolean, style: Palette, modifier: Modifier) {
    val ink = style.ink(player)
    val text = stringResource(if (collects) R.string.turn_banner_collects else R.string.turn_banner, playerName(player))
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(style.card.copy(alpha = 0.94f))
            .drawBehind {
                val line = 2.dp.toPx()
                drawRect(ink, size = size.copy(height = line))
                drawRect(ink, topLeft = Offset(0f, size.height - line), size = size.copy(height = line))
            }
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Piece(player, style, Modifier.size(22.dp))
        Text(text, style = HAND.copy(fontSize = 34.sp), color = ink)
    }
}
