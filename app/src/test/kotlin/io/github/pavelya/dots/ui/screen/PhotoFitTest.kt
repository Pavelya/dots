package io.github.pavelya.dots.ui.screen

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import dots.campaign.Campaign
import dots.campaign.Level
import dots.campaign.Tutorial
import dots.engine.Booster
import dots.engine.MoveResult
import dots.engine.Player
import io.github.pavelya.dots.R
import io.github.pavelya.dots.game.GameUiState
import io.github.pavelya.dots.ui.board.BoardInput
import io.github.pavelya.dots.ui.theme.DotsTheme
import io.github.pavelya.dots.ui.theme.GameTheme
import io.github.pavelya.dots.ui.theme.Palette
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The opponent's face when the full photo doesn't fit above the board (game-screens.md, "When the
 * photo doesn't fit"; the canvas frames `fit-*`). Like the frames, each screen is the app's height
 * between the status bar and the navigation bar, so the game fills the test's screen with no bars
 * of its own. Each level is shown as it opens, in Notebook, light and dark. Reference images in
 * app/src/test/screenshots; they are recorded in CI (CLAUDE.md, "Recording screenshots").
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PhotoFitTest {
    @get:Rule
    val compose = createComposeRule()

    private val human = Player.RED

    /** The game of [level] as it opens, as the view model makes it. */
    private fun opening(level: Level): GameUiState {
        val state = level.newGame()
        val usable = if (state.toMove == human) Booster.entries.filter { state.canUse(it) }.toSet() else emptySet()
        return CanvasGame.ui(MoveResult(state, emptyList()), human, lastMove = null).copy(
            strength = level.strength,
            level = level,
            mission = level.status(state),
            usable = usable,
            guided = Tutorial.lesson(level) != null,
            guide = Tutorial.step(level, state),
        )
    }

    /** The game screen of [level] as it opens, laid out as GameScreen lays it. */
    @Composable
    private fun Opening(level: Level) {
        val ui = opening(level)
        GameBoard(
            ui = ui,
            human = human,
            campaign = CampaignParts(
                title = stringResource(R.string.level_number, level.number),
                mission = missionNote(level, ui.mission!!),
                boosters = { BoosterBar(boosterTools(ui, human), Palette.current) {} },
                world = level.world,
            ),
            input = BoardInput.NONE,
            onUndo = {},
            onPause = {},
            guided = ui.guided,
            guide = ui.guide,
        )
    }

    private fun level(code: String): Level = code.split("-").map(String::toInt).let { (world, number) -> Campaign.level(world, number) }

    /** The photo's size in dp, which a turn of the photo does not change. */
    private fun photoSize(): Int = (compose.onNodeWithTag(OPPONENT_PHOTO_TAG).fetchSemanticsNode().size.width / compose.density.density).roundToInt()

    /**
     * The level [code] on a [screen] (its width x height in dp) as it opens, captured as
     * `game_fit_<code>_<screen>`: the canvas frame `fit-<code>-<screen>` beside it in the PR. Its
     * photo is [photo] dp across, as game-screens.md's table says.
     */
    private fun frame(code: String, screen: String, photo: Int, dark: Boolean) {
        val level = level(code)
        compose.setContent { DotsTheme(GameTheme.NOTEBOOK, dark) { Surface { Opening(level) } } }
        assertEquals("$code at $screen: the photo", photo, photoSize())
        compose.onRoot().captureRoboImage("src/test/screenshots/game_fit_${code.replace('-', '_')}_$screen${if (dark) "_dark" else ""}.png")
    }

    // 360 x 640: the 62 dp photo beside the opponent's line; a boss's counter in its row.

    @Test
    @Config(qualifiers = "w360dp-h640dp-xhdpi")
    fun level106At640() = frame("1-06", "360x640", photo = 62, dark = false)

    @Test
    @Config(qualifiers = "w360dp-h640dp-xhdpi")
    fun level106At640Dark() = frame("1-06", "360x640", photo = 62, dark = true)

    @Test
    @Config(qualifiers = "w360dp-h640dp-xhdpi")
    fun boss115At640() = frame("1-15", "360x640", photo = 62, dark = false)

    @Test
    @Config(qualifiers = "w360dp-h640dp-xhdpi")
    fun boss115At640Dark() = frame("1-15", "360x640", photo = 62, dark = true)

    // 360 x 720: a smaller photo, or the full one; a boss's counter right above the opponent's line.

    @Test
    @Config(qualifiers = "w360dp-h720dp-xhdpi")
    fun level106At720() = frame("1-06", "360x720", photo = 116, dark = false)

    @Test
    @Config(qualifiers = "w360dp-h720dp-xhdpi")
    fun level106At720Dark() = frame("1-06", "360x720", photo = 116, dark = true)

    /** The largest ordinary board, 14 x 14. */
    @Test
    @Config(qualifiers = "w360dp-h720dp-xhdpi")
    fun level406At720() = frame("4-06", "360x720", photo = 124, dark = false)

    @Test
    @Config(qualifiers = "w360dp-h720dp-xhdpi")
    fun level406At720Dark() = frame("4-06", "360x720", photo = 124, dark = true)

    @Test
    @Config(qualifiers = "w360dp-h720dp-xhdpi")
    fun boss115At720() = frame("1-15", "360x720", photo = 88, dark = false)

    @Test
    @Config(qualifiers = "w360dp-h720dp-xhdpi")
    fun boss115At720Dark() = frame("1-15", "360x720", photo = 88, dark = true)

    @Test
    @Config(qualifiers = "w360dp-h720dp-xhdpi")
    fun boss220At720() = frame("2-20", "360x720", photo = 98, dark = false)

    @Test
    @Config(qualifiers = "w360dp-h720dp-xhdpi")
    fun boss220At720Dark() = frame("2-20", "360x720", photo = 98, dark = true)

    @Test
    @Config(qualifiers = "w360dp-h720dp-xhdpi")
    fun boss320At720() = frame("3-20", "360x720", photo = 98, dark = false)

    @Test
    @Config(qualifiers = "w360dp-h720dp-xhdpi")
    fun boss320At720Dark() = frame("3-20", "360x720", photo = 98, dark = true)

    /** The Kraken has no counter, so it keeps the full photo. */
    @Test
    @Config(qualifiers = "w360dp-h720dp-xhdpi")
    fun kraken420At720() = frame("4-20", "360x720", photo = 124, dark = false)

    @Test
    @Config(qualifiers = "w360dp-h720dp-xhdpi")
    fun kraken420At720Dark() = frame("4-20", "360x720", photo = 124, dark = true)

    // 412 x 800: the full photo; a boss's a little smaller.

    @Test
    @Config(qualifiers = "w412dp-h800dp-xhdpi")
    fun level106At800() = frame("1-06", "412x800", photo = 124, dark = false)

    @Test
    @Config(qualifiers = "w412dp-h800dp-xhdpi")
    fun level106At800Dark() = frame("1-06", "412x800", photo = 124, dark = true)

    @Test
    @Config(qualifiers = "w412dp-h800dp-xhdpi")
    fun boss115At800() = frame("1-15", "412x800", photo = 120, dark = false)

    @Test
    @Config(qualifiers = "w412dp-h800dp-xhdpi")
    fun boss115At800Dark() = frame("1-15", "412x800", photo = 120, dark = true)

    /**
     * Every campaign level, bosses and the tutorial included, opens at 360 x 720 dp with the
     * opponent's face above the board, and its board as large as the layout before the steps: the
     * largest whole-dp spacing under a 56 dp top row, above a 56 dp player line and, out of the
     * tutorial, a 64 dp tool bar.
     */
    @Test
    @Config(qualifiers = "w360dp-h720dp-xhdpi")
    fun everyLevelShowsTheFaceAt720() {
        val levels = Campaign.levels
        assertEquals("campaign levels", 75, levels.size)
        val shown = mutableStateOf(levels.first())
        compose.setContent { DotsTheme(GameTheme.NOTEBOOK, false) { Surface { key(shown.value) { Opening(shown.value) } } } }
        val density = compose.density.density
        val root = compose.onRoot().fetchSemanticsNode().size
        assertEquals("the screen, in dp", listOf(360, 720), listOf(root.width, root.height).map { (it / density).roundToInt() })
        val withoutFace = mutableListOf<String>()
        val smallerBoard = mutableListOf<String>()
        for (level in levels) {
            compose.runOnUiThread {
                shown.value = level
                Snapshot.sendApplyNotifications()
            }
            compose.waitForIdle()
            if (compose.onAllNodesWithTag(OPPONENT_PHOTO_TAG).fetchSemanticsNodes().size != 1) withoutFace += level.code
            val state = level.newGame()
            val tools = if (Tutorial.lesson(level) != null) 0 else 64
            val spacing = boardSpacing(360.dp, (720 - 56 - 56 - tools).dp, state.width, state.height).value
            val board = compose.onNodeWithTag(BOARD_TAG).fetchSemanticsNode().size
            val drawn = listOf(board.width, board.height).map { (it / density).roundToInt() }
            if (drawn != listOf(spacing * state.width, spacing * state.height).map { it.roundToInt() }) smallerBoard += "${level.code} $drawn"
        }
        assertEquals("levels with no face at 360 x 720", emptyList<String>(), withoutFace)
        assertEquals("levels whose board is not the largest that fits", emptyList<String>(), smallerBoard)
    }

    /** The steps' edges, from game-screens.md's table. */
    @Test
    fun theStepsByRoom() {
        fun fit(room: Int, counter: Boolean = false) = opponentFit(room.dp, counter)
        assertEquals(OpponentFit.Above(124.dp), fit(192))
        assertEquals(OpponentFit.Above(123.dp), fit(191))
        assertEquals(OpponentFit.Above(72.dp), fit(140))
        assertEquals(OpponentFit.Beside, fit(139))
        assertEquals(OpponentFit.Beside, fit(76))
        assertEquals(OpponentFit.TopRow, fit(75))
        assertEquals(OpponentFit.Above(124.dp), fit(220, counter = true))
        assertEquals(OpponentFit.Above(123.dp), fit(219, counter = true))
        assertEquals(OpponentFit.Above(72.dp), fit(168, counter = true))
        assertEquals(OpponentFit.Beside, fit(167, counter = true))
        assertEquals(OpponentFit.Beside, fit(76, counter = true))
        assertEquals(OpponentFit.TopRow, fit(75, counter = true))
        assertEquals("only the full photo keeps the board touches", listOf(true, false), listOf(fit(192), fit(191)).map { (it as OpponentFit.Above).full })
    }
}
