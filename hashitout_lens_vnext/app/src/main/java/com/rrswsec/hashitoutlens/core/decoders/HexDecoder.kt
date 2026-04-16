package com.rrswsec.hashitoutlens.core.decoders

import com.rrswsec.hashitoutlens.core.Decoder
import com.rrswsec.hashitoutlens.core.TextScorer
import com.rrswsec.hashitoutlens.core.model.DecodeFinding

class HexDecoder : Decoder {
    override fun decode(input: String): List<DecodeFinding> {
        val compact = input.trim().replace("0x", "").replace("\\x", "").replace(" ", "").replace("\n", "")
        if (compact.length < 6 || compact.length % 2 != 0 || !compact.matches(Regex("^[0-9a-fA-F]+$"))) return emptyList()
        val decoded = runCatching {
            compact.chunked(2).map { it.toInt(16).toByte() }.toByteArray().toString(Charsets.UTF_8).trim()
        }.getOrElse { return emptyList() }
        val score = TextScorer.score(decoded)
        return if (decoded.isNotBlank() && score >= 14.0) {
            listOf(
                DecodeFinding(
                    method = "hex",
                    resultText = decoded,
                    confidence = TextScorer.confidence(score),
                    score = score,
                    note = "raw bytes from hex pairs",
                    why = "hex pairs mapped into text that looks deliberate.",
                    chain = listOf("hex"),
                    family = "hex",
                )
            )
        } else emptyList()
    }
}
