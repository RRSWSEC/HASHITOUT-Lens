package com.rrswsec.hashitoutlens.analyzer

import com.rrswsec.hashitoutlens.core.DecoderRegistry
import com.rrswsec.hashitoutlens.core.model.DecodeFinding

class LiveAnalyzer {
    private var lastInput: String = ""

    // keep this cheap. the camera feed should feel alive, not bogged down in theory.
    fun analyze(input: String, extras: List<DecodeFinding> = emptyList()): List<DecodeFinding> {
        val normalized = input.trim()
        if (normalized.isBlank() && extras.isEmpty()) return emptyList()
        if (normalized == lastInput && extras.isEmpty()) return emptyList()
        lastInput = normalized
        return DecoderRegistry.runAll(normalized, extras).take(24)
    }
}
