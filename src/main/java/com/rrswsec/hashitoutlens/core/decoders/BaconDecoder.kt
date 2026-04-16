package com.rrswsec.hashitoutlens.core.decoders

import com.rrswsec.hashitoutlens.core.Decoder
import com.rrswsec.hashitoutlens.core.TextScorer
import com.rrswsec.hashitoutlens.core.model.DecodeFinding
import com.rrswsec.hashitoutlens.core.model.FindingType


class BaconDecoder : Decoder {
    override fun decode(input: String): List<DecodeFinding> {
        val cleaned = input.uppercase().replace(" ", "")
        val stream = when {
            cleaned.all { it == 'A' || it == 'B' } -> cleaned
            cleaned.all { it == '0' || it == '1' } -> cleaned.map { if (it == '0') 'A' else 'B' }.joinToString("")
            else -> return emptyList()
        }
        if (stream.length < 5 || stream.length % 5 != 0) return emptyList()
        val decoded = stream.chunked(5).map { chunk ->
            val value = chunk.map { if (it == 'A') '0' else '1' }.joinToString("").toInt(2)
            if (value in 0..25) ('A'.code + value).toChar() else '?'
        }.joinToString("")
        val score = TextScorer.score(decoded).coerceAtMost(49.0)
        return listOf(
            DecodeFinding(
                method = "Bacon",
                resultText = decoded,
                confidence = TextScorer.confidence(score),
                score = score,
                note = "Baconian 5-bit A/B decode.",
                why = "A/B bitstream grouped cleanly into 5-symbol units.",
                chain = listOf("bacon"),
                findingType = FindingType.DECODE,
                family = "Bacon",
            )
        )
    }
}
