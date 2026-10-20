package com.michael.myandroidai

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.SystemClock
import android.view.View
import kotlin.math.max

/** Lightweight native robot face; eyes blink without external images or network assets. */
class RobotFaceView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val face = RectF()
    private val eyeLeft = RectF()
    private val eyeRight = RectF()
    private val startedAt = SystemClock.uptimeMillis()

    init {
        contentDescription = "Animated AI robot companion"
        isClickable = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val unit = max(1f, minOf(w, h))
        val cx = w / 2f
        val cy = h / 2f

        paint.color = 0xFF111D30.toInt()
        face.set(w * 0.12f, h * 0.10f, w * 0.88f, h * 0.88f)
        canvas.drawRoundRect(face, unit * 0.23f, unit * 0.23f, paint)

        paint.color = 0xFF53D29D.toInt()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = max(1.5f, unit * 0.035f)
        canvas.drawRoundRect(face, unit * 0.23f, unit * 0.23f, paint)
        paint.style = Paint.Style.FILL

        val elapsed = (SystemClock.uptimeMillis() - startedAt) % 3600L
        val blinking = elapsed in 3150L..3270L
        val eyeHeight = if (blinking) unit * 0.025f else unit * 0.13f
        eyeLeft.set(cx - unit * 0.25f, cy - eyeHeight / 2f, cx - unit * 0.08f, cy + eyeHeight / 2f)
        eyeRight.set(cx + unit * 0.08f, cy - eyeHeight / 2f, cx + unit * 0.25f, cy + eyeHeight / 2f)
        paint.color = 0xFF6EE7FF.toInt()
        canvas.drawRoundRect(eyeLeft, unit * 0.08f, unit * 0.08f, paint)
        canvas.drawRoundRect(eyeRight, unit * 0.08f, unit * 0.08f, paint)

        paint.color = 0xFFA78BFA.toInt()
        canvas.drawRoundRect(
            cx - unit * 0.13f, cy + unit * 0.22f,
            cx + unit * 0.13f, cy + unit * 0.27f,
            unit * 0.03f, unit * 0.03f, paint
        )
        postInvalidateDelayed(90L)
    }
}
