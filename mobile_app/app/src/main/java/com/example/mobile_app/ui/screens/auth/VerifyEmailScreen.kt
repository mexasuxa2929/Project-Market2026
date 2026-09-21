package com.example.mobile_app.ui.screens.auth

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile_app.ui.theme.Primary
import com.example.mobile_app.ui.theme.TextSecondary
import com.example.mobile_app.ui.viewmodel.AuthViewModel
import com.example.mobile_app.util.tr
import kotlinx.coroutines.delay

@Composable
fun VerifyEmailScreen(
    authViewModel: AuthViewModel,
    email: String,
    onVerified: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by authViewModel.uiState.collectAsState()
    var code by remember { mutableStateOf("") }
    var countdown by remember { mutableIntStateOf(60) }
    var canResend by remember { mutableStateOf(false) }
    var resendKey by remember { mutableIntStateOf(0) }

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    BackHandler {
        authViewModel.clearVerificationState()
        onBack()
    }

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) onVerified()
    }

    LaunchedEffect(resendKey) {
        countdown = 60
        canResend = false
        while (countdown > 0) {
            delay(1000)
            countdown--
        }
        canResend = true
    }

    val activity = androidx.compose.ui.platform.LocalContext.current as? android.app.Activity
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

    // To'liq binafsha fon — oq karta markazda, tepada/pastda binafsha qoladi
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Primary)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Yuqori panel — binafsha fonda oq yozuv/ikon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                authViewModel.clearVerificationState()
                onBack()
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    "MEXA MARKET",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    letterSpacing = 1.sp,
                    color = Color.White
                )
            }
            // Muvozanat uchun bo'sh joy (o'ngda help yo'q — binafsha minimalizm)
            Spacer(Modifier.size(48.dp))
        }

        // Markaziy oq karta
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 56.dp)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
                    .shadow(16.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.MailOutline,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = tr("verify_title"),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = tr("verify_description").trim(),
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF3F4F6))
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            email,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF374151)
                        )
                    }
                    Spacer(Modifier.height(22.dp))

                    Text(
                        "TASDIQLASH KODI",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = TextSecondary,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))

                    BasicTextField(
                        value = code,
                        onValueChange = { v ->
                            val filtered = v.filter { it.isDigit() }.take(6)
                            code = filtered
                            if (filtered.length == 6) {
                                focusManager.clearFocus()
                            }
                        },
                        modifier = Modifier
                            .height(0.dp)
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                indication = null,
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                            ) {
                                focusRequester.requestFocus()
                                keyboardController?.show()
                            },
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                    ) {
                        for (i in 0 until 6) {
                            val char = code.getOrNull(i)?.toString() ?: ""
                            val isFilled = char.isNotEmpty()
                            val isFocused = code.length == i
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isFilled) Color.White else Color(0xFFF9FAFB))
                                    .border(
                                        width = if (isFocused || isFilled) 1.8.dp else 1.dp,
                                        color = when {
                                            uiState.error != null -> MaterialTheme.colorScheme.error
                                            isFocused || isFilled -> Primary
                                            else -> Color(0xFFE5E7EB)
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    char,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF111827)
                                )
                                if (isFocused && code.length < 6) {
                                    Box(
                                        Modifier
                                            .width(2.dp)
                                            .height(18.dp)
                                            .background(Primary, RoundedCornerShape(1.dp))
                                    )
                                }
                            }
                        }
                    }

                    if (uiState.error != null) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = uiState.error!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { authViewModel.verifyEmail(code) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        enabled = code.length == 6 && !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                tr("verify_button") + " ✓",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    TextButton(
                        onClick = {
                            authViewModel.resendCode()
                            resendKey++
                        },
                        enabled = canResend && !uiState.isLoading
                    ) {
                        Text(
                            text = if (canResend) tr("verify_resend")
                            else tr("verify_resend_timer_prefix") + countdown + tr("verify_resend_timer_suffix"),
                            color = if (canResend) Primary else TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(Modifier.height(2.dp))
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Email noto'g'ri? ",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Text(
                            "Boshqa email kiritish",
                            fontSize = 12.sp,
                            color = Primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable {
                                authViewModel.clearVerificationState()
                                onBack()
                            }
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Xavfsiz tasdiqlash – Mexa Market tizimi",
                        fontSize = 10.sp,
                        color = Color(0xFF9CA3AF),
                        textAlign = TextAlign.Center,
                        letterSpacing = 0.3.sp
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(300)
        try { focusRequester.requestFocus() } catch (_: Exception) {}
    }
}
