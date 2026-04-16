package com.rrswsec.hashitoutlens.core.decoders

import com.rrswsec.hashitoutlens.core.Decoder
import com.rrswsec.hashitoutlens.core.TextScorer
import com.rrswsec.hashitoutlens.core.model.DecodeFinding
import kotlin.math.abs

/**
 * Port of the 10k-line Python script's _recover_vigenere_candidates.
 * Uses Index of Coincidence (IC) to find key lengths and frequency analysis for key recovery.
 * Optimized for speed to handle multiple layers in seconds.
 */
class VigenereBruteDecoder : Decoder {

    override fun decode(input: String): List<DecodeFinding> {
        val alpha = input.filter { it.isLetter() }.uppercase()
        if (alpha.length < 10) return emptyList()

        val results = mutableListOf<DecodeFinding>()
        val periods = estimateKeyLengths(alpha)

        for (period in periods) {
            val key = recoverKey(alpha, period)
            val decoded = decrypt(input, key)
            val score = TextScorer.score(decoded)
            
            if (score > 20.0) {
                results.add(DecodeFinding(
                    resultText = decoded,
                    score = score,
                    confidence = TextScorer.confidence(score),
                    method = "Vigenère (key=\"$key\", period=$period)",
                    chain = listOf("vigenere:$key"),
                    family = "vigenere"
                ))
            }
        }
        return results.sortedByDescending { it.score }.take(5)
    }

    private fun estimateKeyLengths(text: String, maxPeriod: Int = 20): List<Int> {
        val ics = mutableListOf<Pair<Int, Double>>()
        for (p in 1..maxPeriod) {
            var sumIc = 0.0
            for (i in 0 until p) {
                val subset = text.filterIndexed { index, _ -> index % p == i }
                sumIc += calculateIc(subset)
            }
            ics.add(p to (sumIc / p))
        }
        // Return periods with IC closest to English (0.0667)
        return ics.sortedBy { abs(it.second - 0.0667) }.take(4).map { it.first }
    }

    private fun calculateIc(text: String): Double {
        if (text.length < 2) return 0.0
        val counts = text.groupingBy { it }.eachCount()
        val n = text.length.toDouble()
        return counts.values.sumOf { it * (it - 1) }.toDouble() / (n * (n - 1))
    }

    private fun recoverKey(text: String, period: Int): String {
        return (0 until period).map { i ->
            val subset = text.filterIndexed { index, _ -> index % period == i }
            findBestShift(subset)
        }.joinToString("")
    }

    private fun findBestShift(text: String): Char {
        val englishFreqs = doubleArrayOf(
            0.08167, 0.01492, 0.02782, 0.04253, 0.12702, 0.02228, 0.02015,
            0.06094, 0.06966, 0.00153, 0.00772, 0.04025, 0.02406, 0.06749,
            0.07507, 0.01929, 0.00095, 0.05987, 0.06327, 0.09056, 0.02758,
            0.00978, 0.02360, 0.00150, 0.01974, 0.00074
        )
        
        var bestShift = 0
        var minChiSq = Double.MAX_VALUE
        
        for (shift in 0..25) {
            val counts = IntArray(26)
            text.forEach { counts[(it.code - 'A'.code - shift + 26) % 26]++ }
            
            var chiSq = 0.0
            for (i in 0..25) {
                val expected = englishFreqs[i] * text.length
                val observed = counts[i].toDouble()
                chiSq += (observed - expected) * (observed - expected) / expected
            }
            
            if (chiSq < minChiSq) {
                minChiSq = chiSq
                bestShift = shift
            }
        }
        return ('A'.code + bestShift).toChar()
    }

    private fun decrypt(text: String, key: String): String {
        var keyIdx = 0
        return buildString {
            text.forEach { char ->
                if (char.isLetter()) {
                    val base = if (char.isUpperCase()) 'A' else 'a'
                    val shift = key[keyIdx % key.length].uppercaseChar().code - 'A'.code
                    append((((char.code - base.code - shift + 26) % 26) + base.code).toChar())
                    keyIdx++
                } else {
                    append(char)
                }
            }
        }
    }
}
