package com.rrswsec.hashitoutlens.core.decoders

import com.rrswsec.hashitoutlens.core.Decoder
import com.rrswsec.hashitoutlens.core.TextScorer
import com.rrswsec.hashitoutlens.core.model.DecodeFinding

class XorDecoder : Decoder {
    override fun decode(input: String): List<DecodeFinding> {
        // We use ISO_8859_1 to treat the string as raw bytes
        val bytes = input.toByteArray(Charsets.ISO_8859_1)
        if (bytes.size < 4) return emptyList()

        val results = mutableListOf<DecodeFinding>()

        // Single-byte XOR brute force
        for (key in 0..255) {
            val xored = ByteArray(bytes.size)
            for (i in bytes.indices) {
                xored[i] = (bytes[i].toInt() xor key).toByte()
            }
            
            // Try to interpret as UTF-8, fallback to printable ASCII if it looks like garbage
            val text = String(xored, Charsets.UTF_8).replace(Regex("[^\\x20-\\x7E\\n\\r\\t]"), " ")
            val score = TextScorer.score(text)

            if (score >= 40.0) {
                results.add(
                    DecodeFinding(
                        method = "XOR (key=0x${key.toString(16).uppercase()})",
                        resultText = text.trim(),
                        confidence = TextScorer.confidence(score),
                        score = score,
                        family = "xor",
                        why = "Single-byte XOR brute force revealed legible content.",
                        chain = listOf("xor")
                    )
                )
            }
        }
        return results.sortedByDescending { it.score }.take(3)
    }
}
