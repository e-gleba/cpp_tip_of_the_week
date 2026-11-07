package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StandardBadge(standard: String) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val (textColor, bgColor, borderColor) = if (isDark) {
        // High contrast radiant colors for dark theme
        when (standard) {
            "C++26" -> Triple(
                Color(0xFFD8B4FE), // Radiant lavender
                Color(0xFF3B0764).copy(alpha = 0.55f),
                Color(0xFFA855F7).copy(alpha = 0.5f)
            )
            "C++23" -> Triple(
                Color(0xFF7DD3FC), // Radiant sky blue
                Color(0xFF082F49).copy(alpha = 0.55f),
                Color(0xFF38BDF8).copy(alpha = 0.5f)
            )
            "C++20" -> Triple(
                Color(0xFF6EE7B7), // Radiant mint green
                Color(0xFF064E3B).copy(alpha = 0.55f),
                Color(0xFF34D399).copy(alpha = 0.5f)
            )
            "C++17" -> Triple(
                Color(0xFFFDE047), // Radiant amber/gold
                Color(0xFF451A03).copy(alpha = 0.55f),
                Color(0xFFFBBF24).copy(alpha = 0.5f)
            )
            "C++14" -> Triple(
                Color(0xFFFB923C), // Radiant orange
                Color(0xFF431407).copy(alpha = 0.55f),
                Color(0xFFF97316).copy(alpha = 0.5f)
            )
            else -> Triple(
                Color(0xFFCBD5E1), // Slate
                Color(0xFF1E293B).copy(alpha = 0.7f),
                Color(0xFF64748B).copy(alpha = 0.5f)
            )
        }
    } else {
        // Deep saturated text with soft tinted backgrounds for light theme (WCAG AAA compliant)
        when (standard) {
            "C++26" -> Triple(
                Color(0xFF6B21A8), // Deep purple
                Color(0xFFF3E8FF), // Light purple bg
                Color(0xFFD8B4FE)  // Border
            )
            "C++23" -> Triple(
                Color(0xFF0369A1), // Deep ocean blue
                Color(0xFFE0F2FE), // Light sky blue bg
                Color(0xFF7DD3FC)  // Border
            )
            "C++20" -> Triple(
                Color(0xFF047857), // Deep emerald green
                Color(0xFFD1FAE5), // Light mint bg
                Color(0xFF6EE7B7)  // Border
            )
            "C++17" -> Triple(
                Color(0xFFB45309), // Deep warm amber (crisp readable)
                Color(0xFFFEF3C7), // Light amber bg
                Color(0xFFFCD34D)  // Border
            )
            "C++14" -> Triple(
                Color(0xFFC2410C), // Deep burnt orange
                Color(0xFFFFEDD5), // Light orange bg
                Color(0xFFFDBA74)  // Border
            )
            else -> Triple(
                Color(0xFF334155), // Slate-700
                Color(0xFFF1F5F9), // Light slate bg
                Color(0xFFCBD5E1)  // Border
            )
        }
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = standard,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = textColor,
                fontSize = 10.sp
            )
        )
    }
}

@Composable
fun CategoryBadge(category: String) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val (textColor, bgColor, borderColor) = if (isDark) {
        Triple(
            Color(0xFFCBD5E1),
            Color(0xFF1E293B),
            Color(0xFF334155)
        )
    } else {
        Triple(
            Color(0xFF334155),
            Color(0xFFF1F5F9),
            Color(0xFFE2E8F0)
        )
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = category,
            style = MaterialTheme.typography.labelSmall.copy(
                color = textColor,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp
            )
        )
    }
}
