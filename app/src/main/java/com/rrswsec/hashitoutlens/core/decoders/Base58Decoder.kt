package com.rrswsec.hashitoutlens.core.decoders

import com.rrswsec.hashitoutlens.core.Decoder
import com.rrswsec.hashitoutlens.core.TextScorer
import com.rrswsec.hashitoutlens.core.model.DecodeFinding
import java.math.BigInteger

class Base58Decoder : Decoder {
    private val ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"

    override fun decode(input: String): List<DecodeFinding> {
        val clean = input.trim().replace(Regex("\\s"), "")
        if (clean.length < 4 || !clean.all { it in ALPHABET }) return emptyList()

        return try {
            var num = BigInteger.ZERO
            for (char in clean) {
                num = num.multiply(BigInteger.valueOf(58)).add(BigInteger.valueOf(ALPHABET.indexOf(char).toLong()))
            }
            
            val bytes = num.toByteArray()
            // Remove leading zero byte added by BigInteger if present
            val stripped = if (bytes.isNotEmpty() && bytes[0] == 0.toByte()) bytes.copyOfRange(1, bytes.size) else bytes
            
            val decoded = String(stripped, Charsets.UTF_8).replace(Regex("[^\\x20-\\x7E]"), " ")
            val score = TextScorer.score(decoded)

            if (score >= 40.0) {
                listOf(
                    DecodeFinding(
                        method = "Base58",
                        resultText = decoded.trim(),
                        confidence = TextScorer.confidence(score),
                        score = score,
                        family = "base58",
                        why = "Base58 numeric conversion revealed text.",
                        chain = listOf("base58")
                    )
                )
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
