package com.example.securenote.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import javax.crypto.Cipher

/**
 * Owns the master password lifecycle and the raw 32-byte DB key used by SQLCipher.
 *
 * Storage layout (in EncryptedSharedPreferences, itself protected by Android Keystore):
 *   salt                 -- 16 bytes, base64
 *   verifier_hash        -- 32 bytes, base64 (SHA-256 of the derived key + a fixed constant)
 *   iterations           -- int (for future migrations)
 *   biometric_wrapped    -- base64, present only if biometric unlock is enabled
 *   biometric_iv         -- base64, IV for the wrapped blob
 */
class VaultKeyManager(context: Context) {

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context.applicationContext,
        PREFS_NAME,
        MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val biometricKey = KeystoreCipher(BIOMETRIC_KEY_ALIAS)

    val isInitialized: Boolean
        get() = prefs.contains(KEY_SALT) && prefs.contains(KEY_VERIFIER)

    val isBiometricEnabled: Boolean
        get() = prefs.contains(KEY_BIO_WRAPPED) && prefs.contains(KEY_BIO_IV)

    /** Sets up the vault for the first time. Returns the derived raw key (32 bytes). */
    fun initializeVault(password: CharArray): ByteArray {
        val salt = CryptoUtils.randomBytes(CryptoUtils.SALT_BYTES)
        val key = CryptoUtils.deriveKey(password, salt)
        val verifier = verifierFor(key)
        prefs.edit()
            .putString(KEY_SALT, CryptoUtils.encodeBase64(salt))
            .putString(KEY_VERIFIER, CryptoUtils.encodeBase64(verifier))
            .putInt(KEY_ITERATIONS, CryptoUtils.PBKDF2_ITERATIONS)
            .remove(KEY_BIO_WRAPPED)
            .remove(KEY_BIO_IV)
            .apply()
        return key
    }

    /** Returns the raw key if [password] is correct, or null. */
    fun unlockWithPassword(password: CharArray): ByteArray? {
        val salt = prefs.getString(KEY_SALT, null)?.let(CryptoUtils::decodeBase64) ?: return null
        val storedVerifier = prefs.getString(KEY_VERIFIER, null)?.let(CryptoUtils::decodeBase64) ?: return null
        val iterations = prefs.getInt(KEY_ITERATIONS, CryptoUtils.PBKDF2_ITERATIONS)
        val key = CryptoUtils.deriveKey(password, salt, iterations)
        return if (CryptoUtils.constantTimeEquals(verifierFor(key), storedVerifier)) key else {
            key.fill(0); null
        }
    }

    // --- Biometric enrollment / unlock ---

    /** Provides a cipher to be authorized by BiometricPrompt for enrolling biometric unlock. */
    fun cipherForBiometricEnroll(): Cipher = biometricKey.initEncryptCipher()

    /** After successful biometric auth, wrap and persist the raw key. */
    fun enrollBiometric(rawKey: ByteArray, authorizedCipher: Cipher) {
        val wrapped = authorizedCipher.doFinal(rawKey)
        prefs.edit()
            .putString(KEY_BIO_WRAPPED, CryptoUtils.encodeBase64(wrapped))
            .putString(KEY_BIO_IV, CryptoUtils.encodeBase64(authorizedCipher.iv))
            .apply()
    }

    /** Provides a decrypt cipher to be authorized by BiometricPrompt for unlocking. */
    fun cipherForBiometricUnlock(): Cipher? {
        val iv = prefs.getString(KEY_BIO_IV, null)?.let(CryptoUtils::decodeBase64) ?: return null
        return biometricKey.initDecryptCipher(iv)
    }

    /** After successful biometric auth, return the raw key. */
    fun unlockWithBiometric(authorizedCipher: Cipher): ByteArray? {
        val wrapped = prefs.getString(KEY_BIO_WRAPPED, null)?.let(CryptoUtils::decodeBase64) ?: return null
        return authorizedCipher.doFinal(wrapped)
    }

    fun disableBiometric() {
        prefs.edit().remove(KEY_BIO_WRAPPED).remove(KEY_BIO_IV).apply()
        biometricKey.deleteKey()
    }

    fun resetVault() {
        prefs.edit().clear().apply()
        biometricKey.deleteKey()
    }

    private fun verifierFor(key: ByteArray): ByteArray {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        md.update(VERIFIER_CONSTANT)
        md.update(key)
        return md.digest()
    }

    companion object {
        private const val PREFS_NAME = "vault_meta"
        private const val KEY_SALT = "salt"
        private const val KEY_VERIFIER = "verifier"
        private const val KEY_ITERATIONS = "iterations"
        private const val KEY_BIO_WRAPPED = "bio_wrapped"
        private const val KEY_BIO_IV = "bio_iv"
        private const val BIOMETRIC_KEY_ALIAS = "secure_note_biometric_v1"
        private val VERIFIER_CONSTANT = "securenote-verifier-v1".toByteArray()
    }
}
