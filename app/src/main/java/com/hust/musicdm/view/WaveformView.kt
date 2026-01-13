package com.hust.musicdm.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.random.Random
import androidx.core.graphics.toColorInt

class WaveformView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val waveformData = IntArray(50) { 5 + Random.nextInt(50) }
    private var progress = 0f
    private var onSeekListener: ((Float) -> Unit)? = null

    private val activePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val inactivePaint = Paint().apply {
        color = "#33FFFFFF".toColorInt()
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val barRect = RectF()
    private val cornerRadius = 8f

    fun setProgress(progress: Float) {
        this.progress = progress.coerceIn(0f, 1f)
        invalidate()
    }

    fun setOnSeekListener(listener: (Float) -> Unit) {
        this.onSeekListener = listener
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val barCount = waveformData.size
        val totalSpacing = 2f * (barCount - 1)
        val barWidth = (width - totalSpacing) / barCount.toFloat()
        val centerY = height / 2f

        waveformData.forEachIndexed { index, value ->
            val barHeight = (value * 1.1f).coerceAtLeast(8f)
            val left = index * (barWidth + 2f)
            val top = centerY - barHeight / 2f
            val right = left + barWidth
            val bottom = centerY + barHeight / 2f

            barRect.set(left, top, right, bottom)

            val paint = if (index < (barCount * progress).toInt()) {
                activePaint
            } else {
                inactivePaint
            }

            canvas.drawRoundRect(barRect, cornerRadius, cornerRadius, paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val percent = (event.x / width).coerceIn(0f, 1f)
                onSeekListener?.invoke(percent)
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}