package com.rrswsec.hashitoutlens.core

import com.rrswsec.hashitoutlens.core.model.Confidence
import kotlin.math.abs

object TextScorer {
    private val commonWords = listOf(
        "the", "and", "that", "have", "for", "not", "with", "you", "this", "hello",
        "world", "cipher", "decode", "message", "secret", "hidden", "code", "flag"
    )

    private val tetragrams = mapOf(
        "TION" to 6.9, "THER" to 6.7, "THAT" to 6.5, "WITH" to 5.9,
        "HERE" to 5.7, "MENT" to 5.1, "THIS" to 4.9, "FROM" to 4.7,
    )

    private val englishFreq = mapOf(
        'a' to 0.08167, 'b' to 0.01492, 'c' to 0.02782, 'd' to 0.04253, 'e' to 0.12702,
        'f' to 0.02228, 'g' to 0.02015, 'h' to 0.06094, 'i' to 0.06966, 'j' to 0.00153,
        'k' to 0.00772, 'l' to 0.04025, 'm' to 0.02406, 'n' to 0.06749, 'o' to 0.07507,
        'p' to 0.01929, 'q' to 0.00095, 'r' to 0.05987, 's' to 0.06327, 't' to 0.09056,
        'u' to 0.02758, 'v' to 0.00978, 'w' to 0.02360, 'x' to 0.00150, 'y' to 0.01974,
        'z' to 0.00074,
    )

    fun score(text: String): Double {
        if (text.isBlank()) return 0.0
        val normalized = text.trim()
        val printableRatio = normalized.count { it.code in 32..126 || it == '
' || it == '	' }.toDouble() / normalized.length
        val alphaRatio = normalized.count { it.isLetter() || it.isWhitespace() }.toDouble() / normalized.length
        val words = commonWords.sumOf { if (normalized.lowercase().contains(it)) 5.0 else 0.0 }
        val freq = frequencySimilarity(normalized) * 20.0
        val tetras = tetragramBonus(normalized)
        val punctuationPenalty = if (normalized.count { !it.isLetterOrDigit() && !it.isWhitespace() } > normalized.length * 0.35) -8.0 else 0.0
        val bonus = when {
            normalized.contains('{') && normalized.contains('}') -> 8.0
            normalized.contains("://") -> 6.0
            normalized.split(Regex("\s+")).size >= 3 -> 5.0
            else -> 0.0
        }
        return printableRatio * 25 + alphaRatio * 15 + words + freq + tetras + bonus + punctuationPenalty
    }

    fun confidence(score: Double): Confidence = when {
        score >= 52 -> Confidence.HIGH
        score >= 28 -> Confidence.MEDIUM
        else -> Confidence.LOW
    }

    private fun tetragramBonus(text: String): Double {
        val letters = text.uppercase().filter { it.isLetter() }
        if (letters.length < 4) return 0.0
        return letters.windowed(4).sumOf { tetragrams[it] ?: 0.0 }.coerceAtMost(24.0)
    }

    private fun frequencySimilarity(text: String): Double {
        val letters = text.lowercase().filter { it in 'a'..'z' }
        if (letters.length < 8) return 0.0
        val actual = letters.groupingBy { it }.eachCount().mapValues { it.value.toDouble() / letters.length }
        var totalDeviation = 0.0
        for ((c, expected) in englishFreq) {
            totalDeviation += abs((actual[c] ?: 0.0) - expected)
        }
        return (1.0 - totalDeviation).coerceIn(0.0, 1.0)
    }
}
