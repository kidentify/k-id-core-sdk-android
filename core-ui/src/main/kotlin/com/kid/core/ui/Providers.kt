package com.kid.core.ui

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.LayoutDirection
import com.kid.core.AgeGateController
import com.kid.core.AgeGateInputs
import com.kid.core.AgeSignal
import com.kid.core.AskWatcher
import com.kid.core.ClientOptions
import com.kid.core.JurisdictionCode
import com.kid.core.JurisdictionOption
import com.kid.core.KIDClient
import com.kid.core.KIDDeclare
import com.kid.core.LogLevel
import com.kid.core.SessionStore
import com.kid.core.SettingsController
import com.kid.core.SettingsInputs
import com.kid.core.createClient
import com.kid.core.defaultLog
import com.kid.core.defaultPlatformAgeSignalProvider
import com.kid.core.isLiveKey
import com.kid.core.resumableChallengeId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URI

internal enum class DeclarationStatus { READY, PENDING, ERROR }

/** App-level k-ID state: the copy, the jurisdiction list and the look every view shares. */
public class KIDCoreContext internal constructor(
    internal val client: KIDClient,
    publishableKey: String,
    appearance: Appearance,
    localization: KIDLocalization?,
    declare: KIDDeclare?,
    private val scope: CoroutineScope,
    private val sleep: suspend (Int) -> Unit = { delay(it.toLong()) },
) {
    internal val sessions: SessionStore = SessionStore()
    internal val asks: AskWatcher = AskWatcher(client, scope, sessions).also { it.start() }
    internal var appearance: Appearance by mutableStateOf(appearance)

    /** The merged copy the views render. */
    public var strings: KIDStrings by mutableStateOf(mergeStrings(localization))
        private set

    internal var locale: String? by mutableStateOf(localization?.locale)
        private set

    /** The jurisdiction picker's options, translated and sorted for the locale. */
    public var jurisdictions: List<JurisdictionOption> by mutableStateOf(localizeJurisdictions(strings, locale))
        private set

    private val declareInput = if (isLiveKey(publishableKey)) null else declare

    internal var declarationStatus: DeclarationStatus by mutableStateOf(if (declareInput == null) DeclarationStatus.READY else DeclarationStatus.PENDING)
        private set

    /** Why declare failed, with [declarationStatus] at ERROR. */
    internal var declarationError: Throwable? = null
        private set

    private var started = false

    internal fun setLocalization(localization: KIDLocalization?) {
        strings = mergeStrings(localization)
        locale = localization?.locale
        jurisdictions = localizeJurisdictions(strings, locale)
    }

    internal fun start() {
        val input = declareInput ?: return
        if (started) return
        started = true
        scope.launch {
            var attempt = 1
            while (true) {
                try {
                    client.declare(input)
                    declarationStatus = DeclarationStatus.READY
                    return@launch
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    if (attempt >= MAX_DECLARE_ATTEMPTS || !isRetryableDeclareError(error)) {
                        defaultLog(LogLevel.ERROR, "declare failed $error")
                        declarationError = error
                        declarationStatus = DeclarationStatus.ERROR
                        return@launch
                    }
                    sleep(declareRetryDelayMs(attempt))
                    attempt += 1
                }
            }
        }
    }
}

internal val LocalKIDCore = staticCompositionLocalOf<KIDCoreContext?> { null }
internal val LocalKIDLayoutDirection = staticCompositionLocalOf<LayoutDirection?> { null }
internal val LocalKIDAgeGate = staticCompositionLocalOf<AgeGateController?> { null }
internal val LocalKIDSettings = staticCompositionLocalOf<SettingsController?> { null }

@Composable
internal fun requireCore(): KIDCoreContext = LocalKIDCore.current ?: error("Place this inside a KIDCoreProvider.")

@Composable
internal fun requireAgeGate(): AgeGateController = LocalKIDAgeGate.current ?: error("Place this inside a KIDSessionProvider.")

@Composable
internal fun requireSettings(): SettingsController = LocalKIDSettings.current ?: error("Place this inside a KIDSettingsProvider.")

/**
 * The app-level k-ID provider; wrap your app once.
 *
 * [baseUrl] is the k-ID API URL Kai shows for your key. [declare] is read once, when the provider is first composed,
 * and applied only while you use a test key; a live key skips it.
 */
@Composable
public fun KIDCoreProvider(
    publishableKey: String,
    baseUrl: String,
    appearance: Appearance = Appearance(),
    localization: KIDLocalization? = null,
    declare: KIDDeclare? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val core = remember {
        val options = ClientOptions(
            publishableKey = publishableKey,
            baseUrl = baseUrl,
            appId = context.packageName,
            readImage = { uri -> readAppImage(context, uri) },
        )
        val client = createClient(options)
        KIDCoreContext(client, publishableKey, appearance, localization, declare, scope).also { it.start() }
    }
    LaunchedEffect(appearance) { core.appearance = appearance }
    LaunchedEffect(localization) { core.setLocalization(localization) }
    CompositionLocalProvider(
        LocalKIDCore provides core,
        LocalKIDLayoutDirection provides textDirection(localization?.locale),
        content = content,
    )
}

private suspend fun readAppImage(context: Context, uri: URI): ByteArray = withContext(Dispatchers.IO) {
    val path = uri.path.orEmpty()
    if (uri.scheme == "file" && path.startsWith("/android_asset/")) {
        context.assets.open(path.removePrefix("/android_asset/")).use { it.readBytes() }
    } else {
        context.contentResolver.openInputStream(Uri.parse(uri.toString()))?.use { it.readBytes() } ?: throw IOException("no image at $uri")
    }
}

/**
 * Wraps one age gate. Omit [jurisdiction] to show the picker, [age] to show the age step, [email] to show the email
 * form. The SDK persists nothing: store the ids from [onChallenge] and [onSession] and pass them back to resume.
 * [onClaim] gets a one-time token for your backend once the age check has a result, passed or pending; it is not called
 * on resume or when the player is prohibited.
 *
 * Age and jurisdiction freeze once the check runs; later changes are ignored with a warning.
 */
@Composable
public fun KIDSessionProvider(
    jurisdiction: JurisdictionCode? = null,
    age: AgeSignal? = null,
    email: String? = null,
    challengeId: String? = null,
    sessionId: String? = null,
    onChallenge: ((String) -> Unit)? = null,
    onSession: ((String) -> Unit)? = null,
    onClaim: ((String) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val core = requireCore()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val controller = remember {
        AgeGateController(
            core.client,
            scope,
            AgeGateInputs(jurisdiction?.rawValue, age, email, challengeId, sessionId),
            platformAgeSignal = defaultPlatformAgeSignalProvider(context),
            sessions = core.sessions,
        )
    }
    val state by controller.state.collectAsState()
    val currentOnChallenge by rememberUpdatedState(onChallenge)
    val currentOnSession by rememberUpdatedState(onSession)
    val currentOnClaim by rememberUpdatedState(onClaim)
    var firedChallenge by remember { mutableStateOf<String?>(null) }
    var firedSession by remember { mutableStateOf<String?>(null) }
    var firedClaim by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(jurisdiction, age, email) { controller.configure(jurisdiction?.rawValue, age, email) }
    LaunchedEffect(core.declarationStatus) {
        when (core.declarationStatus) {
            DeclarationStatus.READY -> controller.start()
            DeclarationStatus.ERROR -> controller.declarationFailed(core.declarationError)
            DeclarationStatus.PENDING -> Unit
        }
    }
    DisposableEffect(controller) { onDispose { controller.destroy() } }

    val resumable = resumableChallengeId(state)
    LaunchedEffect(resumable) {
        if (resumable != null && resumable != firedChallenge) {
            firedChallenge = resumable
            currentOnChallenge?.invoke(resumable)
        }
    }
    val established = state.sessionId
    LaunchedEffect(established) {
        if (established != null && established != firedSession) {
            firedSession = established
            currentOnSession?.invoke(established)
        }
    }
    val claimToken = state.claimToken
    LaunchedEffect(claimToken) {
        if (claimToken != null && claimToken != firedClaim) {
            firedClaim = claimToken
            currentOnClaim?.invoke(claimToken)
        }
    }

    CompositionLocalProvider(LocalKIDAgeGate provides controller, content = content)
}

/**
 * Wraps the settings menu for [sessionId], the session from the age gate's `onSession`. [onChallenge] gets the
 * settings challenge id once its email is sent; pass it back as [challengeId] to resume.
 */
@Composable
public fun KIDSettingsProvider(
    sessionId: String,
    challengeId: String? = null,
    onChallenge: ((String) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val core = requireCore()
    val scope = rememberCoroutineScope()
    val currentOnChallenge by rememberUpdatedState(onChallenge)
    val controller = remember {
        SettingsController(
            core.client,
            scope,
            SettingsInputs(sessionId, challengeId),
            onChallenge = { currentOnChallenge?.invoke(it) },
            sessions = core.sessions,
            asks = core.asks,
        )
    }
    LaunchedEffect(controller) { controller.start() }
    DisposableEffect(controller) { onDispose { controller.destroy() } }
    CompositionLocalProvider(LocalKIDSettings provides controller, content = content)
}
