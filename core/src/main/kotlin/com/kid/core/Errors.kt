package com.kid.core

/** The closed set of error kinds a caller can branch on. */
public enum class KIDErrorCode(public val rawValue: String) {
    INVALID_REQUEST("invalid_request"),
    UNAUTHORIZED("unauthorized"),
    NOT_FOUND("not_found"),
    RATE_LIMITED("rate_limited"),
    UNAVAILABLE("unavailable"),
    ;

    public companion object {
        internal fun fromRawValue(rawValue: String?): KIDErrorCode? = entries.firstOrNull { it.rawValue == rawValue }
    }
}

/** A failed k-ID call. [message] is safe to show a player; it is never raw server text. */
public class KIDError(
    public val status: Int,
    public val code: KIDErrorCode,
    override val message: String,
    public val rawCode: String? = null,
    /** With [KIDErrorCode.RATE_LIMITED], the whole seconds k-ID asked the caller to wait before trying again, when it said. */
    public val retryAfter: Int? = null,
) : Exception(message) {
    override fun toString(): String = "KIDError(status=$status, code=${code.rawValue}, rawCode=$rawCode, message=$message)"
}

/** The outcome of a user-recoverable action: `ok`, or the error it failed with. */
public class KIDResult(
    public val ok: Boolean,
    public val error: KIDError? = null,
) {
    override fun equals(other: Any?): Boolean =
        this === other || other is KIDResult &&
            ok == other.ok &&
            error == other.error

    override fun hashCode(): Int {
        var result = ok.hashCode()
        result = 31 * result + (error?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String = "KIDResult(ok=$ok, error=$error)"
}

/** The error a failure the SDK catches before any call carries, such as a submit before a challenge exists. */
internal fun localKIDError(): KIDError = KIDError(0, KIDErrorCode.INVALID_REQUEST, KIDErrorMessage.INVALID_REQUEST.text)

/** Anything that is not already a KIDError, such as a dropped connection, becomes the transport error. */
internal fun toKIDError(error: Any?): KIDError =
    error as? KIDError ?: KIDError(0, KIDErrorCode.UNAVAILABLE, KIDErrorMessage.SERVER_ERROR.text)

/** Retry-After as a whole number of seconds; anything else, an HTTP date or an overflow included, is left out. */
internal fun parseRetryAfter(header: String?): Int? {
    val value = header?.trim()?.ifEmpty { null } ?: return null
    if (!value.all { it in '0'..'9' }) return null
    return value.toIntOrNull()
}

// The API returns almost every error as HTTP 400, so map on the JSON `error` code, not the status.
private val rawErrorToCode = mapOf(
    "NOT_FOUND" to KIDErrorCode.NOT_FOUND,
    "UNAUTHORIZED" to KIDErrorCode.UNAUTHORIZED,
    "TOO_MANY_REQUESTS" to KIDErrorCode.RATE_LIMITED,
    "INVALID_INPUT" to KIDErrorCode.INVALID_REQUEST,
    "INVALID_EMAIL" to KIDErrorCode.INVALID_REQUEST,
    "AGE_CONFLICT" to KIDErrorCode.INVALID_REQUEST,
    "FEATURE_DISABLED" to KIDErrorCode.UNAVAILABLE,
    "CANNOT_RESOLVE_JURISDICTION" to KIDErrorCode.UNAVAILABLE,
    "INTERNAL_ERROR" to KIDErrorCode.UNAVAILABLE,
    "UNKNOWN" to KIDErrorCode.UNAVAILABLE,
)

@KIDInternalApi
public fun toKIDErrorCode(rawCode: String?, status: Int): KIDErrorCode {
    rawCode?.let { rawErrorToCode[it] }?.let { return it }
    return when {
        status == 429 -> KIDErrorCode.RATE_LIMITED
        status == 401 || status == 403 -> KIDErrorCode.UNAUTHORIZED
        status == 404 -> KIDErrorCode.NOT_FOUND
        status in 400..499 -> KIDErrorCode.INVALID_REQUEST
        else -> KIDErrorCode.UNAVAILABLE
    }
}

internal enum class KIDErrorMessage(val text: String) {
    INVALID_REQUEST("The request was invalid."),
    INVALID_AGE_SIGNAL("Provide a valid date of birth or age."),
    INVALID_JURISDICTION("Unknown or invalid jurisdiction code."),
    AGE_CONFLICT("The platform age signal conflicts with the provided age."),
    CHALLENGE_ID_REQUIRED("A challenge id is required."),
    CHALLENGE_RESOLVED("This challenge is already resolved; no email can be sent."),
    EMAIL_MISMATCH("The email address doesn't match this challenge's approver, or is required."),
    PERMISSION_NOT_CHANGEABLE("That permission can't be changed here."),
    UNAUTHORIZED_KEY("Invalid or unrecognized publishable key."),
    UNAUTHORIZED_APP("This app isn't allowed to use your publishable key."),
    SESSION_NOT_FOUND("Session not found."),
    CHALLENGE_NOT_FOUND("Challenge not found."),
    RATE_LIMITED("Too many requests. Please wait and try again."),
    SERVER_ERROR("Something went wrong on our side. Please try again."),
    FEATURE_DISABLED("This feature isn't enabled for this product."),
}

private val invalidInputSubcases = listOf(
    Regex("invalid jurisdiction", RegexOption.IGNORE_CASE) to KIDErrorMessage.INVALID_JURISDICTION,
    Regex("dateofbirth|date of birth", RegexOption.IGNORE_CASE) to KIDErrorMessage.INVALID_AGE_SIGNAL,
    Regex("challenge id must be provided", RegexOption.IGNORE_CASE) to KIDErrorMessage.CHALLENGE_ID_REQUIRED,
    Regex("completed challenge", RegexOption.IGNORE_CASE) to KIDErrorMessage.CHALLENGE_RESOLVED,
)

internal data class MessageKeys(val notFound: KIDErrorMessage? = null, val invalid: KIDErrorMessage? = null)

internal fun resolveErrorMessage(rawCode: String?, rawMessage: String?, status: Int, keys: MessageKeys): String {
    val unauthorized = if (status == 403) KIDErrorMessage.UNAUTHORIZED_APP else KIDErrorMessage.UNAUTHORIZED_KEY
    when (rawCode) {
        "AGE_CONFLICT" -> return KIDErrorMessage.AGE_CONFLICT.text
        "INVALID_EMAIL" -> return KIDErrorMessage.EMAIL_MISMATCH.text
        "FEATURE_DISABLED" -> return KIDErrorMessage.FEATURE_DISABLED.text
        "TOO_MANY_REQUESTS" -> return KIDErrorMessage.RATE_LIMITED.text
        "UNAUTHORIZED" -> return unauthorized.text
        "INTERNAL_ERROR" -> return KIDErrorMessage.SERVER_ERROR.text
        "NOT_FOUND" -> return (keys.notFound ?: KIDErrorMessage.SESSION_NOT_FOUND).text
    }
    if (status == 429) return KIDErrorMessage.RATE_LIMITED.text
    if (status == 401 || status == 403) return unauthorized.text
    if (status >= 500 || status == 0) return KIDErrorMessage.SERVER_ERROR.text
    val subcase = rawMessage?.let { message -> invalidInputSubcases.firstOrNull { it.first.containsMatchIn(message) } }
    return (subcase?.second ?: keys.invalid ?: KIDErrorMessage.INVALID_REQUEST).text
}
