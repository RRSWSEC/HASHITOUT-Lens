package com.rrswsec.hashitoutlens.core

import com.rrswsec.hashitoutlens.core.model.Confidence
import com.rrswsec.hashitoutlens.core.model.DecodeFinding
import com.rrswsec.hashitoutlens.core.model.FindingType
import kotlin.math.log2

object EncryptionIdentifier {
    fun identify(input: String): List<DecodeFinding> {
        val text = input.trim()
        if (text.isBlank()) return emptyList()
        val findings = mutableListOf<DecodeFinding>()

        if (Regex("^[0-9a-fA-F]{32}$").matches(text)) {
            findings += hint("Possible MD5 / NTLM hash", 58.0, "32 hex characters match a common one-way hash length.")
        }
        if (Regex("^[0-9a-fA-F]{40}$").matches(text)) {
            findings += hint("Possible SHA-1 hash", 64.0, "40 hex characters match SHA-1 length.")
        }
        if (Regex("^[0-9a-fA-F]{64}$").matches(text)) {
            findings += hint("Possible SHA-256 / Keccak-256 hash", 70.0, "64 hex characters match a common 256-bit digest length.")
        }
        if (text.startsWith("$2a$") || text.startsWith("$2b$") || text.startsWith("$2y$")) {
            findings += hint("bcrypt hash", 86.0, "bcrypt prefix detected.")
        }
        if (text.startsWith("-----BEGIN PGP")) {
            findings += hint("PGP armored block", 88.0, "PGP armor header detected.")
        }
        if (text.startsWith("-----BEGIN RSA PRIVATE KEY-----") || text.startsWith("-----BEGIN OPENSSH PRIVATE KEY-----")) {
            findings += hint("PEM / private-key block", 92.0, "PEM key header detected.")
        }
        val parts = text.split('.')
        if (parts.size == 3 && parts.all { it.matches(Regex("^[A-Za-z0-9_-]+=*$")) }) {
            findings += hint("Possible JWT token", 76.0, "Three-part base64url token shape.")
        }
        if (text.length >= 24) {
            val entropy = entropy(text)
            if (entropy >= 4.5 && !text.contains(' ')) {
                findings += DecodeFinding(
                    method = "High-entropy opaque text",
                    resultText = "Entropy %.2f suggests encryption, compression, or dense encoding".format(entropy),
                    confidence = if (entropy >= 5.2) Confidence.HIGH else Confidence.MEDIUM,
                    score = entropy * 10,
                    note = "Type hint only — not a decryption.",
                    why = "Character distribution is flatter than normal language.",
                    findingType = FindingType.ENCRYPTION_HINT,
                    family = "EncryptionHint",
                )
            }
        }
        return findings
    }

    private fun hint(name: String, score: Double, why: String) = DecodeFinding(
        method = name,
        resultText = name,
        confidence = if (score >= 80) Confidence.HIGH else Confidence.MEDIUM,
        score = score,
        note = "Type hint only — not decrypted plaintext.",
        why = why,
        findingType = FindingType.ENCRYPTION_HINT,
        family = "EncryptionHint",
    )

    private fun entropy(text: String): Double {
        val counts = text.groupingBy { it }.eachCount().values.map { it.toDouble() }
        val n = text.length.toDouble()
        return counts.sumOf {
            val p = it / n
            -p * log2(p)
        }
    }
}
