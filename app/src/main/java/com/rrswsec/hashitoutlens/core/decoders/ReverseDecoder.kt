package com.rrswsec.hashitoutlens.core.decoders

import com.rrswsec.hashitoutlens.core.Decoder
import com.rrswsec.hashitoutlens.core.TextScorer
import com.rrswsec.hashitoutlens.core.model.DecodeFinding

class ReverseDecoder : Decoder {
    override fun decode(input: String): List<DecodeFinding> {
        val decoded = input.reversed()
        val score = TextScorer.score(decoded)
        return if (score >= 40.0) {
            listOf(
                DecodeFinding(
                    method = "reverse",
                    resultText = decoded,
                    confidence = TextScorer.confidence(score),
                    score = score,
                    note = "simple but weirdly common",
                    why = "sometimes the gremlin just flipped the string.",
                    chain = listOf("reverse"),
                    family = "reverse",
                )
            )
        } else emptyList()
    }
}
