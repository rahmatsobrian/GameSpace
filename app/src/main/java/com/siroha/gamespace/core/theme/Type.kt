package com.siroha.gamespace.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Deliberately system font families (FontFamily.Default / .Monospace) rather
// than a downloaded Google Font. A custom display face is a legitimate
// upgrade later, but downloadable fonts need a certificate-hash resource
// array that has to match Google's provider exactly — not something to
// guess at without a compiler to verify against. If you want a specific
// display face, add it via Android Studio's Resource Manager > Fonts >
// Downloadable, which generates that array correctly for you.
private val UiFontFamily = FontFamily.Default
private val DataFontFamily = FontFamily.Monospace

val Typography = Typography(
    titleLarge = TextStyle(fontFamily = UiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = UiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontFamily = UiFontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = UiFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = UiFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = UiFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
)

/**
 * Not part of Material3's default Typography slots — pulled out separately
 * so a monitoring readout can opt into it explicitly:
 * `Text("62", style = DataTextStyles.readoutLarge)`. Monospace here is
 * functional (digit widths don't jitter as a live counter updates), not
 * just a stylistic pick.
 */
object DataTextStyles {
    val readoutLarge = TextStyle(fontFamily = DataFontFamily, fontWeight = FontWeight.Medium, fontSize = 32.sp, lineHeight = 36.sp)
    val readoutMedium = TextStyle(fontFamily = DataFontFamily, fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 24.sp)
    val readoutSmall = TextStyle(fontFamily = DataFontFamily, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 16.sp)
}
