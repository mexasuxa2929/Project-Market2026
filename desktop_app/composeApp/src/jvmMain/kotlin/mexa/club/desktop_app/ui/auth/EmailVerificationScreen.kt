package mexa.club.desktop_app.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import mexa.club.desktop_app.auth.AuthApiClient
import mexa.club.desktop_app.auth.AuthApiResult
import mexa.club.desktop_app.auth.AuthSession
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import mexa.club.desktop_app.localization.AppLanguage
import mexa.club.desktop_app.localization.AppStrings
import mexa.club.desktop_app.localization.appLanguage
import mexa.club.desktop_app.theme.WarehousePalette

@Composable
fun EmailVerificationScreen(
    modifier: Modifier = Modifier,
    email: String,
    username: String,
    password: String,
    onVerified: () -> Unit,
    onBackToLogin: () -> Unit,
) {
    val lang = appLanguage()
    val scope = rememberCoroutineScope()
    val api: AuthApiClient = org.koin.compose.koinInject { org.koin.core.parameter.parametersOf(AppStrings.authNetwork(lang)) }
    val otp = remember { mutableStateListOf("", "", "", "", "", "") }
    val focusRequesters = remember { List(6) { FocusRequester() } }
    var secondsLeft by remember { mutableIntStateOf(45) }
    var loading by remember { mutableStateOf(false) }
    var resendLoading by remember { mutableStateOf(false) }
    var otpFeedback by remember { mutableStateOf<FeedbackDialogState?>(null) }

    LaunchedEffect(Unit) {
        focusRequesters[0].requestFocus()
        while (true) {
            delay(1000)
            if (secondsLeft > 0) secondsLeft--
        }
    }

    Box(modifier.fillMaxSize()) {
        AuthSplitLayout(
            left = { VerifyLeftPanel(lang) },
            right = { _ ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .background(Color.White)
                        .padding(horizontal = 24.dp, vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        LanguageToggleBar()
                    }
                    Spacer(Modifier.height(8.dp))
                    Column(
                        Modifier.width(400.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box {
                            Box(
                                Modifier
                                    .size(96.dp)
                                    .clip(CircleShape)
                                    .background(WarehousePalette.SurfaceContainerLow),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Filled.Mail,
                                    contentDescription = null,
                                    tint = WarehousePalette.Primary,
                                    modifier = Modifier.size(56.dp),
                                )
                            }
                            Box(
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(8.dp, (-8).dp)
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(1.dp, WarehousePalette.OutlineVariant, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = WarehousePalette.Tertiary,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }

                        Spacer(Modifier.height(32.dp))
                        Text(
                            AppStrings.verifyTitle(lang),
                            style = MaterialTheme.typography.headlineLarge,
                            color = WarehousePalette.OnBackground,
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            AppStrings.verifyIntro(lang, email),
                            style = MaterialTheme.typography.bodyLarge,
                            color = WarehousePalette.OnSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(40.dp))

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            repeat(6) { index ->
                                OutlinedTextField(
                                    value = otp[index],
                                    onValueChange = { raw ->
                                        val digitsOnly = raw.filter { it.isDigit() }
                                        when {
                                            digitsOnly.length > 1 -> {
                                                digitsOnly.take(6).forEachIndexed { i, c ->
                                                    if (i < 6) otp[i] = c.toString()
                                                }
                                                val nextFocus = (digitsOnly.length).coerceAtMost(5)
                                                focusRequesters[nextFocus].requestFocus()
                                            }
                                            digitsOnly.isEmpty() -> {
                                                otp[index] = ""
                                                if (index > 0) focusRequesters[index - 1].requestFocus()
                                            }
                                            else -> {
                                                otp[index] = digitsOnly.last().toString()
                                                if (index < 5) focusRequesters[index + 1].requestFocus()
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .width(52.dp)
                                        .height(76.dp)
                                        .focusRequester(focusRequesters[index]),
                                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 22.sp,
                                    ),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF6366F1),
                                        unfocusedBorderColor = WarehousePalette.OutlineVariant,
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White,
                                    ),
                                )
                            }
                        }

                        Spacer(Modifier.height(32.dp))
                        Text(
                            AppStrings.verifyResendQuestion(lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = WarehousePalette.OnSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        if (secondsLeft > 0) {
                            Text(
                                AppStrings.verifyResendIn(lang, secondsLeft),
                                color = WarehousePalette.OnSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        } else {
                            TextButton(
                                onClick = {
                                    resendLoading = true
                                    scope.launch {
                                        when (val r = api.resendCode(email)) {
                                            is AuthApiResult.Ok -> {
                                                secondsLeft = 45
                                                otpFeedback = FeedbackDialogState(
                                                    true,
                                                    AppStrings.verifyResendOkTitle(lang),
                                                    AppStrings.verifyResendOkMessage(lang),
                                                )
                                            }
                                            is AuthApiResult.Err -> {
                                                otpFeedback = FeedbackDialogState(
                                                    false,
                                                    AppStrings.verifyResendFailTitle(lang),
                                                    r.message,
                                                )
                                            }
                                        }
                                        resendLoading = false
                                    }
                                },
                                enabled = !resendLoading,
                            ) {
                                Text(
                                    if (resendLoading) AppStrings.verifyResending(lang) else AppStrings.verifyResend(lang),
                                    color = WarehousePalette.Primary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                val code = otp.joinToString("") { it }
                                when {
                                    code.length != 6 ->
                                        otpFeedback = FeedbackDialogState(
                                            false,
                                            AppStrings.verifyCodeFormatTitle(lang),
                                            AppStrings.verifyCodeFormatMessage(lang),
                                        )
                                    else -> {
                                        loading = true
                                        scope.launch {
                                            when (val v = api.verifyEmail(email, code)) {
                                                is AuthApiResult.Err -> {
                                                    otpFeedback = FeedbackDialogState(
                                                        false,
                                                        AppStrings.verifyFailTitle(lang),
                                                        v.message,
                                                    )
                                                    loading = false
                                                }
                                                is AuthApiResult.Ok -> {
                                                    when (val login = api.login(username, password)) {
                                                        is AuthApiResult.Ok -> {
                                                            AuthSession.setFromLogin(
                                                                login.value.accessToken,
                                                                login.value.refreshToken,
                                                                login.value.sessionId,
                                                            )
                                                            loading = false
                                                            otpFeedback = FeedbackDialogState(
                                                                true,
                                                                AppStrings.verifySuccessTitle(lang),
                                                                AppStrings.verifySuccessMessage(lang),
                                                                onConfirm = onVerified,
                                                            )
                                                        }
                                                        is AuthApiResult.Err -> {
                                                            otpFeedback = FeedbackDialogState(
                                                                false,
                                                                AppStrings.verifyLoginFailTitle(lang),
                                                                AppStrings.verifyLoginFailMessage(lang, login.message),
                                                            )
                                                            loading = false
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = !loading,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WarehousePalette.Primary,
                                contentColor = Color.White,
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                        ) {
                            Text(if (loading) AppStrings.verifySubmitting(lang) else AppStrings.verifySubmit(lang), fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(Modifier.height(48.dp))
                        TextButton(onClick = onBackToLogin) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = WarehousePalette.OnSurfaceVariant, modifier = Modifier.size(18.dp))
                                Text(AppStrings.verifyBackLogin(lang), style = MaterialTheme.typography.bodyMedium, color = WarehousePalette.OnSurfaceVariant)
                            }
                        }
                    }
                }
            },
        )

        FeedbackAlertDialog(
            state = otpFeedback,
            onDismissState = { otpFeedback = null },
            confirmLabel = AppStrings.ok(lang),
        )

        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(32.dp)
                .clickable(onClick = {}),
            shape = RoundedCornerShape(999.dp),
            color = Color.White,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, WarehousePalette.OutlineVariant),
        ) {
            Row(
                Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Filled.HeadsetMic, contentDescription = null, tint = WarehousePalette.Secondary)
                Text(
                    AppStrings.verifyHelp(lang),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold),
                    color = WarehousePalette.OnSurface,
                )
            }
        }
    }
}

@Composable
private fun VerifyLeftPanel(lang: AppLanguage) {
    val slate = Color(0xFF0F172A)
    Box(
        Modifier
            .fillMaxSize()
            .background(slate),
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .offset((-96).dp, (-96).dp)
                    .size(384.dp)
                    .clip(CircleShape)
                    .background(WarehousePalette.Primary.copy(alpha = 0.15f)),
            )
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .offset((-48).dp, 96.dp)
                    .size(256.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF6366F1).copy(alpha = 0.2f)),
            )
        }
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
                    AppStrings.verifyLeftBrand(lang),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = Color.White,
                    ),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Text(
                    AppStrings.verifyLeftHeadline(lang),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 40.sp,
                        fontSize = 32.sp,
                    ),
                    color = Color.White,
                )
                Text(
                    AppStrings.verifyLeftBody(lang),
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, lineHeight = 26.sp),
                    color = Color(0xFF94A3B8),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(32.dp)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(WarehousePalette.Primary.copy(alpha = 0.6f), slate),
                                ),
                            )
                            .border(2.dp, WarehousePalette.Primary, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("AR", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                    Column {
                        Text("Azizbek Rahimov", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
                        Text("Bosh Administrator", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                    }
                }
                Text(
                    AppStrings.verifyLeftFooter(lang),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF64748B),
                )
            }
        }
    }
}
