package com.rrswsec.hashitoutlens.core.model

data class DecodeFinding(
    val method: String,
    val resultText: String,
    val confidence: Confidence,
    val score: Double,
    val note: String = "",
    val why: String = "",
    val chain: List<String> = emptyList(),
    val findingType: FindingType = FindingType.DECODE,
    val family: String = "General",
)
