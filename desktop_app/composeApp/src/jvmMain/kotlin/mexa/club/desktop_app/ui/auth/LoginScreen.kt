package mexa.club.desktop_app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import mexa.club.desktop_app.auth.AuthApiClient
import mexa.club.desktop_app.auth.AuthApiResult
import mexa.club.desktop_app.auth.AuthSession
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mexa.club.desktop_app.localization.AppLanguage
import mexa.club.desktop_app.localization.AppStrings
import mexa.club.desktop_app.localization.appLanguage
import mexa.club.desktop_app.theme.WarehousePalette

private enum class ResetPasswordStep {
    EnterEmail,
    VerifyCode,
    SetNewPassword,
}

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
) {
    val lang = appLanguage()
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val api: AuthApiClient = org.koin.compose.koinInject { org.koin.core.parameter.parametersOf(AppStrings.authNetwork(lang)) }
    var rememberMe by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var loginFeedback by remember { mutableStateOf<FeedbackDialogState?>(null) }
    var resetStep by remember { mutableStateOf<ResetPasswordStep?>(null) }
    var forgotEmail by remember { mutableStateOf("") }
    var forgotCode by remember { mutableStateOf("") }
    var forgotNewPassword by remember { mutableStateOf("") }
    var forgotNewPasswordVisible by remember { mutableStateOf(false) }
    var forgotLoading by remember { mutableStateOf(false) }
    var forgotFeedback by remember { mutableStateOf<FeedbackDialogState?>(null) }

    Box(modifier.fillMaxSize()) {
        AuthSplitLayout(
        left = { LoginLeftPanel(lang) },
        right = { _ ->
            Column(
                Modifier
                    .fillMaxSize()
                    .background(Color.White),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    LanguageToggleBar()
                }

                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Column(Modifier.width(440.dp)) {
                        Column(Modifier.fillMaxWidth()) {
                            Text(
                                AppStrings.loginTitle(lang),
                                style = MaterialTheme.typography.headlineLarge,
                                color = WarehousePalette.OnBackground,
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                AppStrings.loginSubtitle(lang),
                                style = MaterialTheme.typography.bodyLarge,
                                color = WarehousePalette.OnSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.height(40.dp))

                        Text(
                            AppStrings.fieldUsername(lang),
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                            color = WarehousePalette.OnSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(AppStrings.placeholderUsername(lang), color = WarehousePalette.Outline, maxLines = 1) },
                            leadingIcon = {
                                Icon(Icons.Filled.Person, contentDescription = null, tint = WarehousePalette.Outline)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = WarehousePalette.Primary,
                                unfocusedBorderColor = WarehousePalette.OutlineVariant,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                            ),
                        )
                        Spacer(Modifier.height(24.dp))

                        Text(
                            AppStrings.fieldPassword(lang),
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                            color = WarehousePalette.OnSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(AppStrings.placeholderLoginPassword(lang), color = WarehousePalette.Outline, maxLines = 1) },
                            leadingIcon = {
                                Icon(Icons.Filled.Lock, contentDescription = null, tint = WarehousePalette.Outline)
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = null,
                                        tint = WarehousePalette.Outline,
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = WarehousePalette.Primary,
                                unfocusedBorderColor = WarehousePalette.OutlineVariant,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                            ),
                        )
                        Spacer(Modifier.height(16.dp))

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = rememberMe,
                                    onCheckedChange = { rememberMe = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = WarehousePalette.Primary,
                                        checkmarkColor = Color.White,
                                    ),
                                )
                                Text(
                                    AppStrings.rememberMe(lang),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = WarehousePalette.OnSurfaceVariant,
                                )
                            }
                            TextButton(
                                onClick = {
                                    resetStep = ResetPasswordStep.EnterEmail
                                    forgotFeedback = null
                                    forgotEmail = ""
                                    forgotCode = ""
                                    forgotNewPassword = ""
                                    forgotNewPasswordVisible = false
                                },
                            ) {
                                Text(
                                    AppStrings.forgotPasswordLink(lang),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = WarehousePalette.Primary,
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))

                        Button(
                            onClick = {
                                if (username.isBlank() || password.isBlank()) {
                                    loginFeedback = FeedbackDialogState(
                                        isSuccess = false,
                                        title = AppStrings.registerValidationTitle(lang),
                                        message = AppStrings.loginNoCredentials(lang),
                                    )
                                    return@Button
                                }
                                loading = true
                                scope.launch {
                                    when (val r = api.login(username, password)) {
                                        is AuthApiResult.Ok -> {
                                            AuthSession.setFromLogin(
                                                token = r.value.accessToken,
                                                refresh = r.value.refreshToken,
                                                session = r.value.sessionId,
                                                expiresInSeconds = r.value.expiresInSeconds,
                                                rememberMe = rememberMe,
                                            )
                                            if (!AuthSession.hasAdminAccess) {
                                                loading = false
                                                loginFeedback = FeedbackDialogState(
                                                    isSuccess = false,
                                                    title = AppStrings.loginNoAccessTitle(lang),
                                                    message = AppStrings.loginNoAccessMessage(lang),
                                                )
                                                AuthSession.clear()
                                                return@launch
                                            }
                                            loginFeedback = FeedbackDialogState(
                                                isSuccess = true,
                                                title = AppStrings.loginDialogSuccessTitle(lang),
                                                message = AppStrings.loginDialogSuccessMessage(lang),
                                                onConfirm = onLoginSuccess,
                                            )
                                        }
                                        is AuthApiResult.Err -> {
                                            loginFeedback = FeedbackDialogState(
                                                isSuccess = false,
                                                title = AppStrings.loginDialogFailTitle(lang),
                                                message = r.message,
                                            )
                                        }
                                    }
                                    loading = false
                                }
                            },
                            enabled = !loading,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WarehousePalette.Primary,
                                contentColor = Color.White,
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                        ) {
                            Text(
                                if (loading) AppStrings.loginLoading(lang) else AppStrings.loginButton(lang),
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, Modifier.size(20.dp))
                        }

                        Spacer(Modifier.height(24.dp))
                        HorizontalDivider(color = WarehousePalette.SurfaceContainer)
                        Spacer(Modifier.height(24.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                AppStrings.noAccount(lang) + " ",
                                style = MaterialTheme.typography.bodyLarge,
                                color = WarehousePalette.OnSurfaceVariant,
                            )
                            TextButton(onClick = onNavigateToRegister, contentPadding = PaddingValues(0.dp)) {
                                Text(
                                    AppStrings.goRegister(lang),
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = WarehousePalette.Primary,
                                )
                            }
                        }
                    }
                }

                Text(
                    AppStrings.copyright(lang),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    color = WarehousePalette.Outline,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    textAlign = TextAlign.Center,
                )
            }
        },
    )

    if (resetStep == ResetPasswordStep.EnterEmail) {
        Dialog(onDismissRequest = { if (!forgotLoading) resetStep = null }) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
            ) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(AppStrings.forgotDialogTitle(lang), style = MaterialTheme.typography.titleLarge)
                    Text(
                        AppStrings.forgotDialogHint(lang),
                        style = MaterialTheme.typography.bodyMedium,
                        color = WarehousePalette.OnSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = forgotEmail,
                        onValueChange = { forgotEmail = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(AppStrings.forgotPlaceholder(lang)) },
                        singleLine = true,
                        enabled = !forgotLoading,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WarehousePalette.Primary,
                            unfocusedBorderColor = WarehousePalette.OutlineVariant,
                        ),
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = { if (!forgotLoading) resetStep = null }, enabled = !forgotLoading) {
                            Text(AppStrings.cancel(lang))
                        }
                        Button(
                            onClick = {
                                if (forgotEmail.isBlank()) {
                                    forgotFeedback = FeedbackDialogState(
                                        isSuccess = false,
                                        title = AppStrings.forgotEmailRequiredTitle(lang),
                                        message = AppStrings.forgotEmailRequiredMessage(lang),
                                    )
                                    return@Button
                                }
                                forgotLoading = true
                                scope.launch {
                                    when (val r = api.forgotPassword(forgotEmail)) {
                                        is AuthApiResult.Ok -> {
                                            resetStep = ResetPasswordStep.VerifyCode
                                        }
                                        is AuthApiResult.Err -> {
                                            forgotFeedback = FeedbackDialogState(
                                                isSuccess = false,
                                                title = AppStrings.forgotFailTitle(lang),
                                                message = r.message,
                                            )
                                        }
                                    }
                                    forgotLoading = false
                                }
                            },
                            enabled = !forgotLoading,
                        ) {
                            Text(if (forgotLoading) AppStrings.forgotSending(lang) else AppStrings.resetSendCode(lang))
                        }
                    }
                }
            }
        }
    }

    if (resetStep == ResetPasswordStep.VerifyCode) {
        Dialog(onDismissRequest = { if (!forgotLoading) resetStep = null }) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
            ) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(AppStrings.resetVerifyCode(lang), style = MaterialTheme.typography.titleLarge)
                    Text(
                        AppStrings.resetCodePlaceholder(lang),
                        style = MaterialTheme.typography.bodyMedium,
                        color = WarehousePalette.OnSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = forgotEmail,
                        onValueChange = { forgotEmail = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !forgotLoading,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WarehousePalette.Primary,
                            unfocusedBorderColor = WarehousePalette.OutlineVariant,
                        ),
                        label = { Text(AppStrings.fieldEmail(lang)) },
                    )
                    OutlinedTextField(
                        value = forgotCode,
                        onValueChange = { forgotCode = it.filter(Char::isDigit).take(6) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !forgotLoading,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WarehousePalette.Primary,
                            unfocusedBorderColor = WarehousePalette.OutlineVariant,
                        ),
                        label = { Text(AppStrings.resetCodeLabel(lang)) },
                        placeholder = { Text(AppStrings.resetCodePlaceholder(lang)) },
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = { if (!forgotLoading) resetStep = null }, enabled = !forgotLoading) {
                            Text(AppStrings.cancel(lang))
                        }
                        Button(
                            onClick = {
                                if (forgotEmail.isBlank() || forgotCode.isBlank()) {
                                    forgotFeedback = FeedbackDialogState(
                                        isSuccess = false,
                                        title = AppStrings.forgotFailTitle(lang),
                                        message = AppStrings.resetCodeRequired(lang),
                                    )
                                    return@Button
                                }
                                forgotLoading = true
                                scope.launch {
                                    when (val r = api.verifyResetCode(forgotEmail, forgotCode)) {
                                        is AuthApiResult.Ok -> {
                                            resetStep = ResetPasswordStep.SetNewPassword
                                        }
                                        is AuthApiResult.Err -> {
                                            forgotFeedback = FeedbackDialogState(
                                                isSuccess = false,
                                                title = AppStrings.forgotFailTitle(lang),
                                                message = r.message,
                                            )
                                        }
                                    }
                                    forgotLoading = false
                                }
                            },
                            enabled = !forgotLoading,
                        ) {
                            Text(if (forgotLoading) AppStrings.forgotSending(lang) else AppStrings.resetVerifyCode(lang))
                        }
                    }
                }
            }
        }
    }

    if (resetStep == ResetPasswordStep.SetNewPassword) {
        Dialog(onDismissRequest = { if (!forgotLoading) resetStep = null }) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
            ) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(AppStrings.resetApplyPassword(lang), style = MaterialTheme.typography.titleLarge)
                    OutlinedTextField(
                        value = forgotNewPassword,
                        onValueChange = { forgotNewPassword = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(AppStrings.resetNewPasswordPlaceholder(lang)) },
                        singleLine = true,
                        enabled = !forgotLoading,
                        visualTransformation = if (forgotNewPasswordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            IconButton(onClick = { forgotNewPasswordVisible = !forgotNewPasswordVisible }) {
                                Icon(
                                    if (forgotNewPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = null,
                                    tint = WarehousePalette.Outline,
                                )
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WarehousePalette.Primary,
                            unfocusedBorderColor = WarehousePalette.OutlineVariant,
                        ),
                        label = { Text(AppStrings.resetNewPasswordLabel(lang)) },
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = { if (!forgotLoading) resetStep = null }, enabled = !forgotLoading) {
                            Text(AppStrings.cancel(lang))
                        }
                        Button(
                            onClick = {
                                if (forgotNewPassword.length < 8) {
                                    forgotFeedback = FeedbackDialogState(
                                        isSuccess = false,
                                        title = AppStrings.forgotFailTitle(lang),
                                        message = AppStrings.resetPasswordRequired(lang),
                                    )
                                    return@Button
                                }
                                forgotLoading = true
                                scope.launch {
                                    when (val r = api.resetPassword(forgotEmail, forgotCode, forgotNewPassword)) {
                                        is AuthApiResult.Ok -> {
                                            forgotFeedback = FeedbackDialogState(
                                                isSuccess = true,
                                                title = AppStrings.resetPasswordUpdatedTitle(lang),
                                                message = AppStrings.resetPasswordUpdatedMessage(lang),
                                                onConfirm = {
                                                    resetStep = null
                                                    forgotEmail = ""
                                                    forgotCode = ""
                                                    forgotNewPassword = ""
                                                    forgotNewPasswordVisible = false
                                                },
                                            )
                                        }
                                        is AuthApiResult.Err -> {
                                            forgotFeedback = FeedbackDialogState(
                                                isSuccess = false,
                                                title = AppStrings.forgotFailTitle(lang),
                                                message = r.message,
                                            )
                                        }
                                    }
                                    forgotLoading = false
                                }
                            },
                            enabled = !forgotLoading,
                        ) {
                            Text(if (forgotLoading) AppStrings.forgotSending(lang) else AppStrings.resetApplyPassword(lang))
                        }
                    }
                }
            }
        }
    }

    FeedbackAlertDialog(
        state = loginFeedback,
        onDismissState = { loginFeedback = null },
        confirmLabel = AppStrings.ok(lang),
    )

    FeedbackAlertDialog(
        state = forgotFeedback,
        onDismissState = { forgotFeedback = null },
        confirmLabel = AppStrings.ok(lang),
    )
    }
}

@Composable
private fun LoginLeftPanel(lang: AppLanguage) {
    val navy = Color(0xFF0F172A)
    Box(
        Modifier
            .fillMaxSize()
            .background(navy),
    ) {
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 96.dp, y = (-96).dp)
                .size(384.dp)
                .clip(CircleShape)
                .background(WarehousePalette.Primary.copy(alpha = 0.1f))
                .blur(48.dp),
        )
        Column(
            Modifier
                .fillMaxSize()
                .padding(48.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(WarehousePalette.Primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Inventory2, contentDescription = null, tint = Color.White)
                }
                Text(
                    AppStrings.loginLeftBrand(lang),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = Color.White,
                    ),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(32.dp)) {
                Box(
                    Modifier
                        .fillMaxWidth(0.92f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF334155).copy(alpha = 0.5f))
                        .border(1.dp, Color(0xFF475569).copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        WarehousePalette.Primary.copy(alpha = 0.25f),
                                        Color(0xFF0F172A).copy(alpha = 0.9f),
                                    ),
                                ),
                            ),
                    )
                    Icon(
                        Icons.Filled.Inventory2,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.35f),
                        modifier = Modifier.size(120.dp),
                    )
                }
                Column {
                    Text(
                        AppStrings.loginLeftHeadline(lang),
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        AppStrings.loginLeftBody(lang),
                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp),
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.fillMaxWidth(0.95f),
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    Modifier
                        .width(4.dp)
                        .height(40.dp)
                        .background(WarehousePalette.Primary, RoundedCornerShape(2.dp)),
                )
                Text(
                    AppStrings.loginLeftQuote(lang),
                    style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                    color = Color(0xFFCBD5E1),
                    modifier = Modifier.fillMaxWidth(0.95f),
                )
            }
        }
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-64).dp, y = 64.dp)
                .size(256.dp)
                .clip(CircleShape)
                .background(WarehousePalette.Primary.copy(alpha = 0.2f))
                .blur(40.dp),
        )
    }
}
