package com.rrswsec.hashitoutlens.core.decoders

import com.rrswsec.hashitoutlens.core.Decoder
import com.rrswsec.hashitoutlens.core.TextScorer
import com.rrswsec.hashitoutlens.core.model.DecodeFinding

class MorseDecoder : Decoder {
    private val table = mapOf(
        ".-" to 'A', "-..." to 'B', "-.-." to 'C', "-.." to 'D', "." to 'E', "..-." to 'F',
        "--." to 'G', "...." to 'H', ".." to 'I', ".---" to 'J', "-.-" to 'K', ".-.." to 'L',
        "--" to 'M', "-." to 'N', "---" to 'O', ".--." to 'P', "--.-" to 'Q', ".-." to 'R',
        "..." to 'S', "-" to 'T', "..-" to 'U', "...-" to 'V', ".--" to 'W', "-..-" to 'X',
        "-.--" to 'Y', "--.." to 'Z',
        "-----" to '0', ".----" to '1', "..---" to '2', "...--" to '3', "....-" to '4',
        "....." to '5', "-...." to '6', "--..." to '7', "---.." to '8', "----." to '9'
    )

    override fun decode(input: String): List<DecodeFinding> {
        if (input.any { it !in ".-/ |\n\t" }) return emptyList()
        val words = input.replace('|', '/').trim().split('/')
        val decoded = words.joinToString(" ") { word ->
            word.trim().split(Regex("\\s+")).mapNotNull { table[it] }.joinToString("")
        }.trim()
        if (decoded.isBlank()) return emptyList()
        val score = TextScorer.score(decoded)
        return if (score >= 20.0) {
            listOf(
                DecodeFinding(
                    method = "Morse Code",
                    resultText = decoded,
                    confidence = com.rrswsec.hashitoutlens.core.model.Confidence.HIGH,
                    score = score,
                    note = "Dot-dash sequence resolution",
                    why = "Signal timing patterns (dots/dashes) matched International Morse standards.",
                    chain = listOf("morse"),
                    family = "morse",
                )
            )
        } else emptyList()
    }
}
