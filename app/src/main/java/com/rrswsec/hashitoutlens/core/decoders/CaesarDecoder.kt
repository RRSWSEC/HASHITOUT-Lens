package com.rrswsec.hashitoutlens.core.decoders

import com.rrswsec.hashitoutlens.core.Decoder
import com.rrswsec.hashitoutlens.core.TextScorer
import com.rrswsec.hashitoutlens.core.model.DecodeFinding

class CaesarDecoder : Decoder {
    fun shift(input: String, shift: Int): String {
        return buildString {
            input.forEach { char ->
                append(
                    when {
                        char in 'A'..'Z' -> {
                            val base = 'A'.code
                            val offset = (char.code - base - shift) % 26
                            (if (offset < 0) offset + 26 else offset).plus(base).toChar()
                        }
                        char in 'a'..'z' -> {
                            val base = 'a'.code
                            val offset = (char.code - base - shift) % 26
                            (if (offset < 0) offset + 26 else offset).plus(base).toChar()
                        }
                        else -> char
                    }
                )
            }
        }
    }

    override fun decode(input: String): List<DecodeFinding> {
        val results = mutableListOf<DecodeFinding>()
        for (s in 1..25) {
            val decoded = shift(input, s)
            val score = TextScorer.score(decoded)
            if (score >= 40.0) {
                val label = if (s == 13) "ROT13" else "Caesar (+$s)"
                results += DecodeFinding(
                    method = label,
                    resultText = decoded,
                    confidence = com.rrswsec.hashitoutlens.core.model.Confidence.MEDIUM,
                    score = score,
                    note = if (s == 13) "Standard ROT13" else "Caesar shift $s",
                    why = "Alphabetic rotation (shift $s) revealed English-like structure.",
                    chain = listOf("rot:$s"),
                    family = "rot",
                )
            }
        }
        return results.sortedByDescending { it.score }.take(3)
    }
}
