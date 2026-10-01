package com.tomer.chitchat.ui.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import kotlin.concurrent.thread

class ViewOnlineIndi : View {


    //region CONSTRUCTOR

    constructor(con: Context) : super(con)

    constructor(con: Context, attributeSet: AttributeSet) : super(con, attributeSet)

    constructor(context: Context, attr: AttributeSet, defStyle: Int) : super(context, attr, defStyle)

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        aniMat = false
    }

    //endregion CONSTRUCTOR

    //region GLOBALS-->>>

    private val colorBalls = Color.parseColor("#FF0AB68B")
    private val pBlur = Paint().apply {
        color = colorBalls
        maskFilter = BlurMaskFilter(30f, BlurMaskFilter.Blur.NORMAL)
        isAntiAlias = true
    }

    private var aniMat = true
    private val frameDelay = 120L

    private var rotationDeg = 0f
    private var scaleFactor = 0f

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    //endregion GLOBALS-->>>


    override fun onDraw(canvas: Canvas) {
        val half = width.div(2f)
        val fourth = width.div(4f)
        val eighth = width.div(8f)
        canvas.rotate(rotationDeg, half, half)
        canvas.scale(scaleFactor, scaleFactor, half, half)
        canvas.drawCircle(half, half - eighth, fourth, pBlur)
        canvas.drawCircle(half, half + eighth, fourth, pBlur)
        canvas.drawCircle(half - eighth, half, fourth, pBlur)
        canvas.drawCircle(half + eighth, half, fourth, pBlur)
    }

    fun setImmediateStatus(isOnline: Boolean) {
        scaleFactor = if (isOnline) 1f else 0f
        postInvalidate()
        if (isOnline) {
            if (aniMat) return
            aniMat = true
            thread {
                while (aniMat) {
                    SystemClock.sleep(frameDelay)
                    rotationDeg += 10
                    if (rotationDeg == 360f) rotationDeg = 0f
                    postInvalidate()
                }
            }
        } else aniMat = false
    }

    fun setStatusAnimating(isOnline: Boolean) {
        if (isOnline) {
            if (aniMat) return
            aniMat = true
            thread {
                while (aniMat) {
                    SystemClock.sleep(frameDelay)
                    rotationDeg += 10
                    if (rotationDeg == 360f) rotationDeg = 0f
                    postInvalidate()
                }
            }
        } else aniMat = false

        val animator = if (isOnline) ValueAnimator.ofFloat(0f, 1f)
        else ValueAnimator.ofFloat(1f, 0f)
        animator.apply {
            addUpdateListener {
                scaleFactor = it.animatedValue as Float
                postInvalidate()
            }
            duration = 200
            start()
        }

    }

}