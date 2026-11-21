package io.github.pavelya.dots.ui.motion

import dots.engine.Booster
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

/**
 * Star chart's capture (motion.md, "Capture in Star chart"): the line is traced at an even speed,
 * and each star swells as the line reaches it. 0 ms is the moment the closing star lands.
 */
object Twinkle {
    /** A star swells to [PEAK] and back in this long, from the moment the line reaches it. */
    const val LENGTH = 120f
    const val PEAK = 1.25f

    /** How far the line has been traced round the ring: at an even speed, as if ruled. */
    fun traced(capture: Capturing, t: Float): Float = phase(t, capture.ringStart, capture.ringEnd, Ease.LINEAR)

    /**
     * A star's size as a share of its own, for a star the line reaches at [reached] of the way
     * round (the closing star at 1, when the line comes back to it).
     */
    fun size(capture: Capturing, t: Float, reached: Float): Float {
        val start = capture.ringStart + reached * (capture.ringEnd - capture.ringStart)
        return 1f + (PEAK - 1f) * swell(t, start, start + LENGTH)
    }

    /** How far the haze and its hatching have blended in from the paper, over the whole area at once. */
    fun haze(capture: Capturing, t: Float): Float = capture.wash(t)
}

/**
 * Neon arcade's light (motion.md, "Neon arcade"): a placed piece lights up as it settles; a ring is
 * drawn unlit and lights up as it closes, with one flicker and a burst of sparks.
 */
object NeonLight {
    /** A new piece's glow grows from nothing to its band between these times after it is placed. */
    const val GLOW_FROM = 120f
    const val GLOW_TO = 200f

    /** The lit tube goes dark again this long after it lights, for [FLICKER] ms. */
    const val FLICKER_AFTER = 40f
    const val FLICKER = 40f

    /** The sparks fly for this long from the moment the tube lights. */
    const val SPARKS = 250f

    /** Up to this many sparks, spread over [SPARK_SPREAD] degrees away from the area. */
    const val SPARK_COUNT = 10
    const val SPARK_SPREAD = 160f

    /** A spark flies from this far from the closing piece's centre to [SPARK_TO], and is at most [SPARK_LONG] long, in dp at 24 dp spacing. */
    const val SPARK_FROM = 9f
    const val SPARK_TO = 27f
    const val SPARK_LONG = 6f

    /** A spark's tube and core, in dp at 24 dp spacing. */
    const val SPARK_LINE = 2.2f
    const val SPARK_CORE = 0.8f

    /** How much of its glow a piece placed [t] ms ago shows. */
    fun glow(t: Float): Float = phase(t, GLOW_FROM, GLOW_TO, Ease.OUT)

    /** Whether the ring of [capture] is lit at [t]: once it has closed, but for the flicker. */
    fun lit(capture: Capturing, t: Float): Boolean {
        val close = capture.ringEnd
        return t >= close && !(t >= close + FLICKER_AFTER && t < close + FLICKER_AFTER + FLICKER)
    }

    /** How far the sparks have flown, from 0 to 1; null outside their flight. */
    fun sparks(capture: Capturing, t: Float): Float? {
        val close = capture.ringEnd
        return if (t < close || t >= close + SPARKS) null else (t - close) / SPARKS
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
 * The booster effects (motion.md, "Boosters"): 0 ms is the moment the player taps Confirm. In every
 * case the booster's count drops at once and swells to 135% and back in [COUNT_SWELL] ms.
 */
object BoosterTimes {
    const val COUNT_SWELL = 200f

    /** The used booster's count, as a share of its size. */
    fun countScale(t: Float): Float = 1f + 0.35f * swell(t, 0f, COUNT_SWELL)

    /** How long [booster]'s effect plays. Double dot's dots spark when they land, as part of their own moments. */
    fun length(booster: Booster): Float = when (booster) {
        Booster.WALL -> WallEffect.LENGTH
        Booster.HINT -> HintEffect.LENGTH
        Booster.BOMB -> BombEffect.LENGTH
        Booster.DOUBLE_DOT -> DoubleDotEffect.CHIP_IN
        Booster.UNDO -> UndoEffect.LENGTH
    }

    /** A tap finishes the Bomb's and Undo's effects; during the others it only counts (motion.md, timing table). */
    fun tapFinishes(booster: Booster): Boolean = booster == Booster.BOMB || booster == Booster.UNDO
}

/** Wall: a square drawn round the point in one Pencil stroke, then five hatch lines, one after another. */
object WallEffect {
    const val SQUARE = 200f
    const val LENGTH = 400f

    /** How much of the square's outline is drawn. */
    fun square(t: Float): Float = phase(t, 0f, SQUARE, Ease.IN_OUT)

    /** How far the hatching has got: line k of 5 is drawn while this goes from k/5 to (k+1)/5. */
    fun hatch(t: Float): Float = phase(t, SQUARE, LENGTH, Ease.LINEAR)
}

/** Hint: the point circled twice in the player's ink, 250 ms per loop; the circles shrink away when the dot lands. */
object HintEffect {
    const val LOOP = 250f
    const val LENGTH = 2 * LOOP
    const val SHRINK = 150f

    /** Loops drawn, from 0 to 2. */
    fun loops(t: Float): Float = 2f * phase(t, 0f, LENGTH, Ease.LINEAR)

    /** The circles' size after the player's dot lands, from 1 to 0. */
    fun shrink(t: Float): Float = 1f - phase(t, 0f, SHRINK, Ease.IN)
}

/** Bomb: it drops on the centre point, and at [BLAST] the opponent's dots in its plus swell and shrink away. */
object BombEffect {
    const val BLAST = 150f
    const val SHAKE_TO = 300f
    const val SWELLED = 220f
    const val GONE = 350f
    const val LENGTH = 450f

    /** Twelve specks fly out from the centre. */
    const val SPECKS = 12

    /** The bomb's size before the blast: from 60% to 100%; 0 once it has gone off. */
    fun drop(t: Float): Float = if (t >= BLAST) 0f else 0.6f + 0.4f * phase(t, 0f, BLAST, Ease.OUT)

    /** A hit dot's size: whole until the blast, 130% at [SWELLED], nothing at [GONE]. */
    fun hit(t: Float): Float = when {
        t < BLAST -> 1f
        t < SWELLED -> 1f + 0.3f * phase(t, BLAST, SWELLED, Ease.OUT)
        else -> 1.3f * (1f - phase(t, SWELLED, GONE, Ease.IN))
    }

    /** The board's sideways shake, in dp: two swings of 2 dp, each smaller. */
    fun shake(t: Float): Float {
        if (t <= BLAST || t >= SHAKE_TO) return 0f
        val p = (t - BLAST) / (SHAKE_TO - BLAST)
        return 2f * sin(2 * 2 * PI * p).toFloat() * (1 - p)
    }

    /** How far the specks have flown, from 0 to 1; null outside them. */
    fun specks(t: Float): Float? = if (t < BLAST || t >= LENGTH) null else (t - BLAST) / (LENGTH - BLAST)
}

/** Double dot: the "×2" chip grows in; each dot then sparks as it lands, and the chip counts down or leaves. */
object DoubleDotEffect {
    const val CHIP_IN = 300f
    const val SPARK = 250f
    const val CHIP_SWELL = 200f
    const val CHIP_OUT = 150f

    fun chipIn(t: Float): Float = phase(t, 0f, CHIP_IN, Ease.OUT)

    /** After the first dot: the chip, now "×1", swells to 125% and back. */
    fun chipSwell(t: Float): Float = 1f + 0.25f * swell(t, 0f, CHIP_SWELL)

    /** After the second dot: the chip shrinks away. */
    fun chipOut(t: Float): Float = 1f - phase(t, 0f, CHIP_OUT, Ease.IN)

    /** How far the four sparks round a landing dot have flown, from 0 to 1; null outside them. */
    fun spark(t: Float): Float? = if (t < 0f || t >= SPARK) null else t / SPARK
}

/**
 * Undo: the turn is rubbed out newest first, the opponent's answer from [ANSWER], the player's dots
 * from [OWN], each in [RUB] ms, while three Graphite strokes scrub across them. A booster used in
 * that turn flies back to its button from [FLIGHT_FROM] to [FLIGHT_TO], and its count swells; dots
 * a bomb took in that turn grow back in while the player's dots are rubbed out. At
 * [SETTLED] the dots left, the pinhole and the face are as after the undo; the strokes shrink away.
 */
object UndoEffect {
    const val ANSWER = 0f
    const val OWN = 200f
    const val RUB = 250f
    const val SETTLED = OWN + RUB
    const val LENGTH = 600f
    const val FLIGHT_FROM = 200f
    const val FLIGHT_TO = 400f

    /** How far the pieces of the part starting at [from] are rubbed out: their size and their capture go as this reaches 1. */
    fun rubOut(t: Float, from: Float): Float = phase(t, from, from + RUB, Ease.IN)

    /** The scrub strokes over that part's pieces, as a share of their length. */
    fun strokes(t: Float, from: Float): Float = if (t < from) 0f else phase(t, from, from + 50f) * (1f - phase(t, SETTLED, LENGTH, Ease.OUT))

    /** How far the strokes have slid along their slant, in dp: three times back and forth. */
    fun scrub(t: Float, from: Float): Float = 3f * sin(2 * PI * 3 * phase(t, from, from + RUB)).toFloat()

    /** A dot a bomb took in the turn, growing back in: its size from 0 to 1. */
    fun growBack(t: Float): Float = phase(t, OWN, SETTLED, Ease.OUT)

    /** The booster given back, on its way to its button. */
    fun flight(t: Float): Float = phase(t, FLIGHT_FROM, FLIGHT_TO, Ease.IN_OUT)

    /** The given-back booster's count, once it has landed: 135% at the top. */
    fun countScale(t: Float): Float = 1f + 0.35f * swell(t, FLIGHT_TO, FLIGHT_TO + BoosterTimes.COUNT_SWELL)
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
    fun tip(t: Float): Float = phase(t, TIP_FROM, SETTLED, Ease.IN_OUT) - 1f

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

/**
 * The level opening (motion.md, "Level opening, notes and cards"): 0 ms is the moment the board
 * shows. The mission note sits big over the board until [HOLD], then shrinks into its corner by
 * [LENGTH]. A tap ends it at once.
 */
object OpeningNote {
    const val HOLD = 1200f
    const val SHRINK = 300f
    const val LENGTH = HOLD + SHRINK

    /** How far the big note has shrunk into the corner, from 0 (big) to 1 (the corner note's size and place). */
    fun shrink(t: Float): Float = phase(t, HOLD, LENGTH, Ease.IN_OUT)

    /** In the last half of the shrink the corner note shows through the big one: from 0 to 1. */
    fun handOver(t: Float): Float = phase(t, HOLD + SHRINK / 2, LENGTH, Ease.LINEAR)
}

/**
 * Notes and cards over the map or the board (motion.md, "Level opening, notes and cards"): they
 * slide up from below in [IN] ms while the background dims, and leave by sliding down in [OUT] ms.
 * 0 ms is the moment the note is opened, or closed.
 */
object NoteSlide {
    const val IN = 300f
    const val OUT = 200f

    /** A note coming up: from 0 (below the screen, the background clear) to 1 (in place, dimmed). */
    fun coming(t: Float): Float = phase(t, 0f, IN, Ease.OUT)

    /** A note leaving: from 1 (in place) to 0 (below the screen). */
    fun leaving(t: Float): Float = 1f - phase(t, 0f, OUT, Ease.IN)
}
