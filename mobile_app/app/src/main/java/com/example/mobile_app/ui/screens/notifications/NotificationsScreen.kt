package com.example.mobile_app.ui.screens.notifications

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile_app.data.model.notification.AppNotification
import com.example.mobile_app.ui.state.UiState
import com.example.mobile_app.ui.theme.Background
import com.example.mobile_app.ui.theme.Primary
import com.example.mobile_app.ui.theme.TextSecondary
import com.example.mobile_app.ui.viewmodel.NotificationViewModel
import com.example.mobile_app.util.tr

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: NotificationViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.notifications.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()

    // Realtime: yangi bildirishnoma kelganda jimgina yangilash
    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.DisposableEffect(Unit) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: android.content.Context?, intent: android.content.Intent?) {
                viewModel.refreshSilently()
            }
        }
        context.registerReceiver(receiver, android.content.IntentFilter(com.example.mobile_app.MexaMarketApp.ACTION_NOTIFICATION_ADDED), android.content.Context.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (unreadCount > 0) "${tr("notifications_title")} ($unreadCount)"
                        else tr("notifications_title"),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    val hasUnread = unreadCount > 0
                    if (hasUnread) {
                        IconButton(onClick = { viewModel.markAllRead() }) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = tr("notifications_mark_all"),
                                tint = Primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
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
                    Text("😕", fontSize = 44.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(s.message, fontSize = 13.sp, color = Color(0xFFEF4444))
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.load() },
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(tr("notifications_retry"), color = Color.White)
                    }
                }
            }

            is UiState.Success -> {
                if (s.data.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔔", fontSize = 44.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(tr("notifications_empty_title"), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                            Spacer(Modifier.height(6.dp))
                            Text(tr("notifications_empty_subtitle"), fontSize = 13.sp, color = TextSecondary)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(s.data, key = { it.id }) { item ->
                            NotificationCard(
                                item = item,
                                onClick = { viewModel.markRead(item.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(item: AppNotification, onClick: () -> Unit) {
    val (icon, bg) = when (item.channel?.uppercase()) {
        "EMAIL" -> Icons.Default.Email to Color(0xFF4F46E5)
        "SMS", "PHONE" -> Icons.Default.Phone to Color(0xFF0EA5E9)
        else -> Icons.Default.Notifications to Color(0xFF10B981)
    }
    val unread = item.read != true

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (unread) Color(0xFFF5F3FF) else Color.White
        ),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(bg.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = bg, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.subject?.ifBlank { null } ?: tr("notifications_title"),
                        fontSize = 14.sp,
                        fontWeight = if (unread) FontWeight.Bold else FontWeight.SemiBold,
                        color = Color(0xFF0F172A),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val time = shortDate(item.createdAt)
                    if (time != null) {
                        Spacer(Modifier.width(8.dp))
                        Text(time, fontSize = 11.sp, color = TextSecondary)
                    }
                    if (unread) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444))
                        )
                    }
                }
                if (!item.body.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        item.body,
                        fontSize = 13.sp,
                        color = if (unread) Color(0xFF1F2937) else Color(0xFF374151),
                        lineHeight = 18.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun shortDate(iso: String?): String? {
    if (iso.isNullOrBlank()) return null
    val date = iso.substring(0, minOf(10, iso.length))
    val time = if (iso.length >= 16) iso.substring(11, 16) else null
    return if (time != null) "$date $time" else date
}