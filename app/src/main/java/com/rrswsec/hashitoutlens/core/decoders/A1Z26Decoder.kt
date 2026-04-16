package com.rrswsec.hashitoutlens.core.decoders

import com.rrswsec.hashitoutlens.core.Decoder
import com.rrswsec.hashitoutlens.core.TextScorer
import com.rrswsec.hashitoutlens.core.model.DecodeFinding

class A1Z26Decoder : Decoder {
    override fun decode(input: String): List<DecodeFinding> {
        val tokens = input.trim().split(Regex("[\\s,.-]+"))
        if (tokens.size < 2 || tokens.any { it.toIntOrNull() !in 1..26 }) return emptyList()
        val decoded = tokens.mapNotNull { it.toIntOrNull()?.let { num -> ('A'.code + num - 1).toChar() } }.joinToString("")
        val score = TextScorer.score(decoded)
        return if (score >= 40.0) {
            listOf(
                DecodeFinding(
                    method = "a1z26",
                    resultText = decoded,
                    confidence = TextScorer.confidence(score),
                    score = score,
                    note = "number alphabet",
                    why = "1 through 26 turned into a sane alphabet lane.",
                    chain = listOf("a1z26"),
                    family = "a1z26",
                )
            )
        } else emptyList()
    }
}
