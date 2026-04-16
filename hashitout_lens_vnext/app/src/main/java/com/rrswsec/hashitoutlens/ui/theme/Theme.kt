package com.rrswsec.hashitoutlens.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val skunkDark = darkColorScheme(
    primary = Color(0xFF73FF86),
    onPrimary = Color(0xFF04220A),
    secondary = Color(0xFFB4FFC0),
    tertiary = Color(0xFFFFD166),
    background = Color(0xFF070907),
    onBackground = Color(0xFFE8F6EA),
    surface = Color(0xFF0D120D),
    onSurface = Color(0xFFE8F6EA),
    outline = Color(0xFF7B8B7C),
)

private val skunkType = Typography(
    headlineSmall = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 24.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
)

@Composable
fun HashItOutLensTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = skunkDark,
        typography = skunkType,
        content = content,
    )
}
