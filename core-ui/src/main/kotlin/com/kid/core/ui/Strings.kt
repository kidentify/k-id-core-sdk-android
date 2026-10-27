package com.kid.core.ui

import com.kid.core.PermissionName
import kotlinx.serialization.Serializable

/** Every string the views render. */
@Serializable
public data class KIDStrings(
    val jurisdiction: Jurisdiction,
    val age: Age,
    val askAnAdult: AskAnAdult,
    val verifyAge: AskAnAdult,
    val settings: Settings,
    val subdivisions: Map<String, String>,
    val permissions: Map<String, KIDPermissionStrings> = emptyMap(),
) {
    @Serializable
    public data class Jurisdiction(
        val label: String,
        val placeholder: String,
        val submit: String,
        /** `{country}` is the localized country, `{subdivision}` the region. */
        val subdivisionName: String,
    )

    @Serializable
    public data class Age(
        val dateOfBirthLabel: String,
        /** `{age}` is the selected age. */
        val ageLabel: String,
        val submit: String,
    )

    @Serializable
    public data class AskAnAdult(
        val title: String,
        val description: String,
        val emailPlaceholder: String,
        val submit: String,
        val submitting: String,
        val error: String,
        val invalidEmail: String,
    )

    @Serializable
    public data class Settings(
        val title: String,
        /** `{permission}` is the permission name. */
        val enabledLabel: String,
        val on: String,
        val off: String,
        val ask: String,
        val added: String,
        val granted: String,
        val askGrownup: String,
        val asking: String,
        val waiting: String,
        /** Shown in place of [waiting] while the player's own age check is out. */
        val verifying: String,
        val error: String,
    )

    internal fun permissionName(name: String): String = permissions[name]?.name ?: name
}

@Serializable
public data class KIDPermissionStrings(val name: String)

/** A locale's copy, or partial overrides: each field left null keeps the English default. */
public data class KIDLocalization(
    val jurisdiction: Jurisdiction? = null,
    val age: Age? = null,
    val askAnAdult: AskAnAdult? = null,
    val verifyAge: AskAnAdult? = null,
    val settings: Settings? = null,
    val subdivisions: Map<String, String>? = null,
    val permissions: Map<PermissionName, PermissionStrings>? = null,
    val locale: String? = null,
) {
    @Serializable
    public data class Jurisdiction(
        val label: String? = null,
        val placeholder: String? = null,
        val submit: String? = null,
        val subdivisionName: String? = null,
    )

    @Serializable
    public data class Age(val dateOfBirthLabel: String? = null, val ageLabel: String? = null, val submit: String? = null)

    @Serializable
    public data class AskAnAdult(
        val title: String? = null,
        val description: String? = null,
        val emailPlaceholder: String? = null,
        val submit: String? = null,
        val submitting: String? = null,
        val error: String? = null,
        val invalidEmail: String? = null,
    )

    @Serializable
    public data class Settings(
        val title: String? = null,
        val enabledLabel: String? = null,
        val on: String? = null,
        val off: String? = null,
        val ask: String? = null,
        val added: String? = null,
        val granted: String? = null,
        val askGrownup: String? = null,
        val asking: String? = null,
        val waiting: String? = null,
        val error: String? = null,
        val verifying: String? = null,
    )

    @Serializable
    public data class PermissionStrings(val name: String? = null)

    public companion object {
        public val arSA: KIDLocalization by lazy { bundledLocalization(KIDLocale.AR) }
        public val deDE: KIDLocalization by lazy { bundledLocalization(KIDLocale.DE) }
        public val enGB: KIDLocalization by lazy { bundledLocalization(KIDLocale.EN_GB) }
        public val esES: KIDLocalization by lazy { bundledLocalization(KIDLocale.ES) }
        public val esMX: KIDLocalization by lazy { bundledLocalization(KIDLocale.ES_MX) }
        public val frFR: KIDLocalization by lazy { bundledLocalization(KIDLocale.FR) }
        public val frCA: KIDLocalization by lazy { bundledLocalization(KIDLocale.FR_CA) }
        public val hiIN: KIDLocalization by lazy { bundledLocalization(KIDLocale.HI) }
        public val idID: KIDLocalization by lazy { bundledLocalization(KIDLocale.ID) }
        public val itIT: KIDLocalization by lazy { bundledLocalization(KIDLocale.IT) }
        public val jaJP: KIDLocalization by lazy { bundledLocalization(KIDLocale.JA) }
        public val koKR: KIDLocalization by lazy { bundledLocalization(KIDLocale.KO) }
        public val msMY: KIDLocalization by lazy { bundledLocalization(KIDLocale.MS) }
        public val nlNL: KIDLocalization by lazy { bundledLocalization(KIDLocale.NL) }
        public val ptPT: KIDLocalization by lazy { bundledLocalization(KIDLocale.PT) }
        public val ptBR: KIDLocalization by lazy { bundledLocalization(KIDLocale.PT_BR) }
        public val ruRU: KIDLocalization by lazy { bundledLocalization(KIDLocale.RU) }
        public val thTH: KIDLocalization by lazy { bundledLocalization(KIDLocale.TH) }
        public val trTR: KIDLocalization by lazy { bundledLocalization(KIDLocale.TR) }
        public val zhCN: KIDLocalization by lazy { bundledLocalization(KIDLocale.ZH) }
        public val zhTW: KIDLocalization by lazy { bundledLocalization(KIDLocale.ZH_HANT) }
    }
}

private fun KIDStrings.AskAnAdult.merge(o: KIDLocalization.AskAnAdult?): KIDStrings.AskAnAdult = if (o == null) this else KIDStrings.AskAnAdult(
    title = o.title ?: title,
    description = o.description ?: description,
    emailPlaceholder = o.emailPlaceholder ?: emailPlaceholder,
    submit = o.submit ?: submit,
    submitting = o.submitting ?: submitting,
    error = o.error ?: error,
    invalidEmail = o.invalidEmail ?: invalidEmail,
)

internal fun mergeStrings(overrides: KIDLocalization?): KIDStrings {
    val s = DEFAULT_STRINGS
    if (overrides == null) return s
    val j = overrides.jurisdiction
    val a = overrides.age
    val t = overrides.settings
    return KIDStrings(
        jurisdiction = if (j == null) s.jurisdiction else KIDStrings.Jurisdiction(
            label = j.label ?: s.jurisdiction.label,
            placeholder = j.placeholder ?: s.jurisdiction.placeholder,
            submit = j.submit ?: s.jurisdiction.submit,
            subdivisionName = j.subdivisionName ?: s.jurisdiction.subdivisionName,
        ),
        age = if (a == null) s.age else KIDStrings.Age(
            dateOfBirthLabel = a.dateOfBirthLabel ?: s.age.dateOfBirthLabel,
            ageLabel = a.ageLabel ?: s.age.ageLabel,
            submit = a.submit ?: s.age.submit,
        ),
        askAnAdult = s.askAnAdult.merge(overrides.askAnAdult),
        verifyAge = s.verifyAge.merge(overrides.verifyAge),
        settings = if (t == null) s.settings else KIDStrings.Settings(
            title = t.title ?: s.settings.title,
            enabledLabel = t.enabledLabel ?: s.settings.enabledLabel,
            on = t.on ?: s.settings.on,
            off = t.off ?: s.settings.off,
            ask = t.ask ?: s.settings.ask,
            added = t.added ?: s.settings.added,
            granted = t.granted ?: s.settings.granted,
            askGrownup = t.askGrownup ?: s.settings.askGrownup,
            asking = t.asking ?: s.settings.asking,
            waiting = t.waiting ?: s.settings.waiting,
            verifying = t.verifying ?: s.settings.verifying,
            error = t.error ?: s.settings.error,
        ),
        subdivisions = s.subdivisions + (overrides.subdivisions ?: emptyMap()),
        permissions = s.permissions + (overrides.permissions ?: emptyMap()).map { (name, copy) ->
            name.rawValue to KIDPermissionStrings(copy.name ?: s.permissions[name.rawValue]?.name ?: name.rawValue)
        },
    )
}

private val TOKEN = Regex("""\{(\w+)\}""")

/** Fills `{token}` placeholders from [vars], leaving unknown ones verbatim. */
internal fun format(template: String, vars: Map<String, Any>): String =
    TOKEN.replace(template) { match -> vars[match.groupValues[1]]?.toString() ?: match.value }
