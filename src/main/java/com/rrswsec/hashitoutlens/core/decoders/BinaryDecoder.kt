package com.rrswsec.hashitoutlens.core.decoders

import com.rrswsec.hashitoutlens.core.Decoder
import com.rrswsec.hashitoutlens.core.TextScorer
import com.rrswsec.hashitoutlens.core.model.DecodeFinding
import com.rrswsec.hashitoutlens.core.model.FindingType


class BinaryDecoder : Decoder {
    override fun decode(input: String): List<DecodeFinding> {
        val cleaned = input.replace(" ", "").replace("
", "")
        if (cleaned.length < 8 || cleaned.length % 8 != 0 || !cleaned.all { it == '0' || it == '1' }) return emptyList()
        val decoded = cleaned.chunked(8).joinToString("") { it.toInt(2).toChar().toString() }
        val score = TextScorer.score(decoded) + 4
        return listOf(
            DecodeFinding(
                method = "Binary bytes",
                resultText = decoded,
                confidence = TextScorer.confidence(score),
                score = score,
                note = "8-bit binary groups decoded as bytes.",
                why = "Input was strict binary and grouped cleanly into bytes.",
                chain = listOf("binary"),
                findingType = FindingType.DECODE,
                family = "Binary",
            )
        )
    }
}
