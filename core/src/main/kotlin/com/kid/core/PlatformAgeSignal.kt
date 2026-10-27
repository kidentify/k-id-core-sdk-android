package com.kid.core

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import com.google.android.gms.tasks.Task
import com.google.android.play.agesignals.AgeSignalsAccessRequest
import com.google.android.play.agesignals.AgeSignalsManager
import com.google.android.play.agesignals.AgeSignalsManagerFactory
import com.google.android.play.agesignals.AgeSignalsRequest
import com.google.android.play.agesignals.AgeSignalsResult
import com.google.android.play.agesignals.model.AgeRangeSource
import com.google.android.play.agesignals.model.AgeSignalsStatus
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Asks the device's platform for the player's age range, which stands in for the age gate when it is shared. */
@KIDInternalApi
public fun interface PlatformAgeSignalProvider {
    /** Returns null when the platform has no range to give or the person declines to share it. */
    public suspend fun signal(ageGates: List<Int>): PlatformAgeSignal?
}

/**
 * The jurisdiction's minimum, access and civil ages, as the age gates to ask the platform for.
 *
 * A range shorter than two years is rejected, so a minimum age too close to its neighbour is dropped (the check still
 * enforces it). Empty when the ages still cannot make valid gates.
 */
internal fun platformAgeGates(requirements: Requirements): List<Int> {
    fun gates(ages: List<Int?>): List<Int>? {
        val sorted = ages.filterNotNull().filter { it > 0 }.toSortedSet().toList()
        return sorted.takeIf { (listOf(0) + sorted).zip(sorted).all { (low, high) -> high - low >= 2 } }
    }
    return gates(listOf(requirements.minimumAge, requirements.accessAge, requirements.civilAge))
        ?: gates(listOf(requirements.accessAge, requirements.civilAge))
        ?: emptyList()
}

/** Google Play Age Signals, asked from the activity [context] belongs to. */
@KIDInternalApi
public fun defaultPlatformAgeSignalProvider(context: Context): PlatformAgeSignalProvider = GooglePlayAgeSignalProvider({ context.findActivity() })

internal class GooglePlayAgeSignalProvider(
    private val activity: () -> Activity?,
    private val manager: (Activity) -> AgeSignalsManager = { AgeSignalsManagerFactory.create(it.applicationContext) },
    private val foreground: (Activity) -> Boolean = Activity::isInForeground,
) : PlatformAgeSignalProvider {
    // Play takes an app's age gates from Play Console, not the request.
    override suspend fun signal(ageGates: List<Int>): PlatformAgeSignal? {
        val activity = activity()?.takeIf(foreground) ?: return null
        return try {
            val manager = manager(activity)
            val access = manager.requestAgeSignalsAccess(AgeSignalsAccessRequest.builder().setActivity(activity).build()).awaitUnlessBlocked(activity)
            if (access?.ageSignalsStatus() != AgeSignalsStatus.SHARED) return null
            googlePlayAgeSignal(manager.checkAgeSignals(AgeSignalsRequest.builder().build()).await())
        } catch (error: Exception) {
            rethrowCancellation(error)
            null
        }
    }

    // Android silently blocks Play's consent screen from opening in the background, and the access request then never
    // completes. Play posts its answer before the activity can resume from that screen, so access still open when the
    // activity resumes after a stop was blocked. The check opens no screen and is simply awaited.
    private suspend fun <T> Task<T>.awaitUnlessBlocked(activity: Activity): T? = coroutineScope {
        val answer = async { await() }
        val returned = async { activity.returnsFromStop() }
        select<T?> {
            answer.onAwait { it }
            returned.onAwait { if (isComplete) answer.await() else null }
        }.also { coroutineContext.cancelChildren() }
    }
}

private val ageRangeSources = mapOf(
    AgeRangeSource.TIER_A to "TIER_A",
    AgeRangeSource.TIER_B to "TIER_B",
    AgeRangeSource.TIER_C to "TIER_C",
    AgeRangeSource.TIER_D to "TIER_D",
)

internal fun googlePlayAgeSignal(result: AgeSignalsResult): PlatformAgeSignal? {
    val ageLow = result.ageLower() ?: return null
    return PlatformAgeSignal(
        name = "google-play",
        ageLow = ageLow,
        ageHigh = result.ageUpper() ?: -1,
        declarationType = ageRangeSources[result.ageRangeSource()],
    )
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener(Runnable::run) { task ->
        val error = task.exception
        if (error == null) continuation.resume(task.result) else continuation.resumeWithException(error)
    }
}

private suspend fun Activity.returnsFromStop(): Unit = suspendCancellableCoroutine { continuation ->
    val callbacks = object : Application.ActivityLifecycleCallbacks {
        private var stopped = false

        private fun end() {
            unregisterActivityLifecycleCallbacks(this)
            continuation.resume(Unit)
        }

        override fun onActivityStopped(activity: Activity) {
            stopped = true
        }

        override fun onActivityResumed(activity: Activity) {
            if (stopped) end()
        }

        override fun onActivityDestroyed(activity: Activity) = unregisterActivityLifecycleCallbacks(this)

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
        override fun onActivityStarted(activity: Activity) = Unit
        override fun onActivityPaused(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    }
    registerActivityLifecycleCallbacks(callbacks)
    continuation.invokeOnCancellation { unregisterActivityLifecycleCallbacks(callbacks) }
}

private fun Activity.isInForeground(): Boolean {
    if (isFinishing || isDestroyed) return false
    val process = ActivityManager.RunningAppProcessInfo()
    ActivityManager.getMyMemoryState(process)
    return process.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
