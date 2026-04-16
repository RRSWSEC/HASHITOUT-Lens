package com.rrswsec.hashitoutlens.analyzer

import com.rrswsec.hashitoutlens.core.DecoderRegistry
import com.rrswsec.hashitoutlens.core.model.DecodeFinding
import com.rrswsec.hashitoutlens.util.TextNormalizer

class LiveAnalyzer {
    private var lastInput: String = ""

    fun analyze(input: String, extraFindings: List<DecodeFinding> = emptyList()): List<DecodeFinding> {
        val changed = TextNormalizer.materiallyChanged(lastInput, input)
        lastInput = input
        if (!changed && extraFindings.isEmpty()) return emptyList()
        return DecoderRegistry.runAll(input, extraFindings)
    }
}
