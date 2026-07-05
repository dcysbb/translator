package com.example.translator

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Full-screen drag-to-select overlay. Renders a dimmed mask over the whole
 * screen; the user drags out a rectangle, which is cleared (shown bright) with
 * an accent border. On release, [onSelected] fires with the chosen [Rect] (in
 * view coordinates = screen pixels, since this is a full-screen window) if the
 * rect is large enough; a tiny drag (a tap) or too-small rect cancels via
 * [onCanceled].
 *
 * Ported from the codex v0.1.0 reference (com.poozh.translator) and rewritten
 * in idiomatic Kotlin.
 */
class SelectionOverlayView(
    context: Context,
    private val onSelected: (Rect) -> Unit,
    private val onCanceled: () -> Unit
) : View(context) {

    private val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(150, 0, 0, 0)
    }
    private val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(60, 15, 118, 110)
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(45, 212, 191)
        style = Paint.Style.STROKE
        strokeWidth = dp(2f)
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = dp(15f)
    }

    private var startX = 0f
    private var startY = 0f
    private var currentX = 0f
    private var currentY = 0f
    private var dragging = false

    override fun onDraw(canvas: Canvas) {
        // Dim the whole screen.
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), maskPaint)
        canvas.drawText("拖动选择要识别的屏幕区域，点按空白取消", dp(18f), dp(34f), textPaint)
        if (dragging) {
            val rect = currentRect()
            // Brighten the selected area (draw over the mask).
            canvas.drawRect(rect, clearPaint)
            canvas.drawRect(rect, borderPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x
                startY = event.y
                currentX = event.x
                currentY = event.y
                dragging = true
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                currentX = event.x
                currentY = event.y
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                currentX = event.x
                currentY = event.y
                val rect = currentRect()
                dragging = false
                invalidate()
                val movedEnough =
                    abs(event.x - startX) >= dp(10f) || abs(event.y - startY) >= dp(10f)
                when {
                    !movedEnough -> onCanceled()
                    rect.width() >= dp(48f) && rect.height() >= dp(24f) -> onSelected(rect)
                    else -> onCanceled() // too small
                }
            }
        }
        return true
    }

    private fun currentRect(): Rect {
        val left = min(startX, currentX).toInt().coerceIn(0, max(width - 1, 0))
        val top = min(startY, currentY).toInt().coerceIn(0, max(height - 1, 0))
        val right = max(startX, currentX).toInt().coerceIn(left + 1, max(width, 1))
        val bottom = max(startY, currentY).toInt().coerceIn(top + 1, max(height, 1))
        return Rect(left, top, right, bottom)
    }

    private fun dp(value: Float): Float =
        resources.displayMetrics.density * value
}
