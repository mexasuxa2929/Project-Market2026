package com.example.mobile_app.ui.screens.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.res.painterResource
import com.example.mobile_app.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile_app.ui.theme.ChipBackground
import com.example.mobile_app.ui.theme.DividerColor
import com.example.mobile_app.ui.theme.Primary
import com.example.mobile_app.ui.theme.TextSecondary
import com.example.mobile_app.ui.theme.appTextFieldColors
import com.example.mobile_app.ui.viewmodel.AuthViewModel
import com.example.mobile_app.util.GoogleSignInHelper
import com.example.mobile_app.util.tr
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    languageManager: com.example.mobile_app.util.LanguageManager? = null,
    onLoginSuccess: () -> Unit,
    onNeedsVerification: () -> Unit = {},
    onForgotClick: () -> Unit = {}
) {
    val uiState by authViewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(uiState.isLoggedIn, uiState.needsFullName) {
        if (uiState.isLoggedIn && !uiState.needsFullName) onLoginSuccess()
    }
    LaunchedEffect(uiState.needsEmailVerification) {
        if (uiState.needsEmailVerification) onNeedsVerification()
    }

    // Auth ekranlari binafsha — status/nav bar ham binafsha bo'lishi uchun
    // ikonlar oq (light) qilinadi.
    val activity = LocalContext.current as? android.app.Activity
    androidx.compose.runtime.SideEffect {
        try {
            val window = activity?.window
            if (window != null) {
                androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = false
                    isAppearanceLightNavigationBars = false
                }
            }
        } catch (_: Exception) {}
    }
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            try {
                val window = activity?.window
                if (window != null) {
                    androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = true
                        isAppearanceLightNavigationBars = true
                    }
                }
            } catch (_: Exception) {}
        }
    }
    // Toast uchun auto-dismiss
    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            kotlinx.coroutines.delay(3500)
            authViewModel.clearError()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Primary)
    ) {
        // Scrollable content — system barlar ustiga chiqmasligi uchun inset + qo'shimcha margin
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
            Column(
                modifier = Modifier
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Til almashtirish — oldingi holat (oq aktiv, kulrang nofaol)
                if (languageManager != null) {
                    val selectedLang = languageManager.currentLanguage.value
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFF3F4F6))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        com.example.mobile_app.util.Language.entries.forEach { lang ->
                            val isSelected = lang == selectedLang
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) Color.White else Color.Transparent)
                                    .clickable { languageManager.setLanguage(lang) }
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = lang.displayName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Primary else Color(0xFF9CA3AF)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Logo
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(tr("home_logo_m"), fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = tr("login_welcome"),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = tr("login_tagline"),
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(22.dp))

                    // Tab
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(ChipBackground)
                            .padding(4.dp)
                    ) {
                        listOf(tr("login_tab_login"), tr("login_tab_register")).forEachIndexed { index, label ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selectedTab == index) Color.White else Color.Transparent)
                                    .clickable { selectedTab = index }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (selectedTab == index) Primary else TextSecondary,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))

                if (selectedTab == 0) {
                    LoginForm(
                        isLoading = uiState.isLoading,
                        error = null,
                        onLogin = authViewModel::login,
                        onGoogleLogin = authViewModel::loginWithGoogle,
                        onForgotClick = onForgotClick
                    )
                } else {
                    RegisterForm(
                        isLoading = uiState.isLoading,
                        error = null,
                        onRegister = authViewModel::register,
                        onGoogleLogin = authViewModel::loginWithGoogle
                    )
                }
                }
            }
        }

        // Theme'ga mos toast — iconsiz, yumaloq, engil soya
        if (uiState.error != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .padding(top = 8.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                AuthToast(
                    message = uiState.error!!,
                    onDismiss = { authViewModel.clearError() }
                )
            }
        }

        if (uiState.needsFullName) {
            FullNameDialog(
                isLoading = uiState.isLoading,
                error = uiState.error,
                onSave = { authViewModel.submitFullName(it) }
            )
        }
    }
}

@Composable
private fun FullNameDialog(
    isLoading: Boolean,
    error: String?,
    onSave: (String) -> Unit
) {
    var name by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    val fieldColors = appTextFieldColors()
    androidx.compose.material3.AlertDialog(
        onDismissRequest = {},
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        titleContentColor = Color(0xFF0F172A),
        textContentColor = Color(0xFF0F172A),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Primary, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                androidx.compose.material3.Text(
                    "Ism Familiyangiz *",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF0F172A)
                )
            }
        },
        text = {
            androidx.compose.foundation.layout.Column {
                androidx.compose.material3.Text(
                    "Murojaat uchun ismingizni kiriting. Busiz davom etib bo'lmaydi.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { androidx.compose.material3.Text("Masalan: Ali Valiyev", color = TextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors,
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = Primary) }
                )
                if (error != null) {
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.material3.Text(error, color = Color(0xFFEF4444), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = { onSave(name.trim()) },
                enabled = name.trim().length >= 2 && !isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    disabledContainerColor = Primary.copy(alpha = 0.45f),
                    contentColor = Color.White,
                    disabledContentColor = Color.White
                ),
                modifier = Modifier.height(44.dp)
            ) {
                if (isLoading) androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                else androidx.compose.material3.Text("Saqlash", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        },
        dismissButton = null
    )
}

@Composable
private fun LoginForm(
    isLoading: Boolean,
    error: String?,
    onLogin: (String, String) -> Unit,
    onGoogleLogin: (String) -> Unit,
    onForgotClick: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var googleLoading by remember { mutableStateOf(false) }
    var googleError by remember { mutableStateOf<String?>(null) }

    Column {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(tr("login_email")) },
            placeholder = { Text(tr("login_email_placeholder"), color = TextSecondary) },
            leadingIcon = { Icon(Icons.Default.Email, null, tint = TextSecondary) },
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            colors = appTextFieldColors()
        )
        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                tr("login_forgot"),
                fontSize = 13.sp,
                color = Primary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable(onClick = onForgotClick)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(tr("login_password")) },
            placeholder = { Text(tr("login_password_placeholder"), color = TextSecondary) },
            leadingIcon = { Icon(Icons.Default.Lock, null, tint = TextSecondary) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        null, tint = TextSecondary
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = appTextFieldColors()
        )

        if (error != null || googleError != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(error ?: googleError ?: "", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = { onLogin(email, password) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary),
            enabled = !isLoading && !googleLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text(tr("login_button"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        GoogleDivider()
        Spacer(modifier = Modifier.height(16.dp))

        // Google tugmasi
        GoogleButton(
            isLoading = googleLoading,
            enabled = !isLoading,
            onClick = {
                scope.launch {
                    googleLoading = true
                    googleError = null
                    GoogleSignInHelper.signIn(context).fold(
                        onSuccess = { idToken -> onGoogleLogin(idToken) },
                        onFailure = { e -> googleError = e.message }
                    )
                    googleLoading = false
                }
            }
        )

        Spacer(modifier = Modifier.height(14.dp))
        TermsText()
    }
}

@Composable
private fun RegisterForm(
    isLoading: Boolean,
    error: String?,
    onRegister: (String, String, String) -> Unit,
    onGoogleLogin: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var googleLoading by remember { mutableStateOf(false) }
    var googleError by remember { mutableStateOf<String?>(null) }

    Column {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(tr("login_name")) },
            placeholder = { Text(tr("login_username_placeholder"), color = TextSecondary, maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
            leadingIcon = { Icon(Icons.Default.Person, null, tint = TextSecondary) },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = appTextFieldColors()
        )
        Spacer(modifier = Modifier.height(14.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(tr("login_email")) },
            placeholder = { Text(tr("login_email_placeholder"), color = TextSecondary) },
            leadingIcon = { Icon(Icons.Default.Email, null, tint = TextSecondary) },
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            colors = appTextFieldColors()
        )
        Spacer(modifier = Modifier.height(14.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(tr("login_password")) },
            placeholder = { Text(tr("login_password_hint"), color = TextSecondary) },
            leadingIcon = { Icon(Icons.Default.Lock, null, tint = TextSecondary) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        null, tint = TextSecondary
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = appTextFieldColors()
        )

        if (error != null || googleError != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(error ?: googleError ?: "", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = { onRegister(name, email, password) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary),
            enabled = !isLoading && !googleLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text(tr("login_register_button"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        GoogleDivider()
        Spacer(modifier = Modifier.height(16.dp))

        GoogleButton(
            isLoading = googleLoading,
            enabled = !isLoading,
            onClick = {
                scope.launch {
                    googleLoading = true
                    googleError = null
                    GoogleSignInHelper.signIn(context).fold(
                        onSuccess = { idToken -> onGoogleLogin(idToken) },
                        onFailure = { e -> googleError = e.message }
                    )
                    googleLoading = false
                }
            }
        )

        Spacer(modifier = Modifier.height(14.dp))
        TermsText()
    }
}

// ── Reusable composables ──────────────────────────────────────────────────────

@Composable
private fun FieldLabel(text: String) {
    Text(text, fontSize = 13.sp, color = Color(0xFF374151), fontWeight = FontWeight.Medium)
}

@Composable
private fun GoogleDivider() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = DividerColor)
        Text(tr("login_or"), fontSize = 11.sp, color = TextSecondary)
        HorizontalDivider(modifier = Modifier.weight(1f), color = DividerColor)
    }
}

@Composable
private fun GoogleButton(
    isLoading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.5.dp, DividerColor),
        enabled = enabled && !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Primary, strokeWidth = 2.dp)
        } else {
            GoogleLogo()
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = tr("login_google"),
                fontSize = 15.sp,
                color = Color(0xFF374151),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun GoogleLogo() {
    Icon(
        painter = painterResource(id = R.drawable.ic_google),
        contentDescription = "Google",
        modifier = Modifier.size(22.dp),
        tint = Color.Unspecified  // SVG o'z ranglarini saqlaydi
    )
}

@Composable
private fun TermsText() {
    Text(
        text = tr("login_terms"),
        fontSize = 12.sp,
        color = TextSecondary,
        textAlign = TextAlign.Center,
        lineHeight = 17.sp
    )
}

@Composable
private fun AuthToast(
    message: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onDismiss),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1F2937),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}
