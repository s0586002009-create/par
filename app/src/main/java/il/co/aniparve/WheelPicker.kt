package il.co.aniparve

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Lightweight reusable picker: draws the selected value itself, without NumberPicker
 * internals or reflection. One vertical swipe changes one or more steps.
 */
class WheelPicker @JvmOverloads constructor(
    context: Context,
    var caption: String = ""
) : View(context) {
    private val density = resources.displayMetrics.density
    private fun dp(v: Float) = v * density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var formatter: ((Int) -> String)? = null
    var minValue: Int = 0
        set(v) { field = v; value = value.coerceAtLeast(v); invalidate() }
    var maxValue: Int = 59
        set(v) { field = max(v, minValue); value = value.coerceAtMost(field); invalidate() }
    var value: Int = 0
        set(v) { field = v.coerceIn(minValue, maxValue); invalidate() }
    private var downY = 0f
    private var lastY = 0f
    private var moved = false

    init {
        isFocusable = true
        contentDescription = caption
        minimumWidth = dp(76f).toInt()
        minimumHeight = dp(176f).toInt()
    }

    fun setFormatter(f: ((Int) -> String)?) {
        formatter = f
        invalidate()
    }

    private fun label(v: Int): String = formatter?.invoke(v) ?: String.format(java.util.Locale.US, "%02d", v)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = resolveSize(dp(92f).toInt(), widthMeasureSpec)
        val h = resolveSize(dp(176f).toInt(), heightMeasureSpec)
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val centerY = h * 0.43f
        val textX = w / 2f
        val dark = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val primary = if (dark) android.graphics.Color.WHITE else android.graphics.Color.rgb(55, 43, 34)
        val copper = android.graphics.Color.rgb(183, 111, 69)
        paint.style = Paint.Style.FILL
        paint.color = if (dark) android.graphics.Color.argb(45, 220, 180, 145) else android.graphics.Color.argb(38, 183, 111, 69)
        canvas.drawRoundRect(RectF(w * .12f, centerY - dp(22f), w * .88f, centerY + dp(22f)), dp(13f), dp(13f), paint)

        paint.textAlign = Paint.Align.CENTER
        paint.typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
        paint.textSize = dp(16f)
        paint.color = if (dark) 0x88FFFFFF.toInt() else 0x77574A40
        if (value > minValue) canvas.drawText(label(value - 1), textX, centerY - dp(42f), paint)
        if (value < maxValue) canvas.drawText(label(value + 1), textX, centerY + dp(55f), paint)

        paint.color = primary
        paint.typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
        paint.textSize = dp(23f)
        canvas.drawText(label(value), textX, centerY + dp(8f), paint)

        if (caption.isNotBlank()) {
            paint.color = copper
            paint.typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
            paint.textSize = dp(12f)
            canvas.drawText(caption, textX, h - dp(7f), paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downY = event.y
                lastY = event.y
                moved = false
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val delta = lastY - event.y
                val step = dp(24f)
                if (abs(event.y - downY) > dp(8f)) moved = true
                if (abs(delta) >= step) {
                    val count = (abs(delta) / step).toInt().coerceAtLeast(1)
                    value = (value + if (delta > 0) count else -count).coerceIn(minValue, maxValue)
                    lastY = event.y
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (!moved && abs(event.y - downY) < dp(8f)) {
                    value = (value + if (event.y < height / 2f) 1 else -1).coerceIn(minValue, maxValue)
                }
                performClick()
                return true
            }
            MotionEvent.ACTION_CANCEL -> return true
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
