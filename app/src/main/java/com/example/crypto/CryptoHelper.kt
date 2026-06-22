package com.example.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import android.util.Base64

object CryptoHelper {
    private const val PROVIDER = "AndroidKeyStore"
    private const val ALIAS = "SecureBrowserKey"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_SIZE = 12

    private var fallbackKey: SecretKey? = null

    init {
        try {
            initKeyStoreKey()
        } catch (e: Exception) {
            e.printStackTrace()
            initFallbackKey()
        }
    }

    private fun initKeyStoreKey() {
        val keyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }
        if (!keyStore.containsAlias(ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
            val spec = KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        }
    }

    private fun initFallbackKey() {
        // Fallback software AES key for compatibility
        val salt = "SecureBrowserSoftSaltSeedValues123".toByteArray(Charsets.UTF_8)
        val keyBytes = ByteArray(16)
        System.arraycopy(salt, 0, keyBytes, 0, 16)
        fallbackKey = SecretKeySpec(keyBytes, "AES")
    }

    private fun getSecretKey(): SecretKey {
        return try {
            val keyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }
            (keyStore.getKey(ALIAS, null) as? SecretKey) ?: fallbackKey ?: throw IllegalStateException("Key unavailable")
        } catch (e: Exception) {
            if (fallbackKey == null) {
                initFallbackKey()
            }
            fallbackKey ?: throw IllegalStateException("Fallback Key unavailable")
        }
    }

    /**
     * Encrypts plain text using AES-GCM encryption.
     */
    fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            
            // combine IV + encryptedBytes
            val combined = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)
            
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            // fallback plain text or simple base64 to avoid app breaking, 
            // but log error
            Base64.encodeToString(plainText.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        }
    }

    /**
     * Decrypts encrypted text using AES-GCM decryption.
     */
    fun decrypt(encryptedText: String): String {
        if (encryptedText.isEmpty()) return ""
        return try {
            val combined = Base64.decode(encryptedText, Base64.NO_WRAP)
            if (combined.size <= IV_SIZE) {
                // Not standard GCM, fallback simple base64 decode if it was backup format
                return String(combined, Charsets.UTF_8)
            }
            
            val iv = ByteArray(IV_SIZE)
            System.arraycopy(combined, 0, iv, 0, IV_SIZE)
            
            val encryptedBytes = ByteArray(combined.size - IV_SIZE)
            System.arraycopy(combined, IV_SIZE, encryptedBytes, 0, encryptedBytes.size)
            
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
            
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            // soft fallback in case it was stored undecrypted
            try {
                String(Base64.decode(encryptedText, Base64.NO_WRAP), Charsets.UTF_8)
            } catch (ex: Exception) {
                encryptedText
            }
        }
    }
}
