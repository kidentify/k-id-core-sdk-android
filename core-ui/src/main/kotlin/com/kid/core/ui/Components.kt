package com.kid.core.ui

import android.app.DatePickerDialog
import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.kid.core.AgeGateStep
import com.kid.core.AgeSignal
import com.kid.core.KIDErrorCode
import com.kid.core.KIDResult
import com.kid.core.KIDStatus
import com.kid.core.PermissionName
import com.kid.core.VerifyAgeController
import com.kid.core.VerifyAgeStep
import com.kid.core.permission
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale

public enum class KIDAgeMethod(public val rawValue: String) {
    DATE_OF_BIRTH("date-of-birth"),
    AGE_SLIDER("age-slider"),
}

private val defaultAgeMethodPreference = listOf(KIDAgeMethod.AGE_SLIDER, KIDAgeMethod.DATE_OF_BIRTH)

internal const val MAX_SLIDER_AGE = 35

internal fun sliderAgeLabel(age: Int?): String = when (age) {
    null -> "-"
    MAX_SLIDER_AGE -> "$age+"
    else -> age.toString()
}

private val EMAIL = Regex(
    """^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$""",
)

internal fun isValidEmail(email: String): Boolean = EMAIL.matches(email)

internal fun isoDay(date: LocalDate): String = "%04d-%02d-%02d".format(Locale.ROOT, date.year, date.monthValue, date.dayOfMonth)

private fun dateFieldPlaceholder(locale: Locale): String =
    (DateFormat.getBestDateTimePattern(locale, "ddMMyyyy") ?: "dd/MM/yyyy").lowercase(locale)

@Composable
internal fun KIDButton(label: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val a = requireCore().appearance
    KIDElement(
        a,
        ElementName.BUTTON,
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) { KIDText(label) }
    }
}

@Composable
internal fun KIDEmailField(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    val a = requireCore().appearance
    KIDElement(a, ElementName.INPUT, modifier = Modifier.fillMaxWidth()) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = LocalKIDTextStyle.current.merge(androidx.compose.ui.text.TextStyle(color = a.variables.colorText)),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Email,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = placeholder },
            decorationBox = { field ->
                Box {
                    if (value.isEmpty()) KIDElement(a, ElementName.MUTED) { KIDText(placeholder) }
                    field()
                }
            },
        )
    }
}

@Composable
private fun JurisdictionDialog(title: String, selected: String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val core = requireCore()
    val a = core.appearance
    var query by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        KIDElement(a, ElementName.CARD, modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                KIDElement(a, ElementName.TITLE) { KIDText(title) }
                KIDEmailField(query, { query = it }, title)
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(matchingJurisdictions(core.jurisdictions, query), key = { it.code }) { option ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable(role = Role.Button) { onPick(option.code) }
                                .padding(vertical = 12.dp),
                        ) {
                            KIDText(if (option.code == selected) "${option.name} ✓" else option.name, Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

/** The jurisdiction picker. [onSelect] gets the chosen code. Renders only during the jurisdiction step. */
@Composable
public fun KIDSelectJurisdiction(modifier: Modifier = Modifier, onSelect: ((String) -> Unit)? = null) {
    val core = requireCore()
    val flow = requireAgeGate()
    val state by flow.state.collectAsState()
    var code by rememberSaveable { mutableStateOf("") }
    var picking by remember { mutableStateOf(false) }
    if (state.step != AgeGateStep.JURISDICTION) return
    val t = core.strings.jurisdiction
    val a = core.appearance
    KIDElement(a, ElementName.CARD, ElementName.SELECT_JURISDICTION, modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            KIDElement(a, ElementName.LABEL) { KIDText(t.label) }
            KIDElement(
                a,
                ElementName.INPUT,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button) { picking = true }
                    .semantics { contentDescription = t.label },
            ) {
                val name = core.jurisdictions.firstOrNull { it.code == code }?.name
                if (name == null) KIDElement(a, ElementName.MUTED) { KIDText(t.placeholder) } else KIDText(name)
            }
            KIDButton(t.submit, enabled = code.isNotEmpty(), onClick = {
                flow.setJurisdiction(code)
                onSelect?.invoke(code)
            })
        }
    }
    if (picking) {
        JurisdictionDialog(t.label, code, onPick = {
            code = it
            picking = false
        }, onDismiss = { picking = false })
    }
}

/**
 * The age step: [methods] is your preferred order of inputs, limited to what the jurisdiction approves (default: the
 * age slider, then date of birth). [onSubmit] gets the age. Renders only during the age step.
 */
@Composable
public fun KIDCollectAge(modifier: Modifier = Modifier, methods: List<KIDAgeMethod>? = null, onSubmit: ((AgeSignal) -> Unit)? = null) {
    val core = requireCore()
    val flow = requireAgeGate()
    val state by flow.state.collectAsState()
    var dob by rememberSaveable { mutableStateOf<String?>(null) }
    var age by rememberSaveable { mutableStateOf<Int?>(null) }
    val context = LocalContext.current
    val approved = state.requirements?.approvedAgeCollectionMethods?.mapNotNull { m -> KIDAgeMethod.entries.firstOrNull { it.rawValue == m } }
    val method = approved?.let { (methods ?: defaultAgeMethodPreference).firstOrNull(it::contains) ?: it.firstOrNull() }
    if (state.step != AgeGateStep.AGE || method == null) return
    val t = core.strings.age
    val a = core.appearance

    fun submit() {
        val signal = when (method) {
            KIDAgeMethod.DATE_OF_BIRTH -> dob?.let(AgeSignal::DateOfBirth)
            KIDAgeMethod.AGE_SLIDER -> age?.let(AgeSignal::Age)
        } ?: return
        flow.setAge(signal)
        onSubmit?.invoke(signal)
    }

    KIDElement(a, ElementName.CARD, ElementName.COLLECT_AGE, modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (method == KIDAgeMethod.DATE_OF_BIRTH) {
                KIDElement(a, ElementName.LABEL) { KIDText(t.dateOfBirthLabel) }
                KIDElement(
                    a,
                    ElementName.INPUT,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) {
                            val today = LocalDate.now()
                            DatePickerDialog(context, { _, year, month, day ->
                                dob = isoDay(LocalDate.of(year, month + 1, day))
                            }, today.year, today.monthValue - 1, today.dayOfMonth).apply {
                                datePicker.maxDate = System.currentTimeMillis()
                            }.show()
                        }
                        .semantics { contentDescription = t.dateOfBirthLabel },
                ) {
                    val chosen = dob
                    if (chosen == null) {
                        KIDElement(a, ElementName.MUTED) { KIDText(dateFieldPlaceholder(Locale.getDefault())) }
                    } else {
                        KIDText(chosen)
                    }
                }
            } else {
                val label = format(t.ageLabel, mapOf("age" to sliderAgeLabel(age)))
                KIDElement(a, ElementName.LABEL) { KIDText(label) }
                Slider(
                    value = (age ?: 1).toFloat(),
                    onValueChange = { age = it.toInt() },
                    valueRange = 1f..MAX_SLIDER_AGE.toFloat(),
                    steps = MAX_SLIDER_AGE - 2,
                    colors = SliderDefaults.colors(
                        thumbColor = a.variables.colorPrimary,
                        activeTrackColor = a.variables.colorPrimary,
                        inactiveTrackColor = a.variables.colorBorder,
                    ),
                    modifier = Modifier.semantics { contentDescription = label },
                )
            }
            KIDButton(
                t.submit,
                enabled = if (method == KIDAgeMethod.DATE_OF_BIRTH) dob != null else age != null,
                onClick = ::submit,
            )
        }
    }
}

@Composable
internal fun ChallengeEmailForm(
    strings: (KIDStrings) -> KIDStrings.AskAnAdult,
    element: ElementName,
    setEmail: (String) -> Unit,
    send: suspend () -> KIDResult,
    onSent: ((String) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val core = requireCore()
    val a = core.appearance
    val t = strings(core.strings)
    val scope = rememberCoroutineScope()
    var email by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    KIDElement(a, ElementName.CARD, element, modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            KIDElement(a, ElementName.LABEL) { KIDText(t.title) }
            KIDElement(a, ElementName.MUTED) { KIDText(t.description) }
            KIDEmailField(email, { email = it }, t.emailPlaceholder)
            KIDButton(if (busy) t.submitting else t.submit, enabled = !busy && isValidEmail(email), onClick = {
                scope.launch {
                    busy = true
                    error = null
                    setEmail(email)
                    val res = send()
                    if (res.ok) onSent?.invoke(email) else error = if (res.error?.code == KIDErrorCode.INVALID_REQUEST) t.invalidEmail else t.error
                    busy = false
                }
            })
            error?.let { KIDElement(a, ElementName.DANGER) { KIDText(it) } }
        }
    }
}

@Composable
private fun GateEmailForm(step: AgeGateStep, strings: (KIDStrings) -> KIDStrings.AskAnAdult, element: ElementName, onSent: ((String) -> Unit)?, modifier: Modifier) {
    val flow = requireAgeGate()
    val state by flow.state.collectAsState()
    if (state.step == step && !state.emailProvided) {
        ChallengeEmailForm(strings, element, flow::setEmail, flow::submit, onSent, modifier)
    }
}

/** The grownup's email, with a built-in send. [onSent] gets the email once it has gone out. */
@Composable
public fun KIDAskAnAdult(modifier: Modifier = Modifier, onSent: ((String) -> Unit)? = null) {
    GateEmailForm(AgeGateStep.PARENTAL_CONSENT, KIDStrings::askAnAdult, ElementName.ASK_AN_ADULT, onSent, modifier)
}

/** For an adults-only product: emails the player a link to verify their age on k-ID. */
@Composable
public fun KIDVerifyAge(modifier: Modifier = Modifier, onSent: ((String) -> Unit)? = null) {
    GateEmailForm(AgeGateStep.AGE_ASSURANCE, KIDStrings::verifyAge, ElementName.VERIFY_AGE, onSent, modifier)
}

/**
 * The drop-in age gate: draws the capture while it runs, then the slot matching its outcome, and nothing for a slot
 * you leave out. Provide at least [pass].
 */
@Composable
public fun KIDAgeGate(
    modifier: Modifier = Modifier,
    pending: (@Composable () -> Unit)? = null,
    pass: (@Composable () -> Unit)? = null,
    prohibited: (@Composable () -> Unit)? = null,
    fail: (@Composable () -> Unit)? = null,
    error: (@Composable () -> Unit)? = null,
) {
    val flow = requireAgeGate()
    val state by flow.state.collectAsState()
    when (state.status) {
        KIDStatus.RUNNING -> Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            KIDSelectJurisdiction()
            KIDCollectAge()
            KIDAskAnAdult()
            KIDVerifyAge()
        }
        KIDStatus.PENDING -> pending?.invoke()
        KIDStatus.PASS -> pass?.invoke()
        KIDStatus.PROHIBITED -> prohibited?.invoke()
        KIDStatus.FAIL -> fail?.invoke()
        KIDStatus.ERROR -> error?.invoke()
    }
}

/**
 * Verifies the player is an adult (the civil age of their jurisdiction) on the established session: the email form
 * while it needs the player's email, then the slot matching the outcome. Renders nothing until the age gate passes.
 * Showing it again after a fail or error tries again.
 */
@Composable
public fun KIDSessionVerifyAge(
    modifier: Modifier = Modifier,
    onSent: ((String) -> Unit)? = null,
    pending: (@Composable () -> Unit)? = null,
    pass: (@Composable () -> Unit)? = null,
    fail: (@Composable () -> Unit)? = null,
    error: (@Composable () -> Unit)? = null,
) {
    val gate = requireAgeGate()
    gate.state.collectAsState().value
    val controller = gate.sessionVerifyAge() ?: return
    androidx.compose.runtime.key(controller) {
        SessionVerifyAgeBody(controller, onSent, pending, pass, fail, error, modifier)
    }
}

@Composable
private fun SessionVerifyAgeBody(
    controller: VerifyAgeController,
    onSent: ((String) -> Unit)?,
    pending: (@Composable () -> Unit)?,
    pass: (@Composable () -> Unit)?,
    fail: (@Composable () -> Unit)?,
    error: (@Composable () -> Unit)?,
    modifier: Modifier,
) {
    val state by controller.state.collectAsState()
    androidx.compose.runtime.LaunchedEffect(controller) { controller.start() }
    when (state.status) {
        KIDStatus.RUNNING -> if (state.step == VerifyAgeStep.EMAIL) {
            ChallengeEmailForm(KIDStrings::verifyAge, ElementName.VERIFY_AGE, controller::setEmail, controller::submit, onSent, modifier)
        }
        KIDStatus.PENDING -> pending?.invoke()
        KIDStatus.PASS -> pass?.invoke()
        KIDStatus.FAIL -> fail?.invoke()
        KIDStatus.ERROR -> error?.invoke()
        KIDStatus.PROHIBITED -> Unit
    }
}

/** Renders [content] when [name] is enabled for the player, else [fallback]. */
@Composable
public fun KIDPermission(name: PermissionName, fallback: @Composable () -> Unit = {}, content: @Composable () -> Unit) {
    val flow = requireAgeGate()
    flow.state.collectAsState().value
    if (flow.permission.can(name)) content() else fallback()
}
