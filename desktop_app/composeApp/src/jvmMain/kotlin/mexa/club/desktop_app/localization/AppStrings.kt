package mexa.club.desktop_app.localization

/**
 * Barcha foydalanuvchi matnlari: o‘zbek (UZ) va rus (RU).
 */
object AppStrings {

    fun ok(lang: AppLanguage): String = when (lang) {
        AppLanguage.UZ -> "OK"
        AppLanguage.RU -> "OK"
        AppLanguage.EN -> "OK"
    }

    fun cancel(lang: AppLanguage): String = when (lang) {
        AppLanguage.UZ -> "Bekor qilish"
        AppLanguage.RU -> "Отмена"
        AppLanguage.EN -> "Cancel"
    }

    fun authNetwork(lang: AppLanguage): AuthNetworkStrings = when (lang) {
        AppLanguage.UZ -> AuthNetworkStrings(
            emptyResponse = "Bo‘sh javob",
            tokenMissing = "Token yo‘q",
            rateLimited = "Juda ko‘p so‘rov. Birozdan keyin qayta urinib ko‘ring.",
            requestFailed = "So‘rov muvaffaqiyatsiz",
            networkError = "Tarmoq xatosi",
            unknownError = "Noma’lum xato",
            serverErrorPrefix = "Server xatosi",
        )
        AppLanguage.RU -> AuthNetworkStrings(
            emptyResponse = "Пустой ответ",
            tokenMissing = "Токен отсутствует",
            rateLimited = "Слишком много запросов. Попробуйте позже.",
            requestFailed = "Запрос не выполнен",
            networkError = "Ошибка сети",
            unknownError = "Неизвестная ошибка",
            serverErrorPrefix = "Ошибка сервера",
        )
        AppLanguage.EN -> AuthNetworkStrings(
            emptyResponse = "Empty response",
            tokenMissing = "Token missing",
            rateLimited = "Too many requests. Please try again later.",
            requestFailed = "Request failed",
            networkError = "Network error",
            unknownError = "Unknown error",
            serverErrorPrefix = "Server error",
        )
    }

    // --- Login ---
    fun loginTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Kirish"
        AppLanguage.RU -> "Вход"
        AppLanguage.EN -> "Sign in"
    }

    fun loginSubtitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Admin paneliga kirish uchun ma'lumotlaringizni kiriting"
        AppLanguage.RU -> "Введите данные для входа в панель администратора"
        AppLanguage.EN -> "Enter your credentials to access the admin panel"
    }

    fun fieldUsername(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "FOYDALANUVCHI NOMI"
        AppLanguage.RU -> "ИМЯ ПОЛЬЗОВАТЕЛЯ"
        AppLanguage.EN -> "USERNAME"
    }

    fun fieldPassword(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "PAROL"
        AppLanguage.RU -> "ПАРОЛЬ"
        AppLanguage.EN -> "PASSWORD"
    }

    /** Ro‘yxatdan o‘tish formasi uchun oddiy «Parol». */
    fun registerFieldPassword(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Parol"
        AppLanguage.RU -> "Пароль"
        AppLanguage.EN -> "Password"
    }

    fun placeholderUsername(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Login yoki foydalanuvchi nomi"
        AppLanguage.RU -> "Логин или имя пользователя"
        AppLanguage.EN -> "Username or email"
    }

    fun placeholderLoginPassword(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Parolingizni kiriting"
        AppLanguage.RU -> "Введите пароль"
        AppLanguage.EN -> "Enter your password"
    }

    fun passwordStrengthLabel(lang: AppLanguage, strength: PasswordStrength): String =
        when (lang) {
            AppLanguage.UZ -> when (strength) {
                PasswordStrength.Empty -> "Parol kiriting — kamida 8 belgi tavsiya etiladi"
                PasswordStrength.Weak -> "Zaif parol — uzunlik va turli belgilar qo‘shing"
                PasswordStrength.Fair -> "O‘rtacha — katta harf va raqam qo‘shing"
                PasswordStrength.Good -> "Yaxshi parol"
                PasswordStrength.Strong -> "Kuchli parol"
            }
            AppLanguage.RU -> when (strength) {
                PasswordStrength.Empty -> "Введите пароль — рекомендуется от 8 символов"
                PasswordStrength.Weak -> "Слабый пароль — добавьте длину и разные символы"
                PasswordStrength.Fair -> "Средний — добавьте заглавные буквы и цифры"
                PasswordStrength.Good -> "Хороший пароль"
                PasswordStrength.Strong -> "Надёжный пароль"
            }
            AppLanguage.EN -> when (strength) {
                PasswordStrength.Empty -> "Enter a password — at least 8 characters recommended"
                PasswordStrength.Weak -> "Weak password — add length and a mix of characters"
                PasswordStrength.Fair -> "Fair — add uppercase letters and digits"
                PasswordStrength.Good -> "Good password"
                PasswordStrength.Strong -> "Strong password"
            }
        }

    fun rememberMe(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Meni eslab qol"
        AppLanguage.RU -> "Запомнить меня"
        AppLanguage.EN -> "Remember me"
    }

    fun forgotPasswordLink(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Parolni unutdingizmi?"
        AppLanguage.RU -> "Забыли пароль?"
        AppLanguage.EN -> "Forgot password?"
    }

    fun loginButton(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Kirish"
        AppLanguage.RU -> "Войти"
        AppLanguage.EN -> "Sign in"
    }

    fun loginLoading(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Kirilmoqda…"
        AppLanguage.RU -> "Вход…"
        AppLanguage.EN -> "Signing in…"
    }

    fun loginNoCredentials(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Foydalanuvchi nomi va parolni kiriting."
        AppLanguage.RU -> "Введите имя пользователя и пароль."
        AppLanguage.EN -> "Enter your username and password."
    }

    fun loginDialogSuccessTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Kirish muvaffaqiyatli"
        AppLanguage.RU -> "Вход выполнен"
        AppLanguage.EN -> "Signed in successfully"
    }

    fun loginDialogSuccessMessage(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Sessiya ochildi. Davom etish uchun OK tugmasini bosing."
        AppLanguage.RU -> "Сессия открыта. Нажмите OK, чтобы продолжить."
        AppLanguage.EN -> "A session has been opened. Press OK to continue."
    }

    fun loginDialogFailTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Kirish amalga oshmadi"
        AppLanguage.RU -> "Вход не выполнен"
        AppLanguage.EN -> "Sign-in failed"
    }

    fun loginNoAccessTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Kirish taqiqlangan"
        AppLanguage.RU -> "Доступ запрещён"
        AppLanguage.EN -> "Access denied"
    }

    fun loginNoAccessMessage(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Bu panel faqat SUPER_ADMIN va ADMIN rollariga ega foydalanuvchilar uchun. Sizning hisobingizda kerakli ruxsat yo'q."
        AppLanguage.RU -> "Эта панель только для пользователей с ролями SUPER_ADMIN или ADMIN. У вашего аккаунта нет необходимых прав."
        AppLanguage.EN -> "This panel is only available to users with SUPER_ADMIN and ADMIN roles."
    }

    fun noAccount(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Hisobingiz yo'qmi?"
        AppLanguage.RU -> "Нет аккаунта?"
        AppLanguage.EN -> "Don't have an account?"
    }

    fun goRegister(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Ro'yxatdan o'tish"
        AppLanguage.RU -> "Регистрация"
        AppLanguage.EN -> "Sign up"
    }

    // --- Google sign-in ---
    fun googleContinue(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Google bilan davom etish"
        AppLanguage.RU -> "Продолжить с Google"
        AppLanguage.EN -> "Continue with Google"
    }

    fun googleSigningIn(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Google orqali kirilmoqda…"
        AppLanguage.RU -> "Вход через Google…"
        AppLanguage.EN -> "Signing in with Google…"
    }

    fun googleErrorTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Google bilan kirish"
        AppLanguage.RU -> "Вход через Google"
        AppLanguage.EN -> "Google sign-in"
    }

    fun googlePortBusy(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "45123-port band. Boshqa ilovani yopib, qayta urinib ko'ring."
        AppLanguage.RU -> "Порт 45123 занят. Закройте другое приложение и попробуйте снова."
        AppLanguage.EN -> "Port 45123 is busy. Close another app and try again."
    }

    fun googleBrowserError(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Brauzer ochilmadi. Qayta urinib ko'ring."
        AppLanguage.RU -> "Не удалось открыть браузер. Попробуйте ещё раз."
        AppLanguage.EN -> "Browser could not be opened. Please try again."
    }

    fun googleTimeout(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "So'rov vaqti tugadi (3 daqiqa). Qayta urinib ko'ring."
        AppLanguage.RU -> "Время ожидания истекло (3 минуты). Попробуйте ещё раз."
        AppLanguage.EN -> "Request timed out (3 minutes). Please try again."
    }

    fun googleDenied(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Ruxsat bekor qilindi."
        AppLanguage.RU -> "Доступ отклонён."
        AppLanguage.EN -> "Permission denied."
    }

    fun googleUnexpected(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Noma'lum xato yuz berdi. Qayta urinib ko'ring."
        AppLanguage.RU -> "Произошла неизвестная ошибка. Попробуйте ещё раз."
        AppLanguage.EN -> "An unknown error occurred. Please try again."
    }

    fun copyright(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "© 2024 WAREHOUSE MANAGEMENT SYSTEM"
        AppLanguage.RU -> "© 2024 WAREHOUSE MANAGEMENT SYSTEM"
        AppLanguage.EN -> "© 2024 WAREHOUSE MANAGEMENT SYSTEM"
    }

    fun loginLeftBrand(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "WAREHOUSE ADMIN"
        AppLanguage.RU -> "WAREHOUSE ADMIN"
        AppLanguage.EN -> "WAREHOUSE ADMIN"
    }

    fun loginLeftHeadline(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Omborlaringizni aqlli boshqaring"
        AppLanguage.RU -> "Управляйте складами умно"
        AppLanguage.EN -> "Manage your warehouses smartly"
    }

    fun loginLeftBody(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Markazlashtirilgan tizim orqali inventarizatsiya, logistika va xodimlarni boshqarishni yangi bosqichga olib chiqing."
        AppLanguage.RU -> "Централизованная система: инвентаризация, логистика и персонал на новом уровне."
        AppLanguage.EN -> "Manage inventory, logistics and staff through one centralized system"
    }

    fun loginLeftQuote(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "\"Logistika - bu to'g'ri mahsulotni to'g'ri vaqtda to'g'ri joyga yetkazish san'atidir.\""
        AppLanguage.RU -> "\"Логистика — это искусство доставить нужный товар в нужное время и место.\""
        AppLanguage.EN -> "Logistics is the art of getting the right product to the right place at the right time"
    }

    // Forgot password dialog
    fun forgotDialogTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Parolni tiklash"
        AppLanguage.RU -> "Восстановление пароля"
        AppLanguage.EN -> "Password recovery"
    }

    fun forgotDialogHint(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Emailni kiriting, kodni yuboring, keyin kodni tekshirib yangi parolni o‘rnating."
        AppLanguage.RU -> "Введите email, отправьте код, затем подтвердите код и задайте новый пароль."
        AppLanguage.EN -> "Enter your email, send the code, then verify the code and set a new password"
    }

    fun forgotPlaceholder(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "email@example.com"
        AppLanguage.RU -> "email@example.com"
        AppLanguage.EN -> "email@example.com"
    }

    fun forgotSend(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Yuborish"
        AppLanguage.RU -> "Отправить"
        AppLanguage.EN -> "Send"
    }

    fun forgotSending(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Yuborilmoqda…"
        AppLanguage.RU -> "Отправка…"
        AppLanguage.EN -> "Sending…"
    }

    fun forgotEmailRequiredTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Email kerak"
        AppLanguage.RU -> "Нужен email"
        AppLanguage.EN -> "Email required"
    }

    fun forgotEmailRequiredMessage(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Parolni tiklash uchun email manzilini kiriting."
        AppLanguage.RU -> "Введите адрес email для восстановления пароля."
        AppLanguage.EN -> "Enter your email address to recover your password."
    }

    fun forgotSuccessTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "So‘rov yuborildi"
        AppLanguage.RU -> "Запрос отправлен"
        AppLanguage.EN -> "Request sent"
    }

    fun forgotSuccessMessage(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Agar bu email tizimda mavjud bo‘lsa, 6 xonali tiklash kodi yuboriladi."
        AppLanguage.RU -> "Если этот email есть в системе, будет отправлен 6-значный код сброса."
        AppLanguage.EN -> "If this email exists in the system, a 6-digit recovery code will be sent."
    }

    fun forgotFailTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Yuborib bo‘lmadi"
        AppLanguage.RU -> "Не удалось отправить"
        AppLanguage.EN -> "Could not send"
    }

    fun resetCodeLabel(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Tasdiqlash kodi"
        AppLanguage.RU -> "Код подтверждения"
        AppLanguage.EN -> "Confirmation code"
    }

    fun resetCodePlaceholder(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Emailga kelgan 6 xonali kod"
        AppLanguage.RU -> "6-значный код из email"
        AppLanguage.EN -> "6-digit code sent to your email"
    }

    fun resetNewPasswordLabel(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Yangi parol"
        AppLanguage.RU -> "Новый пароль"
        AppLanguage.EN -> "New password"
    }

    fun resetNewPasswordPlaceholder(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Yangi parol (kamida 8 belgi)"
        AppLanguage.RU -> "Новый пароль (не менее 8 символов)"
        AppLanguage.EN -> "New password (at least 8 characters)"
    }

    fun resetSendCode(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Kod yuborish"
        AppLanguage.RU -> "Отправить код"
        AppLanguage.EN -> "Send code"
    }

    fun resetVerifyCode(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Kodni tekshirish"
        AppLanguage.RU -> "Проверить код"
        AppLanguage.EN -> "Verify code"
    }

    fun resetApplyPassword(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Parolni yangilash"
        AppLanguage.RU -> "Обновить пароль"
        AppLanguage.EN -> "Update password"
    }

    fun resetCodeRequired(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Tasdiqlash kodini kiriting."
        AppLanguage.RU -> "Введите код подтверждения."
        AppLanguage.EN -> "Enter the confirmation code."
    }

    fun resetPasswordRequired(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Yangi parol kamida 8 belgidan iborat bo‘lsin."
        AppLanguage.RU -> "Новый пароль должен быть не короче 8 символов."
        AppLanguage.EN -> "New password must be at least 8 characters."
    }

    fun resetCodeVerifiedTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Kod tasdiqlandi"
        AppLanguage.RU -> "Код подтверждён"
        AppLanguage.EN -> "Code verified"
    }

    fun resetCodeVerifiedMessage(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Endi yangi parolni kiriting va yangilang."
        AppLanguage.RU -> "Теперь введите новый пароль и обновите его."
        AppLanguage.EN -> "Now enter your new password and update it."
    }

    fun resetPasswordUpdatedTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Parol yangilandi"
        AppLanguage.RU -> "Пароль обновлён"
        AppLanguage.EN -> "Password updated"
    }

    fun resetPasswordUpdatedMessage(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Parolingiz muvaffaqiyatli yangilandi."
        AppLanguage.RU -> "Ваш пароль успешно обновлён."
        AppLanguage.EN -> "Your password has been updated successfully."
    }

    // --- Register ---
    fun registerTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Ro'yxatdan o'tish"
        AppLanguage.RU -> "Регистрация"
        AppLanguage.EN -> "Sign up"
    }

    fun registerSubtitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Tizimdan foydalanish uchun ma'lumotlaringizni kiriting"
        AppLanguage.RU -> "Введите данные для использования системы"
        AppLanguage.EN -> "Enter your details to start using the system"
    }

    fun registerCompactBrand(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Warehouse Admin"
        AppLanguage.RU -> "Warehouse Admin"
        AppLanguage.EN -> "Warehouse Admin"
    }

    fun fieldDisplayUsername(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Foydalanuvchi nomi"
        AppLanguage.RU -> "Имя пользователя"
        AppLanguage.EN -> "Username"
    }

    fun placeholderUsernameExample(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Masalan: warehouse_admin"
        AppLanguage.RU -> "Например: warehouse_admin"
        AppLanguage.EN -> "e.g. warehouse_admin"
    }

    fun fieldEmail(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Email manzili"
        AppLanguage.RU -> "Email"
        AppLanguage.EN -> "Email address"
    }

    fun placeholderEmail(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "ism@kompaniya.uz"
        AppLanguage.RU -> "имя@компания.uz"
        AppLanguage.EN -> "name@company.uz"
    }

    fun placeholderRegisterPassword(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Min. 8 belgi: katta/kichik, raqam, belgi"
        AppLanguage.RU -> "Мин. 8 симв.: буквы, цифры, знак"
        AppLanguage.EN -> "Min. 8 chars: upper/lower, digit, symbol"
    }

    fun placeholderRegisterConfirm(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Parolni qayta kiriting"
        AppLanguage.RU -> "Повторите пароль"
        AppLanguage.EN -> "Re-enter your password"
    }

    fun fieldConfirmPassword(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Parolni tasdiqlang"
        AppLanguage.RU -> "Подтвердите пароль"
        AppLanguage.EN -> "Confirm password"
    }

    fun termsAgreement(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Men Foydalanish shartlari va Maxfiylik siyosati bilan roziman"
        AppLanguage.RU -> "Я согласен с Условиями использования и Политикой конфиденциальности"
        AppLanguage.EN -> "I agree to the Terms of Use and Privacy Policy"
    }

    fun registerButton(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Ro'yxatdan o'tish"
        AppLanguage.RU -> "Зарегистрироваться"
        AppLanguage.EN -> "Sign up"
    }

    fun registerSending(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Yuborilmoqda…"
        AppLanguage.RU -> "Отправка…"
        AppLanguage.EN -> "Sending…"
    }

    fun registerValidationFields(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Barcha majburiy maydonlarni to‘ldiring."
        AppLanguage.RU -> "Заполните все обязательные поля."
        AppLanguage.EN -> "Fill in all required fields."
    }

    fun registerValidationWeakPassword(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Parol kamida 8 belgidan iborat bo‘lsin."
        AppLanguage.RU -> "Пароль должен быть не короче 8 символов."
        AppLanguage.EN -> "Password must be at least 8 characters."
    }

    fun registerValidationMismatch(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Parol va tasdiqlash bir xil bo‘lishi kerak."
        AppLanguage.RU -> "Пароль и подтверждение должны совпадать."
        AppLanguage.EN -> "Passwords must match."
    }

    fun registerValidationTerms(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Foydalanish shartlarini qabul qiling."
        AppLanguage.RU -> "Примите условия использования."
        AppLanguage.EN -> "Accept the terms of use."
    }

    fun registerFailTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Ro‘yxatdan o‘tish muvaffaqiyatsiz"
        AppLanguage.RU -> "Регистрация не удалась"
        AppLanguage.EN -> "Sign-up failed"
    }

    fun registerSuccessTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Kod yuborildi"
        AppLanguage.RU -> "Код отправлен"
        AppLanguage.EN -> "Code sent"
    }

    fun registerSuccessMessage(lang: AppLanguage, email: String) = when (lang) {
        AppLanguage.UZ -> "Tasdiqlash kodi $email manziliga yuborildi. Keyingi qadamda kodni kiriting."
        AppLanguage.RU -> "Код подтверждения отправлен на $email. На следующем шаге введите код."
        AppLanguage.EN -> "A confirmation code was sent to $email. Enter it in the next step."
    }

    fun registerValidationTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Ma'lumot yetishmayapti"
        AppLanguage.RU -> "Недостаточно данных"
        AppLanguage.EN -> "Missing information"
    }

    fun registerWeakPasswordTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Zaif parol"
        AppLanguage.RU -> "Слабый пароль"
        AppLanguage.EN -> "Weak password"
    }

    fun registerMismatchTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Parollar mos emas"
        AppLanguage.RU -> "Пароли не совпадают"
        AppLanguage.EN -> "Passwords don't match"
    }

    fun registerTermsTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Shartlar"
        AppLanguage.RU -> "Условия"
        AppLanguage.EN -> "Terms"
    }

    fun hasAccount(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Hisobingiz bormi?"
        AppLanguage.RU -> "Уже есть аккаунт?"
        AppLanguage.EN -> "Already have an account?"
    }

    fun goLogin(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Tizimga kirish"
        AppLanguage.RU -> "Войти"
        AppLanguage.EN -> "Sign in"
    }

    fun registerLeftBrand(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "WAREHOUSE ADMIN"
        AppLanguage.RU -> "WAREHOUSE ADMIN"
        AppLanguage.EN -> "WAREHOUSE ADMIN"
    }

    fun registerLeftHeadline(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Logistikani aqlli boshqaruv tizimi bilan optimallashtiring"
        AppLanguage.RU -> "Оптимизируйте логистику с умной системой управления"
        AppLanguage.EN -> "Optimize logistics with a smart management system"
    }

    fun registerLeftBody(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Ombor zaxiralari, buyurtmalar va yetkazib berish zanjirini yagona platformada real vaqt rejimida kuzatib boring."
        AppLanguage.RU -> "Следите за запасами, заказами и цепочкой поставок на одной платформе в реальном времени."
        AppLanguage.EN -> "Run warehouse stock, orders and the delivery chain on a single platform"
    }

    fun registerLeftFooter(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "© 2024 Warehouse Admin Panel. Barcha huquqlar himoyalangan."
        AppLanguage.RU -> "© 2024 Warehouse Admin Panel. Все права защищены."
        AppLanguage.EN -> "© 2024 Warehouse Admin Panel. All rights reserved."
    }

    // --- Email OTP ---
    fun verifyTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Emailingizni tasdiqlang"
        AppLanguage.RU -> "Подтвердите email"
        AppLanguage.EN -> "Verify your email"
    }

    fun verifyIntro(lang: AppLanguage, email: String) = when (lang) {
        AppLanguage.UZ -> "Biz sizning $email elektron pochtangizga 6 xonali tasdiqlash kodini yubordik."
        AppLanguage.RU -> "Мы отправили 6-значный код подтверждения на $email."
        AppLanguage.EN -> "We sent a 6-digit verification code to $email."
    }

    fun verifyResendQuestion(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Kodni olmadingizmi?"
        AppLanguage.RU -> "Не получили код?"
        AppLanguage.EN -> "Didn't receive the code?"
    }

    fun verifyResendIn(lang: AppLanguage, seconds: Int) = when (lang) {
        AppLanguage.UZ -> "Qayta yuborish (${seconds}s)"
        AppLanguage.RU -> "Повторная отправка (${seconds} с)"
        AppLanguage.EN -> "Resend in (${seconds}s)"
    }

    fun verifyResend(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Qayta yuborish"
        AppLanguage.RU -> "Отправить снова"
        AppLanguage.EN -> "Resend"
    }

    fun verifyResending(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Yuborilmoqda…"
        AppLanguage.RU -> "Отправка…"
        AppLanguage.EN -> "Sending…"
    }

    fun verifySubmit(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Tasdiqlash"
        AppLanguage.RU -> "Подтвердить"
        AppLanguage.EN -> "Verify"
    }

    fun verifySubmitting(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Tekshirilmoqda…"
        AppLanguage.RU -> "Проверка…"
        AppLanguage.EN -> "Checking…"
    }

    fun verifyBackLogin(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Kirish sahifasiga qaytish"
        AppLanguage.RU -> "Вернуться ко входу"
        AppLanguage.EN -> "Back to sign-in page"
    }

    fun verifyHelp(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "YORDAM"
        AppLanguage.RU -> "ПОМОЩЬ"
        AppLanguage.EN -> "HELP"
    }

    fun verifyCodeFormatTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Kod noto‘g‘ri formatda"
        AppLanguage.RU -> "Неверный формат кода"
        AppLanguage.EN -> "Invalid code format"
    }

    fun verifyCodeFormatMessage(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "6 raqamli tasdiqlash kodini to‘liq kiriting."
        AppLanguage.RU -> "Введите полный 6-значный код."
        AppLanguage.EN -> "Enter the full 6-digit verification code."
    }

    fun verifyFailTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Tasdiqlash muvaffaqiyatsiz"
        AppLanguage.RU -> "Подтверждение не удалось"
        AppLanguage.EN -> "Verification failed"
    }

    fun verifyLoginFailTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Kirishda xato"
        AppLanguage.RU -> "Ошибка входа"
        AppLanguage.EN -> "Sign-in error"
    }

    fun verifyLoginFailMessage(lang: AppLanguage, detail: String) = when (lang) {
        AppLanguage.UZ -> "Email tasdiqlandi, lekin avtomatik kirish amalga oshmadi: $detail"
        AppLanguage.RU -> "Email подтверждён, но автоматический вход не выполнен: $detail"
        AppLanguage.EN -> "Email verified, but automatic sign-in failed: $detail"
    }

    fun verifySuccessTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Hisob tayyor"
        AppLanguage.RU -> "Аккаунт готов"
        AppLanguage.EN -> "Account ready"
    }

    fun verifySuccessMessage(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Email tasdiqlandi va tizimga kirdingiz."
        AppLanguage.RU -> "Email подтверждён, вы вошли в систему."
        AppLanguage.EN -> "Email verified and you are signed in."
    }

    fun verifyResendOkTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Kod yuborildi"
        AppLanguage.RU -> "Код отправлен"
        AppLanguage.EN -> "Code sent"
    }

    fun verifyResendOkMessage(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Yangi tasdiqlash kodi email manzilingizga yuborildi."
        AppLanguage.RU -> "Новый код подтверждения отправлен на ваш email."
        AppLanguage.EN -> "A new verification code has been sent to your email."
    }

    fun verifyResendFailTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Qayta yuborib bo‘lmadi"
        AppLanguage.RU -> "Не удалось отправить повторно"
        AppLanguage.EN -> "Could not resend"
    }

    fun verifyLeftBrand(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "WAREHOUSE ADMIN"
        AppLanguage.RU -> "WAREHOUSE ADMIN"
        AppLanguage.EN -> "WAREHOUSE ADMIN"
    }

    fun verifyLeftHeadline(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Logistika tizimini boshqarishning eng samarali usuli."
        AppLanguage.RU -> "Самый эффективный способ управления логистикой."
        AppLanguage.EN -> "The most efficient way to manage your logistics system."
    }

    fun verifyLeftBody(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Bizning platformamiz orqali real vaqt rejimida ombor zahiralari va kuryerlar harakatini kuzatib boring."
        AppLanguage.RU -> "Следите за остатками на складе и курьерами в реальном времени."
        AppLanguage.EN -> "Monitor warehouse stock and couriers in real time through our platform"
    }

    fun verifyLeftFooter(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "© 2024 Warehouse Admin Panel. Barcha huquqlar himoyalangan."
        AppLanguage.RU -> "© 2024 Warehouse Admin Panel. Все права защищены."
        AppLanguage.EN -> "© 2024 Warehouse Admin Panel. All rights reserved."
    }

    fun sidebarSubtitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Super Admin Panel"
        AppLanguage.RU -> "Панель супер-админа"
        AppLanguage.EN -> "Super Admin Panel"
    }

    fun logout(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Chiqish"
        AppLanguage.RU -> "Выход"
        AppLanguage.EN -> "Log out"
    }

    fun logoutDialogTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Chiqishni tasdiqlang"
        AppLanguage.RU -> "Подтвердите выход"
        AppLanguage.EN -> "Confirm logout"
    }

    fun logoutDialogMessage(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Haqiqatan ham tizimdan chiqmoqchimisiz? Sessiyangiz tugatiladi."
        AppLanguage.RU -> "Вы действительно хотите выйти? Ваша сессия будет завершена."
        AppLanguage.EN -> "Are you sure you want to log out? Your session will end."
    }

    fun logoutConfirm(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Ha, chiqish"
        AppLanguage.RU -> "Да, выйти"
        AppLanguage.EN -> "Yes, log out"
    }

    fun searchPlaceholder(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Tizim bo'ylab qidiruv..."
        AppLanguage.RU -> "Поиск по системе..."
        AppLanguage.EN -> "Search across the system..."
    }

    fun userDisplayName(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Super Admin"
        AppLanguage.RU -> "Супер-админ"
        AppLanguage.EN -> "Super Admin"
    }

    fun userRole(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Asosiy boshqaruvchi"
        AppLanguage.RU -> "Главный администратор"
        AppLanguage.EN -> "Main administrator"
    }

    fun dashboardPanelTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Boshqaruv Paneli"
        AppLanguage.RU -> "Панель управления"
        AppLanguage.EN -> "Control Panel"
    }

    fun dashboardWelcome(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Xush kelibsiz! Bugungi ombor holati va faoliyat ko'rsatkichlari."
        AppLanguage.RU -> "Добро пожаловать! Состояние склада и показатели активности за сегодня."
        AppLanguage.EN -> "Welcome! Today's warehouse status and activity metrics."
    }

    fun last30Days(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Oxirgi 30 kun"
        AppLanguage.RU -> "Последние 30 дней"
        AppLanguage.EN -> "Last 30 days"
    }

    fun downloadReport(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Hisobot yuklash"
        AppLanguage.RU -> "Скачать отчёт"
        AppLanguage.EN -> "Download report"
    }

    fun statTodayOrders(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Bugungi buyurtmalar"
        AppLanguage.RU -> "Заказы сегодня"
        AppLanguage.EN -> "Today's orders"
    }

    fun statTodayRevenue(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Bugungi daromad"
        AppLanguage.RU -> "Выручка сегодня"
        AppLanguage.EN -> "Today's revenue"
    }

    fun statPendingOrders(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Kutilayotgan buyurtmalar"
        AppLanguage.RU -> "Ожидающие заказы"
        AppLanguage.EN -> "Pending orders"
    }

    fun statLowStock(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Kam qoldiq ogohlantirishlari"
        AppLanguage.RU -> "Предупреждения о запасах"
        AppLanguage.EN -> "Low stock alerts"
    }

    fun badgeAttention(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "E'tibor talab"
        AppLanguage.RU -> "Требует внимания"
        AppLanguage.EN -> "Needs attention"
    }

    fun badgeUrgent(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Tezda bajarilishi kerak"
        AppLanguage.RU -> "Срочно обработать"
        AppLanguage.EN -> "Urgent"
    }

    fun badgeCritical(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Kritik"
        AppLanguage.RU -> "Критично"
        AppLanguage.EN -> "Critical"
    }

    fun badgeDepleting(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Zaxira tugayapti"
        AppLanguage.RU -> "Запас заканчивается"
        AppLanguage.EN -> "Running out"
    }

    fun chartRevenueTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Oylik daromad grafigi"
        AppLanguage.RU -> "График выручки по месяцам"
        AppLanguage.EN -> "Monthly revenue chart"
    }

    fun chartRevenueSeries(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Daromad (UZS)"
        AppLanguage.RU -> "Выручка (UZS)"
        AppLanguage.EN -> "Revenue (UZS)"
    }

    fun chartWeekLabels(lang: AppLanguage): List<String> = when (lang) {
        AppLanguage.UZ -> listOf("01 Okt", "07 Okt", "14 Okt", "21 Okt", "28 Okt", "Bugun")
        AppLanguage.RU -> listOf("01 окт", "07 окт", "14 окт", "21 окт", "28 окт", "Сегодня")
        AppLanguage.EN -> listOf("Oct 01", "Oct 07", "Oct 14", "Oct 21", "Oct 28", "Today")
    }

    fun ordersStatusTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Buyurtmalar holati"
        AppLanguage.RU -> "Статус заказов"
        AppLanguage.EN -> "Order status"
    }

    fun totalLabel(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Jami"
        AppLanguage.RU -> "Всего"
        AppLanguage.EN -> "Total"
    }

    fun donutPending(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "KUTILMOQDA"
        AppLanguage.RU -> "ОЖИДАНИЕ"
        AppLanguage.EN -> "PENDING"
    }

    fun donutDelivered(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "YETKAZILDI"
        AppLanguage.RU -> "ДОСТАВЛЕНО"
        AppLanguage.EN -> "DELIVERED"
    }

    fun donutProcessing(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "JARAYONDA"
        AppLanguage.RU -> "В ОБРАБОТКЕ"
        AppLanguage.EN -> "PROCESSING"
    }

    fun donutCancelled(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "BEKOR QILINGAN"
        AppLanguage.RU -> "ОТМЕНЁН"
        AppLanguage.EN -> "CANCELLED"
    }

    fun topProducts(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Top mahsulotlar"
        AppLanguage.RU -> "Топ товаров"
        AppLanguage.EN -> "Top products"
    }

    fun seeAll(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Hammasini ko'rish"
        AppLanguage.RU -> "Смотреть все"
        AppLanguage.EN -> "See all"
    }

    fun recentOrders(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Oxirgi buyurtmalar"
        AppLanguage.RU -> "Последние заказы"
        AppLanguage.EN -> "Recent orders"
    }

    fun allOrders(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Barchasi"
        AppLanguage.RU -> "Все"
        AppLanguage.EN -> "All"
    }

    fun tableOrderNo(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Buyurtma №"
        AppLanguage.RU -> "Заказ №"
        AppLanguage.EN -> "Order #"
    }

    fun tableAmount(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Summa"
        AppLanguage.RU -> "Сумма"
        AppLanguage.EN -> "Amount"
    }

    fun tableStatus(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Holat"
        AppLanguage.RU -> "Статус"
        AppLanguage.EN -> "Status"
    }

    fun tableProductName(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Mahsulot nomi"
        AppLanguage.RU -> "Товар"
        AppLanguage.EN -> "Product name"
    }

    fun tableSold(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Sotilgan"
        AppLanguage.RU -> "Продано"
        AppLanguage.EN -> "Sold"
    }

    fun tableRevenue(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Daromad"
        AppLanguage.RU -> "Выручка"
        AppLanguage.EN -> "Revenue"
    }

    fun orderStatusDelivered(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Yetkazildi"
        AppLanguage.RU -> "Доставлен"
        AppLanguage.EN -> "Delivered"
    }

    fun orderStatusProcessing(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Jarayonda"
        AppLanguage.RU -> "В обработке"
        AppLanguage.EN -> "Processing"
    }

    fun orderStatusPending(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Kutilmoqda"
        AppLanguage.RU -> "Ожидает"
        AppLanguage.EN -> "Pending"
    }

    fun orderStatusCancelled(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Bekor qilindi"
        AppLanguage.RU -> "Отменён"
        AppLanguage.EN -> "Cancelled"
    }

    fun settingsConnectionTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Ulanish sozlamalari"
        AppLanguage.RU -> "Настройки подключения"
        AppLanguage.EN -> "Connection settings"
    }

    fun settingsGatewayLabel(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Gateway URL"
        AppLanguage.RU -> "Gateway URL"
        AppLanguage.EN -> "Gateway URL"
    }

    fun settingsSave(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Saqlash"
        AppLanguage.RU -> "Сохранить"
        AppLanguage.EN -> "Save"
    }

    fun settingsReset(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Standartga qaytarish"
        AppLanguage.RU -> "Сбросить"
        AppLanguage.EN -> "Reset to default"
    }

    fun settingsSavedTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Saqlandi"
        AppLanguage.RU -> "Сохранено"
        AppLanguage.EN -> "Saved"
    }

    fun settingsSavedMessage(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Gateway URL muvaffaqiyatli saqlandi."
        AppLanguage.RU -> "Gateway URL успешно сохранён."
        AppLanguage.EN -> "Gateway URL saved successfully."
    }

    fun settingsUrlError(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "URL to'g'ri formatda bo'lishi kerak (http:// yoki https://)"
        AppLanguage.RU -> "URL должен начинаться с http:// или https://"
        AppLanguage.EN -> "URL must be valid (http:// or https://)"
    }

    fun settingsLanguageTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Interfeys tili"
        AppLanguage.RU -> "Язык интерфейса"
        AppLanguage.EN -> "Interface language"
    }

    fun settingsSessionTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Joriy sessiya"
        AppLanguage.RU -> "Текущая сессия"
        AppLanguage.EN -> "Current session"
    }

    fun settingsSessionActive(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Sessiya faol"
        AppLanguage.RU -> "Сессия активна"
        AppLanguage.EN -> "Session active"
    }

    fun settingsSessionNone(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Sessiya yo'q"
        AppLanguage.RU -> "Сессия отсутствует"
        AppLanguage.EN -> "No session"
    }

    fun settingsLogout(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Chiqish (Logout)"
        AppLanguage.RU -> "Выход (Logout)"
        AppLanguage.EN -> "Log out"
    }

    fun settingsAboutTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Ilova haqida"
        AppLanguage.RU -> "О приложении"
        AppLanguage.EN -> "About"
    }

    fun settingsGatewayPlaceholder(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "http://localhost:8080"
        AppLanguage.RU -> "http://localhost:8080"
        AppLanguage.EN -> "http://localhost:8080"
    }

    fun settingsLanguageUz(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "O'zbekcha"
        AppLanguage.RU -> "Узбекский"
        AppLanguage.EN -> "O'zbekcha"
    }

    fun settingsLanguageRu(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Русский"
        AppLanguage.RU -> "Русский"
        AppLanguage.EN -> "Русский"
    }

    fun settingsSessionActiveWithToken(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Sessiya faol - token mavjud"
        AppLanguage.RU -> "Сессия активна - токен присутствует"
        AppLanguage.EN -> "Session active - token present"
    }

    fun settingsTokenLabel(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Token"
        AppLanguage.RU -> "Токен"
        AppLanguage.EN -> "Token"
    }

    fun settingsVersion(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Versiya: 1.0.0"
        AppLanguage.RU -> "Версия: 1.0.0"
        AppLanguage.EN -> "Version: 1.0.0"
    }

    fun settingsProductName(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "mexa.club Warehouse Admin"
        AppLanguage.RU -> "mexa.club Warehouse Admin"
        AppLanguage.EN -> "mexa.club Warehouse Admin"
    }

    fun settingsCopyright(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "© 2026 mexa.club"
        AppLanguage.RU -> "© 2026 mexa.club"
        AppLanguage.EN -> "© 2026 mexa.club"
    }

    /** Demo mahsulot: "donа" / "шт" */
    fun unitsPiece(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "dona"
        AppLanguage.RU -> "шт"
        AppLanguage.EN -> "pcs"
    }

    // ─── Users bo'limi ─────────────────────────────────────────────────────────

    fun usersScreenTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Foydalanuvchilar"
        AppLanguage.RU -> "Пользователи"
        AppLanguage.EN -> "Users"
    }

    fun usersScreenSubtitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Tizim foydalanuvchilari va rollarini boshqarish"
        AppLanguage.RU -> "Управление пользователями и ролями системы"
        AppLanguage.EN -> "Manage system users and their roles"
    }

    fun usersSearchPlaceholder(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Username yoki email..."
        AppLanguage.RU -> "Username или email..."
        AppLanguage.EN -> "Username or email..."
    }

    fun usersFilterRole(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Rol"
        AppLanguage.RU -> "Роль"
        AppLanguage.EN -> "Role"
    }

    fun usersFilterStatus(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Holat"
        AppLanguage.RU -> "Статус"
        AppLanguage.EN -> "Status"
    }

    fun usersFilterAllRoles(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Barcha rollar"
        AppLanguage.RU -> "Все роли"
        AppLanguage.EN -> "All roles"
    }

    fun usersFilterAllStatus(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Barcha holat"
        AppLanguage.RU -> "Все статусы"
        AppLanguage.EN -> "All statuses"
    }

    fun usersColUser(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "FOYDALANUVCHI"
        AppLanguage.RU -> "ПОЛЬЗОВАТЕЛЬ"
        AppLanguage.EN -> "USER"
    }

    fun usersColRole(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "ROL VA BO'LIM"
        AppLanguage.RU -> "РОЛЬ И ОТДЕЛ"
        AppLanguage.EN -> "ROLE & DEPARTMENT"
    }

    fun usersColStatus(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "HOLATI"
        AppLanguage.RU -> "СТАТУС"
        AppLanguage.EN -> "STATUS"
    }

    fun usersColVerified(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "TASDIQLANGAN"
        AppLanguage.RU -> "ПОДТВЕРЖДЁН"
        AppLanguage.EN -> "VERIFIED"
    }

    fun usersColLastActivity(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "SO'NGGI FAOLLIK"
        AppLanguage.RU -> "ПОСЛЕДНЯЯ АКТИВНОСТЬ"
        AppLanguage.EN -> "LAST ACTIVITY"
    }

    fun verifiedYes(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Tasdiqlangan"
        AppLanguage.RU -> "Подтверждён"
        AppLanguage.EN -> "Verified"
    }

    fun verifiedPending(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Kutilmoqda"
        AppLanguage.RU -> "Ожидает"
        AppLanguage.EN -> "Pending"
    }

    fun usersEmptyTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Foydalanuvchilar topilmadi"
        AppLanguage.RU -> "Пользователи не найдены"
        AppLanguage.EN -> "No users found"
    }

    fun usersEmptyBody(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Qidiruv shartlariga mos keladigan foydalanuvchi mavjud emas yoki hali qo'shilmagan."
        AppLanguage.RU -> "Пользователи, соответствующие запросу, не найдены или ещё не добавлены."
        AppLanguage.EN -> "No user matches the current search criteria, or none have been added yet"
    }

    fun usersClearFilters(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Filtrni tozalash"
        AppLanguage.RU -> "Сбросить фильтры"
        AppLanguage.EN -> "Clear filters"
    }

    fun usersShowing(lang: AppLanguage, from: Int, to: Int, total: Int) = when (lang) {
        AppLanguage.UZ -> "$from dan $to gacha ko'rsatilmoqda (jami $total)"
        AppLanguage.RU -> "Показано с $from по $to (всего $total)"
        AppLanguage.EN -> "Showing $from to $to (total $total)"
    }

    fun usersPageSize(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Sahifa hajmi"
        AppLanguage.RU -> "Размер страницы"
        AppLanguage.EN -> "Page size"
    }

    fun usersStatTotal(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Jami foydalanuvchilar"
        AppLanguage.RU -> "Всего пользователей"
        AppLanguage.EN -> "Total users"
    }

    fun usersStatTotalSub(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Tizimda ro'yxatdan o'tgan"
        AppLanguage.RU -> "Зарегистрировано в системе"
        AppLanguage.EN -> "Registered in the system"
    }

    fun usersStatAdmins(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Administratorlar"
        AppLanguage.RU -> "Администраторы"
        AppLanguage.EN -> "Administrators"
    }

    fun usersStatAdminsSub(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Admin va super admin"
        AppLanguage.RU -> "Админ и супер админ"
        AppLanguage.EN -> "Admin and super admin"
    }

    fun usersStatWarehouse(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Ombor mudirlari"
        AppLanguage.RU -> "Заведующие складами"
        AppLanguage.EN -> "Warehouse managers"
    }

    fun usersStatWarehouseSub(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Warehouse roli"
        AppLanguage.RU -> "Роль warehouse"
        AppLanguage.EN -> "Warehouse role"
    }

    fun usersStatSessions(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Faol sessiyalar"
        AppLanguage.RU -> "Активные сессии"
        AppLanguage.EN -> "Active sessions"
    }

    fun usersStatSessionsSub(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Hozir tizimda faol"
        AppLanguage.RU -> "Сейчас в системе"
        AppLanguage.EN -> "Currently active in the system"
    }

    fun blockDialogTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Foydalanuvchini bloklash"
        AppLanguage.RU -> "Заблокировать пользователя"
        AppLanguage.EN -> "Block user"
    }

    fun blockDialogConfirm(lang: AppLanguage, name: String) = when (lang) {
        AppLanguage.UZ -> "Haqiqatan ham $name ni bloklamoqchimisiz? Bloklangandan so'ng foydalanuvchi tizimga kira olmaydi."
        AppLanguage.RU -> "Вы действительно хотите заблокировать $name? После блокировки пользователь не сможет войти в систему."
        AppLanguage.EN -> "Do you really want to block $name? After blocking, the user will not be able to sign in."
    }

    fun blockDialogReasonLabel(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Bloklash sababi"
        AppLanguage.RU -> "Причина блокировки"
        AppLanguage.EN -> "Block reason"
    }

    fun blockDialogReasonPlaceholder(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Bloklash sababini yozing..."
        AppLanguage.RU -> "Введите причину блокировки..."
        AppLanguage.EN -> "Write the block reason..."
    }

    fun blockDialogReasonHint(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Ushbu sabab foydalanuvchi profilida va audit logda saqlanadi."
        AppLanguage.RU -> "Эта причина сохранится в профиле пользователя и в журнале аудита."
        AppLanguage.EN -> "This reason is stored in the user profile and the audit log."
    }

    fun blockDialogSubmit(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Bloklash"
        AppLanguage.RU -> "Заблокировать"
        AppLanguage.EN -> "Block"
    }

    fun areYouSurePostfix(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Haqiqatan ham"
        AppLanguage.RU -> "Вы действительно хотите"
        AppLanguage.EN -> "Are you sure you want to"
    }

    fun usersBlockedMessage(lang: AppLanguage, name: String) = when (lang) {
        AppLanguage.UZ -> "$name bloklandi"
        AppLanguage.RU -> "$name заблокирован"
        AppLanguage.EN -> "$name was blocked"
    }

    fun usersUnblockedMessage(lang: AppLanguage, name: String) = when (lang) {
        AppLanguage.UZ -> "$name blokdan chiqarildi"
        AppLanguage.RU -> "$name разблокирован"
        AppLanguage.EN -> "$name was unblocked"
    }

    fun usersRoleUpdatedMessage(lang: AppLanguage, name: String) = when (lang) {
        AppLanguage.UZ -> "${name} roli yangilandi"
        AppLanguage.RU -> "Роль $name обновлена"
        AppLanguage.EN -> "Role of ${name} updated"
    }

    fun usersCreatedMessage(lang: AppLanguage, name: String) = when (lang) {
        AppLanguage.UZ -> "$name yaratildi"
        AppLanguage.RU -> "$name создан"
        AppLanguage.EN -> "$name created"
    }

    fun usersAddButton(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Foydalanuvchi qo'shish"
        AppLanguage.RU -> "Добавить пользователя"
        AppLanguage.EN -> "Add user"
    }

    fun usersErrorGeneric(lang: AppLanguage, detail: String) = when (lang) {
        AppLanguage.UZ -> "Xatolik: $detail"
        AppLanguage.RU -> "Ошибка: $detail"
        AppLanguage.EN -> "Error: $detail"
    }

    fun unblockDialogTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Foydalanuvchini blokdan chiqarish"
        AppLanguage.RU -> "Разблокировать пользователя"
        AppLanguage.EN -> "Unblock user"
    }

    fun unblockDialogConfirm(lang: AppLanguage, name: String) = when (lang) {
        AppLanguage.UZ -> "$name ni blokdan chiqarmoqchimisiz? Foydalanuvchi tizimga qayta kira oladi."
        AppLanguage.RU -> "Разблокировать $name? Пользователь снова сможет войти в систему."
        AppLanguage.EN -> "Do you want to unblock $name? The user will be able to sign in again."
    }

    fun unblockDialogSubmit(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Blokdan chiqarish"
        AppLanguage.RU -> "Разблокировать"
        AppLanguage.EN -> "Unblock"
    }

    fun selfBlockWarning(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "O'z akkauntingizni bloklay olmaysiz."
        AppLanguage.RU -> "Вы не можете заблокировать свой аккаунт."
        AppLanguage.EN -> "You cannot block your own account."
    }

    fun superAdminProtectedWarning(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Super adminlar bloklanishi yoki o'chirilishi mumkin emas."
        AppLanguage.RU -> "Супер-админов нельзя блокировать или удалять."
        AppLanguage.EN -> "Super admins cannot be blocked or deleted."
    }

    fun usersDetailTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Foydalanuvchi ma'lumoti"
        AppLanguage.RU -> "Данные пользователя"
        AppLanguage.EN -> "User details"
    }

    fun usersDetailEmail(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Email"
        AppLanguage.RU -> "Email"
        AppLanguage.EN -> "Email"
    }

    fun usersDetailId(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "ID"
        AppLanguage.RU -> "ID"
        AppLanguage.EN -> "ID"
    }

    fun usersDetailRoles(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Rollar"
        AppLanguage.RU -> "Роли"
        AppLanguage.EN -> "Roles"
    }

    fun usersDetailStatus(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Holat"
        AppLanguage.RU -> "Статус"
        AppLanguage.EN -> "Status"
    }

    fun usersDetailVerified(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Tasdiqlangan"
        AppLanguage.RU -> "Подтверждён"
        AppLanguage.EN -> "Verified"
    }

    fun usersDetailProvider(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Manba"
        AppLanguage.RU -> "Источник"
        AppLanguage.EN -> "Provider"
    }

    fun usersDetailCreatedAt(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Ro'yxatdan o'tgan"
        AppLanguage.RU -> "Зарегистрирован"
        AppLanguage.EN -> "Registered"
    }

    fun usersDetailLastActive(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "So'nggi faollik"
        AppLanguage.RU -> "Последняя активность"
        AppLanguage.EN -> "Last active"
    }

    fun usersDetailBlockReason(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Bloklash sababi"
        AppLanguage.RU -> "Причина блокировки"
        AppLanguage.EN -> "Block reason"
    }

    fun usersDetailBlockedAt(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Bloklangan vaqti"
        AppLanguage.RU -> "Время блокировки"
        AppLanguage.EN -> "Blocked at"
    }

    fun usersDetailClose(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Yopish"
        AppLanguage.RU -> "Закрыть"
        AppLanguage.EN -> "Close"
    }

    fun usersNotAvailable(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Mavjud emas"
        AppLanguage.RU -> "Нет данных"
        AppLanguage.EN -> "Not available"
    }

    fun editRoleDialogTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Foydalanuvchi rolini tahrirlash"
        AppLanguage.RU -> "Изменить роль пользователя"
        AppLanguage.EN -> "Edit user role"
    }

    fun editRoleCurrentRole(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Hozirgi rol:"
        AppLanguage.RU -> "Текущая роль:"
        AppLanguage.EN -> "Current role:"
    }

    fun editRoleNewRole(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Yangi rolni tanlang"
        AppLanguage.RU -> "Выберите новую роль"
        AppLanguage.EN -> "Select a new role"
    }

    fun editRoleLoading(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Rollar yuklanmoqda…"
        AppLanguage.RU -> "Роли загружаются…"
        AppLanguage.EN -> "Loading roles…"
    }

    fun editRoleChoosePlaceholder(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Rol tanlang…"
        AppLanguage.RU -> "Выберите роль…"
        AppLanguage.EN -> "Choose a role…"
    }

    fun editRolePermissionsTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Ruxsatlar ko'rinishi"
        AppLanguage.RU -> "Права доступа"
        AppLanguage.EN -> "Permission overview"
    }

    fun editRolePermissionsEmpty(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Bu rol uchun maxsus ruxsatlar belgilanmagan."
        AppLanguage.RU -> "Для этой роли права не заданы."
        AppLanguage.EN -> "No custom permissions are set for this role."
    }

    fun editRoleSave(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Saqlash"
        AppLanguage.RU -> "Сохранить"
        AppLanguage.EN -> "Save"
    }

    fun editRoleSelfDemoteError(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "O'zingizni SUPER_ADMIN ro'lidan olib tashlay olmaysiz."
        AppLanguage.RU -> "Нельзя снять с себя роль SUPER_ADMIN."
        AppLanguage.EN -> "You cannot remove your own SUPER_ADMIN role."
    }

    fun addUserTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Yangi foydalanuvchi"
        AppLanguage.RU -> "Новый пользователь"
        AppLanguage.EN -> "New user"
    }

    fun addUserUsername(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "USERNAME"
        AppLanguage.RU -> "USERNAME"
        AppLanguage.EN -> "USERNAME"
    }

    fun addUserUsernamePlaceholder(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Masalan: alisher01"
        AppLanguage.RU -> "Например: alisher01"
        AppLanguage.EN -> "e.g. alisher01"
    }

    fun addUserEmailPlaceholder(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "example@mexa.uz"
        AppLanguage.RU -> "example@mexa.uz"
        AppLanguage.EN -> "example@mexa.uz"
    }

    fun addUserRoles(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "ROLLAR"
        AppLanguage.RU -> "РОЛИ"
        AppLanguage.EN -> "ROLES"
    }

    fun addUserActive(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Faol foydalanuvchi"
        AppLanguage.RU -> "Активный пользователь"
        AppLanguage.EN -> "Active user"
    }

    fun addUserActiveHint(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Yangi foydalanuvchi email orqali tasdiqlanishi kerak."
        AppLanguage.RU -> "Новый пользователь должен подтвердить email."
        AppLanguage.EN -> "The new user must verify via email."
    }

    fun addUserUsernameMin(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Kamida 3 belgi"
        AppLanguage.RU -> "Не менее 3 символов"
        AppLanguage.EN -> "At least 3 characters"
    }

    fun addUserUsernameMax(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Ko'pi bilan 30 belgi"
        AppLanguage.RU -> "Не более 30 символов"
        AppLanguage.EN -> "At most 30 characters"
    }

    fun addUserUsernameChars(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Faqat harf, raqam va _"
        AppLanguage.RU -> "Только буквы, цифры и _"
        AppLanguage.EN -> "Letters, digits and _ only"
    }

    fun addUserEmailInvalid(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Email noto'g'ri"
        AppLanguage.RU -> "Неверный email"
        AppLanguage.EN -> "Invalid email"
    }

    fun deleteUserTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Foydalanuvchini o'chirish"
        AppLanguage.RU -> "Удалить пользователя"
        AppLanguage.EN -> "Delete user"
    }

    fun deleteUserConfirm(lang: AppLanguage, name: String, email: String) = when (lang) {
        AppLanguage.UZ -> "«$name» (@$email) butunlay o'chiriladi. Bu amalni bekor qilib bo'lmaydi."
        AppLanguage.RU -> "«$name» (@$email) будет удалён навсегда. Это действие нельзя отменить."
        AppLanguage.EN -> "«$name» (@$email) will be permanently deleted. This action cannot be undone."
    }

    fun deleteUserSubmit(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "O'chirish"
        AppLanguage.RU -> "Удалить"
        AppLanguage.EN -> "Delete"
    }

    // ── Qurilmalar (faol sessiyalar) ─────────────────────────────────────
    fun devicesTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Qurilmalar"
        AppLanguage.RU -> "Устройства"
        AppLanguage.EN -> "Devices"
    }

    fun devicesSubtitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Hisobingizga kirgan faol qurilmalar"
        AppLanguage.RU -> "Активные устройства, вошедшие в аккаунт"
        AppLanguage.EN -> "Active devices signed in to your account"
    }

    fun devicesCount(lang: AppLanguage, count: Int) = when (lang) {
        AppLanguage.UZ -> "$count ta faol qurilma"
        AppLanguage.RU -> "$count активных устройств"
        AppLanguage.EN -> "$count active devices"
    }

    fun devicesHint(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Joriy qurilmadan chiqish mumkin emas. Boshqa qurilmadagi sessiyani yopsangiz, u kirishdan chiqariladi va qayta kirishi kerak bo'ladi."
        AppLanguage.RU -> "С текущего устройства выйти нельзя. Если закрыть сессию на другом устройстве, оно будет выведено из системы и потребуется войти заново."
        AppLanguage.EN -> "You cannot log out from the current device. Closing a session on another device will require signing in again on it."
    }

    fun devicesColDevice(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "QURILMA"
        AppLanguage.RU -> "УСТРОЙСТВО"
        AppLanguage.EN -> "DEVICE"
    }

    fun devicesColIp(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "IP MANZIL"
        AppLanguage.RU -> "IP-АДРЕС"
        AppLanguage.EN -> "IP ADDRESS"
    }

    fun devicesColLoggedIn(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "KIRILGAN"
        AppLanguage.RU -> "ВОШЁЛ"
        AppLanguage.EN -> "SIGNED IN"
    }

    fun devicesColLastActive(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "OXIRGI FAOLLIK"
        AppLanguage.RU -> "ПОСЛЕДНЯЯ АКТИВНОСТЬ"
        AppLanguage.EN -> "LAST ACTIVE"
    }

    fun devicesColStatus(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "HOLAT"
        AppLanguage.RU -> "СТАТУС"
        AppLanguage.EN -> "STATUS"
    }

    fun devicesColAction(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "AMAL"
        AppLanguage.RU -> "ДЕЙСТВИЕ"
        AppLanguage.EN -> "ACTION"
    }

    fun devicesCurrentBadge(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Joriy qurilma"
        AppLanguage.RU -> "Текущее устройство"
        AppLanguage.EN -> "Current device"
    }

    fun devicesActiveBadge(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Faol"
        AppLanguage.RU -> "Активно"
        AppLanguage.EN -> "Active"
    }

    fun devicesClose(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Chiqish"
        AppLanguage.RU -> "Выйти"
        AppLanguage.EN -> "Log out"
    }

    fun devicesCloseTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Qurilmadan chiqish"
        AppLanguage.RU -> "Выйти с устройства"
        AppLanguage.EN -> "Log out device"
    }

    fun devicesCloseMessage(lang: AppLanguage, device: String) = when (lang) {
        AppLanguage.UZ -> "«$device» sessiyasi yopiladi. Ushbu qurilma tizimga qayta kirishi kerak bo'ladi. Davom etasizmi?"
        AppLanguage.RU -> "Сессия «$device» будет закрыта. С этого устройства потребуется войти заново. Продолжить?"
        AppLanguage.EN -> "The session on «$device» will be closed. This device will need to sign in again."
    }

    fun devicesClosed(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Sessiya yopildi"
        AppLanguage.RU -> "Сессия закрыта"
        AppLanguage.EN -> "Session closed"
    }

    fun devicesEmpty(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Faol qurilmalar topilmadi"
        AppLanguage.RU -> "Активные устройства не найдены"
        AppLanguage.EN -> "No active devices found"
    }

    fun devicesLoadError(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Ro'yxatni yuklab bo'lmadi"
        AppLanguage.RU -> "Не удалось загрузить список"
        AppLanguage.EN -> "Could not load the list"
    }

    fun devicesRefresh(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Yangilash"
        AppLanguage.RU -> "Обновить"
        AppLanguage.EN -> "Refresh"
    }

    fun devicesCurrentHint(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "—"
        AppLanguage.RU -> "—"
        AppLanguage.EN -> "—"
    }

    // --- Global qidiruv (T-H1) ---
    fun globalSearchPlaceholder(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Tizim bo'ylab qidiruv…"
        AppLanguage.RU -> "Поиск по системе…"
        AppLanguage.EN -> "Search across the system…"
    }

    /**
     * Global qidiruv natijalari bo'limlari: kalit → sarlavha.
     * Kalit [SuperAdminDashboardLayout] dagi ekran xaritasi bilan mos keladi.
     */
    fun globalSearchTargets(lang: AppLanguage): Map<String, String> = when (lang) {
        AppLanguage.UZ -> linkedMapOf(
            "orders" to "Buyurtmalar",
            "products" to "Mahsulotlar",
            "users" to "Foydalanuvchilar",
            "warehouses" to "Omborlar",
            "notifications" to "Bildirishnomalar",
            "auditlog" to "Audit jurnali",
        )
        AppLanguage.RU -> linkedMapOf(
            "orders" to "Заказы",
            "products" to "Товары",
            "users" to "Пользователи",
            "warehouses" to "Склады",
            "notifications" to "Уведомления",
            "auditlog" to "Журнал аудита",
        )
        AppLanguage.EN -> linkedMapOf(
            "orders" to "Orders",
            "products" to "Products",
            "users" to "Users",
            "warehouses" to "Warehouses",
            "notifications" to "Notifications",
            "auditlog" to "Audit log",
        )
    }

    // --- Tarjima jadvali (T-H5): ekrandagi UZ matn → (RU, EN) ---
    /**
     * Xargishqa (hardcoded) matnlar uchun tarjima jadvali.
     * Kalit — o'zbekcha manba matni; topilmasa o'z qaytadi (fallback UZ).
     */
    private val literalTranslations: Map<String, Pair<String, String>> = mapOf(
    "Qayta urinish" to ("Повторить" to "Retry"),
    "Topilmadi" to ("Не найдено" to "Not found"),
    "Nomi" to ("Название" to "Name"),
    "Bekor" to ("Отменено" to "Cancelled"),
    "Mahsulotlar" to ("Товары" to "Products"),
    "Tuman" to ("Район" to "District"),
    "Hudud" to ("Регион" to "Region"),
    "Rad etish" to ("Отклонить" to "Reject"),
    "Ma'lumot yo'q" to ("Нет данных" to "No data"),
    "Nofaol" to ("Неактивно" to "Inactive"),
    "Tez sinuvchan" to ("Быстропортящийся" to "Perishable"),
    "QIDIRUV" to ("ПОИСК" to "SEARCH"),
    "Bugungi ombor holati va faoliyat ko'rsatkichlari." to ("Сегодняшнее состояние склада и показатели активности." to "Today's warehouse status and activity metrics."),
    "Mahsulotlar topilmadi" to ("Товары не найдены" to "Products not found"),
    "Admin izohi" to ("Комментарий администратора" to "Admin comment"),
    "Izoh (ixtiyoriy)" to ("Комментарий (необязательно)" to "Comment (optional)"),
    "Barcha holatlar" to ("Все статусы" to "All statuses"),
    "Yangi zona" to ("Новая тарифная зона" to "New zone"),
    "Kuryer tayinlash" to ("Назначить курьера" to "Assign courier"),
    "O'zgarish sababi" to ("Причина изменения" to "Reason for change"),
    "— tanlanmagan —" to ("— не выбрано —" to "— not selected —"),
    "Boshlanish (yyyy-MM-ddTHH:mm)" to ("Начало (yyyy-MM-ddTHH:mm)" to "Start (yyyy-MM-ddTHH:mm)"),
    "Tugash (yyyy-MM-ddTHH:mm)" to ("Окончание (yyyy-MM-ddTHH:mm)" to "End (yyyy-MM-ddTHH:mm)"),
    "Omborlar" to ("Склады" to "Warehouses"),
    "Admin tayinlash" to ("Назначить администратора" to "Assign admin"),
    "Adminni olib tashlash" to ("Убрать администратора" to "Remove admin"),
    "Xatolik" to ("Ошибка" to "Error"),
    "Qaytarma yaratish" to ("Создать возврат" to "Create return"),
    "MAHSULOTLAR" to ("ТОВАРЫ" to "PRODUCTS"),
    "AMALLAR" to ("ДЕЙСТВИЯ" to "ACTIONS"),
    "IZOH (ixtiyoriy)" to ("КОММЕНТАРИЙ (необязательно)" to "COMMENT (optional)"),
    "Qator qo'shish" to ("Добавить строку" to "Add row"),
    "MAHSULOT" to ("ТОВАР" to "PRODUCT"),
    "Yakunlash" to ("Завершить" to "Finish"),
    "Orqaga" to ("Назад" to "Back"),
    "Yangi inventarizatsiya" to ("Новая инвентаризация" to "New inventory count"),
    "SANALDI" to ("ПОСЧИТАНО" to "COUNTED"),
    "Transfer qilish" to ("Переместить" to "Transfer"),
    "Masalan: Farg'ona Filiali" to ("Например: филиал Фергана" to "Example: Fergana Branch"),
    "Ombor nomi bo'sh bo'lmasligi kerak" to ("Название склада не может быть пустым" to "Warehouse name must not be empty"),
    "Kuryer biriktirish" to ("Прикрепить курьера" to "Attach courier"),
    "Sababni kiriting..." to ("Введите причину..." to "Enter a reason..."),
    "Sabab majburiy — o'chirish tarixiga yoziladi" to ("Причина обязательна — будет записана в историю удалений" to "Reason is required — it will be recorded in the deletion history"),
    "Tizim rollari va foydalanuvchilar bo'yicha taqsimot" to ("Распределение по ролям и пользователям системы" to "Breakdown by system roles and users"),
    "Yangi rol" to ("Новая роль" to "New role"),
    "RUXSATLAR" to ("ПРАВА" to "PERMISSIONS"),
    "Ruxsatlar yuklanmoqda…" to ("Загрузка прав…" to "Loading permissions…"),
    "TAVSIF" to ("ОПИСАНИЕ" to "DESCRIPTION"),
    "Bu rol haqida qisqacha ma'lumot…" to ("Краткое описание роли…" to "A short description of this role…"),
    "Brendlarni boshqarish" to ("Управление брендами" to "Manage brands"),
    "Kategoriyalarni boshqarish" to ("Управление категориями" to "Manage categories"),
    "Narx bosqichlari yo'q.\nQuyidagi tugma orqali qo'shing." to ("Нет ценовых уровней.\nДобавьте их кнопкой ниже." to "No price tiers yet.\nAdd them with the button below."),
    "Narx bosqichlari" to ("Ценовые уровни" to "Price tiers"),
    "Masalan: Samsung, Apple..." to ("Например: Samsung, Apple..." to "e.g. Samsung, Apple..."),
    "Qora, Oq, Ko'k..." to ("Чёрный, Белый, Синий..." to "Black, White, Blue..."),
    "+ Rang qo'shish" to ("+ Добавить цвет" to "+ Add color"),
    "Qo'shish" to ("Добавить" to "Add"),
    "MIQDORGA BOG'LIQ NARX BOSQICHLARI" to ("ЦЕНОВЫЕ УРОВНИ ЗАВИСИМО ОТ КОЛИЧЕСТВА" to "QUANTITY-BASED PRICE TIERS"),
    "Tavsiya etilgan teglar:" to ("Рекомендуемые теги:" to "Suggested tags:"),
    "Fayl tanlash" to ("Выбрать файл" to "Choose file"),
    "SEO VA ICHKI KODLAR" to ("SEO И ВНУТРЕННИЙ КОД" to "SEO & INTERNAL CODE"),
    "Qidiruvda ko'rinadigan qisqa tavsif..." to ("Краткое описание для поиска..." to "Short description shown in search..."),
    "Tahrirlash" to ("Редактировать" to "Edit"),
    "Asosiy kategoriya (yo'q)" to ("Основная категория (нет)" to "Primary category (none)"),
    "Rasm tanlash" to ("Выбрать изображение" to "Choose image"),
    "Kategoriya holati" to ("Статус категории" to "Category status"),
    "ROL" to ("РОЛЬ" to "ROLE"),
    "Guruh tafsilotlari" to ("Детали группы" to "Group details"),
    "Ma'lumot topilmadi" to ("Данные не найдены" to "No data found"),
    "Jami" to ("Итого" to "Total"),
    "O'chirish" to ("Удалить" to "Delete"),
    "Yopish" to ("Закрыть" to "Close"),
    "Davr" to ("Период" to "Period"),
    "Mahsulot" to ("Товар" to "Product"),
    "Faol" to ("Активен" to "Active"),
    "Miqdor" to ("Количество" to "Quantity"),
    "Tavsif" to ("Описание" to "Description"),
    "Ma'lumot yuklanmadi" to ("Не удалось загрузить данные" to "Failed to load data"),
    "Yangilash" to ("Обновить" to "Refresh"),
    "Chegira" to ("Скидка" to "Discount"),
    "Jami foydalanuvchilar" to ("Всего пользователей" to "Total users"),
    "Faol aksiyalar" to ("Активные акции" to "Active promotions"),
    "Bugungi chegirma (so'm)" to ("Скидка за сегодня (сум)" to "Discount today (UZS)"),
    "Kod" to ("Код" to "Code"),
    "Xizmatga ulanib bo'lmadi" to ("Не удалось подключиться к сервису" to "Could not connect to service"),
    "Qidirish" to ("Поиск" to "Search"),
    "Barcha turlar" to ("Все типы" to "All types"),
    "Turi" to ("Тип" to "Type"),
    "CHEGIRMA" to ("СКИДКА" to "DISCOUNT"),
    "Nofaollashtirish" to ("Деактивировать" to "Deactivate"),
    "BARCODE" to ("ШТРИХКОД" to "BARCODE"),
    "Ombor qoldig'i" to ("Остаток на складе" to "Warehouse stock"),
    "Band" to ("Зарезервировано" to "Reserved"),
    "Erkin" to ("Доступно" to "Available"),
    "Guruhlar" to ("Группы" to "Groups"),
    "Foydalanuvchi guruhlarini boshqarish" to ("Управление группами пользователей" to "Manage user groups"),
    "Yangi guruh" to ("Новая группа" to "New group"),
    "Guruh nomi yoki tavsif bo'yicha..." to ("По названию или описанию группы..." to "By group name or description..."),
    "Guruhlar topilmadi" to ("Группы не найдены" to "No groups found"),
    "NOM" to ("НАЗВАНИЕ" to "NAME"),
    "AZOLAR" to ("УЧАСТНИКИ" to "MEMBERS"),
    "Ko'rish" to ("Просмотр" to "View"),
    "Yangi guruh qo'shish" to ("Добавить группу" to "Add group"),
    "Guruhni tahrirlash" to ("Редактировать группу" to "Edit group"),
    "Guruh nomi" to ("Название группы" to "Group name"),
    "Masalan: Administratorlar" to ("Например: Администраторы" to "e.g. Administrators"),
    "Guruh haqida qisqa ma'lumot" to ("Краткое описание группы" to "Short group description"),
    "Guruhni o'chirish" to ("Удалить группу" to "Delete group"),
    "A'zolar" to ("Участники" to "Members"),
    "A'zolar topilmadi" to ("Участники не найдены" to "No members found"),
    "A'zoni o'chirish" to ("Удалить участника" to "Remove member"),
    "A'zo qo'shish" to ("Добавить участника" to "Add member"),
    "Foydalanuvchi ID (UUID)" to ("ID пользователя (UUID)" to "User ID (UUID)"),
    "Rol (ixtiyoriy)" to ("Роль (необязательно)" to "Role (optional)"),
    "Masalan: viewer" to ("Например: viewer" to "e.g. viewer"),
    "Guruh nomi majburiy" to ("Название группы обязательно" to "Group name is required"),
    "Foydalanuvchi ID majburiy" to ("ID пользователя обязателен" to "User ID is required"),
    "Analitika" to ("Аналитика" to "Analytics"),
    "Savdo, buyurtma va foydalanuvchi ko'rsatkichlari" to ("Показатели продаж, заказов и пользователей" to "Sales, order and user metrics"),
    "Ba'zi bo'limlar yuklanmadi" to ("Некоторые разделы не загрузились" to "Some sections failed to load"),
    "Umumiy ko'rsatkichlar" to ("Общие показатели" to "Overview"),
    "Hisoblangan vaqt" to ("Время вычисления" to "Computed at"),
    "Daromad grafigi" to ("График выручки" to "Revenue chart"),
    "Buyurtmalar taqsimoti" to ("Распределение заказов" to "Orders breakdown"),
    "Konversiya" to ("Конверсия" to "Conversion"),
    "Top mahsulotlar" to ("Лучшие товары" to "Top products"),
    "Zaxira ogohlantirishlari" to ("Оповещения о запасах" to "Stock alerts"),
    "Foydalanuvchilar o'sishi" to ("Рост пользователей" to "User growth"),
    "Chegirmalar" to ("Скидки" to "Discounts"),
    "Bugungi buyurtmalar" to ("Заказы сегодня" to "Orders today"),
    "Bugungi daromad (so'm)" to ("Выручка сегодня (сум)" to "Revenue today (UZS)"),
    "Kam qolgan mahsulotlar" to ("Товары с низким остатком" to "Low stock products"),
    "Bugungi yangi foydalanuvchilar" to ("Новые пользователи сегодня" to "New users today"),
    "Tasdiqlangan foydalanuvchilar" to ("Подтверждённые пользователи" to "Verified users"),
    "Eng yuqori" to ("Максимум" to "Highest"),
    "Yangi foydalanuvchilar" to ("Новые пользователи" to "New users"),
    "Sotilgan" to ("Продано" to "Sold"),
    "Daromad (so'm)" to ("Выручка (сум)" to "Revenue (UZS)"),
    "Ombor" to ("Склад" to "Warehouse"),
    "Ishlatilgan" to ("Использовано" to "Used"),
    "Summa (so'm)" to ("Сумма (сум)" to "Amount (UZS)"),
    "Qidiruv so'rovi bo'sh" to ("Поисковый запрос пуст" to "Search query is empty"),
    "Qidiruv boshqaruvi" to ("Управление поиском" to "Search operations"),
    "Elasticsearch indeksi holati, qayta indekslash va qidiruvni sinash" to ("Состояние индекса Elasticsearch, переиндексация и проверка поиска" to "Elasticsearch index status, reindexing and search testing"),
    "Indeks holati" to ("Состояние индекса" to "Index status"),
    "Server vaqti" to ("Время сервера" to "Server time"),
    "Indeksdagi hujjatlar" to ("Документов в индексе" to "Documents in index"),
    "Jami mahsulotlar" to ("Всего товаров" to "Total products"),
    "Oxirgi reindex" to ("Последняя переиндексация" to "Last reindex"),
    "Vaqt" to ("Время" to "Time"),
    "Reindex bajarilmoqda" to ("Выполняется переиндексация" to "Reindexing in progress"),
    "Qayta indekslash" to ("Переиндексация" to "Reindex"),
    "Reindex muvaffaqiyatli yakunlandi" to ("Переиндексация успешно завершена" to "Reindex completed successfully"),
    "Reindex xatosi" to ("Ошибка переиндексации" to "Reindex error"),
    "Hujjatlar" to ("Документы" to "Documents"),
    "Indeksni sinash" to ("Проверка индекса" to "Test index"),
    "Qidiruv so'rovi" to ("Поисковый запрос" to "Search query"),
    "Mahsulot nomini kiriting..." to ("Введите название товара..." to "Enter product name..."),
    "Takliflar" to ("Результаты" to "Results"),
    "Natijalar" to ("Результаты" to "Results"),
    "Narx" to ("Цена" to "Price"),
    "Kategoriya" to ("Категория" to "Category"),
    "Brend" to ("Бренд" to "Brand"),
    "Mavjud" to ("В наличии" to "Available"),
    "Ha" to ("Да" to "Yes"),
    "Yo'q" to ("Нет" to "No"),
    "Muvaffaqiyatli" to ("Успешно" to "Success"),
    "Muvaffaqiyatsiz" to ("Неудачно" to "Failed"),
    "Aksiyalar" to ("Акции" to "Promotions"),
    "Chegirma va promo-kodlarni boshqarish" to ("Управление скидками и промокодами" to "Manage discounts and promo codes"),
    "Yangi aksiya" to ("Новая акция" to "New promotion"),
    "Jami aksiyalar" to ("Всего акций" to "Total promotions"),
    "Berilgan chegirma" to ("Предоставлено скидок" to "Discount given"),
    "Top aksiya kodi" to ("Лучший промокод" to "Top promo code"),
    "Top aksiya foydalanishlari" to ("Лучшая акция по использованиям" to "Top promotion usage"),
    "Aksiyalar topilmadi" to ("Акции не найдены" to "No promotions found"),
    "Aksiya nomi yoki kod bo'yicha..." to ("По названию или коду акции..." to "By promotion name or code..."),
    "NOM / KOD" to ("НАЗВАНИЕ / КОД" to "NAME / CODE"),
    "TURI" to ("ТИП" to "TYPE"),
    "MOHLAT" to ("СРОК" to "PERIOD"),
    "FOYDALANISH" to ("ИСПОЛЬЗОВАНИЕ" to "USAGE"),
    "HOLAT" to ("СТАТУС" to "STATUS"),
    "FAOL" to ("АКТИВНА" to "ACTIVE"),
    "NOFAOL" to ("НЕАКТИВНА" to "INACTIVE"),
    "Foydalanishlar" to ("Использования" to "Usages"),
    "Qat'iy summa" to ("Фиксированная сумма" to "Fixed amount"),
    "Bepul yetkazish" to ("Бесплатная доставка" to "Free shipping"),
    "Foiz" to ("Процент" to "Percent"),
    "Yangi aksiya qo'shish" to ("Добавить акцию" to "Add promotion"),
    "Aksiyani tahrirlash" to ("Редактировать акцию" to "Edit promotion"),
    "Yoz aksiyasi" to ("Летняя акция" to "Summer sale"),
    "Chegirma qiymati" to ("Значение скидки" to "Discount value"),
    "Boshlanish vaqti" to ("Время начала" to "Start time"),
    "Tugash vaqti" to ("Время окончания" to "End time"),
    "Ixtiyoriy matn" to ("Необязательный текст" to "Optional text"),
    "Aksiyani o'chirish" to ("Удалить акцию" to "Delete promotion"),
    "Aksiyani nofaollashtirish" to ("Деактивировать акцию" to "Deactivate promotion"),
    "Foydalanish tarixi" to ("История использований" to "Usage history"),
    "Foydalanishlar topilmadi" to ("Использования не найдены" to "No usages found"),
    "FOYDALANUVCHI" to ("ПОЛЬЗОВАТЕЛЬ" to "USER"),
    "BUYURTMA" to ("ЗАКАЗ" to "ORDER"),
    "SANA" to ("ДАТА" to "DATE"),
    "Nomi majburiy" to ("Название обязательно" to "Name is required"),
    "Kod majburiy" to ("Код обязателен" to "Code is required"),
    "Chegirma qiymati noto'g'ri" to ("Неверное значение скидки" to "Invalid discount value"),
    "Boshlanish vaqti noto'g'ri formatda" to ("Неверный формат времени начала" to "Invalid start time format"),
    "Tugash vaqti noto'g'ri formatda" to ("Неверный формат времени окончания" to "Invalid end time format"),
    )

    /** [uz] matnini til bo'yicha tarjima qiladi (jadvalda bo'lmasa UZ qaytariladi). */
    fun t(lang: AppLanguage, uz: String): String = when (lang) {
        AppLanguage.UZ -> uz
        AppLanguage.RU -> literalTranslations[uz]?.first ?: uz
        AppLanguage.EN -> literalTranslations[uz]?.second ?: uz
    }

}

data class AuthNetworkStrings(
    val emptyResponse: String,
    val tokenMissing: String,
    val rateLimited: String,
    val requestFailed: String,
    val networkError: String,
    val unknownError: String,
    val serverErrorPrefix: String,
)
