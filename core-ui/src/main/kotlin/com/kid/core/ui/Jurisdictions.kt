package com.kid.core.ui

import com.kid.core.COUNTRIES
import com.kid.core.JurisdictionCode
import com.kid.core.JurisdictionOption
import java.text.Collator
import java.text.Normalizer
import java.util.Locale

/** Resolves the jurisdiction from the client IP, falling back to the picker. */
public val JurisdictionCode.Companion.AUTO: JurisdictionCode get() = JurisdictionCode("auto")

private val englishCountryNames by lazy { COUNTRIES.associate { it.code to it.name } }

private val LANGUAGE_TAG = Regex("""^[A-Za-z]{2,3}(-[A-Za-z0-9]{1,8})*$""")

private fun validLocale(locale: String?): String? {
    if (locale == null || !LANGUAGE_TAG.matches(locale)) return null
    val parts = locale.split('-')
    return (listOf(parts[0].lowercase()) + parts.drop(1).map { part ->
        when {
            part.length == 4 && part.all(Char::isLetter) -> part.lowercase().replaceFirstChar(Char::uppercase)
            part.length == 2 && part.all(Char::isLetter) -> part.uppercase()
            else -> part.lowercase()
        }
    }).joinToString("-")
}

private fun countryNamer(locale: String?): (String) -> String {
    val english = { code: String -> englishCountryNames[code] ?: code }
    if (locale == null || locale.substringBefore('-') == "en") return english
    val display = Locale.forLanguageTag(locale)
    return { code ->
        Locale.Builder().setRegion(code).build().getDisplayCountry(display).takeIf { it.isNotEmpty() && it != code } ?: english(code)
    }
}

internal fun localizeJurisdictions(strings: KIDStrings, locale: String? = null): List<JurisdictionOption> {
    val tag = validLocale(locale)
    val country = countryNamer(tag)
    val subdivisions = DEFAULT_STRINGS.subdivisions.keys.map { code ->
        JurisdictionOption(
            code,
            format(
                strings.jurisdiction.subdivisionName,
                mapOf(
                    "country" to country(code.take(2)),
                    "subdivision" to (strings.subdivisions[code] ?: DEFAULT_STRINGS.subdivisions[code] ?: code),
                ),
            ),
        )
    }
    val collator = Collator.getInstance(Locale.forLanguageTag(tag ?: "en"))
    return (COUNTRIES.map { JurisdictionOption(it.code, country(it.code)) } + subdivisions).sortedWith { a, b -> collator.compare(a.name, b.name) }
}

/** Every country and supported subdivision, in English, sorted by name. */
public val JURISDICTIONS: List<JurisdictionOption> by lazy { localizeJurisdictions(DEFAULT_STRINGS) }

private val MARKS = Regex("""\p{M}+""")

private fun folded(value: String): String = MARKS.replace(Normalizer.normalize(value, Normalizer.Form.NFD), "").lowercase()

internal fun matchingJurisdictions(options: List<JurisdictionOption>, query: String): List<JurisdictionOption> {
    val q = folded(query.trim())
    if (q.isEmpty()) return options
    return options.filter { folded(it.name).contains(q) || it.code.lowercase().contains(q) }
}
