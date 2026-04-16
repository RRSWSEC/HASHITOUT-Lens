package com.rrswsec.hashitoutlens.core

import com.rrswsec.hashitoutlens.core.decoders.*
import com.rrswsec.hashitoutlens.core.model.DecodeFinding
import com.rrswsec.hashitoutlens.core.model.FindingType
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

object DecoderRegistry {
    // Cache of "Winning" transformation families for adaptive speed
    private val winningFamilies = ConcurrentHashMap<String, Int>()
    
    // Technical Lexicon: Words that have been "Verified" by capture sessions
    private val verifiedLexicon = ConcurrentHashMap<String, Int>()
    private var isInitialized = false

    fun initialize(context: android.content.Context) {
        if (isInitialized) return
        val file = java.io.File(context.filesDir, "verified_lexicon.json")
        if (file.exists()) {
            try {
                val json = file.readText()
                val map = org.json.JSONObject(json)
                val keys = map.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    verifiedLexicon[key] = map.getInt(key)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        isInitialized = true
    }

    private fun persist(context: android.content.Context) {
        try {
            val json = org.json.JSONObject()
            verifiedLexicon.forEach { (k, v) -> json.put(k, v) }
            java.io.File(context.filesDir, "verified_lexicon.json").writeText(json.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val baseDecoders: List<Decoder> = listOf(
        CaesarDecoder(),
        AtbashDecoder(),
        ReverseDecoder(),
        Rot47Decoder(),
        Base64Decoder(),
        Base32Decoder(),
        HexDecoder(),
        BinaryDecoder(),
        UrlDecoder(),
        HtmlEntityDecoder(),
        MorseDecoder(),
        A1Z26Decoder(),
        RailFenceDecoder(),
        PolybiusDecoder(),
        VigenereBruteDecoder(),
        BaconRobustDecoder(),
        XorDecoder(),
        ScytaleDecoder(),
        LeetspeakDecoder(),
        Base58Decoder(),
        TriageDecoder(),
        Base85Decoder(),
        TapCodeDecoder(),
        BifidDecoder(),
    )
    
    private val decoders: List<Decoder> = baseDecoders + listOf(ChainedCipherDecoder())
    private val beamSearch = BeamSearchDecoder(baseDecoders)

    private val familyCaps = mapOf(
        "rot" to 10, "rail-fence" to 5, "vigenere" to 8,
        "base64" to 3, "base32" to 3, "hex" to 3, "binary" to 3,
        "atbash" to 3, "reverse" to 3, "rot47" to 3, "polybius" to 3,
        "a1z26" to 3, "morse" to 3, "url" to 3, "html" to 3,
        "encryption-hint" to 10, "barcode" to 10, "stego" to 15,
        "chained" to 10,
        "bacon" to 5,
        "xor" to 5,
        "scytale" to 5,
        "leet" to 5,
        "base58" to 5,
        "triage" to 5,
        "base85" to 5,
        "tap" to 5,
        "bifid" to 5,
    )

    /**
     * "Full-Nasty" Parallel Execution.
     * Runs all decoders across multiple threads for near-instant results.
     */
    suspend fun runAll(
        input: String, 
        extraFindings: List<DecodeFinding> = emptyList(), 
        depth: Int = 1,
        onProgress: ((String) -> Unit)? = null
    ): List<DecodeFinding> = coroutineScope {
        val normalized = input.trim()
        if (normalized.isBlank() && extraFindings.isEmpty()) return@coroutineScope emptyList()

        onProgress?.invoke("> initializing deep scan for: ${normalized.take(15)}...")

        val merged = mutableListOf<DecodeFinding>()
        merged += extraFindings.map { it.copy(originalText = it.originalText ?: normalized) }
        val hints = EncryptionIdentifier.identify(normalized)
        merged += hints.map { it.copy(originalText = normalized) }

        if (normalized.isBlank()) return@coroutineScope merged

        // Adaptive Prioritization
        val sortedDecoders = decoders.sortedByDescending { 
            val score = if (winningFamilies.containsKey(getDecoderFamily(it))) 100 else 0
            val hintScore = if (hints.any { h -> h.family == getDecoderFamily(it) }) 50 else 0
            score + hintScore
        }

        // Parallel FAST PATH
        onProgress?.invoke("> running ${sortedDecoders.size} base decoders...")
        val firstPassFindings = sortedDecoders.map { decoder ->
            async(Dispatchers.Default) {
                runCatching { 
                    val results = decoder.decode(normalized).map { 
                        it.copy(
                            originalText = normalized,
                            confidence = TextScorer.confidence(it.score)
                        ) 
                    }
                    if (results.isNotEmpty()) {
                        onProgress?.invoke("  [${decoder::class.simpleName}] found: ${results.first().resultText.take(15)}...")
                    }
                    results
                }.getOrDefault(emptyList())
            }
        }.awaitAll().flatten()
        
        merged += firstPassFindings

        // Update Winning Paths & Lexicon Cache
        firstPassFindings.filter { it.score >= 85.0 }.forEach {
            winningFamilies[it.family] = (winningFamilies[it.family] ?: 0) + 1
            it.resultText.split(Regex("\\s+")).filter { word -> word.length > 3 && word.any { c -> c.isLetter() } }.forEach { word ->
                verifiedLexicon[word.lowercase()] = (verifiedLexicon[word.lowercase()] ?: 0) + 1
            }
        }

        // Global Beam Search Deep Dive
        onProgress?.invoke("> starting beam search (width=64, depth=8)...")
        val deepFindings = withContext(Dispatchers.Default) {
            beamSearch.decode(normalized, maxDepth = 8, beamWidth = 64, onProgress = onProgress)
        }
        merged += deepFindings

        val deduped = merged
            .filter { it.resultText.isNotBlank() }
            .sortedByDescending { it.score }
            .distinctBy { findingKey(it) }

        val counts = mutableMapOf<String, Int>()
        return@coroutineScope deduped.filter { finding ->
            val cap = familyCaps[finding.family] ?: 999
            val count = counts.getOrDefault(finding.family, 0)
            if (count < cap || finding.findingType == FindingType.ENCRYPTION_HINT) {
                counts[finding.family] = count + 1
                true
            } else {
                false
            }
        }.take(50)
    }

    /**
     * Learning function: Strengthens the model based on user-confirmed clears/exports
     */
    fun learnFromVerifiedSession(context: android.content.Context, findings: List<DecodeFinding>) {
        findings.filter { it.score >= 50.0 }.forEach { 
            winningFamilies[it.family] = (winningFamilies[it.family] ?: 0) + 5 // Heavy weight for confirmed clears
            
            it.resultText.split(Regex("\\s+")).filter { word -> 
                word.length > 3
            }.forEach { word ->
                verifiedLexicon[word.lowercase()] = (verifiedLexicon[word.lowercase()] ?: 0) + 10
            }
        }
        persist(context)
    }

    private fun getDecoderFamily(decoder: Decoder): String = when (decoder) {
        is CaesarDecoder -> "rot"
        is Base64Decoder -> "base64"
        is HexDecoder -> "hex"
        is VigenereBruteDecoder -> "vigenere"
        is AtbashDecoder -> "atbash"
        is ReverseDecoder -> "reverse"
        is Rot47Decoder -> "rot47"
        is BinaryDecoder -> "binary"
        is UrlDecoder -> "url"
        is MorseDecoder -> "morse"
        is Base32Decoder -> "base32"
        is HtmlEntityDecoder -> "html"
        is PolybiusDecoder -> "polybius"
        is RailFenceDecoder -> "rail-fence"
        is ChainedCipherDecoder -> "chained"
        is BaconRobustDecoder -> "bacon"
        is XorDecoder -> "xor"
        is ScytaleDecoder -> "scytale"
        is LeetspeakDecoder -> "leet"
        is Base58Decoder -> "base58"
        is TriageDecoder -> "triage"
        is Base85Decoder -> "base85"
        is TapCodeDecoder -> "tap"
        is BifidDecoder -> "bifid"
        else -> "unknown"
    }

    private fun findingKey(finding: DecodeFinding): String {
        val typePrefix = if (finding.findingType == FindingType.ENCRYPTION_HINT) "hint:" else "out:"
        val body = finding.resultText.lowercase().filter(Char::isLetterOrDigit).take(260)
        return typePrefix + body
    }
}
