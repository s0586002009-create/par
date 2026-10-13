package il.co.aniparve

import android.content.Context
import android.view.View
import android.view.ViewGroup

/** Small dependency-free wrapping layout for action chips; works on narrow screens too. */
class FlowChipLayout(context: Context) : ViewGroup(context) {
    class LayoutParams : ViewGroup.MarginLayoutParams {
        constructor(width: Int, height: Int) : super(width, height)
        constructor(p: ViewGroup.LayoutParams) : super(p)
    }

    private val horizontalGap = (6 * resources.displayMetrics.density).toInt()
    private val verticalGap = (6 * resources.displayMetrics.density).toInt()

    override fun generateDefaultLayoutParams() = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    override fun generateLayoutParams(attrs: android.util.AttributeSet?) = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    override fun generateLayoutParams(p: ViewGroup.LayoutParams?) = if (p == null) LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT) else LayoutParams(p)
    override fun checkLayoutParams(p: ViewGroup.LayoutParams?) = p is LayoutParams

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val available = (width - paddingLeft - paddingRight).coerceAtLeast(0)
        var x = 0
        var y = 0
        var lineHeight = 0
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == View.GONE) continue
            measureChild(child, widthMeasureSpec, heightMeasureSpec)
            val lp = child.layoutParams
            val margins = lp as? ViewGroup.MarginLayoutParams
            val cw = child.measuredWidth + (margins?.let { it.leftMargin + it.rightMargin } ?: 0)
            val ch = child.measuredHeight + (margins?.let { it.topMargin + it.bottomMargin } ?: 0)
            if (x > 0 && x + cw > available) { x = 0; y += lineHeight + verticalGap; lineHeight = 0 }
            x += cw + horizontalGap
            lineHeight = maxOf(lineHeight, ch)
        }
        val desiredHeight = y + lineHeight + paddingTop + paddingBottom
        setMeasuredDimension(resolveSize(width, widthMeasureSpec), resolveSize(desiredHeight, heightMeasureSpec))
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val available = (r - l - paddingLeft - paddingRight).coerceAtLeast(0)
        var x = paddingLeft
        var y = paddingTop
        var lineHeight = 0
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == View.GONE) continue
            val lp = child.layoutParams as? ViewGroup.MarginLayoutParams
            val leftMargin = lp?.leftMargin ?: 0
            val rightMargin = lp?.rightMargin ?: 0
            val topMargin = lp?.topMargin ?: 0
            val bottomMargin = lp?.bottomMargin ?: 0
            val cw = child.measuredWidth + leftMargin + rightMargin
            val ch = child.measuredHeight + topMargin + bottomMargin
            if (x > paddingLeft && x + cw > paddingLeft + available) { x = paddingLeft; y += lineHeight + verticalGap; lineHeight = 0 }
            val childLeft = x + leftMargin
            child.layout(childLeft, y + topMargin, childLeft + child.measuredWidth, y + topMargin + child.measuredHeight)
            x += cw + horizontalGap
            lineHeight = maxOf(lineHeight, ch)
        }
    }
}