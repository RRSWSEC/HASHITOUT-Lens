package com.rrswsec.hashitoutlens.core

import com.rrswsec.hashitoutlens.core.decoders.A1Z26Decoder
import com.rrswsec.hashitoutlens.core.decoders.AtbashDecoder
import com.rrswsec.hashitoutlens.core.decoders.Base32Decoder
import com.rrswsec.hashitoutlens.core.decoders.Base64Decoder
import com.rrswsec.hashitoutlens.core.decoders.BinaryDecoder
import com.rrswsec.hashitoutlens.core.decoders.CaesarDecoder
import com.rrswsec.hashitoutlens.core.decoders.HexDecoder
import com.rrswsec.hashitoutlens.core.decoders.HtmlEntityDecoder
import com.rrswsec.hashitoutlens.core.decoders.MorseDecoder
import com.rrswsec.hashitoutlens.core.decoders.PolybiusDecoder
import com.rrswsec.hashitoutlens.core.decoders.RailFenceDecoder
import com.rrswsec.hashitoutlens.core.decoders.ReverseDecoder
import com.rrswsec.hashitoutlens.core.decoders.Rot47Decoder
import com.rrswsec.hashitoutlens.core.decoders.UrlDecoder
import com.rrswsec.hashitoutlens.core.decoders.VigenereDecoder
import com.rrswsec.hashitoutlens.core.model.DecodeFinding
import com.rrswsec.hashitoutlens.core.model.FindingType

object DecoderRegistry {
    private val decoders: List<Decoder> = listOf(
        CaesarDecoder(),
        AtbashDecoder(),
        ReverseDecoder(),
        Rot47Decoder(),
        Base64Decoder(),
        Base32Decoder(),
        HexDecoder(),
        BinaryDecoder(),
        UrlDecoder(),
        HtmlEntityDecoder(),
        MorseDecoder(),
        A1Z26Decoder(),
        RailFenceDecoder(),
        PolybiusDecoder(),
        VigenereDecoder(),
    )

    private val familyCaps = mapOf(
        "rot" to 3,
        "rail-fence" to 3,
        "vigenere" to 4,
        "base64" to 1,
        "base32" to 1,
        "hex" to 1,
        "binary" to 1,
        "atbash" to 1,
        "reverse" to 1,
        "rot47" to 1,
        "polybius" to 1,
        "a1z26" to 1,
        "morse" to 1,
        "url" to 1,
        "html" to 1,
        "encryption-hint" to 6,
        "barcode" to 6,
        "stego" to 8,
    )

    fun runAll(input: String, extraFindings: List<DecodeFinding> = emptyList()): List<DecodeFinding> {
        val normalized = input.trim()
        if (normalized.isBlank() && extraFindings.isEmpty()) return emptyList()

        val merged = mutableListOf<DecodeFinding>()
        merged += extraFindings
        merged += EncryptionIdentifier.identify(normalized)
        if (normalized.isNotBlank()) {
            merged += decoders.flatMap { decoder ->
                runCatching { decoder.decode(normalized) }.getOrDefault(emptyList())
            }
        }

        val deduped = merged
            .filter { it.resultText.isNotBlank() }
            .sortedByDescending { it.score }
            .distinctBy { findingKey(it) }

        val counts = mutableMapOf<String, Int>()
        return deduped.filter { finding ->
            val cap = familyCaps[finding.family] ?: 999
            val count = counts.getOrDefault(finding.family, 0)
            if (count < cap || finding.findingType == FindingType.ENCRYPTION_HINT) {
                counts[finding.family] = count + 1
                true
            } else {
                false
            }
        }.take(50)
    }

    private fun findingKey(finding: DecodeFinding): String {
        val typePrefix = if (finding.findingType == FindingType.ENCRYPTION_HINT) "hint:" else "out:"
        val body = finding.resultText.lowercase().filter(Char::isLetterOrDigit).take(260)
        return typePrefix + body
    }
}
