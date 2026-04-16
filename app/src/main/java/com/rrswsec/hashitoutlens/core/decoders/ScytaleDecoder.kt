package com.rrswsec.hashitoutlens.core.decoders

import com.rrswsec.hashitoutlens.core.Decoder
import com.rrswsec.hashitoutlens.core.TextScorer
import com.rrswsec.hashitoutlens.core.model.DecodeFinding

class ScytaleDecoder : Decoder {
    override fun decode(input: String): List<DecodeFinding> {
        val text = input.replace(Regex("\\s"), "")
        if (text.length < 6) return emptyList()

        val results = mutableListOf<DecodeFinding>()
        // Brute force diameter (number of rows)
        for (diameter in 2..minOf(text.length / 2, 12)) {
            val rows = diameter
            val cols = (text.length + rows - 1) / rows
            val sb = StringBuilder()
            
            // Re-read columns to rows
            for (c in 0 until cols) {
                for (r in 0 until rows) {
                    val idx = r * cols + c
                    if (idx < text.length) sb.append(text[idx])
                }
            }
            
            val decoded = sb.toString()
            val score = TextScorer.score(decoded)
            if (score >= 40.0) {
                results.add(
                    DecodeFinding(
                        method = "Scytale (d=$diameter)",
                        resultText = decoded,
                        confidence = TextScorer.confidence(score),
                        score = score,
                        family = "scytale",
                        why = "Spiral transposition wrap matched English word density.",
                        chain = listOf("scytale")
                    )
                )
            }
        }
        return results.sortedByDescending { it.score }.take(3)
    }
}
