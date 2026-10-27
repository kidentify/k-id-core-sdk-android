package com.kid.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Duration
import java.time.Instant

private const val MIN_POLL_INTERVAL_MS = 30_000
private const val MAX_POLL_BACKOFF_MS = 300_000
private const val MAX_CHECK_ATTEMPTS = 3
private const val CHECK_RETRY_MS = 1000

@KIDInternalApi
public data class AgeGateInputs(
    val jurisdiction: String? = null,
    val age: AgeSignal? = null,
    val email: String? = null,
    val challengeId: String? = null,
    val sessionId: String? = null,
)

private const val CONSENT_TYPE_MARKER = "PARENTAL_CONSENT"

// Deliberately excludes CHALLENGE_SESSION_UPGRADE_BY_AGE_ASSURANCE, which also contains "AGE_ASSURANCE".
private const val ASSURANCE_TYPE_MARKER = "AGE_GATE_AGE_ASSURANCE"

private fun isConsentChallenge(type: String?): Boolean = type.isNullOrEmpty() || type.contains(CONSENT_TYPE_MARKER)

private fun isAssuranceChallenge(type: String?): Boolean = type?.contains(ASSURANCE_TYPE_MARKER) == true

private fun challengeKindOf(type: String?): KIDChallengeKind? = when {
    isConsentChallenge(type) -> KIDChallengeKind.PARENTAL_CONSENT
    isAssuranceChallenge(type) -> KIDChallengeKind.AGE_ASSURANCE
    else -> null
}

/** The challenge id safe to resume from: only once its email is sent. */
@KIDInternalApi
public fun resumableChallengeId(state: AgeGateState): String? = if (state.status == KIDStatus.PENDING) state.challengeId else null

internal fun millisUntil(from: Instant, to: Instant): Long = Duration.between(from, to).toMillis()

/**
 * Drives the age gate: jurisdiction, age, the check, and any consent or age assurance challenge, through to a session.
 *
 * Read it through [ageGate], [session], [permission] and [challenge].
 */
public class AgeGateController @KIDInternalApi constructor(
    private val client: KIDClient,
    scope: CoroutineScope,
    private val initial: AgeGateInputs = AgeGateInputs(),
    private val platformAgeSignal: PlatformAgeSignalProvider? = null,
    private val sessions: SessionStore? = null,
    pollIntervalMs: Int? = null,
    private val now: () -> Instant = Instant::now,
    setTimer: SetTimer? = null,
    clearTimer: ClearTimer? = null,
    log: KIDLog? = null,
) {
    private val setTimer: SetTimer = setTimer ?: defaultSetTimer(scope)
    private val clearTimer: ClearTimer = clearTimer ?: defaultClearTimer
    private val log: KIDLog = log ?: defaultLog
    private val interval = maxOf(MIN_POLL_INTERVAL_MS, pollIntervalMs ?: MIN_POLL_INTERVAL_MS)
    private val resuming = initial.challengeId != null || initial.sessionId != null

    @KIDInternalApi
    public val tasks: TaskTracker = TaskTracker(scope)

    private var propJurisdiction = initial.jurisdiction
    private var propAge = initial.age
    private var propEmail = initial.email
    private var collectedJurisdiction: String? = null
    private var collectedAge: AgeSignal? = null
    private var collectedEmail: String? = null

    private var locked = resuming
    private var lockedJurisdiction = initial.jurisdiction
    private var lockedAge = initial.age
    private var checked = resuming
    private var checking = false
    private var started = false
    private var resumeStarted = false
    private var polling = false
    private var requirementsFetched = false
    private var autoResolveFailed = false
    private var checkAttempts = 0
    private var warnedLock = false
    private var etag: String? = null
    private var backoff = interval
    private var timer: Any? = null
    private var verifyAge: Pair<String, VerifyAgeController>? = null
    private var platformAsked = false
    private var askingPlatform = false
    private var platformSignal: PlatformAgeSignal? = null
    private var sessionsSubscription: Any? = null
    private var fallbackDelay = 0
    private val stream by lazy {
        ChallengeWatch(
            client = client,
            onStatus = { status ->
                val pending = handleStatus(status)
                if (pending) scheduleWithinDeadline(null)
                pending
            },
            onFallback = { scheduleWithinDeadline(fallbackDelay) },
            now = now,
            log = this.log,
            tasks = tasks,
        )
    }

    private val mutableState = MutableStateFlow(
        AgeGateState(
            status = if (initial.challengeId != null) KIDStatus.PENDING else KIDStatus.RUNNING,
            challengeId = initial.challengeId,
            sessionId = initial.sessionId,
            rawChallengeStatus = if (initial.challengeId != null) RawChallengeStatus.PENDING else null,
            emailProvided = initial.email != null,
        ),
    )

    @KIDInternalApi
    public val state: StateFlow<AgeGateState> = mutableState.asStateFlow()

    init {
        mutableState.value = mutableState.value.copy(step = computeStep(mutableState.value))
    }

    private val current: AgeGateState get() = mutableState.value

    private fun effectiveJurisdiction(): String? = if (locked) lockedJurisdiction else collectedJurisdiction ?: propJurisdiction

    private fun effectiveAge(): AgeSignal? = if (locked) lockedAge else propAge ?: collectedAge

    private fun effectiveEmail(): String? = propEmail ?: collectedEmail

    private fun jurisdictionArg(): String? = effectiveJurisdiction()?.takeIf { it.isNotEmpty() && !it.equals("auto", ignoreCase = true) }

    private fun needJurisdiction(): Boolean {
        if (jurisdictionArg() != null) return false
        return effectiveJurisdiction() == null || autoResolveFailed
    }

    private fun computeStep(state: AgeGateState): AgeGateStep? {
        if (state.status != KIDStatus.RUNNING) return null
        if (needJurisdiction()) return AgeGateStep.JURISDICTION
        if (state.challengeId != null) {
            return if (state.challengeKind == KIDChallengeKind.AGE_ASSURANCE) AgeGateStep.AGE_ASSURANCE else AgeGateStep.PARENTAL_CONSENT
        }
        // The age step waits while the platform may still supply the age instead.
        if (!checked && effectiveAge() == null && platformSignal == null) {
            return if (platformAgeSignal != null && !platformAsked) AgeGateStep.CHECKING else AgeGateStep.AGE
        }
        return AgeGateStep.CHECKING
    }

    private fun commit(patch: (AgeGateState) -> AgeGateState = { it }) {
        val prev = current.session
        val next = patch(current)
        mutableState.value = next.copy(step = computeStep(next))
        syncVerifyAge()
        current.session?.takeIf { it != prev }?.let { sessions?.publish(it) }
    }

    private fun adoptSession(session: Session) {
        if (session == current.session || session.sessionId != current.sessionId) return
        etag = null
        commit { it.copy(session = session) }
    }

    // A restarted gate missed what was published while it was stopped.
    private fun catchUpSession() {
        if (current.session == null) return
        val sessionId = current.sessionId ?: return
        sessions?.current(sessionId)?.let(::adoptSession)
    }

    private fun syncVerifyAge() {
        val sessionId = current.sessionId
        if (current.status != KIDStatus.PASS || sessionId == null || verifyAge?.first == sessionId) return
        verifyAge?.second?.destroy()
        verifyAge = sessionId to VerifyAgeController(
            client = client,
            sessionId = sessionId,
            civilAge = ::sessionCivilAge,
            onSession = { session ->
                etag = null
                commit { it.copy(session = session) }
            },
            pollIntervalMs = interval,
            now = now,
            setTimer = setTimer,
            clearTimer = clearTimer,
            log = log,
            tasks = tasks,
        )
    }

    /** Verifies the player is an adult on the established session; null until the gate passes. Nothing is sent until its `start()`. */
    @KIDInternalApi
    public fun sessionVerifyAge(): VerifyAgeController? = verifyAge?.second

    private suspend fun sessionCivilAge(): Int? {
        current.requirements?.let { return it.civilAge }
        val jurisdiction = current.session?.jurisdiction ?: return null
        val requirements = client.getRequirements(jurisdiction)
        commit { it.copy(requirements = requirements) }
        return requirements.civilAge
    }

    // A resumed session never ran the gate's requirements fetch; fetch them for its jurisdiction.
    private fun fetchSessionRequirements(session: Session?) {
        val jurisdiction = session?.jurisdiction
        if (current.requirements != null || jurisdiction == null) return
        tasks.spawn {
            try {
                val requirements = client.getRequirements(jurisdiction)
                commit { it.copy(requirements = requirements) }
            } catch (error: Exception) {
                rethrowCancellation(error)
                log(LogLevel.ERROR, "could not load the requirements $error")
            }
        }
    }

    private fun stopTimer() {
        timer?.let(clearTimer)
        timer = null
    }

    /** A null delay arms no poll. */
    private fun schedule(delay: Int?) {
        if (!started) return
        stopTimer()
        if (delay == null) return
        timer = setTimer({ firePoll() }, delay)
    }

    private fun watch(delay: Int) {
        val challengeId = current.challengeId
        if (!started || challengeId == null) return
        fallbackDelay = delay
        scheduleWithinDeadline(if (stream.start(challengeId)) null else delay)
    }

    private fun rearm() {
        if (polling || timer != null || stream.active || current.challengeId == null || current.status != KIDStatus.PENDING) return
        backoff = interval
        watch(0)
    }

    /** A null delay arms only the deadline. */
    private fun scheduleWithinDeadline(delay: Int?) {
        var next = delay
        current.expiresAt?.let { expiresAt ->
            val remaining = millisUntil(now(), expiresAt)
            if (remaining <= 0) {
                commit { it.copy(rawChallengeStatus = RawChallengeStatus.FAIL) }
                fail(KIDStatus.FAIL, "challenge expired")
                return
            }
            next = minOf(next?.toLong() ?: remaining, remaining).toInt()
        }
        schedule(next)
    }

    private fun maybeFetchRequirements() {
        if (!started || requirementsFetched) return
        if (jurisdictionArg() == null && (effectiveJurisdiction() == null || autoResolveFailed)) return
        requirementsFetched = true
        val jurisdiction = jurisdictionArg()
        tasks.spawn {
            try {
                val requirements = client.getRequirements(jurisdiction)
                commit { it.copy(requirements = requirements) }
                maybeAskPlatform(requirements)
                maybeCheck()
            } catch (error: Exception) {
                rethrowCancellation(error)
                if (jurisdictionArg() == null && (error as? KIDError)?.rawCode == "CANNOT_RESOLVE_JURISDICTION") {
                    autoResolveFailed = true
                    requirementsFetched = false
                    commit()
                    return@spawn
                }
                fail(KIDStatus.ERROR, error)
            }
        }
    }

    private fun maybeAskPlatform(requirements: Requirements) {
        val provider = platformAgeSignal
        if (provider == null || platformAsked) return
        platformAsked = true
        val gates = platformAgeGates(requirements)
        if (effectiveAge() != null || "platform-account" !in requirements.approvedAgeCollectionMethods || gates.isEmpty()) {
            commit()
            return
        }
        askingPlatform = true
        tasks.spawn {
            if (client.platformAgeSignalTrusted()) {
                platformSignal = provider.signal(gates)
            }
            askingPlatform = false
            commit()
            maybeCheck()
        }
    }

    private fun maybeCheck() {
        if (!started || checked || checking || resuming || askingPlatform) return
        if (effectiveAge() == null && platformSignal == null) return
        if (current.requirements == null) return
        runCheck()
    }

    private fun runCheck() {
        lockedJurisdiction = effectiveJurisdiction()
        lockedAge = effectiveAge()
        checking = true
        locked = true
        commit { it.copy(status = KIDStatus.RUNNING) }
        val age = lockedAge
        val signal = platformSignal
        if (age == null && signal == null) return
        val jurisdiction = jurisdictionArg()
        tasks.spawn { check(age, signal, jurisdiction) }
    }

    private suspend fun check(age: AgeSignal?, signal: PlatformAgeSignal?, jurisdiction: String?) {
        val res = try {
            client.ageGateCheck(age, signal, jurisdiction)
        } catch (error: Exception) {
            rethrowCancellation(error)
            checkFailed(error, transient = (error as? KIDError)?.let { it.status >= 500 } ?: true)
            return
        }
        checked = true
        checking = false
        when (res.status) {
            "PROHIBITED" -> {
                commit { it.copy(status = KIDStatus.PROHIBITED, session = null, challengeId = null) }
                return
            }
            "PASS" -> {
                commit {
                    it.copy(
                        status = KIDStatus.PASS,
                        session = res.session,
                        sessionId = res.session?.sessionId,
                        challengeId = null,
                        claimToken = res.claimToken,
                    )
                }
                return
            }
        }
        val challenge = res.challenge
        val kind = challenge?.let { challengeKindOf(it.type) }
        if (challenge != null && kind != null) {
            commit {
                it.copy(
                    status = KIDStatus.RUNNING,
                    challengeId = challenge.challengeId,
                    challengeKind = kind,
                    rawChallengeStatus = RawChallengeStatus.PENDING,
                    claimToken = res.claimToken,
                )
            }
            return
        }
        fail(KIDStatus.ERROR, challenge?.type?.let { "unsupported challenge type $it" } ?: "challenge without an id")
    }

    private fun checkFailed(error: Throwable, transient: Boolean) {
        checking = false
        checkAttempts += 1
        if (transient && checkAttempts < MAX_CHECK_ATTEMPTS) {
            if (!started) return
            stopTimer()
            timer = setTimer({ runCheck() }, CHECK_RETRY_MS)
            return
        }
        checked = true
        fail(KIDStatus.ERROR, error)
    }

    private fun fail(status: KIDStatus, detail: Any) {
        if (status == KIDStatus.ERROR) log(LogLevel.ERROR, "age gate error $detail")
        stopTimer()
        stream.stop()
        commit { it.copy(status = status, error = if (status == KIDStatus.ERROR) toKIDError(detail) else null) }
    }

    @KIDInternalApi
    public suspend fun submit(): KIDResult {
        val challengeId = current.challengeId ?: run {
            log(LogLevel.WARN, "submit() called before a challenge exists; nothing to send.")
            return KIDResult(ok = false, error = localKIDError())
        }
        val email = effectiveEmail()
        if (email.isNullOrEmpty()) {
            log(LogLevel.WARN, "submit() called without an email; provide the `email` input or render KIDAskAnAdult / KIDVerifyAge.")
            return KIDResult(ok = false, error = localKIDError())
        }
        val res = client.challengeSendEmail(challengeId, email)
        if (res.ok) {
            backoff = interval
            commit { it.copy(status = KIDStatus.PENDING, rawChallengeStatus = RawChallengeStatus.PENDING) }
            watch(interval)
            return KIDResult(ok = true)
        }
        val error = res.error ?: toKIDError(null)
        log(LogLevel.ERROR, "could not send the challenge email $error")
        return KIDResult(ok = false, error = error)
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
            if (handleStatus(status)) scheduleWithinDeadline(if (stream.active) null else interval)
        } catch (error: Exception) {
            rethrowCancellation(error)
            log(LogLevel.ERROR, "poll failed; will retry $error")
            backoff = minOf(backoff * 2, MAX_POLL_BACKOFF_MS)
            scheduleWithinDeadline(if (stream.active) null else backoff)
        }
    }

    private suspend fun handleStatus(status: ChallengeStatus): Boolean {
        val kind = challengeKindOf(status.type)
        if (current.challengeKind == null && status.type != null && kind != null) {
            commit { it.copy(challengeKind = kind) }
        }
        if (status.status == "PASS") {
            val sessionId = status.sessionId ?: current.sessionId
            var session = current.session
            if (sessionId != null) {
                val read = client.sessionGet(sessionId, null)
                etag = read.etag
                session = read.session ?: session
            }
            stopTimer()
            stream.stop()
            commit { it.copy(status = KIDStatus.PASS, rawChallengeStatus = RawChallengeStatus.PASS, session = session, sessionId = sessionId) }
            fetchSessionRequirements(session)
            return false
        }
        if (status.status == "FAIL") {
            commit { it.copy(rawChallengeStatus = RawChallengeStatus.FAIL) }
            fail(KIDStatus.FAIL, "challenge failed")
            return false
        }
        val expiresAt = status.expiresAt
        if (expiresAt != null && now() > expiresAt) {
            commit { it.copy(rawChallengeStatus = RawChallengeStatus.FAIL) }
            fail(KIDStatus.FAIL, "challenge expired")
            return false
        }
        commit {
            it.copy(
                rawChallengeStatus = if (status.status == "IN_PROGRESS") RawChallengeStatus.IN_PROGRESS else RawChallengeStatus.PENDING,
                expiresAt = status.expiresAt,
            )
        }
        return true
    }

    private fun resume() {
        initial.sessionId?.let { sessionId ->
            tasks.spawn { resumeSession(sessionId) }
            return
        }
        if (initial.challengeId != null) {
            backoff = interval
            watch(0)
        }
    }

    private suspend fun resumeSession(sessionId: String) {
        try {
            val read = client.sessionGet(sessionId, null)
            etag = read.etag
            val session = read.session
            if (session != null) {
                commit { it.copy(status = KIDStatus.PASS, rawChallengeStatus = RawChallengeStatus.PASS, session = session, sessionId = sessionId) }
                fetchSessionRequirements(session)
                return
            }
            fail(KIDStatus.ERROR, "resumed session not found")
        } catch (error: Exception) {
            rethrowCancellation(error)
            fail(KIDStatus.ERROR, error)
        }
    }

    @KIDInternalApi
    public fun start() {
        if (started) return
        started = true
        if (sessionsSubscription == null) sessionsSubscription = sessions?.subscribe(::adoptSession)
        catchUpSession()
        if (resuming) {
            if (resumeStarted) {
                rearm()
            } else {
                resumeStarted = true
                resume()
            }
            return
        }
        maybeFetchRequirements()
        maybeCheck()
        rearm()
    }

    @KIDInternalApi
    public fun configure(jurisdiction: String?, age: AgeSignal?, email: String?) {
        if (resuming) return
        propEmail = email
        if (locked) {
            val jurisdictionChanged = jurisdiction != null && jurisdiction != lockedJurisdiction
            val ageChanged = age != null && age != lockedAge
            if ((jurisdictionChanged || ageChanged) && !warnedLock) {
                warnedLock = true
                log(LogLevel.WARN, "age/jurisdiction ignored: an active challenge already exists for this session.")
            }
            commit { it.copy(emailProvided = email != null) }
            return
        }
        propJurisdiction = jurisdiction
        propAge = age
        maybeFetchRequirements()
        maybeCheck()
        commit { it.copy(emailProvided = email != null) }
    }

    @KIDInternalApi
    public fun setJurisdiction(code: String) {
        if (locked) return
        collectedJurisdiction = code
        maybeFetchRequirements()
        maybeCheck()
    }

    @KIDInternalApi
    public fun setAge(signal: AgeSignal) {
        if (locked) return
        collectedAge = signal
        maybeCheck()
    }

    @KIDInternalApi
    public fun setEmail(email: String) {
        collectedEmail = email
    }

    @KIDInternalApi
    public suspend fun refreshSession() {
        val sessionId = current.sessionId ?: return
        val read = try {
            client.sessionGet(sessionId, etag)
        } catch (error: Exception) {
            rethrowCancellation(error)
            throw toKIDError(error)
        }
        if (read.notModified) return
        etag = read.etag
        read.session?.let { session -> commit { it.copy(session = session) } }
    }

    @KIDInternalApi
    public fun declarationFailed(error: Throwable?) {
        commit { it.copy(status = KIDStatus.ERROR, error = toKIDError(error)) }
    }

    @KIDInternalApi
    public fun destroy() {
        verifyAge?.second?.destroy()
        stopTimer()
        sessionsSubscription?.let { sessions?.unsubscribe(it) }
        sessionsSubscription = null
        stream.stop()
        started = false
    }
}
