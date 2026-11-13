package io.github.pavelya.dots.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.TextStyle
import io.github.pavelya.dots.ui.theme.GameTheme

/** The theme's text styles ([GameTheme.type]) for the screens, notes and cards. */
internal object NoteText {
    val display: TextStyle @Composable @ReadOnlyComposable get() = GameTheme.current.type.display
    val headline: TextStyle @Composable @ReadOnlyComposable get() = GameTheme.current.type.headline
    val title: TextStyle @Composable @ReadOnlyComposable get() = GameTheme.current.type.title
    val score: TextStyle @Composable @ReadOnlyComposable get() = GameTheme.current.type.score
    val body: TextStyle @Composable @ReadOnlyComposable get() = GameTheme.current.type.body
    val label: TextStyle @Composable @ReadOnlyComposable get() = GameTheme.current.type.label
    val small: TextStyle @Composable @ReadOnlyComposable get() = GameTheme.current.type.small
}
