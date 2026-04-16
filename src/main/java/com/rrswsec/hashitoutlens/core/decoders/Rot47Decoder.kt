package com.rrswsec.hashitoutlens.core.decoders

import com.rrswsec.hashitoutlens.core.Decoder
import com.rrswsec.hashitoutlens.core.TextScorer
import com.rrswsec.hashitoutlens.core.model.DecodeFinding
import com.rrswsec.hashitoutlens.core.model.FindingType


class Rot47Decoder : Decoder {
    override fun decode(input: String): List<DecodeFinding> {
        if (!input.any { it.code in 33..126 }) return emptyList()
        val decoded = input.map { c ->
            if (c.code in 33..126) (((c.code - 33 + 47) % 94) + 33).toChar() else c
        }.joinToString("")
        val score = TextScorer.score(decoded)
        return listOf(
            DecodeFinding(
                method = "ROT47",
                resultText = decoded,
                confidence = TextScorer.confidence(score),
                score = score,
                note = "Printable ASCII ROT47 transform.",
                why = "Common CTF/simple obfuscation for punctuation-heavy text.",
                chain = listOf("rot47"),
                findingType = FindingType.DECODE,
                family = "ROT",
            )
        )
    }
}
