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
        return if (score >= 40.0) {
            listOf(
                DecodeFinding(
                    method = "ROT47",
                    resultText = decoded,
                    confidence = com.rrswsec.hashitoutlens.core.model.Confidence.MEDIUM,
                    score = score,
                    note = "Full ASCII character rotation (94-char set)",
                    why = "ROT47 character mapping revealed human-readable text from the printable ASCII range.",
                    chain = listOf("rot47"),
                    family = "rot",
                )
            )
        } else emptyList()
    }
}
