package com.example.mobile_app.ui.screens.auth

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile_app.ui.theme.Primary
import com.example.mobile_app.ui.theme.TextSecondary
import com.example.mobile_app.ui.theme.appTextFieldColors
import com.example.mobile_app.ui.viewmodel.AuthViewModel
import com.example.mobile_app.util.tr
import kotlinx.coroutines.delay

@Composable
fun ForgotPasswordScreen(
    authViewModel: AuthViewModel,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val uiState by authViewModel.uiState.collectAsState()
    var step by remember { mutableIntStateOf(1) }
    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(60) }
    var canResend by remember { mutableStateOf(false) }
    var resendKey by remember { mutableIntStateOf(0) }

    // SideEffect for purple system bars (same as Login)
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

    // Auto advance after forgot success
    LaunchedEffect(uiState.successMessage) {
        if (step == 1 && uiState.successMessage == "OK") {
            authViewModel.consumeSuccess()
            step = 2
            resendKey++ // timer boshlash
        }
    }

    // Resend timer — 60s kutish, keyin qayta yuborish mumkin
    LaunchedEffect(step, resendKey) {
        if (step != 2) return@LaunchedEffect
        countdown = 60
        canResend = false
        while (countdown > 0) {
            delay(1000)
            countdown--
        }
        canResend = true
    }

    // Toast auto-dismiss
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
        Box(
            modifier = Modifier
                .fillMaxSize()
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
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top bar inside card
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            if (step == 1) {
                                authViewModel.clearError()
                                onBack()
                            } else step--
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color(0xFF1F2937))
                        }
                        Spacer(Modifier.weight(1f))
                        Text(
                            when (step) {
                                1 -> tr("forgot_title")
                                2 -> tr("forgot_code_title")
                                else -> tr("forgot_new_pass_title")
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF1F2937)
                        )
                        Spacer(Modifier.weight(1f))
                        Spacer(Modifier.width(48.dp))
                    }
                    Spacer(Modifier.height(8.dp))

                    // Logo
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(tr("home_logo_m"), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = when (step) {
                            1 -> tr("forgot_desc")
                            2 -> tr("forgot_code_desc")
                            else -> tr("forgot_new_desc")
                        },
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(20.dp))

                    // Step indicator
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (i in 1..3) {
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (i <= step) Primary else Color(0xFFE5E7EB))
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))

                    when (step) {
                        1 -> {
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(tr("login_email")) },
                                placeholder = { Text("name@example.com", color = TextSecondary) },
                                leadingIcon = { Icon(Icons.Default.Email, null, tint = TextSecondary) },
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                colors = appTextFieldColors()
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { authViewModel.forgotPassword(email) },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                enabled = email.contains("@") && !uiState.isLoading
                            ) {
                                if (uiState.isLoading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                else Text(tr("forgot_send_code"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        2 -> {
                            OutlinedTextField(
                                value = code,
                                onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) code = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(tr("forgot_code_label")) },
                                placeholder = { Text("123456", color = TextSecondary) },
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = appTextFieldColors()
                            )
                            // Email chip
                            Spacer(Modifier.height(8.dp))
                            Text(email, fontSize = 12.sp, color = TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    authViewModel.verifyResetCode(email, code) { step = 3 }
                                },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                enabled = code.length == 6 && !uiState.isLoading
                            ) {
                                if (uiState.isLoading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                else Text(tr("forgot_verify"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = if (canResend) tr("forgot_resend")
                                else tr("verify_resend_timer_prefix") + countdown + tr("verify_resend_timer_suffix"),
                                fontSize = 13.sp,
                                color = if (canResend) Primary else TextSecondary,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.clickable(
                                    enabled = canResend && !uiState.isLoading,
                                    onClick = {
                                        authViewModel.forgotPassword(email)
                                        resendKey++
                                    }
                                )
                            )
                        }
                        3 -> {
                            OutlinedTextField(
                                value = newPassword,
                                onValueChange = { newPassword = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(tr("forgot_new_pass")) },
                                placeholder = { Text(tr("login_password_hint"), color = TextSecondary) },
                                leadingIcon = { Icon(Icons.Default.Lock, null, tint = TextSecondary) },
                                trailingIcon = {
                                    IconButton(onClick = { visible = !visible }) {
                                        Icon(if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null, tint = TextSecondary)
                                    }
                                },
                                visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                colors = appTextFieldColors()
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    authViewModel.resetPassword(email, code, newPassword) { onSuccess() }
                                },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                enabled = newPassword.length >= 8 && !uiState.isLoading
                            ) {
                                if (uiState.isLoading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                else Text(tr("forgot_save"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        // Toast — iconsiz, theme'ga mos (oq karta, yumaloq, soya)
        if (uiState.error != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .padding(top = 8.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .clickable { authViewModel.clearError() }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.error!!,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1F2937),
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}
