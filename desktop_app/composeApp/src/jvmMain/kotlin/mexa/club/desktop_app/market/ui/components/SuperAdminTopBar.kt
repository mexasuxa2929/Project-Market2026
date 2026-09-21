package mexa.club.desktop_app.market.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors

@Composable
fun SuperAdminTopBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    displayName: String,
    subtitle: String,
    avatarInitials: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(MexaWarehouseColors.surfaceLowest)
            .border(1.dp, MexaWarehouseColors.outlineVariant)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            Modifier
                .weight(1f),
            contentAlignment = Alignment.CenterStart,
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = {
                    Text(
                        "Tizim bo'ylab qidiruv…",
                        color = MexaWarehouseColors.outline,
                        fontSize = 14.sp,
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = null,
                        tint = MexaWarehouseColors.outline,
                    )
                },
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MexaWarehouseColors.primary,
                    unfocusedBorderColor = MexaWarehouseColors.outlineVariant,
                    focusedContainerColor = MexaWarehouseColors.surfaceContainerLow,
                    unfocusedContainerColor = MexaWarehouseColors.surfaceContainerLow,
                    cursorColor = MexaWarehouseColors.primary,
                    focusedTextColor = MexaWarehouseColors.onSurface,
                    unfocusedTextColor = MexaWarehouseColors.onSurface,
                ),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = {}) {
                        Icon(
                            Icons.Filled.Notifications,
                            contentDescription = null,
                            tint = MexaWarehouseColors.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = {}) {
                        Icon(
                            Icons.AutoMirrored.Filled.Help,
                            contentDescription = null,
                            tint = MexaWarehouseColors.onSurfaceVariant,
                        )
                    }
                }
                Box(
                    Modifier
                        .width(1.dp)
                        .height(32.dp)
                        .background(MexaWarehouseColors.outlineVariant),
                )
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            displayName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MexaWarehouseColors.onSurface,
                        )
                        Text(
                            subtitle.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MexaWarehouseColors.onSurfaceVariant,
                        )
                    }
                Box(
                    Modifier
                        .size(40.dp)
                        .background(MexaWarehouseColors.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        avatarInitials,
                        color = MexaWarehouseColors.surfaceLowest,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}
