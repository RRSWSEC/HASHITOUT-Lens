package com.rrswsec.hashitoutlens.core.decoders

import com.rrswsec.hashitoutlens.core.Decoder
import com.rrswsec.hashitoutlens.core.TextScorer
import com.rrswsec.hashitoutlens.core.model.DecodeFinding
import com.rrswsec.hashitoutlens.core.model.FindingType


import com.rrswsec.hashitoutlens.core.EnglishWords

class VigenereDecoder : Decoder {
    override fun decode(input: String): List<DecodeFinding> {
        if (!input.any { it.isLetter() }) return emptyList()
        return EnglishWords.likelyKeys.map { key ->
            val decoded = decodeWithKey(input, key)
            val score = TextScorer.score(decoded) + if (decoded.lowercase().contains(key)) -2 else 0
            DecodeFinding(
                method = "Vigenere key=$key",
                resultText = decoded,
                confidence = TextScorer.confidence(score),
                score = score,
                note = "Common-key Vigenere check.",
                why = "Tried a likely key from the built-in wordlist.",
                chain = listOf("vigenere_$key"),
                findingType = FindingType.DECODE,
                family = "Vigenere",
            )
        }
    }

    private fun decodeWithKey(text: String, key: String): String {
        val lowered = key.lowercase()
        var idx = 0
        return text.map { c ->
            if (!c.isLetter()) c else {
                val shift = lowered[idx % lowered.length] - 'a'
                idx++
                val base = if (c.isUpperCase()) 'A'.code else 'a'.code
                (((c.code - base - shift + 26) % 26) + base).toChar()
            }
        }.joinToString("")
    }
}
