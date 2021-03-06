package lol.adel.graph.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.SparseArray
import android.view.MotionEvent
import android.view.View
import help.*
import lol.adel.graph.MinMax
import lol.adel.graph.R
import lol.adel.graph.YAxis
import lol.adel.graph.data.Chart
import lol.adel.graph.data.ChartType
import lol.adel.graph.len
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

@SuppressLint("ViewConstructor")
class ScrollBarView(ctx: Context, private val cameraX: MinMax, private val data: Chart) : View(ctx) {

    private companion object {
        private val HANDLE_WIDTH = 10.dp
        val halfHandle = HANDLE_WIDTH / 2

        private val HANDLE_HEIGHT = 1.dpF
        private val TOUCH_SIZE = 48.dp
        private val TWELVE = 12.dpF

        val off = 18.dpF
    }

    private sealed class Handle {

        object Left : Handle()

        object Right : Handle()

        data class Between(
            val left: X,
            val right: X,
            val x: X
        ) : Handle()
    }

    interface Listener {
        fun onBoundsChange(left: Float, right: Float)
    }

    private val background = Paint().apply { color = ctx.color(R.attr.scroll_overlay) }
    private val foreground = Paint().apply { color = ctx.color(R.attr.scroll_overlay_bright) }

    private val leftHandle = ctx.getDrawable(R.drawable.left_handle)!!
    private val rightHandle = ctx.getDrawable(R.drawable.right_handle)!!

    var listener: Listener? = null

    private var left: PxF = -1f
    private var right: PxF = -1f

    private val dragging: SparseArray<Handle> = SparseArray()

    init {
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
    }

    private fun around(x: X, view: X): Boolean =
        abs(x - view) <= TOUCH_SIZE / 2

    private fun set(left: Float, right: Float) {
        this.left = left
        this.right = right
        listener?.onBoundsChange(left = left / width, right = right / width)
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w == oldw) return

        val lastIndex = data.size - 1

        val realIdxRange = cameraX.len()
        val barWidth = if (data.type == ChartType.BAR) (w / realIdxRange) else 0f
        val extraIdx = realIdxRange / w * (YAxis.SIDE_PADDING + barWidth / 2)

        left = norm(cameraX.min, -extraIdx, lastIndex + extraIdx) * w
        right = norm(cameraX.max, -extraIdx, lastIndex + extraIdx) * w
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        event.multiTouch(
            down = { pointerId, evX, _ ->
                val draggingSize = dragging.size()
                if (draggingSize == 1 && dragging.valueAt(0) is Handle.Between) {
                    return true
                }

                if (dragging[pointerId] == null && draggingSize < 2) {
                    val handle = when {
                        evX in (left + TWELVE)..(right - TWELVE) && draggingSize == 0 ->
                            Handle.Between(left = left, right = right, x = evX)

                        around(evX, left) ->
                            Handle.Left

                        around(evX, right) ->
                            Handle.Right

                        else ->
                            null
                    }
                    dragging.put(pointerId, handle)

                    parent.requestDisallowInterceptTouchEvent(true)
                }
            },
            move = { pointerId, evX, _ ->
                when (val handle = dragging[pointerId]) {
                    Handle.Left ->
                        set(clamp(evX, 0f, right - 48.dp), right)

                    Handle.Right ->
                        set(left, clamp(evX, left + 48.dp, widthF))

                    is Handle.Between -> {
                        val diff = evX - handle.x
                        val newLeft = handle.left + diff
                        val newRight = handle.right + diff
                        val distance = handle.right - handle.left

                        when {
                            newLeft >= 0 && newRight < width ->
                                set(newLeft, newRight)

                            newLeft <= 0 ->
                                set(left = 0f, right = distance)

                            newRight >= widthF ->
                                set(left = max(0f, width - 1 - distance), right = width - 1f)
                        }
                    }
                }
            },
            up = { pointerId, _, _ ->
                dragging.delete(pointerId)

                parent.requestDisallowInterceptTouchEvent(false)
            }
        )

        return true
    }

    private val paint = Paint().apply {
        color = Color.WHITE
        strokeWidth = 2.dpF
        isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = widthF
        val height = heightF

        canvas.drawRect(0f, HANDLE_HEIGHT, left + HANDLE_WIDTH, height - HANDLE_HEIGHT, background)
        canvas.drawRect(right - HANDLE_WIDTH, HANDLE_HEIGHT, width, height - HANDLE_HEIGHT, background)

        leftHandle.setBounds(left.roundToInt(), 0, (left + HANDLE_WIDTH).roundToInt(), height.toInt())
        leftHandle.draw(canvas)
        canvas.drawLine(left + halfHandle, off, left + halfHandle, height - off, paint)

        rightHandle.setBounds((right - HANDLE_WIDTH).roundToInt(), 0, right.roundToInt(), height.toInt())
        rightHandle.draw(canvas)
        canvas.drawLine(right - halfHandle, off, right - halfHandle, height - off, paint)

        canvas.drawRect(left + HANDLE_WIDTH, 0f, right - HANDLE_WIDTH, HANDLE_HEIGHT, foreground)
        canvas.drawRect(left + HANDLE_WIDTH, height - HANDLE_HEIGHT, right - HANDLE_WIDTH, height, foreground)
    }
}
