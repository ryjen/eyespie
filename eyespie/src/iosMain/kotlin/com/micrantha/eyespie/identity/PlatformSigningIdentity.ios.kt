@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.micrantha.eyespie.identity

import kotlinx.cinterop.COpaquePointerVar
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.set
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFDataRef
import platform.CoreFoundation.CFDictionaryCreate
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFErrorRefVar
import platform.CoreFoundation.CFNumberCreate
import platform.CoreFoundation.CFNumberRef
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFNumberIntType
import platform.Security.SecItemCopyMatching
import platform.Security.SecKeyCopyExternalRepresentation
import platform.Security.SecKeyCopyPublicKey
import platform.Security.SecKeyCreateRandomKey
import platform.Security.SecKeyCreateSignature
import platform.Security.SecKeyCreateWithData
import platform.Security.SecKeyRef
import platform.Security.SecKeyVerifySignature
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrApplicationTag
import platform.Security.kSecAttrIsPermanent
import platform.Security.kSecAttrKeyClass
import platform.Security.kSecAttrKeyClassPublic
import platform.Security.kSecAttrKeySizeInBits
import platform.Security.kSecAttrKeyType
import platform.Security.kSecAttrKeyTypeECSECPrimeRandom
import platform.Security.kSecClass
import platform.Security.kSecClassKey
import platform.Security.kSecKeyAlgorithmECDSASignatureMessageX962SHA256
import platform.Security.kSecPrivateKeyAttrs
import platform.Security.kSecReturnRef

actual class PlatformSigningIdentity actual constructor() : SigningIdentity {
    override suspend fun publicKey(): ByteArray {
        val privateKey = loadOrCreatePrivateKey()
        val publicKey = SecKeyCopyPublicKey(privateKey)
            ?: error("Unable to copy Eyespie local identity public key")
        val data = memScoped {
            val error = alloc<CFErrorRefVar>()
            error.value = null
            SecKeyCopyExternalRepresentation(publicKey, error.ptr)
                ?: error("Unable to export Eyespie local identity public key")
        }
        return data.toByteArray().also { encoded ->
            require(encoded.size == P256_X963_PUBLIC_KEY_SIZE)
            require(encoded.first() == P256_X963_UNCOMPRESSED_PREFIX)
        }
    }

    override suspend fun sign(payload: ByteArray): ByteArray {
        val privateKey = loadOrCreatePrivateKey()
        val payloadData = payload.toCFData()
        val signature = memScoped {
            val error = alloc<CFErrorRefVar>()
            error.value = null
            SecKeyCreateSignature(
                privateKey,
                kSecKeyAlgorithmECDSASignatureMessageX962SHA256,
                payloadData,
                error.ptr,
            ) ?: error("Unable to sign with Eyespie local identity")
        }
        return signature.toByteArray()
    }

    override suspend fun verify(
        publicKey: ByteArray,
        payload: ByteArray,
        signature: ByteArray,
    ): Boolean = runCatching {
        require(publicKey.size == P256_X963_PUBLIC_KEY_SIZE)
        require(publicKey.first() == P256_X963_UNCOMPRESSED_PREFIX)

        val key = createPublicKey(publicKey)
        memScoped {
            val error = alloc<CFErrorRefVar>()
            error.value = null
            SecKeyVerifySignature(
                key,
                kSecKeyAlgorithmECDSASignatureMessageX962SHA256,
                payload.toCFData(),
                signature.toCFData(),
                error.ptr,
            )
        }
    }.getOrDefault(false)

    private fun loadOrCreatePrivateKey(): SecKeyRef {
        val tag = KEY_TAG.encodeToByteArray().toCFData()
        val query = cfDictionaryOf(
            kSecClass to kSecClassKey,
            kSecAttrKeyType to kSecAttrKeyTypeECSECPrimeRandom,
            kSecAttrApplicationTag to tag,
            kSecReturnRef to kCFBooleanTrue,
        )

        memScoped {
            val result = alloc<COpaquePointerVar>()
            val status = SecItemCopyMatching(query, result.ptr.reinterpret())
            if (status == errSecSuccess) {
                return result.value?.reinterpret()
                    ?: error("Eyespie local identity key reference was empty")
            }
            check(status == errSecItemNotFound) {
                "Unable to read Eyespie local identity from Keychain: $status"
            }
        }

        val privateAttributes = cfDictionaryOf(
            kSecAttrIsPermanent to kCFBooleanTrue,
            kSecAttrApplicationTag to tag,
        )
        val attributes = cfDictionaryOf(
            kSecAttrKeyType to kSecAttrKeyTypeECSECPrimeRandom,
            kSecAttrKeySizeInBits to cfNumber(P256_KEY_SIZE_BITS),
            kSecPrivateKeyAttrs to privateAttributes,
        )

        return memScoped {
            val error = alloc<CFErrorRefVar>()
            error.value = null
            SecKeyCreateRandomKey(attributes, error.ptr)
                ?: error("Unable to create Eyespie local identity key")
        }
    }

    private fun createPublicKey(encoded: ByteArray): SecKeyRef {
        val attributes = cfDictionaryOf(
            kSecAttrKeyType to kSecAttrKeyTypeECSECPrimeRandom,
            kSecAttrKeyClass to kSecAttrKeyClassPublic,
            kSecAttrKeySizeInBits to cfNumber(P256_KEY_SIZE_BITS),
        )
        return memScoped {
            val error = alloc<CFErrorRefVar>()
            error.value = null
            SecKeyCreateWithData(encoded.toCFData(), attributes, error.ptr)
                ?: error("Unable to import Eyespie P-256 public key")
        }
    }

    private companion object {
        const val KEY_TAG = "com.micrantha.eyespie.local-identity.p256"
        const val P256_KEY_SIZE_BITS = 256
    }
}

private fun ByteArray.toCFData(): CFDataRef = usePinned { pinned ->
    CFDataCreate(
        kCFAllocatorDefault,
        pinned.addressOf(0).reinterpret(),
        size.convert(),
    ) ?: error("Unable to allocate CFData")
}

private fun CFDataRef.toByteArray(): ByteArray {
    val length = CFDataGetLength(this).toInt()
    val bytes = CFDataGetBytePtr(this) ?: return ByteArray(0)
    return ByteArray(length) { index -> bytes[index].toByte() }
}

private fun cfNumber(value: Int): CFNumberRef = memScoped {
    val holder = alloc<IntVar>()
    holder.value = value
    CFNumberCreate(kCFAllocatorDefault, kCFNumberIntType, holder.ptr)
        ?: error("Unable to allocate CFNumber")
}

private fun cfDictionaryOf(vararg entries: Pair<CFTypeRef?, CFTypeRef?>): CFDictionaryRef = memScoped {
    val keys = allocArray<COpaquePointerVar>(entries.size)
    val values = allocArray<COpaquePointerVar>(entries.size)
    entries.forEachIndexed { index, (key, value) ->
        keys[index] = key?.reinterpret()
        values[index] = value?.reinterpret()
    }
    CFDictionaryCreate(
        kCFAllocatorDefault,
        keys,
        values,
        entries.size.convert(),
        null,
        null,
    ) ?: error("Unable to allocate CFDictionary")
}
