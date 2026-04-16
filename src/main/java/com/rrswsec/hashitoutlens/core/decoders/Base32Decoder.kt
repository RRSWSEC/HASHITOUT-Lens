package com.rrswsec.hashitoutlens.core.decoders

import com.rrswsec.hashitoutlens.core.Decoder
import com.rrswsec.hashitoutlens.core.TextScorer
import com.rrswsec.hashitoutlens.core.model.DecodeFinding
import com.rrswsec.hashitoutlens.core.model.FindingType


import java.util.Locale

class Base32Decoder : Decoder {
    private val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    override fun decode(input: String): List<DecodeFinding> {
        val cleaned = input.uppercase(Locale.US).replace("=", "").replace(" ", "")
        if (cleaned.length < 8 || !cleaned.all { it in alphabet }) return emptyList()
        val bytes = mutableListOf<Byte>()
        var buffer = 0
        var bitsLeft = 0
        for (c in cleaned) {
            buffer = (buffer shl 5) or alphabet.indexOf(c)
            bitsLeft += 5
            if (bitsLeft >= 8) {
                bitsLeft -= 8
                bytes += ((buffer shr bitsLeft) and 0xFF).toByte()
            }
        }
        val decoded = bytes.toByteArray().toString(Charsets.UTF_8)
        val score = TextScorer.score(decoded) + 3
        return listOf(
            DecodeFinding(
                method = "Base32",
                resultText = decoded,
                confidence = TextScorer.confidence(score),
                score = score,
                note = "RFC 4648 Base32 decode.",
                why = "Alphabet and length fit Base32 and decoded cleanly.",
                chain = listOf("base32"),
                findingType = FindingType.DECODE,
                family = "Base32",
            )
        )
    }
}
