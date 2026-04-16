package com.rrswsec.hashitoutlens.core

import com.rrswsec.hashitoutlens.core.model.Confidence
import kotlin.math.abs

object TextScorer {
    private val tetragrams = mapOf(
        "TION" to 6.9, "THER" to 6.7, "THAT" to 6.5, "OFTH" to 6.2, "FTHE" to 6.1,
        "WITH" to 5.9, "INTH" to 5.8, "ATIO" to 5.8, "HERE" to 5.7, "OULD" to 5.6,
        "IGHT" to 5.5, "HAVE" to 5.4, "ETHE" to 5.2, "MENT" to 5.1, "IONS" to 5.0,
        "THIS" to 4.9, "TING" to 4.8, "FROM" to 4.7, "EVER" to 4.6, "THEM" to 4.5,
        "OUGH" to 4.3, "ERES" to 4.2, "ENCE" to 4.2,
    )

    private val englishFreq = mapOf(
        'e' to 0.12702, 't' to 0.09056, 'a' to 0.08167, 'o' to 0.07507, 'i' to 0.06966,
        'n' to 0.06749, 's' to 0.06327, 'h' to 0.06094, 'r' to 0.05987, 'd' to 0.04253,
        'l' to 0.04025, 'c' to 0.02782, 'u' to 0.02758, 'm' to 0.02406, 'w' to 0.02360,
        'f' to 0.02228, 'g' to 0.02015, 'y' to 0.01974, 'p' to 0.01929, 'b' to 0.01492,
        'v' to 0.00978, 'k' to 0.00772, 'j' to 0.00153, 'x' to 0.00150, 'q' to 0.00095,
        'z' to 0.00074,
    )

    /**
     * Scores the text based on its likelihood of being meaningful English.
     * 
     * Components (normalized to 100):
     * 1. Word Density (Max 50): Presence of common English words + verified lexicon hits.
     * 2. Tetragram Scoring (0-20%): Common 4-letter patterns (TION, THER, etc).
     * 3. Frequency Analysis (0-15%): Letter distribution vs English average.
     * 4. IC Analysis (0-15%): Index of Coincidence.
     * 
     * Bonuses:
     * - Statement/Paragraph Bonus (Up to +20): Extra weight for long, coherent English text.
     */
    fun score(text: String): Double {
        if (text.isBlank()) return 0.0
        val normalized = text.trim()
        val lower = normalized.lowercase()
        
        // 1. Word Density (Max 50)
        // Include "i" and "a" as they are valid English words
        val words = lower.split(Regex("[^a-z']+")).filter { 
            it.isNotBlank() && (it.length > 1 || it == "i" || it == "a") 
        }
        if (words.isEmpty()) return 0.0
        
        var commonHits = 0
        words.forEach { word ->
            if (EnglishWords.common.contains(word)) commonHits++
        }
        
        val density = commonHits.toDouble() / words.size.coerceAtLeast(1)
        
        // HEAVY PENALTY for "Single Letter Island" syndrome
        // If the average word length is very low but it's not a common word, it's likely gibberish
        val avgWordLength = words.map { it.length }.average()
        val islandPenalty = if (avgWordLength < 2.5 && density < 0.3) 0.2 else 1.0

        var wordScore = (density * 50.0).coerceIn(0.0, 50.0)

        // 2. Tetragrams (Max 15) - Reduced weight
        val tetraScore = (tetragramBonus(normalized) / 24.0 * 15.0).coerceIn(0.0, 15.0)

        // 3. Frequency (Max 10) - Reduced weight
        val freqScore = (frequencySimilarity(normalized) * 10.0).coerceIn(0.0, 10.0)

        // 4. IC (Max 10) - Reduced weight
        val icValue = calculateIC(normalized)
        val icScore = (icValue * 10.0).coerceIn(0.0, 10.0)

        // Combine
        var finalScore = (wordScore + tetraScore + freqScore + icScore) * islandPenalty

        // 5. Statement/Paragraph Bonus (Power-up)
        // Coherent English sentences get a massive boost
        if (words.size >= 8 && density > 0.6) {
            finalScore += 25.0 
        } else if (words.size >= 5 && density > 0.8) {
            finalScore += 15.0
        }

        // Penalties
        val printableRatio = normalized.count { it.code in 32..126 || it == '\n' || it == '\t' }.toDouble() / normalized.length
        if (printableRatio < 0.9) finalScore *= printableRatio 

        val nonAlpha = normalized.count { !it.isLetterOrDigit() && !it.isWhitespace() }.toDouble() / normalized.length
        // If it's mostly symbols, it's not English
        if (nonAlpha > 0.3) finalScore *= (1.0 - nonAlpha)

        return finalScore.coerceIn(0.0, 100.0)
    }

    private fun calculateIC(text: String): Double {
        val letters = text.lowercase().filter { it in 'a'..'z' }
        if (letters.length < 2) return 0.0
        val counts = letters.groupingBy { it }.eachCount()
        val n = letters.length.toDouble()
        val sum = counts.values.sumOf { it * (it - 1) }.toDouble()
        return (sum / (n * (n - 1))) / 0.0667 // Ratio to English IC
    }

    fun confidence(score: Double): Confidence = when {
        score >= 70.0 -> Confidence.HIGH
        score >= 40.0 -> Confidence.MEDIUM
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
        for ((char, expected) in englishFreq) {
            totalDeviation += abs((actual[char] ?: 0.0) - expected)
        }
        return (1.0 - totalDeviation).coerceIn(0.0, 1.0)
    }
}
