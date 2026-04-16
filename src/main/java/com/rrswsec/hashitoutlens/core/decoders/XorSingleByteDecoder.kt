package com.rrswsec.hashitoutlens.core.decoders

import com.rrswsec.hashitoutlens.core.Decoder
import com.rrswsec.hashitoutlens.core.TextScorer
import com.rrswsec.hashitoutlens.core.model.DecodeFinding
import com.rrswsec.hashitoutlens.core.model.FindingType


class XorSingleByteDecoder : Decoder {
    override fun decode(input: String): List<DecodeFinding> {
        val cleaned = input.lowercase().replace(" ", "")
        if (!cleaned.matches(Regex("^[0-9a-f]{8,}$")) || cleaned.length % 2 != 0) return emptyList()
        val bytes = cleaned.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        return (0..255).map { key ->
            val decodedBytes = bytes.map { (it.toInt() xor key).toByte() }.toByteArray()
            val decoded = decodedBytes.toString(Charsets.UTF_8)
            val score = TextScorer.score(decoded)
            DecodeFinding(
                method = "XOR single-byte 0x${key.toString(16).padStart(2, '0')}",
                resultText = decoded,
                confidence = TextScorer.confidence(score),
                score = score,
                note = "Single-byte XOR brute-force against hex input.",
                why = "The input looked like raw bytes; this key produced printable text.",
                chain = listOf("xor_${key.toString(16)}"),
                findingType = FindingType.DECODE,
                family = "XOR_single",
            )
        }.filter { it.score >= 20 }
    }
}
