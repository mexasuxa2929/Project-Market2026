package mexa.club.desktop_app.market.users

import mexa.club.desktop_app.auth.AdminUserListItem
import mexa.club.desktop_app.localization.AppLanguage
import mexa.club.desktop_app.market.model.AdminUser
import mexa.club.desktop_app.market.model.AuthProvider
import mexa.club.desktop_app.market.model.UserRole
import mexa.club.desktop_app.market.model.UserStatus
import mexa.club.desktop_app.market.model.backendRoleToUserRole
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun AdminUserListItem.toAdminUser(): AdminUser {
    val rawRoleNames = roles                                          // asl kodlar saqlash
    val mappedRoles  = roles.mapNotNull(::backendRoleToUserRole).distinct()
    // Backendda real to'liq ism bo'lsa undan, aks holda username dan fallback
    // "null" stringi ham null deb hisoblanadi (backend bugdan "null" qaytishi mumkin)
    val display = fullName?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
        ?: username.replaceFirstChar { it.uppercase() }
    return AdminUser(
        id           = id,
        username     = username,
        email        = email,
        fullName     = display,
        roles        = mappedRoles.ifEmpty { listOf(UserRole.USER) },
        rawRoleNames = rawRoleNames,                                  // ← yangi maydon
        status       = if (enabled) UserStatus.ACTIVE else UserStatus.BLOCKED,
        isVerified   = verified,
        authProvider = when (provider.uppercase()) {
            "GOOGLE" -> AuthProvider.GOOGLE
            else     -> AuthProvider.LOCAL
        },
        verifiedLabel = if (verified) "Tasdiqlangan" else "Kutilmoqda",
        lastActivity  = lastActivityLabel(lastActiveAt, online),
        createdAt     = createdAt,
        lastActiveAt  = lastActiveAt,
        online        = online,
        blockReason   = blockReason,
        blockedAt     = blockedAt,
    )
}

private val dateTimeFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

/**
 * "SO'NGGI FAOLLIK" ustuni uchun:
 *  - onlayn bo'lsa — "Onlayn" / "Онлайн"
 *  - aks holda ISO vaqtdan "kun.moy.yil soat:daqiqa" ko'rinishi
 *  - ma'lumot bo'lmasa — "—"
 */
fun lastActivityLabel(iso: String?, online: Boolean, lang: AppLanguage = AppLanguage.UZ): String {
    if (online) {
        return when (lang) {
            AppLanguage.UZ -> "Onlayn"
            AppLanguage.RU -> "Онлайн"
        }
    }
    val parsed = parseIso(iso) ?: return "—"
    return parsed.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime().format(dateTimeFmt)
}

internal fun parseIso(iso: String?): OffsetDateTime? {
    if (iso.isNullOrBlank()) return null
    runCatching { return OffsetDateTime.parse(iso) }
    runCatching { return LocalDateTime.parse(iso).atOffset(java.time.ZoneOffset.UTC) }
    return null
}

/** Bloklash sanasi va sababini ko'rsatish uchun yordamchi. */
fun blockedInfoLabel(blockedAt: String?, lang: AppLanguage): String? {
    val parsed = parseIso(blockedAt) ?: return null
    val local = parsed.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime().format(dateTimeFmt)
    return when (lang) {
        AppLanguage.UZ -> "Bloklangan: $local"
        AppLanguage.RU -> "Заблокирован: $local"
    }
}
