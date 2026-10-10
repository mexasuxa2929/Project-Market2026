package mexa.club.desktop_app.auth

import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import java.security.SecureRandom
import java.util.prefs.Preferences
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * S-49: sessiya tokenlari plaintext saqlanmaydi.
 *
 * - Kalit: `~/.mexa/.sk` (tasodifiy 256-bit, fayl iga faqat egasi o'qiy oladi)
 * - Payload: `~/.mexa/.sd` — AES-256-GCM (IV payload ichida, tag 128-bit)
 * - Eski plaintext [Preferences] kalitlari bir marta shifrlanadi va o'chiriladi
 *
 * API o'zgarmagan: [save]/[loadAccess]/[loadRefresh]/[expiresAtMs]/[isAccessValid]/[clear].
 */
object TokenStore {

    /** Eski plaintext kalitlar (migratsiya uchun). */
    private const val PREF_ACCESS = "auth_access_token"
    private const val PREF_REFRESH = "auth_refresh_token"
    private const val PREF_EXPIRES = "auth_expires_at_ms"

    private const val ACCESS_REFRESH_SKEW_MS = 5 * 60_000L
    private const val GCM_TAG_BITS = 128
    private const val IV_BYTES = 12
    private const val KEY_BYTES = 32

    private val prefs: Preferences = Preferences.userNodeForPackage(TokenStore::class.java)
    private val lock = Any()
    private val random = SecureRandom()

    private val dir: File = File(System.getProperty("user.home"), ".mexa")
    private val keyFile: File = File(dir, ".sk")
    private val dataFile: File = File(dir, ".sd")
    private var migrated = false

    fun save(accessToken: String, refreshToken: String, expiresInSeconds: Long) = synchronized(lock) {
        migrateOnce()
        val expiresAt = System.currentTimeMillis() + expiresInSeconds * 1000L
        writeSecure(dataFile, encrypt("$expiresAt\n$accessToken\n$refreshToken".toByteArray(Charsets.UTF_8)))
    }

    fun loadAccess(): String? = synchronized(lock) {
        migrateOnce()
        decryptToLines()?.getOrNull(1)
    }

    fun loadRefresh(): String? = synchronized(lock) {
        migrateOnce()
        decryptToLines()?.getOrNull(2)
    }

    fun expiresAtMs(): Long = synchronized(lock) {
        migrateOnce()
        decryptToLines()?.getOrNull(0)?.toLongOrNull() ?: 0L
    }

    /**
     * `true` — access token hali ishlatish mumkin (tugashidan oldin zaxira oynasi).
     * `false` bo‘lsa [TokenRefreshManager] va [AuthApiClient.ensureFreshAccessToken] refresh chaqiradi.
     */
    fun isAccessValid(): Boolean {
        val access = loadAccess() ?: return false
        if (access.isBlank()) return false
        val exp = expiresAtMs()
        if (exp <= 0L) return false
        return System.currentTimeMillis() < exp - ACCESS_REFRESH_SKEW_MS
    }

    fun clear() = synchronized(lock) {
        migrateOnce()
        if (dataFile.exists()) dataFile.delete()
        Unit
    }

    // ── Migratsiya: eski plaintext Preferences → shifrlangan fayl ──────────────

    private fun migrateOnce() {
        if (migrated) return
        migrated = true
        val oldAccess = prefs.get(PREF_ACCESS, null)
        if (oldAccess.isNullOrBlank()) return
        try {
            val oldRefresh = prefs.get(PREF_REFRESH, "")
            val oldExpires = prefs.getLong(PREF_EXPIRES, 0L)
            val expiresAt = oldExpires.coerceAtLeast(System.currentTimeMillis())
            writeSecure(dataFile, encrypt("$expiresAt\n$oldAccess\n$oldRefresh".toByteArray(Charsets.UTF_8)))
        } catch (_: Exception) {
            // Migratsiya chiqsa ham plaintextni o'chiramiz (token yangilanib qayta yoziladi)
        }
        prefs.remove(PREF_ACCESS)
        prefs.remove(PREF_REFRESH)
        prefs.remove(PREF_EXPIRES)
        prefs.flush()
    }

    // ── Shifrlash ─────────────────────────────────────────────────────────────

    private fun encrypt(plain: ByteArray): ByteArray {
        val iv = ByteArray(IV_BYTES).also { random.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, loadOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        val ciphertext = cipher.doFinal(plain)
        return iv + ciphertext
    }

    private fun decrypt(blob: ByteArray): ByteArray? {
        if (blob.size <= IV_BYTES) return null
        return runCatching {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, loadOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, blob, 0, IV_BYTES))
            cipher.doFinal(blob, IV_BYTES, blob.size - IV_BYTES)
        }.getOrNull()
    }

    private fun decryptToLines(): List<String>? =
        runCatching {
            if (!dataFile.exists()) return null
            val blob = dataFile.readBytes()
            val plain = decrypt(blob) ?: return null
            String(plain, Charsets.UTF_8).split("\n", limit = 3)
        }.getOrNull()

    private fun loadOrCreateKey(): SecretKeySpec = synchronized(lock) {
        ensureDir()
        if (!keyFile.exists()) {
            val key = ByteArray(KEY_BYTES).also { random.nextBytes(it) }
            writeSecure(keyFile, key)
        }
        SecretKeySpec(keyFile.readBytes(), "AES")
    }

    // ── Fayl yozish (atomik + ruxsatlar) ──────────────────────────────────────

    private fun writeSecure(file: File, bytes: ByteArray) {
        ensureDir()
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeBytes(bytes)
        restrict(tmp)
        if (file.exists()) file.delete()
        if (!tmp.renameTo(file)) {
            tmp.copyTo(file, overwrite = true)
            tmp.delete()
        }
        restrict(file)
    }

    private fun ensureDir() {
        if (!dir.exists()) dir.mkdirs()
        restrictDir(dir)
    }

    private fun restrict(file: File) {
        try {
            Files.setPosixFilePermissions(file.toPath(), PosixFilePermissions.fromString("rw-------"))
        } catch (_: UnsupportedOperationException) {
            file.setReadable(false, false)
            file.setReadable(true, true)
            file.setWritable(false, false)
            file.setWritable(true, true)
            file.setExecutable(false, false)
        } catch (_: Exception) {
        }
    }

    private fun restrictDir(file: File) {
        try {
            Files.setPosixFilePermissions(file.toPath(), PosixFilePermissions.fromString("rwx------"))
        } catch (_: Exception) {
        }
    }
}
