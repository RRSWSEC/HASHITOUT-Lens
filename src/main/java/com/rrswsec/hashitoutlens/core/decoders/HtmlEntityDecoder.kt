package com.rrswsec.hashitoutlens.core.decoders

import com.rrswsec.hashitoutlens.core.Decoder
import com.rrswsec.hashitoutlens.core.TextScorer
import com.rrswsec.hashitoutlens.core.model.DecodeFinding
import com.rrswsec.hashitoutlens.core.model.FindingType


class HtmlEntityDecoder : Decoder {
    private val entities = mapOf("&lt;" to "<", "&gt;" to ">", "&amp;" to "&", "&quot;" to """, "&#39;" to "'")

    override fun decode(input: String): List<DecodeFinding> {
        if (!input.contains('&')) return emptyList()
        var out = input
        entities.forEach { (k, v) -> out = out.replace(k, v) }
        out = out.replace(Regex("&#(\d+);")) { match -> match.groupValues[1].toIntOrNull()?.toChar()?.toString() ?: match.value }
        if (out == input) return emptyList()
        val score = TextScorer.score(out) + 2
        return listOf(
            DecodeFinding(
                method = "HTML entities",
                resultText = out,
                confidence = TextScorer.confidence(score),
                score = score,
                note = "Common HTML entities decoded.",
                why = "Named/numeric entities resolved into literal characters.",
                chain = listOf("html"),
                findingType = FindingType.DECODE,
                family = "HTML",
            )
        )
    }
}
