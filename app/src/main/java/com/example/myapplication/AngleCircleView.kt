package com.example.myapplication

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class AngleCircleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var angleDegrees = 0f
    private var isDegree = true
    private var onAngleChanged: ((Float) -> Unit)? = null

    private val snapAngles = listOf(0f, 30f, 45f, 60f, 90f, 120f, 135f, 150f, 180f, 210f, 225f, 240f, 270f, 300f, 315f, 330f)

    private val degreeLabels = mapOf(
        0f to "0°", 30f to "30°", 45f to "45°", 60f to "60°", 90f to "90°",
        120f to "120°", 135f to "135°", 150f to "150°", 180f to "180°",
        210f to "210°", 225f to "225°", 240f to "240°", 270f to "270°",
        300f to "300°", 315f to "315°", 330f to "330°"
    )

    private val radianLabels = mapOf(
        0f to "0", 30f to "π/6", 45f to "π/4", 60f to "π/3", 90f to "π/2",
        120f to "2π/3", 135f to "3π/4", 150f to "5π/6", 180f to "π",
        210f to "7π/6", 225f to "5π/4", 240f to "4π/3", 270f to "3π/2",
        300f to "5π/3", 315f to "7π/4", 330f to "11π/6"
    )

    private val circlePaint = Paint().apply {
        color = Color.parseColor("#404045")
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }

    private val axisPaint = Paint().apply {
        color = Color.parseColor("#666666")
        strokeWidth = 2f
        isAntiAlias = true
    }

    private val tickPaint = Paint().apply {
        color = Color.parseColor("#888888")
        strokeWidth = 2f
        isAntiAlias = true
    }

    private val labelPaint = Paint().apply {
        color = Color.parseColor("#AAAAAA")
        textSize = 30f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    private val arrowPaint = Paint().apply {
        color = Color.parseColor("#FF9500")
        strokeWidth = 6f
        isAntiAlias = true
    }

    private val centerDotPaint = Paint().apply {
        color = Color.parseColor("#FF9500")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    fun setOnAngleChangedListener(listener: (Float) -> Unit) {
        onAngleChanged = listener
    }

    fun getAngleDegrees(): Float = angleDegrees

    fun setAngle(degrees: Float) {
        angleDegrees = degrees
        invalidate()
    }

    fun setMode(degree: Boolean) {
        isDegree = degree
        invalidate()
    }

    private fun snapToNearest(angle: Float): Float {
        var nearest = angle
        var minDiff = Float.MAX_VALUE
        for (snap in snapAngles) {
            var diff = Math.abs(angle - snap)
            if (diff > 180f) diff = 360f - diff
            if (diff < minDiff) {
                minDiff = diff
                nearest = snap
            }
        }
        return if (minDiff < 3f) nearest else angle
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val radius = Math.min(cx, cy) - 60f // Уменьшили радиус, чтобы подписи не наезжали

        // Рисуем круг
        canvas.drawCircle(cx, cy, radius, circlePaint)

        // Рисуем оси X и Y
        canvas.drawLine(cx - radius, cy, cx + radius, cy, axisPaint)
        canvas.drawLine(cx, cy - radius, cx, cy + radius, axisPaint)

        // Рисуем отметки и подписи в ключевых точках (кроме 90° сверху)
        for (snap in snapAngles) {

            val angleRad = Math.toRadians(snap.toDouble())
            val tickStartX = cx + (radius - 15f) * cos(angleRad).toFloat()
            val tickStartY = cy - (radius - 15f) * sin(angleRad).toFloat()
            val tickEndX = cx + radius * cos(angleRad).toFloat()
            val tickEndY = cy - radius * sin(angleRad).toFloat()

            canvas.drawLine(tickStartX, tickStartY, tickEndX, tickEndY, tickPaint)

            val labelRadius = radius + 35f // Увеличили отступ для подписей
            val labelX = cx + labelRadius * cos(angleRad).toFloat()
            val labelY = cy - labelRadius * sin(angleRad).toFloat()
            val label = if (isDegree) degreeLabels[snap] ?: "" else radianLabels[snap] ?: ""
            canvas.drawText(label, labelX, labelY, labelPaint)
        }

        // Рисуем стрелку
        val angleRad = Math.toRadians(angleDegrees.toDouble())
        val endX = cx + radius * cos(angleRad).toFloat()
        val endY = cy - radius * sin(angleRad).toFloat()

        canvas.drawLine(cx, cy, endX, endY, arrowPaint)
        canvas.drawCircle(cx, cy, 8f, centerDotPaint)
        canvas.drawCircle(endX, endY, 10f, centerDotPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val cx = width / 2f
                val cy = height / 2f
                val dx = event.x - cx
                val dy = -(event.y - cy)

                var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                if (angle < 0) angle += 360f

                angle = snapToNearest(angle)
                angleDegrees = angle
                onAngleChanged?.invoke(angleDegrees)
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}