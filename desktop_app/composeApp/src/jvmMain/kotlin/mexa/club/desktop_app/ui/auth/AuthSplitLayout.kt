package mexa.club.desktop_app.ui.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val AuthBreakpoint = 900.dp

@Composable
fun AuthSplitLayout(
    left: @Composable () -> Unit,
    right: @Composable (compact: Boolean) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= AuthBreakpoint
        Row(Modifier.fillMaxSize()) {
            if (wide) {
                Box(Modifier.weight(0.45f).fillMaxHeight()) {
                    left()
                }
            }
            Box(
                Modifier
                    .weight(if (wide) 0.55f else 1f)
                    .fillMaxHeight(),
            ) {
                right(!wide)
            }
        }
    }
}
