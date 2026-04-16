package com.rrswsec.hashitoutlens.core.decoders

import com.rrswsec.hashitoutlens.core.Decoder
import com.rrswsec.hashitoutlens.core.TextScorer
import com.rrswsec.hashitoutlens.core.model.DecodeFinding
import com.rrswsec.hashitoutlens.core.model.FindingType


class AtbashDecoder : Decoder {
    override fun decode(input: String): List<DecodeFinding> {
        if (!input.any { it.isLetter() }) return emptyList()
        val decoded = input.map { c ->
            when {
                c in 'A'..'Z' -> ('Z'.code - (c.code - 'A'.code)).toChar()
                c in 'a'..'z' -> ('z'.code - (c.code - 'a'.code)).toChar()
                else -> c
            }
        }.joinToString("")
        val score = TextScorer.score(decoded) + 2
        return listOf(
            DecodeFinding(
                method = "Atbash",
                resultText = decoded,
                confidence = TextScorer.confidence(score),
                score = score,
                note = "Alphabet mirrored end-to-end.",
                why = "Single deterministic classical transform.",
                chain = listOf("atbash"),
                findingType = FindingType.DECODE,
                family = "Atbash",
            )
        )
    }
}
