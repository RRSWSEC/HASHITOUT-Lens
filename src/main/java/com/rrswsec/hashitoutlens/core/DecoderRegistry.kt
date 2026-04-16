package com.rrswsec.hashitoutlens.core

import com.rrswsec.hashitoutlens.core.decoders.A1Z26Decoder
import com.rrswsec.hashitoutlens.core.decoders.AffineDecoder
import com.rrswsec.hashitoutlens.core.decoders.AtbashDecoder
import com.rrswsec.hashitoutlens.core.decoders.BaconDecoder
import com.rrswsec.hashitoutlens.core.decoders.Base32Decoder
import com.rrswsec.hashitoutlens.core.decoders.Base64Decoder
import com.rrswsec.hashitoutlens.core.decoders.BinaryDecoder
import com.rrswsec.hashitoutlens.core.decoders.CaesarDecoder
import com.rrswsec.hashitoutlens.core.decoders.HexDecoder
import com.rrswsec.hashitoutlens.core.decoders.HtmlEntityDecoder
import com.rrswsec.hashitoutlens.core.decoders.MorseDecoder
import com.rrswsec.hashitoutlens.core.decoders.NatoDecoder
import com.rrswsec.hashitoutlens.core.decoders.PolybiusDecoder
import com.rrswsec.hashitoutlens.core.decoders.RailFenceDecoder
import com.rrswsec.hashitoutlens.core.decoders.ReverseDecoder
import com.rrswsec.hashitoutlens.core.decoders.Rot47Decoder
import com.rrswsec.hashitoutlens.core.decoders.UrlDecoder
import com.rrswsec.hashitoutlens.core.decoders.VigenereDecoder
import com.rrswsec.hashitoutlens.core.decoders.XorSingleByteDecoder
import com.rrswsec.hashitoutlens.core.model.DecodeFinding
import com.rrswsec.hashitoutlens.core.model.FindingType

object DecoderRegistry {
    private val decoders: List<Decoder> = listOf(
        CaesarDecoder(), AtbashDecoder(), ReverseDecoder(), Rot47Decoder(),
        Base64Decoder(), Base32Decoder(), HexDecoder(), BinaryDecoder(), UrlDecoder(), HtmlEntityDecoder(),
        MorseDecoder(), NatoDecoder(), A1Z26Decoder(), RailFenceDecoder(), PolybiusDecoder(),
        BaconDecoder(), AffineDecoder(), VigenereDecoder(), XorSingleByteDecoder(),
    )

    private val familyCaps = mapOf(
        "ROT" to 2,
        "RailFence" to 3,
        "Affine" to 4,
        "Vigenere" to 4,
        "XOR_single" to 6,
        "Bacon" to 2,
        "Atbash" to 1,
        "Base64" to 1,
        "Base32" to 1,
        "Hex" to 1,
        "Binary" to 1,
        "EncryptionHint" to 5,
        "Stego" to 8,
        "Barcode" to 6,
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
            .distinctBy {
                val typePrefix = if (it.findingType == FindingType.ENCRYPTION_HINT) "hint:" else "out:"
                typePrefix + it.resultText.lowercase().filter(Char::isLetterOrDigit).take(260)
            }

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
        }.take(40)
    }
}
