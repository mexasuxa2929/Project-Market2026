package mexa.club.desktop_app.market.ui.settings

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import mexa.club.desktop_app.market.api.ApiClient
import mexa.club.desktop_app.market.api.stringField
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors

private data class ProfileInfo(val id: String, val username: String, val email: String, val roles: List<String>)

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var profile by remember { mutableStateOf<ProfileInfo?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }

    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var saveSuccess by remember { mutableStateOf(false) }

    suspend fun fetchProfile(): ProfileInfo = withContext(Dispatchers.IO) {
        val text = ApiClient.get("/api/admin/users/me")
        val data = ApiClient.parseJsonObject(text)?.let { ApiClient.dataObject(it) }
            ?: throw IllegalStateException("Profil topilmadi")
        val roles = (data["roles"] as? JsonArray)?.mapNotNull { it.toString().trim('"') } ?: emptyList()
        ProfileInfo(
            id = data.stringField("id"),
            username = data.stringField("username"),
            email = data.stringField("email"),
            roles = roles,
        )
    }

    LaunchedEffect(Unit) {
        loading = true
        runCatching { fetchProfile() }
            .onSuccess { profile = it }
            .onFailure { loadError = it.message ?: "Xatolik" }
        loading = false
    }

    fun changePassword() {
        saveError = null
        saveSuccess = false
        if (oldPassword.length < 8 || newPassword.length < 8) {
            saveError = "Parol kamida 8 belgidan iborat bo'lishi kerak"
            return
        }
        if (newPassword != confirmPassword) {
            saveError = "Yangi parollar mos kelmadi"
            return
        }
        val uid = profile?.id
        if (uid.isNullOrBlank()) {
            saveError = "Foydalanuvchi aniqlanmadi"
            return
        }
        scope.launch {
            saving = true
            val json = "{\"oldPassword\":\"${oldPassword.replace("\"", "\\\"")}\",\"newPassword\":\"${newPassword.replace("\"", "\\\"")}\"}"
            val result = withContext(Dispatchers.IO) {
                runCatching { ApiClient.put("/api/admin/users/$uid/password", json) }
            }
            result.onSuccess {
                saveSuccess = true
                oldPassword = ""; newPassword = ""; confirmPassword = ""
            }.onFailure { e -> saveError = e.message ?: "Xatolik yuz berdi" }
            saving = false
        }
    }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(40.dp).background(MexaWarehouseColors.indigoAccent.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = MexaWarehouseColors.indigoAccent, modifier = Modifier.size(22.dp))
            }
            Column {
                Text("Sozlamalar", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textPrimary)
                Text("Profil va xavfsizlik sozlamalari", fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
            }
        }

        when {
            loading -> Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MexaWarehouseColors.indigoAccent)
            }
            loadError != null -> Text(loadError ?: "Xatolik", color = MexaWarehouseColors.danger)
            profile != null -> {
                val p = profile!!
                Column(
                    Modifier
                        .widthIn(max = 560.dp)
                        .border(1.dp, MexaWarehouseColors.borderSubtle, RoundedCornerShape(12.dp))
                        .background(MexaWarehouseColors.surfaceLowest, RoundedCornerShape(12.dp))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MexaWarehouseColors.indigoAccent, modifier = Modifier.size(18.dp))
                        Text("Profil ma'lumotlari", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textPrimary)
                    }
                    InfoRow("Foydalanuvchi nomi", p.username)
                    InfoRow("Email", p.email)
                    InfoRow("Rollar", p.roles.joinToString(", ").ifBlank { "—" })
                }
            }
        }

        Column(
            Modifier
                .widthIn(max = 560.dp)
                .border(1.dp, MexaWarehouseColors.borderSubtle, RoundedCornerShape(12.dp))
                .background(MexaWarehouseColors.surfaceLowest, RoundedCornerShape(12.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = MexaWarehouseColors.indigoAccent, modifier = Modifier.size(18.dp))
                Text("Parolni o'zgartirish", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textPrimary)
            }
            OutlinedTextField(
                value = oldPassword, onValueChange = { oldPassword = it; saveError = null },
                label = { Text("Joriy parol") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp),
            )
            OutlinedTextField(
                value = newPassword, onValueChange = { newPassword = it; saveError = null },
                label = { Text("Yangi parol") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp),
            )
            OutlinedTextField(
                value = confirmPassword, onValueChange = { confirmPassword = it; saveError = null },
                label = { Text("Yangi parolni tasdiqlang") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp),
            )
            if (saveError != null) {
                Text(saveError ?: "", color = MexaWarehouseColors.danger, fontSize = 13.sp)
            }
            if (saveSuccess) {
                Text("Parol muvaffaqiyatli o'zgartirildi", color = MexaWarehouseColors.statusActiveFg, fontSize = 13.sp)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(
                    onClick = { changePassword() },
                    enabled = !saving,
                    colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.indigoAccent),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    if (saving) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Saqlash", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MexaWarehouseColors.textPrimary)
    }
}
