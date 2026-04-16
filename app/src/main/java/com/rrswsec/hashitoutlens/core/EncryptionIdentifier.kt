package com.rrswsec.hashitoutlens.core

import com.rrswsec.hashitoutlens.core.model.DecodeFinding
import com.rrswsec.hashitoutlens.core.model.FindingType

object EncryptionIdentifier {
    data class TextProfile(
        val base64Likelihood: Double,
        val hexLikelihood: Double,
        val binaryLikelihood: Double,
        val ic: Double,
        val alphaDensity: Double
    )

    private fun profileText(text: String): TextProfile {
        if (text.isEmpty()) return TextProfile(0.0, 0.0, 0.0, 0.0, 0.0)
        
        val counts = IntArray(256)
        var alphaCount = 0
        var hexCount = 0
        var b64Count = 0
        var binCount = 0
        
        text.forEach { char ->
            val code = char.code
            if (code < 256) counts[code]++
            if (char.isLetter()) alphaCount++
            if (char in "0123456789abcdefABCDEF") hexCount++
            if (char in "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=") b64Count++
            if (char in "01 ") binCount++
        }

        val len = text.length.toDouble()
        val ic = counts.sumOf { it * (it - 1) }.toDouble() / (len * (len - 1))
        
        return TextProfile(
            base64Likelihood = b64Count / len,
            hexLikelihood = hexCount / len,
            binaryLikelihood = binCount / len,
            ic = if (len > 1) ic else 0.0,
            alphaDensity = alphaCount / len
        )
    }

    fun identify(input: String): List<DecodeFinding> {
        val text = input.trim()
        if (text.isBlank()) return emptyList()
        val hits = mutableListOf<DecodeFinding>()

        // Statistical Profiling (O(1) pass)
        val profile = profileText(text)
        
        fun hint(method: String, score: Double, note: String, family: String = "encryption-hint") {
            hits += DecodeFinding(
                method = method,
                resultText = text,
                confidence = TextScorer.confidence(score),
                score = score,
                note = note,
                why = note,
                chain = listOf("hint"),
                findingType = FindingType.ENCRYPTION_HINT,
                family = family,
            )
        }

        // Statistical Logic
        if (profile.base64Likelihood > 0.85) hint("base64 signature (stats)", profile.base64Likelihood * 100, "statistical distribution matches base64", "base64")
        if (profile.hexLikelihood > 0.90) hint("hex signature (stats)", profile.hexLikelihood * 100, "character histogram matches hexadecimal", "hex")
        if (profile.binaryLikelihood > 0.95) hint("binary signature (stats)", profile.binaryLikelihood * 100, "symbols limited to [0,1]", "binary")
        if (profile.ic > 0.06 && profile.alphaDensity > 0.8) {
            val icScore = (profile.ic * 1500.0).coerceIn(0.0, 100.0)
            hint("classical cipher likely", icScore, "high index of coincidence (%.4f)".format(profile.ic), "rot")
        }

        val compact = text.replace("\n", "").replace(" ", "")
        when {
            compact.matches(Regex("^[0-9a-fA-F]{32}$")) -> hint("possible md5", 70.0, "32 hex chars with no separators")
            compact.matches(Regex("^[0-9a-fA-F]{40}$")) -> hint("possible sha1", 70.0, "40 hex chars with no separators")
            compact.matches(Regex("^[0-9a-fA-F]{64}$")) -> hint("possible sha256", 70.0, "64 hex chars with no separators")
        }
        if (text.startsWith("$2a$") || text.startsWith("$2b$") || text.startsWith("$2y$")) {
            hint("possible bcrypt", 95.0, "bcrypt prefix detected")
        }
        if (text.startsWith("Salted__")) {
            hint("possible openssl salted blob", 98.0, "openssl salted header detected")
        }
        if (text.startsWith("-----BEGIN")) {
            hint("cryptographic key/cert", 100.0, "pem header detected")
        }
        if (text.startsWith("MC4C")) {
            hint("possible rsa public key (base64)", 92.0, "common rsa public key asn.1 prefix")
        }
        if (text.startsWith("eyJhbGciOiJKV1QiLCJ0eXAiOiJKV1QifQ")) {
            hint("jwt (header: jwt)", 100.0, "common jwt header detected")
        }
        if (compact.matches(Regex("^[0-9a-fA-F]{32}$"))) hint("md5 hash", 95.0, "32 hex chars")
        if (compact.matches(Regex("^[0-9a-fA-F]{40}$"))) hint("sha1 hash", 95.0, "40 hex chars")
        if (compact.matches(Regex("^[0-9a-fA-F]{64}$"))) hint("sha256 hash", 95.0, "64 hex chars")
        
        if (compact.matches(Regex("^[A-Za-z0-9+/=]{40,}$")) && compact.endsWith("=")) {
            hint("probable base64 blob", 90.0, "shape and padding matches base64")
        }
        if (compact.matches(Regex("^[01 ]{16,}$"))) {
            hint("possible binary", 40.0, "only 0 and 1 symbols detected")
        }
        if (text.contains("-----BEGIN PGP")) {
            val type = when {
                text.contains("PRIVATE KEY") -> "pgp private key"
                text.contains("PUBLIC KEY") -> "pgp public key"
                text.contains("MESSAGE") -> "pgp encrypted message"
                else -> "pgp block"
            }
            hint(type, 100.0, "pgp armor detected")
        }
        if (text.startsWith("ssh-rsa ") || text.startsWith("ssh-ed25519 ")) {
            hint("ssh public key", 100.0, "standard ssh pubkey format")
        }
        
        // JWT Detection from Python
        val jwtParts = text.split(".")
        if (jwtParts.size == 3 && jwtParts[0].length > 10) {
            hint("possible jwt token", 85.0, "3-part dot separated blob (header.payload.signature)")
        }

        // Entropy-based detection for high-bit buffers
        if (compact.length >= 32) {
            val entropy = calculateEntropy(compact)
            if (entropy > 4.5 && compact.length % 16 == 0) {
                hint("aes/block cipher likely", 70.0, "high entropy (%.2f) and block-aligned length".format(entropy))
            } else if (entropy > 4.5) {
                hint("high entropy data", 50.0, "potential encrypted or compressed blob (%.2f)".format(entropy))
            }
        }
        return hits.distinctBy { it.method + it.resultText }
    }

    private fun calculateEntropy(s: String): Double {
        val freq = s.groupingBy { it }.eachCount()
        val len = s.length.toDouble()
        return freq.values.sumOf { 
            val p = it / len
            -p * (Math.log(p) / Math.log(2.0))
        }
    }
}
