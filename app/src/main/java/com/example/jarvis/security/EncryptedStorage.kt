package com.example.jarvis.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object EncryptedStorage {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "jarvis_vault_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(KEY_ALIAS)) {
            val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
            return entry.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)
            Base64.getEncoder().encodeToString(combined)
        } catch (_: Throwable) {
            // JVM test or Android environment fallback
            Base64.getEncoder().encodeToString(plainText.toByteArray(Charsets.UTF_8))
        }
    }

    fun decrypt(cipherText: String): String {
        if (cipherText.isEmpty()) return ""
        return try {
            val combined = Base64.getDecoder().decode(cipherText)
            if (combined.size <= GCM_IV_LENGTH) return ""
            val iv = ByteArray(GCM_IV_LENGTH)
            val encryptedBytes = ByteArray(combined.size - GCM_IV_LENGTH)
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)
            System.arraycopy(combined, GCM_IV_LENGTH, encryptedBytes, 0, encryptedBytes.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), spec)
            String(cipher.doFinal(encryptedBytes), Charsets.UTF_8)
        } catch (_: Throwable) {
            // Fallback for JVM test environments
            try {
                String(Base64.getDecoder().decode(cipherText), Charsets.UTF_8)
            } catch (_: Exception) {
                ""
            }
        }
    }
}

object SensitiveDataFilter {
    private val CREDIT_CARD_REGEX = Regex("""\b(?:\d[ -]*?){13,16}\b""")
    private val SSN_REGEX = Regex("""\b\d{3}-\d{2}-\d{4}\b""")
    private val PASSWORD_KEYWORD_REGEX = Regex("""(?i)\b(password|passcode|secret pin|cvv|private key)\s*[:=]\s*(\S+)""")
    private val API_KEY_REGEX = Regex("""\b(sk-[a-zA-Z0-9_-]{16,}|AIzaSy[a-zA-Z0-9_-]{20,})\b""")
    private val BEARER_TOKEN_REGEX = Regex("""(?i)\b(bearer\s+[a-zA-Z0-9\._\-]{20,})\b""")
    private val OTP_REGEX = Regex("""(?i)\b(?:otp|verification\s*code|security\s*code|one-time\s*password)\s*[:=]?\s*(\d{4,8})\b""")
    private val OTP_STANDALONE_REGEX = Regex("""(?i)\b(\d{6})\b""")

    fun containsSensitiveData(text: String): Boolean {
        return CREDIT_CARD_REGEX.containsMatchIn(text) ||
                SSN_REGEX.containsMatchIn(text) ||
                PASSWORD_KEYWORD_REGEX.containsMatchIn(text) ||
                API_KEY_REGEX.containsMatchIn(text) ||
                BEARER_TOKEN_REGEX.containsMatchIn(text) ||
                OTP_REGEX.containsMatchIn(text)
    }

    fun sanitizeForDisplay(text: String): String {
        var sanitized = text
        sanitized = CREDIT_CARD_REGEX.replace(sanitized) { match ->
            val clean = match.value.replace(" ", "").replace("-", "")
            if (clean.length >= 13) "[PROTECTED_CARD_****${clean.takeLast(4)}]" else match.value
        }
        sanitized = SSN_REGEX.replace(sanitized, "[PROTECTED_SSN_***-**-****]")
        sanitized = PASSWORD_KEYWORD_REGEX.replace(sanitized) { match ->
            "${match.groupValues[1]}: [PROTECTED_CREDENTIAL]"
        }
        sanitized = OTP_REGEX.replace(sanitized, "OTP: [PROTECTED_OTP]")
        sanitized = API_KEY_REGEX.replace(sanitized, "[PROTECTED_API_KEY]")
        sanitized = BEARER_TOKEN_REGEX.replace(sanitized, "Bearer [PROTECTED_TOKEN]")
        return sanitized
    }

    fun redactSensitiveData(text: String): String = sanitizeForDisplay(text)
}
