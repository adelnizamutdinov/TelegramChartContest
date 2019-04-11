package lol.adel.graph.widget.chart

import android.graphics.Canvas
import android.graphics.Paint
import help.*
import lol.adel.graph.R
import lol.adel.graph.get
import lol.adel.graph.len
import lol.adel.graph.norm
import lol.adel.graph.widget.ChartView

class BarDrawer(override val view: ChartView) : ChartDrawer {

    override fun makePaint(clr: ColorInt): Paint =
        Paint().apply {
            style = Paint.Style.STROKE
            color = clr
        }

    override fun labelColor(): ColorInt =
        view.color(R.attr.label_text_bars)

    override fun maxLabelAlpha(): Norm =
        0.5f

    override fun draw(canvas: Canvas) {
        val (start, end) = view.cameraX

        val axis = view.yAxis
        val cameraY = axis.camera
        val height = view.heightF
        val eHeight = axis.effectiveHeight()
        val width = view.widthF
        val startF = start.floor()
        val endC = end.ceil()
        val range = endC - startF
        val barWidth = width / view.cameraX.len()
        val columns = view.animatedColumns
        val buf = view.lineBuf
        val stackSize = range * 4

        var x = view.mapX(startF, width)

        val dimensions = columns.size()

        for (i in startF..endC) {
            val iOffset = (i - startF) * 4
            var y = height
            for (j in 0 until dimensions) {
                val column = columns.valueAt(j)
                if (column.frac > 0) {
                    val barHeight = cameraY.norm(column[i]) * eHeight

                    val bufIdx = j * stackSize + iOffset
                    buf[bufIdx + 0] = x
                    buf[bufIdx + 1] = y - barHeight
                    buf[bufIdx + 2] = x
                    buf[bufIdx + 3] = y

                    y -= barHeight
                }
            }

            x += barWidth
        }

        for (j in 0 until dimensions) {
            val column = columns.valueAt(j)
            if (column.frac > 0) {
                column.paint.strokeWidth = barWidth
                canvas.drawLines(buf, j * stackSize, stackSize, column.paint)
            }
        }

        // TODO touching fade

        view.drawYLines(canvas, width)
    }
}
