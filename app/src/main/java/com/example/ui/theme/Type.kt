package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

fun getTypographyForScale(scale: Float = 1.0f): Typography {
    fun scaled(sp: Int): TextUnit = (sp * scale).sp

    return Typography(
        displayLarge = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = scaled(30),
            lineHeight = scaled(38)
        ),
        headlineMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            fontSize = scaled(22),
            lineHeight = scaled(28)
        ),
        titleLarge = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            fontSize = scaled(18),
            lineHeight = scaled(24)
        ),
        titleMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = scaled(15),
            lineHeight = scaled(20)
        ),
        bodyLarge = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = scaled(15),
            lineHeight = scaled(22),
            letterSpacing = 0.2.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = scaled(13),
            lineHeight = scaled(18)
        ),
        bodySmall = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = scaled(11),
            lineHeight = scaled(15)
        ),
        labelLarge = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = scaled(13),
            lineHeight = scaled(16)
        ),
        labelSmall = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = scaled(10),
            lineHeight = scaled(13)
        )
    )
}

val Typography = getTypographyForScale(1.0f)
