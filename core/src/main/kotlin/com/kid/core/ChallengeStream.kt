package com.kid.core

import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import java.time.Duration
import java.time.Instant

private const val MAX_FAILED_OPENS = 2
private const val RECONNECT_BASE_MS = 1000
private const val RECONNECT_MAX_MS = 30_000

// A stream the server ends sooner than this reconnects with backoff, so a challenge the server already counts as
// expired can't spin while this device's clock still says it is live.
private val MIN_HEALTHY_STREAM: Duration = Duration.ofSeconds(1)

/** A server-sent events parser (the WHATWG event-stream format), fed one byte at a time. */
internal class SseParser {
    data class Event(val event: String, val data: String)

    /** The server's last `retry:` hint, in ms. */
    var retryMs: Int? = null
        private set
    private val line = java.io.ByteArrayOutputStream()
    private var lastWasCR = false
    private var event = ""
    private val data = mutableListOf<String>()

    /** Returns the event a blank line completes. */
    fun push(byte: Byte): Event? {
        val isLF = byte == '\n'.code.toByte()
        val isCR = byte == '\r'.code.toByte()
        val wasCR = lastWasCR
        lastWasCR = isCR
        if (isLF && wasCR) return null
        if (!isLF && !isCR) {
            line.write(byte.toInt())
            return null
        }
        val text = line.toByteArray().decodeToString()
        line.reset()
        return process(text)
    }

    private fun process(text: String): Event? {
        if (text.isEmpty()) {
            val done = if (data.isEmpty()) null else Event(event.ifEmpty { "message" }, data.joinToString("\n"))
            event = ""
            data.clear()
            return done
        }
        if (text.startsWith(":")) return null
        val colon = text.indexOf(':')
        val field = if (colon < 0) text else text.substring(0, colon)
        val value = if (colon < 0) "" else text.substring(colon + 1).removePrefix(" ")
        when (field) {
            "event" -> event = value
            "data" -> data += value
            "retry" -> if (value.isNotEmpty() && value.all { it in '0'..'9' }) value.toIntOrNull()?.let { retryMs = it }
        }
        return null
    }
}

/**
 * Watches a challenge's status over the server's event stream, reconnecting as needed, and falls back to polling when
 * streaming is unavailable (404) or fails to open twice in a row.
 */
internal class ChallengeWatch(
    private val client: KIDClient,
    /** Handles one status; returns false once the challenge needs no more watching. */
    private val onStatus: suspend (ChallengeStatus) -> Boolean,
    /** Streaming is unavailable for this challenge, so the caller polls it instead. */
    private val onFallback: () -> Unit,
    private val now: () -> Instant,
    private val log: KIDLog,
    private val tasks: TaskTracker,
    private val reconnectBaseMs: Int = RECONNECT_BASE_MS,
) {
    private var challengeId: String? = null
    private var fellBack = false
    private var job: Job? = null
    private var generation = 0

    val active: Boolean get() = job != null

    /** Streams the challenge's status. Returns false once it has fallen back to polling, which the caller then runs. */
    fun start(id: String): Boolean {
        if (id != challengeId) {
            stop()
            challengeId = id
            fellBack = false
        }
        if (fellBack) return false
        if (job != null) return true
        generation += 1
        val current = generation
        job = tasks.spawn { run(id, current) }
        return true
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private suspend fun run(id: String, current: Int) {
        var failedOpens = 0
        var backoff = reconnectBaseMs
        fun nextBackoff(): Int = backoff.also { backoff = minOf(backoff * 2, RECONNECT_MAX_MS) }
        fun failOpen(unavailable: Boolean): Int? {
            failedOpens += 1
            if (!unavailable && failedOpens < MAX_FAILED_OPENS) return nextBackoff()
            if (generation != current) return null
            job = null
            fellBack = true
            onFallback()
            return null
        }
        while (generation == current) {
            val openedAt = now()
            var snapshot = false
            val wait = try {
                val end = client.challengeStream(id) { status ->
                    currentCoroutineContext().ensureActive()
                    if (generation != current) return@challengeStream
                    snapshot = true
                    val keepWatching = try {
                        onStatus(status)
                    } catch (error: Exception) {
                        rethrowCancellation(error)
                        // The stream re-sends the status on reconnect, so a failed handling is retried then.
                        log(LogLevel.ERROR, "could not handle a challenge status; will retry $error")
                        true
                    }
                    if (!keepWatching && generation == current) stop()
                }
                // A proxy can pass the headers and hold or drop the body, so an open only counts once a status arrives.
                if (!snapshot) {
                    failOpen(unavailable = false) ?: return
                } else {
                    failedOpens = 0
                    if (end is ChallengeStreamEnd.Closed && Duration.between(openedAt, now()) >= MIN_HEALTHY_STREAM) {
                        backoff = reconnectBaseMs
                        end.retryMs ?: 0
                    } else {
                        nextBackoff()
                    }
                }
            } catch (error: Exception) {
                rethrowCancellation(error)
                failOpen(unavailable = (error as? KIDError)?.code == KIDErrorCode.NOT_FOUND) ?: return
            }
            if (wait > 0) delay(wait.toLong())
        }
    }
}
