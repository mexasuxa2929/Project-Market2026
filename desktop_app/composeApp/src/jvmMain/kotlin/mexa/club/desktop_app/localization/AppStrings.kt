package mexa.club.desktop_app.localization

/**
 * Barcha foydalanuvchi matnlari: o‘zbek (UZ) va rus (RU).
 */
object AppStrings {

    fun ok(lang: AppLanguage): String = when (lang) {
        AppLanguage.UZ -> "OK"
        AppLanguage.RU -> "OK"
    }

    fun cancel(lang: AppLanguage): String = when (lang) {
        AppLanguage.UZ -> "Bekor qilish"
        AppLanguage.RU -> "Отмена"
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
    }

    // --- Login ---
    fun loginTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Kirish"
        AppLanguage.RU -> "Вход"
    }

    fun loginSubtitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Admin paneliga kirish uchun ma'lumotlaringizni kiriting"
        AppLanguage.RU -> "Введите данные для входа в панель администратора"
    }

    fun fieldUsername(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "FOYDALANUVCHI NOMI"
        AppLanguage.RU -> "ИМЯ ПОЛЬЗОВАТЕЛЯ"
    }

    fun fieldPassword(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "PAROL"
        AppLanguage.RU -> "ПАРОЛЬ"
    }

    /** Ro‘yxatdan o‘tish formasi uchun oddiy «Parol». */
    fun registerFieldPassword(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Parol"
        AppLanguage.RU -> "Пароль"
    }

    fun placeholderUsername(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Login yoki foydalanuvchi nomi"
        AppLanguage.RU -> "Логин или имя пользователя"
    }

    fun placeholderLoginPassword(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Parolingizni kiriting"
        AppLanguage.RU -> "Введите пароль"
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
        }

    fun rememberMe(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Meni eslab qol"
        AppLanguage.RU -> "Запомнить меня"
    }

    fun forgotPasswordLink(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Parolni unutdingizmi?"
        AppLanguage.RU -> "Забыли пароль?"
    }

    fun loginButton(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Kirish"
        AppLanguage.RU -> "Войти"
    }

    fun loginLoading(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Kirilmoqda…"
        AppLanguage.RU -> "Вход…"
    }

    fun loginNoCredentials(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Foydalanuvchi nomi va parolni kiriting."
        AppLanguage.RU -> "Введите имя пользователя и пароль."
    }

    fun loginDialogSuccessTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Kirish muvaffaqiyatli"
        AppLanguage.RU -> "Вход выполнен"
    }

    fun loginDialogSuccessMessage(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Sessiya ochildi. Davom etish uchun OK tugmasini bosing."
        AppLanguage.RU -> "Сессия открыта. Нажмите OK, чтобы продолжить."
    }

    fun loginDialogFailTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Kirish amalga oshmadi"
        AppLanguage.RU -> "Вход не выполнен"
    }

    fun loginNoAccessTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Kirish taqiqlangan"
        AppLanguage.RU -> "Доступ запрещён"
    }

    fun loginNoAccessMessage(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Bu panel faqat SUPER_ADMIN va ADMIN rollariga ega foydalanuvchilar uchun. Sizning hisobingizda kerakli ruxsat yo'q."
        AppLanguage.RU -> "Эта панель только для пользователей с ролями SUPER_ADMIN или ADMIN. У вашего аккаунта нет необходимых прав."
    }

    fun noAccount(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Hisobingiz yo'qmi?"
        AppLanguage.RU -> "Нет аккаунта?"
    }

    fun goRegister(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Ro'yxatdan o'tish"
        AppLanguage.RU -> "Регистрация"
    }

    fun copyright(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "© 2024 WAREHOUSE MANAGEMENT SYSTEM"
        AppLanguage.RU -> "© 2024 WAREHOUSE MANAGEMENT SYSTEM"
    }

    fun loginLeftBrand(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "WAREHOUSE ADMIN"
        AppLanguage.RU -> "WAREHOUSE ADMIN"
    }

    fun loginLeftHeadline(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Omborlaringizni aqlli boshqaring"
        AppLanguage.RU -> "Управляйте складами умно"
    }

    fun loginLeftBody(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Markazlashtirilgan tizim orqali inventarizatsiya, logistika va xodimlarni boshqarishni yangi bosqichga olib chiqing."
        AppLanguage.RU -> "Централизованная система: инвентаризация, логистика и персонал на новом уровне."
    }

    fun loginLeftQuote(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "\"Logistika - bu to'g'ri mahsulotni to'g'ri vaqtda to'g'ri joyga yetkazish san'atidir.\""
        AppLanguage.RU -> "\"Логистика — это искусство доставить нужный товар в нужное время и место.\""
    }

    // Forgot password dialog
    fun forgotDialogTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Parolni tiklash"
        AppLanguage.RU -> "Восстановление пароля"
    }

    fun forgotDialogHint(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Emailni kiriting, kodni yuboring, keyin kodni tekshirib yangi parolni o‘rnating."
        AppLanguage.RU -> "Введите email, отправьте код, затем подтвердите код и задайте новый пароль."
    }

    fun forgotPlaceholder(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "email@example.com"
        AppLanguage.RU -> "email@example.com"
    }

    fun forgotSend(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Yuborish"
        AppLanguage.RU -> "Отправить"
    }

    fun forgotSending(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Yuborilmoqda…"
        AppLanguage.RU -> "Отправка…"
    }

    fun forgotEmailRequiredTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Email kerak"
        AppLanguage.RU -> "Нужен email"
    }

    fun forgotEmailRequiredMessage(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Parolni tiklash uchun email manzilini kiriting."
        AppLanguage.RU -> "Введите адрес email для восстановления пароля."
    }

    fun forgotSuccessTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "So‘rov yuborildi"
        AppLanguage.RU -> "Запрос отправлен"
    }

    fun forgotSuccessMessage(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Agar bu email tizimda mavjud bo‘lsa, 6 xonali tiklash kodi yuboriladi."
        AppLanguage.RU -> "Если этот email есть в системе, будет отправлен 6-значный код сброса."
    }

    fun forgotFailTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Yuborib bo‘lmadi"
        AppLanguage.RU -> "Не удалось отправить"
    }

    fun resetCodeLabel(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Tasdiqlash kodi"
        AppLanguage.RU -> "Код подтверждения"
    }

    fun resetCodePlaceholder(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Emailga kelgan 6 xonali kod"
        AppLanguage.RU -> "6-значный код из email"
    }

    fun resetNewPasswordLabel(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Yangi parol"
        AppLanguage.RU -> "Новый пароль"
    }

    fun resetNewPasswordPlaceholder(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Yangi parol (kamida 8 belgi)"
        AppLanguage.RU -> "Новый пароль (не менее 8 символов)"
    }

    fun resetSendCode(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Kod yuborish"
        AppLanguage.RU -> "Отправить код"
    }

    fun resetVerifyCode(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Kodni tekshirish"
        AppLanguage.RU -> "Проверить код"
    }

    fun resetApplyPassword(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Parolni yangilash"
        AppLanguage.RU -> "Обновить пароль"
    }

    fun resetCodeRequired(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Tasdiqlash kodini kiriting."
        AppLanguage.RU -> "Введите код подтверждения."
    }

    fun resetPasswordRequired(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Yangi parol kamida 8 belgidan iborat bo‘lsin."
        AppLanguage.RU -> "Новый пароль должен быть не короче 8 символов."
    }

    fun resetCodeVerifiedTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Kod tasdiqlandi"
        AppLanguage.RU -> "Код подтверждён"
    }

    fun resetCodeVerifiedMessage(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Endi yangi parolni kiriting va yangilang."
        AppLanguage.RU -> "Теперь введите новый пароль и обновите его."
    }

    fun resetPasswordUpdatedTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Parol yangilandi"
        AppLanguage.RU -> "Пароль обновлён"
    }

    fun resetPasswordUpdatedMessage(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Parolingiz muvaffaqiyatli yangilandi."
        AppLanguage.RU -> "Ваш пароль успешно обновлён."
    }

    // --- Register ---
    fun registerTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Ro'yxatdan o'tish"
        AppLanguage.RU -> "Регистрация"
    }

    fun registerSubtitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Tizimdan foydalanish uchun ma'lumotlaringizni kiriting"
        AppLanguage.RU -> "Введите данные для использования системы"
    }

    fun registerCompactBrand(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Warehouse Admin"
        AppLanguage.RU -> "Warehouse Admin"
    }

    fun fieldDisplayUsername(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Foydalanuvchi nomi"
        AppLanguage.RU -> "Имя пользователя"
    }

    fun placeholderUsernameExample(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Masalan: warehouse_admin"
        AppLanguage.RU -> "Например: warehouse_admin"
    }

    fun fieldEmail(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Email manzili"
        AppLanguage.RU -> "Email"
    }

    fun placeholderEmail(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "ism@kompaniya.uz"
        AppLanguage.RU -> "имя@компания.uz"
    }

    fun placeholderRegisterPassword(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Min. 8 belgi: katta/kichik, raqam, belgi"
        AppLanguage.RU -> "Мин. 8 симв.: буквы, цифры, знак"
    }

    fun placeholderRegisterConfirm(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Parolni qayta kiriting"
        AppLanguage.RU -> "Повторите пароль"
    }

    fun fieldConfirmPassword(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Parolni tasdiqlang"
        AppLanguage.RU -> "Подтвердите пароль"
    }

    fun termsAgreement(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Men Foydalanish shartlari va Maxfiylik siyosati bilan roziman"
        AppLanguage.RU -> "Я согласен с Условиями использования и Политикой конфиденциальности"
    }

    fun registerButton(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Ro'yxatdan o'tish"
        AppLanguage.RU -> "Зарегистрироваться"
    }

    fun registerSending(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Yuborilmoqda…"
        AppLanguage.RU -> "Отправка…"
    }

    fun registerValidationFields(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Barcha majburiy maydonlarni to‘ldiring."
        AppLanguage.RU -> "Заполните все обязательные поля."
    }

    fun registerValidationWeakPassword(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Parol kamida 8 belgidan iborat bo‘lsin."
        AppLanguage.RU -> "Пароль должен быть не короче 8 символов."
    }

    fun registerValidationMismatch(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Parol va tasdiqlash bir xil bo‘lishi kerak."
        AppLanguage.RU -> "Пароль и подтверждение должны совпадать."
    }

    fun registerValidationTerms(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Foydalanish shartlarini qabul qiling."
        AppLanguage.RU -> "Примите условия использования."
    }

    fun registerFailTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Ro‘yxatdan o‘tish muvaffaqiyatsiz"
        AppLanguage.RU -> "Регистрация не удалась"
    }

    fun registerSuccessTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Kod yuborildi"
        AppLanguage.RU -> "Код отправлен"
    }

    fun registerSuccessMessage(lang: AppLanguage, email: String) = when (lang) {
        AppLanguage.UZ -> "Tasdiqlash kodi $email manziliga yuborildi. Keyingi qadamda kodni kiriting."
        AppLanguage.RU -> "Код подтверждения отправлен на $email. На следующем шаге введите код."
    }

    fun registerValidationTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Ma'lumot yetishmayapti"
        AppLanguage.RU -> "Недостаточно данных"
    }

    fun registerWeakPasswordTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Zaif parol"
        AppLanguage.RU -> "Слабый пароль"
    }

    fun registerMismatchTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Parollar mos emas"
        AppLanguage.RU -> "Пароли не совпадают"
    }

    fun registerTermsTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Shartlar"
        AppLanguage.RU -> "Условия"
    }

    fun hasAccount(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Hisobingiz bormi?"
        AppLanguage.RU -> "Уже есть аккаунт?"
    }

    fun goLogin(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Tizimga kirish"
        AppLanguage.RU -> "Войти"
    }

    fun registerLeftBrand(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "WAREHOUSE ADMIN"
        AppLanguage.RU -> "WAREHOUSE ADMIN"
    }

    fun registerLeftHeadline(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Logistikani aqlli boshqaruv tizimi bilan optimallashtiring"
        AppLanguage.RU -> "Оптимизируйте логистику с умной системой управления"
    }

    fun registerLeftBody(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Ombor zaxiralari, buyurtmalar va yetkazib berish zanjirini yagona platformada real vaqt rejimida kuzatib boring."
        AppLanguage.RU -> "Следите за запасами, заказами и цепочкой поставок на одной платформе в реальном времени."
    }

    fun registerLeftFooter(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "© 2024 Warehouse Admin Panel. Barcha huquqlar himoyalangan."
        AppLanguage.RU -> "© 2024 Warehouse Admin Panel. Все права защищены."
    }

    // --- Email OTP ---
    fun verifyTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Emailingizni tasdiqlang"
        AppLanguage.RU -> "Подтвердите email"
    }

    fun verifyIntro(lang: AppLanguage, email: String) = when (lang) {
        AppLanguage.UZ -> "Biz sizning $email elektron pochtangizga 6 xonali tasdiqlash kodini yubordik."
        AppLanguage.RU -> "Мы отправили 6-значный код подтверждения на $email."
    }

    fun verifyResendQuestion(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Kodni olmadingizmi?"
        AppLanguage.RU -> "Не получили код?"
    }

    fun verifyResendIn(lang: AppLanguage, seconds: Int) = when (lang) {
        AppLanguage.UZ -> "Qayta yuborish (${seconds}s)"
        AppLanguage.RU -> "Повторная отправка (${seconds} с)"
    }

    fun verifyResend(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Qayta yuborish"
        AppLanguage.RU -> "Отправить снова"
    }

    fun verifyResending(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Yuborilmoqda…"
        AppLanguage.RU -> "Отправка…"
    }

    fun verifySubmit(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Tasdiqlash"
        AppLanguage.RU -> "Подтвердить"
    }

    fun verifySubmitting(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Tekshirilmoqda…"
        AppLanguage.RU -> "Проверка…"
    }

    fun verifyBackLogin(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Kirish sahifasiga qaytish"
        AppLanguage.RU -> "Вернуться ко входу"
    }

    fun verifyHelp(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "YORDAM"
        AppLanguage.RU -> "ПОМОЩЬ"
    }

    fun verifyCodeFormatTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Kod noto‘g‘ri formatda"
        AppLanguage.RU -> "Неверный формат кода"
    }

    fun verifyCodeFormatMessage(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "6 raqamli tasdiqlash kodini to‘liq kiriting."
        AppLanguage.RU -> "Введите полный 6-значный код."
    }

    fun verifyFailTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Tasdiqlash muvaffaqiyatsiz"
        AppLanguage.RU -> "Подтверждение не удалось"
    }

    fun verifyLoginFailTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Kirishda xato"
        AppLanguage.RU -> "Ошибка входа"
    }

    fun verifyLoginFailMessage(lang: AppLanguage, detail: String) = when (lang) {
        AppLanguage.UZ -> "Email tasdiqlandi, lekin avtomatik kirish amalga oshmadi: $detail"
        AppLanguage.RU -> "Email подтверждён, но автоматический вход не выполнен: $detail"
    }

    fun verifySuccessTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Hisob tayyor"
        AppLanguage.RU -> "Аккаунт готов"
    }

    fun verifySuccessMessage(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Email tasdiqlandi va tizimga kirdingiz."
        AppLanguage.RU -> "Email подтверждён, вы вошли в систему."
    }

    fun verifyResendOkTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Kod yuborildi"
        AppLanguage.RU -> "Код отправлен"
    }

    fun verifyResendOkMessage(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Yangi tasdiqlash kodi email manzilingizga yuborildi."
        AppLanguage.RU -> "Новый код подтверждения отправлен на ваш email."
    }

    fun verifyResendFailTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Qayta yuborib bo‘lmadi"
        AppLanguage.RU -> "Не удалось отправить повторно"
    }

    fun verifyLeftBrand(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "WAREHOUSE ADMIN"
        AppLanguage.RU -> "WAREHOUSE ADMIN"
    }

    fun verifyLeftHeadline(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Logistika tizimini boshqarishning eng samarali usuli."
        AppLanguage.RU -> "Самый эффективный способ управления логистикой."
    }

    fun verifyLeftBody(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Bizning platformamiz orqali real vaqt rejimida ombor zahiralari va kuryerlar harakatini kuzatib boring."
        AppLanguage.RU -> "Следите за остатками на складе и курьерами в реальном времени."
    }

    fun verifyLeftFooter(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "© 2024 Warehouse Admin Panel. Barcha huquqlar himoyalangan."
        AppLanguage.RU -> "© 2024 Warehouse Admin Panel. Все права защищены."
    }

    fun sidebarSubtitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Super Admin Panel"
        AppLanguage.RU -> "Панель супер-админа"
    }

    fun logout(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Chiqish"
        AppLanguage.RU -> "Выход"
    }

    fun logoutDialogTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Chiqishni tasdiqlang"
        AppLanguage.RU -> "Подтвердите выход"
    }

    fun logoutDialogMessage(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Haqiqatan ham tizimdan chiqmoqchimisiz? Sessiyangiz tugatiladi."
        AppLanguage.RU -> "Вы действительно хотите выйти? Ваша сессия будет завершена."
    }

    fun logoutConfirm(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Ha, chiqish"
        AppLanguage.RU -> "Да, выйти"
    }

    fun searchPlaceholder(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Tizim bo'ylab qidiruv..."
        AppLanguage.RU -> "Поиск по системе..."
    }

    fun userDisplayName(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Super Admin"
        AppLanguage.RU -> "Супер-админ"
    }

    fun userRole(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Asosiy boshqaruvchi"
        AppLanguage.RU -> "Главный администратор"
    }

    fun dashboardPanelTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Boshqaruv Paneli"
        AppLanguage.RU -> "Панель управления"
    }

    fun dashboardWelcome(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Xush kelibsiz! Bugungi ombor holati va faoliyat ko'rsatkichlari."
        AppLanguage.RU -> "Добро пожаловать! Состояние склада и показатели активности за сегодня."
    }

    fun last30Days(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Oxirgi 30 kun"
        AppLanguage.RU -> "Последние 30 дней"
    }

    fun downloadReport(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Hisobot yuklash"
        AppLanguage.RU -> "Скачать отчёт"
    }

    fun statTodayOrders(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Bugungi buyurtmalar"
        AppLanguage.RU -> "Заказы сегодня"
    }

    fun statTodayRevenue(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Bugungi daromad"
        AppLanguage.RU -> "Выручка сегодня"
    }

    fun statPendingOrders(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Kutilayotgan buyurtmalar"
        AppLanguage.RU -> "Ожидающие заказы"
    }

    fun statLowStock(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Kam qoldiq ogohlantirishlari"
        AppLanguage.RU -> "Предупреждения о запасах"
    }

    fun badgeAttention(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "E'tibor talab"
        AppLanguage.RU -> "Требует внимания"
    }

    fun badgeUrgent(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Tezda bajarilishi kerak"
        AppLanguage.RU -> "Срочно обработать"
    }

    fun badgeCritical(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Kritik"
        AppLanguage.RU -> "Критично"
    }

    fun badgeDepleting(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Zaxira tugayapti"
        AppLanguage.RU -> "Запас заканчивается"
    }

    fun chartRevenueTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Oylik daromad grafigi"
        AppLanguage.RU -> "График выручки по месяцам"
    }

    fun chartRevenueSeries(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Daromad (UZS)"
        AppLanguage.RU -> "Выручка (UZS)"
    }

    fun chartWeekLabels(lang: AppLanguage): List<String> = when (lang) {
        AppLanguage.UZ -> listOf("01 Okt", "07 Okt", "14 Okt", "21 Okt", "28 Okt", "Bugun")
        AppLanguage.RU -> listOf("01 окт", "07 окт", "14 окт", "21 окт", "28 окт", "Сегодня")
    }

    fun ordersStatusTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Buyurtmalar holati"
        AppLanguage.RU -> "Статус заказов"
    }

    fun totalLabel(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Jami"
        AppLanguage.RU -> "Всего"
    }

    fun donutPending(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "KUTILMOQDA"
        AppLanguage.RU -> "ОЖИДАНИЕ"
    }

    fun donutDelivered(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "YETKAZILDI"
        AppLanguage.RU -> "ДОСТАВЛЕНО"
    }

    fun donutProcessing(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "JARAYONDA"
        AppLanguage.RU -> "В ОБРАБОТКЕ"
    }

    fun donutCancelled(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "BEKOR QILINGAN"
        AppLanguage.RU -> "ОТМЕНЁН"
    }

    fun topProducts(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Top mahsulotlar"
        AppLanguage.RU -> "Топ товаров"
    }

    fun seeAll(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Hammasini ko'rish"
        AppLanguage.RU -> "Смотреть все"
    }

    fun recentOrders(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Oxirgi buyurtmalar"
        AppLanguage.RU -> "Последние заказы"
    }

    fun allOrders(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Barchasi"
        AppLanguage.RU -> "Все"
    }

    fun tableOrderNo(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Buyurtma №"
        AppLanguage.RU -> "Заказ №"
    }

    fun tableAmount(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Summa"
        AppLanguage.RU -> "Сумма"
    }

    fun tableStatus(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Holat"
        AppLanguage.RU -> "Статус"
    }

    fun tableProductName(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Mahsulot nomi"
        AppLanguage.RU -> "Товар"
    }

    fun tableSold(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Sotilgan"
        AppLanguage.RU -> "Продано"
    }

    fun tableRevenue(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Daromad"
        AppLanguage.RU -> "Выручка"
    }

    fun orderStatusDelivered(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Yetkazildi"
        AppLanguage.RU -> "Доставлен"
    }

    fun orderStatusProcessing(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Jarayonda"
        AppLanguage.RU -> "В обработке"
    }

    fun orderStatusPending(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Kutilmoqda"
        AppLanguage.RU -> "Ожидает"
    }

    fun orderStatusCancelled(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Bekor qilindi"
        AppLanguage.RU -> "Отменён"
    }

    fun settingsConnectionTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Ulanish sozlamalari"
        AppLanguage.RU -> "Настройки подключения"
    }

    fun settingsGatewayLabel(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Gateway URL"
        AppLanguage.RU -> "Gateway URL"
    }

    fun settingsSave(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Saqlash"
        AppLanguage.RU -> "Сохранить"
    }

    fun settingsReset(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Standartga qaytarish"
        AppLanguage.RU -> "Сбросить"
    }

    fun settingsSavedTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Saqlandi"
        AppLanguage.RU -> "Сохранено"
    }

    fun settingsSavedMessage(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Gateway URL muvaffaqiyatli saqlandi."
        AppLanguage.RU -> "Gateway URL успешно сохранён."
    }

    fun settingsUrlError(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "URL to'g'ri formatda bo'lishi kerak (http:// yoki https://)"
        AppLanguage.RU -> "URL должен начинаться с http:// или https://"
    }

    fun settingsLanguageTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Interfeys tili"
        AppLanguage.RU -> "Язык интерфейса"
    }

    fun settingsSessionTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Joriy sessiya"
        AppLanguage.RU -> "Текущая сессия"
    }

    fun settingsSessionActive(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Sessiya faol"
        AppLanguage.RU -> "Сессия активна"
    }

    fun settingsSessionNone(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Sessiya yo'q"
        AppLanguage.RU -> "Сессия отсутствует"
    }

    fun settingsLogout(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Chiqish (Logout)"
        AppLanguage.RU -> "Выход (Logout)"
    }

    fun settingsAboutTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Ilova haqida"
        AppLanguage.RU -> "О приложении"
    }

    fun settingsGatewayPlaceholder(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "http://localhost:8080"
        AppLanguage.RU -> "http://localhost:8080"
    }

    fun settingsLanguageUz(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "O'zbekcha"
        AppLanguage.RU -> "Узбекский"
    }

    fun settingsLanguageRu(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Русский"
        AppLanguage.RU -> "Русский"
    }

    fun settingsSessionActiveWithToken(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Sessiya faol - token mavjud"
        AppLanguage.RU -> "Сессия активна - токен присутствует"
    }

    fun settingsTokenLabel(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Token"
        AppLanguage.RU -> "Токен"
    }

    fun settingsVersion(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Versiya: 1.0.0"
        AppLanguage.RU -> "Версия: 1.0.0"
    }

    fun settingsProductName(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "mexa.club Warehouse Admin"
        AppLanguage.RU -> "mexa.club Warehouse Admin"
    }

    fun settingsCopyright(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "© 2026 mexa.club"
        AppLanguage.RU -> "© 2026 mexa.club"
    }

    /** Demo mahsulot: "donа" / "шт" */
    fun unitsPiece(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "dona"
        AppLanguage.RU -> "шт"
    }

    // ─── Users bo'limi ─────────────────────────────────────────────────────────

    fun usersScreenTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Foydalanuvchilar"
        AppLanguage.RU -> "Пользователи"
    }

    fun usersScreenSubtitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Tizim foydalanuvchilari va rollarini boshqarish"
        AppLanguage.RU -> "Управление пользователями и ролями системы"
    }

    fun usersSearchPlaceholder(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Username yoki email..."
        AppLanguage.RU -> "Username или email..."
    }

    fun usersFilterRole(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Rol"
        AppLanguage.RU -> "Роль"
    }

    fun usersFilterStatus(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Holat"
        AppLanguage.RU -> "Статус"
    }

    fun usersFilterAllRoles(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Barcha rollar"
        AppLanguage.RU -> "Все роли"
    }

    fun usersFilterAllStatus(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Barcha holat"
        AppLanguage.RU -> "Все статусы"
    }

    fun usersColUser(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "FOYDALANUVCHI"
        AppLanguage.RU -> "ПОЛЬЗОВАТЕЛЬ"
    }

    fun usersColRole(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "ROL VA BO'LIM"
        AppLanguage.RU -> "РОЛЬ И ОТДЕЛ"
    }

    fun usersColStatus(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "HOLATI"
        AppLanguage.RU -> "СТАТУС"
    }

    fun usersColVerified(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "TASDIQLANGAN"
        AppLanguage.RU -> "ПОДТВЕРЖДЁН"
    }

    fun usersColLastActivity(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "SO'NGGI FAOLLIK"
        AppLanguage.RU -> "ПОСЛЕДНЯЯ АКТИВНОСТЬ"
    }

    fun verifiedYes(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Tasdiqlangan"
        AppLanguage.RU -> "Подтверждён"
    }

    fun verifiedPending(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Kutilmoqda"
        AppLanguage.RU -> "Ожидает"
    }

    fun usersEmptyTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Foydalanuvchilar topilmadi"
        AppLanguage.RU -> "Пользователи не найдены"
    }

    fun usersEmptyBody(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Qidiruv shartlariga mos keladigan foydalanuvchi mavjud emas yoki hali qo'shilmagan."
        AppLanguage.RU -> "Пользователи, соответствующие запросу, не найдены или ещё не добавлены."
    }

    fun usersClearFilters(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Filtrni tozalash"
        AppLanguage.RU -> "Сбросить фильтры"
    }

    fun usersShowing(lang: AppLanguage, from: Int, to: Int, total: Int) = when (lang) {
        AppLanguage.UZ -> "$from dan $to gacha ko'rsatilmoqda (jami $total)"
        AppLanguage.RU -> "Показано с $from по $to (всего $total)"
    }

    fun usersPageSize(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Sahifa hajmi"
        AppLanguage.RU -> "Размер страницы"
    }

    fun usersStatTotal(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Jami foydalanuvchilar"
        AppLanguage.RU -> "Всего пользователей"
    }

    fun usersStatTotalSub(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Tizimda ro'yxatdan o'tgan"
        AppLanguage.RU -> "Зарегистрировано в системе"
    }

    fun usersStatAdmins(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Administratorlar"
        AppLanguage.RU -> "Администраторы"
    }

    fun usersStatAdminsSub(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Admin va super admin"
        AppLanguage.RU -> "Админ и супер админ"
    }

    fun usersStatWarehouse(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Ombor mudirlari"
        AppLanguage.RU -> "Заведующие складами"
    }

    fun usersStatWarehouseSub(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Warehouse roli"
        AppLanguage.RU -> "Роль warehouse"
    }

    fun usersStatSessions(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Faol sessiyalar"
        AppLanguage.RU -> "Активные сессии"
    }

    fun usersStatSessionsSub(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Hozir tizimda faol"
        AppLanguage.RU -> "Сейчас в системе"
    }

    fun blockDialogTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Foydalanuvchini bloklash"
        AppLanguage.RU -> "Заблокировать пользователя"
    }

    fun blockDialogConfirm(lang: AppLanguage, name: String) = when (lang) {
        AppLanguage.UZ -> "Haqiqatan ham $name ni bloklamoqchimisiz? Bloklangandan so'ng foydalanuvchi tizimga kira olmaydi."
        AppLanguage.RU -> "Вы действительно хотите заблокировать $name? После блокировки пользователь не сможет войти в систему."
    }

    fun blockDialogReasonLabel(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Bloklash sababi"
        AppLanguage.RU -> "Причина блокировки"
    }

    fun blockDialogReasonPlaceholder(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Bloklash sababini yozing..."
        AppLanguage.RU -> "Введите причину блокировки..."
    }

    fun blockDialogReasonHint(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Ushbu sabab foydalanuvchi profilida va audit logda saqlanadi."
        AppLanguage.RU -> "Эта причина сохранится в профиле пользователя и в журнале аудита."
    }

    fun blockDialogSubmit(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Bloklash"
        AppLanguage.RU -> "Заблокировать"
    }

    fun areYouSurePostfix(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Haqiqatan ham"
        AppLanguage.RU -> "Вы действительно хотите"
    }

    fun usersBlockedMessage(lang: AppLanguage, name: String) = when (lang) {
        AppLanguage.UZ -> "$name bloklandi"
        AppLanguage.RU -> "$name заблокирован"
    }

    fun usersUnblockedMessage(lang: AppLanguage, name: String) = when (lang) {
        AppLanguage.UZ -> "$name blokdan chiqarildi"
        AppLanguage.RU -> "$name разблокирован"
    }

    fun usersRoleUpdatedMessage(lang: AppLanguage, name: String) = when (lang) {
        AppLanguage.UZ -> "${name} roli yangilandi"
        AppLanguage.RU -> "Роль $name обновлена"
    }

    fun usersErrorGeneric(lang: AppLanguage, detail: String) = when (lang) {
        AppLanguage.UZ -> "Xatolik: $detail"
        AppLanguage.RU -> "Ошибка: $detail"
    }

    fun unblockDialogTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Foydalanuvchini blokdan chiqarish"
        AppLanguage.RU -> "Разблокировать пользователя"
    }

    fun unblockDialogConfirm(lang: AppLanguage, name: String) = when (lang) {
        AppLanguage.UZ -> "$name ni blokdan chiqarmoqchimisiz? Foydalanuvchi tizimga qayta kira oladi."
        AppLanguage.RU -> "Разблокировать $name? Пользователь снова сможет войти в систему."
    }

    fun unblockDialogSubmit(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Blokdan chiqarish"
        AppLanguage.RU -> "Разблокировать"
    }

    fun selfBlockWarning(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "O'z akkauntingizni bloklay olmaysiz."
        AppLanguage.RU -> "Вы не можете заблокировать свой аккаунт."
    }

    fun superAdminProtectedWarning(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Super adminlar bloklanishi yoki o'chirilishi mumkin emas."
        AppLanguage.RU -> "Супер-админов нельзя блокировать или удалять."
    }

    fun usersDetailTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Foydalanuvchi ma'lumoti"
        AppLanguage.RU -> "Данные пользователя"
    }

    fun usersDetailEmail(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Email"
        AppLanguage.RU -> "Email"
    }

    fun usersDetailId(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "ID"
        AppLanguage.RU -> "ID"
    }

    fun usersDetailRoles(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Rollar"
        AppLanguage.RU -> "Роли"
    }

    fun usersDetailStatus(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Holat"
        AppLanguage.RU -> "Статус"
    }

    fun usersDetailVerified(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Tasdiqlangan"
        AppLanguage.RU -> "Подтверждён"
    }

    fun usersDetailProvider(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Manba"
        AppLanguage.RU -> "Источник"
    }

    fun usersDetailCreatedAt(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Ro'yxatdan o'tgan"
        AppLanguage.RU -> "Зарегистрирован"
    }

    fun usersDetailLastActive(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "So'nggi faollik"
        AppLanguage.RU -> "Последняя активность"
    }

    fun usersDetailBlockReason(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Bloklash sababi"
        AppLanguage.RU -> "Причина блокировки"
    }

    fun usersDetailBlockedAt(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Bloklangan vaqti"
        AppLanguage.RU -> "Время блокировки"
    }

    fun usersDetailClose(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Yopish"
        AppLanguage.RU -> "Закрыть"
    }

    fun usersNotAvailable(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Mavjud emas"
        AppLanguage.RU -> "Нет данных"
    }

    fun editRoleDialogTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Foydalanuvchi rolini tahrirlash"
        AppLanguage.RU -> "Изменить роль пользователя"
    }

    fun editRoleCurrentRole(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Hozirgi rol:"
        AppLanguage.RU -> "Текущая роль:"
    }

    fun editRoleNewRole(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Yangi rolni tanlang"
        AppLanguage.RU -> "Выберите новую роль"
    }

    fun editRoleLoading(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Rollar yuklanmoqda…"
        AppLanguage.RU -> "Роли загружаются…"
    }

    fun editRoleChoosePlaceholder(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Rol tanlang…"
        AppLanguage.RU -> "Выберите роль…"
    }

    fun editRolePermissionsTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Ruxsatlar ko'rinishi"
        AppLanguage.RU -> "Права доступа"
    }

    fun editRolePermissionsEmpty(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Bu rol uchun maxsus ruxsatlar belgilanmagan."
        AppLanguage.RU -> "Для этой роли права не заданы."
    }

    fun editRoleSave(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Saqlash"
        AppLanguage.RU -> "Сохранить"
    }

    fun editRoleSelfDemoteError(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "O'zingizni SUPER_ADMIN ro'lidan olib tashlay olmaysiz."
        AppLanguage.RU -> "Нельзя снять с себя роль SUPER_ADMIN."
    }

    fun addUserTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Yangi foydalanuvchi"
        AppLanguage.RU -> "Новый пользователь"
    }

    fun addUserUsername(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "USERNAME"
        AppLanguage.RU -> "USERNAME"
    }

    fun addUserUsernamePlaceholder(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Masalan: alisher01"
        AppLanguage.RU -> "Например: alisher01"
    }

    fun addUserEmailPlaceholder(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "example@mexa.uz"
        AppLanguage.RU -> "example@mexa.uz"
    }

    fun addUserRoles(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "ROLLAR"
        AppLanguage.RU -> "РОЛИ"
    }

    fun addUserActive(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Faol foydalanuvchi"
        AppLanguage.RU -> "Активный пользователь"
    }

    fun addUserActiveHint(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Yangi foydalanuvchi email orqali tasdiqlanishi kerak."
        AppLanguage.RU -> "Новый пользователь должен подтвердить email."
    }

    fun addUserUsernameMin(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Kamida 3 belgi"
        AppLanguage.RU -> "Не менее 3 символов"
    }

    fun addUserUsernameMax(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Ko'pi bilan 30 belgi"
        AppLanguage.RU -> "Не более 30 символов"
    }

    fun addUserUsernameChars(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Faqat harf, raqam va _"
        AppLanguage.RU -> "Только буквы, цифры и _"
    }

    fun addUserEmailInvalid(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Email noto'g'ri"
        AppLanguage.RU -> "Неверный email"
    }

    fun deleteUserTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "Foydalanuvchini o'chirish"
        AppLanguage.RU -> "Удалить пользователя"
    }

    fun deleteUserConfirm(lang: AppLanguage, name: String, email: String) = when (lang) {
        AppLanguage.UZ -> "«$name» (@$email) butunlay o'chiriladi. Bu amalni bekor qilib bo'lmaydi."
        AppLanguage.RU -> "«$name» (@$email) будет удалён навсегда. Это действие нельзя отменить."
    }

    fun deleteUserSubmit(lang: AppLanguage) = when (lang) {
        AppLanguage.UZ -> "O'chirish"
        AppLanguage.RU -> "Удалить"
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
