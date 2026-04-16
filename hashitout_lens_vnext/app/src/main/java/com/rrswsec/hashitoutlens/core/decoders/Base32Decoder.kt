package com.rrswsec.hashitoutlens.core.decoders

import com.rrswsec.hashitoutlens.core.Decoder
import com.rrswsec.hashitoutlens.core.TextScorer
import com.rrswsec.hashitoutlens.core.model.DecodeFinding

class Base32Decoder : Decoder {
    private val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    override fun decode(input: String): List<DecodeFinding> {
        val compact = input.trim().uppercase().replace("=", "").replace(" ", "")
        if (compact.length < 8 || compact.any { it !in alphabet }) return emptyList()

        val bytes = mutableListOf<Byte>()
        var buffer = 0
        var bitsLeft = 0
        for (char in compact) {
            buffer = (buffer shl 5) or alphabet.indexOf(char)
            bitsLeft += 5
            while (bitsLeft >= 8) {
                bitsLeft -= 8
                bytes += ((buffer shr bitsLeft) and 0xFF).toByte()
            }
        }
        val decoded = bytes.toByteArray().toString(Charsets.UTF_8).trim()
        val score = TextScorer.score(decoded)
        return if (decoded.isNotBlank() && score >= 16.0) {
            listOf(
                DecodeFinding(
                    method = "base32",
                    resultText = decoded,
                    confidence = TextScorer.confidence(score),
                    score = score,
                    note = "five-bit alphabet",
                    why = "base32 opened up into readable text.",
                    chain = listOf("base32"),
                    family = "base32",
                )
            )
        } else emptyList()
    }
}
