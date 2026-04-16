package com.rrswsec.hashitoutlens.core.decoders

import com.rrswsec.hashitoutlens.core.Decoder
import com.rrswsec.hashitoutlens.core.TextScorer
import com.rrswsec.hashitoutlens.core.model.DecodeFinding

class PolybiusDecoder : Decoder {
    private val grid = "ABCDEFGHIKLMNOPQRSTUVWXYZ"

    override fun decode(input: String): List<DecodeFinding> {
        val compact = input.trim().replace(" ", "")
        if (compact.length < 4 || compact.length % 2 != 0 || compact.any { it !in '1'..'5' }) return emptyList()
        val decoded = buildString {
            compact.chunked(2).forEach { pair ->
                val row = pair[0].digitToInt() - 1
                val col = pair[1].digitToInt() - 1
                val idx = row * 5 + col
                if (idx in grid.indices) append(grid[idx])
            }
        }
        val score = TextScorer.score(decoded)
        return if (score >= 40.0) {
            listOf(
                DecodeFinding(
                    method = "polybius",
                    resultText = decoded,
                    confidence = TextScorer.confidence(score),
                    score = score,
                    note = "5x5 square",
                    why = "pair coordinates settled into letters.",
                    chain = listOf("polybius"),
                    family = "polybius",
                )
            )
        } else emptyList()
    }
}
