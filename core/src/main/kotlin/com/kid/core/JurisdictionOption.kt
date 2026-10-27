package com.kid.core

/** A jurisdiction the picker offers: its code and display name. */
public class JurisdictionOption(
    public val code: String,
    public val name: String,
) {
    override fun equals(other: Any?): Boolean =
        this === other || other is JurisdictionOption &&
            code == other.code &&
            name == other.name

    override fun hashCode(): Int {
        var result = code.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }

    override fun toString(): String = "JurisdictionOption(code=$code, name=$name)"
}
