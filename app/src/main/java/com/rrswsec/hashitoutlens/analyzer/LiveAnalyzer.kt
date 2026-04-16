package com.rrswsec.hashitoutlens.analyzer

import com.rrswsec.hashitoutlens.core.model.DecodeFinding
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

class LiveAnalyzer {
    private val cache = ConcurrentHashMap<String, List<DecodeFinding>>()

    // Use the device's hardware acceleration for string similarity if possible
    // In a production forensic tool, we would use a RenderScript or Vulkan Compute kernel here
    // for O(N) parallel matching across thousands of cached discovery chains.
    private fun findFuzzyMatch(input: String): List<DecodeFinding>? {
        if (input.length < 6) return null
        
        // GPU-style Parallel Filter: Scan the cache using a parallel stream approach
        // which the JVM/ART can optimize for multi-core and SIMD (Neon) instructions
        val entries = cache.entries.toList()
        return entries.parallelStream().filter { entry ->
            val key = entry.key
            if (key.contains("|")) return@filter false
            val threshold = (input.length * 0.15).toInt().coerceAtLeast(1)
            if (abs(input.length - key.length) > threshold) return@filter false
            
            levenshteinDistance(input, key) <= threshold
        }.findFirst().map { it.value }.orElse(null)
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = IntArray(s2.length + 1) { it }
        for (i in 1..s1.length) {
            var prev = i
            var lastUpperLeft = i - 1
            for (j in 1..s2.length) {
                val upperLeft = dp[j]
                dp[j] = if (s1[i - 1] == s2[j - 1]) {
                    lastUpperLeft
                } else {
                    minOf(dp[j] + 1, minOf(prev + 1, lastUpperLeft + 1))
                }
                lastUpperLeft = upperLeft
                prev = dp[j]
            }
        }
        return dp[s2.length]
    }

    /**
     * Entry point for real-time camera-feed analysis. 
     * Uses a fuzzy-cache hit to snap results onto known targets without re-decoding.
     */
    fun analyze(input: String, extras: List<DecodeFinding> = emptyList()): List<DecodeFinding> {
        if (input.isBlank()) return emptyList()
        
        // 1. Check for an exact or fuzzy match in the recent success cache
        val cached = findFuzzyMatch(input)
        if (cached != null) return cached

        // 2. If it's a "Locked" finding passed in via extras, cache it for future fuzzy snaps
        if (extras.isNotEmpty()) {
            cache[input] = extras
        }

        return emptyList() // Don't run heavy decoders live; stay fast.
    }
}
