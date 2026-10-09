package mexa.club.desktop_app.market.ui.groups

import mexa.club.desktop_app.localization.AppStrings
import mexa.club.desktop_app.localization.appLanguage
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import mexa.club.desktop_app.market.api.ApiClient
import mexa.club.desktop_app.market.api.GatewayRealtimeHub
import mexa.club.desktop_app.market.api.jsonString
import mexa.club.desktop_app.market.api.stringField
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors

// ─── Models (GroupResponse / GroupDetailResponse / GroupMemberResponse) ───────

data class GroupRow(
    val id: String,
    val name: String,
    val description: String,
    val memberCount: Int,
    val createdBy: String,
    val createdAt: String,
)

data class GroupMemberRow(
    val id: String,
    val userId: String,
    val username: String,
    val email: String,
    val roleInGroup: String,
    val joinedAt: String,
)

// ─── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun GroupsScreen(modifier: Modifier = Modifier) {
    val lang = appLanguage()
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var groups by remember { mutableStateOf<List<GroupRow>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var createOpen by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<GroupRow?>(null) }
    var deleteTarget by remember { mutableStateOf<GroupRow?>(null) }
    var detailTarget by remember { mutableStateOf<GroupRow?>(null) }

    suspend fun fetchGroups(): List<GroupRow> = withContext(Dispatchers.IO) {
        val text = ApiClient.get("/api/admin/groups")
        val root = ApiClient.parseJsonObject(text)
        // Envelope {success, data:[...]} — data massiv (sahifalanmagan to'liq ro'yxat)
        val arr = (root?.get("data") as? JsonArray) ?: JsonArray(emptyList())
        arr.mapNotNull { el ->
            val g = el as? JsonObject ?: return@mapNotNull null
            val id = g.stringField("id").ifEmpty { return@mapNotNull null }
            GroupRow(
                id = id,
                name = g.stringField("name"),
                description = g.stringField("description"),
                memberCount = g.stringField("memberCount").toIntOrNull() ?: 0,
                createdBy = g.stringField("createdBy"),
                createdAt = g.stringField("createdAt"),
            )
        }
    }

    fun reload() {
        scope.launch {
            loading = true
            error = null
            runCatching { fetchGroups() }
                .onSuccess { groups = it }
                .onFailure { e -> error = e.message ?: AppStrings.t(lang, "Xatolik") }
            loading = false
        }
    }

    LaunchedEffect(Unit) { reload() }

    LaunchedEffect(Unit) {
        // Realtime mutatsiyalar → avtomatik yangilash
        GatewayRealtimeHub.dashboardRefresh.drop(1).collect { reload() }
    }

    // Guruhlar serverda to'liq keladi (pagination yo'q) — qidiruv lokal filtr
    val visibleGroups = remember(groups, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) groups
        else groups.filter {
            it.name.contains(q, ignoreCase = true) || it.description.contains(q, ignoreCase = true)
        }
    }

    if (createOpen) {
        GroupFormDialog(
            initial = null,
            onDismiss = { createOpen = false },
            onSaved = { createOpen = false; reload() },
        )
    }

    editTarget?.let { target ->
        GroupFormDialog(
            initial = target,
            onDismiss = { editTarget = null },
            onSaved = { editTarget = null; reload() },
        )
    }

    deleteTarget?.let { target ->
        GroupDeleteDialog(
            group = target,
            onDismiss = { deleteTarget = null },
            onDeleted = { deleteTarget = null; reload() },
        )
    }

    detailTarget?.let { target ->
        GroupDetailDialog(
            group = target,
            onDismiss = { detailTarget = null },
            onChanged = { reload() },
        )
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(40.dp).background(MexaWarehouseColors.indigoAccent.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Group, contentDescription = null, tint = MexaWarehouseColors.indigoAccent, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(AppStrings.t(appLanguage(), "Guruhlar"), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textPrimary)
                Text(AppStrings.t(appLanguage(), "Foydalanuvchi guruhlarini boshqarish"), fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
            }
            Button(
                onClick = { createOpen = true },
                colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.indigoAccent),
                shape = RoundedCornerShape(8.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(AppStrings.t(appLanguage(), "Yangi guruh"), fontWeight = FontWeight.SemiBold)
            }
        }

        // ── Filter (lokal qidiruv) ────────────────────────────────────────────
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(AppStrings.t(appLanguage(), "Guruh nomi yoki tavsif bo'yicha..."), fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
        )

        // ── Content ───────────────────────────────────────────────────────────
        when {
            loading -> Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MexaWarehouseColors.indigoAccent)
            }

            error != null -> Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(error ?: AppStrings.t(appLanguage(), "Xatolik"), color = MexaWarehouseColors.danger)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { reload() }) { Text(AppStrings.t(appLanguage(), "Qayta urinish")) }
                }
            }

            visibleGroups.isEmpty() -> Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Text(AppStrings.t(appLanguage(), "Guruhlar topilmadi"), color = MexaWarehouseColors.textMuted)
            }

            else -> GroupsTable(
                groups = visibleGroups,
                onView = { detailTarget = it },
                onEdit = { editTarget = it },
                onDelete = { deleteTarget = it },
            )
        }
    }
}

// ─── Table ────────────────────────────────────────────────────────────────────

@Composable
private fun GroupsTable(
    groups: List<GroupRow>,
    onView: (GroupRow) -> Unit,
    onEdit: (GroupRow) -> Unit,
    onDelete: (GroupRow) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, MexaWarehouseColors.borderSubtle, RoundedCornerShape(12.dp))
            .background(MexaWarehouseColors.surfaceLowest, RoundedCornerShape(12.dp)),
    ) {
        Row(
            Modifier.fillMaxWidth().background(MexaWarehouseColors.tableHeaderBg).padding(vertical = 12.dp, horizontal = 20.dp),
        ) {
            listOf(
                AppStrings.t(appLanguage(), "NOM") to 2f,
                AppStrings.t(appLanguage(), "TAVSIF") to 2.4f,
                AppStrings.t(appLanguage(), "AZOLAR") to 0.8f,
                AppStrings.t(appLanguage(), "AMALLAR") to 1.4f,
            ).forEach { (h, w) ->
                Text(
                    h,
                    modifier = Modifier.weight(w),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.4.sp,
                    color = MexaWarehouseColors.outline,
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(MexaWarehouseColors.borderSubtle))
        groups.forEachIndexed { idx, g ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(if (idx % 2 == 0) MexaWarehouseColors.surfaceLowest else MexaWarehouseColors.surfaceContainerLow.copy(alpha = 0.35f))
                    .clickable { onView(g) }
                    .padding(vertical = 10.dp, horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(g.name, modifier = Modifier.weight(2f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textPrimary)
                Text(
                    g.description.ifBlank { "—" },
                    modifier = Modifier.weight(2.4f),
                    fontSize = 13.sp,
                    color = MexaWarehouseColors.textMuted,
                    maxLines = 2,
                )
                Box(Modifier.weight(0.8f)) {
                    Box(
                        Modifier.background(MexaWarehouseColors.infoBadgeBg, RoundedCornerShape(6.dp)).padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(g.memberCount.toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.indigoAccent)
                    }
                }
                Row(modifier = Modifier.weight(1.4f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    TableAction(icon = Icons.Default.Visibility, tint = MexaWarehouseColors.indigoAccent, description = AppStrings.t(appLanguage(), "Ko'rish")) { onView(g) }
                    TableAction(icon = Icons.Default.Edit, tint = MexaWarehouseColors.outline, description = AppStrings.t(appLanguage(), "Tahrirlash")) { onEdit(g) }
                    TableAction(icon = Icons.Default.Delete, tint = MexaWarehouseColors.danger, description = AppStrings.t(appLanguage(), "O'chirish")) { onDelete(g) }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(MexaWarehouseColors.borderSubtle.copy(alpha = 0.6f)))
        }
    }
}

@Composable
private fun TableAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    description: String,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(30.dp)) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(18.dp))
    }
}

// ─── Create / Edit dialog (GroupRequest: name + description) ──────────────────

@Composable
private fun GroupFormDialog(
    initial: GroupRow?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    val lang = appLanguage()
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val nameError = name.isBlank() && error != null

    fun save() {
        if (name.trim().isEmpty()) {
            error = AppStrings.t(lang, "Guruh nomi majburiy")
            return
        }
        scope.launch {
            saving = true
            error = null
            val json = buildString {
                append("{\"name\":${name.trim().jsonString()}")
                val desc = description.trim()
                if (desc.isNotEmpty()) append(",\"description\":${desc.jsonString()}")
                append("}")
            }
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    if (initial == null) ApiClient.post("/api/admin/groups", json)
                    else ApiClient.put("/api/admin/groups/${initial.id}", json)
                }
            }
            saving = false
            result.onSuccess { onSaved() }
                .onFailure { e -> error = e.message ?: AppStrings.t(lang, "Xatolik") }
        }
    }

    Dialog(
        onDismissRequest = { if (!saving) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MexaWarehouseColors.surfaceLowest,
            tonalElevation = 4.dp,
            modifier = Modifier.widthIn(min = 340.dp, max = 540.dp).fillMaxWidth(0.88f),
        ) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (initial == null) AppStrings.t(appLanguage(), "Yangi guruh qo'shish")
                        else AppStrings.t(appLanguage(), "Guruhni tahrirlash"),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MexaWarehouseColors.textPrimary,
                    )
                    IconButton(onClick = { if (!saving) onDismiss() }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = AppStrings.t(appLanguage(), "Yopish"), tint = MexaWarehouseColors.textMuted)
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(AppStrings.t(appLanguage(), "Guruh nomi"), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MexaWarehouseColors.textPrimary)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; error = null },
                        placeholder = { Text(AppStrings.t(appLanguage(), "Masalan: Administratorlar"), color = MexaWarehouseColors.textCaption) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = nameError,
                        shape = RoundedCornerShape(8.dp),
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(AppStrings.t(appLanguage(), "Tavsif"), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MexaWarehouseColors.textPrimary)
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it; error = null },
                        placeholder = { Text(AppStrings.t(appLanguage(), "Guruh haqida qisqa ma'lumot"), color = MexaWarehouseColors.textCaption) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        shape = RoundedCornerShape(8.dp),
                    )
                }

                error?.let { err ->
                    Text(err, color = MexaWarehouseColors.danger, fontSize = 13.sp)
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                ) {
                    OutlinedButton(onClick = { if (!saving) onDismiss() }, shape = RoundedCornerShape(8.dp)) {
                        Text(AppStrings.cancel(appLanguage()))
                    }
                    Button(
                        onClick = { save() },
                        enabled = !saving,
                        colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.indigoAccent),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        if (saving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.White,
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(AppStrings.settingsSave(appLanguage()), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ─── Delete confirmation ──────────────────────────────────────────────────────

@Composable
private fun GroupDeleteDialog(
    group: GroupRow,
    onDismiss: () -> Unit,
    onDeleted: () -> Unit,
) {
    val lang = appLanguage()
    val scope = rememberCoroutineScope()
    var deleting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!deleting) onDismiss() },
        title = {
            Text(
                AppStrings.t(appLanguage(), "Guruhni o'chirish"),
                fontWeight = FontWeight.Bold,
                color = MexaWarehouseColors.textPrimary,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    AppStrings.t(appLanguage(), "\"${group.name}\" guruhini o'chirishni tasdiqlaysizmi? Bu amalni ortga qaytarib bo'lmaydi."),
                    fontSize = 13.sp,
                    color = MexaWarehouseColors.textMuted,
                )
                error?.let { Text(it, fontSize = 12.sp, color = MexaWarehouseColors.danger) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    deleting = true
                    error = null
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            runCatching { ApiClient.delete("/api/admin/groups/${group.id}") }
                        }
                        deleting = false
                        result.onSuccess { onDeleted() }
                            .onFailure { e -> error = e.message ?: AppStrings.t(lang, "Xatolik") }
                    }
                },
                enabled = !deleting,
                colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.danger),
            ) {
                if (deleting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                    Spacer(Modifier.width(8.dp))
                }
                Text(AppStrings.t(appLanguage(), "O'chirish"))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = { if (!deleting) onDismiss() }) {
                Text(AppStrings.cancel(appLanguage()))
            }
        },
    )
}

// ─── Detail dialog: members + add/remove ──────────────────────────────────────

@Composable
private fun GroupDetailDialog(
    group: GroupRow,
    onDismiss: () -> Unit,
    onChanged: () -> Unit,
) {
    val lang = appLanguage()
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var members by remember { mutableStateOf<List<GroupMemberRow>>(emptyList()) }
    var userId by remember { mutableStateOf("") }
    var roleInGroup by remember { mutableStateOf("") }
    var adding by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<String?>(null) }

    fun load() {
        scope.launch {
            loading = true
            error = null
            runCatching {
                withContext(Dispatchers.IO) {
                    val text = ApiClient.get("/api/admin/groups/${group.id}")
                    val data = ApiClient.parseJsonObject(text)?.let { ApiClient.dataObject(it) }
                    val arr = (data?.get("members") as? JsonArray) ?: JsonArray(emptyList())
                    arr.mapNotNull { el ->
                        val m = el as? JsonObject ?: return@mapNotNull null
                        GroupMemberRow(
                            id = m.stringField("id").ifEmpty { return@mapNotNull null },
                            userId = m.stringField("userId"),
                            username = m.stringField("username"),
                            email = m.stringField("email"),
                            roleInGroup = m.stringField("roleInGroup"),
                            joinedAt = m.stringField("joinedAt"),
                        )
                    }
                }
            }.onSuccess { members = it }
                .onFailure { e -> error = e.message ?: AppStrings.t(lang, "Xatolik") }
            loading = false
        }
    }

    fun addMember() {
        if (userId.trim().isEmpty()) {
            actionError = AppStrings.t(lang, "Foydalanuvchi ID majburiy")
            return
        }
        adding = true
        actionError = null
        scope.launch {
            val json = buildString {
                append("{\"userId\":${userId.trim().jsonString()}")
                val role = roleInGroup.trim()
                if (role.isNotEmpty()) append(",\"roleInGroup\":${role.jsonString()}")
                append("}")
            }
            val result = withContext(Dispatchers.IO) {
                runCatching { ApiClient.post("/api/admin/groups/${group.id}/members", json) }
            }
            adding = false
            result.onSuccess {
                userId = ""
                roleInGroup = ""
                onChanged()
                load()
            }.onFailure { e -> actionError = e.message ?: AppStrings.t(lang, "Xatolik") }
        }
    }

    fun removeMember(member: GroupMemberRow) {
        actionError = null
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { ApiClient.delete("/api/admin/groups/${group.id}/members/${member.id}") }
            }
            result.onSuccess {
                onChanged()
                load()
            }.onFailure { e -> actionError = e.message ?: AppStrings.t(lang, "Xatolik") }
        }
    }

    LaunchedEffect(group.id) { load() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MexaWarehouseColors.surfaceLowest,
            tonalElevation = 4.dp,
            modifier = Modifier.widthIn(min = 340.dp, max = 620.dp).fillMaxWidth(0.9f),
        ) {
            Column(
                Modifier
                    .heightIn(max = 640.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            AppStrings.t(appLanguage(), "Guruh tafsilotlari"),
                            fontSize = 13.sp,
                            color = MexaWarehouseColors.textMuted,
                        )
                        Text(
                            group.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MexaWarehouseColors.textPrimary,
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = AppStrings.t(appLanguage(), "Yopish"), tint = MexaWarehouseColors.textMuted)
                    }
                }

                if (group.description.isNotBlank()) {
                    Text(group.description, fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
                }

                Text(
                    AppStrings.t(appLanguage(), "A'zolar"),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MexaWarehouseColors.textPrimary,
                )

                when {
                    loading -> Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MexaWarehouseColors.indigoAccent)
                    }

                    error != null -> Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(error ?: AppStrings.t(appLanguage(), "Xatolik"), color = MexaWarehouseColors.danger, fontSize = 13.sp)
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { load() }, shape = RoundedCornerShape(8.dp)) {
                                Text(AppStrings.t(appLanguage(), "Qayta urinish"))
                            }
                        }
                    }

                    members.isEmpty() -> Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                        Text(AppStrings.t(appLanguage(), "A'zolar topilmadi"), color = MexaWarehouseColors.textMuted, fontSize = 13.sp)
                    }

                    else -> Column(
                        Modifier
                            .fillMaxWidth()
                            .border(1.dp, MexaWarehouseColors.borderSubtle, RoundedCornerShape(10.dp)),
                    ) {
                        members.forEachIndexed { idx, m ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .background(if (idx % 2 == 0) MexaWarehouseColors.surfaceLowest else MexaWarehouseColors.surfaceContainerLow.copy(alpha = 0.35f))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        m.username.ifBlank { m.userId },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MexaWarehouseColors.textPrimary,
                                    )
                                    if (m.email.isNotBlank()) {
                                        Text(m.email, fontSize = 12.sp, color = MexaWarehouseColors.textMuted)
                                    }
                                    if (m.roleInGroup.isNotBlank()) {
                                        Box(
                                            Modifier.padding(top = 4.dp).background(MexaWarehouseColors.infoBadgeBg, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 2.dp),
                                        ) {
                                            Text(m.roleInGroup, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.indigoAccent)
                                        }
                                    }
                                }
                                IconButton(onClick = { removeMember(m) }, modifier = Modifier.size(32.dp)) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = AppStrings.t(appLanguage(), "A'zoni o'chirish"),
                                        tint = MexaWarehouseColors.danger,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                            if (idx < members.lastIndex) {
                                Box(Modifier.fillMaxWidth().height(1.dp).background(MexaWarehouseColors.borderSubtle))
                            }
                        }
                    }
                }

                Box(Modifier.fillMaxWidth().height(1.dp).background(MexaWarehouseColors.borderSubtle))

                Text(
                    AppStrings.t(appLanguage(), "A'zo qo'shish"),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MexaWarehouseColors.textPrimary,
                )

                OutlinedTextField(
                    value = userId,
                    onValueChange = { userId = it; actionError = null },
                    label = { Text(AppStrings.t(appLanguage(), "Foydalanuvchi ID (UUID)")) },
                    placeholder = { Text(AppStrings.t(appLanguage(), "0f8b..."), color = MexaWarehouseColors.textCaption) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                )
                OutlinedTextField(
                    value = roleInGroup,
                    onValueChange = { roleInGroup = it; actionError = null },
                    label = { Text(AppStrings.t(appLanguage(), "Rol (ixtiyoriy)")) },
                    placeholder = { Text(AppStrings.t(appLanguage(), "Masalan: viewer"), color = MexaWarehouseColors.textCaption) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                )

                actionError?.let { Text(it, color = MexaWarehouseColors.danger, fontSize = 13.sp) }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                ) {
                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                        Text(AppStrings.cancel(appLanguage()))
                    }
                    Button(
                        onClick = { addMember() },
                        enabled = !adding && !loading,
                        colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.indigoAccent),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        if (adding) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(AppStrings.t(appLanguage(), "Qo'shish"), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
