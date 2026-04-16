package com.rrswsec.hashitoutlens.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rrswsec.hashitoutlens.analyzer.ImportedImageAnalyzer
import com.rrswsec.hashitoutlens.analyzer.LiveAnalyzer
import com.rrswsec.hashitoutlens.camera.LiveFrameResult
import com.rrswsec.hashitoutlens.core.DecoderRegistry
import com.rrswsec.hashitoutlens.core.model.Confidence
import com.rrswsec.hashitoutlens.core.model.DecodeFinding
import com.rrswsec.hashitoutlens.core.model.FindingType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LensViewModel : ViewModel() {
    private val liveAnalyzer = LiveAnalyzer()
    private val importedAnalyzer = ImportedImageAnalyzer()
    private val _uiState = MutableStateFlow(LensUiState())
    val uiState: StateFlow<LensUiState> = _uiState.asStateFlow()

    fun onLiveFrame(result: LiveFrameResult) {
        val combinedText = buildString {
            append(result.text.trim())
            if (result.barcodes.isNotEmpty()) {
                if (isNotEmpty()) append('\n')
                append(result.barcodes.joinToString("\n"))
            }
        }.trim()
        val extras = result.barcodes.distinct().map {
            DecodeFinding(
                method = "barcode / qr content",
                resultText = it,
                confidence = Confidence.HIGH,
                score = 88.0,
                note = "direct symbol decode",
                why = "ml kit decoded a supported symbol in the live frame.",
                chain = listOf("barcode"),
                findingType = FindingType.BARCODE,
                family = "barcode",
            )
        }
        viewModelScope.launch(Dispatchers.Default) {
            val findings = liveAnalyzer.analyze(combinedText, extras)
            if (findings.isNotEmpty() || combinedText.isNotBlank()) {
                _uiState.value = _uiState.value.copy(
                    recognizedText = result.text,
                    barcodeTexts = result.barcodes.distinct(),
                    hiddenTexts = emptyList(),
                    findings = if (findings.isNotEmpty()) findings else DecoderRegistry.runAll(combinedText, extras),
                    isAnalyzing = false,
                    modeLabel = "live camera",
                )
            }
        }
    }

    fun analyzeImportedImage(context: Context, uri: Uri) {
        _uiState.value = _uiState.value.copy(isAnalyzing = true, importedImageUri = uri, modeLabel = "imported image")
        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) { importedAnalyzer.analyze(context, uri) }
            _uiState.value = _uiState.value.copy(
                recognizedText = result.ocrTexts.joinToString("\n"),
                barcodeTexts = result.barcodeTexts,
                hiddenTexts = result.hiddenTexts,
                findings = result.findings,
                importedImageUri = uri,
                isAnalyzing = false,
                modeLabel = "imported image",
            )
        }
    }
}
