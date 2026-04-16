package com.rrswsec.hashitoutlens.core.decoders

import com.rrswsec.hashitoutlens.core.Decoder
import com.rrswsec.hashitoutlens.core.TextScorer
import com.rrswsec.hashitoutlens.core.model.DecodeFinding

class LeetspeakDecoder : Decoder {
    private val map = mapOf(
        '4' to 'A', '3' to 'E', '1' to 'I', '0' to 'O', '7' to 'T', '5' to 'S',
        '@' to 'A', '$' to 'S', '!' to 'I', '8' to 'B', '9' to 'G', '6' to 'G'
    )

    override fun decode(input: String): List<DecodeFinding> {
        if (!input.any { it in map.keys }) return emptyList()
        
        val decoded = input.map { map[it.lowercaseChar()] ?: it.uppercaseChar() }.joinToString("")
        val score = TextScorer.score(decoded)
        
        return if (score >= 40.0) {
            listOf(
                DecodeFinding(
                    method = "Leetspeak",
                    resultText = decoded,
                    confidence = TextScorer.confidence(score),
                    score = score,
                    family = "leet",
                    why = "Replaced common leetspeak substitutions.",
                    chain = listOf("leet")
                )
            )
        } else emptyList()
    }
}
