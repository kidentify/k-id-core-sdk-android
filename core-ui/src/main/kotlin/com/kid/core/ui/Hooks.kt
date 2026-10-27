package com.kid.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.kid.core.JurisdictionOption
import com.kid.core.UseKIDAgeGate
import com.kid.core.UseKIDChallenge
import com.kid.core.UseKIDPermission
import com.kid.core.UseKIDPermissionSettings
import com.kid.core.UseKIDSession
import com.kid.core.ageGate
import com.kid.core.challenge
import com.kid.core.permission
import com.kid.core.permissionSettings
import com.kid.core.session

/** The age gate's status, ids, requirements and `submit`; recomposes as it changes. Call inside a KIDSessionProvider. */
@Composable
public fun useKIDAgeGate(): UseKIDAgeGate {
    val controller = requireAgeGate()
    controller.state.collectAsState().value
    return controller.ageGate
}

/** The established session, its permissions and its age verification. Call inside a KIDSessionProvider. */
@Composable
public fun useKIDSession(): UseKIDSession {
    val controller = requireAgeGate()
    controller.state.collectAsState().value
    controller.sessionVerifyAge()?.state?.collectAsState()?.value
    return controller.session
}

/** `can` and `get` for the session's permissions. Call inside a KIDSessionProvider. */
@Composable
public fun useKIDPermission(): UseKIDPermission {
    val controller = requireAgeGate()
    controller.state.collectAsState().value
    return controller.permission
}

/** The challenge's id, status and deadline, for a custom waiting screen. Call inside a KIDSessionProvider. */
@Composable
public fun useKIDChallenge(): UseKIDChallenge {
    val controller = requireAgeGate()
    controller.state.collectAsState().value
    return controller.challenge
}

/** The settings menu's permissions, `change`, `upgrade` and the in-flight ask. Call inside a KIDSettingsProvider. */
@Composable
public fun useKIDPermissionSettings(): UseKIDPermissionSettings {
    val controller = requireSettings()
    controller.state.collectAsState().value
    return controller.permissionSettings
}

/** The merged copy the views render. Call inside a KIDCoreProvider. */
@Composable
public fun useKIDStrings(): KIDStrings = requireCore().strings

/** The jurisdiction picker's options for the locale, for a custom picker. Call inside a KIDCoreProvider. */
@Composable
public fun useKIDJurisdictions(): List<JurisdictionOption> = requireCore().jurisdictions
