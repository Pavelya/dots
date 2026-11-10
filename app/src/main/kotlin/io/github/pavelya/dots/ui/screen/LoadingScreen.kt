package io.github.pavelya.dots.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import io.github.pavelya.dots.R

/** Shown while the saved game is read. The drawn ring and the tip come with A-06. */
@Composable
fun LoadingScreen() {
    val loading = stringResource(R.string.loading)
    Box(Modifier.fillMaxSize().semantics { contentDescription = loading }, contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displaySmall)
    }
}
