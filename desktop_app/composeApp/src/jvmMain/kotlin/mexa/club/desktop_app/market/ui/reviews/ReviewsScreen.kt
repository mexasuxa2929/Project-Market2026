package mexa.club.desktop_app.market.ui.reviews

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import mexa.club.desktop_app.market.api.ApiClient
import mexa.club.desktop_app.market.api.stringField
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors

// ─── Data ─────────────────────────────────────────────────────────────────────

private data class ReviewRow(
    val id: String,
    val userId: String,
    val productId: String,
    val username: String,
    val rating: Int,
    val comment: String,
    val createdAt: String,
)

// ─── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun ReviewsScreen(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()

    var reviews       by remember { mutableStateOf<List<ReviewRow>>(emptyList()) }
    var loading       by remember { mutableStateOf(true) }
    var error         by remember { mutableStateOf<String?>(null) }
    var page          by remember { mutableStateOf(0) }
    var totalElements by remember { mutableStateOf(0L) }
    var totalPages    by remember { mutableStateOf(1) }
    var refreshKey    by remember { mutableStateOf(0) }
    var deletingId    by remember { mutableStateOf<String?>(null) }
    var deleteError   by remember { mutableStateOf<String?>(null) }

    // ── Load ──────────────────────────────────────────────────────────────────
    LaunchedEffect(page, refreshKey) {
        loading = true; error = null
        runCatching {
            withContext(Dispatchers.IO) {
                val text = ApiClient.get("/api/shops/admin/reviews?page=$page&size=20")
                val root = ApiClient.parseJsonObject(text)
                val data = root?.get("data") as? JsonObject
                val arr  = data?.get("content") as? JsonArray
                totalElements = (data?.get("totalElements") as? JsonPrimitive)?.content?.toLongOrNull() ?: 0L
                totalPages    = (data?.get("totalPages")    as? JsonPrimitive)?.content?.toIntOrNull()  ?: 1
                arr?.filterIsInstance<JsonObject>()?.map { r ->
                    ReviewRow(
                        id        = r.stringField("id"),
                        userId    = r.stringField("userId"),
                        productId = r.stringField("productId"),
                        username  = r.stringField("username").ifBlank { "—" },
                        rating    = (r["rating"] as? JsonPrimitive)?.content?.toIntOrNull() ?: 0,
                        comment   = r.stringField("comment"),
                        createdAt = r.stringField("createdAt").take(10),
                    )
                } ?: emptyList()
            }
        }.onSuccess { reviews = it }.onFailure { error = it.message }
        loading = false
    }

    // ── Delete dialog ─────────────────────────────────────────────────────────
    deletingId?.let { rid ->
        AlertDialog(
            onDismissRequest = { deletingId = null; deleteError = null },
            title  = { Text("Sharhni o'chirish", fontWeight = FontWeight.Bold) },
            text   = {
                Column {
                    Text("Ushbu sharhni o'chirishni tasdiqlaysizmi?")
                    deleteError?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = MexaWarehouseColors.error, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            runCatching {
                                withContext(Dispatchers.IO) { ApiClient.delete("/api/shops/admin/reviews/$rid") }
                            }.onSuccess {
                                deletingId = null; deleteError = null
                                if (page == 0) refreshKey++ else page = 0
                            }.onFailure { deleteError = it.message }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.error),
                    shape  = RoundedCornerShape(8.dp),
                ) { Text("O'chirish", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { deletingId = null; deleteError = null }) { Text("Bekor") }
            },
            shape = RoundedCornerShape(12.dp),
        )
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // Header
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Column {
                Text("Sharhlar", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textPrimary)
                Text("Foydalanuvchilar sharhlari va reytinglari", fontSize = 14.sp, color = MexaWarehouseColors.textMuted)
            }
            IconButton(
                onClick = { if (page == 0) refreshKey++ else page = 0 },
                modifier = Modifier
                    .size(38.dp)
                    .border(1.dp, MexaWarehouseColors.outlineVariant, RoundedCornerShape(8.dp))
                    .background(MexaWarehouseColors.surfaceLowest, RoundedCornerShape(8.dp)),
            ) {
                Icon(Icons.Filled.Refresh, "Yangilash", tint = MexaWarehouseColors.textMuted, modifier = Modifier.size(18.dp))
            }
        }

        // Error
        error?.let {
            Text(it, color = MexaWarehouseColors.error, fontSize = 13.sp)
        }

        // Jami
        if (!loading) {
            Text("$totalElements ta sharh topildi", fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
        }

        // Table
        androidx.compose.material3.Surface(
            modifier   = Modifier.fillMaxWidth(),
            shape      = RoundedCornerShape(12.dp),
            color      = MexaWarehouseColors.surfaceLowest,
            border     = androidx.compose.foundation.BorderStroke(1.dp, MexaWarehouseColors.borderSubtle),
        ) {
            Column {
                // Header
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(MexaWarehouseColors.tableHeaderBg)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("FOYDALANUVCHI", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                        color = MexaWarehouseColors.textMuted, modifier = Modifier.weight(1f))
                    Text("MAHSULOT ID", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                        color = MexaWarehouseColors.textMuted, modifier = Modifier.weight(1.4f))
                    Text("REYTING", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                        color = MexaWarehouseColors.textMuted, modifier = Modifier.width(90.dp))
                    Text("SHARH", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                        color = MexaWarehouseColors.textMuted, modifier = Modifier.weight(2f))
                    Text("SANA", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                        color = MexaWarehouseColors.textMuted, modifier = Modifier.width(90.dp))
                    Spacer(Modifier.width(48.dp))
                }

                HorizontalDivider(color = MexaWarehouseColors.borderSubtle)

                when {
                    loading -> Box(Modifier.fillMaxWidth().height(200.dp), Alignment.Center) {
                        CircularProgressIndicator(color = MexaWarehouseColors.primary, modifier = Modifier.size(36.dp))
                    }
                    reviews.isEmpty() -> Box(Modifier.fillMaxWidth().height(200.dp), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("💬", fontSize = 40.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("Sharhlar yo'q", fontSize = 14.sp, color = MexaWarehouseColors.textMuted)
                        }
                    }
                    else -> reviews.forEachIndexed { idx, r ->
                        ReviewTableRow(
                            review    = r,
                            onDelete  = { deletingId = r.id },
                        )
                        if (idx < reviews.lastIndex)
                            HorizontalDivider(color = MexaWarehouseColors.borderSubtle.copy(alpha = 0.5f))
                    }
                }
            }
        }

        // Pagination
        if (!loading && totalPages > 1) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = { if (page > 0) page-- },
                    enabled = page > 0,
                    shape   = RoundedCornerShape(8.dp),
                ) { Text("← Oldingi") }
                Spacer(Modifier.width(12.dp))
                Text("${page + 1} / $totalPages", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.width(12.dp))
                OutlinedButton(
                    onClick = { if (page < totalPages - 1) page++ },
                    enabled = page < totalPages - 1,
                    shape   = RoundedCornerShape(8.dp),
                ) { Text("Keyingi →") }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

// ─── Row ──────────────────────────────────────────────────────────────────────

@Composable
private fun ReviewTableRow(review: ReviewRow, onDelete: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Foydalanuvchi
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier.size(30.dp).background(MexaWarehouseColors.primary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(review.username.firstOrNull()?.uppercase() ?: "?", fontSize = 12.sp,
                    fontWeight = FontWeight.Bold, color = MexaWarehouseColors.primary)
            }
            Text(review.username, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                color = MexaWarehouseColors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        // Mahsulot ID
        Text(
            review.productId.take(8) + "…",
            fontSize = 12.sp, color = MexaWarehouseColors.textMuted,
            modifier = Modifier.weight(1.4f), maxLines = 1,
        )

        // Reyting
        Row(Modifier.width(90.dp), verticalAlignment = Alignment.CenterVertically) {
            repeat(5) { i ->
                Text(
                    if (i < review.rating) "★" else "☆",
                    fontSize = 13.sp,
                    color = if (i < review.rating) Color(0xFFF59E0B) else MexaWarehouseColors.borderSubtle,
                )
            }
        }

        // Sharh
        Text(
            review.comment.ifBlank { "—" },
            fontSize = 12.sp, color = MexaWarehouseColors.textMuted,
            modifier = Modifier.weight(2f), maxLines = 2, overflow = TextOverflow.Ellipsis,
        )

        // Sana
        Text(review.createdAt, fontSize = 11.sp, color = MexaWarehouseColors.textMuted, modifier = Modifier.width(90.dp))

        // O'chirish
        Box(Modifier.width(48.dp), contentAlignment = Alignment.Center) {
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Delete, "O'chirish",
                    tint = MexaWarehouseColors.error, modifier = Modifier.size(16.dp))
            }
        }
    }
}
