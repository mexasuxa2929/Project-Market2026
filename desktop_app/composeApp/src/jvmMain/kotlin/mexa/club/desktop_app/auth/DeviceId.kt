package mexa.club.desktop_app.auth

import java.util.UUID
import java.util.prefs.Preferences

/**
 * Ushbu o'rnatilgan qurilma uchun barqaror identifikator.
 * Login/refresh so'rovlariga yuboriladi: backend shu deviceId bilan faol sessiya topsa,
 * tokenni qayta yaratganda (recreate) yangi qurilma yaratmay, eski sessiyani qayta ishlatadi.
 */
object DeviceId {
    private val prefs: Preferences = Preferences.userNodeForPackage(TokenStore::class.java)
    private const val KEY = "auth_device_id"

    val value: String by lazy {
        val existing = prefs.get(KEY, null)?.trim().orEmpty()
        if (existing.isNotBlank()) {
            existing
        } else {
            UUID.randomUUID().toString().also {
                prefs.put(KEY, it)
                prefs.flush()
            }
        }
    }
}
