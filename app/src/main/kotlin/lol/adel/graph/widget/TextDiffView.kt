package lol.adel.graph.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.Typeface
import android.text.TextPaint
import android.view.Gravity
import android.view.View
import help.*
import lol.adel.graph.Interpolators
import lol.adel.graph.R
import lol.adel.graph.Typefaces

class TextDiffView(ctx: Context) : View(ctx) {

    private val newBounds = Rect()

    private val oldPaint = TextPaint().apply {
        isAntiAlias = true
    }
    private val newPaint = TextPaint().apply {
        isAntiAlias = true
    }
    private val unchangedPaint = TextPaint().apply {
        isAntiAlias = true
    }

    private var frac = 1f
    private val anim = valueAnimator().apply {
        interpolator = Interpolators.DECELERATE
        addUpdateListener {
            frac = it.animatedFraction
            invalidate()
        }
        duration = 200
    }

    private var splitIdx: Idx = 0
    private var prevText: String = ""

    var text: String = ""
        set(value) {
            if (field != value) {
                splitIdx = if (fullFlip) 0 else firstChangeFromEnd(field, value)
                unchangedPaint.getTextBounds(value, 0, value.length - splitIdx, newBounds)
                prevText = field

                if (field != "") {
                    anim.restart()
                }

                field = value
            }
        }

    private val paints = listOf(oldPaint, newPaint, unchangedPaint)

    var fullFlip: Boolean = false

    var typeface: Typeface = Typefaces.normal
        set(value) {
            field = value
            paints.forEachByIndex { it.typeface = value }
            invalidate()
        }

    var textColor: ColorInt = ctx.color(R.attr.label_text)
        set(value) {
            field = value
            paints.forEachByIndex { it.color = value }
            invalidate()
        }

    var textSizeDp: Float = 14.dpF
        set(value) {
            field = value
            paints.forEachByIndex { it.textSize = value }
            invalidate()
        }

    var gravity: Int = Gravity.START

    init {
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val height = heightF
        val width = widthF

        val blankX = newBounds.width().toFloat()
        val halfBlankX = blankX / 2

        val descent = unchangedPaint.descent()

        val textLen = text.length

        run {
            val textStart = textLen - splitIdx
            val startX = when (gravity) {
                Gravity.START ->
                    blankX

                else ->
                    width - unchangedPaint.measureText(text, textStart, textLen)
            }
            canvas.drawText(text, textStart, textLen, startX, height / 2 + descent, unchangedPaint)
        }

        run {
            val oldFrac = 1 - frac
            val halfFrac = denorm(oldFrac, 0.5f, 1f)

            oldPaint.alphaF = oldFrac
            oldPaint.textSize = textSizeDp * halfFrac

            val startX = when (gravity) {
                Gravity.START ->
                    0f

                else ->
                    width - unchangedPaint.measureText(prevText)
            }

            val x = denorm(halfFrac, startX + halfBlankX, startX)
            val y = denorm(frac, height / 2, 0f) + descent

            canvas.drawText(prevText, 0, prevText.length - splitIdx, x, y, oldPaint)
        }

        run {
            val halfFrac = denorm(frac, 0.5f, 1f)

            newPaint.alphaF = frac
            newPaint.textSize = textSizeDp * halfFrac

            val startX = when (gravity) {
                Gravity.START ->
                    0f

                else ->
                    width - unchangedPaint.measureText(text)
            }

            val x = denorm(halfFrac, startX + halfBlankX, startX)
            val y = denorm(frac, height, height / 2) + descent

            canvas.drawText(text, 0, textLen - splitIdx, x, y, newPaint)
        }
    }
}
