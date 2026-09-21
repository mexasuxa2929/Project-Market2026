package com.example.mobile_app.ui.components

/**
 * Rasm variantlari — mobile app uchun.
 *
 * Backend 3 xil o'lchamda rasm saqlaydi:
 * - THUMB:  300x300 px — card'lar uchun
 * - MEDIUM: 800x800 px — detail sahifasidagi slayder uchun
 * - LARGE:  1920x1080 px — zoom uchun
 *
 * `imageUrls` ro'yxatida barcha variantlar tartib bilan saqlanadi:
 * [THUMB1, MEDIUM1, LARGE1, THUMB2, MEDIUM2, LARGE2, ...]
 */
enum class ImageVariant {
    THUMB,
    MEDIUM,
    LARGE
}
