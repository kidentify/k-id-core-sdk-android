package com.kid.core

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant

public enum class KIDStatus(public val rawValue: String) {
    RUNNING("running"),
    PENDING("pending"),
    PASS("pass"),
    PROHIBITED("prohibited"),
    FAIL("fail"),
    ERROR("error"),
}

public enum class RawChallengeStatus(public val rawValue: String) {
    PENDING("pending"),
    IN_PROGRESS("in-progress"),
    PASS("pass"),
    FAIL("fail"),
}

@KIDInternalApi
public enum class AgeGateStep(public val rawValue: String) {
    JURISDICTION("jurisdiction"),
    AGE("age"),
    PARENTAL_CONSENT("parentalConsent"),
    AGE_ASSURANCE("ageAssurance"),
    CHECKING("checking"),
}

/** Who the challenge email goes to: a parent for consent, or the player to verify their own age. */
public enum class KIDChallengeKind(public val rawValue: String) {
    PARENTAL_CONSENT("parentalConsent"),
    AGE_ASSURANCE("ageAssurance"),
}

@KIDInternalApi
public data class AgeGateState(
    val status: KIDStatus,
    val step: AgeGateStep? = null,
    val challengeId: String? = null,
    val challengeKind: KIDChallengeKind? = null,
    val sessionId: String? = null,
    val session: Session? = null,
    /** One-time claim token for this check; null until the check, after a prohibition, and on resume. */
    val claimToken: String? = null,
    val rawChallengeStatus: RawChallengeStatus? = null,
    val expiresAt: Instant? = null,
    val requirements: Requirements? = null,
    val emailProvided: Boolean = false,
    val error: KIDError? = null,
)

@KIDInternalApi
public typealias SetTimer = (fn: () -> Unit, ms: Int) -> Any

@KIDInternalApi
public typealias ClearTimer = (handle: Any) -> Unit

internal fun defaultSetTimer(scope: CoroutineScope): SetTimer = { fn, ms ->
    scope.launch {
        delay(ms.coerceAtLeast(0).toLong())
        fn()
    }
}

internal val defaultClearTimer: ClearTimer = { handle -> (handle as? Job)?.cancel() }

@KIDInternalApi
public enum class LogLevel { WARN, ERROR }

@KIDInternalApi
public typealias KIDLog = (LogLevel, String) -> Unit

@KIDInternalApi
public val defaultLog: KIDLog = { level, message ->
    if (level == LogLevel.WARN) Log.w("k-ID", message) else Log.e("k-ID", message)
}

/** The controller's in-flight work, so a test (or a teardown) can tell when it has all run. */
@KIDInternalApi
public class TaskTracker(private val scope: CoroutineScope) {
    private val jobs = mutableSetOf<Job>()

    public fun spawn(work: suspend () -> Unit): Job {
        val job = scope.launch { work() }
        if (!job.isCompleted) {
            jobs += job
            job.invokeOnCompletion { jobs -= job }
        }
        return job
    }

    public val isIdle: Boolean get() = jobs.isEmpty()
}

internal fun rethrowCancellation(error: Exception) {
    if (error is CancellationException) throw error
}
