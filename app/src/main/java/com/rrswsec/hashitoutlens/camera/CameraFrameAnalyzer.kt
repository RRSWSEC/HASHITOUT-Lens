package com.rrswsec.hashitoutlens.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.rrswsec.hashitoutlens.ocr.BarcodeRecognitionEngine
import com.rrswsec.hashitoutlens.ocr.TextRecognitionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean

class CameraFrameAnalyzer(
    private val onFrameResult: (LiveFrameResult) -> Unit,
) : ImageAnalysis.Analyzer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val textEngine = TextRecognitionEngine()
    private val barcodeEngine = BarcodeRecognitionEngine()
    private val busy = AtomicBoolean(false)

    // Forensic Cache: Keep the rawest form of the last frame for "Surgical Reconstruction"
    private var lastRawBuffer: ByteArray? = null
    private var lastWidth = 0
    private var lastHeight = 0
    private var lastRotation = 0

    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return
        }
        if (!busy.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val image = InputImage.fromMediaImage(mediaImage, rotationDegrees)
        
        scope.launch {
            try {
                val visionText = runCatching { textEngine.process(image) }.getOrNull()
                val barcodes = runCatching { barcodeEngine.recognize(image) }.getOrDefault(emptyList())
                
                val textBlocks = visionText?.textBlocks?.map { block ->
                    val rect = block.boundingBox ?: Rect()
                    
                    // ML Kit coordinates are relative to the InputImage. 
                    // We need to normalize them based on the image size AFTER rotation for the UI.
                    val isRotated = rotationDegrees == 90 || rotationDegrees == 270
                    val w = if (isRotated) imageProxy.height else imageProxy.width
                    val h = if (isRotated) imageProxy.width else imageProxy.height

                    val normalizedRect = when (rotationDegrees) {
                        90 -> {
                            android.graphics.RectF(
                                1f - (rect.bottom.toFloat() / imageProxy.height),
                                rect.left.toFloat() / imageProxy.width,
                                1f - (rect.top.toFloat() / imageProxy.height),
                                rect.right.toFloat() / imageProxy.width
                            )
                        }
                        270 -> {
                            android.graphics.RectF(
                                rect.top.toFloat() / imageProxy.height,
                                1f - (rect.right.toFloat() / imageProxy.width),
                                rect.bottom.toFloat() / imageProxy.height,
                                1f - (rect.left.toFloat() / imageProxy.width)
                            )
                        }
                        180 -> {
                            android.graphics.RectF(
                                1f - (rect.right.toFloat() / imageProxy.width),
                                1f - (rect.bottom.toFloat() / imageProxy.height),
                                1f - (rect.left.toFloat() / imageProxy.width),
                                1f - (rect.top.toFloat() / imageProxy.height)
                            )
                        }
                        else -> {
                            android.graphics.RectF(
                                rect.left.toFloat() / imageProxy.width,
                                rect.top.toFloat() / imageProxy.height,
                                rect.right.toFloat() / imageProxy.width,
                                rect.bottom.toFloat() / imageProxy.height
                            )
                        }
                    }
                    
                    TextBlockInfo(
                        text = block.text,
                        boundingBox = rect,
                        normalizedBoundingBox = normalizedRect,
                        lines = block.lines.map { line ->
                            TextLineInfo(text = line.text, boundingBox = line.boundingBox)
                        }
                    )
                } ?: emptyList()

                val fullText = visionText?.text ?: ""
                
                // PERFORMANCE FIX: Update the raw buffer every frame (fast), 
                // but only generate the Bitmap for the UI occasionally or if requested.
                val nv21 = yuv420888ToNv21(imageProxy)
                lastRawBuffer = nv21
                lastWidth = imageProxy.width
                lastHeight = imageProxy.height
                lastRotation = rotationDegrees

                val bitmap = if (shouldGenerateBitmap()) {
                    generateBitmapFromNv21(nv21, lastWidth, lastHeight, lastRotation, quality = 40)
                } else {
                    null
                }

                onFrameResult(LiveFrameResult(
                    text = fullText, 
                    barcodes = barcodes,
                    textBlocks = textBlocks,
                    frameBitmap = bitmap
                ))
            } finally {
                busy.set(false)
                imageProxy.close()
            }
        }
    }

    private var frameCounter = 0
    private fun shouldGenerateBitmap(): Boolean {
        // UI Refresh Rate: Generate a preview bitmap every 5th frame
        return frameCounter++ % 5 == 0
    }

    private fun generateBitmapFromNv21(nv21: ByteArray, width: Int, height: Int, rotation: Int, quality: Int): Bitmap? {
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, width, height), quality, out)
        val imageBytes = out.toByteArray()
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        
        return if (rotation != 0) {
            val matrix = Matrix()
            matrix.postRotate(rotation.toFloat())
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
    }

    /**
     * Public API for the ViewModel to request a high-fidelity reconstruction 
     * of the last captured frame without waiting for the next analyzer cycle.
     */
    fun reconstructLastFrame(): Bitmap? {
        val buffer = lastRawBuffer ?: return null
        return generateBitmapFromNv21(buffer, lastWidth, lastHeight, lastRotation, quality = 100)
    }

    private fun yuv420888ToNv21(image: ImageProxy): ByteArray {
        val pixelCount = image.width * image.height
        val pixelSizeBits = ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888)
        val outputBuffer = ByteArray(pixelCount * pixelSizeBits / 8)
        val planes = image.planes
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer
        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()
        var pos = 0
        for (i in 0 until ySize) {
            outputBuffer[pos++] = yBuffer.get()
        }
        for (i in 0 until vSize step planes[2].pixelStride) {
            outputBuffer[pos++] = vBuffer.get()
            outputBuffer[pos++] = uBuffer.get()
        }
        return outputBuffer
    }
}
