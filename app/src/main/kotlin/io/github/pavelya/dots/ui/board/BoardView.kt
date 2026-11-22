package io.github.pavelya.dots.ui.board

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dots.ai.Danger
import dots.engine.Area
import dots.engine.Booster
import dots.engine.Capture
import dots.engine.GameState
import dots.engine.Player
import dots.engine.Pos
import dots.engine.areas
import io.github.pavelya.dots.game.BoardMoment
import io.github.pavelya.dots.game.BoosterMoment
import io.github.pavelya.dots.game.CapturedArea
import io.github.pavelya.dots.game.ThreatMark
import io.github.pavelya.dots.game.UndoneTurn
import io.github.pavelya.dots.ui.motion.Announce
import io.github.pavelya.dots.ui.motion.BombEffect
import io.github.pavelya.dots.ui.motion.Capturing
import io.github.pavelya.dots.ui.motion.DoubleDotEffect
import io.github.pavelya.dots.ui.motion.Ease
import io.github.pavelya.dots.ui.motion.EndOfGame
import io.github.pavelya.dots.ui.motion.HintEffect
import io.github.pavelya.dots.ui.motion.UndoEffect
import io.github.pavelya.dots.ui.motion.WallEffect
import io.github.pavelya.dots.ui.motion.length
import io.github.pavelya.dots.ui.screen.Doodles
import io.github.pavelya.dots.ui.motion.Placing
import io.github.pavelya.dots.ui.motion.RingEffects
import io.github.pavelya.dots.ui.motion.NeonLight
import io.github.pavelya.dots.ui.motion.Threat
import io.github.pavelya.dots.ui.motion.Twinkle
import io.github.pavelya.dots.ui.theme.BoardLook
import io.github.pavelya.dots.ui.theme.CaptureMotion
import io.github.pavelya.dots.ui.theme.PieceShape
import io.github.pavelya.dots.ui.theme.RingEffect
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Draws a position: grid, captured areas (washed and hatched in the captor's colours, inside a ring
 * drawn like a pen stroke), live and captured pieces (a cut across each captured one), the last
 * move's pinhole, and a placement preview. With high-contrast pieces ([BoardLook.halo]) each piece
 * sits in a band of the paper or wash under it. It
 * decides nothing about the game; [PlayableBoard] adds touch. [showLinks] draws the links between
 * neighbouring dots of one colour, which makes chains visible; they are off unless asked for (the
 * player's setting, the tutorial's linking level, How to play), and the board tells tests whether
 * they show ([LinksShown]). [dangers] marks groups close to
 * capture (the threat highlight). [marked] is the dot a level's mission is about (rescue or
 * target), circled twice in the player's ink. [guides] draws the preview's row and column across
 * the board, around a finger that hides the point itself (placing by drag). [pointer] is the point
 * the tutorial asks the player to tap: circled in the player's ink, with a hand pointing at it (the
 * wireframes' "Tutorial level"). Blocked points, a wall booster's among them, carry the wall's mark;
 * [marks] adds the booster preview and the Hint's circle (A-05).
 *
 * Motion (docs/design/motion.md): the [moment] of the dot that has just landed plays as [time]
 * runs from 0 ms: the dot grows and settles, its links grow, the pinhole moves, an announced dot
 * spreads two ripples, a capture draws its ring, spreads its wash and cuts its dots, and new
 * threats draw in and pulse. [edgeMarks] marks a dot's row and column on the board's top and left
 * edges. Drag's guides reach out as far as [guideReach] says. At the end of a game the captured
 * areas light up in turn ([glow]). Time is read while drawing, so only drawing follows it.
 *
 * A [booster] just used plays its effect as [boosterTime] runs from 0 ms (motion.md, "Boosters"):
 * the wall is drawn and hatched, the hint circled twice, the bomb drops and its hits swell and
 * shrink away, and Undo rubs out the turn it took back. A dot placed after a Double dot sparks; one
 * placed after a Hint makes its circles shrink away.
 */
@Composable
fun BoardView(
    state: GameState,
    style: BoardStyle,
    description: String,
    modifier: Modifier = Modifier,
    lastMove: Pos? = null,
    preview: Pos? = null,
    showLinks: Boolean = false,
    dangers: List<Danger> = emptyList(),
    marked: Pos? = null,
    guides: Boolean = false,
    pointer: Pos? = null,
    marks: BoardMarks = BoardMarks.NONE,
    moment: BoardMoment? = null,
    time: () -> Float = { Float.MAX_VALUE },
    edgeMarks: Pos? = null,
    guideReach: () -> Float = { 1f },
    glow: AreaGlow? = null,
    booster: BoosterMoment? = null,
    boosterTime: () -> Float = { Float.MAX_VALUE },
) {
    val areas = remember(state) { state.areas() }
    val regions = remember(areas) { areas.map { it.region.toHashSet() } }
    val undone = remember(booster, state) { UndoneParts.of(booster, state, regions) }
    Canvas(
        modifier = modifier
            .aspectRatio(state.width.toFloat() / state.height)
            .semantics {
                contentDescription = description
                linksShown = showLinks
            },
    ) {
        val geometry = BoardGeometry(state.width, state.height, size.width, size.height)
        val guided = if (guides) preview ?: marks.point else null
        val bt = if (booster == null) Float.MAX_VALUE else boosterTime()
        val frame = Frame.of(moment, if (moment == null) Float.MAX_VALUE else time(), areas, regions, edgeMarks, glow, booster, bt, undone)
        drawPosition(state, areas, geometry, style, lastMove, preview, showLinks, dangers, marked, marks, guided, frame, if (guided != null) guideReach() else 1f)
        pointer?.let { drawPointer(geometry.center(it), geometry, style) }
    }
}

/**
 * The end of a game (motion.md, "End of game"): the captured areas, in the [order] they were
 * captured, light up in turn as [time] runs, at the times of [ending].
 */
class AreaGlow(val order: List<CapturedArea>, val ending: EndOfGame, val time: () -> Float)

/** One area being captured in this frame: by [capture], its ring drawn from the closing dot ([start] on the ring). */
private class AreaCapture(val capture: Capture, val start: Int)

/**
 * Where the moment stands in the frame being drawn, at [t] ms: everything [drawPosition] animates.
 * Without a moment every value is at its end, and the board is drawn as it stands.
 */
private class Frame(
    val moment: BoardMoment?,
    val t: Float,
    /** The captures of the moment's areas, by area index. */
    val capturing: Map<Int, AreaCapture>,
    val timing: Capturing?,
    /** The cut drawn across each dot the moment captured, from 0 to 1; until it starts the dot keeps its full ink. */
    val cuts: Map<Pos, Float>,
    val edgeMarks: Pos?,
    /** How much each area is lit at the end of the game, by area index. */
    val glow: Map<Int, Float>,
    /** The booster just used and the time since, in ms. */
    val booster: BoosterMoment?,
    val bt: Float,
    /** What an Undo takes back that the position no longer shows. */
    val undone: UndoneParts,
    /**
     * For each dot on a ring being drawn, how far round the ring the line reaches it, from the
     * closing dot: the closing dot itself at 1, when the line comes back to it.
     */
    val reached: Map<Pos, Float> = emptyMap(),
) {
    val newDot: Pos? get() = moment?.dot?.takeIf { t < Placing.SETTLED }

    /**
     * The size of the piece on ([x], [y]) as a share of its own: a dot just placed grows to 115% and
     * settles (motion.md, "Placing a dot"); one Undo gives back after a bomb grows back in; in Star
     * chart, a star the line has just reached twinkles (motion.md, "Capture in Star chart"). With a
     * ring effect only the closing star twinkles, as the rope pulls tight or the stamp comes down
     * (cosmetics.md).
     */
    fun pieceScale(x: Int, y: Int, look: BoardLook): Float {
        val dot = newDot
        val base = if (dot != null && dot.x == x && dot.y == y) Placing.dotScale(t) else grownBack(x, y)
        if (look.capture != CaptureMotion.CONSTELLATION || timing == null) return base
        if (look.ringEffect != RingEffect.THEME) {
            val closing = capturing.values.any { it.capture.closing.x == x && it.capture.closing.y == y }
            return if (closing) base * RingEffects.closingStar(timing, t) else base
        }
        val share = reached[Pos(x, y)] ?: return base
        return base * Twinkle.size(timing, t, share)
    }

    /** The booster whose effect is playing in this frame, or null. */
    val boosting: BoosterMoment? get() = booster?.takeIf { bt < it.length }

    /** The turn an Undo is rubbing out in this frame, or null. */
    val undoing: UndoneTurn? get() = boosting?.undone

    /** How far Undo has rubbed out what [side] played in the turn: the player's own part comes second. */
    fun rubbedOut(side: Player): Float = UndoEffect.rubOut(bt, undoFrom(side))

    fun undoFrom(side: Player): Float = if (side == booster?.by) UndoEffect.OWN else UndoEffect.ANSWER

    /** A dot a bomb took in the turn Undo takes back grows back in: its size from 0 to 1; 1 for any other dot. */
    fun grownBack(x: Int, y: Int): Float =
        if (undoing == null || undone.back.isEmpty() || Pos(x, y) !in undone.back) 1f else UndoEffect.growBack(bt)

    /** How much of the cut across [pos] is left while an Undo takes back the capture that made it; 0 for any other dot. */
    fun cutLeft(pos: Pos): Float {
        if (undoing == null) return 0f
        val captor = undone.uncut[pos] ?: return 0f
        return 1f - rubbedOut(captor)
    }

    companion object {
        fun of(
            moment: BoardMoment?,
            t: Float,
            areas: List<Area>,
            regions: List<Set<Pos>>,
            edgeMarks: Pos?,
            glow: AreaGlow?,
            booster: BoosterMoment?,
            bt: Float,
            undone: UndoneParts,
        ): Frame {
            val captures = moment?.captures.orEmpty()
            val timing = if (captures.isEmpty() || moment == null) null else Capturing.of(moment.captured)
            val capturing = HashMap<Int, AreaCapture>()
            val cuts = HashMap<Pos, Float>()
            val reached = HashMap<Pos, Float>()
            if (timing != null && t < timing.length) {
                for (capture in captures) {
                    val inside = capture.dots.firstOrNull() ?: capture.region.firstOrNull() ?: continue
                    val index = regions.indexOfFirst { inside in it }
                    if (index >= 0 && index !in capturing) {
                        val start = ringIndex(areas[index].ring, capture.closing)
                        capturing[index] = AreaCapture(capture, start)
                        reached += reachedOn(areas[index].ring, start)
                    }
                    val nearest = capture.dots.sortedBy { hypot((it.x - capture.closing.x).toFloat(), (it.y - capture.closing.y).toFloat()) }
                    nearest.forEachIndexed { i, dot -> cuts[dot] = timing.cut(t, i) }
                }
            }
            val lit = HashMap<Int, Float>()
            if (glow != null) {
                val now = glow.time()
                glow.order.forEachIndexed { i, area ->
                    val index = regions.indexOfFirst { area.first in it }
                    if (index >= 0) lit[index] = glow.ending.glow(now, i)
                }
            }
            return Frame(moment, t, capturing, timing, cuts, edgeMarks, lit, booster, bt, undone, reached)
        }

        /** How far round [ring] the line reaches each of its dots, starting from its [start]th; that one is reached again at 1. */
        private fun reachedOn(ring: List<Pos>, start: Int): Map<Pos, Float> {
            val seq = ring.drop(start) + ring.take(start) + ring[start]
            val legs = seq.zipWithNext { a, b -> hypot((b.x - a.x).toFloat(), (b.y - a.y).toFloat()) }
            val total = legs.sum()
            if (total <= 0f) return emptyMap()
            var run = 0f
            return seq.drop(1).zip(legs).associate { (dot, leg) ->
                run += leg
                dot to run / total
            }
        }

        /** Where [ring] reaches the closing dot, or the ring's dot nearest to it. */
        private fun ringIndex(ring: List<Pos>, closing: Pos): Int =
            ring.indexOf(closing).takeIf { it >= 0 } ?: ring.indices.minBy { max(abs(ring[it].x - closing.x), abs(ring[it].y - closing.y)) }
    }
}

/**
 * What an Undo takes back that the position after it no longer shows: the captured [areas] of the
 * turn, and the dots they had captured, which are free again, with their captor ([uncut]). The
 * dots a bomb took in that turn are on the board again ([back]): they grow back in.
 */
private class UndoneParts(val areas: List<Area>, val uncut: Map<Pos, Player>, val back: Set<Pos> = emptySet()) {
    companion object {
        private val NONE = UndoneParts(emptyList(), emptyMap())

        fun of(booster: BoosterMoment?, state: GameState, regions: List<Set<Pos>>): UndoneParts {
            val before = booster?.undone?.before ?: return NONE
            val areas = before.areas().filter { area -> regions.none { area.region.first() in it } }
            val uncut = HashMap<Pos, Player>()
            val back = HashSet<Pos>()
            for (y in 0 until state.height) for (x in 0 until state.width) {
                val owner = state.ownerAt(x, y) ?: continue
                if (before.isCaptured(x, y) && !state.isCaptured(x, y) && before.ownerAt(x, y) == owner) uncut[Pos(x, y)] = owner.opponent
                if (before.ownerAt(x, y) == null) back += Pos(x, y)
            }
            return UndoneParts(areas, uncut, back)
        }
    }
}

/** Whether the board draws the links between touching dots of one side; for tests, TalkBack does not read it. */
val LinksShown = SemanticsPropertyKey<Boolean>("LinksShown")
private var SemanticsPropertyReceiver.linksShown by LinksShown

/**
 * What a booster adds to the board (A-05). In the booster preview: the [targets] it may be aimed
 * at, the [point] chosen (drag's guides follow it), a [wall] about to go up, the bomb's [plus] and the pieces it would take away ([gone]: the
 * bomb's hits, or the turn Undo takes back), pale in dashed rings. After a Hint: its [hint] point,
 * circled twice in the player's ink. In an open-ended game, on Stop's note and after a stop: the
 * [open] dots that can still be captured, each in a dashed Highlighter ring, at [openSize] of its
 * size as the rings grow in or shrink away (read while drawing).
 */
data class BoardMarks(
    val targets: List<Pos> = emptyList(),
    val point: Pos? = null,
    val wall: Pos? = null,
    val plus: List<Pos> = emptyList(),
    val gone: List<Pos> = emptyList(),
    val hint: Pos? = null,
    val open: List<Pos> = emptyList(),
    val openSize: () -> Float = { 1f },
) {
    companion object {
        val NONE = BoardMarks()
    }
}

/**
 * The tutorial's point to tap: a ring in the player's ink, and a hand, 48 x 56 dp, whose fingertip
 * touches the point from below and to the right, so it hides little of the board around it.
 */
private fun DrawScope.drawPointer(at: Offset, geometry: BoardGeometry, style: BoardStyle) {
    drawRing(at, geometry.spacing * 0.45f, style.color(Player.RED), max(geometry.spacing * 0.05f, 1.5.dp.toPx()))
    val unit = 1.dp.toPx()
    translate(at.x - 6 * unit, at.y + 4 * unit) {
        scale(unit, unit, pivot = Offset.Zero) {
            drawPath(HAND, style.paper)
            drawPath(HAND, style.pencil, style = Stroke(width = 1.6f, join = StrokeJoin.Round))
        }
    }
}

/** A hand pointing up with its first finger, in a 48 x 56 box (the wireframe's tutorial hand). */
private val HAND: Path = Path().apply {
    moveTo(14f, 4f)
    relativeQuadraticTo(4f, -3f, 7f, 0f)
    relativeLineTo(0f, 22f)
    relativeLineTo(2f, -10f)
    relativeQuadraticTo(4f, -3f, 7f, 0f)
    relativeLineTo(0f, 10f)
    relativeLineTo(2f, -6f)
    relativeQuadraticTo(4f, -3f, 7f, 0f)
    relativeLineTo(0f, 8f)
    relativeLineTo(1f, -3f)
    relativeQuadraticTo(4f, -2f, 6f, 1f)
    relativeLineTo(0f, 14f)
    relativeQuadraticTo(0f, 14f, -14f, 14f)
    relativeLineTo(-6f, 0f)
    relativeQuadraticTo(-8f, 0f, -13f, -8f)
    relativeLineTo(-9f, -14f)
    relativeQuadraticTo(-1f, -5f, 4f, -5f)
    relativeLineTo(6f, 6f)
    close()
}

/**
 * Every layer of the position, in the theme's drawing order (theme-notebook.md, "The board"):
 * paper, washes, hatching, grid, links, drag's guides along [guided]'s row and column, the
 * high-contrast bands round the pieces, rings, threat rings, pieces and their cuts, the last
 * move, closing points, the preview. The booster marks lie with the pieces they are about, and
 * the circled points on top.
 */
private fun DrawScope.drawPosition(
    state: GameState,
    areas: List<Area>,
    geometry: BoardGeometry,
    style: BoardStyle,
    lastMove: Pos?,
    preview: Pos?,
    showLinks: Boolean,
    dangers: List<Danger>,
    marked: Pos?,
    marks: BoardMarks,
    guided: Pos?,
    frame: Frame,
    guideReach: Float,
) {
    drawRect(style.paper)
    val strokes = areas.map { ringStroke(it, geometry, style.look) }
    // Undo: the areas of the turn it takes back blend back to paper while their rings undraw.
    val undoing = frame.undoing
    val undoneAreas = if (undoing == null) emptyList() else frame.undone.areas
    val undoneStrokes = undoneAreas.map { ringStroke(it, geometry, style.look) }
    undoneAreas.forEachIndexed { i, area ->
        val p = frame.rubbedOut(area.by)
        if (p < 1f) drawArea(area.by, undoneStrokes[i], style, fade(style.palette.wash(area.by), style.paper, p), fade(style.palette.pale(area.by), style.paper, p))
    }
    areas.forEachIndexed { i, area ->
        val capturing = frame.capturing[i]
        val lit = frame.glow[i] ?: 0f
        val wash = if (lit > 0f) lerp(style.palette.wash(area.by), style.palette.pale(area.by), lit) else style.palette.wash(area.by)
        when {
            capturing == null -> drawArea(area.by, strokes[i], style, wash)
            // Rubber stamp: the area comes down with the ring, large and pale, and shrinks to its place.
            style.look.ringEffect == RingEffect.STAMP -> {
                val down = RingEffects.down(frame.timing!!, frame.t)
                if (down > 0f) {
                    val stamp = ringStroke(area, geometry, style.look, RingEffects.stamp(frame.timing, frame.t))
                    drawArea(area.by, stamp, style, fade(wash, style.paper, 1f - down), fade(style.palette.pale(area.by), style.paper, 1f - down))
                }
            }
            // Star chart: the haze and its hatching blend in from the paper over the whole area.
            style.look.capture == CaptureMotion.CONSTELLATION -> {
                val haze = 1f - Twinkle.haze(frame.timing!!, frame.t)
                if (haze < 1f) drawArea(area.by, strokes[i], style, fade(wash, style.paper, haze), fade(style.palette.pale(area.by), style.paper, haze))
            }
            else -> drawSpreadingArea(area, strokes[i], capturing.capture, frame, geometry, style, wash)
        }
    }
    drawPlusDiscs(state, marks.plus, geometry, style)
    drawGrid(state, geometry, style)
    drawChartPoints(state, geometry, style)
    val boosting = frame.boosting
    val wallGoingUp = boosting?.point?.takeIf { boosting.booster == Booster.WALL }
    drawWalls(state, geometry, style, skip = wallGoingUp)
    wallGoingUp?.let { drawWallGoingUp(geometry.center(it), geometry, style, WallEffect.square(frame.bt), WallEffect.hatch(frame.bt)) }
    // Undo gives back a wall used in the turn: it stays until it flies back to its button.
    val givenBack = undoing?.givenBack?.takeIf { frame.bt < UndoEffect.FLIGHT_FROM }
    if (givenBack == Booster.WALL) undoing?.givenBackAt?.let { drawWall(geometry.center(it), geometry, style, preview = false) }
    drawTargets(state, marks.targets, geometry, style)
    marks.wall?.let { drawWall(geometry.center(it), geometry, style, preview = true) }
    if (showLinks) {
        drawLinks(state, geometry, style, frame)
        drawGoingLinks(state, geometry, style, frame)
    }
    guided?.let { drawGuides(state, it, geometry, style, areas, strokes, guideReach) }
    drawHalos(state, geometry, style, areas, strokes, frame)
    drawGlows(state, geometry, style, frame)
    // A tube is drawn in three passes, every ring's glow before any tube, so no glow lies over another ring's tube.
    val layers = if (style.look.ringGlow.dp > 0f || style.look.ringCore > 0f) RingLayer.entries else listOf(RingLayer.TUBE)
    // A ring effect's ring on its way to its place: the loose rope, the stamp coming down (cosmetics.md).
    val moving = areas.indices.associateWith { i -> effectRing(areas[i], strokes[i], frame.capturing[i], frame, geometry, style) }
    for (layer in layers) {
        areas.forEachIndexed { i, area ->
            val width = 1f + 0.9f * (frame.glow[i] ?: 0f)
            val capturing = frame.capturing[i]
            val timing = frame.timing
            // Neon arcade: the tube is drawn unlit and lights up as it closes.
            val lit = capturing == null || timing == null || style.look.capture != CaptureMotion.NEON || NeonLight.lit(timing, frame.t)
            if (layer != RingLayer.TUBE && !lit) return@forEachIndexed
            if (capturing == null || timing == null) return@forEachIndexed drawRingOf(area.by, strokes[i], style, width = width, layer = layer)
            when (style.look.ringEffect) {
                RingEffect.THEME -> {
                    val drawn = if (style.look.capture == CaptureMotion.CONSTELLATION) Twinkle.traced(timing, frame.t) else timing.ring(frame.t)
                    drawAreaRing(area.by, strokes[i], style, from = capturing.start, drawn = drawn, width = width, layer = layer)
                }
                // Thrown loose round the area from the closing piece, then pulled tight.
                RingEffect.LASSO -> drawRope(area.by, moving.getValue(i), style, from = capturing.start, drawn = RingEffects.thrown(timing, frame.t), width = width, layer = layer)
                // Down at once, pale and large, shrinking to its place as its colours come in.
                RingEffect.STAMP -> {
                    val down = RingEffects.down(timing, frame.t)
                    if (down > 0f) drawStamp(area.by, moving.getValue(i), style, width = width, layer = layer, pale = 1f - down)
                }
            }
        }
        undoneAreas.forEachIndexed { i, area -> drawRingOf(area.by, undoneStrokes[i], style, drawn = 1f - frame.rubbedOut(area.by), layer = layer) }
    }
    drawThreatRings(dangers, geometry, style, frame)
    if (marks.open.isNotEmpty()) drawOpenRings(marks.open, marks.openSize(), geometry, style)
    drawPlusRings(state, marks.plus, geometry, style)
    drawRipples(geometry, style, frame)
    drawDots(state, geometry, style, frame)
    drawBombHits(geometry, style, frame)
    drawRubbingOut(geometry, style, frame)
    drawGone(state, marks.gone, marks.plus, geometry, style)
    val look = style.look
    val spacing = geometry.spacing
    val moment = frame.moment
    val pinhole = when {
        moment != null && frame.t < Placing.PINHOLE -> moment.pinholeBefore
        // Undo moves the pinhole once the turn is gone.
        undoing != null && frame.bt < UndoEffect.SETTLED -> null
        else -> lastMove
    }
    // A bomb may take the last dot away: the pinhole goes with it.
    // A dot the bomb's plus would take shows no pinhole: it is already pale.
    pinhole?.takeIf { state.ownerAt(it) != null && !(it in marks.plus && it in marks.gone) }?.let { drawLastMove(geometry.center(it), geometry, style) }
    drawClosingPoints(state, dangers, geometry, style, frame)
    drawBurst(geometry, style, frame)
    drawSparksOfLight(state, geometry, style, frame)
    drawBomb(geometry, style, frame)
    drawSparks(geometry, style, frame)
    val ink = style.color(Player.RED)
    marked?.let { drawCircled(geometry.center(it), geometry, ink) }
    marks.hint?.let { hint ->
        // A Hint circles its point twice as it is shown.
        val loops = if (boosting?.booster == Booster.HINT && boosting.point == hint) HintEffect.loops(frame.bt) else 2f
        drawCircled(geometry.center(hint), geometry, ink, loops = loops)
    }
    // When the player's dot lands, the Hint's circles shrink away; Undo leaves them until the Hint flies back.
    moment?.hintBefore?.takeIf { frame.t < HintEffect.SHRINK }?.let { drawCircled(geometry.center(it), geometry, ink, size = HintEffect.shrink(frame.t)) }
    if (givenBack == Booster.HINT) undoing?.givenBackAt?.let { drawCircled(geometry.center(it), geometry, ink) }
    preview?.let {
        val center = geometry.center(it)
        val piece = style.piece(state.toMove)
        val dot = look.dot.px(spacing, density)
        drawPiece(piece, center, dot, style.palette.pale(state.toMove))
        val outline = look.previewOutline.px(spacing, density)
        if (outline > 0f) drawPiece(piece, center, dot, style.color(state.toMove), outline)
        drawRing(center, look.previewRing.px(spacing, density) / 2, style.color(state.toMove), look.previewLine.px(spacing, density))
    }
    drawEdgeMarks(state, geometry, style, frame)
}

/**
 * A captured area while its capture plays (motion.md, "Capture"): the wash and the hatching spread
 * from the closing dot as a growing circle, kept inside the ring.
 */
private fun DrawScope.drawSpreadingArea(area: Area, stroke: RingStroke, capture: Capture, frame: Frame, geometry: BoardGeometry, style: BoardStyle, wash: Color) {
    val spread = frame.timing!!.wash(frame.t)
    if (spread <= 0f) return
    if (spread >= 1f) return drawArea(area.by, stroke, style, wash)
    val from = geometry.center(capture.closing)
    val reach = area.ring.maxOf { val c = geometry.center(it); hypot(c.x - from.x, c.y - from.y) } + geometry.spacing
    val circle = Path().apply { addOval(Rect(from, reach * spread)) }
    clipPath(circle) { drawArea(area.by, stroke, style, wash) }
}

/**
 * The ripples of an announced dot (motion.md, "The computer's move"): two rings in its side's ink
 * grow from 22 dp to 96 dp across while their line thins from 3 dp to 0.5 dp. They are drawn
 * under the pieces, so they never hide a dot.
 */
private fun DrawScope.drawRipples(geometry: BoardGeometry, style: BoardStyle, frame: Frame) {
    val moment = frame.moment ?: return
    if (!moment.announced || frame.t >= Announce.LENGTH) return
    val center = geometry.center(moment.dot)
    val ink = style.color(moment.by)
    for (i in Announce.RIPPLES.indices) {
        val p = Announce.ripple(frame.t, i) ?: continue
        val across = Announce.RIPPLE_FROM + (Announce.RIPPLE_TO - Announce.RIPPLE_FROM) * p
        val line = Announce.LINE_FROM + (Announce.LINE_TO - Announce.LINE_FROM) * p
        drawRing(center, across.dp.toPx() / 2, ink, line.dp.toPx())
    }
}

/**
 * The edge marks of an announced dot: a short stroke on the top edge above its column and one on
 * the left edge beside its row, in its side's ink, in the half spacing of paper outside the grid.
 * They grow in from the edges as the dot lands, and shrink back when the player's next dot lands.
 */
private fun DrawScope.drawEdgeMarks(state: GameState, geometry: BoardGeometry, style: BoardStyle, frame: Frame) {
    val moment = frame.moment
    val t = frame.t
    val shown = frame.edgeMarks
    if (shown != null) {
        val reach = if (moment != null && moment.announced && moment.dot == shown) Announce.marksIn(t) else 1f
        drawEdgeMark(shown, reach, state, geometry, style)
    }
    val gone = moment?.marksGone
    if (gone != null && gone != shown) drawEdgeMark(gone, Announce.marksOut(t), state, geometry, style)
}

private fun DrawScope.drawEdgeMark(dot: Pos, reach: Float, state: GameState, geometry: BoardGeometry, style: BoardStyle) {
    if (reach <= 0f) return
    val owner = state.ownerAt(dot) ?: return
    val ink = style.color(owner)
    val length = Announce.MARK_LENGTH.dp.toPx() * reach
    val width = Announce.MARK_LINE.dp.toPx()
    val center = geometry.center(dot)
    drawLine(ink, Offset(center.x, 0f), Offset(center.x, length), width)
    drawLine(ink, Offset(0f, center.y), Offset(length, center.y), width)
}

/**
 * A big capture's burst (motion.md, "Big capture"): 14 short strokes in the captor's ink around
 * the area move outwards, grow and shrink away.
 */
private fun DrawScope.drawBurst(geometry: BoardGeometry, style: BoardStyle, frame: Frame) {
    val timing = frame.timing ?: return
    val p = timing.burst(frame.t) ?: return
    for (capture in frame.moment?.captures.orEmpty()) {
        val ring = capture.ring.map(geometry::center)
        val cx = ring.sumOf { it.x.toDouble() }.toFloat() / ring.size
        val cy = ring.sumOf { it.y.toDouble() }.toFloat() / ring.size
        val radius = ring.maxOf { hypot(it.x - cx, it.y - cy) } + geometry.spacing * 0.35f
        val out = geometry.spacing * 0.8f * p
        val length = geometry.spacing * 0.45f * sin(PI * p).toFloat()
        val ink = style.color(capture.by)
        for (k in 0 until Capturing.BURST_STROKES) {
            val angle = 2 * PI * k / Capturing.BURST_STROKES + 0.2
            val dx = cos(angle).toFloat()
            val dy = sin(angle).toFloat()
            val from = radius + out
            drawLine(ink, Offset(cx + dx * from, cy + dy * from), Offset(cx + dx * (from + length), cy + dy * (from + length)), 2.dp.toPx(), cap = StrokeCap.Round)
        }
    }
}

/**
 * A captured area's ring as a pen stroke (theme-notebook.md, "The ring"): its [outline], through the
 * centres of the ring's dots, each side bowed a little, and the [sides] one by one, each with its
 * own width. The bow and the width come from a seed taken from the area, so the same ring looks
 * the same on every frame.
 */
internal class RingStroke(
    val outline: Path,
    val sides: List<Pair<Path, Float>>,
    /** Each side as a quadratic curve: its start, its control point and its end. */
    val curves: List<Triple<Offset, Offset, Offset>> = emptyList(),
    /** The theme's ring width in pixels, before each side's own change. */
    val width: Float = 0f,
    /** The board's spacing in pixels. */
    val spacing: Float = 0f,
    /** The ring's size as a share of its place: above 1 while a ring effect brings it in. */
    val scale: Float = 1f,
) {
    /** The same ring with other [sides]: its glow or its core. */
    fun withSides(sides: List<Pair<Path, Float>>): RingStroke = RingStroke(outline, sides, curves, width, spacing, scale)
}

/**
 * [area]'s ring at [scale] times its size about the middle of its dots: 1 is its place on the
 * board. Its sides bow the same way at every size.
 */
private fun DrawScope.ringStroke(area: Area, geometry: BoardGeometry, look: BoardLook, scale: Float = 1f): RingStroke {
    val first = area.region.first()
    val random = Random(31 * (31 * area.by.ordinal + first.x) + first.y)
    val centres = area.ring.map(geometry::center)
    val points = if (scale == 1f) {
        centres
    } else {
        val mx = centres.sumOf { it.x.toDouble() }.toFloat() / centres.size
        val my = centres.sumOf { it.y.toDouble() }.toFloat() / centres.size
        centres.map { Offset(mx + (it.x - mx) * scale, my + (it.y - my) * scale) }
    }
    val bow = look.ringBow.dp.toPx()
    val width = look.ring.px(geometry.spacing, density)
    val outline = Path()
    val sides = ArrayList<Pair<Path, Float>>(points.size)
    val curves = ArrayList<Triple<Offset, Offset, Offset>>(points.size)
    for (i in points.indices) {
        val a = points[i]
        val b = points[(i + 1) % points.size]
        val length = hypot(b.x - a.x, b.y - a.y)
        // A quadratic curve's middle lies halfway to its control point: the control point sits
        // twice the bow off the straight line.
        val offset = if (length > 0f) 2 * bow * (random.nextFloat() * 2 - 1) / length else 0f
        val cx = (a.x + b.x) / 2 - (b.y - a.y) * offset
        val cy = (a.y + b.y) / 2 + (b.x - a.x) * offset
        val sideWidth = width * (1 + look.ringWobble * (random.nextFloat() * 2 - 1))
        if (i == 0) outline.moveTo(a.x, a.y)
        outline.quadraticTo(cx, cy, b.x, b.y)
        sides += Path().apply {
            moveTo(a.x, a.y)
            quadraticTo(cx, cy, b.x, b.y)
        } to sideWidth
        curves += Triple(a, Offset(cx, cy), b)
    }
    outline.close()
    return RingStroke(outline, sides, curves, width, geometry.spacing, scale)
}

/**
 * The ring of [area] as its ring effect shows it in this frame: Lasso's rope loose, or Rubber
 * stamp's ring still coming down, at its size about the area's middle; [stroke] once in place.
 */
private fun DrawScope.effectRing(area: Area, stroke: RingStroke, capturing: AreaCapture?, frame: Frame, geometry: BoardGeometry, style: BoardStyle): RingStroke {
    val timing = frame.timing
    if (capturing == null || timing == null) return stroke
    val scale = when (style.look.ringEffect) {
        RingEffect.THEME -> 1f
        RingEffect.LASSO -> RingEffects.rope(timing, frame.t)
        RingEffect.STAMP -> RingEffects.stamp(timing, frame.t)
    }
    return if (scale == 1f) stroke else ringStroke(area, geometry, style.look, scale)
}

/**
 * Inside a ring: the captor's wash, then its hatching, clipped to the ring (theme-notebook.md,
 * "Captured areas"). Red's lines rise to the right, Blue's fall, one every [BoardLook.hatchGap] dp
 * at every spacing.
 */
private fun DrawScope.drawArea(by: Player, stroke: RingStroke, style: BoardStyle, wash: Color = style.palette.wash(by), ink: Color = style.palette.pale(by)) {
    drawPath(stroke.outline, wash)
    val width = style.look.hatch.px(0f, density)
    if (width <= 0f) return
    // Lines x + y = c rise to the right; lines x - y = c fall. A gap of g between lines is g times
    // the square root of 2 along an axis.
    val step = style.look.hatchGap.dp.toPx() * sqrt(2f)
    val box = stroke.outline.getBounds()
    val rising = by == Player.RED
    val from = if (rising) box.left + box.top else box.left - box.bottom
    val to = if (rising) box.right + box.bottom else box.right - box.top
    clipPath(stroke.outline) {
        var c = floor(from / step) * step
        while (c <= to) {
            if (rising) {
                drawLine(ink, Offset(c - box.top, box.top), Offset(c - box.bottom, box.bottom), width)
            } else {
                drawLine(ink, Offset(c + box.top, box.top), Offset(c + box.bottom, box.bottom), width)
            }
            c += step
        }
    }
}

/** The passes of a ring drawn as a tube (theme-neon-arcade.md, "The ring"); a plain ring is the tube alone. */
internal enum class RingLayer { GLOW, TUBE, CORE }

/**
 * A captured area's ring in place, as the ring effect draws it: the theme's ring, Lasso's rope or
 * Rubber stamp's double line. As Undo takes it back it is undrawn ([drawn] below 1) from side [from].
 */
private fun DrawScope.drawRingOf(by: Player, ring: RingStroke, style: BoardStyle, from: Int = 0, drawn: Float = 1f, width: Float = 1f, layer: RingLayer) {
    when (style.look.ringEffect) {
        RingEffect.THEME -> drawAreaRing(by, ring, style, from, drawn, width, layer)
        RingEffect.LASSO -> drawRope(by, ring, style, from, drawn, width, layer)
        RingEffect.STAMP -> drawStamp(by, ring, style, from, drawn, width, layer)
    }
}

/**
 * The ring round a captured area, in the captor's ink, each side with round ends, [width] times
 * as thick as drawn. While a capture plays, it is drawn like a pen stroke: from side [from] (the
 * closing dot) round the area, as far as [drawn] of its length. As a tube, its [layer] is the
 * glow to either side of it in the captor's pale ink, the tube itself, or the core along its
 * middle; all three thicken together. [pale] blends it towards the paper (Rubber stamp coming down).
 */
internal fun DrawScope.drawAreaRing(
    by: Player,
    ring: RingStroke,
    style: BoardStyle,
    from: Int = 0,
    drawn: Float = 1f,
    width: Float = 1f,
    layer: RingLayer = RingLayer.TUBE,
    pale: Float = 0f,
) {
    val look = style.look
    val ink = fade(
        when (layer) {
            RingLayer.GLOW -> style.palette.pale(by)
            RingLayer.TUBE -> style.color(by)
            RingLayer.CORE -> style.palette.core(by)
        },
        style.paper,
        pale,
    )
    val glow = look.ringGlow.px(0f, density)
    val stroke = when (layer) {
        RingLayer.GLOW -> ring.withSides(ring.sides.map { (side, w) -> side to w + 2 * glow })
        RingLayer.TUBE -> ring
        RingLayer.CORE -> ring.withSides(ring.sides.map { (side, w) -> side to w * look.ringCore })
    }
    if (drawn >= 1f) {
        for ((side, w) in stroke.sides) drawPath(side, ink, style = Stroke(w * width, cap = StrokeCap.Round))
        return
    }
    if (drawn <= 0f) return
    val sides = stroke.sides.drop(from) + stroke.sides.take(from)
    val measure = PathMeasure()
    val lengths = sides.map { (side, _) -> measure.setPath(side, false); measure.length }
    var left = lengths.sum() * drawn
    for ((i, pair) in sides.withIndex()) {
        val (side, w) = pair
        val length = lengths[i]
        if (left >= length) {
            drawPath(side, ink, style = Stroke(w * width, cap = StrokeCap.Round))
            left -= length
        } else {
            measure.setPath(side, false)
            val part = Path()
            measure.getSegment(0f, left, part, true)
            drawPath(part, ink, style = Stroke(w * width, cap = StrokeCap.Round))
            break
        }
    }
}

/** Every blocked point, a wall booster's or the level's own, with the wall's mark; the wall going up at [skip] draws itself. */
private fun DrawScope.drawWalls(state: GameState, geometry: BoardGeometry, style: BoardStyle, skip: Pos? = null) {
    for (y in 0 until state.height) for (x in 0 until state.width) {
        if (state.isBlocked(x, y) && !state.isHole(x, y) && (skip == null || skip.x != x || skip.y != y)) {
            drawWall(geometry.center(x, y), geometry, style, preview = false)
        }
    }
}

/**
 * The wall's mark (motion.md, Boosters: Wall): a square, 0.7 of the spacing, round the point in one
 * Pencil stroke, filled with five hatch lines in Graphite. In the [preview] its outline is dashed.
 */
private fun DrawScope.drawWall(at: Offset, geometry: BoardGeometry, style: BoardStyle, preview: Boolean) {
    val half = geometry.spacing * 0.35f
    val width = max(geometry.spacing * 0.075f, 1.5.dp.toPx())
    val left = at.x - half
    val top = at.y - half
    clipRect(left, top, at.x + half, at.y + half) {
        val graphite = style.graphite
        for (i in 1..5) {
            // Lines rising to the right, evenly across the square's diagonal.
            val d = 4 * half * i / 6f
            drawLine(graphite, Offset(left, top + d), Offset(left + d, top), width * 0.6f)
        }
    }
    val effect = if (preview) PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.dp.toPx())) else null
    drawRect(style.pencil, Offset(left, top), Size(2 * half, 2 * half), style = Stroke(width, pathEffect = effect))
}

/** The points a booster may be aimed at: a small dot in the pale ink of the side to move. */
private fun DrawScope.drawTargets(state: GameState, targets: List<Pos>, geometry: BoardGeometry, style: BoardStyle) {
    val ink = style.palette.pale(state.toMove)
    for (point in targets) drawCircle(ink, geometry.spacing * 0.1f, geometry.center(point))
}

/**
 * The bomb's plus (theme-notebook.md, "The marks they lead to"): each point on a disc of the wash of
 * the side to move, 0.92 of the spacing across. The discs lie under the grid, like a captured
 * area's wash, so the pieces in the plus stay on top.
 */
private fun DrawScope.drawPlusDiscs(state: GameState, plus: List<Pos>, geometry: BoardGeometry, style: BoardStyle) {
    val wash = style.palette.wash(state.toMove)
    for (point in plus) drawCircle(wash, geometry.spacing * 0.46f, geometry.center(point))
}

/** The bomb's plus rings: dashed, in the ink of the side to move, drawn with the rings and under the pieces. */
private fun DrawScope.drawPlusRings(state: GameState, plus: List<Pos>, geometry: BoardGeometry, style: BoardStyle) {
    val ink = style.color(state.toMove)
    for (point in plus) drawDashedRing(geometry.center(point), geometry.spacing * 0.46f, ink, 1.5.dp.toPx())
}

/**
 * Pieces a booster would take away: pale, in a dashed Graphite ring (Undo's preview, motion.md).
 * The bomb's hits lie in its [plus]: they sit on its wash and its rings already mark them.
 */
private fun DrawScope.drawGone(state: GameState, gone: List<Pos>, plus: List<Pos>, geometry: BoardGeometry, style: BoardStyle) {
    val dot = style.look.dot.px(geometry.spacing, density)
    val glow = style.look.glow.px(geometry.spacing, density)
    for (point in gone) {
        val owner = state.ownerAt(point.x, point.y) ?: continue
        val c = geometry.center(point)
        val piece = style.piece(owner)
        val inPlus = point in plus
        // The surface under it hides the live piece, and its glow in Neon arcade.
        val surface = if (inPlus) style.palette.wash(state.toMove) else style.paper
        if (glow > 0f) drawPieceBand(piece, c, dot, surface, glow + dot * 0.05f) else drawPiece(piece.copy(across = piece.across * 1.1f), c, dot, surface)
        drawPiece(piece, c, dot, style.palette.pale(owner))
        if (!inPlus) drawDashedRing(c, geometry.spacing * 0.45f, style.graphite, 1.5.dp.toPx())
    }
}

/**
 * The dots that can still be captured after a stop (motion.md, "Stop and Done"): a dashed
 * Highlighter ring round each, 0.84 of the spacing across, at [size] of it (draw.py, `open`).
 */
private fun DrawScope.drawOpenRings(open: List<Pos>, size: Float, geometry: BoardGeometry, style: BoardStyle) {
    if (size <= 0f) return
    val dash = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.5.dp.toPx()))
    val radius = geometry.spacing * 0.42f * size
    for (point in open) drawCircle(style.palette.highlighter, radius, geometry.center(point), style = Stroke(1.5.dp.toPx(), pathEffect = dash))
}

private fun DrawScope.drawDashedRing(center: Offset, radius: Float, color: Color, width: Float) {
    val dash = 3.dp.toPx()
    drawCircle(color, radius, center, style = Stroke(width, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash))))
}

/**
 * Drag's guides (theme-notebook.md, "Drag's guides"): four arms from the preview's ring along its
 * row and column to the board's outermost grid lines, in the ink of the side to move, dashed in
 * the theme's dashes, each arm's dashes starting at the ring. A fingertip covers about 60 dp; the
 * arms run past it.
 */
private fun DrawScope.drawGuides(state: GameState, point: Pos, geometry: BoardGeometry, style: BoardStyle, areas: List<Area>, strokes: List<RingStroke>, reach: Float) {
    val look = style.look
    val ink = style.color(state.toMove).copy(alpha = look.guideAlpha)
    val width = look.guide.px(geometry.spacing, density)
    val dashes = if (look.guideDash > 0f) PathEffect.dashPathEffect(floatArrayOf(look.guideDash.dp.toPx(), look.guideGap.dp.toPx())) else null
    val center = geometry.center(point)
    val ring = look.previewRing.px(geometry.spacing, density) / 2
    val ends = listOf(
        geometry.center(0, point.y),
        geometry.center(state.width - 1, point.y),
        geometry.center(point.x, 0),
        geometry.center(point.x, state.height - 1),
    )
    val arms = ends.mapNotNull { end ->
        val length = hypot(end.x - center.x, end.y - center.y)
        if (length <= ring) return@mapNotNull null
        val start = Offset(center.x + (end.x - center.x) * ring / length, center.y + (end.y - center.y) * ring / length)
        // Drawn out from the ring to the edge as the finger goes down (motion.md, "Placing by drag").
        start to if (reach >= 1f) end else Offset(start.x + (end.x - start.x) * reach, start.y + (end.y - start.y) * reach)
    }
    // High contrast: each arm on a strip of the paper or wash under it, hiding the grid and hatching there.
    val casing = look.guideCasing.px(geometry.spacing, density)
    if (casing > 0f) onSurface(areas, strokes, style) { surface -> for ((start, end) in arms) drawLine(surface, start, end, width + 2 * casing) }
    for ((start, end) in arms) drawLine(ink, start, end, width, pathEffect = dashes)
}

/**
 * Draws [layer] in the paper, then again inside each captured area's ring in its wash, so it takes
 * the colour of whatever lies under it (high contrast's bands and strips).
 */
private fun DrawScope.onSurface(areas: List<Area>, strokes: List<RingStroke>, style: BoardStyle, layer: DrawScope.(Color) -> Unit) {
    layer(style.paper)
    for ((area, stroke) in areas.zip(strokes)) clipPath(stroke.outline) { layer(style.palette.wash(area.by)) }
}

/**
 * High contrast (pieces.md, "High contrast"): a band [BoardLook.halo] wide round every piece, in the
 * paper or wash under it, so grid lines, hatching, links and guides stop short of the piece. Rings
 * and threat rings are drawn after it and stay whole.
 */
private fun DrawScope.drawHalos(state: GameState, geometry: BoardGeometry, style: BoardStyle, areas: List<Area>, strokes: List<RingStroke>, frame: Frame) {
    val halo = style.look.halo.px(geometry.spacing, density)
    if (halo <= 0f) return
    val dot = style.look.dot.px(geometry.spacing, density)
    onSurface(areas, strokes, style) { surface ->
        for (y in 0 until state.height) for (x in 0 until state.width) {
            val owner = state.ownerAt(x, y) ?: continue
            val scale = frame.pieceScale(x, y, style.look)
            if (scale <= 0f) continue
            val piece = style.piece(owner)
            if (piece.shape == PieceShape.DOT || piece.shape == PieceShape.DIAMOND) {
                drawPiece(piece.grown(halo, dot).let { it.copy(across = it.across * scale) }, geometry.center(x, y), dot, surface)
            } else {
                drawPieceBand(piece.copy(across = piece.across * scale), geometry.center(x, y), dot, surface, halo * scale)
            }
        }
    }
}

/**
 * Neon arcade's glow (theme-neon-arcade.md, "The board"): a band [BoardLook.glow] wide round every
 * live piece in its side's pale ink. It grows and shrinks with its piece, a piece just placed
 * lights up as it settles (motion.md, "Placing a dot in Neon arcade"), and a captured piece goes
 * dark as its cut starts.
 */
private fun DrawScope.drawGlows(state: GameState, geometry: BoardGeometry, style: BoardStyle, frame: Frame) {
    val glow = style.look.glow.px(geometry.spacing, density)
    if (glow <= 0f || style.look.halo.px(geometry.spacing, density) > 0f) return
    val dot = style.look.dot.px(geometry.spacing, density)
    val newDot = frame.newDot
    for (y in 0 until state.height) for (x in 0 until state.width) {
        val owner = state.ownerAt(x, y) ?: continue
        val pos = Pos(x, y)
        val cut = if (state.isCaptured(x, y)) frame.cuts[pos] ?: 1f else frame.cutLeft(pos)
        if (cut > 0f) continue
        val scale = frame.pieceScale(x, y, style.look)
        val lit = if (newDot == pos) NeonLight.glow(frame.t) else 1f
        if (scale <= 0f || lit <= 0f) continue
        val piece = style.piece(owner)
        drawPieceBand(piece.copy(across = piece.across * scale), geometry.center(x, y), dot, style.palette.pale(owner), glow * scale * lit)
    }
}

/** A point at every grid crossing, in a theme whose grid has them (theme-star-chart.md, "Chart points"). */
private fun DrawScope.drawChartPoints(state: GameState, geometry: BoardGeometry, style: BoardStyle) {
    val across = style.look.chartPoint.px(geometry.spacing, density)
    if (across <= 0f) return
    for (y in 0 until state.height) for (x in 0 until state.width) {
        if (!state.isHole(x, y)) drawCircle(style.palette.chartPoint, across / 2, geometry.center(x, y))
    }
}

/**
 * Neon arcade's sparks (motion.md, "Capture in Neon arcade"): as the tube lights up, up to ten
 * short lit streaks fly out from the closing piece, away from the area, growing and then shrinking
 * away. A way that would cross another piece is left out.
 */
private fun DrawScope.drawSparksOfLight(state: GameState, geometry: BoardGeometry, style: BoardStyle, frame: Frame) {
    if (style.look.capture != CaptureMotion.NEON) return
    val timing = frame.timing ?: return
    val q = NeonLight.sparks(timing, frame.t) ?: return
    val u = geometry.unit()
    val from = (NeonLight.SPARK_FROM + (NeonLight.SPARK_TO - NeonLight.SPARK_FROM - NeonLight.SPARK_LONG) * Ease.OUT.transform(q)) * u
    val length = NeonLight.SPARK_LONG * sin(PI * q).toFloat() * u
    if (length < 0.3f * u) return
    val pieces = (0 until state.height).flatMap { y -> (0 until state.width).filter { x -> state.ownerAt(x, y) != null }.map { x -> Pos(x, y) } }
    for (capture in frame.moment?.captures.orEmpty()) {
        val closing = capture.closing
        val center = geometry.center(closing)
        val ring = capture.ring.map(geometry::center)
        val away = atan2(center.y - ring.sumOf { it.y.toDouble() }.toFloat() / ring.size, center.x - ring.sumOf { it.x.toDouble() }.toFloat() / ring.size)
        val spread = Math.toRadians(NeonLight.SPARK_SPREAD.toDouble()).toFloat()
        for (k in 0 until NeonLight.SPARK_COUNT) {
            val angle = away + (k - (NeonLight.SPARK_COUNT - 1) / 2f) * spread / (NeonLight.SPARK_COUNT - 1)
            val dx = cos(angle)
            val dy = sin(angle)
            // None crosses a piece: every point of its flight stays 8 dp from the other pieces' centres.
            val crosses = pieces.any { p ->
                p != closing && generateSequence(NeonLight.SPARK_FROM) { it + 2f }.takeWhile { it <= NeonLight.SPARK_TO }.any { r ->
                    val c = geometry.center(p)
                    hypot(center.x + dx * r * u - c.x, center.y + dy * r * u - c.y) < 8f * u
                }
            }
            if (crosses) continue
            val start = Offset(center.x + dx * from, center.y + dy * from)
            val end = Offset(center.x + dx * (from + length), center.y + dy * (from + length))
            drawLine(style.color(capture.by), start, end, NeonLight.SPARK_LINE * u, cap = StrokeCap.Round)
            drawLine(style.palette.core(capture.by), start, end, NeonLight.SPARK_CORE * u, cap = StrokeCap.Round)
        }
    }
}

/**
 * The last move: a Paper pinhole in the middle of the newest piece (pieces.md), or, in a theme
 * without one, a Grid ring round it.
 */
private fun DrawScope.drawLastMove(at: Offset, geometry: BoardGeometry, style: BoardStyle) {
    val look = style.look
    val spacing = geometry.spacing
    if (look.pinhole > 0f) {
        drawCircle(style.paper, look.dot.px(spacing, density) * look.pinhole / 2, at)
    } else {
        drawRing(at, look.lastMoveRing.px(spacing, density) / 2, style.grid, look.lastMoveLine.px(spacing, density))
    }
}

/** The grid lines, the outermost ones (the board's edge) in the edge's width. */
private fun DrawScope.drawGrid(state: GameState, geometry: BoardGeometry, style: BoardStyle) {
    val line = style.look.grid.px(geometry.spacing, density)
    val edge = style.look.edge.px(geometry.spacing, density)
    for (x in 0 until state.width) {
        val width = if (x == 0 || x == state.width - 1) edge else line
        drawLine(style.grid, geometry.center(x, 0), geometry.center(x, state.height - 1), width)
    }
    for (y in 0 until state.height) {
        val width = if (y == 0 || y == state.height - 1) edge else line
        drawLine(style.grid, geometry.center(0, y), geometry.center(state.width - 1, y), width)
    }
}

/** A line in the side's pale ink between every two live dots of one colour that touch, orthogonally or diagonally. */
private fun DrawScope.drawLinks(state: GameState, geometry: BoardGeometry, style: BoardStyle, frame: Frame) {
    val width = style.look.link.px(geometry.spacing, density)
    // A new dot's links grow from each neighbour to it (motion.md, "Placing a dot and chain lines").
    val newDot = frame.moment?.dot?.takeIf { frame.t < Placing.LINKS_TO }
    val growth = Placing.links(frame.t)
    for (y in 0 until state.height) for (x in 0 until state.width) {
        val owner = state.ownerAt(x, y) ?: continue
        if (state.isCaptured(x, y)) continue
        for ((dx, dy) in LINK_DIRECTIONS) {
            val nx = x + dx
            val ny = y + dy
            if (!state.inBounds(nx, ny) || state.ownerAt(nx, ny) != owner || state.isCaptured(nx, ny)) continue
            var from = geometry.center(x, y)
            var to = geometry.center(nx, ny)
            if (newDot != null && (newDot == Pos(x, y) || newDot == Pos(nx, ny))) {
                if (growth <= 0f) continue
                if (newDot == Pos(x, y)) from = to.also { to = from }
                to = Offset(from.x + (to.x - from.x) * growth, from.y + (to.y - from.y) * growth)
            }
            // The links of a dot growing back after a bomb grow back with it, from the neighbour.
            val here = frame.grownBack(x, y)
            val there = frame.grownBack(nx, ny)
            if (here < 1f || there < 1f) {
                val grown = min(here, there)
                if (grown <= 0f) continue
                if (here < there) from = to.also { to = from }
                to = Offset(from.x + (to.x - from.x) * grown, from.y + (to.y - from.y) * grown)
            }
            drawLine(style.palette.pale(owner), from, to, width)
        }
    }
}

/** Each touching pair once: right, down, and the two diagonals going right. */
private val LINK_DIRECTIONS = listOf(1 to 0, 0 to 1, 1 to 1, 1 to -1)

/**
 * How a threat looks in this frame (motion.md, "Threat pulse"): a new one draws in ([drawIn] from 0
 * to 1) and, when one dot away, pulses ([pulse] 0 to 1). Older threats are still; a new one is not
 * shown until 200 ms after the dot that made it.
 */
private class ThreatLook(val drawIn: Float, val pulse: Float)

private val STILL_THREAT = ThreatLook(1f, 0f)

private fun Frame.threat(danger: Danger): ThreatLook? {
    val moment = moment ?: return STILL_THREAT
    if (t >= Threat.LENGTH || ThreatMark.of(danger) !in moment.freshThreats) return STILL_THREAT
    val since = t - Threat.DELAY
    if (since < 0f) return null
    return ThreatLook(Threat.drawIn(since), if (danger.dotsNeeded == 1) Threat.pulse(since) else 0f)
}

/**
 * A Highlighter ring around every dot of a group close to capture: solid when one dot away, faint
 * when two. A new one shrinks from 130% to its size while its colour blends from Paper, then
 * pulses three times: its line from 2 dp to 3.5 dp and the ring to 108%.
 */
private fun DrawScope.drawThreatRings(dangers: List<Danger>, geometry: BoardGeometry, style: BoardStyle, frame: Frame) {
    val look = style.look
    val spacing = geometry.spacing
    val radius = look.threatRing.px(spacing, density) / 2
    for (danger in dangers) {
        val shown = frame.threat(danger) ?: continue
        val near = danger.dotsNeeded == 1
        val full = if (near) style.palette.highlighter else style.palette.highlighterFaint
        val ring = if (shown.drawIn >= 1f) full else lerp(style.paper, full, shown.drawIn)
        val width = (if (near) look.threatNear else look.threatFar).px(spacing, density) * (1f + 0.75f * shown.pulse)
        val size = (if (shown.drawIn >= 1f) 1f else 1.3f - 0.3f * shown.drawIn) * (1f + 0.08f * shown.pulse)
        for (dot in danger.group) drawRing(geometry.center(dot), radius * size, ring, width)
    }
}

/**
 * For a group one dot from capture, a small circle in the attacker's ink on the point that would
 * close the ring. A new one grows from nothing, then swells to 135% with each pulse.
 */
private fun DrawScope.drawClosingPoints(state: GameState, dangers: List<Danger>, geometry: BoardGeometry, style: BoardStyle, frame: Frame) {
    val look = style.look
    val spacing = geometry.spacing
    for (danger in dangers) {
        if (danger.dotsNeeded != 1) continue
        val shown = frame.threat(danger) ?: continue
        val size = shown.drawIn * (1f + 0.35f * shown.pulse)
        if (size <= 0f) continue
        val attacker = state.ownerAt(danger.group.first())?.opponent ?: continue
        for (point in danger.closingPoints) {
            drawRing(geometry.center(point), look.closingPoint.px(spacing, density) / 2 * size, style.color(attacker), look.closingLine.px(spacing, density))
        }
    }
}

/**
 * Each side's pieces. Captured ones keep the theme's share of their ink and carry its cut
 * (pieces.md): a band in the captor's wash through the middle of the piece, along the captor's
 * hatching, rising for Red and falling for Blue.
 */
private fun DrawScope.drawDots(state: GameState, geometry: BoardGeometry, style: BoardStyle, frame: Frame) {
    val look = style.look
    val dot = look.dot.px(geometry.spacing, density)
    for (y in 0 until state.height) for (x in 0 until state.width) {
        val owner = state.ownerAt(x, y) ?: continue
        val color = style.color(owner)
        val center = geometry.center(x, y)
        val scale = frame.pieceScale(x, y, look)
        if (scale <= 0f) continue
        val piece = style.piece(owner).let { if (scale == 1f) it else it.copy(across = it.across * scale) }
        // A dot being captured keeps its ink until its cut is drawn across it; Undo takes the cut back.
        val cut = if (state.isCaptured(x, y)) frame.cuts[Pos(x, y)] ?: 1f else frame.cutLeft(Pos(x, y))
        drawPiece(piece, center, dot, if (cut > 0f) color.copy(alpha = look.capturedAlpha) else color)
        if (cut > 0f && look.cut > 0f) {
            val captor = owner.opponent
            // Half the cut's length along each axis, at 45°; it is drawn from its lower-left end.
            val d = dot * look.cutLength / 2 / sqrt(2f)
            val dy = if (captor == Player.RED) -d else d
            val start = Offset(center.x - d, center.y - dy)
            val end = Offset(center.x + d, center.y + dy)
            drawLine(style.palette.wash(captor), start, Offset(start.x + (end.x - start.x) * cut, start.y + (end.y - start.y) * cut), dot * look.cut)
        }
    }
}

private fun DrawScope.drawRing(center: Offset, radius: Float, color: Color, width: Float) {
    drawCircle(color, radius, center, style = Stroke(width))
}

/** [color] blended towards [to] by [p]; exactly [color] at 0, so still frames do not move. */
internal fun fade(color: Color, to: Color, p: Float): Color = if (p <= 0f) color else lerp(color, to, min(p, 1f))

/** Booster marks are sized for the canvas's 24 dp spacing: one of its dp, in pixels, at this board's spacing. */
private fun BoardGeometry.unit(): Float = spacing / 24f

/**
 * A point circled twice in the player's ink: the mission's dot and the Hint (the next level's mark
 * on the map). While a Hint is shown its two [loops] are drawn one after the other, each from its
 * own start; when the dot lands they shrink to nothing with [size].
 */
private fun DrawScope.drawCircled(at: Offset, geometry: BoardGeometry, ink: Color, loops: Float = 2f, size: Float = 1f) {
    val rings = listOf(Triple(0.42f, 0.07f, -120f), Triple(0.58f, 0.05f, -80f))
    rings.forEachIndexed { k, (radius, width, start) ->
        val part = (loops - k).coerceIn(0f, 1f)
        if (part <= 0f || size <= 0f) return@forEachIndexed
        val r = geometry.spacing * radius * size
        val line = geometry.spacing * width
        if (part >= 1f) {
            drawRing(at, r, ink, line)
        } else {
            drawArc(ink, start, 360f * part, useCenter = false, topLeft = Offset(at.x - r, at.y - r), size = Size(2 * r, 2 * r), style = Stroke(line, cap = StrokeCap.Round))
        }
    }
}

/**
 * A wall going up (motion.md, Boosters: Wall): the square is drawn round the point in one Pencil
 * stroke as far as [square] says, then the five hatch lines one after another as [hatch] goes to 1.
 */
private fun DrawScope.drawWallGoingUp(at: Offset, geometry: BoardGeometry, style: BoardStyle, square: Float, hatch: Float) {
    if (square >= 1f && hatch >= 1f) return drawWall(at, geometry, style, preview = false)
    val half = geometry.spacing * 0.35f
    val width = max(geometry.spacing * 0.075f, 1.5.dp.toPx())
    val left = at.x - half
    val top = at.y - half
    for (i in 1..5) {
        val part = (hatch * 5 - (i - 1)).coerceIn(0f, 1f)
        if (part <= 0f) continue
        // The rising line x + y = d inside the square, from its lower-left end.
        val d = 4 * half * i / 6f
        val start = Offset(left + max(0f, d - 2 * half), top + min(d, 2 * half))
        val end = Offset(left + min(d, 2 * half), top + max(0f, d - 2 * half))
        drawLine(style.graphite, start, Offset(start.x + (end.x - start.x) * part, start.y + (end.y - start.y) * part), width * 0.6f)
    }
    if (square <= 0f) return
    val outline = Path().apply {
        moveTo(left, top)
        lineTo(left + 2 * half, top)
        lineTo(left + 2 * half, top + 2 * half)
        lineTo(left, top + 2 * half)
        close()
    }
    val shown = if (square >= 1f) {
        outline
    } else {
        val measure = PathMeasure()
        measure.setPath(outline, false)
        Path().also { measure.getSegment(0f, measure.length * square, it, true) }
    }
    drawPath(shown, style.pencil, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/**
 * The links that go with a booster: a bomb's hit dots lose theirs at the blast, and the dots Undo
 * rubs out pull theirs back from the neighbours.
 */
private fun DrawScope.drawGoingLinks(state: GameState, geometry: BoardGeometry, style: BoardStyle, frame: Frame) {
    val boosting = frame.boosting ?: return
    val width = style.look.link.px(geometry.spacing, density)
    if (boosting.booster == Booster.BOMB && frame.bt < BombEffect.BLAST) {
        val owner = boosting.by.opponent
        for (dot in boosting.removed) for (n in neighbours(dot)) {
            val other = n in boosting.removed
            if (!other && (!state.inBounds(n.x, n.y) || state.ownerAt(n) != owner || state.isCaptured(n.x, n.y))) continue
            drawLine(style.palette.pale(owner), geometry.center(dot), geometry.center(n), width)
        }
    }
    val undone = boosting.undone ?: return
    val before = undone.before
    for (dot in undone.answer + undone.own) {
        val owner = before.ownerAt(dot) ?: continue
        val left = 1f - frame.rubbedOut(owner)
        if (left <= 0f) continue
        for (n in neighbours(dot)) {
            if (!before.inBounds(n.x, n.y) || before.ownerAt(n) != owner || before.isCaptured(n.x, n.y)) continue
            val from = geometry.center(n)
            val to = geometry.center(dot)
            drawLine(style.palette.pale(owner), from, Offset(from.x + (to.x - from.x) * left, from.y + (to.y - from.y) * left), width)
        }
    }
}

/** The eight points around [dot]. */
private fun neighbours(dot: Pos): List<Pos> =
    (-1..1).flatMap { dy -> (-1..1).map { dx -> Pos(dot.x + dx, dot.y + dy) } }.filter { it != dot }

/**
 * The bomb's hits (motion.md, Boosters: Bomb): the opponent's dots in its plus, no longer in the
 * position, stay until the blast, swell to 130% and shrink to nothing.
 */
private fun DrawScope.drawBombHits(geometry: BoardGeometry, style: BoardStyle, frame: Frame) {
    val boosting = frame.boosting?.takeIf { it.booster == Booster.BOMB } ?: return
    val size = BombEffect.hit(frame.bt)
    if (size <= 0f) return
    val owner = boosting.by.opponent
    val piece = style.piece(owner).let { it.copy(across = it.across * size) }
    val dot = style.look.dot.px(geometry.spacing, density)
    // In Neon arcade a hit keeps its glow, which swells and shrinks with it.
    val glow = if (style.look.halo.px(geometry.spacing, density) > 0f) 0f else style.look.glow.px(geometry.spacing, density) * size
    for (hit in boosting.removed) {
        drawPieceBand(piece, geometry.center(hit), dot, style.palette.pale(owner), glow)
        drawPiece(piece, geometry.center(hit), dot, style.color(owner))
    }
}

/**
 * The bomb dropping on its centre point, 22 dp, growing from 60% to its size, and from the blast
 * the twelve specks in the opponent's ink flying out, growing and shrinking away.
 */
private fun DrawScope.drawBomb(geometry: BoardGeometry, style: BoardStyle, frame: Frame) {
    val boosting = frame.boosting?.takeIf { it.booster == Booster.BOMB } ?: return
    val center = geometry.center(boosting.point ?: return)
    val u = geometry.unit()
    val drop = BombEffect.drop(frame.bt)
    if (drop > 0f) {
        // The icon's 24-unit box, its body's middle at (11, 14), drawn 22 dp across.
        val k = drop * 22f / 24f * u
        translate(center.x - 11f * k, center.y - 13f * k) {
            scale(k, k, pivot = Offset.Zero) {
                drawPath(BOMB, style.palette.card)
                drawPath(BOMB, style.pencil, style = Stroke(2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
    val q = BombEffect.specks(frame.bt) ?: return
    val ink = style.color(boosting.by.opponent)
    val from = (14f + 30f * Ease.OUT.transform(q)) * u
    val length = 9f * sin(PI * q).toFloat() * u
    if (length <= 0.2f * u) return
    for (k in 0 until BombEffect.SPECKS) {
        val angle = k * PI / 6 + 0.26
        val dx = cos(angle).toFloat()
        val dy = sin(angle).toFloat()
        drawLine(ink, Offset(center.x + dx * from, center.y + dy * from), Offset(center.x + dx * (from + length), center.y + dy * (from + length)), 2.5f * u, cap = StrokeCap.Round)
    }
}

/** The bomb's icon (draw.py `ICONS["bomb"]`), in its 24-unit box. */
private val BOMB: Path by lazy { PathParser().parsePathString(Doodles.BOMB).toPath() }

/** After a Double dot, four short strokes spark out from each dot as it lands, in its side's ink. */
private fun DrawScope.drawSparks(geometry: BoardGeometry, style: BoardStyle, frame: Frame) {
    val moment = frame.moment?.takeIf { it.doubleDots != null } ?: return
    val q = DoubleDotEffect.spark(frame.t) ?: return
    val u = geometry.unit()
    val from = (9f + 8f * q) * u
    val length = 6f * sin(PI * q).toFloat() * u
    if (length <= 0.2f * u) return
    val center = geometry.center(moment.dot)
    for (k in 0 until 4) {
        val angle = PI / 4 + k * PI / 2
        val dx = cos(angle).toFloat()
        val dy = sin(angle).toFloat()
        drawLine(style.color(moment.by), Offset(center.x + dx * from, center.y + dy * from), Offset(center.x + dx * (from + length), center.y + dy * (from + length)), 2f * u, cap = StrokeCap.Round)
    }
}

/**
 * Undo rubbing out the turn (motion.md, Boosters: Undo): each piece it takes back, pale in its
 * dashed Graphite ring as the preview showed it, shrinks to nothing while three short Graphite
 * strokes scrub back and forth across it; the opponent's answer first, then the player's dots.
 * The strokes shrink away at the end.
 */
private fun DrawScope.drawRubbingOut(geometry: BoardGeometry, style: BoardStyle, frame: Frame) {
    val undone = frame.undoing ?: return
    val before = undone.before
    val dot = style.look.dot.px(geometry.spacing, density)
    val u = geometry.unit()
    for (point in undone.answer + undone.own) {
        val owner = before.ownerAt(point) ?: continue
        val c = geometry.center(point)
        val size = 1f - frame.rubbedOut(owner)
        if (size > 0f) {
            val piece = style.piece(owner).let { it.copy(across = it.across * size) }
            drawPiece(piece.copy(across = piece.across * 1.1f), c, dot, style.paper)
            drawPiece(piece, c, dot, style.palette.pale(owner))
            drawDashedRing(c, geometry.spacing * 0.45f * size, style.graphite, 1.5.dp.toPx())
        }
        val from = frame.undoFrom(owner)
        val k = UndoEffect.strokes(frame.bt, from)
        if (k <= 0.01f) continue
        val shift = UndoEffect.scrub(frame.bt, from)
        val angle = -PI / 6
        val ux = cos(angle).toFloat()
        val uy = sin(angle).toFloat()
        val half = 8f * k * u
        listOf(-4.5f, 0f, 4.5f).forEachIndexed { j, across ->
            val along = (shift + if (j == 1) 1.5f else -1f) * u
            val sx = c.x - uy * across * u + ux * along
            val sy = c.y + ux * across * u + uy * along
            drawLine(style.graphite, Offset(sx - ux * half, sy - uy * half), Offset(sx + ux * half, sy + uy * half), 2f * u, cap = StrokeCap.Round)
        }
    }
}
