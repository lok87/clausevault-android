package cloud.clausevault.app.ui

import androidx.compose.ui.graphics.Color

fun riskHeatColor(score: Int): Color = when {
    score <= 40 -> Color(0xFF22C55E)
    score <= 70 -> Color(0xFFCA8A04)
    else -> Color(0xFFEF4444)
}

fun riskHeatLabel(score: Int): String = when {
    score <= 40 -> "Low"
    score <= 70 -> "Elevated"
    else -> "High"
}
