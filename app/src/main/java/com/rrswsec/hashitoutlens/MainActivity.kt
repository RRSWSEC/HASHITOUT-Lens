package com.rrswsec.hashitoutlens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rrswsec.hashitoutlens.ui.HashItOutLensApp
import com.rrswsec.hashitoutlens.ui.LensViewModel
import com.rrswsec.hashitoutlens.ui.theme.HashItOutLensTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HashItOutLensTheme {
                val viewModel: LensViewModel = viewModel()
                HashItOutLensApp(viewModel = viewModel)
            }
        }
    }
}
