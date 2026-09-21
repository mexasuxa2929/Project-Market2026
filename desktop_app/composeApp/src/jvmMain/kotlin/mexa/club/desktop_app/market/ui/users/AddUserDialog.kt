package mexa.club.desktop_app.market.ui.users

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import mexa.club.desktop_app.localization.PasswordStrength
import mexa.club.desktop_app.localization.evaluatePasswordStrength
import mexa.club.desktop_app.localization.filledSegments
import mexa.club.desktop_app.market.model.CreateUserForm
import mexa.club.desktop_app.market.model.UserRole
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors
import mexa.club.desktop_app.market.ui.users.common.RoleBadge

@Composable
fun AddUserDialog(
    onDismiss: () -> Unit,
    onSave: (CreateUserForm) -> Unit,
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var active by remember { mutableStateOf(true) }
    var selectedRoles by remember { mutableStateOf(setOf(UserRole.ADMIN)) }

    val usernameError by remember(username) {
        derivedStateOf {
            when {
                username.isBlank() -> null
                username.length < 3 -> "Kamida 3 belgi"
                username.length > 30 -> "Ko'pi bilan 30 belgi"
                !username.matches(Regex("^[a-zA-Z0-9_]+$")) -> "Faqat harf, raqam va _"
                else -> null
            }
        }
    }
    val emailError by remember(email) {
        derivedStateOf {
            when {
                email.isBlank() -> null
                !email.contains('@') || !email.contains('.') -> "Email noto'g'ri"
                else -> null
            }
        }
    }
    val passwordStrength = remember(password) { evaluatePasswordStrength(password) }
    val isValid by remember(username, email, password, usernameError, emailError) {
        derivedStateOf {
            username.isNotBlank() && email.isNotBlank() && password.length >= 8 &&
                usernameError == null && emailError == null
        }
    }

    Dialog(
        onDismissRequest = {},
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Surface(
            modifier = Modifier.widthIn(min = 340.dp, max = 540.dp).fillMaxWidth(0.88f),
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 8.dp,
        ) {
            Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Yangi foydalanuvchi", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Yopish")
                    }
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("USERNAME") },
                    placeholder = { Text("Masalan: alisher01") },
                    isError = usernameError != null,
                    supportingText = usernameError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("EMAIL") },
                    placeholder = { Text("example@mexa.uz") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    isError = emailError != null,
                    supportingText = emailError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("PASSWORD") },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                LinearProgressIndicator(
                    progress = { passwordStrength.filledSegments() / 4f },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    color = if (passwordStrength == PasswordStrength.Strong) MexaWarehouseColors.statusActiveFg else MexaWarehouseColors.indigoAccent,
                )
                Spacer(Modifier.height(16.dp))
                Text("ROLLAR", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textMuted)
                Spacer(Modifier.height(8.dp))
                val roleGrid = listOf(UserRole.SUPER_ADMIN, UserRole.ADMIN, UserRole.WAREHOUSE, UserRole.COURIER)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    roleGrid.chunked(2).forEach { rowRoles ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            rowRoles.forEach { role ->
                                Row(
                                    Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Checkbox(
                                        checked = selectedRoles.contains(role),
                                        onCheckedChange = { checked ->
                                            selectedRoles = if (checked) selectedRoles + role else selectedRoles - role
                                        },
                                    )
                                    RoleBadge(role)
                                }
                            }
                            if (rowRoles.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = active,
                        onCheckedChange = { active = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MexaWarehouseColors.indigoAccent,
                            checkedTrackColor = MexaWarehouseColors.indigoAccent.copy(alpha = 0.4f),
                        ),
                    )
                    Column(Modifier.padding(start = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Faol foydalanuvchi", fontWeight = FontWeight.SemiBold)
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.padding(start = 4.dp).height(16.dp))
                        }
                        Text(
                            "Yangi foydalanuvchi email orqali tasdiqlanishi kerak.",
                            fontSize = 12.sp,
                            color = MexaWarehouseColors.textMuted,
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss) { Text("Bekor qilish") }
                    Button(
                        onClick = {
                            onSave(
                                CreateUserForm(
                                    username = username.trim(),
                                    email = email.trim(),
                                    password = password,
                                    roles = selectedRoles,
                                    isActive = active,
                                ),
                            )
                        },
                        enabled = isValid,
                        modifier = Modifier.padding(start = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.indigoAccent),
                    ) {
                        Text("Saqlash")
                    }
                }
            }
        }
    }
}
