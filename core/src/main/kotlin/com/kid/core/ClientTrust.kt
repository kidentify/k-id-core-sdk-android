package com.kid.core

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.math.BigInteger
import java.net.URI
import java.security.GeneralSecurityException
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.ProviderException
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.UUID

/** A P-256 key that signs DPoP proofs; its private half never leaves the device. */
@KIDInternalApi
public interface DeviceKey {
    public val publicJWK: Map<String, String>

    /** Returns the raw 64-byte `r || s` signature JWS expects. */
    public fun sign(data: ByteArray): ByteArray
}

@KIDInternalApi
public fun interface DeviceKeyProvider {
    public fun key(): DeviceKey
}

/** Proves a fresh hardware key to k-ID with Android Key Attestation. */
@KIDInternalApi
public fun interface KeyAttestationProvider {
    /** The certificate chain, leaf first, of a new hardware key generated with [challenge]; null when there is none. */
    public suspend fun attestationChain(challenge: ByteArray): List<ByteArray>?
}

private const val KEYSTORE = "AndroidKeyStore"
private const val DPOP_ALIAS = "com.k-id.core.dpop"
private const val ATTESTATION_ALIAS = "com.k-id.core.attestation"

internal fun base64URL(data: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(data)

private fun unsignedFixed(value: BigInteger, size: Int): ByteArray {
    val bytes = value.toByteArray()
    return when {
        bytes.size == size -> bytes
        bytes.size > size -> bytes.copyOfRange(bytes.size - size, bytes.size)
        else -> ByteArray(size - bytes.size) + bytes
    }
}

internal fun jwk(publicKey: ECPublicKey): Map<String, String> = mapOf(
    "kty" to "EC",
    "crv" to "P-256",
    "x" to base64URL(unsignedFixed(publicKey.w.affineX, 32)),
    "y" to base64URL(unsignedFixed(publicKey.w.affineY, 32)),
)

/** Java signs ECDSA as an ASN.1 `SEQUENCE { r, s }`; JWS wants the two integers concatenated. */
internal fun derToRawSignature(der: ByteArray): ByteArray {
    var offset = 2
    if (der[1].toInt() and 0x80 != 0) offset += der[1].toInt() and 0x7f
    fun integer(): BigInteger {
        require(der[offset].toInt() == 0x02) { "not an ASN.1 integer" }
        val length = der[offset + 1].toInt()
        val value = BigInteger(1, der.copyOfRange(offset + 2, offset + 2 + length))
        offset += 2 + length
        return value
    }
    val r = integer()
    val s = integer()
    return unsignedFixed(r, 32) + unsignedFixed(s, 32)
}

internal class JcaDeviceKey(private val privateKey: PrivateKey, publicKey: ECPublicKey) : DeviceKey {
    override val publicJWK: Map<String, String> = jwk(publicKey)

    override fun sign(data: ByteArray): ByteArray {
        val signature = Signature.getInstance("SHA256withECDSA")
        signature.initSign(privateKey)
        signature.update(data)
        return derToRawSignature(signature.sign())
    }
}

/** The install's Keystore key, created once and kept; a device without a usable Keystore gets a key for this launch. */
internal object KeystoreDeviceKeyProvider : DeviceKeyProvider {
    private var cached: DeviceKey? = null

    @Synchronized
    override fun key(): DeviceKey = cached ?: load().also { cached = it }

    private fun load(): DeviceKey = try {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        val entry = keyStore.getEntry(DPOP_ALIAS, null) as? KeyStore.PrivateKeyEntry
        if (entry != null) {
            JcaDeviceKey(entry.privateKey, entry.certificate.publicKey as ECPublicKey)
        } else {
            val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, KEYSTORE)
            generator.initialize(
                KeyGenParameterSpec.Builder(DPOP_ALIAS, KeyProperties.PURPOSE_SIGN)
                    .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .build(),
            )
            val pair = generator.generateKeyPair()
            JcaDeviceKey(pair.private, pair.public as ECPublicKey)
        }
    } catch (_: GeneralSecurityException) {
        softwareKey()
    } catch (_: ProviderException) {
        softwareKey()
    }

    private fun softwareKey(): DeviceKey {
        val generator = KeyPairGenerator.getInstance("EC")
        generator.initialize(ECGenParameterSpec("secp256r1"))
        val pair = generator.generateKeyPair()
        return JcaDeviceKey(pair.private, pair.public as ECPublicKey)
    }
}

/** A throwaway Keystore key per mint: Key Attestation binds its challenge when the key is made. */
internal object KeystoreKeyAttestation : KeyAttestationProvider {
    override suspend fun attestationChain(challenge: ByteArray): List<ByteArray>? = withContext(Dispatchers.IO) {
        try {
            val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, KEYSTORE)
            generator.initialize(
                KeyGenParameterSpec.Builder(ATTESTATION_ALIAS, KeyProperties.PURPOSE_SIGN)
                    .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .setAttestationChallenge(challenge)
                    .build(),
            )
            generator.generateKeyPair()
            val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
            val chain = keyStore.getCertificateChain(ATTESTATION_ALIAS)?.map { it.encoded }
            keyStore.deleteEntry(ATTESTATION_ALIAS)
            chain
        } catch (_: GeneralSecurityException) {
            null
        } catch (_: ProviderException) {
            null
        }
    }
}

internal fun jwkThumbprint(jwk: Map<String, String>): String {
    val canonical = """{"crv":"${jwk["crv"]}","kty":"${jwk["kty"]}","x":"${jwk["x"]}","y":"${jwk["y"]}"}"""
    return base64URL(sha256(canonical.encodeToByteArray()))
}

internal fun sha256(data: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(data)

internal fun dpopProof(
    key: DeviceKey,
    method: String,
    url: String,
    now: Instant,
    nonce: String? = null,
    accessToken: String? = null,
): String {
    val uri = URI(url)
    val target = URI(uri.scheme, uri.authority, uri.path, null, null).toString()
    val header = buildJsonObject {
        put("typ", "dpop+jwt")
        put("alg", "ES256")
        put("jwk", JsonObject(key.publicJWK.mapValues { JsonPrimitive(it.value) }))
    }
    val claims = buildJsonObject {
        put("htm", method)
        put("htu", target)
        put("iat", now.epochSecond)
        put("jti", UUID.randomUUID().toString())
        nonce?.let { put("nonce", it) }
        accessToken?.let { put("ath", base64URL(sha256(it.encodeToByteArray()))) }
    }
    val signingInput = "${base64URL(header.toString().encodeToByteArray())}.${base64URL(claims.toString().encodeToByteArray())}"
    return "$signingInput.${base64URL(key.sign(signingInput.encodeToByteArray()))}"
}

internal class ClientTrustResponse(val ok: Boolean, val status: Int, val body: ByteArray, val retryAfter: Int? = null)

internal class ClientTrust(
    private val send: suspend (method: String, path: String, body: ByteArray?, headers: Map<String, String>) -> ClientTrustResponse,
    private val url: (String) -> String,
    private val keys: DeviceKeyProvider,
    private val attestation: KeyAttestationProvider?,
    private val now: () -> Instant,
    private val log: KIDLog,
) {
    private class Token(val value: String, val expiresAt: Instant, val trustLevel: TrustLevel)

    private val mutex = Mutex()
    private var token: Token? = null
    private var nextMintAt = Instant.MIN

    // Null when the last mint failed before reaching k-ID (no device key), or did not fail.
    private var failure: ClientTrustResponse? = null

    suspend fun headers(method: String, path: String): Map<String, String> {
        val token = current() ?: return emptyMap()
        val proof = runCatching { dpopProof(keys.key(), method, url(path), now(), accessToken = token.value) }.getOrNull()
            ?: return emptyMap()
        return mapOf("Kid-Client" to token.value, "DPoP" to proof)
    }

    /** The trust level of the token this client sends, minting one if it has none; null when it has none to send. */
    suspend fun trustLevel(): TrustLevel? = current()?.trustLevel

    /**
     * The token step's failure that leaves calls without a token, with a 429's `retryAfter` as the seconds left before
     * the next mint; null when there is none.
     */
    fun tokenFailure(): ClientTrustResponse? {
        val failed = failure ?: return null
        if (failed.status != 429) return failed
        val left = Duration.between(now(), nextMintAt)
        val seconds = maxOf(1L, left.seconds + if (left.nano > 0) 1 else 0).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        return ClientTrustResponse(failed.ok, failed.status, failed.body, seconds)
    }

    fun invalidate() {
        token = null
    }

    private suspend fun current(): Token? = mutex.withLock {
        token?.takeIf { Duration.between(now(), it.expiresAt) > EXPIRY_MARGIN }?.let { return it }
        if (now() < nextMintAt) return null
        val minted = mint()
        token = minted
        // A rate limit's Retry-After replaces the cooldown, so the mint is tried again as soon as k-ID allows.
        if (minted == null) nextMintAt = now() + (failure?.retryAfter?.let { Duration.ofSeconds(it.toLong()) } ?: MINT_COOLDOWN)
        minted
    }

    private suspend fun mint(): Token? {
        failure = null
        val key = runCatching { keys.key() }.getOrNull() ?: return null
        val challenge = send("POST", "/client/challenge", "{}".encodeToByteArray(), emptyMap())
        if (!challenge.ok) {
            failure = challenge
            return null
        }
        val issued = runCatching { json.decodeFromString<ClientChallenge>(challenge.body.decodeToString()) }.getOrNull()
            ?: return null

        val clientDataHash = sha256("${issued.nonce}.${jwkThumbprint(key.publicJWK)}".encodeToByteArray())
        val chain = attestation?.attestationChain(clientDataHash)
        val body = ClientAttestRequest(
            platform = "android",
            attestationChain = chain?.map { Base64.getEncoder().encodeToString(it) },
        )
        val proof = runCatching { dpopProof(key, "POST", url("/client/attest"), now(), nonce = issued.nonce) }.getOrNull()
            ?: return null
        val attested = send("POST", "/client/attest", json.encodeToString(body).encodeToByteArray(), mapOf("DPoP" to proof))
        if (!attested.ok) {
            failure = attested
            return null
        }
        val minted = runCatching { json.decodeFromString<ClientToken>(attested.body.decodeToString()) }.getOrNull()
            ?: return null
        if (attestation != null && minted.trustLevel != TrustLevel.VERIFIED) {
            log(
                LogLevel.WARN,
                "Key Attestation failed. It needs a release build signed with a certificate registered in Kai, on a " +
                    "device with a locked bootloader; use a test key while developing.",
            )
        }
        return Token(minted.clientToken, minted.expiresAt, minted.trustLevel)
    }

    companion object {
        /** A token is refreshed this long before it expires, so a call never races its expiry. */
        val EXPIRY_MARGIN: Duration = Duration.ofSeconds(60)

        /** So an outage of the mint endpoints doesn't add two requests to every call. */
        val MINT_COOLDOWN: Duration = Duration.ofSeconds(60)
    }
}
