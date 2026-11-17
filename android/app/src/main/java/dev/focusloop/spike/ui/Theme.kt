package dev.focusloop.spike.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Teal = Color(0xFF0F7C88)
val Ink = Color(0xFF16343A)
val Muted = Color(0xFF5E7074)
val CanvasColor = Color(0xFFF7FAFB)
val Border = Color(0xFFDCE7E9)
val Mint = Color(0xFFE6F3F3)
val BlueWash = Color(0xFFEDF4F8)

@Composable
fun FocusLoopTheme(content: @Composable () -> Unit) {
    val base = Typography()
    MaterialTheme(
        colorScheme = lightColorScheme(primary = Teal, onPrimary = Color.White, primaryContainer = Mint,
            onPrimaryContainer = Ink, background = CanvasColor, onBackground = Ink, surface = Color.White,
            onSurface = Ink, surfaceVariant = Mint, onSurfaceVariant = Muted, outline = Muted, outlineVariant = Border,
            surfaceTint = Teal, surfaceContainer = Color.White, surfaceContainerLow = CanvasColor,
            surfaceContainerHigh = Color.White, surfaceContainerHighest = Mint, surfaceContainerLowest = Color.White,
            secondary = Teal, secondaryContainer = Mint, onSecondaryContainer = Teal),
        typography = base.copy(
            headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 38.sp),
            headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 34.sp),
            titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
            titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
            bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
            bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
            bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp),
            labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp)),
        shapes = Shapes(small = RoundedCornerShape(10.dp), medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(20.dp)),
        content = content)
}
