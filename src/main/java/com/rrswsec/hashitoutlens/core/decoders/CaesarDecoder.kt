package com.rrswsec.hashitoutlens.core.decoders

import com.rrswsec.hashitoutlens.core.Decoder
import com.rrswsec.hashitoutlens.core.TextScorer
import com.rrswsec.hashitoutlens.core.model.DecodeFinding
import com.rrswsec.hashitoutlens.core.model.FindingType


class CaesarDecoder : Decoder {
    override fun decode(input: String): List<DecodeFinding> {
        if (!input.any { it.isLetter() }) return emptyList()
        return (1..25).map { shift ->
            val decoded = input.map { c ->
                if (!c.isLetter()) c else {
                    val base = if (c.isUpperCase()) 'A' else 'a'
                    (((c.code - base.code - shift + 26) % 26) + base.code).toChar()
                }
            }.joinToString("")
            val score = TextScorer.score(decoded)
            DecodeFinding(
                method = "Caesar Shift $shift",
                resultText = decoded,
                confidence = TextScorer.confidence(score),
                score = score,
                note = "ROT/Caesar brute-force candidate.",
                why = "Shift $shift produced printable output with language score %.1f".format(score),
                chain = listOf("caesar_$shift"),
                findingType = FindingType.DECODE,
                family = "ROT",
            )
        }
    }
}
