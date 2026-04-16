package com.rrswsec.hashitoutlens.core.decoders

import com.rrswsec.hashitoutlens.core.Decoder
import com.rrswsec.hashitoutlens.core.EnglishWords
import com.rrswsec.hashitoutlens.core.TextScorer
import com.rrswsec.hashitoutlens.core.model.DecodeFinding

/**
 * Port of the Python 'decode_bacon_robust'.
 * Handles binary-ish streams (0/1, A/B, Case-based, Vowel-based) and tries multiple variants.
 */
class BaconRobustDecoder : Decoder {
    private val B26 = mapOf(
        "AAAAA" to "A", "AAAAB" to "B", "AAABA" to "C", "AAABB" to "D", "AABAA" to "E",
        "AABAB" to "F", "AABBA" to "G", "AABBB" to "H", "ABAAA" to "I", "ABAAB" to "J",
        "ABABA" to "K", "ABABB" to "L", "ABBAA" to "M", "ABBAB" to "N", "ABBBA" to "O",
        "ABBBB" to "P", "BAAAA" to "Q", "BAAAB" to "R", "BAABA" to "S", "BAABB" to "T",
        "BABAA" to "U", "BABAB" to "V", "BABBA" to "W", "BABBB" to "X", "BBAAA" to "Y",
        "BBAAB" to "Z"
    )

    override fun decode(input: String): List<DecodeFinding> {
        val findings = mutableListOf<DecodeFinding>()
        val streams = getBinaryStreams(input)

        for ((label, ab) in streams) {
            for (offset in 0 until 5) {
                if (ab.length - offset < 10) continue
                val truncated = ab.substring(offset)
                val len = (truncated.length / 5) * 5
                if (len < 10) continue
                val u = truncated.substring(0, len)
                
                // Try normal and swapped
                val variants = listOf(
                    "normal" to u,
                    "swapped" to u.map { if (it == 'A') 'B' else 'A' }.joinToString("")
                )

                for ((vName, vStr) in variants) {
                    val decoded = decodeWithTable(vStr, B26)
                    val score = TextScorer.score(decoded)
                    
                    if (decoded.isNotBlank() && score >= 40.0) {
                        findings.add(
                            DecodeFinding(
                                method = "Bacon ($label, $vName, off=$offset)",
                                resultText = decoded,
                                confidence = TextScorer.confidence(score),
                                score = score,
                                note = "Robust binary stream extraction",
                                why = "mapped binary-like patterns ($label) into Bacon's cipher.",
                                family = "bacon",
                                chain = listOf("bacon")
                            )
                        )
                    }
                }
            }
        }
        return findings.sortedByDescending { it.score }.distinctBy { it.resultText }.take(5)
    }

    private fun decodeWithTable(ab: String, table: Map<String, String>): String {
        val sb = StringBuilder()
        for (i in 0 until ab.length - 4 step 5) {
            val chunk = ab.substring(i, i + 5)
            sb.append(table[chunk] ?: "?")
        }
        return sb.toString()
    }

    private fun getBinaryStreams(text: String): List<Pair<String, String>> {
        val out = mutableListOf<Pair<String, String>>()
        val clean = text.replace(Regex("\\s+"), "")
        
        // 01 stream
        val zerosOnes = text.filter { it == '0' || it == '1' }
        if (zerosOnes.length >= 10) {
            out.add("01" to zerosOnes.map { if (it == '0') 'A' else 'B' }.joinToString(""))
            out.add("10" to zerosOnes.map { if (it == '1') 'A' else 'B' }.joinToString(""))
        }

        // Case-based
        val letters = text.filter { it.isLetter() }
        if (letters.length >= 10 && letters.any { it.isLowerCase() } && letters.any { it.isUpperCase() }) {
            out.add("case" to letters.map { if (it.isLowerCase()) 'A' else 'B' }.joinToString(""))
            out.add("case-inv" to letters.map { if (it.isUpperCase()) 'A' else 'B' }.joinToString(""))
        }
        
        // AB direct
        val abOnly = text.uppercase().filter { it == 'A' || it == 'B' }
        if (abOnly.length >= 10) {
            out.add("AB" to abOnly)
        }

        return out
    }
}
