package com.haskatech.myday

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * MyDay's single visual source of truth.
 * Keep semantic roles here so screens do not invent competing colour values.
 */
object MyDayColors {
    val Navy = Color(0xFF14243A)
    val Lime = Color(0xFFB7E445)
    val Paper = Color(0xFFF4F6F9)
    val Muted = Color(0xFF748094)
    val White = Color.White
    val SurfaceVariant = Color(0xFFEDF1F5)
    val Border = Color(0xFFE3E8EF)
    val SoftLime = Color(0xFFEAF2D4)
    val SoftNeutral = Color(0xFFF0F2F5)
    val SoftNeutralAlt = Color(0xFFF0F3F6)
    val WarningBackground = Color(0xFFFFF4E5)
    val WarningText = Color(0xFF8A4B08)
    val Divider = Color(0xFFE8ECF1)
    val LimeDeep = Color(0xFF82A92D)
    val BorderStrong = Color(0xFFE7EBF0)
    val PaperTint = Color(0xFFFBFDF7)
    val SurfaceMuted = Color(0xFFF0F2F4)
    val SurfaceSubtle = Color(0xFFE9EDF2)
}

private val MyDayColorScheme = lightColorScheme(
    primary = MyDayColors.Navy,
    onPrimary = MyDayColors.White,
    secondary = MyDayColors.Lime,
    onSecondary = MyDayColors.Navy,
    background = MyDayColors.Paper,
    onBackground = MyDayColors.Navy,
    surface = MyDayColors.White,
    onSurface = MyDayColors.Navy,
    surfaceVariant = MyDayColors.SurfaceVariant,
    onSurfaceVariant = MyDayColors.Muted,
    outline = MyDayColors.Border,
    primaryContainer = MyDayColors.SoftLime
)

private val MyDayShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp)
)

@Composable
fun MyDayTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MyDayColorScheme,
        typography = Typography(),
        shapes = MyDayShapes,
        content = content
    )
}
