package com.example.translator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ─────────────────────────── Glass Palette ────────────────────────────
//
// Shared tokens for the liquid-glass look. Kept here so MainActivity and the
// floating overlay render the same material. "Glass" reads best over a dark
// backdrop, so fills are light-on-dark translucent layers, not solid colour.

object GlassPalette {
    // Accent (carried over from the previous neon theme for continuity).
    val Accent = Color(0xFF00D4AA)
    val AccentSecondary = Color(0xFF00B4D8)
    val AccentPurple = Color(0xFF7B2FBE)

    // Deep backdrop behind the glass — what shows through the translucency.
    val BackdropTop = Color(0xFF0D0D1A)
    val BackdropMid = Color(0xFF161630)
    val BackdropBottom = Color(0xFF1B1A3A)

    // Glass surface fills. These are WHITE at low alpha so the backdrop
    // refracts through, like frosted glass.
    val GlassFill = Color.White.copy(alpha = 0.07f)
    val GlassFillStrong = Color.White.copy(alpha = 0.12f)
    val GlassFillPressed = Color.White.copy(alpha = 0.18f)

    // Edge / specular highlights.
    val HighlightTop = Color.White.copy(alpha = 0.55f)   // bright top edge
    val HighlightTopSoft = Color.White.copy(alpha = 0.18f)
    val BorderGlass = Color.White.copy(alpha = 0.22f)
    val BorderSubtle = Color.White.copy(alpha = 0.10f)

    // Text.
    val TextPrimary = Color.White
    val TextSecondary = Color(0xFFB8C0D0)
    val TextMuted = Color(0xFF8B93A7)

    val ErrorText = Color(0xFFFF6B6B)
    val SuccessGreen = Color(0xFF00D4AA)

    val BgGradient = Brush.verticalGradient(listOf(BackdropTop, BackdropMid, BackdropBottom))
    val AccentGradient = Brush.horizontalGradient(listOf(Accent, AccentSecondary))
}

// ─────────────────────────── Glass Surfaces ────────────────────────────

/**
 * The liquid-glass material: a translucent frosted fill, a bright specular
 * highlight along the top edge, a soft inner vertical gradient (glass
 * "thickness"), a hairline border, and a soft drop shadow. Apply to any
 * already-clipped container.
 *
 * Usage: `Modifier.clip(shape).then(Modifier.liquidGlass(shape))` — clip first
 * so the highlight/shadow respect the rounded corners.
 */
fun Modifier.liquidGlass(
    shape: Shape,
    elevation: Dp = 10.dp,
    fillAlpha: Float = 0.10f,
    pressed: Boolean = false
): Modifier = this
    // Soft drop shadow (glass floating above the backdrop).
    .shadow(
        elevation = elevation,
        shape = shape,
        ambientColor = Color.Black.copy(alpha = 0.35f),
        spotColor = Color.Black.copy(alpha = 0.45f)
    )
    // Base frosted fill — lets the backdrop show through.
    .background(
        if (pressed) GlassPalette.GlassFillPressed
        else Color.White.copy(alpha = fillAlpha)
    )
    // Inner vertical gradient: brighter at top, fading down — glass thickness.
    .background(
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = if (pressed) 0.20f else 0.14f),
                Color.White.copy(alpha = 0.02f)
            )
        )
    )
    // Specular top-edge highlight: a thin bright band right at the top, like
    // light catching the rim of the glass. Drawn via drawBehind so it sits
    // inside the clipped shape.
    .drawBehind {
        val h = size.height
        // Bright specular line concentrated in the top ~12% then fading.
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    GlassPalette.HighlightTop,
                    GlassPalette.HighlightTopSoft,
                    Color.Transparent
                ),
                startY = 0f,
                endY = h * 0.18f
            ),
            topLeft = Offset.Zero,
            size = size
        )
    }
    // Hairline bright border (the glass rim).
    .border(
        width = 0.8.dp,
        brush = Brush.verticalGradient(
            colors = listOf(
                GlassPalette.BorderGlass,
                GlassPalette.BorderSubtle
            )
        ),
        shape = shape
    )

/**
 * Borderless liquid-glass variant for the **floating overlay**: the overlay
 * floats over arbitrary screen content (not the app's dark backdrop), so any
 * rim border AND any directional drop shadow read as a visible "box" outline.
 *
 * Notably this uses **no [shadow]** at all: Compose's `Modifier.shadow` lights
 * from the top-left, so its spot shadow always lands on the right/bottom edges
 * — exactly the persistent outline users see. Instead, separation comes from
 * the frosted fill itself plus the window's blur-behind. The result reads as
 * glass melting into the screen with no visible edge on any side.
 */
fun Modifier.liquidGlassOverlay(
    shape: Shape,
    fillAlpha: Float = 0.10f
): Modifier = this
    // Frosted fill — no shadow, no border. The semi-translucency + the window's
    // blur-behind provide all the depth; a shadow would only create a hard
    // right/bottom outline.
    .background(Color.White.copy(alpha = fillAlpha))
    // Inner vertical gradient (glass thickness) — kept subtle and even.
    .background(
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.02f))
        )
    )
    // A soft inner glow along the top instead of a hard border, so there's no
    // visible rim yet the glass still has dimension.
    .drawBehind {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.White.copy(alpha = 0.30f), Color.Transparent),
                startY = 0f,
                endY = size.height * 0.22f
            )
        )
    }

/**
 * A liquid-glass card container. Clips to [cornerRadius], applies the glass
 * material, and hosts [content] in a padded column.
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    elevation: Dp = 12.dp,
    fillAlpha: Float = 0.10f,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .then(Modifier.liquidGlass(shape, elevation = elevation, fillAlpha = fillAlpha)),
        content = content
    )
}

/**
 * A pill/capsule-shaped glass surface for buttons and tags.
 */
@Composable
fun LiquidGlassPill(
    modifier: Modifier = Modifier,
    pressed: Boolean = false,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .clip(shape)
            .then(Modifier.liquidGlass(shape, elevation = 6.dp, pressed = pressed)),
        propagateMinConstraints = true
    ) {
        content()
    }
}

/** Convenience: an accent-rimmed glass border for emphasized elements. */
fun Modifier.glassAccentBorder(shape: Shape): Modifier =
    this.border(BorderStroke(1.dp, GlassPalette.Accent.copy(alpha = 0.4f)), shape)
