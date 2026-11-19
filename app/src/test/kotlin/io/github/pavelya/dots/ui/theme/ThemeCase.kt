package io.github.pavelya.dots.ui.theme

/**
 * A theme that a screenshot test runs in, for tests parameterized over [ALL]. Notebook's reference
 * images keep their names; another theme's take its [key] before a closing `_dark`, as the canvas
 * names its frames (`game_threat_star_chart_dark` beside `game-threat-star-chart-dark.png`).
 */
class ThemeCase(val theme: GameTheme, val key: String) {
    val isNotebook: Boolean get() = theme == GameTheme.NOTEBOOK

    /** The reference image's name for [base], Notebook's name. */
    fun name(base: String): String = when {
        isNotebook -> base
        base.endsWith("_dark") -> "${base.removeSuffix("_dark")}_${key}_dark"
        else -> "${base}_$key"
    }

    override fun toString(): String = key

    companion object {
        val NOTEBOOK = ThemeCase(GameTheme.NOTEBOOK, "notebook")
        val STAR_CHART = ThemeCase(GameTheme.STAR_CHART, "star_chart")
        val NEON_ARCADE = ThemeCase(GameTheme.NEON_ARCADE, "neon_arcade")

        val ALL = listOf(NOTEBOOK, STAR_CHART, NEON_ARCADE)

        /** The parameters of a test parameterized over every theme. */
        fun parameters(): List<Array<Any>> = ALL.map { arrayOf<Any>(it) }
    }
}
