package com.rrswsec.hashitoutlens.core.decoders

import com.rrswsec.hashitoutlens.core.Decoder
import com.rrswsec.hashitoutlens.core.TextScorer
import com.rrswsec.hashitoutlens.core.model.DecodeFinding
import java.net.URLDecoder

class UrlDecoder : Decoder {
    override fun decode(input: String): List<DecodeFinding> {
        val decoded = runCatching { URLDecoder.decode(input, Charsets.UTF_8.name()).trim() }.getOrElse { return emptyList() }
        if (decoded == input.trim()) return emptyList()
        val score = TextScorer.score(decoded)
        if (score < 40.0) return emptyList()
        return listOf(
            DecodeFinding(
                method = "url decode",
                resultText = decoded,
                confidence = TextScorer.confidence(score),
                score = score,
                note = "percent escape cleanup",
                why = "the url layer was definitely in the way here.",
                chain = listOf("url"),
                family = "url",
            )
        )
    }
}
