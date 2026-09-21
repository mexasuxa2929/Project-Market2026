package mexa.club.desktop_app.auth

import mexa.club.desktop_app.settings.AppPreferences

/**
 * Auth va OTP uchun API bazasi desktop sozlamalaridan olinadi.
 */
object AuthConfig {
    val baseUrl: String
        get() = AppPreferences.baseUrl
}
