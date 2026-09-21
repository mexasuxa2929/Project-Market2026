package mexa.club.desktop_app.localization

/**
 * Parol kuchi: uzunlik va belgi xilma-xilligi (kichik, katta harf, raqam, maxsus belgi).
 */
enum class PasswordStrength {
    Empty,
    Weak,
    Fair,
    Good,
    Strong,
}

fun evaluatePasswordStrength(password: String): PasswordStrength {
    if (password.isEmpty()) return PasswordStrength.Empty

    var score = 0
    val len = password.length
    when {
        len >= 14 -> score += 4
        len >= 10 -> score += 3
        len >= 8 -> score += 2
        len >= 6 -> score += 1
    }

    if (password.any { it.isLowerCase() }) score++
    if (password.any { it.isUpperCase() }) score++
    if (password.any { it.isDigit() }) score++
    if (password.any { !it.isLetterOrDigit() && !it.isWhitespace() }) score++

    return when {
        score <= 3 -> PasswordStrength.Weak
        score <= 5 -> PasswordStrength.Fair
        score <= 7 -> PasswordStrength.Good
        else -> PasswordStrength.Strong
    }
}

fun PasswordStrength.filledSegments(): Int =
    when (this) {
        PasswordStrength.Empty -> 0
        PasswordStrength.Weak -> 1
        PasswordStrength.Fair -> 2
        PasswordStrength.Good -> 3
        PasswordStrength.Strong -> 4
    }
