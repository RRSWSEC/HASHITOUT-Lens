package com.rrswsec.hashitoutlens.core.decoders

import com.rrswsec.hashitoutlens.core.Decoder
import com.rrswsec.hashitoutlens.core.TextScorer
import com.rrswsec.hashitoutlens.core.model.DecodeFinding
import com.rrswsec.hashitoutlens.core.model.FindingType


class PolybiusDecoder : Decoder {
    private val grid = "ABCDEFGHIKLMNOPQRSTUVWXYZ"

    override fun decode(input: String): List<DecodeFinding> {
        val clean = input.replace(" ", "")
        if (clean.length < 4 || clean.length % 2 != 0 || !clean.all { it in '1'..'5' }) return emptyList()
        val decoded = clean.chunked(2).joinToString("") {
            val row = it[0].digitToInt() - 1
            val col = it[1].digitToInt() - 1
            grid[(row * 5) + col].toString()
        }
        val score = TextScorer.score(decoded) + 2
        return listOf(
            DecodeFinding(
                method = "Polybius",
                resultText = decoded,
                confidence = TextScorer.confidence(score),
                score = score,
                note = "5x5 Polybius square decode (I/J merged).",
                why = "Pairs fell into the standard 1..5 Polybius coordinate range.",
                chain = listOf("polybius"),
                findingType = FindingType.DECODE,
                family = "Polybius",
            )
        )
    }
}
