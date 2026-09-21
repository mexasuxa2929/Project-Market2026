package com.example.mobile_app.ui.screens.profile

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile_app.data.local.TokenManager
import com.example.mobile_app.ui.theme.Primary
import com.example.mobile_app.ui.theme.Background
import com.example.mobile_app.ui.theme.TextSecondary
import com.example.mobile_app.util.Language
import com.example.mobile_app.util.LanguageManager
import com.example.mobile_app.util.tr

private val PrimaryLight = Color(0xFFEEF0FE)
private val Danger = Color(0xFFEF4444)
private val TextDark = Color(0xFF1F2937)
@Composable
fun ProfileScreen(
    authViewModel: com.example.mobile_app.ui.viewmodel.AuthViewModel,
    languageManager: LanguageManager? = null,
    onLogout: () -> Unit,
    onNavigateToAddresses: () -> Unit = {},
    onNavigateToFavorites: () -> Unit = {},
    onNavigateToPaymentMethods: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {}
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    val selectedLanguage = languageManager?.currentLanguage?.value ?: Language.UZ

    // TokenManager dagi keshni state sifatida saqlaymiz + serverdan yangilab olamiz.
    // Shunda "Foydalanuvchi" o'rniga real fullname chiqadi.
    var cachedFullName by remember { mutableStateOf(TokenManager.fullName) }
    var cachedUsername by remember { mutableStateOf(TokenManager.username) }
    var cachedEmail by remember { mutableStateOf(TokenManager.email) }

    LaunchedEffect(Unit) {
        try {
            val repo = com.example.mobile_app.data.repository.AuthRepository(
                com.example.mobile_app.data.remote.RetrofitClient.authApiService
            )
            repo.getCurrentUser().onSuccess { user ->
                user.fullName?.trim()?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }?.let {
                    TokenManager.fullName = it
                    cachedFullName = it
                }
                user.username?.trim()?.takeIf { it.isNotBlank() }?.let {
                    TokenManager.username = it
                    cachedUsername = it
                }
                user.email?.trim()?.takeIf { it.isNotBlank() }?.let {
                    TokenManager.email = it
                    cachedEmail = it
                }
            }
        } catch (_: Exception) {}
    }

    fun cleanName(value: String?): String? {
        val t = value?.trim()
        if (t.isNullOrBlank() || t.equals("null", ignoreCase = true)) return null
        return t
    }
    val displayName = cleanName(cachedFullName) ?: cleanName(cachedUsername) ?: tr("profile_default_user")
    val avatarLetter = (cleanName(cachedFullName) ?: cleanName(cachedUsername) ?: "U").firstOrNull()?.toString()?.uppercase() ?: "U"

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White,
            titleContentColor = TextDark,
            textContentColor = TextSecondary,
            title = { Text(tr("profile_logout_title"), fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = { Text(tr("profile_logout_confirm"), fontSize = 14.sp) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        authViewModel.logout()
                        onLogout()
                    }
                ) {
                    Text(tr("profile_logout_yes"), color = Danger, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(tr("profile_cancel"), color = Primary)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
            Spacer(Modifier.height(16.dp))

            // Foydalanuvchi kartasi
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = avatarLetter,
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .align(Alignment.BottomStart)
                                    .clip(CircleShape)
                                    .background(Primary)
                                    .border(2.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.padding(start = 14.dp)) {
                            Text(
                                text = displayName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp,
                                color = TextDark
                            )
                            Text(
                                text = cachedEmail ?: "",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF3F4F6))
                                .padding(3.dp)
                        ) {
                            Language.entries.forEach { lang ->
                                val isSelected = lang == selectedLanguage
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color.White else Color.Transparent)
                                        .clickable { languageManager?.setLanguage(lang) }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = lang.displayName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Primary else Color(0xFF9CA3AF)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Menyu kartasi
            Spacer(Modifier.height(16.dp))
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 5.dp)) {
                    ProfileMenuItem(Icons.Default.LocationOn, tr("profile_addresses"), onClick = onNavigateToAddresses)
                    MenuDivider()
                    ProfileMenuItem(Icons.Default.CreditCard, tr("profile_payment"), onClick = onNavigateToPaymentMethods)
                    MenuDivider()
                    ProfileMenuItem(Icons.Default.FavoriteBorder, tr("profile_favorites"), onClick = onNavigateToFavorites)
                    MenuDivider()
                    ProfileMenuItem(Icons.Outlined.Settings, tr("profile_settings"), onClick = onNavigateToSettings)
                    MenuDivider()
                    ProfileMenuItem(Icons.Outlined.HelpOutline, tr("profile_help"), onClick = onNavigateToHelp)
                    MenuDivider()
                    ProfileMenuItem(
                        icon = Icons.Outlined.Logout,
                        label = tr("profile_logout"),
                        textColor = Danger,
                        iconColor = Danger,
                        iconBackground = Color(0xFFFEECEC),
                        showArrow = false,
                        onClick = { showLogoutDialog = true }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
}

@Composable
private fun MenuDivider() {
    Divider(color = Color(0xFFF3F4F6), modifier = Modifier.padding(start = 68.dp))
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    label: String,
    textColor: Color = TextDark,
    iconColor: Color = Primary,
    iconBackground: Color = PrimaryLight,
    showArrow: Boolean = true,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Text(
                text = label,
                modifier = Modifier.padding(start = 14.dp),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
        }
        if (showArrow) {
            Icon(Icons.Default.ChevronRight, null, tint = Color(0xFFD1D5DB), modifier = Modifier.size(18.dp))
        }
    }
}