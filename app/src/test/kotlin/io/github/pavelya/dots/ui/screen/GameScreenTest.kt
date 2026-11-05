package io.github.pavelya.dots.ui.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import dots.ai.Ai
import dots.engine.Action
import dots.engine.GameSettings
import dots.engine.GameState
import dots.engine.Opening
import io.github.pavelya.dots.game.gameViewModel
import io.github.pavelya.dots.ui.board.BoardGeometry
import io.github.pavelya.dots.ui.theme.DotsTheme
import kotlinx.coroutines.Dispatchers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GameScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val settings = GameSettings(width = 5, height = 5, dotsPerPlayer = 2, opening = Opening.EMPTY)

    private val firstFreePoint = object : Ai {
        override fun choose(state: GameState): Action = Action.Place(state.legalMoves().first())
    }

    /** Taps a board point twice (choose, then place), as a player would. */
    private fun place(x: Int, y: Int) {
        repeat(2) {
            compose.onNodeWithTag(BOARD_TAG).performTouchInput {
                click(BoardGeometry(settings.width, settings.height, width.toFloat(), height.toFloat()).center(x, y))
            }
        }
    }

    private fun waitFor(text: String) =
        compose.waitUntil(timeoutMillis = 5_000) { compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty() }

    @Test
    fun `a whole game against the computer ends on the result screen, and play again restarts`() {
        val vm = gameViewModel(
            newSettings = { settings },
            newComputer = { firstFreePoint },
            computerDispatcher = Dispatchers.Default,
            replyDelayMillis = 0,
        )
        compose.setContent { DotsTheme { GameScreen(vm, onBack = {}) } }
        waitFor("Your turn · 2 dots left")

        place(4, 4)
        waitFor("Your turn · 1 dot left")
        place(4, 3)

        waitFor("A draw, 0 to 0")
        compose.onNodeWithText("Play again").assertIsDisplayed().performClick()
        waitFor("Your turn · 2 dots left")
        compose.onNodeWithText("Undo").assertIsDisplayed()
    }
}
