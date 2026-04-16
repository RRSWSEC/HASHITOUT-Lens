package com.rrswsec.hashitoutlens.core.decoders

import android.util.Base64
import com.rrswsec.hashitoutlens.core.Decoder
import com.rrswsec.hashitoutlens.core.TextScorer
import com.rrswsec.hashitoutlens.core.model.DecodeFinding

class Base64Decoder : Decoder {
    override fun decode(input: String): List<DecodeFinding> {
        val compact = input.trim().replace("\n", "")
        if (!compact.matches(Regex("^[A-Za-z0-9+/=_-]{8,}$"))) return emptyList()
        val variants = listOf(
            "base64" to compact,
            "base64url" to compact.replace('-', '+').replace('_', '/'),
        )
        return variants.mapNotNull { (label, candidate) ->
            runCatching {
                val padded = candidate + "=".repeat((4 - candidate.length % 4) % 4)
                val decoded = String(Base64.decode(padded, Base64.DEFAULT)).trim()
                val score = TextScorer.score(decoded)
                if (decoded.isNotBlank() && score >= 40.0) {
                    DecodeFinding(
                        method = label,
                        resultText = decoded,
                        confidence = TextScorer.confidence(score),
                        score = score,
                        note = "base lane",
                        why = "this decoded clean and landed in readable territory.",
                        chain = listOf(label),
                        family = if (label == "base64") "base64" else "base64",
                    )
                } else null
            }.getOrNull()
        }.sortedByDescending { it.score }
    }
}
