package com.rrswsec.hashitoutlens.core.decoders

import com.rrswsec.hashitoutlens.core.Decoder
import com.rrswsec.hashitoutlens.core.TextScorer
import com.rrswsec.hashitoutlens.core.model.DecodeFinding
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Port of the 10k-line Python script's beam_chain_decode.
 * Ultra-Fast Parallel Beam Search (width=64, depth=8)
 */
class BeamSearchDecoder(private val decoders: List<Decoder>) {

    data class BeamNode(
        val text: String,
        val score: Double,
        val chain: List<String>,
        val methods: List<String>,
        val depth: Int = 0
    )

    suspend fun decode(
        input: String, 
        maxDepth: Int = 8, 
        beamWidth: Int = 64,
        onProgress: ((String) -> Unit)? = null
    ): List<DecodeFinding> = coroutineScope {
        val seeds = segmentCandidates(input)
        val bestFindings = Collections.synchronizedList(mutableListOf<DecodeFinding>())
        val seen = ConcurrentHashMap.newKeySet<String>()

        seeds.map { seed ->
            async(Dispatchers.Default) {
                var currentBeam = listOf(BeamNode(seed, TextScorer.score(seed), emptyList(), emptyList()))
                
                repeat(maxDepth) { depth ->
                    onProgress?.invoke("> depth ${depth+1}: analyzing ${currentBeam.size} nodes...")
                    val nextCandidates = PriorityQueue<BeamNode>(compareByDescending { it.score })
                    
                    // Hardware-Accelerated Parallel Processing:
                    val results = currentBeam.map { node ->
                        async(Dispatchers.Default) {
                            decoders.flatMap { decoder ->
                                if (decoder::class.simpleName?.contains("BeamSearch") == true || 
                                    decoder::class.simpleName?.contains("Chained") == true) return@flatMap emptyList<BeamNode>()
                                
                                runCatching { 
                                    val decoded = decoder.decode(node.text)
                                    decoded.map {
                                        val resultKey = it.resultText.lowercase().trim()
                                        if (seen.add(resultKey)) {
                                            // LIVE WORK LOGGING: Show what's actually being discovered
                                            val previewText = it.resultText.take(20).replace("\n", " ")
                                            onProgress?.invoke("  [${decoder::class.simpleName}] > $previewText...")

                                            BeamNode(
                                                text = it.resultText,
                                                score = it.score,
                                                chain = node.chain + it.chain,
                                                methods = node.methods + it.method,
                                                depth = depth + 1
                                            )
                                        } else null
                                    }.filterNotNull()
                                }.getOrDefault(emptyList())
                            }
                        }
                    }.awaitAll().flatten()

                    results.forEach { nextCandidates.add(it) }
                    
                    val nextBeam = mutableListOf<BeamNode>()
                    while (nextBeam.size < beamWidth) {
                        val candidate = nextCandidates.poll() ?: break
                        nextBeam.add(candidate)
                        
                        // Capture findings with threshold (Adaptive: lower threshold for deeper chains)
                        val threshold = if (depth > 2) 30.0 else 45.0
                        if (candidate.score >= threshold) {
                            val confidence = when {
                                candidate.score >= 90.0 -> com.rrswsec.hashitoutlens.core.model.Confidence.HIGH
                                candidate.score >= 70.0 -> com.rrswsec.hashitoutlens.core.model.Confidence.MEDIUM
                                else -> com.rrswsec.hashitoutlens.core.model.Confidence.LOW
                            }
                            bestFindings.add(DecodeFinding(
                                method = "Chain [${candidate.methods.joinToString(" -> ")}]",
                                resultText = candidate.text,
                                score = (candidate.score + (depth * 2.5)).coerceAtMost(100.0),
                                chain = candidate.chain,
                                originalText = seed,
                                confidence = confidence
                            ))
                        }
                    }
                    if (nextBeam.isEmpty()) return@async
                    currentBeam = nextBeam
                }
            }
        }.awaitAll()

        bestFindings.sortedByDescending { it.score }
            .distinctBy { it.resultText.lowercase().trim() }
            .take(50)
    }

    private fun segmentCandidates(text: String): List<String> {
        val out = mutableSetOf(text)
        val stripped = text.trim()
        if (stripped.isNotEmpty()) out.add(stripped)
        
        // Carve out islands of technical looking text
        val regex = Regex("[A-Za-z0-9+/=_-]{8,}|(?:[01]{8,})|(?:[0-9a-fA-F]{8,})")
        regex.findAll(text).forEach { match ->
            if (match.value.length >= 6) out.add(match.value)
        }
        
        // Split by common delimiters
        text.split(Regex("[|;]+|\\s{2,}")).forEach { part ->
            val p = part.trim()
            if (p.length >= 6) out.add(p)
        }
        
        return out.toList()
    }
}
