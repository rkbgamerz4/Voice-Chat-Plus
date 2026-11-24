package com.voicechatplus.android

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.roundToInt

/** Displays the real microphone level and a decaying peak marker. */
class LevelMeterView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bounds = RectF()
    private var current = 0f
    private var peak = 0f

    fun update(level: Float, peakLevel: Float) {
        current = level.coerceIn(0f, 1f)
        peak = peakLevel.coerceIn(0f, 1f)
        invalidate()
    }

    fun reset() {
        update(0f, 0f)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val radius = height / 2f
        bounds.set(0f, 0f, width.toFloat(), height.toFloat())

        paint.color = Color.parseColor("#09121E")
        canvas.drawRoundRect(bounds, radius, radius, paint)

        val filled = (width * current).roundToInt()
        if (filled > 1) {
            paint.color = Color.parseColor("#00A8FF")
            canvas.drawRoundRect(RectF(0f, 0f, filled.toFloat(), height.toFloat()), radius, radius, paint)
        }

        val peakX = width * peak
        if (peakX > 1f) {
            paint.color = Color.parseColor("#FFFFFF")
            canvas.drawRect(peakX - 2f, 0f, peakX + 2f, height.toFloat(), paint)
        }
    }
}
