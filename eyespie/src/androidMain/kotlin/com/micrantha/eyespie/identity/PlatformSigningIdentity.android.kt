package com.micrantha.eyespie.identity

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.math.BigInteger
import java.security.AlgorithmParameters
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec

actual class PlatformSigningIdentity actual constructor() : SigningIdentity {
    private val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    override suspend fun publicKey(): ByteArray = canonicalPublicKey(loadOrCreate().public as ECPublicKey)

    override suspend fun sign(payload: ByteArray): ByteArray {
        val signer = Signature.getInstance(SIGNATURE_ALGORITHM)
        signer.initSign(loadOrCreate().private)
        signer.update(payload)
        return signer.sign()
    }

    override suspend fun verify(
        publicKey: ByteArray,
        payload: ByteArray,
        signature: ByteArray,
    ): Boolean = runCatching {
        val verifier = Signature.getInstance(SIGNATURE_ALGORITHM)
        verifier.initVerify(decodePublicKey(publicKey))
        verifier.update(payload)
        verifier.verify(signature)
    }.getOrDefault(false)

    private fun loadOrCreate(): KeyPair {
        val existing = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.PrivateKeyEntry
        if (existing != null) {
            return KeyPair(existing.certificate.publicKey, existing.privateKey)
        }

        val generator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC,
            ANDROID_KEYSTORE,
        )
        generator.initialize(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY,
            )
                .setAlgorithmParameterSpec(ECGenParameterSpec(CURVE_NAME))
                .setDigests(KeyProperties.DIGEST_SHA256)
                .build(),
        )
        return generator.generateKeyPair()
    }

    private fun canonicalPublicKey(publicKey: ECPublicKey): ByteArray = byteArrayOf(
        P256_X963_UNCOMPRESSED_PREFIX,
    ) + publicKey.w.affineX.toFixedUnsigned(P256_COORDINATE_SIZE) +
        publicKey.w.affineY.toFixedUnsigned(P256_COORDINATE_SIZE)

    private fun decodePublicKey(encoded: ByteArray): ECPublicKey {
        require(encoded.size == P256_X963_PUBLIC_KEY_SIZE)
        require(encoded.first() == P256_X963_UNCOMPRESSED_PREFIX)

        val x = BigInteger(1, encoded.copyOfRange(1, 1 + P256_COORDINATE_SIZE))
        val y = BigInteger(1, encoded.copyOfRange(1 + P256_COORDINATE_SIZE, encoded.size))
        val parameters = AlgorithmParameters.getInstance(KeyProperties.KEY_ALGORITHM_EC).apply {
            init(ECGenParameterSpec(CURVE_NAME))
        }.getParameterSpec(ECParameterSpec::class.java)

        return KeyFactory.getInstance(KeyProperties.KEY_ALGORITHM_EC)
            .generatePublic(ECPublicKeySpec(ECPoint(x, y), parameters)) as ECPublicKey
    }

    private fun BigInteger.toFixedUnsigned(size: Int): ByteArray {
        val bytes = toByteArray()
        val unsigned = if (bytes.size > size && bytes.first() == 0.toByte()) {
            bytes.copyOfRange(1, bytes.size)
        } else {
            bytes
        }
        require(unsigned.size <= size)
        return ByteArray(size - unsigned.size) + unsigned
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "com.micrantha.eyespie.local-identity.p256"
        const val CURVE_NAME = "secp256r1"
        const val SIGNATURE_ALGORITHM = "SHA256withECDSA"
        const val P256_COORDINATE_SIZE = 32
    }
}
