package com.rrswsec.hashitoutlens.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LensViewModel(application: Application) : AndroidViewModel(application) {
    private val liveAnalyzer = LiveAnalyzer()
    private val importedAnalyzer = ImportedImageAnalyzer()
    private val _uiState = MutableStateFlow(LensUiState())
    val uiState: StateFlow<LensUiState> = _uiState.asStateFlow()

    init {
        com.rrswsec.hashitoutlens.core.DecoderRegistry.initialize(application)
    }

    fun onLiveFrame(result: LiveFrameResult) {
        if (_uiState.value.isFrozen) return

        viewModelScope.launch {
            // Check for fuzzy matches in the live view to snap tabs back onto known targets
            val snapFindings = liveAnalyzer.analyze(result.text, _uiState.value.lockedFindings)
            
            _uiState.value = _uiState.value.copy(
                recognizedText = result.text,
                barcodeTexts = result.barcodes.distinct(),
                textBlocks = result.textBlocks,
                findings = snapFindings, // Show snapped tabs in live view
                isAnalyzing = false,
                lastFrameBitmap = result.frameBitmap ?: _uiState.value.lastFrameBitmap
            )
        }
    }

    fun toggleFreeze() {
        val currentState = _uiState.value
        val nowFrozen = !currentState.isFrozen
        
        if (!nowFrozen) {
            _uiState.value = currentState.copy(
                isFrozen = false,
                isAnalyzing = false,
                frozenBitmap = null,
                findings = emptyList(),
                modeLabel = "live camera",
                selectionRect = null,
                decodingLog = emptyList()
            )
            return
        }

        // SNAPSHOT
        val snapshot = currentState.lastFrameBitmap
        _uiState.value = currentState.copy(
            isFrozen = true,
            isAnalyzing = true,
            frozenBitmap = snapshot,
            modeLabel = "captured",
            decodingLog = listOf("> initiating forensic capture...")
        )

        // TRIGGER DEEP ANALYSIS
        viewModelScope.launch(Dispatchers.Default) {
            if (snapshot == null) {
                kotlinx.coroutines.delay(100)
            }
            
            val finalSnapshot = _uiState.value.frozenBitmap ?: _uiState.value.lastFrameBitmap
            if (finalSnapshot != null && _uiState.value.frozenBitmap == null) {
                _uiState.value = _uiState.value.copy(frozenBitmap = finalSnapshot)
            }

            // FILTER: Only decode text blocks that are inside the current capture
            // But for the global scan on initial CAP, we take everything.
            val combined = (currentState.recognizedText + "\n" + currentState.barcodeTexts.joinToString("\n")).trim()
            
            // Global Analysis with Live Log
            val globalFindings = DecoderRegistry.runAll(combined, depth = 3, onProgress = { log ->
                _uiState.value = _uiState.value.copy(decodingLog = (_uiState.value.decodingLog + log).takeLast(15))
            })
            
            // Block-level Analysis
            val blockFindings = currentState.textBlocks.map { block ->
                async {
                    DecoderRegistry.runAll(block.text, depth = 3).map { it.copy(originalText = block.text) }
                }
            }.awaitAll().flatten()

            val allFindings = (blockFindings + globalFindings).distinctBy { it.resultText + it.method + it.originalText }

            _uiState.value = _uiState.value.copy(
                findings = allFindings,
                isAnalyzing = false,
                lockedFindings = (currentState.lockedFindings + allFindings.filter { it.score >= 85.0 }).distinctBy { it.resultText },
                decodingLog = _uiState.value.decodingLog + "> scan complete. ${allFindings.size} findings."
            )
        }
    }

    private var selectionJob: kotlinx.coroutines.Job? = null

    fun selectFinding(finding: DecodeFinding) {
        _uiState.value = _uiState.value.copy(selectedFinding = finding)
    }

    fun onSelectionMade(rect: android.graphics.RectF) {
        val currentState = _uiState.value
        if (!currentState.isFrozen || currentState.frozenBitmap == null) return
        
        selectionJob?.cancel()
        
        _uiState.value = currentState.copy(
            selectionRect = rect, 
            isAnalyzing = true,
            decodingLog = listOf("> surgical crop initiated...", "> isolating selection text...")
        )
        
        selectionJob = viewModelScope.launch(Dispatchers.Default) {
            kotlinx.coroutines.delay(50) 
            
            // 1. Get the actual OCR text that falls inside this rectangle from the original scan
            val textInSelection = currentState.textBlocks.filter { block ->
                val norm = block.normalizedBoundingBox ?: return@filter false
                // Check if the block is significantly inside the selection
                rect.contains(norm.centerX(), norm.centerY()) || 
                (norm.left >= rect.left && norm.right <= rect.right && norm.top >= rect.top && norm.bottom <= rect.bottom)
            }.joinToString("\n") { it.text }.trim()

            // 2. Perform a fresh OCR on the cropped bitmap to catch high-res details
            val croppedBitmap = cropBitmap(currentState.frozenBitmap, rect)
            val result = importedAnalyzer.analyzeBitmap(croppedBitmap)
            
            // 3. Combine them, but prioritize the specific isolated text
            val combined = if (textInSelection.isNotBlank()) textInSelection else {
                (result.ocrTexts.joinToString("\n") + "\n" + result.barcodeTexts.joinToString("\n")).trim()
            }
            
            if (combined.isBlank()) {
                _uiState.value = _uiState.value.copy(isAnalyzing = false, decodingLog = _uiState.value.decodingLog + "! no text found in selection")
                return@launch
            }

            _uiState.value = _uiState.value.copy(decodingLog = _uiState.value.decodingLog + "> target: ${combined.take(20)}...")

            // 4. Run ONLY the isolated text through the deep decoder
            val deepFindings = DecoderRegistry.runAll(combined, depth = 3, onProgress = { log ->
                _uiState.value = _uiState.value.copy(decodingLog = (_uiState.value.decodingLog + log).takeLast(15))
            })

            val newLocked = deepFindings.filter { it.score >= 85.0 }
            val currentLocked = _uiState.value.lockedFindings
            val updatedLocked = (currentLocked + newLocked).distinctBy { it.resultText + it.method }

            _uiState.value = _uiState.value.copy(
                findings = deepFindings, 
                selectedFinding = deepFindings.maxByOrNull { it.score },
                isAnalyzing = false, 
                lockedFindings = updatedLocked,
                decodingLog = _uiState.value.decodingLog + "> isolation complete."
            )
        }
    }

    private fun cropBitmap(source: android.graphics.Bitmap, rect: android.graphics.RectF): android.graphics.Bitmap {
        val left = (rect.left * source.width).toInt().coerceIn(0, source.width - 1)
        val top = (rect.top * source.height).toInt().coerceIn(0, source.height - 1)
        val right = (rect.right * source.width).toInt().coerceIn(left + 1, source.width)
        val bottom = (rect.bottom * source.height).toInt().coerceIn(top + 1, source.height)
        return android.graphics.Bitmap.createBitmap(source, left, top, right - left, bottom - top)
    }

    fun setIrlStegoMode() {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(modeLabel = "irl stego", isAnalyzing = true)
        viewModelScope.launch(Dispatchers.Default) {
            val combined = (currentState.recognizedText + "\n" + currentState.barcodeTexts.joinToString("\n")).trim()
            val stegoFindings = DecoderRegistry.runAll(combined)
            _uiState.value = _uiState.value.copy(findings = stegoFindings, isAnalyzing = false)
        }
    }

    fun analyzeImportedImage(context: Context, uri: Uri) {
        _uiState.value = _uiState.value.copy(isAnalyzing = true, importedImageUri = uri, modeLabel = "imported image")
        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) { importedAnalyzer.analyze(context, uri) }

            val newLocked = result.findings.filter { it.confidence == Confidence.HIGH }
            val currentLocked = _uiState.value.lockedFindings
            val updatedLocked = (currentLocked + newLocked).distinctBy { it.resultText + it.method }

            _uiState.value = _uiState.value.copy(
                recognizedText = result.ocrTexts.joinToString("\n"),
                barcodeTexts = result.barcodeTexts,
                hiddenTexts = result.hiddenTexts,
                findings = result.findings,
                isAnalyzing = false,
                lockedFindings = updatedLocked
            )
        }
    }

    fun runDeepDecipher() = runFilter("cipher")
    fun runDeepStego() = runFilter("stego")
    fun runDeepEncryption() = runFilter("encryption")

    fun clearResults() {
        val currentState = _uiState.value
        val findings = currentState.findings
        val locked = currentState.lockedFindings
        
        if (findings.isNotEmpty() || locked.isNotEmpty()) {
            val combined = (findings + locked).distinctBy { it.resultText }
            saveFindingsToLog(combined)
            // Learn from this successful session and persist it
            com.rrswsec.hashitoutlens.core.DecoderRegistry.learnFromVerifiedSession(getApplication(), combined)
        }
        
        _uiState.value = currentState.copy(
            lockedFindings = emptyList(), 
            findings = emptyList(),
            selectedFinding = null,
            recognizedText = "",
            barcodeTexts = emptyList()
        )
    }

    private fun saveFindingsToLog(findings: List<DecodeFinding>) {
        if (findings.isEmpty()) return
        
        val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
        val displayTimestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        
        // Use the best finding's transformation chain as the filename
        val bestFinding = findings.maxByOrNull { it.score }
        val chainName = bestFinding?.method?.replace(" ", "_")?.replace(">", "to") ?: "unknown_chain"
        val fileName = "decipher_${chainName}_${timestamp}.txt"
        
        val logContent = buildString {
            append("--- HASHITOUT LENS DISCOVERY LOG ---\n")
            append("Timestamp: $displayTimestamp\n")
            append("Primary Chain: ${bestFinding?.method ?: "N/A"}\n")
            append("Confidence: ${bestFinding?.score?.toInt() ?: 0}%\n")
            append("=".repeat(40)).append("\n\n")
            
            findings.distinctBy { it.resultText }.forEach { f ->
                append("METHOD:     ${f.method}\n")
                append("RESULT:     ${f.resultText}\n")
                append("ORIGINAL:   ${f.originalText ?: "N/A"}\n")
                append("-".repeat(20)).append("\n")
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<android.app.Application>().applicationContext
                val directory = context.getExternalFilesDir(null)
                val file = java.io.File(directory, fileName)
                file.writeText(logContent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleAutoFreeze() {
        _uiState.value = _uiState.value.copy(isAutoFreezeEnabled = !_uiState.value.isAutoFreezeEnabled)
    }

    fun exportFindings(context: android.content.Context) {
        val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val findings = _uiState.value.lockedFindings
        
        if (findings.isEmpty()) {
            android.widget.Toast.makeText(context, "No high-confidence findings to export", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        val header = "--- HASHITOUT LENS LOG [$timestamp] ---\n" +
                     "Total Findings: ${findings.size}\n" +
                     "=".repeat(40) + "\n\n"

        val text = findings.joinToString("\n" + "-".repeat(40) + "\n") { 
            "METHOD:     ${it.method}\n" +
            "CONFIDENCE: ${it.score.toInt()}%\n" +
            "ORIGINAL:   ${it.originalText ?: "N/A"}\n" +
            "RESULT:     ${it.resultText}\n" +
            "NOTE:       ${it.note}"
        }
        
        val fullExport = header + text
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, fullExport)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "HashItOut Lens Export")
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Export Findings"))
    }

    fun exportProduct(context: android.content.Context) {
        val state = _uiState.value
        val bitmap = state.frozenBitmap ?: state.lastFrameBitmap ?: return
        val rect = state.selectionRect
        val finding = state.selectedFinding ?: state.findings.maxByOrNull { it.score }
        
        if (finding == null && rect == null) {
            android.widget.Toast.makeText(context, "No analysis to export", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val composite = createCompositeBitmap(bitmap, rect, finding, state.decodingLog)
                val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                val fileName = "FORENSIC_PRODUCT_$timestamp.png"
                
                val directory = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
                val file = java.io.File(directory, fileName)
                java.io.FileOutputStream(file).use { out ->
                    composite.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                }
                
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Product saved: $fileName", android.widget.Toast.LENGTH_LONG).show()
                    
                    // Share Intent
                    val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(android.content.Intent.createChooser(intent, "Share Forensic Product"))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Export failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun createCompositeBitmap(
        source: android.graphics.Bitmap, 
        rect: android.graphics.RectF?, 
        finding: com.rrswsec.hashitoutlens.core.model.DecodeFinding?,
        log: List<String>
    ): android.graphics.Bitmap {
        val result = source.copy(android.graphics.Bitmap.Config.ARGB_8888, true)
        val canvas = android.graphics.Canvas(result)
        val w = source.width.toFloat()
        val h = source.height.toFloat()

        if (rect != null) {
            val left = rect.left * w
            val top = rect.top * h
            val right = rect.right * w
            val bottom = rect.bottom * h
            
            // 1. Draw the Selection Box
            val boxPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#A2D9A2") // PhosphorGreen
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = w * 0.005f
            }
            canvas.drawRect(left, top, right, bottom, boxPaint)

            // 2. Draw the Magazine Tab if we have a finding
            if (finding != null) {
                val tabPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    style = android.graphics.Paint.Style.FILL
                    alpha = 240
                }
                
                canvas.save()
                val centerX = (left + right) / 2
                val centerY = (top + bottom) / 2
                canvas.rotate((Math.random() * 4 - 2).toFloat(), centerX, centerY)
                
                canvas.drawRect(left, top, right, bottom, tabPaint)
                
                val textPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.BLACK
                    textSize = (bottom - top) * 0.6f
                    textAlign = android.graphics.Paint.Align.CENTER
                    typeface = android.graphics.Typeface.SANS_SERIF
                }
                
                // Fit text
                var currentSize = textPaint.textSize
                while (textPaint.measureText(finding.resultText) > (right - left) * 0.9f && currentSize > 12f) {
                    currentSize -= 2f
                    textPaint.textSize = currentSize
                }
                
                canvas.drawText(finding.resultText, centerX, centerY - (textPaint.descent() + textPaint.ascent()) / 2, textPaint)
                canvas.restore()
            }

            // 3. Draw the Forensic Log overlayed on the box
            val logBgPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                alpha = 180
            }
            val logTextPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#A2D9A2")
                textSize = (bottom - top) * 0.1f
                typeface = android.graphics.Typeface.MONOSPACE
            }

            val logHeight = (bottom - top) * 0.3f
            canvas.drawRect(left, top, right, top + logHeight, logBgPaint)
            
            var y = top + logTextPaint.textSize * 1.2f
            log.takeLast(5).forEach { line ->
                canvas.drawText(line, left + 10f, y, logTextPaint)
                y += logTextPaint.textSize * 1.1f
            }
        }
        
        return result
    }

    private fun runFilter(family: String) {
        val currentState = _uiState.value
        viewModelScope.launch(Dispatchers.Default) {
            val combined = (currentState.recognizedText + "\n" + currentState.barcodeTexts.joinToString("\n")).trim()
            val findings = DecoderRegistry.runAll(combined).filter { it.family == family || it.family == "encoding" }
            _uiState.value = _uiState.value.copy(findings = findings)
        }
    }
}
