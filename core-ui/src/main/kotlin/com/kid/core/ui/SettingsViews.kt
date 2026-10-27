package com.kid.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kid.core.KIDChallengeKind
import com.kid.core.KIDErrorCode
import com.kid.core.KIDResult
import com.kid.core.ManagedBy
import com.kid.core.Permission
import com.kid.core.PermissionName
import com.kid.core.SettingsChallengeState
import com.kid.core.SettingsChallengeStatus
import com.kid.core.permissionSettings
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal enum class VerifyAgeStep { VERIFY, EMAIL, SENDING }

/**
 * Where a Verify age row goes once its own request returns. A failure puts it back to its button with the error, so the
 * player starts over; without an email, INVALID_REQUEST is the age check asking for the player's email instead.
 */
internal fun verifyAgeStep(res: KIDResult, sentEmail: Boolean): Pair<VerifyAgeStep, KIDErrorCode?> {
    if (res.ok) return VerifyAgeStep.VERIFY to null
    val code = res.error?.code ?: KIDErrorCode.UNAVAILABLE
    if (!sentEmail && code == KIDErrorCode.INVALID_REQUEST) return VerifyAgeStep.EMAIL to null
    return VerifyAgeStep.VERIFY to code
}

/** The grownup form after its own ask: a failure stages the asked permissions again, with the error. */
internal fun grownupAskReset(res: KIDResult, asked: List<PermissionName>): Pair<List<PermissionName>, KIDErrorCode?> =
    if (res.ok) emptyList<PermissionName>() to null else asked to (res.error?.code ?: KIDErrorCode.UNAVAILABLE)

/** The grownup form's error: its own failed ask, or a parental-consent challenge the watcher saw fail. */
internal fun grownupFailed(askFailed: Boolean, challenge: SettingsChallengeState): Boolean =
    askFailed || (challenge.status == SettingsChallengeStatus.ERROR && challenge.challengeKind == KIDChallengeKind.PARENTAL_CONSENT)

/**
 * The grownup form's staged set after a challenge change: a failed parental-consent ask is staged again. It never touches
 * the form's own failure flag, because this runs on a composition snapshot that can be older than the ask's result.
 */
internal fun grownupRestage(challenge: SettingsChallengeState, staged: List<PermissionName>): List<PermissionName> =
    if (challenge.status == SettingsChallengeStatus.ERROR && challenge.challengeKind == KIDChallengeKind.PARENTAL_CONSENT &&
        staged.isEmpty()
    ) {
        challenge.permissions
    } else {
        staged
    }

@Composable
private fun PlayerRow(permission: Permission) {
    val core = requireCore()
    val settings = requireSettings()
    val t = core.strings.settings
    val a = core.appearance
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val name = format(t.enabledLabel, mapOf("permission" to core.strings.permissionName(permission.name)))
    KIDElement(a, ElementName.ROW, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KIDElement(a, ElementName.LABEL, modifier = Modifier.weight(1f)) { KIDText(name) }
            KIDElement(a, ElementName.CONTROL_COLUMN) {
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        KIDElement(a, ElementName.MUTED) { KIDText(if (permission.enabled) t.on else t.off) }
                        Switch(
                            checked = permission.enabled,
                            enabled = !busy,
                            onCheckedChange = { enabled ->
                                scope.launch {
                                    busy = true
                                    failed = !settings.change(permission.permissionName, enabled).ok
                                    busy = false
                                }
                            },
                            colors = SwitchDefaults.colors(checkedTrackColor = a.variables.colorPrimary),
                            modifier = Modifier.semantics { contentDescription = name },
                        )
                    }
                    if (failed) KIDElement(a, ElementName.DANGER) { KIDText(t.error) }
                }
            }
        }
    }
}

// The challenge comes in as a parameter, not read from the controller here: Compose skips a row whose parameters are
// unchanged, so a kind or permissions filled in later by the stream would otherwise never reach it.
@Composable
private fun VerifyAgeRow(permission: Permission, challenge: SettingsChallengeState, disabled: Boolean) {
    val core = requireCore()
    val settings = requireSettings()
    val s = settings.permissionSettings
    val strings = core.strings
    val t = strings.settings
    val a = core.appearance
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf(VerifyAgeStep.VERIFY) }
    var failedCode by remember { mutableStateOf<KIDErrorCode?>(null) }
    var email by remember { mutableStateOf("") }
    val name = format(t.enabledLabel, mapOf("permission" to strings.permissionName(permission.name)))
    val mine = challenge.challengeKind == KIDChallengeKind.AGE_ASSURANCE && permission.permissionName in challenge.permissions

    suspend fun request(playerEmail: String?): KIDResult {
        step = VerifyAgeStep.SENDING
        failedCode = null
        val res = s.upgrade(listOf(permission.permissionName), playerEmail)
        val (next, failed) = verifyAgeStep(res, playerEmail != null)
        step = next
        failedCode = failed
        return res
    }

    if (step == VerifyAgeStep.SENDING || (mine && challenge.status == SettingsChallengeStatus.PENDING)) {
        KIDElement(a, ElementName.ROW, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KIDElement(a, ElementName.LABEL, modifier = Modifier.weight(1f)) { KIDText(name) }
                KIDElement(a, ElementName.MUTED) {
                    KIDText(if (step == VerifyAgeStep.SENDING) strings.verifyAge.submitting else t.verifying)
                }
            }
        }
        return
    }
    if (step == VerifyAgeStep.EMAIL) {
        // In the row's scope, not the form's: SENDING takes the form off screen, and leaving composition cancels its scope
        // mid-request, which would strand the ask after its email was already sent.
        ChallengeEmailForm(
            KIDStrings::verifyAge,
            ElementName.VERIFY_AGE,
            { email = it },
            { withContext(scope.coroutineContext) { request(email) } },
            null,
        )
        return
    }
    KIDElement(a, ElementName.ROW, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KIDElement(a, ElementName.LABEL, modifier = Modifier.weight(1f)) { KIDText(name) }
            KIDElement(a, ElementName.CONTROL_COLUMN) {
                Column(horizontalAlignment = Alignment.End) {
                    val label = strings.verifyAge.title
                    KIDElement(
                        a,
                        ElementName.PILL,
                        ElementName.PILL_ASK,
                        modifier = Modifier
                            .clickable(enabled = !disabled, role = Role.Button, onClick = { scope.launch { request(null) } })
                            .semantics { contentDescription = "$label $name" },
                    ) { KIDText(label) }
                    failedCode?.let {
                        val message = if (it == KIDErrorCode.INVALID_REQUEST) strings.verifyAge.invalidEmail else strings.verifyAge.error
                        KIDElement(a, ElementName.DANGER) { KIDText(message) }
                    }
                }
            }
        }
    }
}

@Composable
private fun GuardianRow(permission: Permission, added: Boolean, disabled: Boolean, onToggle: () -> Unit) {
    val core = requireCore()
    val t = core.strings.settings
    val a = core.appearance
    val name = format(t.enabledLabel, mapOf("permission" to core.strings.permissionName(permission.name)))
    KIDElement(a, ElementName.ROW, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KIDElement(a, ElementName.LABEL, modifier = Modifier.weight(1f)) { KIDText(name) }
            if (permission.enabled) {
                KIDElement(a, ElementName.MUTED) { KIDText(t.granted) }
            } else {
                val label = if (added) t.added else t.ask
                KIDElement(
                    a,
                    ElementName.PILL,
                    if (added) ElementName.PILL_ADDED else ElementName.PILL_ASK,
                    modifier = Modifier
                        .clickable(enabled = !disabled, role = Role.Button, onClick = onToggle)
                        .semantics { contentDescription = "$label $name" },
                ) { KIDText(label) }
            }
        }
    }
}

/** The permission menu without its surrounding card, for your own settings screen. */
@Composable
public fun KIDPermissionSettings(modifier: Modifier = Modifier) {
    val core = requireCore()
    val settings = requireSettings()
    settings.state.collectAsState().value
    val s = settings.permissionSettings
    val strings = core.strings
    val t = strings.settings
    val a = core.appearance
    val scope = rememberCoroutineScope()
    var staged by remember { mutableStateOf(listOf<PermissionName>()) }
    var approverEmail by remember { mutableStateOf("") }
    var failedCode by remember { mutableStateOf<KIDErrorCode?>(null) }
    // The ask this form sent itself, so a Verify age row's request never shows here.
    var asking by remember { mutableStateOf(false) }
    var askFailed by remember { mutableStateOf(false) }
    val inFlight = s.challenge.status == SettingsChallengeStatus.SENDING || s.challenge.status == SettingsChallengeStatus.PENDING
    val needsEmail = !s.hasApproverEmail && !inFlight
    val waiting = s.challenge.status == SettingsChallengeStatus.PENDING && s.challenge.challengeKind != KIDChallengeKind.AGE_ASSURANCE
    val grownupFailed = grownupFailed(askFailed, s.challenge)

    LaunchedEffect(s.challenge) { staged = grownupRestage(s.challenge, staged) }

    if (s.error != null && !s.isLoaded) {
        KIDElement(a, ElementName.DANGER, modifier = modifier) { KIDText(t.error) }
        return
    }
    if (!s.isLoaded) return
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        for (p in s.permissions.filter { it.managedBy != ManagedBy.PROHIBITED }) {
            val name = p.permissionName
            if (p.managedBy == ManagedBy.GUARDIAN) {
                GuardianRow(
                    permission = p,
                    added = if (inFlight) name in s.challenge.permissions else name in staged,
                    disabled = inFlight,
                    onToggle = { staged = if (name in staged) staged - name else staged + name },
                )
            } else if (p.verifiedAgeThreshold != null && !p.enabled) {
                VerifyAgeRow(p, s.challenge, disabled = inFlight || staged.isNotEmpty())
            } else {
                PlayerRow(p)
            }
        }
        if (staged.isNotEmpty() || asking || waiting) {
            KIDElement(a, ElementName.ASK_FORM, modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (needsEmail) {
                        KIDElement(a, ElementName.LABEL) { KIDText(strings.askAnAdult.title) }
                        KIDElement(a, ElementName.MUTED) { KIDText(strings.askAnAdult.description) }
                        KIDEmailField(approverEmail, { approverEmail = it }, strings.askAnAdult.emailPlaceholder)
                    }
                    val label = when {
                        asking -> t.asking
                        waiting -> t.waiting
                        else -> t.askGrownup
                    }
                    KIDButton(label, enabled = !inFlight && !(needsEmail && !isValidEmail(approverEmail)), onClick = {
                        if (staged.isEmpty()) return@KIDButton
                        val names = staged
                        staged = emptyList()
                        // Cleared only here, as a new send starts: see grownupRestage.
                        askFailed = false
                        scope.launch {
                            asking = true
                            val res = s.upgrade(names, if (s.hasApproverEmail) null else approverEmail)
                            asking = false
                            val (restaged, failed) = grownupAskReset(res, names)
                            if (restaged.isNotEmpty()) staged = restaged
                            failedCode = failed
                            askFailed = failed != null
                        }
                    })
                }
            }
        }
        if (grownupFailed) {
            val message = when {
                s.hasApproverEmail -> t.error
                failedCode == KIDErrorCode.INVALID_REQUEST -> strings.askAnAdult.invalidEmail
                else -> strings.askAnAdult.error
            }
            KIDElement(a, ElementName.DANGER) { KIDText(message) }
        }
    }
}

/** The all-in-one permission menu: player-managed permissions toggle, guardian-managed ones are asked for together. */
@Composable
public fun KIDSettings(modifier: Modifier = Modifier) {
    val core = requireCore()
    val a = core.appearance
    KIDElement(a, ElementName.CARD, ElementName.SETTINGS, modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            KIDElement(a, ElementName.TITLE) { KIDText(core.strings.settings.title) }
            KIDPermissionSettings()
        }
    }
}
