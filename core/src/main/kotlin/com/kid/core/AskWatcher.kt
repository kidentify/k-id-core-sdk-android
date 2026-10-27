package com.kid.core

import kotlinx.coroutines.CoroutineScope
import java.time.Instant

private const val MIN_POLL_INTERVAL_MS = 30_000
private const val MAX_POLL_BACKOFF_MS = 300_000

/** A grownup ask that was emailed and is waiting on an answer; [Status.ERROR] once it failed or expired. */
@KIDInternalApi
public data class PendingAsk(
    val status: Status,
    val challengeId: String,
    val permissions: List<PermissionName>,
    val challengeKind: KIDChallengeKind? = null,
) {
    public enum class Status { PENDING, ERROR }
}

/** The kind of a settings challenge, from its server type; null while the type is unknown. */
internal fun settingsChallengeKindOf(type: String?): KIDChallengeKind? = when {
    type == null -> null
    type == "CHALLENGE_SESSION_UPGRADE_BY_AGE_ASSURANCE" -> KIDChallengeKind.AGE_ASSURANCE
    type == "CHALLENGE_SESSION_UPGRADE" || type.contains("PARENTAL_CONSENT") -> KIDChallengeKind.PARENTAL_CONSENT
    else -> null
}

/**
 * Watches each session's pending grownup ask until it is answered, so a grant reaches the session store whether or not
 * a settings screen is mounted.
 */
@KIDInternalApi
public class AskWatcher(
    private val client: KIDClient,
    scope: CoroutineScope,
    private val sessions: SessionStore? = null,
    pollIntervalMs: Int? = null,
    private val now: () -> Instant = Instant::now,
    setTimer: SetTimer? = null,
    clearTimer: ClearTimer? = null,
    log: KIDLog? = null,
    public val tasks: TaskTracker = TaskTracker(scope),
) {
    private class Polling(var backoff: Int, var timer: Any? = null, var inFlight: Boolean = false, var granted: Boolean = false)

    private val setTimer: SetTimer = setTimer ?: defaultSetTimer(scope)
    private val clearTimer: ClearTimer = clearTimer ?: defaultClearTimer
    private val log: KIDLog = log ?: defaultLog
    private val interval = maxOf(MIN_POLL_INTERVAL_MS, pollIntervalMs ?: MIN_POLL_INTERVAL_MS)

    private var started = false
    private val asks = mutableMapOf<String, PendingAsk>()
    private val polling = mutableMapOf<String, Polling>()
    private val rereads = mutableMapOf<String, Pair<Any, suspend () -> Boolean>>()
    private val listeners = mutableMapOf<Any, () -> Unit>()
    private val streams = mutableMapOf<String, ChallengeWatch>()
    private val streamed = mutableMapOf<String, String>()
    private val fallbackDelays = mutableMapOf<String, Int>()

    public fun get(sessionId: String): PendingAsk? = asks[sessionId]

    public fun subscribe(listener: () -> Unit): Any {
        val token = Any()
        listeners[token] = listener
        return token
    }

    public fun unsubscribe(token: Any) {
        listeners.remove(token)
    }

    private fun emit() {
        listeners.values.toList().forEach { it() }
    }

    private fun pollingFor(sessionId: String): Polling = polling.getOrPut(sessionId) { Polling(interval) }

    /**
     * Watches [challengeId], replacing the session's earlier ask; without a stream it polls after [delay] ms (one poll
     * interval by default).
     */
    public fun watch(
        sessionId: String,
        challengeId: String,
        permissions: List<PermissionName>,
        delay: Int? = null,
        challengeKind: KIDChallengeKind? = null,
    ) {
        asks[sessionId] = PendingAsk(PendingAsk.Status.PENDING, challengeId, permissions, challengeKind)
        pollingFor(sessionId).let {
            it.backoff = interval
            it.granted = false
            stopTimer(it)
        }
        emit()
        watchAsk(sessionId, delay ?: interval)
    }

    public fun clear(sessionId: String) {
        polling[sessionId]?.let(::stopTimer)
        streams[sessionId]?.stop()
        if (asks.remove(sessionId) != null) emit()
    }

    /**
     * While attached, a granted ask is re-read through [reread], so a mounted settings screen keeps its own read.
     * [reread] returns false when the read failed, to be retried.
     */
    public fun attach(sessionId: String, reread: suspend () -> Boolean): Any {
        val token = Any()
        rereads[sessionId] = token to reread
        return token
    }

    public fun detach(sessionId: String, token: Any) {
        if (rereads[sessionId]?.first === token) rereads.remove(sessionId)
    }

    public fun start() {
        if (started) return
        started = true
        for (sessionId in asks.keys.toList()) {
            if (!pollingFor(sessionId).inFlight && streams[sessionId]?.active != true) watchAsk(sessionId, 0)
        }
    }

    public fun stop() {
        polling.values.forEach(::stopTimer)
        streams.values.forEach(ChallengeWatch::stop)
        started = false
    }

    private fun watchAsk(sessionId: String, delay: Int) {
        val ask = asks[sessionId] ?: return
        if (!started || ask.status != PendingAsk.Status.PENDING) return
        fallbackDelays[sessionId] = delay
        streamed[sessionId] = ask.challengeId
        if (!streamFor(sessionId).start(ask.challengeId)) schedule(sessionId, delay)
    }

    private fun streamFor(sessionId: String): ChallengeWatch = streams.getOrPut(sessionId) {
        ChallengeWatch(
            client = client,
            onStatus = { status -> streamed[sessionId]?.let { handleStatus(sessionId, it, status) } ?: false },
            onFallback = { schedule(sessionId, fallbackDelays[sessionId] ?: interval) },
            now = now,
            log = log,
            tasks = tasks,
        )
    }

    // A poll or stream result for an ask the session has since replaced is dropped, so it never lands on the new one.
    private fun isCurrent(sessionId: String, challengeId: String): Boolean = asks[sessionId]?.challengeId == challengeId

    private fun stopTimer(p: Polling) {
        p.timer?.let(clearTimer)
        p.timer = null
    }

    private fun schedule(sessionId: String, delay: Int) {
        if (!started) return
        val p = pollingFor(sessionId)
        stopTimer(p)
        p.timer = setTimer({ firePoll(sessionId) }, delay)
    }

    private fun firePoll(sessionId: String) {
        val p = pollingFor(sessionId)
        p.timer = null
        p.inFlight = true
        tasks.spawn {
            try {
                poll(sessionId, p)
            } finally {
                p.inFlight = false
            }
        }
    }

    private suspend fun reread(sessionId: String): Boolean {
        val attached = rereads[sessionId]
        if (attached != null) return attached.second()
        return try {
            client.sessionGet(sessionId, null).session?.let { sessions?.publish(it) }
            true
        } catch (error: Exception) {
            rethrowCancellation(error)
            log(LogLevel.ERROR, "could not load the session $error")
            false
        }
    }

    // The grant only counts once the granted session reached the store, so a failed read is retried, not dropped.
    private suspend fun finishGranted(sessionId: String, challengeId: String, p: Polling) {
        p.granted = true
        val read = reread(sessionId)
        if (!isCurrent(sessionId, challengeId)) return
        if (!read) {
            p.backoff = minOf(p.backoff * 2, MAX_POLL_BACKOFF_MS)
            schedule(sessionId, p.backoff)
            return
        }
        p.granted = false
        asks.remove(sessionId)
        emit()
    }

    private suspend fun poll(sessionId: String, p: Polling) {
        val ask = asks[sessionId] ?: return
        if (ask.status != PendingAsk.Status.PENDING) return
        if (p.granted) return finishGranted(sessionId, ask.challengeId, p)
        try {
            val status = client.challengeGetStatus(ask.challengeId)
            if (handleStatus(sessionId, ask.challengeId, status)) schedule(sessionId, interval)
        } catch (error: Exception) {
            rethrowCancellation(error)
            log(LogLevel.ERROR, "could not check for the grownup's answer; will retry $error")
            if (!isCurrent(sessionId, ask.challengeId)) return
            p.backoff = minOf(p.backoff * 2, MAX_POLL_BACKOFF_MS)
            schedule(sessionId, p.backoff)
        }
    }

    private suspend fun handleStatus(sessionId: String, challengeId: String, status: ChallengeStatus): Boolean {
        val ask = asks[sessionId] ?: return false
        if (ask.challengeId != challengeId || ask.status != PendingAsk.Status.PENDING) return false
        val p = pollingFor(sessionId)
        p.backoff = interval
        val asked = status.permissionNames
        if (ask.permissions.isEmpty() && !asked.isNullOrEmpty()) {
            asks[sessionId] = ask.copy(permissions = asked)
            emit()
        }
        val kind = settingsChallengeKindOf(status.type)
        if (kind != null && kind != ask.challengeKind) {
            asks[sessionId]?.let { asks[sessionId] = it.copy(challengeKind = kind) }
            emit()
        }
        if (status.status == "PASS") {
            finishGranted(sessionId, challengeId, p)
            return false
        }
        val expiresAt = status.expiresAt
        if (status.status == "FAIL" || (expiresAt != null && now() > expiresAt)) {
            asks[sessionId]?.let { asks[sessionId] = it.copy(status = PendingAsk.Status.ERROR) }
            emit()
            return false
        }
        return true
    }
}
