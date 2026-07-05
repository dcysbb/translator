package com.poozh.translator.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.view.View
import android.widget.TextView

data class Md3ColorScheme(
    val primary: Int,
    val onPrimary: Int,
    val primaryContainer: Int,
    val onPrimaryContainer: Int,
    val secondary: Int,
    val onSecondary: Int,
    val secondaryContainer: Int,
    val onSecondaryContainer: Int,
    val tertiary: Int,
    val onTertiary: Int,
    val tertiaryContainer: Int,
    val onTertiaryContainer: Int,
    val error: Int,
    val onError: Int,
    val errorContainer: Int,
    val onErrorContainer: Int,
    val surface: Int,
    val onSurface: Int,
    val onSurfaceVariant: Int,
    val surfaceContainerLowest: Int,
    val surfaceContainerLow: Int,
    val surfaceContainer: Int,
    val surfaceContainerHigh: Int,
    val surfaceContainerHighest: Int,
    val outline: Int,
    val outlineVariant: Int,
    val inverseSurface: Int,
    val inverseOnSurface: Int
)

enum class Md3TextStyle {
    HeadlineMedium,
    HeadlineSmall,
    TitleLarge,
    TitleMedium,
    TitleSmall,
    BodyLarge,
    BodyMedium,
    BodySmall,
    LabelLarge,
    LabelMedium
}

enum class Md3ButtonStyle {
    Filled,
    FilledTonal,
    Outlined,
    Text
}

object Md3 {
    val light = Md3ColorScheme(
        primary = Color.rgb(0, 105, 109),
        onPrimary = Color.WHITE,
        primaryContainer = Color.rgb(111, 247, 247),
        onPrimaryContainer = Color.rgb(0, 32, 34),
        secondary = Color.rgb(74, 99, 100),
        onSecondary = Color.WHITE,
        secondaryContainer = Color.rgb(204, 232, 232),
        onSecondaryContainer = Color.rgb(5, 31, 32),
        tertiary = Color.rgb(78, 96, 124),
        onTertiary = Color.WHITE,
        tertiaryContainer = Color.rgb(214, 227, 255),
        onTertiaryContainer = Color.rgb(7, 28, 55),
        error = Color.rgb(186, 26, 26),
        onError = Color.WHITE,
        errorContainer = Color.rgb(255, 218, 214),
        onErrorContainer = Color.rgb(65, 0, 2),
        surface = Color.rgb(250, 253, 252),
        onSurface = Color.rgb(25, 28, 29),
        onSurfaceVariant = Color.rgb(63, 73, 74),
        surfaceContainerLowest = Color.WHITE,
        surfaceContainerLow = Color.rgb(244, 247, 246),
        surfaceContainer = Color.rgb(238, 242, 241),
        surfaceContainerHigh = Color.rgb(232, 236, 235),
        surfaceContainerHighest = Color.rgb(226, 230, 229),
        outline = Color.rgb(111, 121, 122),
        outlineVariant = Color.rgb(190, 201, 201),
        inverseSurface = Color.rgb(45, 49, 50),
        inverseOnSurface = Color.rgb(239, 241, 240)
    )

    val dark = Md3ColorScheme(
        primary = Color.rgb(78, 217, 218),
        onPrimary = Color.rgb(0, 55, 57),
        primaryContainer = Color.rgb(0, 80, 83),
        onPrimaryContainer = Color.rgb(111, 247, 247),
        secondary = Color.rgb(176, 204, 204),
        onSecondary = Color.rgb(27, 52, 53),
        secondaryContainer = Color.rgb(50, 75, 76),
        onSecondaryContainer = Color.rgb(204, 232, 232),
        tertiary = Color.rgb(181, 199, 232),
        onTertiary = Color.rgb(31, 49, 75),
        tertiaryContainer = Color.rgb(54, 72, 100),
        onTertiaryContainer = Color.rgb(214, 227, 255),
        error = Color.rgb(255, 180, 171),
        onError = Color.rgb(105, 0, 5),
        errorContainer = Color.rgb(147, 0, 10),
        onErrorContainer = Color.rgb(255, 218, 214),
        surface = Color.rgb(16, 20, 20),
        onSurface = Color.rgb(224, 227, 226),
        onSurfaceVariant = Color.rgb(190, 201, 201),
        surfaceContainerLowest = Color.rgb(11, 15, 15),
        surfaceContainerLow = Color.rgb(24, 28, 28),
        surfaceContainer = Color.rgb(28, 32, 32),
        surfaceContainerHigh = Color.rgb(38, 42, 43),
        surfaceContainerHighest = Color.rgb(49, 53, 53),
        outline = Color.rgb(137, 147, 148),
        outlineVariant = Color.rgb(63, 73, 74),
        inverseSurface = Color.rgb(224, 227, 226),
        inverseOnSurface = Color.rgb(45, 49, 50)
    )

    fun surface(
        context: Context,
        color: Int,
        radiusDp: Float,
        strokeColor: Int? = null,
        strokeWidthDp: Float = 1f
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = context.dp(radiusDp)
            strokeColor?.let { setStroke(context.dp(strokeWidthDp).toInt().coerceAtLeast(1), it) }
        }
    }

    fun ripple(
        context: Context,
        fillColor: Int,
        radiusDp: Float,
        strokeColor: Int? = null,
        strokeWidthDp: Float = 1f,
        rippleColor: Int = withAlpha(light.primary, 0.14f)
    ): RippleDrawable {
        val content = surface(context, fillColor, radiusDp, strokeColor, strokeWidthDp)
        val mask = surface(context, Color.WHITE, radiusDp)
        return RippleDrawable(ColorStateList.valueOf(rippleColor), content, mask)
    }

    fun buttonBackground(context: Context, style: Md3ButtonStyle): RippleDrawable {
        return when (style) {
            Md3ButtonStyle.Filled -> ripple(context, light.primary, 999f, rippleColor = withAlpha(light.onPrimary, 0.18f))
            Md3ButtonStyle.FilledTonal -> ripple(context, light.secondaryContainer, 999f)
            Md3ButtonStyle.Outlined -> ripple(context, Color.TRANSPARENT, 999f, light.outline)
            Md3ButtonStyle.Text -> ripple(context, Color.TRANSPARENT, 999f)
        }
    }

    fun buttonTextColor(style: Md3ButtonStyle): Int {
        return when (style) {
            Md3ButtonStyle.Filled -> light.onPrimary
            Md3ButtonStyle.FilledTonal -> light.onSecondaryContainer
            Md3ButtonStyle.Outlined,
            Md3ButtonStyle.Text -> light.primary
        }
    }

    fun applyTextStyle(view: TextView, style: Md3TextStyle, color: Int) {
        val spec = when (style) {
            Md3TextStyle.HeadlineMedium -> TypeSpec(28f, 400)
            Md3TextStyle.HeadlineSmall -> TypeSpec(24f, 400)
            Md3TextStyle.TitleLarge -> TypeSpec(22f, 400)
            Md3TextStyle.TitleMedium -> TypeSpec(16f, 500)
            Md3TextStyle.TitleSmall -> TypeSpec(14f, 500)
            Md3TextStyle.BodyLarge -> TypeSpec(16f, 400)
            Md3TextStyle.BodyMedium -> TypeSpec(14f, 400)
            Md3TextStyle.BodySmall -> TypeSpec(12f, 400)
            Md3TextStyle.LabelLarge -> TypeSpec(14f, 500)
            Md3TextStyle.LabelMedium -> TypeSpec(12f, 500)
        }
        view.textSize = spec.sizeSp
        view.typeface = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Typeface.create(Typeface.DEFAULT, spec.weight, false)
        } else if (spec.weight >= 500) {
            Typeface.DEFAULT_BOLD
        } else {
            Typeface.DEFAULT
        }
        view.setTextColor(color)
    }

    fun bindStateLayer(view: View, pressedScale: Float = 0.98f) {
        Md3Motion.bindPressFeedback(view, pressedScale)
    }

    fun withAlpha(color: Int, alpha: Float): Int {
        return Color.argb(
            (255 * alpha).toInt().coerceIn(0, 255),
            Color.red(color),
            Color.green(color),
            Color.blue(color)
        )
    }

    fun Context.dp(value: Float): Float = value * resources.displayMetrics.density

    private data class TypeSpec(val sizeSp: Float, val weight: Int)
}
