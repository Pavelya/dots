package dots.cli

import java.io.File

/**
 * The asset-register check described in docs/assets.md ("The check"): every asset file in the
 * repository has a row in the register, every row matches a file, and no row has an empty cell.
 */
object AssetRegister {
    const val PATH = "docs/assets.md"

    private val columns = listOf("File", "What it is", "Tool and plan", "Date", "Prompt or source", "Licence", "Author", "Edits")

    private val assetTypes = setOf(
        // images, and the editable sources they are made from
        "png", "jpg", "jpeg", "webp", "gif", "svg", "bmp", "ico", "avif", "heic", "psd", "kra", "xcf", "ai",
        // sound and music
        "mp3", "ogg", "wav", "flac", "m4a", "aac", "opus", "mid", "midi",
        // fonts
        "ttf", "otf", "woff", "woff2",
        // video
        "mp4", "webm", "mov",
    )

    /** Android image and font folders: every file in them is an asset, XML vectors and font descriptions included. */
    private val assetFolder = Regex("""[^/]+/src/[^/]+/res/(drawable|mipmap|font)[^/]*/[^/]+""")

    /** Screenshot-test references: drawn by the app's own code. */
    private const val SCREENSHOTS = "app/src/test/screenshots/"

    private val pathCell = Regex("^`([^`]+)`$")
    private val unescapedPipe = Regex("""(?<!\\)\|""")

    fun isAsset(path: String): Boolean =
        !path.startsWith(SCREENSHOTS) &&
            (path.substringAfterLast('.', "").lowercase() in assetTypes || assetFolder.matches(path))

    /** A register row: its line in the file, its path pattern (or null if the first cell is not one) and its cells. */
    class Row(val line: Int, val pattern: String?, val cells: List<String>) {
        val regex: Regex? = pattern?.let { p -> Regex(p.split('*').joinToString("[^/]*") { Regex.escape(it) }) }
    }

    /** The table rows under the "## Register" heading, without the header and separator rows. */
    fun rows(register: String): List<Row>? {
        val lines = register.lines()
        val start = lines.indexOfFirst { it.trim() == "## Register" }
        if (start < 0) return null
        val rows = mutableListOf<Row>()
        for (index in start + 1 until lines.size) {
            val line = lines[index].trim()
            if (line.startsWith("## ")) break
            if (!line.startsWith("|")) continue
            val cells = line.removePrefix("|").removeSuffix("|").split(unescapedPipe).map { it.trim() }
            if (cells.first() == columns.first() || cells.all { it.matches(Regex(":?-+:?")) }) continue
            rows += Row(index + 1, pathCell.matchEntire(cells.first())?.groupValues?.get(1), cells)
        }
        return rows
    }

    /** Every problem with [files] (paths from the repository root) and the [register] text; empty when all is well. */
    fun problems(files: List<String>, register: String): List<String> {
        val rows = rows(register) ?: return listOf("$PATH: no \"## Register\" heading")
        val problems = mutableListOf<String>()
        for (row in rows) {
            val at = "$PATH line ${row.line}"
            if (row.cells.size != columns.size) {
                problems += "$at: ${row.cells.size} cells, expected ${columns.size} (${columns.joinToString(", ")})"
                continue
            }
            row.cells.forEachIndexed { column, cell ->
                if (cell.isEmpty()) problems += "$at: empty \"${columns[column]}\""
            }
            val regex = row.regex
            if (regex == null) {
                problems += "$at: the first cell must be one path in backticks"
            } else if (files.none { regex.matches(it) }) {
                problems += "$at: `${row.pattern}` matches no file"
            }
        }
        for (file in files.filter(::isAsset).sorted()) {
            if (rows.none { it.regex?.matches(file) == true }) problems += "$file: asset file with no row in $PATH"
        }
        return problems
    }

    /** The checked-in files plus new ones git does not ignore, as paths from the repository root. */
    fun repositoryFiles(root: File): List<String> {
        val process = ProcessBuilder("git", "ls-files", "-z", "--cached", "--others", "--exclude-standard")
            .directory(root)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        check(process.waitFor() == 0) { "git ls-files failed in $root: $output" }
        // A file deleted from the working tree but not yet from git is no longer there to register.
        return output.split('\u0000').filter { it.isNotEmpty() && File(root, it).isFile }
    }

    /** The repository's root: the nearest folder upwards that holds settings.gradle.kts. */
    fun repositoryRoot(): File =
        generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
            .firstOrNull { File(it, "settings.gradle.kts").isFile }
            ?: error("no settings.gradle.kts above ${System.getProperty("user.dir")}")
}
