package com.kid.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The design tokens every view draws from; pass only the ones you change. */
@Immutable
public data class AppearanceVariables(
    val colorPrimary: Color = Color(0xFF4F46E5),
    val colorOnPrimary: Color = Color(0xFFFFFFFF),
    val colorText: Color = Color(0xFF111827),
    val colorTextMuted: Color = Color(0xFF6B7280),
    val colorSurface: Color = Color(0xFFFFFFFF),
    val colorSurfaceMuted: Color = Color(0xFFF3F4F6),
    val colorBorder: Color = Color(0xFFE5E7EB),
    val colorDanger: Color = Color(0xFFDC2626),
    val fontFamily: FontFamily? = null,
    val radius: Dp = 12.dp,
)

/** One element's style; a field left null keeps the default. */
@Immutable
public data class KIDElementStyle(
    val foregroundColor: Color? = null,
    val backgroundColor: Color? = null,
    val borderColor: Color? = null,
    val fontSize: TextUnit? = null,
    val fontWeight: FontWeight? = null,
    val fontFamily: FontFamily? = null,
    val padding: PaddingValues? = null,
    val cornerRadius: Dp? = null,
) {
    internal fun mergedOver(base: KIDElementStyle): KIDElementStyle = KIDElementStyle(
        foregroundColor = foregroundColor ?: base.foregroundColor,
        backgroundColor = backgroundColor ?: base.backgroundColor,
        borderColor = borderColor ?: base.borderColor,
        fontSize = fontSize ?: base.fontSize,
        fontWeight = fontWeight ?: base.fontWeight,
        fontFamily = fontFamily ?: base.fontFamily,
        padding = padding ?: base.padding,
        cornerRadius = cornerRadius ?: base.cornerRadius,
    )
}

public enum class ElementName {
    CARD, TITLE, LABEL, INPUT, BUTTON, MUTED, DANGER, ROW, PILL, PILL_ASK, PILL_ADDED, ASK_FORM, CONTROL_COLUMN,
    VERIFY_AGE, SELECT_JURISDICTION, COLLECT_AGE, ASK_AN_ADULT, SETTINGS,
}

/** The views' look: [variables] are the tokens, [elements] override one element each and beat every default. */
@Immutable
public data class Appearance(
    val variables: AppearanceVariables = AppearanceVariables(),
    val elements: Map<ElementName, KIDElementStyle> = emptyMap(),
)

private fun AppearanceVariables.font(size: Int, weight: FontWeight = FontWeight.Normal) =
    KIDElementStyle(fontSize = size.sp, fontWeight = weight, fontFamily = fontFamily)

internal fun defaultElementStyle(key: ElementName, v: AppearanceVariables): KIDElementStyle = when (key) {
    ElementName.CARD -> v.font(16).copy(
        foregroundColor = v.colorText,
        backgroundColor = v.colorSurface,
        borderColor = v.colorBorder,
        padding = PaddingValues(20.dp),
        cornerRadius = v.radius,
    )
    ElementName.TITLE -> v.font(16, FontWeight.SemiBold)
    ElementName.LABEL -> v.font(14, FontWeight.SemiBold)
    ElementName.INPUT -> KIDElementStyle(
        borderColor = v.colorBorder,
        padding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        cornerRadius = v.radius / 2,
    )
    ElementName.BUTTON -> v.font(16, FontWeight.SemiBold).copy(
        foregroundColor = v.colorOnPrimary,
        backgroundColor = v.colorPrimary,
        padding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        cornerRadius = v.radius / 2,
    )
    ElementName.MUTED -> v.font(14).copy(foregroundColor = v.colorTextMuted)
    ElementName.DANGER -> v.font(14).copy(foregroundColor = v.colorDanger)
    ElementName.PILL -> v.font(13, FontWeight.SemiBold).copy(
        padding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        cornerRadius = 999.dp,
    )
    ElementName.PILL_ASK -> KIDElementStyle(foregroundColor = v.colorText, borderColor = v.colorBorder)
    ElementName.PILL_ADDED -> KIDElementStyle(
        foregroundColor = v.colorOnPrimary,
        backgroundColor = v.colorPrimary,
        borderColor = v.colorPrimary,
    )
    else -> KIDElementStyle()
}

internal fun resolveElementStyle(appearance: Appearance, keys: List<ElementName>): KIDElementStyle {
    val defaults = keys.fold(KIDElementStyle()) { acc, key -> defaultElementStyle(key, appearance.variables).mergedOver(acc) }
    return keys.fold(defaults) { acc, key -> appearance.elements[key]?.mergedOver(acc) ?: acc }
}

internal val LocalKIDTextStyle = staticCompositionLocalOf { TextStyle.Default }

private fun KIDElementStyle.textStyle(inherited: TextStyle): TextStyle = inherited.merge(
    TextStyle(
        color = foregroundColor ?: Color.Unspecified,
        fontSize = fontSize ?: TextUnit.Unspecified,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
    ),
)

@Composable
internal fun KIDElement(
    appearance: Appearance,
    vararg keys: ElementName,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val style = resolveElementStyle(appearance, keys.toList())
    val shape = RoundedCornerShape(style.cornerRadius ?: 0.dp)
    var box = modifier.clip(shape)
    style.backgroundColor?.let { box = box.background(it, shape) }
    style.borderColor?.let { box = box.border(1.dp, it, shape) }
    style.padding?.let { box = box.padding(it) }
    CompositionLocalProvider(
        LocalKIDTextStyle provides style.textStyle(LocalKIDTextStyle.current),
        LocalLayoutDirection provides (LocalKIDLayoutDirection.current ?: LocalLayoutDirection.current),
    ) {
        Box(box) { content() }
    }
}

@Composable
internal fun KIDText(text: String, modifier: Modifier = Modifier) {
    BasicText(text, modifier, style = LocalKIDTextStyle.current)
}
