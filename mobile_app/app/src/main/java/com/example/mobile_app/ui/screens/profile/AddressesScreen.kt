package com.example.mobile_app.ui.screens.profile

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Work
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mobile_app.data.model.address.AddressResponse
import com.example.mobile_app.data.model.address.CreateAddressRequest
import com.example.mobile_app.data.model.address.GeoPlace
import com.example.mobile_app.ui.state.UiState
import com.example.mobile_app.ui.theme.Background
import com.example.mobile_app.ui.theme.Primary
import com.example.mobile_app.ui.theme.TextSecondary
import com.example.mobile_app.ui.viewmodel.AddressViewModel
import com.example.mobile_app.util.tr

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressesScreen(
    onBack: () -> Unit,
    onOpenMap: () -> Unit = {},
    mapPicked: GeoPlace? = null,
    onMapPickedConsumed: () -> Unit = {},
    addressViewModel: AddressViewModel = viewModel(factory = AddressViewModel.Factory)
) {
    val state by addressViewModel.addresses.collectAsState()
    val saving by addressViewModel.saving.collectAsState()
    val message by addressViewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Dialog + forma holati ViewModel'da — xaritaga o'tib qaytganda ham
    // dialog yopilmaydi va yozilganlar o'chmaydi.
    val dialogOpen by addressViewModel.dialogOpen.collectAsState()
    val editingId by addressViewModel.editingId.collectAsState()
    val serviceMap by addressViewModel.serviceMap.collectAsState()
    val formLabel by addressViewModel.formLabel.collectAsState()
    val formLine2 by addressViewModel.formLine2.collectAsState()
    val formIsDefault by addressViewModel.formIsDefault.collectAsState()
    val formPicked by addressViewModel.formPicked.collectAsState()
    var deleting by remember { mutableStateOf<AddressResponse?>(null) }
    LaunchedEffect(mapPicked) {
        if (mapPicked != null) {
            addressViewModel.onMapPicked(mapPicked)
            onMapPickedConsumed()
        }
    }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            addressViewModel.consumeMessage()
        }
    }

    Scaffold(
        containerColor = Background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                modifier = Modifier.height(48.dp),
                title = { Text(tr("addresses_title"), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A)) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color(0xFF0F172A), modifier = Modifier.size(20.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    addressViewModel.clearSearch()
                    addressViewModel.openAddDialog()
                },
                containerColor = Primary
            ) {
                Icon(Icons.Default.Add, contentDescription = tr("addresses_add"), tint = Color.White)
            }
        }
    ) { padding ->
        // Tahrirlanayotgan manzil — id bo'yicha ro'yxatdan topiladi.
        val editing = (state as? UiState.Success)?.data?.firstOrNull { it.id == editingId }
        when (val s = state) {
            is UiState.Loading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Primary, strokeWidth = 2.dp)
            }

            is UiState.Error -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📍", fontSize = 44.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(s.message, color = TextSecondary, fontSize = 14.sp)
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { addressViewModel.load() },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Primary),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text(tr("catalog_retry"), color = Color.White) }
                }
            }

            is UiState.Success -> {
                val addresses = s.data
                if (addresses.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📍", fontSize = 64.sp)
                            Spacer(Modifier.height(16.dp))
                            Text(tr("addresses_empty"), color = Color(0xFF0F172A), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(addresses, key = { it.id }) { item ->
                            AddressCard(
                                address = item,
                                served = serviceMap[item.id],
                                onEdit = { addressViewModel.openEditDialog(item) },
                                onDelete = { deleting = item },
                                onSetDefault = { addressViewModel.setDefault(item.id) }
                            )
                        }
                    }
                }
            }
        }

        if (dialogOpen) {
            AddressEditDialog(
                title = if (editing == null) tr("addresses_add_title") else tr("addresses_edit"),
                saving = saving,
                label = formLabel,
                onLabelChange = addressViewModel::setFormLabel,
                line2 = formLine2,
                onLine2Change = addressViewModel::setFormLine2,
                isDefault = formIsDefault,
                onIsDefaultChange = addressViewModel::setFormIsDefault,
                picked = formPicked,
                onOpenMap = onOpenMap,
                onDismiss = { addressViewModel.closeDialog() },
                onSave = { addressViewModel.submitDialog() }
            )
        }

        deleting?.let { address ->
            AlertDialog(
                onDismissRequest = { deleting = null },
                shape = RoundedCornerShape(20.dp),
                containerColor = Color.White,
                titleContentColor = Color(0xFF0F172A),
                textContentColor = TextSecondary,
                title = { Text(tr("addresses_delete_confirm_title"), fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                text = { Text(tr("addresses_delete_confirm_body"), fontSize = 14.sp) },
                confirmButton = {
                    androidx.compose.material3.Button(
                        onClick = {
                            addressViewModel.deleteAddress(address.id)
                            deleting = null
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                    ) { Text(tr("addresses_delete"), color = Color.White) }
                },
                dismissButton = {
                    TextButton(onClick = { deleting = null }) { Text(tr("addresses_cancel"), color = TextSecondary) }
                }
            )
        }
    }
}

@Composable
private fun ServiceStatusChip(served: Boolean) {
    val bg = if (served) Color(0xFFE8F8EF) else Color(0xFFFDECEC)
    val fg = if (served) Color(0xFF16A34A) else Color(0xFFEF4444)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = if (served) "✓ " else "✕ ",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = fg
        )
        Text(
            text = tr(if (served) "addresses_served" else "addresses_not_served"),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = fg
        )
    }
}

@Composable
private fun AddressCard(
    address: AddressResponse,
    served: Boolean?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            // Birinchi qator: ikon + sarlavha (ixcham, 1-2 qator)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (address.label?.lowercase()) {
                            "uy", "home", "домашний" -> Icons.Default.Home
                            "ish", "office", "офис" -> Icons.Default.Work
                            else -> Icons.Default.LocationOn
                        },
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            address.label?.ifBlank { tr("addresses_other") } ?: tr("addresses_other"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (address.defaultAddress == true) {
                            Spacer(Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Primary.copy(alpha = 0.12f))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    tr("addresses_default"),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Primary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = address.displayName.ifBlank { tr("home_delivery_address") },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        softWrap = false
                    )
                }
            }
            // Mo'ljal / kichik manzil — ixcham
            if (!address.line2.isNullOrBlank() && address.line2 != address.displayName) {
                Spacer(Modifier.height(6.dp))
                Text(
                    address.line2!!,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            if (served != null) {
                Spacer(Modifier.height(8.dp))
                ServiceStatusChip(served = served)
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFFF3F4F6))
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (address.defaultAddress != true) {
                    TextButton(
                        onClick = onSetDefault,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(tr("addresses_set_default"), fontSize = 11.sp, color = Primary, fontWeight = FontWeight.Medium)
                    }
                } else {
                    Spacer(Modifier.width(4.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = tr("addresses_edit"), tint = Color(0xFF6B7280), modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = tr("addresses_delete"), tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AddressEditDialog(
    title: String,
    saving: Boolean = false,
    label: String,
    onLabelChange: (String) -> Unit,
    line2: String,
    onLine2Change: (String) -> Unit,
    isDefault: Boolean,
    onIsDefaultChange: (Boolean) -> Unit,
    picked: GeoPlace?,
    onOpenMap: () -> Unit = {},
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val fieldColors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Primary,
        unfocusedBorderColor = Color(0xFFD1D5DB),
        focusedLabelColor = Primary,
        unfocusedLabelColor = TextSecondary,
        cursorColor = Primary,
        focusedTextColor = Color(0xFF0F172A),
        unfocusedTextColor = Color(0xFF0F172A)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        // Tashqariga bexos bosilganda dialog yopilmasligi uchun —
        // faqat "Bekor qilish"/"Saqlash" yopadi (yozilganlar saqlanadi).
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        ),
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White,
        titleContentColor = Color(0xFF0F172A),
        textContentColor = Color(0xFF0F172A),
        title = { Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // Tanlangan nuqta + xarita tugmasi (qidiruv olib tashlandi)
                picked?.let { p ->
                    Text(
                        "${tr("address_picked_label")}: ${p.name}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Primary,
                        maxLines = 2
                    )
                }
                OutlinedButton(
                    onClick = onOpenMap,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.LocationOn, null, tint = Primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(tr("address_pick_on_map"), color = Primary, fontWeight = FontWeight.SemiBold)
                }
                OutlinedTextField(
                    value = label,
                    onValueChange = onLabelChange,
                    label = { Text(tr("addresses_name_label")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors
                )
                OutlinedTextField(
                    value = line2,
                    onValueChange = onLine2Change,
                    label = { Text(tr("addresses_line2_label")) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Switch(
                        checked = isDefault,
                        onCheckedChange = onIsDefaultChange,
                        colors = androidx.compose.material3.SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Primary,
                            checkedBorderColor = Primary,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFD1D5DB),
                            uncheckedBorderColor = Color(0xFFD1D5DB)
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(tr("addresses_is_default"), fontSize = 14.sp, color = Color(0xFF0F172A))
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = onSave,
                enabled = picked != null && !saving,
                shape = RoundedCornerShape(12.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                if (saving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text(tr("addresses_save"), color = Color.White)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(tr("addresses_cancel"), color = TextSecondary) }
        }
    )
}