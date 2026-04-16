package com.rrswsec.hashitoutlens.core.decoders

import com.rrswsec.hashitoutlens.core.Decoder
import com.rrswsec.hashitoutlens.core.TextScorer
import com.rrswsec.hashitoutlens.core.model.DecodeFinding

class AtbashDecoder : Decoder {
    override fun decode(input: String): List<DecodeFinding> {
        val decoded = buildString {
            input.forEach { char ->
                append(
                    when {
                        char in 'A'..'Z' -> ('Z'.code - (char.code - 'A'.code)).toChar()
                        char in 'a'..'z' -> ('z'.code - (char.code - 'a'.code)).toChar()
                        else -> char
                    }
                )
            }
        }
        val score = TextScorer.score(decoded)
        return if (score >= 16.0) {
            listOf(
                DecodeFinding(
                    method = "atbash",
                    resultText = decoded,
                    confidence = TextScorer.confidence(score),
                    score = score,
                    note = "mirrored alphabet",
                    why = "the mirrored alphabet pass made this look cleaner.",
                    chain = listOf("atbash"),
                    family = "atbash",
                )
            )
        } else emptyList()
    }
}
