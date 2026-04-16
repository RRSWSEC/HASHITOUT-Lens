package com.rrswsec.hashitoutlens.core

import com.rrswsec.hashitoutlens.core.model.DecodeFinding
import com.rrswsec.hashitoutlens.core.model.FindingType

object EncryptionIdentifier {
    fun identify(input: String): List<DecodeFinding> {
        val text = input.trim()
        if (text.isBlank()) return emptyList()
        val hits = mutableListOf<DecodeFinding>()

        fun hint(method: String, score: Double, note: String) {
            hits += DecodeFinding(
                method = method,
                resultText = text,
                confidence = TextScorer.confidence(score),
                score = score,
                note = note,
                why = note,
                chain = listOf("hint"),
                findingType = FindingType.ENCRYPTION_HINT,
                family = "encryption-hint",
            )
        }

        val compact = text.replace("\n", "").replace(" ", "")
        when {
            compact.matches(Regex("^[0-9a-fA-F]{32}$")) -> hint("possible md5", 62.0, "32 hex chars with no separators")
            compact.matches(Regex("^[0-9a-fA-F]{40}$")) -> hint("possible sha1", 62.0, "40 hex chars with no separators")
            compact.matches(Regex("^[0-9a-fA-F]{64}$")) -> hint("possible sha256", 64.0, "64 hex chars with no separators")
        }
        if (text.startsWith("$2a$") || text.startsWith("$2b$") || text.startsWith("$2y$")) {
            hint("possible bcrypt", 70.0, "bcrypt prefix detected")
        }
        if (text.startsWith("Salted__")) {
            hint("possible openssl salted blob", 72.0, "openssl salted header detected")
        }
        if (compact.matches(Regex("^[A-Za-z0-9+/=]{20,}$"))) {
            hint("possible base64", 52.0, "shape looks like base64")
        }
        if (compact.matches(Regex("^[01 ]{16,}$"))) {
            hint("possible binary", 50.0, "only 0 and 1 symbols detected")
        }
        if (compact.matches(Regex("^[0-9A-Fa-f ]{16,}$"))) {
            hint("possible hex", 46.0, "hex-heavy character set")
        }
        return hits.distinctBy { it.method + it.resultText }
    }
}
