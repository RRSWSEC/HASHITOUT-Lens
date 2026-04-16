package com.rrswsec.hashitoutlens.core.decoders

import com.rrswsec.hashitoutlens.core.Decoder
import com.rrswsec.hashitoutlens.core.TextScorer
import com.rrswsec.hashitoutlens.core.model.DecodeFinding

class ChainedCipherDecoder : Decoder {
    override fun decode(input: String): List<DecodeFinding> {
        val results = mutableListOf<DecodeFinding>()
        val seen = mutableSetOf<String>()
        val caesar = CaesarDecoder()
        val atbashDecoder = AtbashDecoder()

        fun addIfBetter(text: String, chain: List<String>, method: String) {
            if (text.length < 3 || seen.contains(text)) return
            val score = TextScorer.score(text)
            if (score > 15.0) {
                seen.add(text)
                results.add(DecodeFinding(
                    resultText = text,
                    score = score,
                    confidence = TextScorer.confidence(score),
                    method = method,
                    chain = chain,
                    family = "chained"
                ))
            }
        }

        val rev = input.reversed()
        val atbashStr = atbashDecoder.atbash(input)
        val atbashRevStr = atbashDecoder.atbash(rev)

        // Path 1: Reverse -> ROT(n)
        for (n in 0..25) {
            val p = caesar.shift(rev, n)
            addIfBetter(p, listOf("reverse", "rot:$n"), "Reverse -> ROT$n")
        }

        // Path 2: Reverse -> Atbash
        addIfBetter(atbashRevStr, listOf("reverse", "atbash"), "Reverse -> Atbash")

        // Path 3: Reverse -> Atbash -> ROT(n)
        for (n in 0..25) {
            val p = caesar.shift(atbashRevStr, n)
            addIfBetter(p, listOf("reverse", "atbash", "rot:$n"), "Reverse -> Atbash -> ROT$n")
        }

        // Path 4: Atbash -> Reverse
        val revAtbash = atbashStr.reversed()
        addIfBetter(revAtbash, listOf("atbash", "reverse"), "Atbash -> Reverse")

        // Path 5: Atbash -> ROT(n)
        for (n in 0..25) {
            val p = caesar.shift(atbashStr, n)
            addIfBetter(p, listOf("atbash", "rot:$n"), "Atbash -> ROT$n")
        }

        // Path 6: Atbash -> ROT(n) -> Reverse
        for (n in 0..25) {
            val p = caesar.shift(atbashStr, n).reversed()
            addIfBetter(p, listOf("atbash", "rot:$n", "reverse"), "Atbash -> ROT$n -> Reverse")
        }

        // Path 7: ROT(n) -> Atbash
        for (n in 0..25) {
            val rotated = caesar.shift(input, n)
            val p = atbashDecoder.atbash(rotated)
            addIfBetter(p, listOf("rot:$n", "atbash"), "ROT$n -> Atbash")
        }

        return results.sortedByDescending { it.score }.take(10)
    }
}
