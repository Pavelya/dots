package io.github.pavelya.dots.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dots.engine.Player
import dots.engine.Pos
import io.github.pavelya.dots.R
import io.github.pavelya.dots.game.Bubble
import io.github.pavelya.dots.game.GameUiState
import io.github.pavelya.dots.game.OpponentLook
import io.github.pavelya.dots.ui.board.BoardStyle
import io.github.pavelya.dots.ui.board.BoardView
import kotlinx.coroutines.delay
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min

/**
 * The game board as the canvas game frames lay it out (docs/design/canvas, `game-start`,
 * `game-threat`, `game-big-capture`; the wireframes' "Game board: the parts" and "Two players"):
 *
 * - **Top row:** the pause icon (left), the game's name, and in the campaign the mission note (right).
 * - **The opponent above the board:** against the computer, its face taped in like a photo, with a
 *   speech bubble; between two people, Blue's piece in the photo. Under it the opponent's piece,
 *   name, tally and score.
 * - **The board,** edge to edge, at the largest whole-dp spacing that fits ([boardSpacing]).
 * - **The player below the board:** piece, name, tally and score, and the dots left.
 * - **The tool bar:** the campaign's boosters (A-05), or Undo in free play (decision 0023). Two
 *   players have none.
 *
 * A pen stroke under a name marks the side to move. Between two people, a banner names the side to
 * play at each change of turn. Spare height goes to the opponent; when there is too little for the
 * photo, only the opponent's line stays, and on a small phone that line moves into the top row in
 * place of the game's name, as the wireframes' small frames show.
 *
 * The inks are the plain POC ones ([GameStyle.plain]); the Notebook theme (V-02, V-03) brings the
 * paper, fonts and hand-drawn icons, V-12 the faces.
 */
@Composable
internal fun GameBoard(
    ui: GameUiState,
    human: Player,
    campaign: CampaignParts?,
    onTap: (Pos?) -> Unit,
    onUndo: () -> Unit,
    onPause: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < 0.5f
    val style = remember(colors) { GameStyle.plain(colors, dark) }
    val boardStyle = remember(colors) { BoardStyle.plain(colors, dark) }
    val state = ui.state
    val names = Names(ui.vsComputer, human)
    val below = if (ui.vsComputer) human else Player.RED
    val above = below.opponent
    val toolBar = campaign != null || ui.vsComputer
    val turnBanner = turnBanner(ui)
    BoxWithConstraints(modifier.fillMaxSize().background(style.paper)) {
        val bottom = PLAYER_LINE + if (toolBar) TOOL_BAR else 0.dp
        val spacing = boardSpacing(maxWidth, maxHeight - TOP_ROW - bottom, state.width, state.height)
        val boardHeight = spacing * state.height
        val spare = maxHeight - TOP_ROW - bottom - boardHeight
        // On a small phone the opponent's line moves into the top row, and the mission note narrows to make room.
        val compact = spare < OPPONENT_LINE
        val opponentLine = @Composable { inTopRow: Boolean ->
            SideLine(above, names.of(above), state.score(above), toMove = isToMove(ui, above), style, inTopRow)
        }
        Column(Modifier.fillMaxSize()) {
            val title = campaign?.title ?: stringResource(if (ui.vsComputer) R.string.play_free else R.string.play_two)
            TopRow(showPause = !state.isOver, style = style, onPause = onPause) {
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
                if (spare >= OPPONENT_WITH_PHOTO) {
                    Photo(above, ui.opponent, style)
                    Spacer(Modifier.height(8.dp))
                }
                if (spare >= OPPONENT_LINE) {
                    opponentLine(false)
                    Spacer(Modifier.height(12.dp))
                }
            }
            BoardView(
                state = state,
                style = boardStyle,
                description = stringResource(R.string.board_description, state.width, state.height, status(ui, names)),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(spacing * state.width, boardHeight)
                    .testTag(BOARD_TAG),
                lastMove = ui.lastMove,
                preview = ui.preview,
                dangers = ui.dangers,
                onTap = onTap,
            )
            Row(
                modifier = Modifier.fillMaxWidth().height(PLAYER_LINE).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SideLine(below, names.of(below), state.score(below), toMove = isToMove(ui, below), style)
                Spacer(Modifier.weight(1f))
                state.dotsLeft(below)?.let { DotsLeft(it, style) }
            }
            if (toolBar) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(TOOL_BAR).padding(start = 18.dp, end = 18.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    if (campaign != null) {
                        campaign.boosters(this)
                    } else {
                        UndoTool(enabled = ui.canUndo && !ui.thinking, style = style, onClick = onUndo, modifier = Modifier.weight(1f))
                        repeat(3) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        campaign?.let { MissionNote(style, Modifier.align(Alignment.TopEnd), if (compact) 120.dp else 136.dp, it.mission) }
        turnBanner?.let { TurnBanner(it, style, Modifier.offset(y = maxHeight * 0.38f)) }
    }
}

/** The top row, under the status bar. */
private val TOP_ROW = 56.dp

/** The player's line under the board. */
private val PLAYER_LINE = 56.dp

/** The tool bar: 56 dp buttons and a little paper under them. */
private val TOOL_BAR = 64.dp

/** The opponent's line above the board, when it fits there. */
private val OPPONENT_LINE = 52.dp

/** The height the photo takes, with its tape. */
private val PHOTO_HEIGHT = 132.dp

/** The opponent's line with the photo above it: below this much spare height, the photo goes. */
private val OPPONENT_WITH_PHOTO = OPPONENT_LINE + PHOTO_HEIGHT + 8.dp

/**
 * The largest whole-dp spacing at which a [columns] x [rows] board fits [width] and [height]: 24 dp
 * on a 360 x 800 dp phone, 21 dp on 320 x 568 (docs/design/flows.md, Wireframes, the layout rules).
 */
internal fun boardSpacing(width: Dp, height: Dp, columns: Int, rows: Int): Dp =
    floor(min(width.value / columns, height.value / rows)).coerceAtLeast(1f).dp

private fun isToMove(ui: GameUiState, player: Player): Boolean = !ui.state.isOver && ui.state.toMove == player

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

/** The top row: the pause icon and the [content], the game's name or, on a small phone, the opponent's line. */
@Composable
private fun TopRow(showPause: Boolean, style: GameStyle, onPause: () -> Unit, content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxWidth().height(TOP_ROW)) {
        if (showPause) {
            val label = stringResource(R.string.pause)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.CenterStart)
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
 * The opponent taped in like a photo, 124 dp with a 108 dp face (illustrations.md, "Where each
 * picture goes"), and its speech bubble beside it. Between two people, the photo holds Blue's piece.
 */
@Composable
private fun Photo(player: Player, look: OpponentLook?, style: GameStyle) {
    // The canvas places the photo 64 dp from the left of a 360 dp screen and the bubble at 198 dp.
    Box(Modifier.width(296.dp).height(PHOTO_HEIGHT)) {
        Box(Modifier.offset(x = 32.dp, y = 8.dp).rotate(-3f)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(124.dp)
                    .shadow(4.dp, RectangleShape, clip = false)
                    .background(style.card)
                    .padding(8.dp),
            ) {
                if (look != null) {
                    Image(
                        remember(look.mood, style) { Doodles.face(look.mood, style.blue, style.bluePale, style.paper) },
                        contentDescription = null,
                        modifier = Modifier.size(108.dp),
                    )
                } else {
                    Piece(player, style, Modifier.size(56.dp))
                }
            }
            Tape(style, Modifier.offset(x = 36.dp, y = (-9).dp).rotate(4f).size(width = 52.dp, height = 16.dp))
        }
        look?.bubble?.let { SpeechBubble(bubbleText(it), style, Modifier.offset(x = 166.dp, y = 40.dp)) }
    }
}

@Composable
private fun bubbleText(bubble: Bubble): String = stringResource(
    when (bubble) {
        Bubble.GOOD_LUCK -> R.string.bubble_good_luck
        Bubble.YOUR_MOVE -> R.string.bubble_your_move
        Bubble.HEY -> R.string.bubble_hey
        Bubble.OUCH -> R.string.bubble_ouch
        Bubble.WELL_PLAYED -> R.string.bubble_well_played
        Bubble.AGAIN -> R.string.bubble_again
    },
)

/** Handwriting, until the Notebook theme brings Caveat (V-03). */
private val HAND = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)

@Composable
private fun SpeechBubble(text: String, style: GameStyle, modifier: Modifier) {
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
internal fun Tape(style: GameStyle, modifier: Modifier) {
    Box(modifier.background(style.tape))
}

/**
 * A side's line: its piece, its name in its ink, its tally and its score. A pen stroke under the
 * name marks the side to move. TalkBack reads it as one sentence and announces changes. A [compact]
 * line, for the top row of a small phone, is smaller and has no tally.
 */
@Composable
private fun SideLine(player: Player, name: String, score: Int, toMove: Boolean, style: GameStyle, compact: Boolean = false) {
    val ink = style.ink(player)
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
        Piece(player, style, Modifier.size(if (compact) 12.dp else 14.dp))
        Text(
            name,
            style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = ink,
            maxLines = 1,
            modifier = if (toMove) Modifier.penStroke(ink) else Modifier,
        )
        if (!compact) Tally(score, ink)
        Text(score.toString(), style = if (compact) SCORE.copy(fontSize = 18.sp) else SCORE, color = ink)
    }
}

/** Big numbers: the scores and the dots left. */
private val SCORE = TextStyle(fontSize = 24.sp, lineHeight = 28.sp, fontWeight = FontWeight.ExtraBold, fontFeatureSettings = "tnum")

/** A side's piece (pieces.md): a round dot for Red, a diamond for Blue. */
@Composable
internal fun Piece(player: Player, style: GameStyle, modifier: Modifier) {
    val ink = style.ink(player)
    Canvas(modifier) {
        if (player == Player.RED) {
            drawCircle(ink, radius = size.minDimension * 6f / 14f)
        } else {
            val path = Path().apply {
                moveTo(size.width / 2, 0f)
                lineTo(size.width, size.height / 2)
                lineTo(size.width / 2, size.height)
                lineTo(0f, size.height / 2)
                close()
            }
            drawPath(path, ink)
        }
    }
}

/** The canvas's pen stroke under a name: a slightly wavy line, 4 dp under the text. */
private fun Modifier.penStroke(ink: Color): Modifier = drawWithContent {
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
    drawPath(path, ink, style = Stroke(width = 2.2f * unit, cap = StrokeCap.Round))
}

/** The most marks a tally draws; the score beside it gives the rest. Keeps the line on a 320 dp phone. */
private const val TALLY_MARKS = 15

/** A score as ink tally marks, in fives (docs/design/canvas/draw.py, `tally`). */
@Composable
private fun Tally(score: Int, ink: Color) {
    val marks = score.coerceIn(0, TALLY_MARKS)
    val groups = ceil(marks / 5f).toInt().coerceAtLeast(1)
    Canvas(Modifier.size(width = 24.dp * groups, height = 18.dp)) {
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

/** The dots the player below the board has left: a big number and a label. */
@Composable
private fun DotsLeft(left: Int, style: GameStyle) {
    val description = pluralStringResource(R.plurals.game_dots_left, left, left)
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
    ) {
        Text(left.toString(), style = SCORE, color = style.pencil, modifier = Modifier.alignByBaseline())
        Text(
            pluralStringResource(R.plurals.game_dots_left_label, left),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = style.graphite,
            modifier = Modifier.alignByBaseline(),
        )
    }
}

/** The hand-cut shape of a tool in the tool bar (the canvas's `toolbar`). */
internal val TOOL_SHAPE = RoundedCornerShape(topStart = 14.dp, topEnd = 18.dp, bottomEnd = 12.dp, bottomStart = 20.dp)

/**
 * Free play's Undo: a tool with no count (decision 0023). When it cannot be used, its outline is
 * dashed and its ink grey, as an empty booster on the canvas.
 */
@Composable
private fun UndoTool(enabled: Boolean, style: GameStyle, onClick: () -> Unit, modifier: Modifier) {
    val ink = if (enabled) style.blue else style.graphite
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .height(56.dp)
            .clip(TOOL_SHAPE)
            .background(style.card)
            .toolOutline(if (enabled) style.bluePale else style.graphite, dashed = !enabled, TOOL_SHAPE)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
    ) {
        Icon(remember(ink) { Doodles.icon(Doodles.UNDO, ink) }, contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(24.dp))
        Text(stringResource(R.string.game_undo), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = ink)
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

/** The campaign's mission note, taped in the top right corner. A-03 fills it. */
@Composable
private fun MissionNote(style: GameStyle, modifier: Modifier, width: Dp, content: @Composable () -> Unit) {
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

/** Between two people, across the screen at each change of turn: the side to play now. */
@Composable
private fun TurnBanner(player: Player, style: GameStyle, modifier: Modifier) {
    val ink = style.ink(player)
    val text = stringResource(R.string.turn_banner, playerName(player))
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
