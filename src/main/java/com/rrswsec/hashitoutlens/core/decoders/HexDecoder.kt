package com.rrswsec.hashitoutlens.core.decoders

import com.rrswsec.hashitoutlens.core.Decoder
import com.rrswsec.hashitoutlens.core.TextScorer
import com.rrswsec.hashitoutlens.core.model.DecodeFinding
import com.rrswsec.hashitoutlens.core.model.FindingType


class HexDecoder : Decoder {
    override fun decode(input: String): List<DecodeFinding> {
        val cleaned = input.lowercase().replace("0x", "").replace("\x", "").replace(":", "").replace(" ", "")
        if (cleaned.length < 4 || !cleaned.matches(Regex("^[0-9a-f]+$"))) return emptyList()
        val even = if (cleaned.length % 2 == 0) cleaned else "0$cleaned"
        val bytes = even.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val decoded = bytes.toString(Charsets.UTF_8)
        val score = TextScorer.score(decoded) + 3
        return listOf(
            DecodeFinding(
                method = "Hex",
                resultText = decoded,
                confidence = TextScorer.confidence(score),
                score = score,
                note = "Hex string converted to bytes and UTF-8 text.",
                why = "Input shape strongly matched hexadecimal bytes.",
                chain = listOf("hex"),
                findingType = FindingType.DECODE,
                family = "Hex",
            )
        )
    }
}
