package com.kid.core

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

/** Marks the SDK's own wiring between its modules; not part of the public API. */
@RequiresOptIn(level = RequiresOptIn.Level.ERROR, message = "Internal to the k-ID SDK.")
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FIELD,
    AnnotationTarget.LOCAL_VARIABLE,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.TYPEALIAS,
)
public annotation class KIDInternalApi

/** RFC 3339 with or without fractional seconds (Go's time encoding drops a zero fraction). */
internal object InstantSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(DateTimeFormatter.ISO_INSTANT.format(value))
    }

    override fun deserialize(decoder: Decoder): Instant = parseISODate(decoder.decodeString())
}

internal fun parseISODate(raw: String): Instant = OffsetDateTime.parse(raw).toInstant()

/** The player's session: their age status and the permissions it grants. */
@Serializable
public class Session(
    public val sessionId: String,
    public val ageStatus: String,
    public val jurisdiction: String? = null,
    public val etag: String? = null,
    public val hasApproverEmail: Boolean? = null,
    public val permissions: List<Permission> = emptyList(),
    public val ageVerification: AgeVerification? = null,
) {
    override fun equals(other: Any?): Boolean =
        this === other || other is Session &&
            sessionId == other.sessionId &&
            ageStatus == other.ageStatus &&
            jurisdiction == other.jurisdiction &&
            etag == other.etag &&
            hasApproverEmail == other.hasApproverEmail &&
            permissions == other.permissions &&
            ageVerification == other.ageVerification

    override fun hashCode(): Int {
        var result = sessionId.hashCode()
        result = 31 * result + ageStatus.hashCode()
        result = 31 * result + (jurisdiction?.hashCode() ?: 0)
        result = 31 * result + (etag?.hashCode() ?: 0)
        result = 31 * result + (hasApproverEmail?.hashCode() ?: 0)
        result = 31 * result + permissions.hashCode()
        result = 31 * result + (ageVerification?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "Session(sessionId=$sessionId, " +
            "ageStatus=$ageStatus, " +
            "jurisdiction=$jurisdiction, " +
            "etag=$etag, " +
            "hasApproverEmail=$hasApproverEmail, " +
            "permissions=$permissions, " +
            "ageVerification=$ageVerification)"
}

/** The age a player proved, counting birthdays since, and when. */
@Serializable
public class AgeVerification(
    public val verifiedAge: Int,
    @Serializable(with = InstantSerializer::class) public val verifiedAt: Instant,
) {
    override fun equals(other: Any?): Boolean =
        this === other || other is AgeVerification &&
            verifiedAge == other.verifiedAge &&
            verifiedAt == other.verifiedAt

    override fun hashCode(): Int {
        var result = verifiedAge.hashCode()
        result = 31 * result + verifiedAt.hashCode()
        return result
    }

    override fun toString(): String = "AgeVerification(verifiedAge=$verifiedAge, verifiedAt=$verifiedAt)"
}

/** Who decides whether a permission is on. */
@Serializable
public enum class ManagedBy {
    @SerialName("PLAYER") PLAYER,
    @SerialName("GUARDIAN") GUARDIAN,
    @SerialName("PROHIBITED") PROHIBITED,
}

@Serializable
public class Permission(
    public val name: String,
    public val enabled: Boolean,
    public val managedBy: ManagedBy? = null,
    /** The age the player must verify before this permission can be enabled; null when no age check applies. */
    public val verifiedAgeThreshold: Int? = null,
) {
    public val permissionName: PermissionName get() = PermissionName(name)

    override fun equals(other: Any?): Boolean =
        this === other || other is Permission &&
            name == other.name &&
            enabled == other.enabled &&
            managedBy == other.managedBy &&
            verifiedAgeThreshold == other.verifiedAgeThreshold

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + enabled.hashCode()
        result = 31 * result + (managedBy?.hashCode() ?: 0)
        result = 31 * result + (verifiedAgeThreshold?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "Permission(name=$name, " +
            "enabled=$enabled, " +
            "managedBy=$managedBy, " +
            "verifiedAgeThreshold=$verifiedAgeThreshold)"
}

@Serializable
public class Challenge(
    public val challengeId: String,
    public val type: String? = null,
) {
    override fun equals(other: Any?): Boolean =
        this === other || other is Challenge &&
            challengeId == other.challengeId &&
            type == other.type

    override fun hashCode(): Int {
        var result = challengeId.hashCode()
        result = 31 * result + (type?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String = "Challenge(challengeId=$challengeId, type=$type)"
}

@KIDInternalApi
@Serializable
public data class ChallengeStatus(
    val status: String,
    val type: String? = null,
    val sessionId: String? = null,
    @Serializable(with = InstantSerializer::class) val expiresAt: Instant? = null,
    val permissions: List<String>? = null,
) {
    internal val permissionNames: List<PermissionName>? get() = permissions?.map(::PermissionName)
}

/** The jurisdiction's age rules, and the ways it lets the age gate collect an age. */
@Serializable
public class Requirements(
    public val shouldDisplay: Boolean,
    public val digitalConsentAge: Int? = null,
    public val civilAge: Int? = null,
    public val minimumAge: Int? = null,
    public val accessAge: Int? = null,
    public val approvedAgeCollectionMethods: List<String> = emptyList(),
) {
    override fun equals(other: Any?): Boolean =
        this === other || other is Requirements &&
            shouldDisplay == other.shouldDisplay &&
            digitalConsentAge == other.digitalConsentAge &&
            civilAge == other.civilAge &&
            minimumAge == other.minimumAge &&
            accessAge == other.accessAge &&
            approvedAgeCollectionMethods == other.approvedAgeCollectionMethods

    override fun hashCode(): Int {
        var result = shouldDisplay.hashCode()
        result = 31 * result + (digitalConsentAge?.hashCode() ?: 0)
        result = 31 * result + (civilAge?.hashCode() ?: 0)
        result = 31 * result + (minimumAge?.hashCode() ?: 0)
        result = 31 * result + (accessAge?.hashCode() ?: 0)
        result = 31 * result + approvedAgeCollectionMethods.hashCode()
        return result
    }

    override fun toString(): String =
        "Requirements(shouldDisplay=$shouldDisplay, " +
            "digitalConsentAge=$digitalConsentAge, " +
            "civilAge=$civilAge, " +
            "minimumAge=$minimumAge, " +
            "accessAge=$accessAge, " +
            "approvedAgeCollectionMethods=$approvedAgeCollectionMethods)"
}

@KIDInternalApi
@Serializable
public data class PlatformAgeSignal(
    val name: String,
    val ageLow: Int,
    val ageHigh: Int,
    val declarationType: String? = null,
)

@KIDInternalApi
@Serializable
public data class AgeResult(
    val status: String,
    val session: Session? = null,
    val challenge: Challenge? = null,
    val claimToken: String? = null,
)

@KIDInternalApi
@Serializable
public data class InviteParentResult(
    val status: String,
    val challenge: Challenge,
)

@KIDInternalApi
@Serializable
public enum class VerifySessionAgeStatus {
    @SerialName("PASS") PASS,
    @SerialName("CHALLENGE") CHALLENGE,
    @SerialName("FAIL") FAIL,
}

@Serializable
internal data class VerifySessionAgeResponse(
    val status: VerifySessionAgeStatus,
    val challenge: Challenge? = null,
)

@Serializable
internal data class UpgradeSessionResponse(
    val session: Session,
    val challenge: Challenge? = null,
)

@Serializable
internal data class CheckAgeGateRequest(
    val jurisdiction: String? = null,
    val dateOfBirth: String? = null,
    val age: Int? = null,
    val platformAgeSignal: PlatformAgeSignal? = null,
)

@Serializable
internal data class SendEmailRequest(val challengeId: String, val email: String? = null)

@Serializable
internal data class InviteParentRequest(val sessionId: String, val parentEmail: String, val playerName: String? = null)

@Serializable
internal data class ChangeSessionRequest(val sessionId: String, val name: String, val enabled: Boolean)

@Serializable
internal data class RequestedPermission(val name: String)

@Serializable
internal data class UpgradeSessionRequest(val sessionId: String, val requestedPermissions: List<RequestedPermission>)

@Serializable
internal data class VerifySessionAgeRequest(val sessionId: String)

@Serializable
internal data class DeclaredPermission(val name: String)

@Serializable
internal data class ClientChallenge(
    val nonce: String,
    @Serializable(with = InstantSerializer::class) val expiresAt: Instant,
)

@Serializable
internal enum class TrustLevel {
    @SerialName("verified") VERIFIED,
    @SerialName("basic") BASIC,
}

@Serializable
internal data class ClientToken(
    val clientToken: String,
    @Serializable(with = InstantSerializer::class) val expiresAt: Instant,
    val trustLevel: TrustLevel,
)

@Serializable
internal data class ClientAttestRequest(
    val platform: String,
    val attestationChain: List<String>? = null,
)

@Serializable
internal data class ErrorBody(
    val error: String? = null,
    val errorMessage: String? = null,
)

/** The player's age: a `YYYY-MM-DD` date of birth, or an age in years. */
public sealed interface AgeSignal {
    public data class DateOfBirth(val value: String) : AgeSignal

    public data class Age(val value: Int) : AgeSignal
}

/** An optional declared value: [Keep] what k-ID has, [Clear] it, or [Set] it. */
public sealed interface DeclareField<out T> {
    public data object Keep : DeclareField<Nothing>

    public data object Clear : DeclareField<Nothing>

    public data class Set<out T>(val value: T) : DeclareField<T>
}

/** Read once when the provider is created, and applied only while you use a test key. */
public data class KIDDeclare(
    val permissions: List<Permission>,
    val privacyPolicyUrl: DeclareField<String> = DeclareField.Keep,
    val termsOfServiceUrl: DeclareField<String> = DeclareField.Keep,
    /** A public https link, or an image in the app (an asset, a resource, a content or file URI), sent inline. */
    val iconUrl: DeclareField<String> = DeclareField.Keep,
    /** The address k-ID's emails give players and parents for help. */
    val supportEmail: String,
) {
    public data class Permission(val name: PermissionName)
}
