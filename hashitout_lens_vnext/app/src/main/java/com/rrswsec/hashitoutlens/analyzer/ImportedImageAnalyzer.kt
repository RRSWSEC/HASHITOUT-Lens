package com.rrswsec.hashitoutlens.analyzer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.rrswsec.hashitoutlens.core.DecoderRegistry
import com.rrswsec.hashitoutlens.core.TextScorer
import com.rrswsec.hashitoutlens.core.model.Confidence
import com.rrswsec.hashitoutlens.core.model.DecodeFinding
import com.rrswsec.hashitoutlens.core.model.FindingType
import com.rrswsec.hashitoutlens.ocr.BarcodeRecognitionEngine
import com.rrswsec.hashitoutlens.ocr.TextRecognitionEngine
import java.io.ByteArrayOutputStream
import java.util.zip.Inflater

class ImportedImageAnalyzer {
    private val textEngine = TextRecognitionEngine()
    private val barcodeEngine = BarcodeRecognitionEngine()

    fun analyze(context: Context, uri: Uri): AnalysisResult {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return AnalysisResult()
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return AnalysisResult()

        val ocrTexts = mutableListOf<String>()
        val barcodeTexts = mutableListOf<String>()
        val extraFindings = mutableListOf<DecodeFinding>()

        val originalImage = InputImage.fromBitmap(bitmap, 0)
        val originalText = runCatching { textEngine.recognize(originalImage) }.getOrDefault("")
        if (originalText.isNotBlank()) ocrTexts += originalText
        barcodeTexts += runCatching { barcodeEngine.recognize(originalImage) }.getOrDefault(emptyList())

        val inverted = invertBitmap(bitmap)
        val invertedImage = InputImage.fromBitmap(inverted, 0)
        val invertedText = runCatching { textEngine.recognize(invertedImage) }.getOrDefault("")
        if (invertedText.isNotBlank() && invertedText != originalText) {
            ocrTexts += invertedText
            extraFindings += stegoFinding(
                method = "inverted image ocr",
                text = invertedText,
                score = 42.0,
                why = "contrast inversion let hidden or faint text breathe a little."
            )
        }
        val invertedBarcodes = runCatching { barcodeEngine.recognize(invertedImage) }.getOrDefault(emptyList())
        invertedBarcodes.filterNot { it in barcodeTexts }.forEach { value ->
            barcodeTexts += value
            extraFindings += barcodeFinding(
                method = "inverted barcode",
                text = value,
                score = 62.0,
                why = "the symbol only clicked once the colors flipped."
            )
        }

        extractPngText(bytes).forEach { text ->
            extraFindings += stegoFinding(
                method = "png text chunk",
                text = text,
                score = 78.0,
                why = "embedded png text metadata was sitting right there."
            )
        }
        extractJpegComments(bytes).forEach { text ->
            extraFindings += stegoFinding(
                method = "jpeg comment",
                text = text,
                score = 76.0,
                why = "jpeg comment bytes held readable text."
            )
        }
        extractLsbText(bitmap).forEach { (label, text) ->
            extraFindings += stegoFinding(
                method = label,
                text = text,
                score = TextScorer.score(text) + 18.0,
                why = "a light lsb pull gave us something that looks intentional."
            )
        }

        barcodeTexts.distinct().forEach { value ->
            extraFindings += barcodeFinding(
                method = "barcode / qr content",
                text = value,
                score = 88.0,
                why = "ml kit pulled a clean symbol decode."
            )
        }

        val combinedInput = buildList {
            addAll(ocrTexts)
            addAll(barcodeTexts)
            addAll(extraFindings.filter { it.findingType == FindingType.STEGO }.map { it.resultText })
        }.joinToString("\n").trim()

        return AnalysisResult(
            ocrTexts = ocrTexts.distinct(),
            barcodeTexts = barcodeTexts.distinct(),
            hiddenTexts = extraFindings.filter { it.findingType == FindingType.STEGO }.map { it.resultText }.distinct(),
            findings = DecoderRegistry.runAll(combinedInput, extraFindings),
        )
    }

    private fun barcodeFinding(method: String, text: String, score: Double, why: String) = DecodeFinding(
        method = method,
        resultText = text,
        confidence = Confidence.HIGH,
        score = score,
        note = "direct symbol decode",
        why = why,
        chain = listOf("barcode"),
        findingType = FindingType.BARCODE,
        family = "barcode",
    )

    private fun stegoFinding(method: String, text: String, score: Double, why: String) = DecodeFinding(
        method = method,
        resultText = text.take(2400),
        confidence = when {
            score >= 72.0 -> Confidence.HIGH
            score >= 40.0 -> Confidence.MEDIUM
            else -> Confidence.LOW
        },
        score = score,
        note = "hidden text or metadata extraction",
        why = why,
        chain = listOf("hidden"),
        findingType = FindingType.STEGO,
        family = "stego",
    )

    private fun invertBitmap(source: Bitmap): Bitmap {
        val out = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        for (x in 0 until source.width) {
            for (y in 0 until source.height) {
                val color = source.getPixel(x, y)
                out.setPixel(
                    x,
                    y,
                    Color.argb(Color.alpha(color), 255 - Color.red(color), 255 - Color.green(color), 255 - Color.blue(color))
                )
            }
        }
        return out
    }

    private fun extractLsbText(bitmap: Bitmap): List<Pair<String, String>> {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val streams = linkedMapOf(
            "lsb r" to pixels.map { Color.red(it) and 1 },
            "lsb g" to pixels.map { Color.green(it) and 1 },
            "lsb b" to pixels.map { Color.blue(it) and 1 },
            "lsb gray" to pixels.map { ((Color.red(it) + Color.green(it) + Color.blue(it)) / 3) and 1 },
        )
        return streams.mapNotNull { (label, bits) ->
            val text = bitsToPrintable(bits)
            if (text.length >= 4 && TextScorer.score(text) >= 18.0) label to text else null
        }
    }

    private fun bitsToPrintable(bits: List<Int>): String {
        val out = StringBuilder()
        val maxBits = minOf(bits.size - (bits.size % 8), 32768)
        for (i in 0 until maxBits step 8) {
            var value = 0
            for (j in 0..7) value = (value shl 1) or bits[i + j]
            if (value == 0) break
            if (value in 9..13 || value in 32..126) out.append(value.toChar()) else if (out.length > 2) break
        }
        return out.toString().trim()
    }

    private fun extractPngText(bytes: ByteArray): List<String> {
        val pngSig = byteArrayOf(137.toByte(), 80, 78, 71, 13, 10, 26, 10)
        if (bytes.size < 8 || !bytes.copyOfRange(0, 8).contentEquals(pngSig)) return emptyList()
        val texts = mutableListOf<String>()
        var pos = 8
        while (pos + 8 < bytes.size) {
            val len = readInt(bytes, pos)
            val type = bytes.copyOfRange(pos + 4, pos + 8).toString(Charsets.US_ASCII)
            val dataStart = pos + 8
            val dataEnd = dataStart + len
            if (dataEnd > bytes.size) break
            val chunk = bytes.copyOfRange(dataStart, dataEnd)
            when (type) {
                "tEXt", "iTXt" -> {
                    val text = chunk.toString(Charsets.UTF_8).replace('\u0000', ' ').trim()
                    if (text.isNotBlank()) texts += text
                }
                "zTXt" -> {
                    runCatching {
                        val compressed = chunk.dropWhile { it.toInt() != 0 }.drop(2).toByteArray()
                        val inflater = Inflater()
                        inflater.setInput(compressed)
                        val buf = ByteArrayOutputStream()
                        val out = ByteArray(1024)
                        while (!inflater.finished()) {
                            val count = inflater.inflate(out)
                            if (count <= 0) break
                            buf.write(out, 0, count)
                        }
                        val text = buf.toByteArray().toString(Charsets.UTF_8).trim()
                        if (text.isNotBlank()) texts += text
                    }
                }
            }
            pos = dataEnd + 4
        }
        return texts.distinct()
    }

    private fun extractJpegComments(bytes: ByteArray): List<String> {
        val out = mutableListOf<String>()
        var pos = 0
        while (pos + 4 < bytes.size) {
            if (bytes[pos] == 0xFF.toByte() && bytes[pos + 1] == 0xFE.toByte()) {
                val len = ((bytes[pos + 2].toInt() and 0xFF) shl 8) or (bytes[pos + 3].toInt() and 0xFF)
                val end = pos + 2 + len
                if (end <= bytes.size) {
                    val text = bytes.copyOfRange(pos + 4, end).toString(Charsets.UTF_8).trim()
                    if (text.isNotBlank()) out += text
                }
                pos = end
            } else {
                pos += 1
            }
        }
        return out.distinct()
    }

    private fun readInt(bytes: ByteArray, start: Int): Int {
        return ((bytes[start].toInt() and 0xFF) shl 24) or
            ((bytes[start + 1].toInt() and 0xFF) shl 16) or
            ((bytes[start + 2].toInt() and 0xFF) shl 8) or
            (bytes[start + 3].toInt() and 0xFF)
    }

    data class AnalysisResult(
        val ocrTexts: List<String> = emptyList(),
        val barcodeTexts: List<String> = emptyList(),
        val hiddenTexts: List<String> = emptyList(),
        val findings: List<DecodeFinding> = emptyList(),
    )
}
