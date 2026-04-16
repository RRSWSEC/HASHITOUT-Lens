package com.rrswsec.hashitoutlens.core.decoders

import com.rrswsec.hashitoutlens.core.Decoder
import com.rrswsec.hashitoutlens.core.TextScorer
import com.rrswsec.hashitoutlens.core.model.Confidence
import com.rrswsec.hashitoutlens.core.model.DecodeFinding

/**
 * Port of Tap Code decoder. 
 * Detects dot groups (e.g., ".. ...") or number pairs and maps them to a Polybius-like grid.
 */
class TapCodeDecoder : Decoder {
    private val grid = arrayOf(
        charArrayOf('A', 'B', 'C', 'D', 'E'),
        charArrayOf('F', 'G', 'H', 'I', 'J'), // K is often omitted/mapped to C
        charArrayOf('L', 'M', 'N', 'O', 'P'),
        charArrayOf('Q', 'R', 'S', 'T', 'U'),
        charArrayOf('V', 'W', 'X', 'Y', 'Z')
    )

    override fun decode(input: String): List<DecodeFinding> {
        // Clean input: look for patterns like ". .. ... ...." or "2 3 1 4"
        val clean = input.trim()
        
        // Try dot-based tap code
        val dotResult = decodeDots(clean)
        if (dotResult != null) return listOf(dotResult)

        // Try number-based tap code (e.g., "1 3 2 4")
        val numResult = decodeNumbers(clean)
        if (numResult != null) return listOf(numResult)

        return emptyList()
    }

    private fun decodeDots(text: String): DecodeFinding? {
        val groups = text.split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (groups.size < 2 || groups.size % 2 != 0) return null
        if (!groups.all { g -> g.all { it == '.' } }) return null

        val sb = StringBuilder()
        for (i in 0 until groups.size step 2) {
            val r = groups[i].length - 1
            val c = groups[i + 1].length - 1
            if (r in 0..4 && c in 0..4) {
                sb.append(grid[r][c])
            } else return null
        }

        val decoded = sb.toString()
        val score = TextScorer.score(decoded)
        return if (score >= 35.0) {
            createFinding("Tap Code (Dots)", decoded, score)
        } else null
    }

    private fun decodeNumbers(text: String): DecodeFinding? {
        val nums = text.split(Regex("[^0-9]+")).filter { it.isNotEmpty() }
        if (nums.size < 2 || nums.size % 2 != 0) return null
        
        val sb = StringBuilder()
        for (i in 0 until nums.size step 2) {
            val r = nums[i].toIntOrNull()?.minus(1) ?: return null
            val c = nums[i + 1].toIntOrNull()?.minus(1) ?: return null
            if (r in 0..4 && c in 0..4) {
                sb.append(grid[r][c])
            } else return null
        }

        val decoded = sb.toString()
        val score = TextScorer.score(decoded)
        return if (score >= 35.0) {
            createFinding("Tap Code (Numbers)", decoded, score)
        } else null
    }

    private fun createFinding(method: String, decoded: String, score: Double): DecodeFinding {
        return DecodeFinding(
            method = method,
            resultText = decoded,
            confidence = Confidence.HIGH,
            score = score,
            family = "tap",
            why = "Detected Polybius coordinate pairs matching human-readable word patterns.",
            chain = listOf("tap")
        )
    }
}
