package com.tomer.chitchat.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import com.tomer.chitchat.R
import com.tomer.chitchat.utils.Utils.Companion.px
import kotlin.math.abs

class VideoTrimView : View {

    //region CONSTRUCTOR
    constructor(con: Context) : super(con)
    constructor(con: Context, attributeSet: AttributeSet) : super(con, attributeSet)
    constructor(context: Context, attr: AttributeSet, defStyle: Int) : super(
        context,
        attr,
        defStyle
    )
    //endregion

    //region GLOBALS
    private val handleWidth = 16.px
    private val borderThickness = 2.px
    private val touchTargetPadding = 24.px

    private val overlayColor = "#99000000".toColorInt()
    private val activeColor = "#FFFFFF".toColorInt()

    private val overlayPaint = Paint().apply { style = Paint.Style.FILL; color = overlayColor }
    private val blackPaint = Paint().apply { style = Paint.Style.FILL; color = "#000000".toColorInt() }
    private val borderPaint = Paint().apply { style = Paint.Style.FILL; color = activeColor }
    private val progressPaint = Paint().apply {
        style = Paint.Style.FILL; color = activeColor; strokeWidth = borderThickness
    }
    private val pointsPaint = Paint().apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.primary)
        isAntiAlias = true
    }

    // State positions (0.0 to 1.0)
    private var startPercent = 0.0f
    private var endPercent = 1.0f
    private var currentPercent = 0f

    private var activeThumb: Thumb? = null
    private var isTouchDown = false
    private val dotRadius = 2f.px
    private val dotGap = 7.px
    private var centerY = 0f

    enum class Thumb { LEFT, RIGHT }

    var onRangeChangeListener: ((start: Float, end: Float) -> Unit)? = null
    var onTouchDownListener: ((isDown: Boolean, start: Float, curr: Float, end: Float) -> Unit)? =
        null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerY = h / 2f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        // --- KEY LOGIC CHANGE ---
        // The "Playable Area" is inset by handleWidth on both sides.
        // Video 0% starts at x = handleWidth
        // Video 100% ends at x = Width - handleWidth
        val timelineWidth = w - (2 * handleWidth)

        // Calculate the Cut Points (The Inner Edges)
        val startX = handleWidth + (startPercent * timelineWidth)
        val endX = handleWidth + (endPercent * timelineWidth)

        // Calculate Progress Pointer
        // It must also map to the timelineWidth
        val progressX = handleWidth + (currentPercent * timelineWidth)

        // 1. Draw Dimmed Overlays
        // Left Overlay: From 0 to startX (The startX IS the inner edge of left handle)
        canvas.drawRect(0f, 0f, startX, h, overlayPaint)
        // Right Overlay: From endX to Width
        canvas.drawRect(endX, 0f, w, h, overlayPaint)

        // 2. Draw Top/Bottom Borders
        // STRICTLY connecting the inner faces (startX to endX)
        canvas.drawRect(startX, 0f, endX, borderThickness, borderPaint) // Top
        canvas.drawRect(startX, h - borderThickness, endX, h, borderPaint) // Bottom

        // 3. Draw Handles
        // Left Handle: Sits to the LEFT of startX
        // Rect: [startX - handleWidth] to [startX]
        canvas.drawRect(startX - handleWidth, 0f, startX, h, borderPaint)

        // Right Handle: Sits to the RIGHT of endX
        // Rect: [endX] to [endX + handleWidth]
        canvas.drawRect(endX, 0f, endX + handleWidth, h, borderPaint)

        // 4. Draw Progress Pointer
        // Only draw if within bounds (optional, but looks cleaner)
        if (!isTouchDown) {
            canvas.drawRect(
                progressX - (borderThickness / 2),
                0f,
                progressX + (borderThickness / 2),
                h,
                progressPaint
            )
        }

        // 5. Draw Dots (Centered inside the handles)
        drawDots(canvas, startX - (handleWidth / 2f)) // Center of Left Handle
        drawDots(canvas, endX + (handleWidth / 2f))   // Center of Right Handle
    }

    private fun drawDots(canvas: Canvas, xCenter: Float) {
        canvas.drawCircle(xCenter, centerY - dotGap, dotRadius, pointsPaint)
        canvas.drawCircle(xCenter, centerY, dotRadius, pointsPaint)
        canvas.drawCircle(xCenter, centerY + dotGap, dotRadius, pointsPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val w = width.toFloat()
        val timelineWidth = w - (2 * handleWidth)

        // Reverse Math: Convert screen X back to Percentage
        // P = (ScreenX - Offset) / TimelineWidth
        val rawPercent = (x - handleWidth) / timelineWidth
        val touchPercent = rawPercent.coerceIn(0f, 1f)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                onTouchDownListener?.invoke(true, startPercent, currentPercent, endPercent)
                isTouchDown = true

                // Calculate where the handles ARE currently
                val startX = handleWidth + (startPercent * timelineWidth)
                val endX = handleWidth + (endPercent * timelineWidth)

                // Hit Test: Check distance to the CENTER of the handle visual
                // Left Handle Center is (startX - halfWidth)
                val leftHandleCenter = startX - (handleWidth / 2f)
                // Right Handle Center is (endX + halfWidth)
                val rightHandleCenter = endX + (handleWidth / 2f)

                val distToStart = abs(x - leftHandleCenter)
                val distToEnd = abs(x - rightHandleCenter)

                val hitZone = handleWidth + touchTargetPadding

                if (distToStart < hitZone && distToStart < distToEnd) {
                    activeThumb = Thumb.LEFT
                } else if (distToEnd < hitZone) {
                    activeThumb = Thumb.RIGHT
                } else {
                    return false
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (activeThumb == null) return false

                // Minimum gap (e.g. 5% of timeline)
                val minGap = 0.05f

                when (activeThumb) {
                    Thumb.LEFT -> {
                        startPercent = touchPercent.coerceIn(0f, endPercent - minGap)
                    }

                    Thumb.RIGHT -> {
                        endPercent = touchPercent.coerceIn(startPercent + minGap, 1f)
                    }

                    else -> {}
                }

                onRangeChangeListener?.invoke(startPercent, endPercent)
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isTouchDown = false
                activeThumb = null
                onTouchDownListener?.invoke(false, startPercent, currentPercent, endPercent)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    fun setProgress(current: Float) {
        if (!isTouchDown) {
            currentPercent = current.coerceIn(startPercent, endPercent)
            invalidate()
        }
    }

    fun setRange(start: Float, end: Float) {
        startPercent = start.coerceIn(0f, 1f)
        endPercent = end.coerceIn(startPercent, 1f)
        invalidate()
    }
}