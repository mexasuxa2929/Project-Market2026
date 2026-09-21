package com.example.mobile_app.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.bitmapConfig
import coil3.request.crossfade
import coil3.size.Precision
import coil3.size.Scale
import coil3.request.CachePolicy
import coil3.request.allowHardware
import com.example.mobile_app.R

@Composable
fun ProductImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    size: Pair<Int, Int>? = null,
    cornerRadius: Dp = 0.dp,
    placeholderSize: Dp = 48.dp,
    allowHardware: Boolean = true
) {
    val shape = if (cornerRadius > 0.dp) RoundedCornerShape(cornerRadius) else RoundedCornerShape(0.dp)

    if (url.isNullOrBlank()) {
        Box(
            modifier = modifier
                .clip(shape)
                .background(Color(0xFFF3F4F6)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.placeholder_product),
                contentDescription = null,
                modifier = Modifier.size(placeholderSize),
                tint = Color(0xFFBDBDBD)
            )
        }
        return
    }

    val requestBuilder = ImageRequest.Builder(LocalContext.current)
        .data(url)
        .crossfade(30)
        .scale(Scale.FILL)
        .bitmapConfig(Bitmap.Config.RGB_565)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .diskCachePolicy(CachePolicy.ENABLED)
        .networkCachePolicy(CachePolicy.ENABLED)
        .allowHardware(allowHardware)

    if (size != null) {
        // Aniq o'lcham berilgan bo'lsa — shu o'lchamda decode qilish
        requestBuilder
            .size(size.first, size.second)
            .precision(Precision.EXACT)
    } else {
        // Aniq o'lcham berilmagan ro'yxat kartalari uchun 512px gacha decode —
        // asl o'lchamni to'liq decode qilmaslik (xotira + tezlik uchun)
        requestBuilder
            .size(512, 512)
            .precision(Precision.INEXACT)
    }

    AsyncImage(
        model = requestBuilder.build(),
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier.clip(shape),
        placeholder = painterResource(R.drawable.placeholder_product),
        error = painterResource(R.drawable.placeholder_error)
    )
}
