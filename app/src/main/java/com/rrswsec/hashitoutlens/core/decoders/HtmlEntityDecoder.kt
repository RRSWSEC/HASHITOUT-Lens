package com.rrswsec.hashitoutlens.core.decoders

import android.text.Html
import com.rrswsec.hashitoutlens.core.Decoder
import com.rrswsec.hashitoutlens.core.TextScorer
import com.rrswsec.hashitoutlens.core.model.DecodeFinding

class HtmlEntityDecoder : Decoder {
    override fun decode(input: String): List<DecodeFinding> {
        val decoded = Html.fromHtml(input, Html.FROM_HTML_MODE_LEGACY).toString().trim()
        if (decoded == input.trim()) return emptyList()
        val score = TextScorer.score(decoded)
        if (score < 40.0) return emptyList()
        return listOf(
            DecodeFinding(
                method = "html entity decode",
                resultText = decoded,
                confidence = TextScorer.confidence(score),
                score = score,
                note = "entity cleanup",
                why = "markup junk fell away and left cleaner text.",
                chain = listOf("html"),
                family = "html",
            )
        )
    }
}
