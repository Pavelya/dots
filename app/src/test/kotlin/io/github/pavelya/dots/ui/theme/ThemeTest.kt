package io.github.pavelya.dots.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import dots.engine.Player
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The Notebook theme holds the values of its design pages, docs/design/theme-notebook.md and
 * pieces.md. Paths are from the app module, where Gradle runs the tests.
 */
class ThemeTest {
    private val page = File("../docs/design/theme-notebook.md").readText()
    private val notebook = GameTheme.NOTEBOOK

    /** The rows of the page's table under [heading]: each row's cells, without the header. */
    private fun table(heading: String): List<List<String>> =
        page.substringAfter("## $heading\n").lineSequence()
            .dropWhile { !it.startsWith("|") }
            .takeWhile { it.startsWith("|") }
            .drop(2)
            .map { row -> row.trim('|').split("|").map { it.trim() } }
            .toList()

    private fun hex(color: Color): String = "#%06X".format(color.toArgb() and 0xFFFFFF)

    /** Each colour of the page's "Colours" table, by its name there. */
    private fun named(palette: Palette): Map<String, Color> = mapOf(
        "Paper" to palette.paper, "Card" to palette.card, "Grid" to palette.grid,
        "Red ink" to palette.red, "Blue ink" to palette.blue,
        "Red wash" to palette.redWash, "Blue wash" to palette.blueWash,
        "Red pale" to palette.redPale, "Blue pale" to palette.bluePale,
        "Highlighter" to palette.highlighter, "Highlighter faint" to palette.highlighterFaint,
        "Pencil" to palette.pencil, "Graphite" to palette.graphite, "Margin" to palette.margin,
    )

    @Test
    fun `every colour is the page's, light and dark`() {
        val rows = table("Colours")
        assertEquals(14, rows.size)
        for ((name, light, dark) in rows) {
            assertEquals("$name, light", light.trim('`'), hex(named(notebook.light).getValue(name)))
            assertEquals("$name, dark", dark.trim('`'), hex(named(notebook.dark).getValue(name)))
        }
    }

    @Test
    fun `the Material roles take the page's colours`() {
        val roles: Map<String, (ColorScheme) -> Color> = mapOf(
            "background" to { it.background }, "surface" to { it.surface },
            "surfaceContainer" to { it.surfaceContainer },
            "onBackground" to { it.onBackground }, "onSurface" to { it.onSurface },
            "onSurfaceVariant" to { it.onSurfaceVariant },
            "primary" to { it.primary }, "onPrimary" to { it.onPrimary },
            "secondary" to { it.secondary }, "onSecondary" to { it.onSecondary },
            "outline" to { it.outline }, "outlineVariant" to { it.outlineVariant },
            "error" to { it.error }, "onError" to { it.onError },
        )
        val rows = table("Interface colours")
        var checked = 0
        for (palette in listOf(notebook.light, notebook.dark)) {
            val colour = named(palette)
            for ((names, name) in rows) {
                for (role in Regex("`(\\w+)`").findAll(names).map { it.groupValues[1] }) {
                    assertEquals(role, colour.getValue(name), roles.getValue(role)(palette.material))
                    checked++
                }
            }
            // "and its lower and higher steps"
            val card = palette.card
            with(palette.material) {
                assertEquals(listOf(card, card, card, card), listOf(surfaceContainerLowest, surfaceContainerLow, surfaceContainerHigh, surfaceContainerHighest))
            }
        }
        assertEquals(2 * roles.size, checked)
    }

    @Test
    fun `board sizes at the usual 24 dp spacing and at the limits`() {
        val look = notebook.board
        // At 1 px per dp, so pixels read as dp.
        fun px(measure: Measure, spacing: Float) = measure.px(spacing, density = 1f)
        assertEquals(12f, px(look.dot, 24f))
        assertEquals(9f, px(look.dot, 10f))
        assertEquals(20f, px(look.dot, 60f))
        assertEquals(PieceShape.DOT, look.red.shape)
        assertEquals(PieceShape.DIAMOND, look.blue.shape)
        assertEquals(14f, px(look.dot, 24f) * look.blue.across, 0.001f)
        assertEquals(1f, px(look.grid, 24f))
        assertEquals(1.5f, px(look.edge, 24f))
        assertEquals(1.5f, px(look.link, 24f))
        assertEquals(20.16f, px(look.threatRing, 24f), 0.001f)
        assertEquals(2f, px(look.threatNear, 24f))
        assertEquals(1.5f, px(look.threatFar, 24f))
        assertEquals(8f, px(look.closingPoint, 24f), 0.001f)
        assertEquals(1.5f, px(look.closingLine, 24f))
        assertEquals(21.6f, px(look.previewRing, 24f), 0.001f)
        assertEquals(2f, px(look.previewLine, 24f))
        assertEquals(1f, look.capturedAlpha)
    }

    @Test
    fun `a palette gives each side its ink, wash and pale`() {
        val palette = notebook.light
        assertEquals(listOf(palette.red, palette.redWash, palette.redPale), listOf(palette.ink(Player.RED), palette.wash(Player.RED), palette.pale(Player.RED)))
        assertEquals(listOf(palette.blue, palette.blueWash, palette.bluePale), listOf(palette.ink(Player.BLUE), palette.wash(Player.BLUE), palette.pale(Player.BLUE)))
    }
}
