package com.poozh.translator.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class SelectionOverlayView(
    context: Context,
    private val onSelected: (Rect) -> Unit,
    private val onCanceled: () -> Unit
) : View(context) {
    private val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(138, 0, 0, 0)
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Md3.withAlpha(Md3.light.primaryContainer, 0.24f)
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Md3.light.primary
        style = Paint.Style.STROKE
        strokeWidth = dp(2f)
    }
    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Md3.light.primary
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = dp(4f)
    }
    private val chipPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Md3.light.inverseOnSurface
        textSize = dp(14f)
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    private val sizePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Md3.light.onPrimary
        textSize = dp(12f)
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    private val rectF = RectF()
    private val chipRect = RectF()

    private var startX = 0f
    private var startY = 0f
    private var currentX = 0f
    private var currentY = 0f
    private var dragging = false

    override fun onDraw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), maskPaint)
        drawInstruction(canvas)
        if (!dragging) return

        val rect = currentRect()
        rectF.set(rect)
        canvas.drawRoundRect(rectF, dp(12f), dp(12f), fillPaint)
        canvas.drawRoundRect(rectF, dp(12f), dp(12f), borderPaint)
        drawHandles(canvas)
        drawSizeChip(canvas, rect)
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
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                currentX = event.x
                currentY = event.y
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP -> {
                currentX = event.x
                currentY = event.y
                val rect = currentRect()
                dragging = false
                invalidate()
                if (abs(event.x - startX) < dp(10f) && abs(event.y - startY) < dp(10f)) {
                    onCanceled()
                } else if (rect.width() >= dp(48f) && rect.height() >= dp(24f)) {
                    // Convert view-local coordinates to absolute screen coordinates.
                    // The overlay window does NOT start at the physical screen origin —
                    // on most devices it is laid out below the status bar (e.g.
                    // locOnScreen.y = 36). The captured bitmap covers the full physical
                    // screen, so without this offset the crop slides up by the status
                    // bar height vs. the box the user drew.
                    val loc = IntArray(2)
                    getLocationOnScreen(loc)
                    val screenRect = Rect(
                        rect.left + loc[0],
                        rect.top + loc[1],
                        rect.right + loc[0],
                        rect.bottom + loc[1]
                    )
                    onSelected(screenRect)
                } else {
                    onCanceled()
                }
                return true
            }
        }
        return true
    }

    private fun drawInstruction(canvas: Canvas) {
        val label = "拖动选区"
        val horizontal = dp(16f)
        val top = dp(16f)
        val textWidth = textPaint.measureText(label)
        chipRect.set(horizontal, top, horizontal + textWidth + dp(32f), top + dp(40f))
        chipPaint.color = Md3.light.inverseSurface
        canvas.drawRoundRect(chipRect, dp(20f), dp(20f), chipPaint)
        canvas.drawText(label, chipRect.left + dp(16f), chipRect.top + dp(25f), textPaint)
    }

    private fun drawHandles(canvas: Canvas) {
        val length = min(dp(28f), min(rectF.width(), rectF.height()) * 0.28f)
        canvas.drawLine(rectF.left, rectF.top, rectF.left + length, rectF.top, handlePaint)
        canvas.drawLine(rectF.left, rectF.top, rectF.left, rectF.top + length, handlePaint)
        canvas.drawLine(rectF.right, rectF.top, rectF.right - length, rectF.top, handlePaint)
        canvas.drawLine(rectF.right, rectF.top, rectF.right, rectF.top + length, handlePaint)
        canvas.drawLine(rectF.left, rectF.bottom, rectF.left + length, rectF.bottom, handlePaint)
        canvas.drawLine(rectF.left, rectF.bottom, rectF.left, rectF.bottom - length, handlePaint)
        canvas.drawLine(rectF.right, rectF.bottom, rectF.right - length, rectF.bottom, handlePaint)
        canvas.drawLine(rectF.right, rectF.bottom, rectF.right, rectF.bottom - length, handlePaint)
    }

    private fun drawSizeChip(canvas: Canvas, rect: Rect) {
        val label = "${rect.width()} x ${rect.height()}"
        val chipWidth = sizePaint.measureText(label) + dp(24f)
        val chipHeight = dp(30f)
        val minLeft = dp(16f)
        val maxLeft = max(minLeft, width - chipWidth - dp(16f))
        val chipLeft = rectF.left.coerceIn(minLeft, maxLeft)
        val chipTop = if (rectF.bottom + chipHeight + dp(12f) < height) {
            rectF.bottom + dp(8f)
        } else {
            rectF.top - chipHeight - dp(8f)
        }.coerceIn(dp(64f), height - chipHeight - dp(16f))
        chipRect.set(chipLeft, chipTop, chipLeft + chipWidth, chipTop + chipHeight)
        chipPaint.color = Md3.light.primary
        canvas.drawRoundRect(chipRect, dp(15f), dp(15f), chipPaint)
        canvas.drawText(label, chipRect.left + dp(12f), chipRect.top + dp(20f), sizePaint)
    }

    private fun currentRect(): Rect {
        val left = min(startX, currentX).toInt().coerceIn(0, max(width - 1, 0))
        val top = min(startY, currentY).toInt().coerceIn(0, max(height - 1, 0))
        val right = max(startX, currentX).toInt().coerceIn(left + 1, max(width, 1))
        val bottom = max(startY, currentY).toInt().coerceIn(top + 1, max(height, 1))
        return Rect(left, top, right, bottom)
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}
