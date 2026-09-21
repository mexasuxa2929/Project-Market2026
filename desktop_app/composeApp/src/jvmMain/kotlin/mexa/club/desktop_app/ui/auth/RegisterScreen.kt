package mexa.club.desktop_app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import mexa.club.desktop_app.auth.AuthApiClient
import mexa.club.desktop_app.auth.AuthApiResult
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mexa.club.desktop_app.localization.AppLanguage
import mexa.club.desktop_app.localization.AppStrings
import mexa.club.desktop_app.localization.appLanguage
import mexa.club.desktop_app.theme.WarehousePalette

@Composable
fun RegisterScreen(
    modifier: Modifier = Modifier,
    onRegistered: (email: String, username: String, password: String) -> Unit,
    onNavigateToLogin: () -> Unit,
) {
    val lang = appLanguage()
    val scope = rememberCoroutineScope()
    val api: AuthApiClient = org.koin.compose.koinInject { org.koin.core.parameter.parametersOf(AppStrings.authNetwork(lang)) }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var terms by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var registerFeedback by remember { mutableStateOf<FeedbackDialogState?>(null) }

    Box(modifier.fillMaxSize()) {
        AuthSplitLayout(
            left = { RegisterLeftPanel(lang) },
            right = { compact ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .background(Color.White)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(Modifier.width(448.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            LanguageToggleBar()
                        }
                        Spacer(Modifier.height(8.dp))
                        if (compact) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Box(
                                    Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(WarehousePalette.Primary),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Filled.Inventory2,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                Text(
                                    AppStrings.registerCompactBrand(lang),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = WarehousePalette.Slate900,
                                )
                            }
                            Spacer(Modifier.height(48.dp))
                        }

                        Text(
                            AppStrings.registerTitle(lang),
                            style = MaterialTheme.typography.headlineLarge,
                            color = WarehousePalette.OnSurface,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            AppStrings.registerSubtitle(lang),
                            style = MaterialTheme.typography.bodyLarge,
                            color = WarehousePalette.Outline,
                        )
                        Spacer(Modifier.height(40.dp))

                        FieldLabel(AppStrings.fieldDisplayUsername(lang))
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(AppStrings.placeholderUsernameExample(lang), color = WarehousePalette.Outline, maxLines = 1) },
                            leadingIcon = {
                                Icon(Icons.Filled.Person, contentDescription = null, tint = WarehousePalette.Outline, modifier = Modifier.size(20.dp))
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(4.dp),
                            colors = fieldColors(),
                        )
                        Spacer(Modifier.height(24.dp))

                        FieldLabel(AppStrings.fieldEmail(lang))
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(AppStrings.placeholderEmail(lang), color = WarehousePalette.Outline, maxLines = 1) },
                            leadingIcon = {
                                Icon(Icons.Filled.Mail, contentDescription = null, tint = WarehousePalette.Outline, modifier = Modifier.size(20.dp))
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(4.dp),
                            colors = fieldColors(),
                        )
                        Spacer(Modifier.height(24.dp))

                        FieldLabel(AppStrings.registerFieldPassword(lang))
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(AppStrings.placeholderRegisterPassword(lang), color = WarehousePalette.Outline, maxLines = 1) },
                            leadingIcon = {
                                Icon(Icons.Filled.Lock, contentDescription = null, tint = WarehousePalette.Outline, modifier = Modifier.size(20.dp))
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = null,
                                        tint = WarehousePalette.Outline,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(4.dp),
                            colors = fieldColors(),
                        )
                        Spacer(Modifier.height(12.dp))
                        PasswordStrengthMeter(password = password, lang = lang)
                        Spacer(Modifier.height(24.dp))

                        FieldLabel(AppStrings.fieldConfirmPassword(lang))
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = confirm,
                            onValueChange = { confirm = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(AppStrings.placeholderRegisterConfirm(lang), color = WarehousePalette.Outline, maxLines = 1) },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.VerifiedUser,
                                    contentDescription = null,
                                    tint = WarehousePalette.Outline,
                                    modifier = Modifier.size(20.dp),
                                )
                            },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(4.dp),
                            colors = fieldColors(),
                        )
                        Spacer(Modifier.height(24.dp))

                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Checkbox(
                                checked = terms,
                                onCheckedChange = { terms = it },
                                modifier = Modifier.padding(top = 2.dp),
                                colors = CheckboxDefaults.colors(
                                    checkedColor = WarehousePalette.Primary,
                                    checkmarkColor = Color.White,
                                ),
                            )
                            Text(
                                AppStrings.termsAgreement(lang),
                                style = MaterialTheme.typography.bodyMedium,
                                color = WarehousePalette.OnSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.height(8.dp))

                        Button(
                            onClick = {
                                when {
                                    username.isBlank() || email.isBlank() || password.isBlank() ->
                                        registerFeedback = FeedbackDialogState(
                                            false,
                                            AppStrings.registerValidationTitle(lang),
                                            AppStrings.registerValidationFields(lang),
                                        )
                                    password.length < 8 ->
                                        registerFeedback = FeedbackDialogState(
                                            false,
                                            AppStrings.registerWeakPasswordTitle(lang),
                                            AppStrings.registerValidationWeakPassword(lang),
                                        )
                                    password != confirm ->
                                        registerFeedback = FeedbackDialogState(
                                            false,
                                            AppStrings.registerMismatchTitle(lang),
                                            AppStrings.registerValidationMismatch(lang),
                                        )
                                    !terms ->
                                        registerFeedback = FeedbackDialogState(
                                            false,
                                            AppStrings.registerTermsTitle(lang),
                                            AppStrings.registerValidationTerms(lang),
                                        )
                                    else -> {
                                        loading = true
                                        scope.launch {
                                            when (val r = api.register(username, email, password)) {
                                                is AuthApiResult.Ok -> {
                                                    val e = email.trim()
                                                    val u = username.trim()
                                                    val p = password
                                                    registerFeedback = FeedbackDialogState(
                                                        true,
                                                        AppStrings.registerSuccessTitle(lang),
                                                        AppStrings.registerSuccessMessage(lang, e),
                                                        onConfirm = { onRegistered(e, u, p) },
                                                    )
                                                }
                                                is AuthApiResult.Err -> {
                                                    registerFeedback = FeedbackDialogState(
                                                        false,
                                                        AppStrings.registerFailTitle(lang),
                                                        r.message,
                                                    )
                                                }
                                            }
                                            loading = false
                                        }
                                    }
                                }
                            },
                            enabled = !loading,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WarehousePalette.Primary,
                                contentColor = Color.White,
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                        ) {
                            Text(if (loading) AppStrings.registerSending(lang) else AppStrings.registerButton(lang), fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, Modifier.size(18.dp))
                        }

                        Spacer(Modifier.height(40.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Text(AppStrings.hasAccount(lang) + " ", style = MaterialTheme.typography.bodyMedium, color = WarehousePalette.OnSurfaceVariant)
                            TextButton(onClick = onNavigateToLogin) {
                                Text(
                                    AppStrings.goLogin(lang),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = WarehousePalette.Primary,
                                )
                            }
                        }
                    }
                }
            },
        )

        FeedbackAlertDialog(
            state = registerFeedback,
            onDismissState = { registerFeedback = null },
            confirmLabel = AppStrings.ok(lang),
        )
    }
}

@Composable
private fun RegisterLeftPanel(lang: AppLanguage) {
    val slate = Color(0xFF0F172A)
    Box(
        Modifier
            .fillMaxSize()
            .background(slate),
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .offset((-48).dp, (-48).dp)
                    .size(320.dp)
                    .clip(CircleShape)
                    .background(WarehousePalette.Primary.copy(alpha = 0.35f)),
            )
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(48.dp, 48.dp)
                    .size(280.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF312E81).copy(alpha = 0.5f)),
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
                        .clip(RoundedCornerShape(4.dp))
                        .background(WarehousePalette.PrimaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Inventory2, contentDescription = null, tint = Color.White)
                }
                Text(
                    AppStrings.registerLeftBrand(lang),
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
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFF475569), RoundedCornerShape(12.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(WarehousePalette.Primary.copy(alpha = 0.3f), slate),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Inventory2,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(80.dp),
                    )
                }
                Text(
                    AppStrings.registerLeftHeadline(lang),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, lineHeight = 34.sp),
                    color = Color.White,
                )
                Text(
                    AppStrings.registerLeftBody(lang),
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, lineHeight = 26.sp),
                    color = Color(0xFF94A3B8),
                )
            }

            Text(
                AppStrings.registerLeftFooter(lang),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF64748B),
            )
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp, fontWeight = FontWeight.SemiBold),
        color = WarehousePalette.OnSurfaceVariant,
    )
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = WarehousePalette.Primary,
    unfocusedBorderColor = WarehousePalette.OutlineVariant,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
)
