package com.kid.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant

private const val MIN_POLL_INTERVAL_MS = 30_000
private const val MAX_POLL_BACKOFF_MS = 300_000

public enum class VerifyAgeStep(public val rawValue: String) {
    CHECKING("checking"),
    EMAIL("email"),
}

@KIDInternalApi
public data class VerifyAgeState(
    val status: KIDStatus,
    val step: VerifyAgeStep? = null,
    val challengeId: String? = null,
    val rawChallengeStatus: RawChallengeStatus? = null,
    val expiresAt: Instant? = null,
    val error: KIDError? = null,
)

/**
 * Verifies the player on an established session is an adult (the civil age of their jurisdiction).
 *
 * [civilAge] is the civil age the re-read session must prove before the flow passes; [onSession] receives the session
 * re-read once k-ID reports a pass.
 */
public class VerifyAgeController @KIDInternalApi constructor(
    private val client: KIDClient,
    private val sessionId: String,
    private val civilAge: suspend () -> Int?,
    private val onSession: ((Session) -> Unit)? = null,
    pollIntervalMs: Int? = null,
    private val now: () -> Instant = Instant::now,
    private val setTimer: SetTimer,
    private val clearTimer: ClearTimer,
    log: KIDLog? = null,
    @property:KIDInternalApi public val tasks: TaskTracker,
) {
    private val log: KIDLog = log ?: defaultLog
    private val interval = maxOf(MIN_POLL_INTERVAL_MS, pollIntervalMs ?: MIN_POLL_INTERVAL_MS)
    private var started = false
    private var checked = false
    private var polling = false
    private var confirming = false
    private var confirmation = 0
    private var collectedEmail: String? = null
    private var backoff = interval
    private var timer: Any? = null
    private var fallbackDelay = 0
    private val stream by lazy {
        ChallengeWatch(
            client = client,
            onStatus = { status ->
                val pending = handleStatus(status)
                if (pending) schedule(null)
                pending
            },
            onFallback = { schedule(fallbackDelay) },
            now = now,
            log = this.log,
            tasks = tasks,
        )
    }

    private val mutableState = MutableStateFlow(VerifyAgeState(status = KIDStatus.RUNNING, step = VerifyAgeStep.CHECKING))

    @KIDInternalApi
    public val state: StateFlow<VerifyAgeState> = mutableState.asStateFlow()

    private var current: VerifyAgeState
        get() = mutableState.value
        set(value) {
            mutableState.value = value
        }

    private fun stopTimer() {
        timer?.let(clearTimer)
        timer = null
    }

    /** A null delay arms only the deadline. */
    private fun schedule(delay: Int?) {
        if (!started) return
        stopTimer()
        var next = delay
        current.expiresAt?.let { expiresAt ->
            val remaining = millisUntil(now(), expiresAt)
            if (remaining <= 0) {
                finish(KIDStatus.FAIL)
                return
            }
            next = minOf(next?.toLong() ?: remaining, remaining).toInt()
        }
        if (next == null) return
        timer = setTimer({ firePoll() }, next)
    }

    private fun watch(delay: Int) {
        val challengeId = current.challengeId
        if (!started || challengeId == null) return
        fallbackDelay = delay
        schedule(if (stream.start(challengeId)) null else delay)
    }

    private fun finish(status: KIDStatus, detail: Any = "") {
        if (status == KIDStatus.ERROR) log(LogLevel.ERROR, "age verification error $detail")
        stopTimer()
        stream.stop()
        current = current.copy(
            status = status,
            step = null,
            rawChallengeStatus = if (status == KIDStatus.FAIL) RawChallengeStatus.FAIL else current.rawChallengeStatus,
            error = if (status == KIDStatus.ERROR) toKIDError(detail) else null,
        )
    }

    private fun scheduleConfirm(delay: Int) {
        if (!started) return
        stopTimer()
        timer = setTimer({ fireConfirm() }, delay)
    }

    private fun fireConfirm() {
        timer = null
        tasks.spawn { confirmPass() }
    }

    // k-ID reporting a pass is not enough: the session itself must now prove an adult.
    // A remount can start a newer confirmation while an older read is still in flight; only the newest may land.
    private suspend fun confirmPass() {
        confirmation += 1
        val id = confirmation
        confirming = true
        val verifiedAge: Int?
        val civil: Int?
        try {
            val read = client.sessionGet(sessionId, null)
            if (id != confirmation) return
            read.session?.let { onSession?.invoke(it) }
            verifiedAge = read.session?.ageVerification?.verifiedAge
            civil = civilAge()
            if (id != confirmation) return
        } catch (error: Exception) {
            rethrowCancellation(error)
            if (id != confirmation) return
            log(LogLevel.ERROR, "could not re-read the session after verifying age; will retry $error")
            backoff = minOf(backoff * 2, MAX_POLL_BACKOFF_MS)
            current = current.copy(status = KIDStatus.ERROR, step = null, error = toKIDError(error))
            scheduleConfirm(backoff)
            return
        }
        confirming = false
        backoff = interval
        if (verifiedAge == null || civil == null || verifiedAge < civil) {
            finish(KIDStatus.FAIL)
            return
        }
        current = current.copy(
            status = KIDStatus.PASS,
            step = null,
            error = null,
            rawChallengeStatus = if (current.challengeId != null) RawChallengeStatus.PASS else null,
        )
    }

    private suspend fun check() {
        checked = true
        val result = try {
            client.sessionVerifyAge(sessionId)
        } catch (error: Exception) {
            rethrowCancellation(error)
            finish(KIDStatus.ERROR, error)
            return
        }
        if (result.status == VerifySessionAgeStatus.PASS) {
            confirmPass()
            return
        }
        if (result.status == VerifySessionAgeStatus.FAIL) {
            finish(KIDStatus.FAIL)
            return
        }
        val challenge = result.challenge ?: run {
            finish(KIDStatus.ERROR, "challenge without an id")
            return
        }
        current = current.copy(step = VerifyAgeStep.EMAIL, challengeId = challenge.challengeId, rawChallengeStatus = RawChallengeStatus.PENDING)
    }

    private fun firePoll() {
        timer = null
        polling = true
        tasks.spawn {
            poll()
            polling = false
        }
    }

    private suspend fun poll() {
        val challengeId = current.challengeId ?: return
        try {
            val status = client.challengeGetStatus(challengeId)
            backoff = interval
            if (handleStatus(status)) schedule(if (stream.active) null else interval)
        } catch (error: Exception) {
            rethrowCancellation(error)
            log(LogLevel.ERROR, "age verification poll failed; will retry $error")
            backoff = minOf(backoff * 2, MAX_POLL_BACKOFF_MS)
            schedule(if (stream.active) null else backoff)
        }
    }

    private suspend fun handleStatus(status: ChallengeStatus): Boolean {
        if (status.status == "PASS") {
            stopTimer()
            confirmPass()
            stream.stop()
            return false
        }
        val expiresAt = status.expiresAt
        if (status.status == "FAIL" || (expiresAt != null && now() > expiresAt)) {
            finish(KIDStatus.FAIL)
            return false
        }
        current = current.copy(
            rawChallengeStatus = if (status.status == "IN_PROGRESS") RawChallengeStatus.IN_PROGRESS else RawChallengeStatus.PENDING,
            expiresAt = status.expiresAt,
        )
        return true
    }

    @KIDInternalApi
    public fun start() {
        // A finished fail or error starts over with a fresh check, so remounting the view tries again.
        if (current.status == KIDStatus.FAIL || (current.status == KIDStatus.ERROR && !confirming)) {
            stopTimer()
            started = true
            backoff = interval
            current = VerifyAgeState(status = KIDStatus.RUNNING, step = VerifyAgeStep.CHECKING)
            tasks.spawn { check() }
            return
        }
        if (started) return
        started = true
        if (!checked) {
            tasks.spawn { check() }
            return
        }
        if (polling || timer != null || stream.active) return
        if (confirming) {
            scheduleConfirm(0)
        } else if (current.status == KIDStatus.PENDING) {
            watch(0)
        }
    }

    @KIDInternalApi
    public fun setEmail(email: String) {
        collectedEmail = email
    }

    @KIDInternalApi
    public suspend fun submit(): KIDResult {
        val challengeId = current.challengeId
        if (challengeId == null || (current.status != KIDStatus.RUNNING && current.status != KIDStatus.PENDING)) {
            log(LogLevel.WARN, "submit() called before a challenge exists; nothing to send.")
            return KIDResult(ok = false, error = localKIDError())
        }
        val email = collectedEmail
        if (email.isNullOrEmpty()) {
            log(LogLevel.WARN, "submit() called without an email; pass one to `verifyAge.submit` or render KIDSessionVerifyAge.")
            return KIDResult(ok = false, error = localKIDError())
        }
        val res = client.challengeSendEmail(challengeId, email)
        if (!res.ok) {
            val error = res.error ?: toKIDError(null)
            log(LogLevel.ERROR, "could not send the verification email $error")
            return KIDResult(ok = false, error = error)
        }
        backoff = interval
        current = current.copy(status = KIDStatus.PENDING, step = null)
        watch(interval)
        return KIDResult(ok = true)
    }

    @KIDInternalApi
    public fun destroy() {
        stopTimer()
        stream.stop()
        started = false
    }
}
