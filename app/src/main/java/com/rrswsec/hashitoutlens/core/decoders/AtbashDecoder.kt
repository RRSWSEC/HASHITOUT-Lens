package com.rrswsec.hashitoutlens.core.decoders

import com.rrswsec.hashitoutlens.core.Decoder
import com.rrswsec.hashitoutlens.core.TextScorer
import com.rrswsec.hashitoutlens.core.model.DecodeFinding

class AtbashDecoder : Decoder {
    fun atbash(input: String): String {
        return buildString {
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
    }

    override fun decode(input: String): List<DecodeFinding> {
        val decoded = atbash(input)
        val score = TextScorer.score(decoded)
        return if (score >= 40.0) {
            listOf(
                DecodeFinding(
                    method = "Atbash",
                    resultText = decoded,
                    confidence = com.rrswsec.hashitoutlens.core.model.Confidence.MEDIUM,
                    score = score,
                    note = "Standard alphabet reversal",
                    why = "Reversing the alphabet revealed clear English word distribution.",
                    chain = listOf("atbash"),
                    family = "atbash",
                )
            )
        } else emptyList()
    }
}
