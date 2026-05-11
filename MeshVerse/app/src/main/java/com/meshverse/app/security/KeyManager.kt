package com.meshverse.app.security

import android.util.Base64
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import javax.crypto.KeyAgreement

/**
 * Manages Curve25519/ECDH key pairs for end-to-end encryption.
 * In production this would integrate with the Signal Protocol library.
 * Here we use standard JCA with EC P-256 as a practical approximation.
 */
class KeyManager {

    private var identityKeyPair: KeyPair? = null
    private var lastRotationAt: Long = 0L

    fun generateIdentityKeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(ECGenParameterSpec("secp256r1"), SecureRandom())
        val kp = kpg.generateKeyPair()
        identityKeyPair = kp
        return kp
    }

    fun getOrCreateIdentityKeyPair(): KeyPair {
        return identityKeyPair ?: generateIdentityKeyPair()
    }

    fun rotateIdentityKeyPair(): KeyPair {
        lastRotationAt = System.currentTimeMillis()
        return generateIdentityKeyPair()
    }

    fun getLastRotationAt(): Long = lastRotationAt

    fun getPublicKeyBase64(): String {
        val kp = getOrCreateIdentityKeyPair()
        return Base64.encodeToString(kp.public.encoded, Base64.NO_WRAP)
    }

    /**
     * Compute shared secret from our private key and peer's public key.
     * This is the base for deriving the AES-GCM session key.
     */
    fun computeSharedSecret(peerPublicKeyBase64: String): ByteArray {
        val kp = getOrCreateIdentityKeyPair()
        val peerKeyBytes = Base64.decode(peerPublicKeyBase64, Base64.NO_WRAP)
        val keyFactory = java.security.KeyFactory.getInstance("EC")
        val peerPublicKey = keyFactory.generatePublic(
            java.security.spec.X509EncodedKeySpec(peerKeyBytes)
        )
        val ka = KeyAgreement.getInstance("ECDH")
        ka.init(kp.private)
        ka.doPhase(peerPublicKey, true)
        return ka.generateSecret()
    }

    fun generateDeviceFingerprint(publicKeyBase64: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(Base64.decode(publicKeyBase64, Base64.NO_WRAP))
        return hash.take(8).joinToString(":") { "%02X".format(it) }
    }
}
