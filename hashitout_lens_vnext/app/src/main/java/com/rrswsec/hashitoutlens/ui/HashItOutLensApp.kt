package com.rrswsec.hashitoutlens.ui

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable

@Composable
fun HashItOutLensApp(viewModel: LensViewModel) {
    Surface {
        LensScreen(viewModel = viewModel)
    }
}
