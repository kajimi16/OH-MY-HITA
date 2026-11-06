package com.stupidtree.hita.ui.ai

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** 用 Android Keystore 加密 API Key；不把明文写进 SharedPreferences。 */
internal object SecretStore {
    private const val ALIAS = "hita_ai_key"
    private const val PREFS = "ai_plugin"
    private const val ENC = "key_enc"
    private const val IV = "key_iv"
    private const val LEGACY = "key"

    fun load(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val legacy = prefs.getString(LEGACY, null)
        if (!legacy.isNullOrBlank()) {
            save(context, legacy)
            prefs.edit().remove(LEGACY).apply()
            return legacy
        }
        val enc = prefs.getString(ENC, null) ?: return ""
        val iv = prefs.getString(IV, null) ?: return ""
        return runCatching {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                key(),
                GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)),
            )
            String(cipher.doFinal(Base64.decode(enc, Base64.NO_WRAP)))
        }.getOrDefault("")
    }

    fun save(context: Context, value: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (value.isBlank()) {
            prefs.edit().remove(ENC).remove(IV).remove(LEGACY).apply()
            return
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(value.toByteArray())
        prefs.edit()
            .putString(ENC, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .putString(IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .remove(LEGACY)
            .apply()
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return generator.generateKey()
    }
}
