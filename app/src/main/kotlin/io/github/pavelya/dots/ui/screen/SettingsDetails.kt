package io.github.pavelya.dots.ui.screen

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.pavelya.dots.R
import io.github.pavelya.dots.data.Credits
import io.github.pavelya.dots.ui.theme.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A page Settings opens, on the same paper (the wireframes' frame "Settings details"): Privacy,
 * Credits, the open-source licences, and stand-ins for Send feedback (A-12) and the Play Games
 * backup (O-02). Credits opens the licences with [onPage].
 */
@Composable
fun SettingsDetailsScreen(page: SettingsPage, onPage: (SettingsPage) -> Unit, onBack: () -> Unit) {
    when (page) {
        SettingsPage.FEEDBACK -> PaperPage(stringResource(R.string.settings_feedback), onBack, stringResource(R.string.back_to_settings)) {
            IntroCard(stringResource(R.string.stand_in, "A-12"))
        }
        SettingsPage.PRIVACY -> PaperPage(stringResource(R.string.settings_privacy), onBack, stringResource(R.string.back_to_settings)) {
            IntroCard(stringResource(R.string.privacy_intro))
        }
        SettingsPage.BACKUP -> PaperPage(stringResource(R.string.settings_backup), onBack, stringResource(R.string.back_to_settings)) {
            IntroCard(stringResource(R.string.stand_in, "O-02"))
        }
        SettingsPage.CREDITS -> PaperPage(stringResource(R.string.credits_title), onBack, stringResource(R.string.back_to_settings)) {
            InfoRow(stringResource(R.string.credits_made_by), Credits.MADE_BY)
            AssetCredits(R.string.credits_fonts, Credits.fonts)
            AssetCredits(R.string.credits_sounds, Credits.sounds)
            LinkRow(stringResource(R.string.licences_title)) { onPage(SettingsPage.LICENCES) }
        }
        SettingsPage.LICENCES -> PaperPage(stringResource(R.string.licences_title), onBack) {
            IntroCard(stringResource(R.string.licences_intro))
            for (library in Credits.libraries) InfoRow(library.name, library.licence.title)
            for (licence in Credits.licences) {
                SectionHead(licence.title)
                LicenceText(licence)
            }
        }
    }
}

/** A heading ([title]) and one row per font, sound or music track; nothing when the app ships none. */
@Composable
private fun AssetCredits(@StringRes title: Int, assets: List<Credits.Asset>) {
    if (assets.isEmpty()) return
    SectionHead(stringResource(title))
    for (asset in assets) {
        InfoRow(asset.name, listOf(asset.source, asset.licence?.title ?: stringResource(R.string.credits_ours)).joinToString(" · "))
    }
}

/** The full text of [licence], from the app's assets, on a white card. */
@Composable
private fun LicenceText(licence: Credits.Licence) {
    val style = Palette.current
    val context = LocalContext.current
    val text by produceState<String?>(null, licence) {
        value = withContext(Dispatchers.IO) {
            context.assets.open(licence.file).bufferedReader().use { reflow(it.readText()) }
        }
    }
    SelectionContainer {
        Text(
            text.orEmpty(),
            style = NoteText.small.copy(fontSize = 12.sp),
            color = style.pencil,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .shadow(2.dp, RectangleShape, clip = false)
                .background(style.card)
                .padding(12.dp),
        )
    }
}

/**
 * A plain-text licence with its lines broken for 80 columns, as paragraphs that wrap to the
 * phone's width: each run of lines between blank lines becomes one line.
 */
internal fun reflow(text: String): String =
    text.split(Regex("""\n\s*\n"""))
        .map { paragraph -> paragraph.lines().joinToString(" ") { it.trim() }.trim() }
        .filter { it.isNotEmpty() }
        .joinToString("\n\n")
