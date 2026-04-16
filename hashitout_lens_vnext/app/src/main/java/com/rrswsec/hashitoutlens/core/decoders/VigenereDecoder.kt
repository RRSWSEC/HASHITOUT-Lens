package com.rrswsec.hashitoutlens.core.decoders

import com.rrswsec.hashitoutlens.core.Decoder
import com.rrswsec.hashitoutlens.core.TextScorer
import com.rrswsec.hashitoutlens.core.model.DecodeFinding

class VigenereDecoder : Decoder {
    private val commonKeys = listOf(
        "key", "secret", "password", "cipher", "crypto", "hashitout", "hello", "world", "flag"
    )

    override fun decode(input: String): List<DecodeFinding> {
        if (input.count { it.isLetter() } < 6) return emptyList()
        return commonKeys.mapNotNull { key ->
            val decoded = decodeWithKey(input, key)
            val score = TextScorer.score(decoded)
            if (score >= 18.0) {
                DecodeFinding(
                    method = "vigenere $key",
                    resultText = decoded,
                    confidence = TextScorer.confidence(score),
                    score = score,
                    note = "common key pass",
                    why = "the key $key pushed the text into readable shape.",
                    chain = listOf("vigenere:$key"),
                    family = "vigenere",
                )
            } else null
        }.sortedByDescending { it.score }
    }

    private fun decodeWithKey(text: String, key: String): String {
        val lowerKey = key.lowercase()
        var keyIndex = 0
        return buildString {
            text.forEach { char ->
                if (char.isLetter()) {
                    val shift = lowerKey[keyIndex % lowerKey.length] - 'a'
                    val base = if (char.isUpperCase()) 'A' else 'a'
                    append(((char.code - base.code - shift + 26) % 26 + base.code).toChar())
                    keyIndex += 1
                } else {
                    append(char)
                }
            }
        }
    }
}
