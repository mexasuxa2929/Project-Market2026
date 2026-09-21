package com.example.mobile_app.ui.screens.profile

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payments
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile_app.ui.theme.Background
import com.example.mobile_app.ui.theme.Primary
import com.example.mobile_app.ui.theme.TextSecondary
import com.example.mobile_app.util.tr

private data class SavedCard(
    val number: String = "",
    val holder: String = "",
    val expiry: String = "",
    val isCash: Boolean = false,
    val gradient: List<Color>? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodsScreen(onBack: () -> Unit) {
    var cards by remember {
        mutableStateOf(
            listOf(
                SavedCard(
                    number = "8600 1234 5678 9012",
                    holder = "USER NAME",
                    expiry = "12/28",
                    gradient = listOf(Color(0xFF3730A3), Color(0xFF4F46E5))
                ),
                SavedCard(isCash = true)
            )
        )
    }
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text(tr("payments_title"), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = Primary
            ) {
                Icon(Icons.Default.Add, contentDescription = tr("payments_add"), tint = Color.White)
            }
        }
    ) { padding ->
        if (cards.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("💳", fontSize = 44.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(tr("payments_empty"), color = TextSecondary, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                itemsIndexed(cards, key = { _, card -> card.number }) { index, card ->
                    PaymentCard(
                        card = card,
                        onDelete = { cards = cards.filterIndexed { i, _ -> i != index } }
                    )
                }
            }
        }

        if (showDialog) {
            AddCardDialog(
                onDismiss = { showDialog = false },
                onSave = { number, holder, expiry ->
                    cards = cards + SavedCard(
                        number = number.trim().chunked(4).joinToString(" "),
                        holder = holder.uppercase(),
                        expiry = expiry,
                        gradient = listOf(Color(0xFF0F766E), Color(0xFF14B8A6))
                    )
                    showDialog = false
                }
            )
        }
    }
}

@Composable
private fun PaymentCard(card: SavedCard, onDelete: () -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (card.isCash) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF064E3B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Payments, null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(tr("payments_cash"), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                        Text(tr("payments_cash") + " · " + tr("payments_delete"), fontSize = 12.sp, color = TextSecondary)
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(card.gradient ?: listOf(Primary, Primary)),
                        RoundedCornerShape(18.dp)
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = card.number,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = tr("payments_delete"), tint = Color.White.copy(alpha = 0.85f))
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text(tr("payments_card_holder"), fontSize = 9.sp, color = Color.White.copy(alpha = 0.7f))
                            Text(card.holder, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.White)
                        }
                        Column {
                            Text(tr("payments_expiry"), fontSize = 9.sp, color = Color.White.copy(alpha = 0.7f))
                            Text(card.expiry, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddCardDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var number by remember { mutableStateOf("") }
    var holder by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("payments_add_title"), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it.filter(Char::isDigit).take(16) },
                    label = { Text(tr("payments_card_number")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = holder,
                    onValueChange = { holder = it },
                    label = { Text(tr("payments_card_holder")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = expiry,
                    onValueChange = { expiry = it.filter { c -> c.isDigit() || c == '/' }.take(5) },
                    label = { Text(tr("payments_expiry")) },
                    placeholder = { Text("MM/YY") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(number, holder, expiry) },
                enabled = number.length == 16 && holder.isNotBlank() && expiry.length == 5
            ) { Text(tr("payments_save")) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(tr("common_cancel"), color = TextSecondary) }
        }
    )
}