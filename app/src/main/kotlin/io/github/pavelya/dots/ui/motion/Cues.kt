package io.github.pavelya.dots.ui.motion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import dots.engine.Booster
import dots.engine.GameState
import dots.engine.Player
import io.github.pavelya.dots.audio.Effect
import io.github.pavelya.dots.audio.LocalAudio
import io.github.pavelya.dots.audio.Sounding
import io.github.pavelya.dots.game.BoardMoment
import io.github.pavelya.dots.game.BoosterMoment
import io.github.pavelya.dots.game.GameUiState
import io.github.pavelya.dots.game.OpponentLook
import io.github.pavelya.dots.haptics.Buzz
import io.github.pavelya.dots.haptics.LocalHaptics
import io.github.pavelya.dots.ui.theme.GameTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.pow

/** An [effect] to start [at] ms into a moment, played at [rate] (1 is as recorded). */
data class Cue(val effect: Effect, val at: Float, val rate: Float = 1f)

/**
 * Which sound starts when in each moment of docs/design/motion.md: the page "Check against the
 * motion storyboard" of docs/design/sound.md, timed from the beats in Times.kt. A moment missing
 * here is one of sound.md's deliberate silences.
 */
object Cues {
    /** The dot's takes play in turn, each at a slightly different speed of these, 95% to 105% (sound.md, rule 7). */
    private val PLACE_SPEEDS = floatArrayOf(1f, 0.97f, 1.04f, 0.95f, 1.02f, 1.05f, 0.98f)

    /** Blue's dots, the computer's included, play the dot at this speed, a little lower (rule 10). */
    const val BLUE = 0.85f

    /** A big capture's ring, slower and lower. */
    const val BIG_RING = 0.8f

    /** Hint: the point circled twice, lighter and higher, [HINT_GAP] ms apart. */
    const val HINT = 1.6f
    const val HINT_GAP = 250f

    /** Undo: one slow, low scrub. */
    const val UNDO = 0.75f

    /** Each earned star: star 1 as recorded, star 2 one note up, star 3 two notes up. */
    val STARS = floatArrayOf(1f, 1.12f, 1.5f)

    /** A new world's sound starts this long into the page turn. */
    const val WORLD_OPEN_IN_PAGE = 300f

    /** The daily streak's sound follows the win's, once it has played. */
    const val STREAK_AFTER = 1_500f

    /** The dot's sound for moment [id], placed by [by]. */
    fun place(id: Int, by: Player): Cue {
        val speed = PLACE_SPEEDS[id % PLACE_SPEEDS.size] * if (by == Player.BLUE) BLUE else 1f
        return Cue(Effect.PLACE[id % Effect.PLACE.size], 0f, speed)
    }

    /**
     * The sounds of a dot that has just landed ([moment]), with [state] the board after it. Against
     * the computer [human] is the player; between two people [human] is null. [dotsLowHeard] is true
     * once this game has warned that the dots run low.
     */
    fun moment(moment: BoardMoment, state: GameState, human: Player?, dotsLowHeard: Boolean): List<Cue> = buildList {
        add(place(moment.id, moment.by))
        moment.capturing?.let { capture ->
            add(Cue(Effect.RING_DRAW, capture.ringStart, if (capture.big) BIG_RING else 1f))
            val closing = when {
                capture.big -> Effect.CAPTURE_BIG
                human != null && moment.by != human -> Effect.CAPTURED
                else -> Effect.RING_CLOSE
            }
            add(Cue(closing, capture.ringEnd))
        }
        // Only danger to the player's own dots: against the computer the human's, between two people the side to move next.
        val own = human ?: moment.by.opponent
        if (moment.freshThreats.any { state.ownerAt(it.group.first()) == own }) add(Cue(Effect.THREAT, Threat.DELAY))
        val counted = human ?: Player.RED
        if (!dotsLowHeard && moment.by == counted && state.dotsLeft(counted) == RunningOut.LOW) add(Cue(Effect.DOTS_LOW, 0f))
        if (human != null && moment.by == human && OpponentLook.bossTurn(state, human.opponent)) add(Cue(Effect.BOSS_TURN, Placing.SETTLED))
    }

    /** The sounds of a booster the player has just used, from the tap on Confirm. */
    fun booster(used: BoosterMoment): List<Cue> = when (used.booster) {
        Booster.WALL -> listOf(Cue(Effect.RING_DRAW, 0f))
        Booster.HINT -> listOf(Cue(Effect.RING_DRAW, 0f, HINT), Cue(Effect.RING_DRAW, HINT_GAP, HINT))
        Booster.BOMB -> listOf(Cue(Effect.BOMB, 0f))
        Booster.DOUBLE_DOT -> listOf(Cue(Effect.DOUBLE_DOT, 0f))
        Booster.UNDO -> listOf(Cue(Effect.RING_DRAW, 0f, UNDO))
    }

    /** The end of a game: each captured area plays one note higher as it lights up. */
    fun ending(times: EndOfGame): List<Cue> = List(times.areas) { i -> Cue(Effect.AREA_LIGHT, times.areaStart(i), areaRate(i)) }

    /**
     * Area [i]'s speed: the notes D, E, G and A, then on up an octave. The phone plays at most
     * twice as fast, so areas after the fifth stay on the D an octave up.
     */
    fun areaRate(i: Int): Float {
        val semitones = AREA_STEPS[i % AREA_STEPS.size] + 12 * (i / AREA_STEPS.size)
        return 2f.pow(semitones / 12f).coerceAtMost(2f)
    }

    private val AREA_STEPS = intArrayOf(0, 2, 5, 7)

    /**
     * The result note's sounds, from the moment it starts to slide up: the [headline]'s as it
     * writes in, then a stamp for each of the [stars] earned as it lands, or the [streak] going up.
     * With animations [still], the stars are not stamped one by one, and make no sound.
     */
    fun result(headline: Effect, stars: Int, streak: Boolean, still: Boolean): List<Cue> = buildList {
        add(Cue(headline, ResultReveal.SETTLED))
        if (!still) for (i in 0 until stars.coerceAtMost(STARS.size)) add(Cue(Effect.STAR, ResultReveal.starStart(i) + ResultReveal.STAR, STARS[i]))
        if (streak) add(Cue(Effect.REWARD, ResultReveal.SETTLED + STREAK_AFTER))
    }

    /** The outcome's sound: a level or the daily puzzle passed, or a game won, between two people by either side. */
    fun headline(ui: GameUiState, human: Player): Effect {
        val passed = ui.levelEnd?.result?.isPassed ?: ui.dailyEnd?.passed
        val winner = ui.state.winner
        return when {
            passed != null -> if (passed) Effect.WIN else Effect.LOSE
            winner == null -> Effect.DRAW
            !ui.vsComputer || winner == human -> Effect.WIN
            else -> Effect.LOSE
        }
    }

    /** The map after Next: a new world opens with its sound as its page turns; a level unlock is silent. */
    fun unlock(worldOpens: Boolean, still: Boolean): List<Cue> =
        if (!worldOpens) emptyList() else listOf(Cue(Effect.WORLD_OPEN, if (still) 0f else Unlocking.FILL_TO + WORLD_OPEN_IN_PAGE))
}

/** A vibration, [buzz], to start [at] ms into a moment. */
data class Beat(val buzz: Buzz, val at: Float)

/**
 * Which vibration starts when (docs/design/motion.md, rule 6), on the same beats as the sounds
 * (sound.md, rule 3): light as a dot lands, medium as a ring closes, strong as a big capture's
 * ring closes and as a bomb goes off, light as each earned star is stamped. Every dot and every
 * capture is felt, the computer's too, as each is heard.
 */
object Beats {
    /** The vibrations of a dot that has just landed. */
    fun moment(moment: BoardMoment): List<Beat> = buildList {
        add(Beat(Buzz.LIGHT, 0f))
        moment.capturing?.let { add(Beat(if (it.big) Buzz.STRONG else Buzz.MEDIUM, it.ringEnd)) }
    }

    /** The vibrations of a booster the player has just used, from the tap on Confirm: only the bomb's blast. */
    fun booster(used: BoosterMoment): List<Beat> =
        if (used.booster == Booster.BOMB) listOf(Beat(Buzz.STRONG, BombEffect.BLAST)) else emptyList()

    /**
     * The result note's vibrations, from the moment it starts to slide up: one as each of the
     * [stars] earned lands. With animations [still], the stars are not stamped, and nothing is felt.
     */
    fun result(stars: Int, still: Boolean): List<Beat> =
        if (still) emptyList() else List(stars.coerceIn(0, Cues.STARS.size)) { i -> Beat(Buzz.LIGHT, ResultReveal.starStart(i) + ResultReveal.STAR) }
}

/**
 * Plays [cues] and [beats] once for each [key], timed from now, if [play] says so when it starts;
 * nothing for a null [key]. Each sound plays the file of the theme the screen is drawn in
 * ([GameTheme.sound]): the theme's own board sounds, Notebook's for the rest. Each beat vibrates
 * the phone ([LocalHaptics]). A tap that finishes [clock] (motion.md, rule 1) stops the sounds and
 * vibrations still to come and fades out the sounds playing in 50 ms (sound.md, rule 8).
 */
@Composable
fun PlayCues(key: Any?, clock: Timeline?, play: () -> Boolean = { true }, beats: () -> List<Beat> = { emptyList() }, cues: () -> List<Cue>) {
    val audio = LocalAudio.current
    val haptics = LocalHaptics.current
    val theme by rememberUpdatedState(GameTheme.current)
    val latest by rememberUpdatedState(cues)
    val latestBeats by rememberUpdatedState(beats)
    val allowed by rememberUpdatedState(play)
    LaunchedEffect(key) {
        if (key == null || !allowed()) return@LaunchedEffect
        val sounding = ArrayList<Sounding>()
        // Sounds and vibrations in one list, so those on the same millisecond start together.
        val sounds: List<Pair<Float, () -> Unit>> = latest().map { cue -> cue.at to { sounding += audio.play(cue.effect, cue.rate, theme.sound(cue.effect)) } }
        val felt: List<Pair<Float, () -> Unit>> = latestBeats().map { beat -> beat.at to { haptics.buzz(beat.buzz) } }
        val steps = (sounds + felt).sortedBy { it.first }
        if (steps.isEmpty()) return@LaunchedEffect
        val finishes = clock?.finishes
        val schedule = launch {
            var t = 0f
            for ((at, step) in steps) {
                if (at > t) delay((at - t).toLong())
                t = at
                step()
            }
        }
        if (clock != null) {
            snapshotFlow { clock.finishes }.first { it != finishes }
            schedule.cancel()
            sounding.forEach(Sounding::fadeOut)
        }
    }
}
