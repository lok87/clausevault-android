package cloud.clausevault.app.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColors = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF5B8DEF),
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF1E3A5F),
    secondary = androidx.compose.ui.graphics.Color(0xFF94A3B8),
    background = androidx.compose.ui.graphics.Color(0xFF0F1419),
    surface = androidx.compose.ui.graphics.Color(0xFF1A1F26),
    onBackground = androidx.compose.ui.graphics.Color(0xFFE8EAED),
    onSurface = androidx.compose.ui.graphics.Color(0xFFE8EAED),
)

@Composable
fun ClauseVaultTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        dark -> DarkColors
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

private val Typography = androidx.compose.material3.Typography()
