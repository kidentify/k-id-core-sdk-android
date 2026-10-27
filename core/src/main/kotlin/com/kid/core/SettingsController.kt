package com.kid.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant

public enum class SettingsChallengeStatus(public val rawValue: String) {
    IDLE("idle"),
    SENDING("sending"),
    PENDING("pending"),
    ERROR("error"),
}

/** The in-flight ask to the player's grownup, and the permissions it asks for. */
public class SettingsChallengeState(
    public val status: SettingsChallengeStatus,
    public val permissions: List<PermissionName> = emptyList(),
    public val challengeId: String? = null,
    /** What the challenge asks for: a grownup's approval, or the player's own age check. Null until known. */
    public val challengeKind: KIDChallengeKind? = null,
) {
    override fun equals(other: Any?): Boolean =
        this === other || other is SettingsChallengeState &&
            status == other.status &&
            permissions == other.permissions &&
            challengeId == other.challengeId &&
            challengeKind == other.challengeKind

    override fun hashCode(): Int {
        var result = status.hashCode()
        result = 31 * result + permissions.hashCode()
        result = 31 * result + (challengeId?.hashCode() ?: 0)
        result = 31 * result + (challengeKind?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "SettingsChallengeState(status=$status, " +
            "permissions=$permissions, " +
            "challengeId=$challengeId, " +
            "challengeKind=$challengeKind)"
}

@KIDInternalApi
public data class SettingsState(
    val session: Session? = null,
    val isLoaded: Boolean = false,
    val error: KIDError? = null,
    val challenge: SettingsChallengeState,
)

@KIDInternalApi
public data class SettingsInputs(val sessionId: String, val challengeId: String? = null)

/** Drives the settings menu: reads the session's permissions, changes them, and asks the grownup for more. */
public class SettingsController @KIDInternalApi constructor(
    private val client: KIDClient,
    scope: CoroutineScope,
    private val initial: SettingsInputs,
    private val onChallenge: ((String) -> Unit)? = null,
    private val sessions: SessionStore? = null,
    /** The app-wide watcher that keeps polling an ask after this controller is destroyed; without one, the controller
     * polls its own asks only while started. */
    asks: AskWatcher? = null,
    pollIntervalMs: Int? = null,
    now: () -> Instant = Instant::now,
    setTimer: SetTimer? = null,
    clearTimer: ClearTimer? = null,
    log: KIDLog? = null,
) {
    private val log: KIDLog = log ?: defaultLog

    @KIDInternalApi
    public val tasks: TaskTracker = TaskTracker(scope)

    private val ownsAsks = asks == null
    private val asks: AskWatcher = asks
        ?: AskWatcher(client, scope, sessions, pollIntervalMs, now, setTimer, clearTimer, log, tasks)

    private var started = false
    private var resumed = false
    private var etag: String? = null
    private var sessionsSubscription: Any? = null
    private var asksSubscription: Any? = null
    private var attachment: Any? = null


    // The ask while it is being sent, or once sending failed; after that the watcher holds it.
    private var local: SettingsChallengeState? =
        initial.challengeId?.let { SettingsChallengeState(SettingsChallengeStatus.PENDING, challengeId = it) }

    private val mutableState = MutableStateFlow(SettingsState(challenge = challengeState()))

    @KIDInternalApi
    public val state: StateFlow<SettingsState> = mutableState.asStateFlow()

    private var current: SettingsState
        get() = mutableState.value
        set(value) {
            mutableState.value = value
        }

    private fun challengeState(): SettingsChallengeState {
        local?.let { return it }
        val ask = asks.get(initial.sessionId) ?: return SettingsChallengeState(SettingsChallengeStatus.IDLE)
        val status = if (ask.status == PendingAsk.Status.PENDING) SettingsChallengeStatus.PENDING else SettingsChallengeStatus.ERROR
        return SettingsChallengeState(status, ask.permissions, ask.challengeId, ask.challengeKind)
    }

    private fun syncChallenge() {
        current = current.copy(challenge = challengeState())
    }

    private fun setLocal(next: SettingsChallengeState?) {
        local = next
        syncChallenge()
    }

    private fun commitSession(session: Session?) {
        val prev = current.session
        current = current.copy(session = session, isLoaded = true, error = null)
        session?.takeIf { it != prev }?.let { sessions?.publish(it) }
    }

    private fun adoptSession(session: Session) {
        if (session == current.session || session.sessionId != initial.sessionId) return
        etag = null
        commitSession(session)
    }

    // A restarted controller missed what was published while it was stopped.
    private fun catchUpSession() {
        if (current.session == null) return
        sessions?.current(initial.sessionId)?.let(::adoptSession)
    }

    private suspend fun loadSession(): Boolean {
        try {
            val read = client.sessionGet(initial.sessionId, etag)
            if (read.notModified) return true
            if (read.session == null) {
                current = current.copy(error = read.notFound ?: toKIDError(null))
                return true
            }
            etag = read.etag
            commitSession(read.session)
            return true
        } catch (error: Exception) {
            rethrowCancellation(error)
            log(LogLevel.ERROR, "could not load the session $error")
            current = current.copy(error = toKIDError(error))
            return false
        }
    }

    @KIDInternalApi
    public fun start() {
        if (started) return
        started = true
        if (sessionsSubscription == null) sessionsSubscription = sessions?.subscribe(::adoptSession)
        catchUpSession()
        if (asksSubscription == null) asksSubscription = asks.subscribe(::syncChallenge)
        if (attachment == null) attachment = asks.attach(initial.sessionId, ::loadSession)
        if (ownsAsks) asks.start()
        tasks.spawn { loadSession() }
        if (!resumed) {
            resumed = true
            val challengeId = initial.challengeId
            if (challengeId != null && asks.get(initial.sessionId) == null) {
                asks.watch(initial.sessionId, challengeId, emptyList(), delay = 0)
            }
            local = null
        }
        syncChallenge()
    }

    @KIDInternalApi
    public suspend fun change(name: PermissionName, enabled: Boolean): KIDResult =
        when (val result = client.sessionChange(initial.sessionId, name, enabled)) {
            is SessionChangeResult.Failed -> KIDResult(ok = false, error = result.error)
            is SessionChangeResult.Ok -> {
                etag = null
                commitSession(result.session)
                KIDResult(ok = true)
            }
        }

    @KIDInternalApi
    public suspend fun upgrade(names: List<PermissionName>, approverEmail: String? = null): KIDResult {
        asks.clear(initial.sessionId)
        setLocal(SettingsChallengeState(SettingsChallengeStatus.SENDING, names))
        var challengeKind: KIDChallengeKind? = null
        try {
            val result = client.sessionUpgrade(initial.sessionId, names)
            val challenge = result.challenge
            if (challenge == null) {
                loadSession()
                setLocal(null)
                return KIDResult(ok = true)
            }
            challengeKind = settingsChallengeKindOf(challenge.type)
            // Nothing is sent without the player's email; the next call asks again, and this challenge just expires.
            if (challengeKind == KIDChallengeKind.AGE_ASSURANCE && approverEmail == null) {
                setLocal(SettingsChallengeState(SettingsChallengeStatus.ERROR, names, null, challengeKind))
                return KIDResult(
                    ok = false,
                    error = KIDError(0, KIDErrorCode.INVALID_REQUEST, "The player's email is needed to verify their age."),
                )
            }
            val sent = client.challengeSendEmail(challenge.challengeId, approverEmail)
            if (!sent.ok) {
                setLocal(SettingsChallengeState(SettingsChallengeStatus.ERROR, names, challenge.challengeId, challengeKind))
                return KIDResult(ok = false, error = sent.error ?: toKIDError(null))
            }
            onChallenge?.invoke(challenge.challengeId)
            local = null
            asks.watch(initial.sessionId, challenge.challengeId, names, challengeKind = challengeKind)
            return KIDResult(ok = true)
        } catch (error: Exception) {
            rethrowCancellation(error)
            log(LogLevel.ERROR, "could not start the settings challenge $error")
            setLocal(SettingsChallengeState(SettingsChallengeStatus.ERROR, names, null, challengeKind))
            return KIDResult(ok = false, error = toKIDError(error))
        }
    }

    @KIDInternalApi
    public suspend fun refresh() {
        loadSession()
    }

    @KIDInternalApi
    public fun destroy() {
        if (ownsAsks) asks.stop()
        attachment?.let { asks.detach(initial.sessionId, it) }
        attachment = null
        asksSubscription?.let(asks::unsubscribe)
        asksSubscription = null
        sessionsSubscription?.let { sessions?.unsubscribe(it) }
        sessionsSubscription = null
        started = false
    }
}
