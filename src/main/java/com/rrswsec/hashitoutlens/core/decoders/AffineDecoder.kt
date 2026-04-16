package com.rrswsec.hashitoutlens.core.decoders

import com.rrswsec.hashitoutlens.core.Decoder
import com.rrswsec.hashitoutlens.core.TextScorer
import com.rrswsec.hashitoutlens.core.model.DecodeFinding
import com.rrswsec.hashitoutlens.core.model.FindingType


class AffineDecoder : Decoder {
    private val keys = listOf(3 to 7, 5 to 8, 7 to 3, 9 to 2, 11 to 5, 25 to 1)

    override fun decode(input: String): List<DecodeFinding> {
        if (!input.any { it.isLetter() }) return emptyList()
        return keys.mapNotNull { (a, b) ->
            val inv = modInverse(a, 26) ?: return@mapNotNull null
            val decoded = input.map { c ->
                if (!c.isLetter()) c else {
                    val base = if (c.isUpperCase()) 'A'.code else 'a'.code
                    (((inv * ((c.code - base - b + 26) % 26)) % 26) + base).toChar()
                }
            }.joinToString("")
            val score = TextScorer.score(decoded)
            DecodeFinding(
                method = "Affine a=$a b=$b",
                resultText = decoded,
                confidence = TextScorer.confidence(score),
                score = score,
                note = "Affine substitution candidate.",
                why = "Tried a common invertible affine key pair.",
                chain = listOf("affine_${a}_$b"),
                findingType = FindingType.DECODE,
                family = "Affine",
            )
        }
    }

    private fun modInverse(a: Int, m: Int): Int? = (1 until m).firstOrNull { (a * it) % m == 1 }
}
