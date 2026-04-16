package com.rrswsec.hashitoutlens.core.model

data class DetectionBundle(
    val ocrText: String = "",
    val barcodeTexts: List<String> = emptyList(),
    val hiddenTexts: List<String> = emptyList(),
)
