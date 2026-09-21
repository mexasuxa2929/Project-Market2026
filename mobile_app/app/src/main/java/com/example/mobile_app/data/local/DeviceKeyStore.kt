package com.example.mobile_app.data.local

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Refresh tokenni qurilmaga bog'lash (device-binding) uchun apparat-qo'llab-
 * quvvatlanadigan Android Keystore kaliti.
 *
 * - Kalit Android Keystore ichida saqlanadi va tashqariga chiqarib bo'lmaydi
 *   (non-extractable).
 * - Imamkon bo'lsa StrongBox (hardware-backed) ishlatiladi.
 * - Shuning uchun refresh token shifri faqat shu qurilmada ochiladi; faylni
 *   boshqa qurilmaga ko'chirib bo'lmaydi.
 */
object DeviceKeyStore {
    private const val KEY_ALIAS = "mexa_refresh_token_key"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION =
        "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_GCM}/${KeyProperties.ENCRYPTION_PADDING_NONE}"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    }

    private fun getOrCreateKey(): SecretKey {
        keyStore.getKey(KEY_ALIAS, null)?.let { return it as SecretKey }
        return createKey()
    }

    private fun createKey(): SecretKey {
        val builder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)

        // Avval StrongBox (apparat) urinib ko'ramiz, qo'llab-quvvatlanmasa oddiy Keystore
        return try {
            generator.init(builder.setIsStrongBoxBacked(true).build())
            generator.generateKey()
        } catch (e: Exception) {
            generator.init(builder.setIsStrongBoxBacked(false).build())
            generator.generateKey()
        }
    }

    /** Ochiq matnni (refresh token) shifrlab, Base64 satr qaytaradi. */
    fun encrypt(plaintext: String?): String? {
        if (plaintext.isNullOrEmpty()) return null
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            val iv = cipher.iv
            val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            val out = ByteArray(iv.size + encrypted.size)
            System.arraycopy(iv, 0, out, 0, iv.size)
            System.arraycopy(encrypted, 0, out, iv.size, encrypted.size)
            Base64.encodeToString(out, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    /** Base64 shifrni ochib, ochiq matn (refresh token) qaytaradi.
     *  Boshqa qurilma / kalit yo'q bo'lsa null qaytaradi. */
    fun decrypt(ciphertext: String?): String? {
        if (ciphertext.isNullOrEmpty()) return null
        return try {
            val data = Base64.decode(ciphertext, Base64.NO_WRAP)
            val iv = data.copyOfRange(0, GCM_IV_LENGTH)
            val encrypted = data.copyOfRange(GCM_IV_LENGTH, data.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_LENGTH, iv))
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }
}
