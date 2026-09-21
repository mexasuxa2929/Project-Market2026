package com.example.mobile_app

import android.content.Context
import android.graphics.Bitmap
import coil3.ImageLoader
import coil3.request.crossfade
import coil3.size.Precision
import coil3.size.Size
import coil3.request.allowHardware
import coil3.request.bitmapConfig
import coil3.request.maxBitmapSize
import coil3.memory.MemoryCache
import coil3.request.CachePolicy

object CoilConfig {

    fun imageLoader(context: Context): ImageLoader {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory() / (1024 * 1024) // MB
        val isLowRam = maxMemory <= 256 // 256MB yoki kam RAM = past darajadagi qurilma

        val builder = ImageLoader.Builder(context)
            .crossfade(150)
            .precision(Precision.INEXACT)
            .allowHardware(true)
            .bitmapConfig(Bitmap.Config.RGB_565)

        if (isLowRam) {
            // Past darajadagi qurilmalar uchun kichik rasm o'lchami
            builder.maxBitmapSize(Size(800, 800))
            builder.memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.10) // 10% xotira (kam)
                    .build()
            }
        } else {
            builder.memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.15) // 15% xotira
                    .build()
            }
        }

        return builder
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .build()
    }
}
