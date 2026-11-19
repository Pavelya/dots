package io.github.pavelya.dots.ui.motion

import dots.engine.Booster
import dots.engine.Capture
import dots.engine.GameState
import dots.engine.Player
import dots.engine.Pos
import dots.engine.TurnRule
import io.github.pavelya.dots.audio.Effect
import io.github.pavelya.dots.game.BoardMoment
import io.github.pavelya.dots.game.BoosterMoment
import io.github.pavelya.dots.game.ThreatMark
import io.github.pavelya.dots.ui.theme.GameTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Star chart and Neon arcade play their own board sounds and Notebook's for every other effect, as
 * docs/design/sound.md lists them ("Board sounds of the other themes"). Paths are from the app
 * module, where Gradle runs the tests.
 */
class ThemeSoundsTest {
    private val page = File("../docs/design/sound.md").readText()

    /** Each cosmetic theme, its heading on the page, and its key in the file names. */
    private val themes = listOf(
        Triple(GameTheme.STAR_CHART, "### Star chart", "star_chart"),
        Triple(GameTheme.NEON_ARCADE, "### Neon arcade", "neon_arcade"),
    )

    /** The rows of the first table after [heading], each row's cells, without the header. */
    private fun table(heading: String): List<List<String>> = page.substringAfter("\n$heading\n")
        .lineSequence()
        .dropWhile { !it.startsWith("|") }
        .takeWhile { it.startsWith("|") }
        .drop(2)
        .map { row -> row.trim('|').split("|").map { it.trim() } }
        .toList()

    /** The file names in a cell: "`sfx_place_1`, `_2`, `_3`" holds sfx_place_1, sfx_place_2 and sfx_place_3. */
    private fun names(cell: String): List<String> {
        val tokens = Regex("`([a-z0-9_*]+)`").findAll(cell).map { it.groupValues[1] }.toList()
        val first = tokens.first()
        return tokens.map { if (it.startsWith("_")) first.substringBeforeLast('_') + it else it }
    }

    /** The effects a "Replaces" cell names: `sfx_place_*` is the dot's three takes. */
    private fun effects(cell: String): List<Effect> = names(cell).flatMap { name ->
        Effect.entries.filter { if (name.endsWith("*")) it.file.startsWith(name.dropLast(1)) else it.file == name }
    }

    @Test
    fun `a theme replaces the board's sounds and keeps Notebook's for the rest`() {
        val rows = table("### What a theme replaces")
        val replaced = rows.filter { it[1] == "the theme's own" }.flatMap { names(it[0]) }
        val kept = rows.filter { it[1] == "Notebook's" }.flatMap { names(it[0]) }
        assertEquals(replaced.sorted(), Effect.BOARD.map { it.file }.sorted())
        assertEquals(Effect.entries.map { it.file }.sorted(), (replaced + kept).sorted())
        assertTrue("Notebook plays the game's own effects", GameTheme.NOTEBOOK.sounds.isEmpty())
    }

    @Test
    fun `each theme has the files of its table on sound md, at Notebook's volumes`() {
        for ((theme, heading, _) in themes) {
            val listed = table(heading).flatMap { row ->
                val files = names(row[0])
                val replaces = effects(row[1])
                assertEquals("$heading: ${row[0]}", files.size, replaces.size)
                replaces.forEach { assertEquals("$heading: ${it.file}'s volume", if (it.share < 1f) "half" else "full", row[4]) }
                replaces.zip(files)
            }.toMap()
            assertEquals(heading, listed, theme.sounds)
        }
    }

    @Test
    fun `every theme sound is in the app`() {
        for ((theme, heading, _) in themes) {
            val missing = theme.sounds.values.filterNot { File("src/main/assets/audio/$it.ogg").isFile }
            assertEquals("$heading: files not in app/src/main/assets/audio", emptyList<String>(), missing)
        }
    }

    // The moments of sound.md's "Check against the motion storyboard", as the screens cue them.

    private val board = GameState.start(width = 6, height = 6, dotsPerPlayer = 20)

    private fun moment(by: Player, captured: Int = 0, threats: Set<ThreatMark> = emptySet()) = BoardMoment(
        id = 1,
        dot = Pos(3, 3),
        by = by,
        announced = by == Player.BLUE,
        captures = if (captured > 0) listOf(Capture(by, List(captured) { Pos(it, 0) }, List(captured) { Pos(it, 0) }, closing = Pos(0, 1))) else emptyList(),
        freshThreats = threats,
    )

    private fun moments(): Map<String, List<Cue>> {
        val threatened = board.play(Pos(1, 1)).state.play(Pos(4, 4)).state
        val low = GameState.start(width = 6, height = 6, dotsPerPlayer = 6).play(Pos(0, 0)).state.play(Pos(5, 5)).state
        val bossStart = GameState.start(width = 6, height = 6, dotsPerPlayer = 10, turnRules = mapOf(Player.BLUE to TurnRule.DoubleMove(3)))
        val bossTurn = listOf(Pos(0, 0), Pos(5, 5), Pos(0, 1), Pos(5, 4), Pos(0, 2)).fold(bossStart) { s, p -> s.play(p).state }
        return linkedMapOf(
            "the dot's three takes" to (0..2).map { Cues.place(it, Player.RED) },
            "placing a dot" to Cues.moment(moment(Player.RED), board, Player.RED, dotsLowHeard = false),
            "the computer's move" to Cues.moment(moment(Player.BLUE), board, Player.RED, dotsLowHeard = false),
            "capture" to Cues.moment(moment(Player.RED, captured = 2), board, Player.RED, dotsLowHeard = false),
            "the computer's capture" to Cues.moment(moment(Player.BLUE, captured = 1), board, Player.RED, dotsLowHeard = false),
            "big capture" to Cues.moment(moment(Player.RED, captured = Capturing.BIG), board, Player.RED, dotsLowHeard = false),
            "threat pulse" to Cues.moment(moment(Player.BLUE, threats = setOf(ThreatMark(setOf(Pos(1, 1)), 1))), threatened, Player.RED, dotsLowHeard = false),
            "dots running out" to Cues.moment(moment(Player.RED), low, Player.RED, dotsLowHeard = false),
            "the boss's turn" to Cues.moment(moment(Player.RED), bossTurn, Player.RED, dotsLowHeard = false),
        ) + Booster.entries.associate { "booster ${it.name}" to Cues.booster(BoosterMoment(1, it, Player.RED)) } + linkedMapOf(
            "end of game" to Cues.ending(EndOfGame(4)),
            "star reveal, won" to Cues.result(Effect.WIN, stars = 3, streak = true, still = false),
            "star reveal, lost" to Cues.result(Effect.LOSE, stars = 0, streak = false, still = false),
            "star reveal, draw" to Cues.result(Effect.DRAW, stars = 1, streak = false, still = false),
            "a world opens" to Cues.unlock(worldOpens = true, still = false),
        )
    }

    @Test
    fun `each theme plays its own sound at each board moment and Notebook's otherwise`() {
        val all = moments()
        // Every effect is cued somewhere, so every replacement is reached.
        assertEquals(Effect.entries.toSet(), all.values.flatten().map { it.effect }.toSet())
        for ((name, cues) in all) {
            assertEquals("Notebook, $name", cues.map { it.effect.file }, cues.map { GameTheme.NOTEBOOK.sound(it.effect) })
            for ((theme, _, key) in themes) {
                val expected = cues.map { if (it.effect in Effect.BOARD) "sfx_${key}_${it.effect.file.removePrefix("sfx_")}" else it.effect.file }
                assertEquals("${theme.name}, $name", expected, cues.map { theme.sound(it.effect) })
            }
        }
    }

    @Test
    fun `a capture in each theme, sound by sound`() {
        val capture = moments().getValue("capture")
        assertEquals(
            listOf("sfx_star_chart_place_2", "sfx_star_chart_ring_draw", "sfx_star_chart_ring_close"),
            capture.map { GameTheme.STAR_CHART.sound(it.effect) },
        )
        assertEquals(
            listOf("sfx_neon_arcade_place_2", "sfx_neon_arcade_ring_draw", "sfx_neon_arcade_ring_close"),
            capture.map { GameTheme.NEON_ARCADE.sound(it.effect) },
        )
        // The same beats as Notebook's: only the files change.
        assertEquals(listOf(0f, 150f, 550f), capture.map { it.at })
    }
}
