package mexa.club.desktop_app.settings

import java.util.prefs.Preferences

object AppPreferences {
    private val prefs: Preferences = Preferences.userNodeForPackage(AppPreferences::class.java)

    private const val KEY_BASE_URL = "api_base_url"
    private const val KEY_LANGUAGE = "app_language"

    // ─────────────────────────────────────────────────────────────────────────
    // ASOSIY SOZLAMA — serverga o'tganda FAQAT SHU QATORNI o'zgartiring:
    //
    //   LOCAL:   "http://127.0.0.1:8080"
    //   SERVER:  "https://api.mexa.club"   ← shu yerga server URL qo'ying
    // ─────────────────────────────────────────────────────────────────────────
    private const val DEFAULT_URL = "http://127.0.0.1:8080"

    var baseUrl: String
        get() {
            val raw = prefs.get(KEY_BASE_URL, DEFAULT_URL).trimEnd('/')
            return if (raw.contains("://localhost")) {
                raw.replace("://localhost", "://127.0.0.1")
            } else {
                raw
            }
        }
        set(value) {
            prefs.put(KEY_BASE_URL, value.trimEnd('/'))
        }

    var languageCode: String
        get() = prefs.get(KEY_LANGUAGE, "UZ")
        set(value) {
            prefs.put(KEY_LANGUAGE, value)
        }

    /** Saqlangan URL ni o'chirib DEFAULT_URL ga qaytaradi. */
    fun resetUrl() {
        prefs.remove(KEY_BASE_URL)
    }

    /** Barcha sozlamalarni default ga qaytaradi. */
    fun reset() {
        prefs.remove(KEY_BASE_URL)
        prefs.remove(KEY_LANGUAGE)
    }

    /** Joriy saqlangan URL yoki default. Debug uchun. */
    fun debugInfo(): String =
        "baseUrl=${baseUrl}  (saved=${prefs.get(KEY_BASE_URL, "(none)")}, default=$DEFAULT_URL)"
}
