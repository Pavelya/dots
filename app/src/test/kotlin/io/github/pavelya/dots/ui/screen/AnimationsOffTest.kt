package io.github.pavelya.dots.ui.screen

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import dots.engine.GameSettings
import dots.engine.Opening
import dots.engine.Player
import dots.engine.Pos
import io.github.pavelya.dots.game.GameViewModel
import io.github.pavelya.dots.game.gameViewModel
import io.github.pavelya.dots.ui.motion.LocalReduceMotion
import io.github.pavelya.dots.ui.theme.DotsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A whole game with the phone's animations removed (motion.md, rule 5): every moment shows its
 * end at once, Result comes up as soon as the game is over, and the game is the same game. With
 * animations on, Result waits for the end of the game to play.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class AnimationsOffTest {
    @get:Rule
    val compose = createComposeRule()

    /** Two people on a 7 x 7 board: Red rings Blue's (5, 5), then Blue's (1, 1); 8 dots each. */
    private val settings = GameSettings(width = 7, height = 7, dotsPerPlayer = 8, opening = Opening.EMPTY)
    private val red = listOf(Pos(5, 4), Pos(4, 5), Pos(6, 5), Pos(5, 6), Pos(1, 0), Pos(0, 1), Pos(2, 1), Pos(1, 2))
    private val blue = listOf(Pos(5, 5), Pos(1, 1), Pos(3, 3), Pos(3, 4), Pos(0, 6), Pos(6, 0), Pos(3, 6), Pos(6, 3))

    private fun play(vm: GameViewModel) {
        for (i in red.indices) {
            compose.runOnUiThread { repeat(2) { vm.onTap(red[i]) } }
            compose.mainClock.advanceTimeByFrame()
            if (!vm.ui.value.isOver) {
                compose.runOnUiThread { repeat(2) { vm.onTap(blue[i]) } }
                compose.mainClock.advanceTimeByFrame()
            }
        }
    }

    private fun resultShown(): Boolean = compose.onAllNodes(hasText("Play again")).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun `with animations removed, the game plays to its end and Result comes up at once`() {
        val vm = gameViewModel(newSettings = { settings })
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(LocalReduceMotion provides true) {
                DotsTheme { GameScreen(vm, onToMap = {}, onRules = {}) }
            }
        }
        compose.mainClock.advanceTimeByFrame()
        play(vm)
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeByFrame()

        assertTrue(resultShown())
        assertTrue(compose.onAllNodes(hasText("Red wins!")).fetchSemanticsNodes().isNotEmpty())
        // The same game as the rules play it.
        var rules = settings.newGame()
        for (i in red.indices) {
            rules = rules.play(red[i]).state
            if (!rules.isOver) rules = rules.play(blue[i]).state
        }
        assertEquals(rules.score(Player.RED), vm.ui.value.state.score(Player.RED))
        assertEquals(rules.score(Player.BLUE), vm.ui.value.state.score(Player.BLUE))
        assertEquals(2, vm.ui.value.state.score(Player.RED))
        assertEquals(0, vm.ui.value.state.score(Player.BLUE))
    }

    @Test
    fun `with animations on, Result waits until the areas have lit up`() {
        val vm = gameViewModel(newSettings = { settings })
        compose.mainClock.autoAdvance = false
        compose.setContent { DotsTheme { GameScreen(vm, onToMap = {}, onRules = {}) } }
        compose.mainClock.advanceTimeByFrame()
        play(vm)
        assertTrue(vm.ui.value.isOver)
        compose.mainClock.advanceTimeBy(300)
        assertFalse(resultShown())
        compose.mainClock.advanceTimeBy(2_000)
        assertTrue(resultShown())
    }
}
