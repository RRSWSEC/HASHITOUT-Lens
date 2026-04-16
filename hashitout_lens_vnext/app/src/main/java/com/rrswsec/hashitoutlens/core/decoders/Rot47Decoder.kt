package com.rrswsec.hashitoutlens.core.decoders

import com.rrswsec.hashitoutlens.core.Decoder
import com.rrswsec.hashitoutlens.core.TextScorer
import com.rrswsec.hashitoutlens.core.model.DecodeFinding

class Rot47Decoder : Decoder {
    override fun decode(input: String): List<DecodeFinding> {
        val decoded = input.map { c ->
            if (c.code in 33..126) (33 + ((c.code - 33 + 47) % 94)).toChar() else c
        }.joinToString("")
        val score = TextScorer.score(decoded)
        return if (score >= 14.0) {
            listOf(
                DecodeFinding(
                    method = "rot47",
                    resultText = decoded,
                    confidence = TextScorer.confidence(score),
                    score = score,
                    note = "printable ascii flip",
                    why = "rot47 gave us something a human might have meant.",
                    chain = listOf("rot47"),
                    family = "rot47",
                )
            )
        } else emptyList()
    }
}
