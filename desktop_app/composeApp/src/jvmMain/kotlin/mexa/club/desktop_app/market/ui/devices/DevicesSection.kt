package mexa.club.desktop_app.market.ui.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mexa.club.desktop_app.auth.AuthApiClient
import mexa.club.desktop_app.auth.AuthApiResult
import mexa.club.desktop_app.auth.DeviceSessionItem
import mexa.club.desktop_app.localization.AppLanguage
import mexa.club.desktop_app.localization.AppStrings
import mexa.club.desktop_app.localization.appLanguage
import mexa.club.desktop_app.market.ui.components.PaginationBar
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors

private fun fmtTs(raw: String?): String =
    raw?.takeIf { it.isNotBlank() }?.replace('T', ' ')?.take(16) ?: "—"

private fun deviceLabel(s: DeviceSessionItem): String {
    val info = s.deviceInfo?.trim().orEmpty()
    return when {
        info.isBlank() -> s.ipAddress ?: "—"
        info.length > 44 -> info.take(44) + "…"
        else -> info
    }
}

/** Settings ichidagi "Qurilmalar" kartasi: faol sessiyalar ro'yxati + yopish. */
@Composable
fun DevicesSection(modifier: Modifier = Modifier) {
    val lang = appLanguage()
    val scope = rememberCoroutineScope()
    val api: AuthApiClient = org.koin.compose.koinInject { org.koin.core.parameter.parametersOf(AppStrings.authNetwork(lang)) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var sessions by remember { mutableStateOf<List<DeviceSessionItem>>(emptyList()) }
    var pendingClose by remember { mutableStateOf<DeviceSessionItem?>(null) }
    var closing by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }

    fun reload() {
        scope.launch {
            loading = true
            error = null
            when (val r = api.listSessions()) {
                is AuthApiResult.Ok -> sessions = r.value
                is AuthApiResult.Err -> error = r.message
            }
            loading = false
        }
    }

    LaunchedEffect(Unit) { reload() }

    Box(modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .border(1.dp, MexaWarehouseColors.borderSubtle, RoundedCornerShape(12.dp))
                .background(MexaWarehouseColors.surfaceLowest, RoundedCornerShape(12.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    Icons.Default.Devices,
                    contentDescription = null,
                    tint = MexaWarehouseColors.indigoAccent,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    AppStrings.devicesTitle(lang),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MexaWarehouseColors.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    color = MexaWarehouseColors.indigoAccent.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(999.dp),
                ) {
                    Text(
                        AppStrings.devicesCount(lang, sessions.size),
                        Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MexaWarehouseColors.indigoAccent,
                        maxLines = 1,
                    )
                }
                IconButton(onClick = { reload() }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = AppStrings.devicesRefresh(lang),
                        tint = MexaWarehouseColors.textMuted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Text(
                AppStrings.devicesHint(lang),
                fontSize = 12.sp,
                color = MexaWarehouseColors.textMuted,
                lineHeight = 17.sp,
            )

            when {
                loading -> Box(Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MexaWarehouseColors.indigoAccent)
                }

                error != null -> Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${AppStrings.devicesLoadError(lang)}: ${error ?: ""}",
                            color = MexaWarehouseColors.danger,
                            fontSize = 13.sp,
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = { reload() }) { Text("Qayta urinish", fontSize = 13.sp) }
                    }
                }

                sessions.isEmpty() -> Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                    Text(AppStrings.devicesEmpty(lang), color = MexaWarehouseColors.textMuted, fontSize = 13.sp)
                }

                else -> DevicesTable(
                    sessions = sessions,
                    lang = lang,
                    onCloseRequest = { pendingClose = it },
                )
            }
        }

        notice?.let { msg ->
            LaunchedEffect(msg) {
                delay(3500)
                notice = null
            }
            Surface(
                Modifier.align(Alignment.TopCenter).padding(top = 4.dp),
                shape = RoundedCornerShape(10.dp),
                color = MexaWarehouseColors.surfaceLowest,
                shadowElevation = 6.dp,
            ) {
                Text(
                    msg,
                    Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MexaWarehouseColors.textPrimary,
                )
            }
        }
    }

    pendingClose?.let { target ->
        AlertDialog(
            onDismissRequest = { if (!closing) pendingClose = null },
            title = {
                Text(
                    AppStrings.devicesCloseTitle(lang),
                    fontWeight = FontWeight.Bold,
                    color = MexaWarehouseColors.textPrimary,
                )
            },
            text = {
                Text(
                    AppStrings.devicesCloseMessage(lang, deviceLabel(target)),
                    fontSize = 13.sp,
                    color = MexaWarehouseColors.textMuted,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        closing = true
                        scope.launch {
                            when (val r = api.closeSession(target.id)) {
                                is AuthApiResult.Ok -> notice = AppStrings.devicesClosed(lang)
                                is AuthApiResult.Err -> notice = r.message
                            }
                            closing = false
                            pendingClose = null
                            reload()
                        }
                    },
                    enabled = !closing,
                    colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.danger),
                ) {
                    if (closing) {
                        CircularProgressIndicator(
                            Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(AppStrings.devicesClose(lang))
                    }
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { pendingClose = null }, enabled = !closing) {
                    Text(AppStrings.cancel(lang))
                }
            },
        )
    }
}

@Composable
private fun DevicesTable(
    sessions: List<DeviceSessionItem>,
    lang: AppLanguage,
    onCloseRequest: (DeviceSessionItem) -> Unit,
) {
    var listPage by remember { mutableStateOf(0) }
    var listPageSize by remember { mutableStateOf(10) }
    val totalPages = if (sessions.isEmpty()) 1 else (sessions.size + listPageSize - 1) / listPageSize
    val safePage = listPage.coerceIn(0, totalPages - 1)
    val paged = sessions.drop(safePage * listPageSize).take(listPageSize)
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(MexaWarehouseColors.tableHeaderBg, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .padding(vertical = 10.dp, horizontal = 14.dp),
        ) {
            HeaderCell(AppStrings.devicesColDevice(lang), 3f)
            HeaderCell(AppStrings.devicesColIp(lang), 1.4f)
            HeaderCell(AppStrings.devicesColLoggedIn(lang), 1.6f)
            HeaderCell(AppStrings.devicesColLastActive(lang), 1.6f)
            HeaderCell(AppStrings.devicesColStatus(lang), 1.5f)
            HeaderCell(AppStrings.devicesColAction(lang), 1f)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(MexaWarehouseColors.borderSubtle))

        paged.forEachIndexed { idx, s ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(
                        if (idx % 2 == 0) MexaWarehouseColors.surfaceLowest
                        else MexaWarehouseColors.surfaceContainerLow.copy(alpha = 0.35f),
                    )
                    .padding(vertical = 10.dp, horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val info = s.deviceInfo?.trim().orEmpty().ifBlank { "—" }
                Text(
                    info,
                    Modifier.weight(3f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MexaWarehouseColors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    s.ipAddress ?: "—",
                    Modifier.weight(1.4f),
                    fontSize = 13.sp,
                    color = MexaWarehouseColors.textPrimary,
                )
                Text(
                    fmtTs(s.createdAt),
                    Modifier.weight(1.6f),
                    fontSize = 13.sp,
                    color = MexaWarehouseColors.textMuted,
                )
                Text(
                    fmtTs(s.lastUsedAt),
                    Modifier.weight(1.6f),
                    fontSize = 13.sp,
                    color = MexaWarehouseColors.textMuted,
                )
                Box(Modifier.weight(1.5f)) {
                    if (s.current) {
                        Surface(
                            color = MexaWarehouseColors.greenBadgeBg,
                            shape = RoundedCornerShape(999.dp),
                        ) {
                            Text(
                                AppStrings.devicesCurrentBadge(lang),
                                Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MexaWarehouseColors.greenBadgeFg,
                                maxLines = 1,
                            )
                        }
                    } else {
                        Surface(
                            color = MexaWarehouseColors.outline.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(999.dp),
                        ) {
                            Text(
                                AppStrings.devicesActiveBadge(lang),
                                Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MexaWarehouseColors.textMuted,
                                maxLines = 1,
                            )
                        }
                    }
                }
                Box(Modifier.weight(1f)) {
                    if (s.current) {
                        Text("—", fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
                    } else {
                        TextButton(onClick = { onCloseRequest(s) }) {
                            Text(
                                AppStrings.devicesClose(lang),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MexaWarehouseColors.danger,
                            )
                        }
                    }
                }
            }
            if (idx < paged.lastIndex) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(MexaWarehouseColors.borderSubtle))
            }
        }

        if (sessions.isNotEmpty()) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(MexaWarehouseColors.borderSubtle))
            val fromIdx = safePage * listPageSize + 1
            val toIdx = minOf((safePage + 1) * listPageSize, sessions.size)
            PaginationBar(
                page = safePage,
                pageSize = listPageSize,
                totalElements = sessions.size,
                infoText = "$fromIdx - $toIdx / ${sessions.size} ta qurilma",
                onPageChange = { listPage = it },
                onPageSizeChange = { s -> listPageSize = s; listPage = 0 },
            )
        }
    }
}

@Composable
private fun RowScope.HeaderCell(label: String, weight: Float) {
    Text(
        label.uppercase(),
        modifier = Modifier.weight(weight),
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.4.sp,
        color = MexaWarehouseColors.outline,
        maxLines = 1,
    )
}
