package io.github.pavelya.dots.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import dots.engine.Player
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun `rings, hatching, links and drag's guides as the page draws them`() {
        val look = notebook.board
        fun px(measure: Measure, spacing: Float) = measure.px(spacing, density = 1f)
        // The ring: 1/12 of the spacing, at least 1.5 dp, at most 3 dp; bowing by up to 0.4 dp and
        // varying by up to 15%.
        assertEquals(2f, px(look.ring, 24f), 0.001f)
        assertEquals(1.5f, px(look.ring, 12f), 0.001f)
        assertEquals(3f, px(look.ring, 60f), 0.001f)
        assertEquals(0.4f, look.ringBow)
        assertEquals(0.15f, look.ringWobble)
        // Hatching: 1 dp, one line every 5 dp, at every spacing.
        assertEquals(1f, px(look.hatch, 24f))
        assertEquals(1f, px(look.hatch, 60f))
        assertEquals(5f, look.hatchGap)
        assertEquals(true, look.links)
        // Drag's guides: 1.5 dp, dashes 4 dp long with 3 dp gaps, solid ink, at every spacing.
        assertEquals(1.5f, px(look.guide, 24f))
        assertEquals(1.5f, px(look.guide, 21f))
        assertEquals(listOf(4f, 3f, 1f), listOf(look.guideDash, look.guideGap, look.guideAlpha))
    }

    @Test
    fun `text styles are the page's sizes and weights`() {
        val styles = with(notebook.type) {
            mapOf("Display" to display, "Headline" to headline, "Title" to title, "Score" to score, "Body" to body, "Label" to label, "Small" to small)
        }
        // The section's second table, one row per style.
        val rows = page.substringAfter("## Type\n").substringBefore("\n## ").lines()
            .map { row -> row.trim().trim('|').split("|").map { it.trim() } }
            .filter { it.size == 6 && it[0] in styles }
        assertEquals(7, rows.size)
        assertTrue("two fonts", caveat != null && nunito != null && caveat != nunito)
        for ((name, font, weight, size, lineHeight) in rows) {
            val style = styles.getValue(name)
            assertEquals("$name weight", weight.toInt(), style.fontWeight?.weight)
            assertEquals("$name size", size, "${style.fontSize.value.toInt()} sp")
            assertEquals("$name line height", lineHeight, "${style.lineHeight.value.toInt()} sp")
            val family = if (font == "Caveat") caveat else nunito
            assertEquals("$name font", family, style.fontFamily)
        }
    }

    @Test
    fun `the Material styles use the theme's fonts`() {
        val type = notebook.type
        with(type.material) {
            assertEquals(type.display, displaySmall)
            assertEquals(type.headline, headlineSmall)
            assertEquals(type.title, titleLarge)
            assertEquals(type.body, bodyLarge)
            assertEquals(type.label, labelLarge)
            for (style in listOf(titleMedium, titleSmall, bodyMedium, bodySmall, labelMedium, labelSmall)) {
                assertEquals(nunito, style.fontFamily)
            }
        }
    }

    private val caveat = notebook.type.display.fontFamily
    private val nunito = notebook.type.body.fontFamily

    @Test
    fun `a palette gives each side its ink, wash and pale`() {
        val palette = notebook.light
        assertEquals(listOf(palette.red, palette.redWash, palette.redPale), listOf(palette.ink(Player.RED), palette.wash(Player.RED), palette.pale(Player.RED)))
        assertEquals(listOf(palette.blue, palette.blueWash, palette.bluePale), listOf(palette.ink(Player.BLUE), palette.wash(Player.BLUE), palette.pale(Player.BLUE)))
    }
}
