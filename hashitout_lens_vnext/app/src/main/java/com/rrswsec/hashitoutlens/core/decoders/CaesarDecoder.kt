package com.rrswsec.hashitoutlens.core.decoders

import com.rrswsec.hashitoutlens.core.Decoder
import com.rrswsec.hashitoutlens.core.TextScorer
import com.rrswsec.hashitoutlens.core.model.DecodeFinding

class CaesarDecoder : Decoder {
    override fun decode(input: String): List<DecodeFinding> {
        val results = mutableListOf<DecodeFinding>()
        for (shift in 1..25) {
            val decoded = buildString {
                input.forEach { char ->
                    append(
                        when {
                            char in 'A'..'Z' -> ((char.code - 'A'.code - shift + 26) % 26 + 'A'.code).toChar()
                            char in 'a'..'z' -> ((char.code - 'a'.code - shift + 26) % 26 + 'a'.code).toChar()
                            else -> char
                        }
                    )
                }
            }
            val score = TextScorer.score(decoded)
            if (score >= 18.0) {
                results += DecodeFinding(
                    method = "caesar shift $shift",
                    resultText = decoded,
                    confidence = TextScorer.confidence(score),
                    score = score,
                    note = "classic rot lane",
                    why = "shift $shift made the text look more human.",
                    chain = listOf("caesar:$shift"),
                    family = "rot",
                )
            }
        }
        return results.sortedByDescending { it.score }
    }
}
