package mexa.club.desktop_app.market

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mexa.club.desktop_app.market.session.SessionManager
import mexa.club.desktop_app.market.session.bootstrapSessionFromGateway

fun launchMarketSessionAfterLogin(
    scope: CoroutineScope,
    accessToken: String,
    refreshToken: String?,
    onFinished: () -> Unit = {},
) {
    SessionManager.setSession(accessToken, refreshToken, null)
    scope.launch(Dispatchers.IO) {
        bootstrapSessionFromGateway(accessToken, refreshToken)
        withContext(Dispatchers.Main) { onFinished() }
    }
}
