package com.kid.core.ui

import com.kid.core.KIDError
import com.kid.core.KIDErrorCode

internal const val MAX_DECLARE_ATTEMPTS = 4

private const val BASE_DELAY_MS = 250
private const val MAX_DELAY_MS = 2000

private val retryableCodes = setOf(KIDErrorCode.UNAVAILABLE, KIDErrorCode.RATE_LIMITED)

internal fun isRetryableDeclareError(error: Throwable?): Boolean = (error as? KIDError)?.code in retryableCodes

internal fun declareRetryDelayMs(attempt: Int): Int = minOf(BASE_DELAY_MS shl (attempt - 1).coerceIn(0, 16), MAX_DELAY_MS)
