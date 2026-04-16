package com.rrswsec.hashitoutlens.core.decoders

import com.rrswsec.hashitoutlens.core.Decoder
import com.rrswsec.hashitoutlens.core.TextScorer
import com.rrswsec.hashitoutlens.core.model.DecodeFinding
import com.rrswsec.hashitoutlens.core.model.FindingType


class NatoDecoder : Decoder {
    private val table = mapOf(
        "ALPHA" to "A", "BRAVO" to "B", "CHARLIE" to "C", "DELTA" to "D", "ECHO" to "E",
        "FOXTROT" to "F", "GOLF" to "G", "HOTEL" to "H", "INDIA" to "I", "JULIET" to "J",
        "KILO" to "K", "LIMA" to "L", "MIKE" to "M", "NOVEMBER" to "N", "OSCAR" to "O",
        "PAPA" to "P", "QUEBEC" to "Q", "ROMEO" to "R", "SIERRA" to "S", "TANGO" to "T",
        "UNIFORM" to "U", "VICTOR" to "V", "WHISKEY" to "W", "XRAY" to "X", "YANKEE" to "Y", "ZULU" to "Z"
    )

    override fun decode(input: String): List<DecodeFinding> {
        val words = input.uppercase().split(Regex("\s+")).filter { it.isNotBlank() }
        if (words.isEmpty() || words.any { it !in table }) return emptyList()
        val decoded = words.joinToString("") { table[it].orEmpty() }
        val score = TextScorer.score(decoded) + 3
        return listOf(
            DecodeFinding(
                method = "NATO phonetic",
                resultText = decoded,
                confidence = TextScorer.confidence(score),
                score = score,
                note = "NATO alphabet words collapsed to letters.",
                why = "Every token matched a valid NATO phonetic word.",
                chain = listOf("nato"),
                findingType = FindingType.DECODE,
                family = "Morse",
            )
        )
    }
}
