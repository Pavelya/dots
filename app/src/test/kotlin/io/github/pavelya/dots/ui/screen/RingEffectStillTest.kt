package io.github.pavelya.dots.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.unit.dp
import dots.engine.Player
import dots.engine.Pos
import io.github.pavelya.dots.data.PieceStyleId
import io.github.pavelya.dots.data.RingEffectId
import io.github.pavelya.dots.game.BoardMoment
import io.github.pavelya.dots.ui.board.BoardInput
import io.github.pavelya.dots.ui.motion.LocalReduceMotion
import io.github.pavelya.dots.ui.theme.DotsTheme
import io.github.pavelya.dots.ui.theme.ThemeCase
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * With the phone's animations removed, a ring effect keeps its look and drops only its motion
 * (docs/design/cosmetics.md, Rules): the big capture's moment, just started, draws the same board
 * as no moment at all, in every theme and with each ring effect; with animations on, the
 * same moment just started draws something else (the ring is not there yet).
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w720dp-h800dp-xhdpi")
class RingEffectStillTest(private val case: ThemeCase) {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun themes(): List<Array<Any>> = ThemeCase.parameters()
    }

    @get:Rule
    val compose = createComposeRule()

    /** Red closes the big ring at (5, 7), as the view model hands the moment to the screen. */
    private val moment = BoardMoment(
        id = 1,
        dot = Pos(5, 7),
        by = Player.RED,
        announced = false,
        captures = CanvasGame.bigCapture.captures,
        pinholeBefore = Pos(7, 7),
        freshThreats = emptySet(),
    )

    /**
     * Two game boards side by side, the big capture's moment just started on the left and no moment
     * on the right, with animations removed when [reduce]: their pixels.
     */
    private fun boards(rings: RingEffectId, reduce: Boolean): Pair<IntArray, IntArray> {
        val still = CanvasGame.ui(CanvasGame.bigCapture)
        val playing = still.copy(moment = moment)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(LocalReduceMotion provides reduce) {
                DotsTheme(case.theme, dark = false, pieces = PieceStyleId.HEARTS_AND_CLUBS, rings = rings) {
                    Row {
                        for ((tag, ui) in listOf("playing" to playing, "still" to still)) {
                            Box(Modifier.width(360.dp).fillMaxHeight().testTag(tag)) {
                                GameBoard(ui, Player.RED, campaign = null, input = BoardInput.NONE, onUndo = {}, onPause = {})
                            }
                        }
                    }
                }
            }
        }
        // The first frame composes, the next is the moment's first (MotionScreenshotTest).
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeByFrame()
        // Only the boards: the rest of the screen (the score's swell, the face) has its own rules.
        fun pixels(tag: String): IntArray =
            compose.onNode(hasTestTag(BOARD_TAG) and hasAnyAncestor(hasTestTag(tag))).captureToImage().toPixelMap().buffer
        return pixels("playing") to pixels("still")
    }

    @Test
    fun lassoWithAnimationsRemoved() {
        val (playing, still) = boards(RingEffectId.LASSO, reduce = true)
        assertArrayEquals(still, playing)
    }

    @Test
    fun rubberStampWithAnimationsRemoved() {
        val (playing, still) = boards(RingEffectId.RUBBER_STAMP, reduce = true)
        assertArrayEquals(still, playing)
    }

    @Test
    fun rubberStampWithAnimationsOn() {
        val (playing, still) = boards(RingEffectId.RUBBER_STAMP, reduce = false)
        assertNotEquals(still.toList(), playing.toList())
    }
}
