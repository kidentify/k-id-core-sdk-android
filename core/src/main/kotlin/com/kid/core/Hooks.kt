package com.kid.core

import java.time.Instant

/** The age gate's state, and `submit`, which sends the challenge email once a challenge exists (re-callable to re-send). */
public class UseKIDAgeGate internal constructor(
    public val status: KIDStatus,
    public val challengeId: String?,
    public val challengeKind: KIDChallengeKind?,
    public val sessionId: String?,
    /** Why the gate ended in [KIDStatus.ERROR]: a k-ID API error as is, and any other failure as unavailable. */
    public val error: KIDError?,
    public val requirements: Requirements?,
    private val submitAction: suspend () -> KIDResult,
) {
    public suspend fun submit(): KIDResult = submitAction()
}

/** The established session and its permissions; `refresh` is a cheap re-read. */
public class UseKIDSession internal constructor(
    public val session: Session?,
    public val permissions: Map<PermissionName, Boolean>,
    public val sessionId: String?,
    public val isLoaded: Boolean,
    public val ageVerification: AgeVerification?,
    public val verifyAge: UseKIDSessionVerifyAge,
    private val refreshAction: suspend () -> Unit,
) {
    public suspend fun refresh(): Unit = refreshAction()
}

/** Client permissions gate what a player SEES; enforce the real capability server-side. */
public class UseKIDPermission internal constructor(permissions: List<Permission>) {
    private val byName = permissions.associateBy { it.permissionName }

    public fun can(name: PermissionName): Boolean = byName[name]?.enabled ?: false

    public fun get(name: PermissionName): Permission? = byName[name]
}

/** The challenge, for a custom waiting screen; `expiresAt` is set only when it has a deadline. */
public class UseKIDChallenge(
    public val challengeId: String?,
    public val status: RawChallengeStatus?,
    public val expiresAt: Instant?,
) {
    override fun equals(other: Any?): Boolean =
        this === other || other is UseKIDChallenge &&
            challengeId == other.challengeId &&
            status == other.status &&
            expiresAt == other.expiresAt

    override fun hashCode(): Int {
        var result = (challengeId?.hashCode() ?: 0)
        result = 31 * result + (status?.hashCode() ?: 0)
        result = 31 * result + (expiresAt?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String = "UseKIDChallenge(challengeId=$challengeId, status=$status, expiresAt=$expiresAt)"
}

/** Verifying the session's player is an adult: `start` checks, `submit` sends the player the link. */
public class UseKIDSessionVerifyAge internal constructor(
    public val status: KIDStatus,
    public val step: VerifyAgeStep?,
    public val challengeId: String?,
    public val rawChallengeStatus: RawChallengeStatus?,
    public val expiresAt: Instant?,
    /** Why the check ended in [KIDStatus.ERROR]: a k-ID API error as is, and any other failure as unavailable. */
    public val error: KIDError?,
    private val startAction: () -> Unit,
    private val submitAction: suspend (String) -> KIDResult,
) {
    /** Checks the session is a verified adult, minting a challenge when it is not. Idempotent; a no-op until the gate passes. */
    public fun start(): Unit = startAction()

    /** Sends the player the verification link. Re-callable to re-send to a corrected address. */
    public suspend fun submit(email: String): KIDResult = submitAction(email)
}

/** The settings menu's permissions, `change` for player-managed ones and `upgrade` to ask the grownup for more. */
public class UseKIDPermissionSettings internal constructor(
    public val permissions: List<Permission>,
    public val isLoaded: Boolean,
    /** Why the session failed to load, or null; show an error and call `refresh` to retry. */
    public val error: KIDError?,
    public val hasApproverEmail: Boolean,
    public val challenge: SettingsChallengeState,
    private val changeAction: suspend (PermissionName, Boolean) -> KIDResult,
    private val upgradeAction: suspend (List<PermissionName>, String?) -> KIDResult,
    private val refreshAction: suspend () -> Unit,
) {
    public suspend fun change(name: PermissionName, enabled: Boolean): KIDResult = changeAction(name, enabled)

    /**
     * Asks the grownup to approve all of [names] in one email. Pass [approverEmail] when [hasApproverEmail] is false.
     *
     * A permission with a `verifiedAgeThreshold` needs the player's own age check instead: without an email this returns
     * [KIDErrorCode.INVALID_REQUEST] when that check is needed; call it again with the player's own email.
     */
    public suspend fun upgrade(names: List<PermissionName>, approverEmail: String? = null): KIDResult = upgradeAction(names, approverEmail)

    public suspend fun refresh(): Unit = refreshAction()
}

public val AgeGateController.ageGate: UseKIDAgeGate
    get() {
        val state = state.value
        return UseKIDAgeGate(
            status = state.status,
            challengeId = state.challengeId,
            challengeKind = state.challengeKind,
            sessionId = state.sessionId,
            error = state.error,
            requirements = state.requirements,
            submitAction = ::submit,
        )
    }

public val AgeGateController.session: UseKIDSession
    get() {
        val state = state.value
        val controller = sessionVerifyAge()
        val verify = controller?.state?.value ?: VerifyAgeState(status = KIDStatus.RUNNING)
        return UseKIDSession(
            session = state.session,
            permissions = state.session?.permissions?.associate { it.permissionName to it.enabled } ?: emptyMap(),
            sessionId = state.sessionId,
            isLoaded = state.session != null,
            ageVerification = state.session?.ageVerification,
            verifyAge = UseKIDSessionVerifyAge(
                status = verify.status,
                step = verify.step,
                challengeId = verify.challengeId,
                rawChallengeStatus = verify.rawChallengeStatus,
                expiresAt = verify.expiresAt,
                error = verify.error,
                startAction = { controller?.start() },
                submitAction = { email ->
                    if (controller == null) {
                        KIDResult(ok = false, error = localKIDError())
                    } else {
                        controller.setEmail(email)
                        controller.submit()
                    }
                },
            ),
            refreshAction = ::refreshSession,
        )
    }

public val AgeGateController.permission: UseKIDPermission
    get() = UseKIDPermission(state.value.session?.permissions ?: emptyList())

public val AgeGateController.challenge: UseKIDChallenge
    get() = state.value.let { UseKIDChallenge(it.challengeId, it.rawChallengeStatus, it.expiresAt) }

public val SettingsController.permissionSettings: UseKIDPermissionSettings
    get() {
        val state = state.value
        return UseKIDPermissionSettings(
            permissions = state.session?.permissions ?: emptyList(),
            isLoaded = state.isLoaded,
            error = state.error,
            hasApproverEmail = state.session?.hasApproverEmail != false,
            challenge = state.challenge,
            changeAction = ::change,
            upgradeAction = ::upgrade,
            refreshAction = ::refresh,
        )
    }
