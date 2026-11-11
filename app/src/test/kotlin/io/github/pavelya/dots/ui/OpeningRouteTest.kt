package io.github.pavelya.dots.ui

import dots.campaign.Campaign
import dots.campaign.Progress
import dots.engine.GameSettings
import dots.log.GameLogEntry
import io.github.pavelya.dots.game.levelGame
import org.junit.Assert.assertEquals
import org.junit.Test

/** Where the app opens after loading (docs/design/flows.md, First launch and Returning player). */
class OpeningRouteTest {
    private val level1 = Campaign.levels[0]
    private val level2 = Campaign.levels[1]
    private val passed = Progress(mapOf(level1.code to 2))
    private val level1Game = levelGame(level1, threatHighlight = true, now = 0)
    private val level2Game = levelGame(level2, threatHighlight = true, now = 0)
    private val freeGame = GameLogEntry.start(GameSettings.POC, "normal", threatHighlight = false, now = 0)

    @Test
    fun `until Level 1 is passed, its board opens, new or saved`() {
        assertEquals(Routes.GAME, openingRoute(Progress.NONE, saved = null))
        assertEquals(Routes.GAME, openingRoute(Progress.NONE, saved = level1Game))
    }

    @Test
    fun `another saved game is not replaced: the map opens`() {
        assertEquals(Routes.MAP, openingRoute(Progress.NONE, saved = freeGame))
    }

    @Test
    fun `after Level 1 is passed, the map opens, whatever is saved`() {
        assertEquals(Routes.MAP, openingRoute(passed, saved = null))
        assertEquals(Routes.MAP, openingRoute(passed, saved = level1Game))
        assertEquals(Routes.MAP, openingRoute(passed, saved = level2Game))
        assertEquals(Routes.MAP, openingRoute(passed, saved = freeGame))
    }
}
