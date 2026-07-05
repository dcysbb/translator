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
