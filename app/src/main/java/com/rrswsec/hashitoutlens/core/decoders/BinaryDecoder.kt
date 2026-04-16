package com.rrswsec.hashitoutlens.core.decoders

import com.rrswsec.hashitoutlens.core.Decoder
import com.rrswsec.hashitoutlens.core.TextScorer
import com.rrswsec.hashitoutlens.core.model.DecodeFinding

class BinaryDecoder : Decoder {
    override fun decode(input: String): List<DecodeFinding> {
        val compact = input.trim().replace(" ", "").replace("\n", "")
        if (compact.length < 8 || compact.length % 8 != 0 || compact.any { it != '0' && it != '1' }) return emptyList()
        val decoded = compact.chunked(8).joinToString("") { it.toInt(2).toChar().toString() }.trim()
        val score = TextScorer.score(decoded)
        return if (decoded.isNotBlank() && score >= 40.0) {
            listOf(
                DecodeFinding(
                    method = "binary",
                    resultText = decoded,
                    confidence = TextScorer.confidence(score),
                    score = score,
                    note = "8-bit walk",
                    why = "bit chunks landed in text instead of static.",
                    chain = listOf("binary"),
                    family = "binary",
                )
            )
        } else emptyList()
    }
}
