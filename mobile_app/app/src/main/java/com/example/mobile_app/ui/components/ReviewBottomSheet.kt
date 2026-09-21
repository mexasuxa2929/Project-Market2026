package com.example.mobile_app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile_app.data.model.MyReview
import com.example.mobile_app.ui.theme.Primary
import com.example.mobile_app.ui.theme.StarColor
import com.example.mobile_app.ui.theme.TextSecondary
import com.example.mobile_app.ui.viewmodel.ReviewViewModel
import com.example.mobile_app.util.tr

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewBottomSheet(
    productName: String,
    existingReview: MyReview?,
    submitState: ReviewViewModel.SubmitState,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSubmit: (rating: Int, comment: String?) -> Unit,
) {
    var selectedRating by remember { mutableIntStateOf(existingReview?.rating ?: 0) }
    var comment        by remember { mutableStateOf(existingReview?.comment ?: "") }

    // Muvaffaqiyatli bo'lganda yopish
    LaunchedEffect(submitState) {
        if (submitState is ReviewViewModel.SubmitState.Success) onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Sarlavha
            Text(
                if (existingReview != null) tr("review_title_update") else tr("review_title_new"),
                fontSize = 18.sp, fontWeight = FontWeight.Bold,
                color = Color(0xFF111827),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                productName,
                fontSize = 13.sp, color = TextSecondary,
                textAlign = TextAlign.Center, maxLines = 2,
            )
            Spacer(Modifier.height(24.dp))

            // Yulduzlar
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                (1..5).forEach { star ->
                    val filled = star <= selectedRating
                    val tint by animateColorAsState(
                        if (filled) StarColor else Color(0xFFD1D5DB),
                        label = "star_$star"
                    )
                    Icon(imageVector = if (filled) Icons.Default.Star else Icons.Outlined.StarBorder,
                        contentDescription = "$star yulduz",
                        tint = tint,
                        modifier = Modifier
                            .size(44.dp)
                            .clickable { selectedRating = star },
                    )
                }
            }

            // Yulduz label
            Spacer(Modifier.height(8.dp))
            Text(
                if (selectedRating > 0) tr("review_star_$selectedRating") else tr("review_star_hint"),
                fontSize = 14.sp,
                color = if (selectedRating > 0) StarColor else TextSecondary,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(Modifier.height(20.dp))

            // Izoh
            OutlinedTextField(
                value = comment,
                onValueChange = { if (it.length <= 4000) comment = it },
                placeholder = { Text(tr("review_placeholder"), fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                shape = RoundedCornerShape(12.dp),
                maxLines = 6,
                trailingIcon = {
                    Text("${comment.length}/4000", fontSize = 10.sp,
                        color = TextSecondary, modifier = Modifier.padding(end = 8.dp))
                }
            )

            Spacer(Modifier.height(16.dp))

            // Xato xabari
            if (submitState is ReviewViewModel.SubmitState.Error) {
                Box(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFEF2F2))
                        .padding(12.dp)
                ) {
                    Text(submitState.message, fontSize = 13.sp, color = Color(0xFFDC2626))
                }
                Spacer(Modifier.height(12.dp))
            }

            // Tugmalar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) { Text(tr("review_cancel"), color = TextSecondary) }

                Button(
                    onClick = {
                        if (selectedRating > 0)
                            onSubmit(selectedRating, comment.trim().ifEmpty { null })
                    },
                    modifier = Modifier.weight(2f),
                    enabled = selectedRating > 0 && submitState !is ReviewViewModel.SubmitState.Loading,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    if (submitState is ReviewViewModel.SubmitState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            if (existingReview != null) tr("review_update") else tr("review_submit"),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
