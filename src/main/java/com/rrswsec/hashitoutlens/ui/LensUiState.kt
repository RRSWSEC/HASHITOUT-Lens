package com.rrswsec.hashitoutlens.ui

import android.net.Uri
import com.rrswsec.hashitoutlens.core.model.DecodeFinding

data class LensUiState(
    val recognizedText: String = "",
    val barcodeTexts: List<String> = emptyList(),
    val hiddenTexts: List<String> = emptyList(),
    val findings: List<DecodeFinding> = emptyList(),
    val importedImageUri: Uri? = null,
    val isAnalyzing: Boolean = false,
    val modeLabel: String = "Live Camera",
)
