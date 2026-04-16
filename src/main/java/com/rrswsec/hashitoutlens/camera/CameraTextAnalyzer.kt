package com.rrswsec.hashitoutlens.camera

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.rrswsec.hashitoutlens.ocr.BarcodeRecognitionEngine
import com.rrswsec.hashitoutlens.ocr.TextRecognitionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

data class LiveFrameResult(
    val text: String,
    val barcodes: List<String>,
)

class CameraTextAnalyzer(
    private val onFrameResult: (LiveFrameResult) -> Unit,
) : ImageAnalysis.Analyzer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val textEngine = TextRecognitionEngine()
    private val barcodeEngine = BarcodeRecognitionEngine()
    private val busy = AtomicBoolean(false)

    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run {
            imageProxy.close(); return
        }
        if (!busy.compareAndSet(false, true)) {
            imageProxy.close(); return
        }
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scope.launch {
            try {
                val text = runCatching { textEngine.recognize(image) }.getOrDefault("")
                val barcodes = runCatching { barcodeEngine.recognize(image) }.getOrDefault(emptyList())
                if (text.isNotBlank() || barcodes.isNotEmpty()) {
                    onFrameResult(LiveFrameResult(text = text, barcodes = barcodes))
                }
            } finally {
                busy.set(false)
                imageProxy.close()
            }
        }
    }
}
