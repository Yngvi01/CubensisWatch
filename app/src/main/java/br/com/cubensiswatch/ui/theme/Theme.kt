package br.com.cubensiswatch.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color

private val ForestDark = darkColorScheme(
    primary = Color(0xFFA7C98A), onPrimary = Color(0xFF1D3614),
    secondary = Color(0xFFD0B990), background = Color(0xFF11140F),
    surface = Color(0xFF191D17), surfaceVariant = Color(0xFF343A30),
)
private val ForestLight = lightColorScheme(
    primary = Color(0xFF466B32), onPrimary = Color.White,
    secondary = Color(0xFF765B32), background = Color(0xFFF6F7F2),
    surface = Color(0xFFFFFEFA), surfaceVariant = Color(0xFFE4E8DC),
)

@Composable
fun CubensisTheme(themeMode: String = "system", content: @Composable () -> Unit) {
    val useDarkTheme = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(colorScheme = if (useDarkTheme) ForestDark else ForestLight, content = content)
}
