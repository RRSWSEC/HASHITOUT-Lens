package com.rrswsec.hashitoutlens.ui

import android.graphics.Bitmap
import android.net.Uri
import com.rrswsec.hashitoutlens.core.model.DecodeFinding

import android.graphics.RectF

data class LensUiState(
    val recognizedText: String = "",
    val barcodeTexts: List<String> = emptyList(),
    val hiddenTexts: List<String> = emptyList(),
    val findings: List<DecodeFinding> = emptyList(),
    val isAnalyzing: Boolean = false,
    val importedImageUri: Uri? = null,
    val modeLabel: String = "live camera",
    val isFrozen: Boolean = false,
    val textBlocks: List<com.rrswsec.hashitoutlens.camera.TextBlockInfo> = emptyList(),
    val frozenBitmap: Bitmap? = null,
    val lastFrameBitmap: Bitmap? = null,
    val selectionRect: RectF? = null,
    val lockedFindings: List<DecodeFinding> = emptyList(),
    val selectedFinding: DecodeFinding? = null,
    val isAutoFreezeEnabled: Boolean = false,
    val decodingLog: List<String> = emptyList(),
)
