package com.rrswsec.hashitoutlens.core.decoders

import com.rrswsec.hashitoutlens.core.Decoder
import com.rrswsec.hashitoutlens.core.TextScorer
import com.rrswsec.hashitoutlens.core.model.DecodeFinding
import com.rrswsec.hashitoutlens.core.model.FindingType


import android.util.Base64

class Base64Decoder : Decoder {
    override fun decode(input: String): List<DecodeFinding> {
        val cleaned = input.trim().replace("
", "")
        if (!cleaned.matches(Regex("^[A-Za-z0-9+/=_-]{8,}$"))) return emptyList()
        val attempts = listOf(
            "Base64" to cleaned,
            "Base64URL" to cleaned.replace('-', '+').replace('_', '/'),
        )
        return attempts.mapNotNull { (label, candidate) ->
            runCatching {
                val padded = candidate + "=".repeat((4 - candidate.length % 4) % 4)
                val decoded = String(Base64.decode(padded, Base64.DEFAULT))
                val score = TextScorer.score(decoded) + 4
                DecodeFinding(
                    method = label,
                    resultText = decoded,
                    confidence = TextScorer.confidence(score),
                    score = score,
                    note = "Valid base64-style decode.",
                    why = "$label parser accepted the input and produced printable output.",
                    chain = listOf(label.lowercase()),
                    findingType = FindingType.DECODE,
                    family = "Base64",
                )
            }.getOrNull()
        }
    }
}
