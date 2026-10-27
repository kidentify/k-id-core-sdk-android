package com.kid.core

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.time.Instant

@KIDInternalApi
public data class SessionRead(
    val session: Session?,
    val etag: String?,
    val notModified: Boolean,
    /** k-ID's answer when the session no longer exists. */
    val notFound: KIDError? = null,
)

@KIDInternalApi
public sealed interface SessionChangeResult {
    public data class Ok(val session: Session) : SessionChangeResult

    public data class Failed(val error: KIDError) : SessionChangeResult
}

@KIDInternalApi
public data class SessionUpgradeResult(val session: Session, val challenge: Challenge?)

@KIDInternalApi
public data class SessionVerifyAgeResult(val status: VerifySessionAgeStatus, val challenge: Challenge?)

@KIDInternalApi
public data class SendEmailResult(val ok: Boolean, val error: KIDError? = null)

@KIDInternalApi
public interface KIDClient {
    public suspend fun ageGateCheck(age: AgeSignal?, platformAgeSignal: PlatformAgeSignal?, jurisdiction: String?): AgeResult

    /** Whether the server will use a platform age signal from this client: a verified app on a live key, any app on a test key. */
    public suspend fun platformAgeSignalTrusted(): Boolean

    public suspend fun getRequirements(jurisdiction: String?): Requirements

    public suspend fun sessionGet(sessionId: String, etag: String?): SessionRead

    public suspend fun challengeGetStatus(challengeId: String): ChallengeStatus

    /**
     * Streams the challenge's status, handing each one to [onStatus] in turn, until the stream ends or the caller is
     * cancelled. Throws a [KIDError] when the stream can't be opened; [KIDErrorCode.NOT_FOUND] means streaming is
     * unavailable, so poll instead.
     */
    public suspend fun challengeStream(challengeId: String, onStatus: suspend (ChallengeStatus) -> Unit): ChallengeStreamEnd

    public suspend fun challengeSendEmail(challengeId: String, email: String?): SendEmailResult

    public suspend fun challengeInviteParent(sessionId: String, parentEmail: String, playerName: String?): InviteParentResult

    public suspend fun sessionChange(sessionId: String, name: PermissionName, enabled: Boolean): SessionChangeResult

    public suspend fun sessionUpgrade(sessionId: String, permissionNames: List<PermissionName>): SessionUpgradeResult

    public suspend fun sessionVerifyAge(sessionId: String): SessionVerifyAgeResult

    public suspend fun declare(input: KIDDeclare)
}

@KIDInternalApi
public class HttpRequest(
    public val method: String,
    public val url: String,
    public val headers: Map<String, String>,
    public val body: ByteArray?,
)

@KIDInternalApi
public class HttpResponse(public val status: Int, public val headers: Map<String, String>, public val body: ByteArray)

@KIDInternalApi
public typealias KIDTransport = suspend (HttpRequest) -> HttpResponse

/** An opened streaming response; [close] ends it, which also unblocks a [body] read in progress. */
@KIDInternalApi
public class HttpStreamResponse(
    public val status: Int,
    public val headers: Map<String, String>,
    public val body: Flow<ByteArray>,
    public val close: () -> Unit,
)

@KIDInternalApi
public typealias KIDStreamTransport = suspend (HttpRequest) -> HttpStreamResponse

/** How an opened challenge stream ended: the server closed it (with its reconnect hint, if any), or it dropped. */
@KIDInternalApi
public sealed interface ChallengeStreamEnd {
    public data class Closed(val retryMs: Int?) : ChallengeStreamEnd

    public data object Dropped : ChallengeStreamEnd
}

@KIDInternalApi
public class ClientOptions(
    public val publishableKey: String?,
    public val baseUrl: String,
    /** Sent as `Kid-App-Id`, which stands in for the `Origin` a native request lacks: the app's package name. */
    public val appId: String?,
    public val transport: KIDTransport = ::urlConnectionTransport,
    public val streamTransport: KIDStreamTransport = ::urlConnectionStreamTransport,
    public val deviceKeys: DeviceKeyProvider = KeystoreDeviceKeyProvider,
    public val keyAttestation: KeyAttestationProvider = KeystoreKeyAttestation,
    public val now: () -> Instant = Instant::now,
    public val log: KIDLog? = null,
    /** Reads an image in the app (an asset, a resource, a content or file URI) for the declared icon. */
    public val readImage: (suspend (URI) -> ByteArray)? = null,
)

@KIDInternalApi
public fun createClient(options: ClientOptions): KIDClient = HttpClient(options)

internal val json = Json {
    ignoreUnknownKeys = true
    // An unknown enum value (a new `managedBy`) falls back to the field's default rather than failing the response.
    coerceInputValues = true
    explicitNulls = false
}

private fun <T> JsonObjectBuilder.putField(key: String, field: DeclareField<T>, encode: (T) -> JsonElement) {
    when (field) {
        DeclareField.Keep -> Unit
        DeclareField.Clear -> put(key, JsonNull)
        is DeclareField.Set -> put(key, encode(field.value))
    }
}

internal suspend fun urlConnectionTransport(request: HttpRequest): HttpResponse = withContext(Dispatchers.IO) {
    val connection = URI(request.url).toURL().openConnection() as HttpURLConnection
    try {
        connection.requestMethod = request.method
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        connection.useCaches = false
        for ((name, value) in request.headers) connection.setRequestProperty(name, value)
        request.body?.let { body ->
            connection.doOutput = true
            connection.outputStream.use { it.write(body) }
        }
        val status = connection.responseCode
        val stream = if (status >= 400) connection.errorStream else connection.inputStream
        val body = stream?.use { it.readBytes() } ?: ByteArray(0)
        val headers = connection.headerFields.entries
            .filter { it.key != null && it.value.isNotEmpty() }
            .associate { it.key to it.value.first() }
        HttpResponse(status, headers, body)
    } finally {
        connection.disconnect()
    }
}

// The server sends a heartbeat every 20s, so a read that waits longer than this is on a dead connection.
private const val STREAM_READ_TIMEOUT_MS = 60_000

internal suspend fun urlConnectionStreamTransport(request: HttpRequest): HttpStreamResponse = withContext(Dispatchers.IO) {
    val connection = URI(request.url).toURL().openConnection() as HttpURLConnection
    try {
        connection.requestMethod = request.method
        connection.connectTimeout = 15_000
        connection.readTimeout = STREAM_READ_TIMEOUT_MS
        connection.useCaches = false
        for ((name, value) in request.headers) connection.setRequestProperty(name, value)
        val status = connection.responseCode
        val headers = connection.headerFields.entries
            .filter { it.key != null && it.value.isNotEmpty() }
            .associate { it.key to it.value.first() }
        val body = flow {
            val stream = (if (status >= 400) connection.errorStream else connection.inputStream) ?: return@flow
            stream.use {
                val buffer = ByteArray(8192)
                while (true) {
                    val read = it.read(buffer)
                    if (read < 0) break
                    emit(buffer.copyOf(read))
                }
            }
        }.flowOn(Dispatchers.IO)
        HttpStreamResponse(status, headers, body, close = connection::disconnect)
    } catch (error: IOException) {
        connection.disconnect()
        throw error
    }
}

private fun encodeURIComponent(value: String): String =
    URLEncoder.encode(value, "UTF-8").replace("+", "%20").replace("%21", "!").replace("%27", "'")
        .replace("%28", "(").replace("%29", ")").replace("%7E", "~")

internal class Response(
    val ok: Boolean,
    val status: Int,
    val body: ByteArray,
    val etag: String?,
    val retryAfter: Int? = null,
) {
    val errorBody: ErrorBody by lazy { decode<ErrorBody>() ?: ErrorBody() }

    inline fun <reified T> decode(): T? = try {
        json.decodeFromString<T>(body.decodeToString())
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}

internal class HttpClient(options: ClientOptions) : KIDClient {
    private val publishableKey = options.publishableKey
    private val appId = options.appId
    private val baseUrl = options.baseUrl.trimEnd('/')
    private val transport = options.transport
    private val streamTransport = options.streamTransport
    private val readImage = options.readImage
    private val clientTrust: ClientTrust? = options.publishableKey?.let { key ->
        ClientTrust(
            send = { method, path, body, headers ->
                val response = rawSend(method, path, body, headers)
                ClientTrustResponse(response.ok, response.status, response.body, response.retryAfter)
            },
            url = { path -> baseUrl + path },
            keys = options.deviceKeys,
            // A test key always mints basic, so it never spends a hardware attestation.
            attestation = if (isLiveKey(key)) options.keyAttestation else null,
            now = options.now,
            log = options.log ?: defaultLog,
        )
    }

    private fun kidError(response: Response, keys: MessageKeys = MessageKeys()): KIDError {
        val body = response.errorBody
        val code = toKIDErrorCode(body.error, response.status)
        return KIDError(
            status = response.status,
            code = code,
            message = resolveErrorMessage(body.error, body.errorMessage, response.status, keys),
            rawCode = body.error,
            retryAfter = if (code == KIDErrorCode.RATE_LIMITED) response.retryAfter else null,
        )
    }

    private suspend fun send(method: String, path: String, body: ByteArray? = null, headers: Map<String, String> = emptyMap()): Response {
        val trust = clientTrust ?: return rawSend(method, path, body, headers)
        suspend fun attempt(): Response {
            val tokenHeaders = trust.headers(method, path)
            val response = rawSend(method, path, body, headers + tokenHeaders)
            return tokenFailure(response.status, tokenHeaders) ?: response
        }
        val first = attempt()
        if (first.errorBody.error != "CLIENT_TOKEN_INVALID") return first
        trust.invalidate()
        return attempt()
    }

    /** A call that went out without a token and came back 401 reports why it had none: the token step's own failure. */
    private fun tokenFailure(status: Int, tokenHeaders: Map<String, String>): Response? {
        if (status != 401 || tokenHeaders.isNotEmpty()) return null
        val failure = clientTrust?.tokenFailure() ?: return null
        return Response(false, failure.status, failure.body, null, failure.retryAfter)
    }

    private fun request(method: String, path: String, body: ByteArray?, headers: Map<String, String>): HttpRequest {
        val all = buildMap {
            if (body != null) put("Content-Type", "application/json")
            publishableKey?.let { put("Authorization", "Bearer $it") }
            appId?.let { put("Kid-App-Id", it) }
            putAll(headers)
        }
        return HttpRequest(method, baseUrl + path, all, body)
    }

    private suspend fun rawSend(method: String, path: String, body: ByteArray?, headers: Map<String, String>): Response {
        return try {
            val response = transport(request(method, path, body, headers))
            val etag = response.headers.entries.firstOrNull { it.key.equals("ETag", ignoreCase = true) }?.value
            val retryAfter = if (response.status == 429) {
                parseRetryAfter(response.headers.entries.firstOrNull { it.key.equals("Retry-After", ignoreCase = true) }?.value)
            } else {
                null
            }
            Response(response.status in 200..299, response.status, response.body, etag, retryAfter)
        } catch (_: IOException) {
            Response(false, 0, ByteArray(0), null)
        } catch (_: IllegalArgumentException) {
            Response(false, 0, ByteArray(0), null)
        }
    }

    private suspend inline fun <reified T> post(path: String, body: T): Response =
        send("POST", path, json.encodeToString(body).encodeToByteArray())

    private inline fun <reified T> decoded(response: Response, keys: MessageKeys = MessageKeys()): T {
        if (!response.ok) throw kidError(response, keys)
        return response.decode<T>()
            ?: throw KIDError(response.status, KIDErrorCode.UNAVAILABLE, KIDErrorMessage.SERVER_ERROR.text)
    }

    override suspend fun ageGateCheck(age: AgeSignal?, platformAgeSignal: PlatformAgeSignal?, jurisdiction: String?): AgeResult {
        val request = CheckAgeGateRequest(
            jurisdiction = jurisdiction?.ifEmpty { null },
            dateOfBirth = (age as? AgeSignal.DateOfBirth)?.value,
            age = (age as? AgeSignal.Age)?.value,
            platformAgeSignal = platformAgeSignal,
        )
        return decoded(post("/age-gate/check", request), MessageKeys(invalid = KIDErrorMessage.INVALID_AGE_SIGNAL))
    }

    override suspend fun platformAgeSignalTrusted(): Boolean {
        val trust = clientTrust ?: return false
        val trustLevel = trust.trustLevel() ?: return false
        return publishableKey?.let(::isLiveKey) == false || trustLevel == TrustLevel.VERIFIED
    }

    override suspend fun getRequirements(jurisdiction: String?): Requirements {
        val query = jurisdiction?.ifEmpty { null }?.let { "?jurisdiction=${encodeURIComponent(it)}" } ?: ""
        return decoded(send("GET", "/age-gate/get-requirements$query"), MessageKeys(invalid = KIDErrorMessage.INVALID_JURISDICTION))
    }

    override suspend fun sessionGet(sessionId: String, etag: String?): SessionRead {
        val response = send(
            "GET",
            "/session/get?sessionId=${encodeURIComponent(sessionId)}",
            headers = etag?.let { mapOf("If-None-Match" to it) } ?: emptyMap(),
        )
        if (response.status == 304) return SessionRead(null, etag, notModified = true)
        if (!response.ok) {
            val error = kidError(response, MessageKeys(notFound = KIDErrorMessage.SESSION_NOT_FOUND))
            if (error.code == KIDErrorCode.NOT_FOUND) return SessionRead(null, null, notModified = false, notFound = error)
            throw error
        }
        val session = decoded<Session>(response)
        return SessionRead(session, session.etag ?: response.etag, notModified = false)
    }

    override suspend fun challengeGetStatus(challengeId: String): ChallengeStatus = decoded(
        send("GET", "/challenge/get-status?challengeId=${encodeURIComponent(challengeId)}"),
        MessageKeys(notFound = KIDErrorMessage.CHALLENGE_NOT_FOUND),
    )

    override suspend fun challengeStream(challengeId: String, onStatus: suspend (ChallengeStatus) -> Unit): ChallengeStreamEnd {
        val path = "/challenge/stream?challengeId=${encodeURIComponent(challengeId)}"
        val keys = MessageKeys(notFound = KIDErrorMessage.CHALLENGE_NOT_FOUND)

        var tokenHeaders = emptyMap<String, String>()
        suspend fun open(): HttpStreamResponse {
            tokenHeaders = clientTrust?.headers("GET", path) ?: emptyMap()
            val headers = mapOf("Accept" to "text/event-stream") + tokenHeaders
            return try {
                streamTransport(request("GET", path, null, headers))
            } catch (_: IOException) {
                throw streamUnavailable()
            } catch (_: IllegalArgumentException) {
                throw streamUnavailable()
            }
        }
        suspend fun refused(response: HttpStreamResponse): KIDError {
            tokenFailure(response.status, tokenHeaders)?.let {
                response.close()
                return kidError(it, keys)
            }
            val body = try {
                val chunks = mutableListOf<ByteArray>()
                response.body.collect { chunks += it }
                chunks.fold(ByteArray(0)) { all, chunk -> all + chunk }
            } catch (_: IOException) {
                ByteArray(0)
            } finally {
                response.close()
            }
            return kidError(Response(false, response.status, body, null), keys)
        }

        var response = open()
        if (response.status !in 200..299) {
            val error = refused(response)
            val trust = clientTrust
            if (trust == null || error.rawCode != "CLIENT_TOKEN_INVALID") throw error
            trust.invalidate()
            response = open()
            if (response.status !in 200..299) throw refused(response)
        }
        val contentType = response.headers.entries.firstOrNull { it.key.equals("Content-Type", ignoreCase = true) }?.value
        // A proxy that answers with something other than a stream can't stream to this client.
        if (contentType?.startsWith("text/event-stream") != true) {
            response.close()
            throw streamUnavailable()
        }

        val parser = SseParser()
        return coroutineScope {
            // Closing the connection on cancellation unblocks a read waiting on the next heartbeat.
            val closer = launch(start = CoroutineStart.UNDISPATCHED) {
                try {
                    awaitCancellation()
                } finally {
                    response.close()
                }
            }
            try {
                response.body.collect { chunk ->
                    for (byte in chunk) {
                        val event = parser.push(byte) ?: continue
                        if (event.event != "status") continue
                        val status = try {
                            json.decodeFromString<ChallengeStatus>(event.data)
                        } catch (_: SerializationException) {
                            continue
                        } catch (_: IllegalArgumentException) {
                            continue
                        }
                        onStatus(status)
                    }
                }
                ChallengeStreamEnd.Closed(parser.retryMs)
            } catch (_: IOException) {
                ChallengeStreamEnd.Dropped
            } finally {
                closer.cancel()
            }
        }
    }

    override suspend fun challengeSendEmail(challengeId: String, email: String?): SendEmailResult {
        val response = post("/challenge/send-email", SendEmailRequest(challengeId, email?.ifEmpty { null }))
        if (response.ok) return SendEmailResult(ok = true)
        return SendEmailResult(
            ok = false,
            error = kidError(response, MessageKeys(KIDErrorMessage.CHALLENGE_NOT_FOUND, KIDErrorMessage.CHALLENGE_RESOLVED)),
        )
    }

    override suspend fun challengeInviteParent(sessionId: String, parentEmail: String, playerName: String?): InviteParentResult =
        decoded(
            post("/challenge/invite-parent", InviteParentRequest(sessionId, parentEmail, playerName?.ifEmpty { null })),
            MessageKeys(notFound = KIDErrorMessage.SESSION_NOT_FOUND),
        )

    override suspend fun sessionChange(sessionId: String, name: PermissionName, enabled: Boolean): SessionChangeResult {
        val response = post("/session/change", ChangeSessionRequest(sessionId, name.rawValue, enabled))
        if (response.ok) {
            return response.decode<Session>()?.let(SessionChangeResult::Ok)
                ?: SessionChangeResult.Failed(KIDError(response.status, KIDErrorCode.UNAVAILABLE, KIDErrorMessage.SERVER_ERROR.text))
        }
        return SessionChangeResult.Failed(
            kidError(response, MessageKeys(KIDErrorMessage.SESSION_NOT_FOUND, KIDErrorMessage.PERMISSION_NOT_CHANGEABLE)),
        )
    }

    override suspend fun sessionUpgrade(sessionId: String, permissionNames: List<PermissionName>): SessionUpgradeResult {
        val request = UpgradeSessionRequest(sessionId, permissionNames.map { RequestedPermission(it.rawValue) })
        val data = decoded<UpgradeSessionResponse>(
            post("/session/upgrade", request),
            MessageKeys(notFound = KIDErrorMessage.SESSION_NOT_FOUND),
        )
        return SessionUpgradeResult(data.session, data.challenge)
    }

    override suspend fun sessionVerifyAge(sessionId: String): SessionVerifyAgeResult {
        val data = decoded<VerifySessionAgeResponse>(
            post("/session/verify-age", VerifySessionAgeRequest(sessionId)),
            MessageKeys(notFound = KIDErrorMessage.SESSION_NOT_FOUND),
        )
        return SessionVerifyAgeResult(data.status, data.challenge)
    }

    override suspend fun declare(input: KIDDeclare) {
        val icon = input.iconUrl
        val iconUrl = if (icon is DeclareField.Set) DeclareField.Set(declaredIconUrl(icon.value, transport, readImage)) else icon
        val request = buildJsonObject {
            put("permissions", json.encodeToJsonElement(input.permissions.map { DeclaredPermission(it.name.rawValue) }))
            putField("privacyPolicyUrl", input.privacyPolicyUrl, ::JsonPrimitive)
            putField("termsOfServiceUrl", input.termsOfServiceUrl, ::JsonPrimitive)
            putField("iconUrl", iconUrl, ::JsonPrimitive)
            put("supportEmail", JsonPrimitive(input.supportEmail))
        }
        val response = post("/declare", request)
        if (!response.ok) {
            val body = response.errorBody
            val code = toKIDErrorCode(body.error, response.status)
            throw KIDError(
                status = response.status,
                code = code,
                message = body.errorMessage ?: body.error ?: "Declare failed.",
                rawCode = body.error,
                retryAfter = if (code == KIDErrorCode.RATE_LIMITED) response.retryAfter else null,
            )
        }
    }
}

@KIDInternalApi
public fun isLiveKey(publishableKey: String): Boolean = publishableKey.startsWith("kgp_live_")

private fun streamUnavailable() = KIDError(0, KIDErrorCode.UNAVAILABLE, KIDErrorMessage.SERVER_ERROR.text)
