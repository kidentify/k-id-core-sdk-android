package com.kid.core.ui

import androidx.compose.ui.unit.LayoutDirection
import com.kid.core.PermissionName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

public enum class KIDLocale(public val rawValue: String) {
    AR("ar"), DE("de"), EN("en"), EN_GB("en-GB"), ES("es"), ES_MX("es-MX"), FR("fr"), FR_CA("fr-CA"), HI("hi"),
    ID("id"), IT("it"), JA("ja"), KO("ko"), MS("ms"), NL("nl"), PT("pt"), PT_BR("pt-BR"), RU("ru"), TH("th"),
    TR("tr"), ZH("zh"), ZH_HANT("zh-Hant"),
}

public val KID_LOCALES: List<KIDLocale> = KIDLocale.entries

private val bundleJson = Json { ignoreUnknownKeys = true }

@Serializable
private data class LocaleFile(
    val jurisdiction: KIDLocalization.Jurisdiction? = null,
    val age: KIDLocalization.Age? = null,
    val askAnAdult: KIDLocalization.AskAnAdult? = null,
    val verifyAge: KIDLocalization.AskAnAdult? = null,
    val settings: KIDLocalization.Settings? = null,
    val subdivisions: Map<String, String>? = null,
)

internal fun bundled(folder: String, locale: String): String {
    val path = "/com/kid/core/ui/localization/$folder/$locale.json"
    val stream = KIDStrings::class.java.getResourceAsStream(path)
        ?: error("k-ID core-ui is missing its bundled $folder/$locale.json")
    return stream.use { it.readBytes().decodeToString() }
}

/** The built-in English copy. */
public val DEFAULT_STRINGS: KIDStrings by lazy {
    bundleJson.decodeFromString<KIDStrings>(bundled("locales", "en")).copy(
        permissions = bundleJson.decodeFromString<Map<String, KIDPermissionStrings>>(bundled("permissions", "en")),
    )
}

internal fun bundledLocalization(locale: KIDLocale): KIDLocalization {
    val file = bundleJson.decodeFromString<LocaleFile>(bundled("locales", locale.rawValue))
    val permissions = bundleJson.decodeFromString<Map<String, KIDLocalization.PermissionStrings>>(bundled("permissions", locale.rawValue))
    return KIDLocalization(
        jurisdiction = file.jurisdiction,
        age = file.age,
        askAnAdult = file.askAnAdult,
        verifyAge = file.verifyAge,
        settings = file.settings,
        subdivisions = file.subdivisions,
        permissions = permissions.mapKeys { PermissionName(it.key) },
        locale = locale.rawValue,
    )
}

private val traditionalChineseSubtags = setOf("hant", "tw", "hk", "mo")

private fun resolveLocale(tag: String): KIDLocale {
    val lower = tag.replace('_', '-').lowercase()
    fun match(t: String) = KIDLocale.entries.firstOrNull { it.rawValue.lowercase() == t }
    match(lower)?.let { return it }
    val parts = lower.split('-')
    val language = parts.first()
    if (language == "zh" && parts.drop(1).any { it in traditionalChineseSubtags }) return KIDLocale.ZH_HANT
    return match(language) ?: KIDLocale.EN
}

/**
 * The localization for a player's language tag: the exact tag first, then Traditional Chinese for `zh-TW`, `zh-HK`,
 * `zh-MO` and `zh-Hant`, then the base language, then English. English is `KIDLocalization(locale = "en")`, which
 * keeps the built-in copy.
 */
public fun getKIDLocalization(locale: String): KIDLocalization = when (resolveLocale(locale)) {
    KIDLocale.EN -> KIDLocalization(locale = "en")
    KIDLocale.AR -> KIDLocalization.arSA
    KIDLocale.DE -> KIDLocalization.deDE
    KIDLocale.EN_GB -> KIDLocalization.enGB
    KIDLocale.ES -> KIDLocalization.esES
    KIDLocale.ES_MX -> KIDLocalization.esMX
    KIDLocale.FR -> KIDLocalization.frFR
    KIDLocale.FR_CA -> KIDLocalization.frCA
    KIDLocale.HI -> KIDLocalization.hiIN
    KIDLocale.ID -> KIDLocalization.idID
    KIDLocale.IT -> KIDLocalization.itIT
    KIDLocale.JA -> KIDLocalization.jaJP
    KIDLocale.KO -> KIDLocalization.koKR
    KIDLocale.MS -> KIDLocalization.msMY
    KIDLocale.NL -> KIDLocalization.nlNL
    KIDLocale.PT -> KIDLocalization.ptPT
    KIDLocale.PT_BR -> KIDLocalization.ptBR
    KIDLocale.RU -> KIDLocalization.ruRU
    KIDLocale.TH -> KIDLocalization.thTH
    KIDLocale.TR -> KIDLocalization.trTR
    KIDLocale.ZH -> KIDLocalization.zhCN
    KIDLocale.ZH_HANT -> KIDLocalization.zhTW
}

private val rtlLanguages = setOf("ar", "fa", "he", "ur")

internal fun textDirection(locale: String?): LayoutDirection? {
    if (locale.isNullOrEmpty()) return null
    val language = locale.split('-', '_').first().lowercase()
    return if (language in rtlLanguages) LayoutDirection.Rtl else LayoutDirection.Ltr
}
