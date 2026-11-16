package io.github.pavelya.dots.ui.motion

import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/*
 * Every time of docs/design/motion.md, in ms, and where each movement stands at a time t. The
 * drawing code reads only these, so a time changes here and on the motion page together, and
 * MotionTimesTest checks them against the page. Sounds (V-10) and vibrations (V-07) start on the
 * beats named here.
 */

/** Placing a dot and chain lines (motion.md): 0 ms is the moment the dot is placed. */
object Placing {
    /** The dot grows to 115% by this time, then settles to 100% by [SETTLED]. */
    const val GROWN = 120f
    const val SETTLED = 200f

    /** The links from each neighbouring dot of the same side grow between these times. */
    const val LINKS_FROM = 80f
    const val LINKS_TO = 230f

    /** The last-move pinhole moves to the new dot. */
    const val PINHOLE = 200f

    /** The pen stroke under the name of the side to move is drawn in this long. */
    const val PEN_STROKE = 200f

    /** The whole moment. */
    const val LENGTH = LINKS_TO

    /** The new dot's size as a share of its own. */
    fun dotScale(t: Float): Float = when {
        t < GROWN -> 1.15f * phase(t, 0f, GROWN, Ease.OUT)
        else -> 1.15f - 0.15f * phase(t, GROWN, SETTLED, Ease.IN_OUT)
    }

    /** How far each new link has grown, from the neighbour to the new dot. */
    fun links(t: Float): Float = phase(t, LINKS_FROM, LINKS_TO, Ease.OUT)
}

/** Drag's guides (motion.md, "Placing by drag"): 0 ms is the moment the finger goes down. */
object Guides {
    const val DRAWN = 100f

    /** How far the guides reach from the ring to the board's edges, as a share of the way. */
    fun length(t: Float): Float = phase(t, 0f, DRAWN, Ease.OUT)
}

/** The computer's move (motion.md): 0 ms is the moment its dot lands. */
object Announce {
    /** Two ripples start at these times and last [RIPPLE] ms each. */
    val RIPPLES = floatArrayOf(0f, 200f)
    const val RIPPLE = 700f

    /** A ripple's size across and its line, in dp, at its start and end. */
    const val RIPPLE_FROM = 22f
    const val RIPPLE_TO = 96f
    const val LINE_FROM = 3f
    const val LINE_TO = 0.5f

    /** The edge marks grow in, and later shrink back, in this long. */
    const val MARKS = 150f

    /** An edge mark's length and line, in dp. */
    const val MARK_LENGTH = 8f
    const val MARK_LINE = 2.5f

    const val LENGTH = 900f

    /** How far ripple [i] has spread, from 0 to 1; null before it starts and once it is gone. */
    fun ripple(t: Float, i: Int): Float? {
        val start = RIPPLES[i]
        if (t < start || t >= start + RIPPLE) return null
        return phase(t, start, start + RIPPLE, Ease.OUT)
    }

    fun marksIn(t: Float): Float = phase(t, 0f, MARKS, Ease.OUT)

    fun marksOut(t: Float): Float = 1f - phase(t, 0f, MARKS, Ease.IN)
}

/**
 * A capture (motion.md, "Capture"), or with [big] the big capture of 5 dots or more: 0 ms is the
 * moment the closing dot lands.
 */
class Capturing(val big: Boolean) {
    /** The ring is drawn like a pen stroke between these times; it closes at [ringEnd] (medium or strong vibration). */
    val ringStart = 150f
    val ringEnd = if (big) 650f else 550f

    /** The wash and hatching spread from the closing dot. */
    val washStart = if (big) 650f else 500f
    val washEnd = if (big) 950f else 800f

    /** The first cut starts here; each next one [cutGap] ms later, nearest dot first, [CUT] ms each. */
    val cutStart = if (big) 700f else 600f
    val cutGap = if (big) 30f else 40f

    /** "+N" grows in, then flies to the captor's tally. */
    val plusIn = if (big) 850f else 700f
    val plusShown = if (big) 1000f else 850f
    val plusLands = if (big) 1250f else 1100f

    /** The opponent's face changes. */
    val face = if (big) 900f else 800f

    /** The tally, the score and the mission note take the points, then swell for [SWELL] ms. */
    val score = plusLands

    val length = if (big) 1400f else 1250f

    fun ring(t: Float): Float = phase(t, ringStart, ringEnd, Ease.IN_OUT)

    fun wash(t: Float): Float = phase(t, washStart, washEnd, Ease.OUT)

    /** How far the cut across the [i]th captured dot (nearest the closing dot first) is drawn. */
    fun cut(t: Float, i: Int): Float {
        val start = cutStart + i * cutGap
        return phase(t, start, start + CUT, Ease.LINEAR)
    }

    /** The +N's size, from nothing to its size. */
    fun plusGrow(t: Float): Float = phase(t, plusIn, plusShown, Ease.OUT)

    /** How far the +N has flown towards the tally. */
    fun plusFlight(t: Float): Float = phase(t, plusShown, plusLands, Ease.IN)

    /** The score's size as a share of its own: 130% at the top of the swell. */
    fun scoreScale(t: Float): Float = 1f + 0.3f * swell(t, score, score + SWELL)

    /** The mission note's size: 106% at the top. */
    fun noteScale(t: Float): Float = 1f + 0.06f * swell(t, score, score + SWELL)

    /** Big capture: the board's sideways shake, in dp: three swings of 2 dp, each smaller. */
    fun shake(t: Float): Float {
        if (!big || t <= SHAKE_FROM || t >= SHAKE_TO) return 0f
        val p = (t - SHAKE_FROM) / (SHAKE_TO - SHAKE_FROM)
        return 2f * sin(3 * PI * p).toFloat() * (1 - p)
    }

    /** Big capture: how far the burst of strokes has gone; null outside it. */
    fun burst(t: Float): Float? = if (!big || t < BURST_FROM || t >= BURST_TO) null else phase(t, BURST_FROM, BURST_TO, Ease.OUT)

    /** Big capture: the words' size ("Six at once!"): they grow with the +N and shrink away at the end. */
    fun words(t: Float): Float = if (!big) 0f else plusGrow(t) * (1f - phase(t, score, length, Ease.IN))

    companion object {
        /** A capture of this many dots or more is a big capture. */
        const val BIG = 5

        const val CUT = 100f
        const val SWELL = 150f
        const val SHAKE_FROM = 650f
        const val SHAKE_TO = 830f
        const val BURST_FROM = 650f
        const val BURST_TO = 900f

        /** The 14 short strokes of a big capture's burst. */
        const val BURST_STROKES = 14

        fun of(dots: Int): Capturing = Capturing(dots >= BIG)
    }
}

/** The threat pulse (motion.md): 0 ms is [DELAY] after the dot that makes the threat lands. */
object Threat {
    const val DELAY = 200f
    const val DRAW_IN = 250f
    const val PULSE = 1600f
    const val PULSES = 3
    const val STILL = DRAW_IN + PULSES * PULSE

    /** From the dot that made the threat, to stillness. */
    const val LENGTH = DELAY + STILL

    /** How far the rings have drawn in: their size goes from 130% to 100%, their colour from Paper to Highlighter. */
    fun drawIn(t: Float): Float = phase(t, 0f, DRAW_IN, Ease.OUT)

    /** Where a pulse stands, from 0 (rest) to 1 (its top); 0 once still. */
    fun pulse(t: Float): Float {
        if (t <= DRAW_IN || t >= STILL) return 0f
        val inPulse = (t - DRAW_IN) % PULSE
        return swell(inPulse, 0f, PULSE)
    }
}

/** Dots running out (motion.md): from [LOW] dots left, the count turns Highlighter and the last dots show as a row. */
object RunningOut {
    const val LOW = 5
    const val COLOUR = 200f

    /** The row writes in, one small piece every [PIECE_GAP] ms, [PIECE] ms each. */
    const val PIECE_GAP = 50f
    const val PIECE = 150f

    /** Each dot after that: the number swells to 130% and back, and the last small piece shrinks away. */
    const val NUMBER_SWELL = 300f

    /** At [NUDGE_AT] dots left, mission not done: the mission note swings once, 3° each way. */
    const val NUDGE_AT = 3
    const val NUDGE = 400f

    /** At 1 left, "Last dot!" writes in. */
    const val LAST_DOT = 300f

    const val LENGTH = 450f

    /** The [i]th small piece writing in when the count reaches [LOW]. */
    fun pieceIn(t: Float, i: Int): Float = phase(t, i * PIECE_GAP, i * PIECE_GAP + PIECE, Ease.OUT)

    fun pieceOut(t: Float): Float = 1f - phase(t, 0f, PIECE, Ease.IN)

    fun numberScale(t: Float): Float = 1f + 0.3f * swell(t, 0f, NUMBER_SWELL)

    /** The mission note's turn, in degrees. */
    fun nudge(t: Float): Float = if (t <= 0f || t >= NUDGE) 0f else 3f * sin(2 * PI * t / NUDGE).toFloat()

    fun lastDot(t: Float): Float = phase(t, 0f, LAST_DOT, Ease.LINEAR)
}

/**
 * The end of a game (motion.md, "End of game") with [areas] captured areas: 0 ms is the moment the
 * game is over and the last move has played out.
 */
class EndOfGame(val areas: Int) {
    /** Between the starts of two areas lighting up: 150 ms, less when there are many, so all fit in 1500 ms. */
    val gap: Float = if (areas <= 1) GAP else min(GAP, (LIGHT_UNTIL - FIRST - AREA) / (areas - 1))

    fun areaStart(i: Int): Float = FIRST + i * gap

    /** The end of the last area's light; the result note follows [NOTE_AFTER] ms later. */
    val lastArea: Float = if (areas == 0) FIRST else areaStart(areas - 1) + AREA

    val noteStart: Float = lastArea + NOTE_AFTER

    val length: Float = noteStart

    /** The pause icon and the tool bar leaving, from 0 to 1. */
    fun toolsOut(t: Float): Float = phase(t, 0f, TOOLS_OUT, Ease.IN)

    /** Area [i] lighting up: 0, up to 1, back to 0. */
    fun glow(t: Float, i: Int): Float = swell(t, areaStart(i), areaStart(i) + AREA)

    companion object {
        const val TOOLS_OUT = 250f
        const val FIRST = 300f
        const val AREA = 300f
        const val GAP = 150f
        const val LIGHT_UNTIL = 1500f
        const val NOTE_AFTER = 200f
    }
}

/**
 * The result note (motion.md, "End of game" and "Star reveal"): 0 ms is the moment it starts to
 * slide up. It settles at [SETTLED], which is 0 ms of the star reveal.
 */
object ResultReveal {
    const val SLIDE = 300f

    /** It tips to −2.5° and settles at its −1.5° slant in the last 100 ms of the slide. */
    const val TIP_FROM = 200f
    const val SETTLED = SLIDE

    const val HEADLINE = 300f

    /** Each star: earned ones are stamped, the others drawn in outline, one every [STAR_GAP] ms after the headline. */
    const val STAR_GAP = 300f
    const val STAR = 200f

    const val LENGTH = SETTLED + 1100f

    /** How far the note has come up, from 0 (below the screen) to 1. */
    fun slide(t: Float): Float = phase(t, 0f, SLIDE, Ease.OUT)

    /** The note's turn beyond its slant, in degrees: −1 while it slides, back to 0 as it settles. */
    fun tip(t: Float): Float = -1f * (1f - phase(t, TIP_FROM, SETTLED, Ease.IN_OUT))

    /** How much of the headline is written, left to right. */
    fun headline(t: Float): Float = phase(t, SETTLED, SETTLED + HEADLINE, Ease.LINEAR)

    fun starStart(i: Int): Float = SETTLED + HEADLINE + i * STAR_GAP

    /** Star [i]: from 0 (not there) to 1 (in place). Stamped stars ease in; outlines are drawn evenly. */
    fun star(t: Float, i: Int, earned: Boolean): Float =
        phase(t, starStart(i), starStart(i) + STAR, if (earned) Ease.IN else Ease.LINEAR)
}

/**
 * The level unlock on the map (motion.md, "Level unlock"): 0 ms is the moment the map shows after
 * Next. When the stars open a world, its page turns in [PAGE_TURN] ms before the path inks in, and
 * everything after moves that much later.
 */
class Unlocking(val worldOpens: Boolean) {
    private val shift = if (worldOpens) PAGE_TURN else 0f

    /** Star [i] of the new ones flies from the result note to the level's stop. */
    fun star(t: Float, i: Int): Float = phase(t, i * STAR_GAP, i * STAR_GAP + STAR, Ease.IN_OUT)

    /** The stop fills with a disc growing from its centre. */
    fun fill(t: Float): Float = phase(t, FILL_FROM, FILL_TO, Ease.OUT)

    /** The stop's red "next" rings, shrinking away in the first 150 ms of the fill. */
    fun rings(t: Float): Float = 1f - phase(t, FILL_FROM, FILL_FROM + 150f, Ease.IN)

    /** How far right of its place the new world's page still is, as a share of the width: from 1 to 0 while it turns in. */
    fun page(t: Float): Float =
        if (!worldOpens || t < FILL_TO || t >= FILL_TO + PAGE_TURN) 0f else 1f - phase(t, FILL_TO, FILL_TO + PAGE_TURN, Ease.IN_OUT)

    /** The path to the next level inking in. */
    fun path(t: Float): Float = phase(t, PATH_FROM + shift, PATH_TO + shift, Ease.IN_OUT)

    /** The next level circled twice in red: 0 to 2 loops. */
    fun circle(t: Float): Float = 2f * phase(t, CIRCLE_FROM + shift, CIRCLE_TO + shift, Ease.LINEAR)

    /** "next!" writing in beside it. */
    fun next(t: Float): Float = phase(t, NEXT_FROM + shift, CIRCLE_TO + shift, Ease.LINEAR)

    /** The next level's card comes up. */
    val card: Float = CARD + shift

    companion object {
        const val STAR = 400f
        const val STAR_GAP = 100f
        const val FILL_FROM = 500f
        const val FILL_TO = 800f
        const val PATH_FROM = 800f
        const val PATH_TO = 1200f
        const val CIRCLE_FROM = 1200f
        const val CIRCLE_TO = 1600f
        const val NEXT_FROM = 1300f
        const val CARD = 1800f
        const val PAGE_TURN = 600f

        /** Only new stars fly, when the level was passed before: the card comes up once they land. */
        fun starsOnly(stars: Int): Float = max(0, stars - 1) * STAR_GAP + STAR + 300f
    }
}
