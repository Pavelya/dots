package dots.cli

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AssetRegisterTest {
    private fun register(vararg rows: String) = """
        # Asset register

        ## Register

        | File | What it is | Tool and plan | Date | Prompt or source | Licence | Author | Edits |
        | --- | --- | --- | --- | --- | --- | --- | --- |
        ${rows.joinToString("\n        ")}
    """.trimIndent()

    private fun row(path: String, prompt: String = "drawn in code") =
        "| `$path` | an icon | Inkscape | 2026-11-07 | $prompt | Ours | the owner | none |"

    @Test
    fun `every asset file in this repository has a complete row, and every row a file`() {
        val root = AssetRegister.repositoryRoot()
        val problems = AssetRegister.problems(
            AssetRegister.repositoryFiles(root),
            File(root, AssetRegister.PATH).readText(),
        )
        assertTrue(problems.isEmpty(), "Asset register problems (see ${AssetRegister.PATH}):\n" + problems.joinToString("\n"))
    }

    @Test
    fun `an unregistered file in a checked-out repository fails the check`() {
        val root = Files.createTempDirectory("asset-register").toFile()
        try {
            fun git(vararg args: String) =
                assertEquals(0, ProcessBuilder("git", *args).directory(root).redirectErrorStream(true).start().waitFor())
            git("init", "-q")
            File(root, "docs").mkdirs()
            File(root, AssetRegister.PATH).writeText(register(row("app/icon.png")))
            File(root, "app/src/main/res/drawable").mkdirs()
            File(root, "app/icon.png").writeBytes(byteArrayOf(1))
            File(root, "app/src/main/res/drawable/test.png").writeBytes(byteArrayOf(1))

            val problems = AssetRegister.problems(AssetRegister.repositoryFiles(root), File(root, AssetRegister.PATH).readText())
            assertEquals(listOf("app/src/main/res/drawable/test.png: asset file with no row in docs/assets.md"), problems)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `asset files are images, sound, fonts, video and everything in the app's image and font folders`() {
        listOf(
            "docs/design/brand/logo.svg",
            "docs/images/plan.PNG",
            "app/src/main/res/raw/capture.ogg",
            "app/src/main/res/font/notebook.ttf",
            "app/src/main/res/font/notebook.xml",
            "app/src/main/assets/sounds/tap.wav",
            "app/src/main/res/drawable/ring.xml",
            "app/src/main/res/drawable-night/paper.xml",
            "app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml",
            "store/trailer.mp4",
        ).forEach { assertTrue(AssetRegister.isAsset(it), it) }
        listOf(
            "app/src/main/res/values/strings.xml",
            "app/src/main/AndroidManifest.xml",
            "app/src/main/assets/levels/world1.json",
            "app/src/test/screenshots/HomeScreen.png",
            "docs/assets.md",
            "engine/src/main/kotlin/dots/engine/Game.kt",
        ).forEach { assertFalse(AssetRegister.isAsset(it), it) }
    }

    @Test
    fun `a star covers every size of one image, and a pipe in a prompt can be escaped`() {
        val files = listOf("app/src/main/res/mipmap-hdpi/ic_launcher.png", "app/src/main/res/mipmap-xhdpi/ic_launcher.png")
        val problems = AssetRegister.problems(files, register(row("app/src/main/res/mipmap-*/ic_launcher.png", prompt = "a ring \\| a dot")))
        assertEquals(emptyList(), problems)
    }

    @Test
    fun `a star does not cross folders`() {
        val problems = AssetRegister.problems(listOf("docs/a/b.png"), register(row("docs/*.png")))
        assertEquals(
            listOf("docs/assets.md line 7: `docs/*.png` matches no file", "docs/a/b.png: asset file with no row in docs/assets.md"),
            problems,
        )
    }

    @Test
    fun `rows without a file, with empty cells, with missing cells or without a path are reported`() {
        val problems = AssetRegister.problems(
            listOf("a.png"),
            register(
                row("a.png"),
                row("gone.png"),
                "| `a.png` | an icon |  | 2026-11-07 | drawn | Ours | the owner | none |",
                "| `a.png` | an icon | Inkscape |",
                "| a.png | an icon | Inkscape | 2026-11-07 | drawn | Ours | the owner | none |",
            ),
        )
        assertEquals(
            listOf(
                "docs/assets.md line 8: `gone.png` matches no file",
                "docs/assets.md line 9: empty \"Tool and plan\"",
                "docs/assets.md line 10: 3 cells, expected 8 (File, What it is, Tool and plan, Date, Prompt or source, Licence, Author, Edits)",
                "docs/assets.md line 11: the first cell must be one path in backticks",
            ),
            problems,
        )
    }

    @Test
    fun `a register without its heading is reported`() {
        assertEquals(listOf("docs/assets.md: no \"## Register\" heading"), AssetRegister.problems(emptyList(), "# Asset register\n"))
    }
}
