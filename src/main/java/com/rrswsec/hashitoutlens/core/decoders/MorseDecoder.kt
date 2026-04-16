package com.rrswsec.hashitoutlens.core.decoders

import com.rrswsec.hashitoutlens.core.Decoder
import com.rrswsec.hashitoutlens.core.TextScorer
import com.rrswsec.hashitoutlens.core.model.DecodeFinding
import com.rrswsec.hashitoutlens.core.model.FindingType


class MorseDecoder : Decoder {
    private val table = mapOf(
        ".-" to "A", "-..." to "B", "-.-." to "C", "-.." to "D", "." to "E", "..-." to "F",
        "--." to "G", "...." to "H", ".." to "I", ".---" to "J", "-.-" to "K", ".-.." to "L",
        "--" to "M", "-." to "N", "---" to "O", ".--." to "P", "--.-" to "Q", ".-." to "R",
        "..." to "S", "-" to "T", "..-" to "U", "...-" to "V", ".--" to "W", "-..-" to "X",
        "-.--" to "Y", "--.." to "Z", "-----" to "0", ".----" to "1", "..---" to "2", "...--" to "3",
        "....-" to "4", "....." to "5", "-...." to "6", "--..." to "7", "---.." to "8", "----." to "9"
    )

    override fun decode(input: String): List<DecodeFinding> {
        if (!input.all { it == '.' || it == '-' || it == ' ' || it == '/' || it == '|' || it == '
' }) return emptyList()
        val words = input.replace('|', '/').split('/').map { word ->
            word.trim().split(Regex("\s+")).mapNotNull { table[it] }.joinToString("")
        }.filter { it.isNotBlank() }
        if (words.isEmpty()) return emptyList()
        val decoded = words.joinToString(" ")
        val score = TextScorer.score(decoded) + 4
        return listOf(
            DecodeFinding(
                method = "Morse",
                resultText = decoded,
                confidence = TextScorer.confidence(score),
                score = score,
                note = "Dot/dash Morse code decode.",
                why = "Input alphabet and separators matched Morse patterns.",
                chain = listOf("morse"),
                findingType = FindingType.DECODE,
                family = "Morse",
            )
        )
    }
}
