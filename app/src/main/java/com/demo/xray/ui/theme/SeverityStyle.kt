package com.demo.xray.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.demo.xray.core.model.Severity

/**
 * Severity is always paired with an icon and a text label; PRD 12.2 requires findings never rely
 * on color alone.
 */
@Composable
fun Severity.icon(): ImageVector =
    when (this) {
        Severity.CRITICAL -> Icons.Filled.Error
        Severity.HIGH -> Icons.Filled.PriorityHigh
        Severity.MEDIUM -> Icons.Filled.Warning
        Severity.LOW -> Icons.Filled.WarningAmber
        Severity.INFO -> Icons.Filled.Info
    }

fun Severity.color(): Color =
    when (this) {
        Severity.CRITICAL -> Color(0xFFB3261E)
        Severity.HIGH -> Color(0xFFD84315)
        Severity.MEDIUM -> Color(0xFFB8860B)
        Severity.LOW -> Color(0xFF3D6B47)
        Severity.INFO -> Color(0xFF3A5F8A)
    }

fun Severity.label(): String =
    when (this) {
        Severity.CRITICAL -> "Critical"
        Severity.HIGH -> "High"
        Severity.MEDIUM -> "Medium"
        Severity.LOW -> "Low"
        Severity.INFO -> "Info"
    }
