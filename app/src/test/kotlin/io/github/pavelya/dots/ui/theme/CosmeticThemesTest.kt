package io.github.pavelya.dots.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import io.github.pavelya.dots.ui.board.bounds
import java.io.File
import java.util.Locale
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Star chart and Neon arcade hold the values of their theme pages, docs/design/theme-star-chart.md
 * and theme-neon-arcade.md: every colour, the Material roles, the board's sizes, every contrast
 * pair with its target, and the colour-blindness check of the two inks. Paths are from the app
 * module, where Gradle runs the tests.
 */
class CosmeticThemesTest {
    /** A theme's page, and the colours of its "Colours" table by their names there. */
    private class Page(val theme: GameTheme, file: String, val names: (Palette) -> Map<String, Color>) {
        val text = File("../docs/design/$file").readText()

        /** The rows of the first table after [heading]: each row's cells, without the header. */
        fun table(heading: String, after: String? = null): List<List<String>> {
            var section = text.substringAfter("## $heading\n")
            if (after != null) section = section.substringAfter(after)
            return section.lineSequence()
                .dropWhile { !it.startsWith("|") }
                .takeWhile { it.startsWith("|") }
                .drop(2)
                .map { row -> row.trim('|').split("|").map { it.trim() } }
                .toList()
        }
    }

    private val starChart = Page(GameTheme.STAR_CHART, "theme-star-chart.md") { p ->
        mapOf(
            "Paper" to p.paper, "Card" to p.card, "Grid" to p.grid, "Chart point" to p.chartPoint,
            "Gold ink" to p.red, "Ice ink" to p.blue, "Gold haze" to p.redWash, "Ice haze" to p.blueWash,
            "Gold pale" to p.redPale, "Ice pale" to p.bluePale, "Nova" to p.highlighter, "Nova faint" to p.highlighterFaint,
            "Starlight" to p.pencil, "Dust" to p.graphite,
        )
    }

    private val neonArcade = Page(GameTheme.NEON_ARCADE, "theme-neon-arcade.md") { p ->
        mapOf(
            "Paper" to p.paper, "Card" to p.card, "Grid" to p.grid,
            "Magenta ink" to p.red, "Cyan ink" to p.blue, "Magenta wash" to p.redWash, "Cyan wash" to p.blueWash,
            "Magenta glow" to p.redPale, "Cyan glow" to p.bluePale, "Magenta core" to p.redCore, "Cyan core" to p.blueCore,
            "Volt" to p.highlighter, "Volt faint" to p.highlighterFaint, "Main text" to p.pencil, "Dim text" to p.graphite,
        )
    }

    private val pages = listOf(starChart, neonArcade)

    private fun hex(color: Color): String = "#%06X".format(color.toArgb() and 0xFFFFFF)

    @Test
    fun `every colour is the page's, light and dark`() {
        for (page in pages) {
            val rows = page.table("Colours")
            assertEquals(page.names(page.theme.light).size, rows.size)
            for (row in rows) {
                val (name, light, dark) = row
                assertEquals("${page.theme.name}: $name, light", light.trim('`'), hex(page.names(page.theme.light).getValue(name)))
                assertEquals("${page.theme.name}: $name, dark", dark.trim('`'), hex(page.names(page.theme.dark).getValue(name)))
            }
        }
    }

    @Test
    fun `the tape, the margin and the scrim are the page's`() {
        // "The tape is Gold pale" / "Magenta glow"; the margin can be Grid; the scrim is Notebook's.
        for (page in pages) for (palette in listOf(page.theme.light, page.theme.dark)) {
            assertEquals(palette.redPale, palette.tape)
            assertEquals(palette.grid, palette.margin)
        }
        for (page in pages) {
            assertEquals(GameTheme.NOTEBOOK.light.scrim, page.theme.light.scrim)
            assertEquals(GameTheme.NOTEBOOK.dark.scrim, page.theme.dark.scrim)
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
        for (page in pages) {
            var checked = 0
            for (palette in listOf(page.theme.light, page.theme.dark)) {
                val colour = page.names(palette)
                for ((names, name) in page.table("Interface colours")) {
                    for (role in Regex("`(\\w+)`").findAll(names).map { it.groupValues[1] }) {
                        assertEquals("${page.theme.name}: $role", colour.getValue(name), roles.getValue(role)(palette.material))
                        checked++
                    }
                }
                val card = palette.card
                with(palette.material) {
                    assertEquals(listOf(card, card, card, card), listOf(surfaceContainerLowest, surfaceContainerLow, surfaceContainerHigh, surfaceContainerHighest))
                }
            }
            assertEquals(2 * roles.size, checked)
        }
    }

    /** At 1 px per dp, so pixels read as dp. */
    private fun px(measure: Measure, spacing: Float = 24f) = measure.px(spacing, density = 1f)

    @Test
    fun `both boards keep Notebook's sizes, with straight rings and sparser hatching`() {
        val notebook = GameTheme.NOTEBOOK.board
        for (theme in listOf(GameTheme.STAR_CHART, GameTheme.NEON_ARCADE)) {
            val look = theme.board
            for (spacing in listOf(10f, 24f, 60f)) {
                assertEquals(px(notebook.dot, spacing), px(look.dot, spacing))
                assertEquals(px(notebook.ring, spacing), px(look.ring, spacing))
                assertEquals(px(notebook.threatRing, spacing), px(look.threatRing, spacing))
                assertEquals(px(notebook.closingPoint, spacing), px(look.closingPoint, spacing))
                assertEquals(px(notebook.previewRing, spacing), px(look.previewRing, spacing))
            }
            assertEquals(listOf(1f, 1.5f, 1f, 1.5f, 2f, 1.5f, 1.5f, 2f, 1.5f), listOf(look.grid, look.edge, look.hatch, look.link, look.threatNear, look.threatFar, look.closingLine, look.previewLine, look.guide).map { px(it) })
            assertEquals(listOf(4f, 3f, 1f), listOf(look.guideDash, look.guideGap, look.guideAlpha))
            // "Straight and of even width": no bow, no change of thickness.
            assertEquals(0f, look.ringBow)
            assertEquals(0f, look.ringWobble)
            assertEquals(6f, look.hatchGap)
            // The pinhole and the cut as Notebook's: 3.6 dp across; 2 dp wide and 18 dp long.
            assertEquals(3.6f, px(look.dot) * look.pinhole, 0.001f)
            assertEquals(2f, px(look.dot) * look.cut, 0.001f)
            assertEquals(18f, px(look.dot) * look.cutLength, 0.001f)
            assertEquals(1f, look.capturedAlpha)
            assertEquals(NotebookType, theme.type)
        }
    }

    @Test
    fun `Star chart's stars, comets and chart points`() {
        val look = GameTheme.STAR_CHART.board
        val dot = px(look.dot)
        assertEquals(PieceShape.STAR, look.red.shape)
        assertEquals(PieceShape.COMET, look.blue.shape)
        // The star: 16.8 dp across its points.
        assertEquals(16.8f, dot * look.red.across, 0.001f)
        // The comet: a 9 dp head, its middle streak's tip 16.8 dp from the head's centre, up and to the left.
        assertEquals(9f, dot * look.blue.across, 0.001f)
        val comet = look.blue.bounds(dot)
        assertEquals(16.8f, hypot(comet.left, comet.top), 0.01f)
        assertEquals(4.5f, comet.right, 0.001f)
        assertEquals(4.5f, comet.bottom, 0.001f)
        assertEquals(2f, px(look.chartPoint))
        assertEquals(2f, px(look.chartPoint, 60f))
        assertEquals(CaptureMotion.CONSTELLATION, look.capture)
        // Nothing glows.
        assertEquals(listOf(0f, 0f), listOf(px(look.glow), px(look.ringGlow)))
        assertEquals(0f, look.ringCore)
        assertEquals(FaceInks.STAR_CHART, GameTheme.STAR_CHART.faceInks)
    }

    @Test
    fun `Neon arcade's discs, triangles, glows and tubes`() {
        val look = GameTheme.NEON_ARCADE.board
        val dot = px(look.dot)
        assertEquals(PieceShape.DOT, look.red.shape)
        assertEquals(PieceShape.TRIANGLE, look.blue.shape)
        assertEquals(12f, dot * look.red.across, 0.001f)
        // The triangle: corners 7.9 dp from its centre, 7.9 dp above the point and its base 4 dp below; sides 13.7 dp.
        val triangle = look.blue.bounds(dot)
        assertEquals(-7.92f, triangle.top, 0.01f)
        assertEquals(3.96f, triangle.bottom, 0.01f)
        assertEquals(13.7f, triangle.width, 0.05f)
        assertEquals(1.5f, px(look.glow))
        assertEquals(1.5f, px(look.ringGlow))
        // The core: a third of the tube.
        assertEquals(2f / 3, px(look.ring) * look.ringCore, 0.001f)
        assertEquals(0f, px(look.chartPoint))
        assertEquals(CaptureMotion.NEON, look.capture)
        assertEquals(FaceInks.NEON_ARCADE, GameTheme.NEON_ARCADE.faceInks)
    }

    @Test
    fun `high-contrast pieces as the theme pages draw them, the band in place of the glow`() {
        for (theme in listOf(GameTheme.STAR_CHART, GameTheme.NEON_ARCADE)) {
            val look = theme.board(highContrast = true)
            assertEquals(14.4f, px(look.dot), 0.001f)
            assertEquals(1.5f, px(look.halo))
            assertEquals(1.5f, px(look.previewOutline))
            assertEquals(2f, px(look.guide))
            assertEquals(1f, px(look.guideCasing))
            assertEquals(0f, px(look.glow))
            // The tubes keep their glow.
            assertEquals(theme.board.ringGlow, look.ringGlow)
        }
        val stars = GameTheme.STAR_CHART.board(highContrast = true)
        assertEquals(20.2f, px(stars.dot) * stars.red.across, 0.05f)
        assertEquals(10.8f, px(stars.dot) * stars.blue.across, 0.001f)
        val neon = GameTheme.NEON_ARCADE.board(highContrast = true)
        assertEquals(9.5f, px(neon.dot) * neon.blue.across / 2, 0.01f)
    }

    // ---- Contrast (WCAG 2), as the pages' scripts compute it ----

    private fun linear(c: Int): Double {
        val v = c / 255.0
        return if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }

    private fun luminance(color: Color, matrix: List<List<Double>>? = null): Double {
        val argb = color.toArgb()
        var lin = listOf(argb shr 16 and 0xFF, argb shr 8 and 0xFF, argb and 0xFF).map(::linear)
        if (matrix != null) lin = matrix.map { row -> row.zip(lin).sumOf { (m, v) -> m * v }.coerceIn(0.0, 1.0) }
        return 0.2126 * lin[0] + 0.7152 * lin[1] + 0.0722 * lin[2]
    }

    /** The contrast of [a] and [b], cut (not rounded) to two decimals, as the pages give it. */
    private fun ratio(a: Color, b: Color, matrix: List<List<Double>>? = null): String {
        val la = luminance(a, matrix)
        val lb = luminance(b, matrix)
        val r = (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
        return String.format(Locale.ROOT, "%.2f", floor(r * 100) / 100)
    }

    /** A colour named in a contrast pair ("paper", "gold ink", "ice pale (hatch line)"). */
    private fun Page.colour(palette: Palette, name: String): Color {
        val key = name.substringBefore(" (").trim()
        return names(palette).entries.first { it.key.equals(key, ignoreCase = true) }.value
    }

    @Test
    fun `every contrast pair is the page's figure and meets its target`() {
        for (page in pages) {
            val rows = page.table("Contrast")
            assertTrue(rows.size > 30)
            for ((kind, pair, target, light, dark) in rows) {
                // "Pinhole: paper on gold ink", "Ice ink on gold pale (hatch line)".
                val (front, back) = pair.substringAfter(": ").substringBefore(" (").split(" on ")
                for ((palette, figure) in listOf(page.theme.light to light, page.theme.dark to dark)) {
                    val r = ratio(page.colour(palette, front), page.colour(palette, back))
                    assertEquals("${page.theme.name}: $kind, $pair", figure, r)
                    if (target != "–") assertTrue("${page.theme.name}: $pair is $r, under $target", r.toDouble() >= target.toDouble())
                }
            }
        }
    }

    /** Machado, Oliveira and Fernandes (2009), at full strength, on linear RGB (docs/design/pieces/draw.py). */
    private val simulations: Map<String, List<List<Double>>?> = mapOf(
        "As seen" to null,
        "Greyscale" to List(3) { listOf(0.2126, 0.7152, 0.0722) },
        "Protanopia" to listOf(listOf(0.152286, 1.052583, -0.204868), listOf(0.114503, 0.786281, 0.099216), listOf(-0.003882, -0.048116, 1.051998)),
        "Deuteranopia" to listOf(listOf(0.367322, 0.860646, -0.227968), listOf(0.280085, 0.672501, 0.047413), listOf(-0.011820, 0.042940, 0.968881)),
        "Tritanopia" to listOf(listOf(1.255528, -0.076749, -0.178779), listOf(-0.078411, 0.930809, 0.147602), listOf(0.004733, 0.691367, 0.303900)),
    )

    @Test
    fun `colour-blindness check - the two inks in every view are the page's, and the shapes tell the sides apart`() {
        for (page in pages) {
            val rows = page.table("Colour-blindness check", after = "**Colour alone is not enough.**")
            assertEquals(simulations.keys.toList(), rows.map { it[0] })
            for ((view, light, dark) in rows) {
                val matrix = simulations.getValue(view)
                assertEquals("${page.theme.name}: $view, light", light, ratio(page.theme.light.red, page.theme.light.blue, matrix))
                assertEquals("${page.theme.name}: $view, dark", dark, ratio(page.theme.dark.red, page.theme.dark.blue, matrix))
            }
            // Colour alone is not enough: the sides' shapes differ on the usual and the high-contrast board.
            for (look in listOf(page.theme.board, page.theme.highContrastBoard)) assertNotEquals(look.red.shape, look.blue.shape)
        }
    }

    @Test
    fun `every theme is listed, Notebook first, and no theme shares a name`() {
        assertEquals(listOf("Notebook", "Star chart", "Neon arcade"), GameTheme.ALL.map { it.name })
        assertEquals(GameTheme.ALL.size, GameTheme.ALL.map { it.faceInks }.toSet().size)
    }
}
