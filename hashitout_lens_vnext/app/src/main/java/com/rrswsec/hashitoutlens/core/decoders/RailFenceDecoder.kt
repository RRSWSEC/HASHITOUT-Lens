package com.rrswsec.hashitoutlens.core.decoders

import com.rrswsec.hashitoutlens.core.Decoder
import com.rrswsec.hashitoutlens.core.TextScorer
import com.rrswsec.hashitoutlens.core.model.DecodeFinding

class RailFenceDecoder : Decoder {
    override fun decode(input: String): List<DecodeFinding> {
        if (input.length < 6) return emptyList()
        val results = mutableListOf<DecodeFinding>()
        for (rails in 2..4) {
            val decoded = decodeRailFence(input, rails)
            val score = TextScorer.score(decoded)
            if (score >= 16.0) {
                results += DecodeFinding(
                    method = "rail fence $rails",
                    resultText = decoded,
                    confidence = TextScorer.confidence(score),
                    score = score,
                    note = "zig zag unwrap",
                    why = "$rails rails made the stream feel less broken.",
                    chain = listOf("rail-fence:$rails"),
                    family = "rail-fence",
                )
            }
        }
        return results.sortedByDescending { it.score }
    }

    private fun decodeRailFence(text: String, rails: Int): String {
        val pattern = mutableListOf<Int>()
        var rail = 0
        var direction = 1
        repeat(text.length) {
            pattern += rail
            if (rail == 0) direction = 1
            if (rail == rails - 1) direction = -1
            rail += direction
        }
        val indices = pattern.indices.sortedBy { pattern[it] }
        val result = CharArray(text.length)
        for ((pos, originalIndex) in indices.withIndex()) {
            result[originalIndex] = text[pos]
        }
        return String(result)
    }
}
