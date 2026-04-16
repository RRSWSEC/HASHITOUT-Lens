package com.rrswsec.hashitoutlens.core.decoders

import com.rrswsec.hashitoutlens.core.Decoder
import com.rrswsec.hashitoutlens.core.TextScorer
import com.rrswsec.hashitoutlens.core.model.DecodeFinding
import com.rrswsec.hashitoutlens.core.model.FindingType


import java.net.URLDecoder

class UrlDecoder : Decoder {
    override fun decode(input: String): List<DecodeFinding> {
        if (!input.contains('%') && !input.contains('+')) return emptyList()
        return runCatching {
            val decoded = URLDecoder.decode(input, "UTF-8")
            val score = TextScorer.score(decoded) + 2
            listOf(
                DecodeFinding(
                    method = "URL decode",
                    resultText = decoded,
                    confidence = TextScorer.confidence(score),
                    score = score,
                    note = "Percent-encoded text decoded.",
                    why = "URL-style escapes were present and decoded deterministically.",
                    chain = listOf("url"),
                    findingType = FindingType.DECODE,
                    family = "URL",
                )
            )
        }.getOrDefault(emptyList())
    }
}
