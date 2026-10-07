package mexa.club.desktop_app.ui.auth

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import mexa.club.desktop_app.auth.AuthApiClient
import mexa.club.desktop_app.auth.AuthApiResult
import mexa.club.desktop_app.auth.AuthSession
import mexa.club.desktop_app.auth.GoogleAuthFlow
import mexa.club.desktop_app.localization.AppLanguage
import mexa.club.desktop_app.localization.AppStrings
import mexa.club.desktop_app.theme.WarehousePalette

/**
 * Google bilan kirish/ro'yxatdan o'tish tugmasi (butun OAuth jarayoni ichida).
 *
 * @param onFeedback dialog xabarlari (muvaffaqiyat/xato) uchun chaqiriladi
 * @param onSuccess sessiya muvaffaqiyatli ochildidan keyin chaqiriladi
 */
@Composable
fun GoogleSignInButton(
    lang: AppLanguage,
    api: AuthApiClient,
    rememberMe: Boolean,
    onFeedback: (FeedbackDialogState) -> Unit,
    onSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 52.dp,
) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = {
            if (loading) return@OutlinedButton
            loading = true
            scope.launch {
                when (val flow = GoogleAuthFlow.signIn()) {
                    is GoogleAuthFlow.Result.Failure -> {
                        onFeedback(
                            FeedbackDialogState(
                                isSuccess = false,
                                title = AppStrings.googleErrorTitle(lang),
                                message = when (flow.reason) {
                                    GoogleAuthFlow.Reason.PORT_BUSY -> AppStrings.googlePortBusy(lang)
                                    GoogleAuthFlow.Reason.BROWSER_FAILED -> AppStrings.googleBrowserError(lang)
                                    GoogleAuthFlow.Reason.TIMEOUT -> AppStrings.googleTimeout(lang)
                                    GoogleAuthFlow.Reason.ACCESS_DENIED -> AppStrings.googleDenied(lang)
                                    GoogleAuthFlow.Reason.STATE_MISMATCH,
                                    GoogleAuthFlow.Reason.MALFORMED,
                                    -> AppStrings.googleUnexpected(lang)
                                },
                            ),
                        )
                    }
                    is GoogleAuthFlow.Result.Success -> {
                        when (val r = api.loginWithGoogle(flow.idToken)) {
                            is AuthApiResult.Err -> {
                                onFeedback(
                                    FeedbackDialogState(
                                        isSuccess = false,
                                        title = AppStrings.googleErrorTitle(lang),
                                        message = r.message,
                                    ),
                                )
                            }
                            is AuthApiResult.Ok -> {
                                AuthSession.setFromLogin(
                                    token = r.value.accessToken,
                                    refresh = r.value.refreshToken,
                                    session = r.value.sessionId,
                                    expiresInSeconds = r.value.expiresInSeconds,
                                    rememberMe = rememberMe,
                                )
                                if (!AuthSession.hasAdminAccess) {
                                    AuthSession.clear()
                                    onFeedback(
                                        FeedbackDialogState(
                                            isSuccess = false,
                                            title = AppStrings.loginNoAccessTitle(lang),
                                            message = AppStrings.loginNoAccessMessage(lang),
                                        ),
                                    )
                                } else {
                                    onFeedback(
                                        FeedbackDialogState(
                                            isSuccess = true,
                                            title = AppStrings.loginDialogSuccessTitle(lang),
                                            message = AppStrings.loginDialogSuccessMessage(lang),
                                            onConfirm = onSuccess,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }
                loading = false
            }
        },
        enabled = !loading,
        modifier = modifier.fillMaxWidth().height(height),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, WarehousePalette.OutlineVariant),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = WarehousePalette.OnSurface,
        ),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = WarehousePalette.Primary,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                AppStrings.googleSigningIn(lang),
                style = MaterialTheme.typography.titleMedium,
                color = WarehousePalette.OnSurfaceVariant,
            )
        } else {
            GoogleGLogo()
            Spacer(Modifier.width(12.dp))
            Text(
                AppStrings.googleContinue(lang),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                color = WarehousePalette.OnSurface,
            )
        }
    }
}

@Composable
private fun GoogleGLogo() {
    Text(
        "G",
        color = Color(0xFF4285F4),
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
    )
}
