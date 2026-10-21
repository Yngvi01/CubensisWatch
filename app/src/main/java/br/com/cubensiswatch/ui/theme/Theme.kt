package br.com.cubensiswatch.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ForestDark = darkColorScheme(
    primary = Color(0xFFA7C98A), onPrimary = Color(0xFF1D3614),
    secondary = Color(0xFFD0B990), background = Color(0xFF11140F),
    surface = Color(0xFF191D17), surfaceVariant = Color(0xFF343A30),
)
@Composable
fun CubensisTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ForestDark, content = content)
}
