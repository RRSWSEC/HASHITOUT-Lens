package com.rrswsec.hashitoutlens.camera

import android.graphics.Bitmap
import android.graphics.Rect

data class LiveFrameResult(
    val text: String,
    val barcodes: List<String>,
    val textBlocks: List<TextBlockInfo> = emptyList(),
    val frameBitmap: Bitmap? = null,
    val requestHighRes: Boolean = false
)

data class TextBlockInfo(
    val text: String,
    val boundingBox: Rect?,
    val normalizedBoundingBox: android.graphics.RectF? = null,
    val lines: List<TextLineInfo> = emptyList()
)

data class TextLineInfo(
    val text: String,
    val boundingBox: android.graphics.Rect?,
    val normalizedBoundingBox: android.graphics.RectF? = null,
    val elements: List<TextElementInfo> = emptyList()
)

data class TextElementInfo(
    val text: String,
    val boundingBox: android.graphics.Rect?,
    val normalizedBoundingBox: android.graphics.RectF? = null
)
