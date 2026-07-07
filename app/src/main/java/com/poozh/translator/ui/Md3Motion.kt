package com.poozh.translator.ui

import android.view.MotionEvent
import android.view.View
import android.view.animation.PathInterpolator

object Md3Motion {
    private val emphasized = PathInterpolator(0.2f, 0f, 0f, 1f)
    private val emphasizedDecelerate = PathInterpolator(0.05f, 0.7f, 0.1f, 1f)
    private val emphasizedAccelerate = PathInterpolator(0.3f, 0f, 0.8f, 0.15f)

    fun bindPressFeedback(view: View, pressedScale: Float = 0.98f) {
        view.isClickable = true
        view.isFocusable = true
        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> press(v, pressedScale)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> release(v)
            }
            false
        }
    }

    fun press(view: View, scale: Float = 0.98f) {
        view.animate()
            .scaleX(scale)
            .scaleY(scale)
            .setDuration(100L)
            .setInterpolator(emphasized)
            .start()
    }

    fun release(view: View) {
        view.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(250L)
            .setInterpolator(emphasizedDecelerate)
            .start()
    }

    fun enter(view: View, fromY: Float = 24f) {
        view.alpha = 0f
        view.translationY = fromY
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(400L)
            .setInterpolator(emphasizedDecelerate)
            .start()
    }

    fun exit(view: View, endAction: () -> Unit) {
        view.animate()
            .alpha(0f)
            .translationY(12f)
            .setDuration(200L)
            .setInterpolator(emphasizedAccelerate)
            .withEndAction(endAction)
            .start()
    }

    /**
     * Scale the view INTO view from a small size, anchored at (pivotX, pivotY)
     * — typically the floating bubble's center — so the panel reads as growing
     * out of the bubble. Uses the M3 emphasizedDecelerate curve (non-linear,
     * fast-then-ease) for an organic pop. Also fades alpha 0→1. The final alpha
     * is [endAlpha] so it composes with the user's overlay-opacity setting.
     */
    fun scaleInFrom(
        view: View,
        pivotX: Float,
        pivotY: Float,
        startScale: Float = 0.35f,
        endAlpha: Float = 1f,
        duration: Long = 320L
    ) {
        view.pivotX = pivotX
        view.pivotY = pivotY
        view.scaleX = startScale
        view.scaleY = startScale
        view.alpha = 0f
        view.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(endAlpha)
            .setDuration(duration)
            .setInterpolator(emphasizedDecelerate)
            .start()
    }

    /**
     * Scale the view OUT toward (pivotX, pivotY) — the inverse of
     * [scaleInFrom]: the panel shrinks back into the bubble and fades.
     * emphasizedAccelerate (slow-then-fast) makes it feel "sucked in".
     */
    fun scaleOutTo(
        view: View,
        pivotX: Float,
        pivotY: Float,
        endScale: Float = 0.35f,
        duration: Long = 220L,
        endAction: () -> Unit
    ) {
        view.pivotX = pivotX
        view.pivotY = pivotY
        view.animate()
            .scaleX(endScale)
            .scaleY(endScale)
            .alpha(0f)
            .setDuration(duration)
            .setInterpolator(emphasizedAccelerate)
            .withEndAction(endAction)
            .start()
    }

    fun updateText(view: View, textChange: () -> Unit) {
        view.animate()
            .alpha(0.52f)
            .setDuration(100L)
            .setInterpolator(emphasized)
            .withEndAction {
                textChange()
                view.animate()
                    .alpha(1f)
                    .setDuration(250L)
                    .setInterpolator(emphasizedDecelerate)
                    .start()
            }
            .start()
    }
}
