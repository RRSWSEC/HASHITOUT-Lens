package com.rrswsec.hashitoutlens.core.decoders

import com.rrswsec.hashitoutlens.core.Decoder
import com.rrswsec.hashitoutlens.core.TextScorer
import com.rrswsec.hashitoutlens.core.model.DecodeFinding
import com.rrswsec.hashitoutlens.core.model.FindingType


class A1Z26Decoder : Decoder {
    override fun decode(input: String): List<DecodeFinding> {
        val parts = input.split(Regex("[\s,.-]+"))
        if (parts.isEmpty() || parts.any { it.isBlank() || it.toIntOrNull() !in 1..26 }) return emptyList()
        val decoded = parts.joinToString("") { ((it.toInt() - 1) + 'A'.code).toChar().toString() }
        val score = TextScorer.score(decoded) + 3
        return listOf(
            DecodeFinding(
                method = "A1Z26",
                resultText = decoded,
                confidence = TextScorer.confidence(score),
                score = score,
                note = "Numbers mapped to alphabet positions.",
                why = "All numeric tokens fell into the 1..26 range.",
                chain = listOf("a1z26"),
                findingType = FindingType.DECODE,
                family = "Numeric",
            )
        )
    }
}
