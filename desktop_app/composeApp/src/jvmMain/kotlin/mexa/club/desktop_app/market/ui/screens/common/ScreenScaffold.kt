package mexa.club.desktop_app.market.ui.screens.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mexa.club.desktop_app.market.ui.components.LoadingBlock

@Composable
fun ScreenScaffold(
    loading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        when {
            loading -> LoadingBlock()
            error != null -> {
                Text(error, color = MaterialTheme.colorScheme.error)
                Button(onClick = onRetry) { Text("Qayta urinish") }
            }
            else -> content()
        }
    }
}
