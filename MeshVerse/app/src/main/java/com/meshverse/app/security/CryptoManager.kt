package com.meshverse.app.security

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM encryption/decryption for mesh messages.
 * Session keys are derived from ECDH shared secrets using HKDF.
 * Provides forward secrecy when combined with ephemeral key pairs.
 */
class CryptoManager(private val keyManager: KeyManager) {

    companion object {
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128
        private const val AES_KEY_LENGTH = 32
    }

    /**
     * Encrypt plaintext for a peer using their public key.
     * Returns Base64-encoded (IV + ciphertext).
     */
    fun encrypt(plaintext: String, peerPublicKeyBase64: String): String {
        val sharedSecret = keyManager.computeSharedSecret(peerPublicKeyBase64)
        val aesKey = deriveAesKey(sharedSecret)

        val iv = ByteArray(GCM_IV_LENGTH).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(aesKey, "AES"), GCMParameterSpec(GCM_TAG_LENGTH, iv))

        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val combined = iv + ciphertext
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    /**
     * Decrypt ciphertext sent by a peer using our private key.
     */
    fun decrypt(ciphertextBase64: String, senderPublicKeyBase64: String): String {
        val sharedSecret = keyManager.computeSharedSecret(senderPublicKeyBase64)
        val aesKey = deriveAesKey(sharedSecret)

        val combined = Base64.decode(ciphertextBase64, Base64.NO_WRAP)
        val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
        val ciphertext = combined.copyOfRange(GCM_IV_LENGTH, combined.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(aesKey, "AES"), GCMParameterSpec(GCM_TAG_LENGTH, iv))

        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    /** Derive AES-256 key from shared secret using SHA-256 HKDF-like KDF */
    private fun deriveAesKey(sharedSecret: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(sharedSecret).copyOf(AES_KEY_LENGTH)
    }

    /** Sign data with our identity private key (Ed25519 via EC) */
    fun sign(data: ByteArray): ByteArray {
        val kp = keyManager.getOrCreateIdentityKeyPair()
        val sig = java.security.Signature.getInstance("SHA256withECDSA")
        sig.initSign(kp.private)
        sig.update(data)
        return sig.sign()
    }

    /** Verify a signature from a peer */
    fun verify(data: ByteArray, signature: ByteArray, peerPublicKeyBase64: String): Boolean {
        return try {
            val peerKeyBytes = Base64.decode(peerPublicKeyBase64, Base64.NO_WRAP)
            val keyFactory = java.security.KeyFactory.getInstance("EC")
            val peerPublicKey = keyFactory.generatePublic(
                java.security.spec.X509EncodedKeySpec(peerKeyBytes)
            )
            val sig = java.security.Signature.getInstance("SHA256withECDSA")
            sig.initVerify(peerPublicKey)
            sig.update(data)
            sig.verify(signature)
        } catch (e: Exception) {
            false
        }
    }
}
