package com.rrswsec.hashitoutlens.core.decoders

import com.rrswsec.hashitoutlens.core.Decoder
import com.rrswsec.hashitoutlens.core.TextScorer
import com.rrswsec.hashitoutlens.core.model.DecodeFinding
import com.rrswsec.hashitoutlens.core.model.FindingType


class ReverseDecoder : Decoder {
    override fun decode(input: String): List<DecodeFinding> {
        val reversed = input.reversed()
        val score = TextScorer.score(reversed) + 1
        return listOf(
            DecodeFinding(
                method = "Reverse",
                resultText = reversed,
                confidence = TextScorer.confidence(score),
                score = score,
                note = "Input read backwards.",
                why = "Useful when ciphertext is simply reversed before another transform.",
                chain = listOf("reverse"),
                findingType = FindingType.DECODE,
                family = "Reverse",
            )
        )
    }
}
