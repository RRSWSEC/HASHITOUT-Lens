package com.rrswsec.hashitoutlens.core.decoders

import com.rrswsec.hashitoutlens.core.Decoder
import com.rrswsec.hashitoutlens.core.TextScorer
import com.rrswsec.hashitoutlens.core.model.DecodeFinding
import com.rrswsec.hashitoutlens.core.model.FindingType


class RailFenceDecoder : Decoder {
    override fun decode(input: String): List<DecodeFinding> {
        val text = input.filterNot(Char::isWhitespace)
        if (text.length < 6) return emptyList()
        return (2..6).map { rails ->
            val decoded = decodeRailFence(text, rails)
            val score = TextScorer.score(decoded) + 1
            DecodeFinding(
                method = "Rail Fence $rails",
                resultText = decoded,
                confidence = TextScorer.confidence(score),
                score = score,
                note = "Rail fence transposition brute-force.",
                why = "Reconstructed zig-zag path with $rails rails.",
                chain = listOf("railfence_$rails"),
                findingType = FindingType.DECODE,
                family = "RailFence",
            )
        }
    }

    private fun decodeRailFence(text: String, rails: Int): String {
        val pattern = mutableListOf<Int>()
        var rail = 0
        var direction = 1
        repeat(text.length) {
            pattern += rail
            if (rail == 0) direction = 1 else if (rail == rails - 1) direction = -1
            rail += direction
        }
        val sorted = pattern.withIndex().sortedBy { it.value }
        val result = CharArray(text.length)
        sorted.forEachIndexed { idx, indexedValue -> result[indexedValue.index] = text[idx] }
        return String(result)
    }
}
