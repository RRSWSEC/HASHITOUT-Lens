package com.rrswsec.hashitoutlens.core.decoders

import com.rrswsec.hashitoutlens.core.Decoder
import com.rrswsec.hashitoutlens.core.TextScorer
import com.rrswsec.hashitoutlens.core.model.Confidence
import com.rrswsec.hashitoutlens.core.model.DecodeFinding

/**
 * Port of Bifid cipher decoder (fractionated transposition).
 * Simplified brute force version for common periods.
 */
class BifidDecoder : Decoder {
    private val grid = arrayOf(
        charArrayOf('A', 'B', 'C', 'D', 'E'),
        charArrayOf('F', 'G', 'H', 'I', 'J'), // I/J typically combined
        charArrayOf('L', 'M', 'N', 'O', 'P'),
        charArrayOf('Q', 'R', 'S', 'T', 'U'),
        charArrayOf('V', 'W', 'X', 'Y', 'Z')
    )

    override fun decode(input: String): List<DecodeFinding> {
        val clean = input.uppercase().filter { it in 'A'..'Z' }.replace('J', 'I')
        if (clean.length < 6) return emptyList()

        val results = mutableListOf<DecodeFinding>()
        
        // Try common periods for CTF challenges
        for (period in listOf(clean.length, 5, 7, 10)) {
            if (period > clean.length) continue
            
            try {
                val decoded = decryptBifid(clean, period)
                val score = TextScorer.score(decoded)
                if (score >= 40.0) {
                    results.add(
                        DecodeFinding(
                            method = "Bifid (p=$period)",
                            resultText = decoded,
                            confidence = Confidence.MEDIUM,
                            score = score,
                            family = "bifid",
                            why = "Detected fractionated transposition matching period $period.",
                            chain = listOf("bifid")
                        )
                    )
                }
            } catch (e: Exception) {}
        }

        return results.sortedByDescending { it.score }.take(2)
    }

    private fun decryptBifid(text: String, period: Int): String {
        val coords = text.map { char ->
            var found: Pair<Int, Int>? = null
            for (r in 0..4) {
                for (c in 0..4) {
                    if (grid[r][c] == char) {
                        found = r to c
                        break
                    }
                }
                if (found != null) break
            }
            found ?: (0 to 0)
        }

        val decoded = StringBuilder()
        for (i in 0 until coords.size step period) {
            val end = minOf(i + period, coords.size)
            val currentBlock = coords.subList(i, end)
            
            val stream = mutableListOf<Int>()
            currentBlock.forEach { stream.add(it.first) }
            currentBlock.forEach { stream.add(it.second) }
            
            for (j in 0 until stream.size step 2) {
                decoded.append(grid[stream[j]][stream[j+1]])
            }
        }
        return decoded.toString()
    }
}
