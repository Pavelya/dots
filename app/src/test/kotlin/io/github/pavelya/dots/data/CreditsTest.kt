package io.github.pavelya.dots.data

import io.github.pavelya.dots.ui.screen.reflow
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Credits in Settings lists everything the app ships that someone else may own: every font,
 * sound and music file, and every library the app is built with, each with a licence whose full
 * text the app carries. Paths are from the app module, where Gradle runs the tests.
 */
class CreditsTest {
    private val main = File("src/main")

    /** Fonts, sounds and music in the app: every file in a `font` or `raw` resource folder, and every font or audio file in the assets. */
    private fun shippedAssets(): List<String> {
        val inResources = main.resolve("res").listFiles().orEmpty()
            .filter { it.isDirectory && (it.name.startsWith("font") || it.name.startsWith("raw")) }
            .flatMap { folder -> folder.walk().filter { it.isFile }.toList() }
        val inAssets = main.resolve("assets").walk()
            .filter { it.isFile && it.extension.lowercase() in FONT_AND_AUDIO }
            .toList()
        return (inResources + inAssets).map { it.relativeTo(main).invariantSeparatorsPath }.sorted()
    }

    @Test
    fun `every font, sound and music file the app ships is in the credits`() {
        val credited = (Credits.fonts + Credits.sounds).flatMap { it.files }.toSet()
        val missing = shippedAssets().filterNot { it in credited }
        assertEquals("Files with no entry in Credits.fonts or Credits.sounds", emptyList<String>(), missing)
    }

    @Test
    fun `every file the credits name is in the app`() {
        val gone = (Credits.fonts + Credits.sounds).flatMap { it.files }.filterNot { main.resolve(it).isFile }
        assertEquals("Credited files that are not in app/src/main", emptyList<String>(), gone)
    }

    @Test
    fun `every library the app is built with is in the credits`() {
        val modules = catalogModules()
        val aliases = Regex("""^\s*implementation\(libs\.([\w.]+)\)""", RegexOption.MULTILINE)
            .findAll(File("build.gradle.kts").readText())
            .map { it.groupValues[1] }
            .toList()
        assertTrue("No libraries found in app/build.gradle.kts", aliases.isNotEmpty())
        val groups = aliases.map { alias -> modules[alias] ?: error("libs.$alias is not in gradle/libs.versions.toml") }
            .map { it.substringBefore(':') }
            .distinct()
        val credited = Credits.libraries.flatMap { it.groups }.toSet()
        assertEquals("Library groups with no entry in Credits.libraries", emptyList<String>(), groups.filterNot { it in credited })
    }

    @Test
    fun `the app carries the full text of every licence it relies on`() {
        for (licence in Credits.licences) {
            val file = main.resolve("assets").resolve(licence.file)
            assertTrue("${licence.title}: no ${licence.file} in the assets", file.isFile && file.length() > 1_000)
        }
        val apache = main.resolve("assets").resolve(Credits.Licence.APACHE_2.file).readText()
        assertTrue(apache.contains("Apache License") && apache.contains("Version 2.0, January 2004"))
    }

    @Test
    fun `a licence's lines join into paragraphs`() {
        val text = "   Apache License\n   Version 2.0\n\n   1. Definitions.\n\n      \"License\" shall mean\n      the terms.\n"
        assertEquals("Apache License Version 2.0\n\n1. Definitions.\n\n\"License\" shall mean the terms.", reflow(text))
    }

    /** The version catalogue's libraries: alias, as the build scripts write it (`compose.ui`), to module (`androidx.compose.ui:ui`). */
    private fun catalogModules(): Map<String, String> {
        val catalogue = File("../gradle/libs.versions.toml").readText()
        return Regex("""^([\w-]+)\s*=\s*\{\s*module\s*=\s*"([^"]+)"""", RegexOption.MULTILINE)
            .findAll(catalogue)
            .associate { it.groupValues[1].replace('-', '.') to it.groupValues[2] }
    }

    private companion object {
        val FONT_AND_AUDIO = setOf("ttf", "otf", "woff", "woff2", "mp3", "ogg", "wav", "flac", "m4a", "aac", "opus", "mid", "midi")
    }
}
