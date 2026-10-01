package com.tomer.chitchat.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import com.tomer.chitchat.utils.qrProvider.BackgroundProvider
import com.tomer.chitchat.utils.qrProvider.GradModel

class MsgBackground : View {


    //region CONSTRUCTOR

    constructor(con: Context) : super(con)

    constructor(con: Context, attributeSet: AttributeSet) : super(con, attributeSet)

    constructor(context: Context, attr: AttributeSet, defStyle: Int) : super(context, attr, defStyle)


    //endregion CONSTRUCTOR

    //region GLOBALS-->>>

    private var isSent: Boolean? = null
    private var canShow = true
    private var cornerRadius = 20f
    private val bounds = RectF()
    private var gradModel: GradModel? = null
    private val paint = Paint().apply {
        isAntiAlias = true
    }

    //endregion GLOBALS-->>>

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        bounds.set(0f, 0f, w.toFloat(), h.toFloat())
        if (gradModel == null) return
        paint.shader = BackgroundProvider.createLinearGradient(Point(w, h), gradModel!!)

    }

    override fun onDraw(canvas: Canvas) {
        if (!canShow) return
        canvas.drawRoundRect(bounds, cornerRadius, cornerRadius, paint)
        if (isSent == null)
            return
        if (isSent!!)
            canvas.drawRect(bounds.width().times(.5f), bounds.height().times(.5f), bounds.right, bounds.bottom, paint)
        else canvas.drawRect(0f, bounds.height().times(.5f), bounds.width().times(.5f), bounds.bottom, paint)

    }

    //region COMMU

    fun setData(isSent: Boolean?, cornerRadius: Float, @ColorInt color: Int) {
        this.isSent = isSent
        this.gradModel = null
        this.cornerRadius = cornerRadius
        canShow = true
        paint.apply {
            this.color = color
            shader = null
        }
        postInvalidate()
    }

    fun setData(isSent: Boolean?, cornerRadius: Float, gradModel: GradModel) {
        this.isSent = isSent
        this.gradModel = gradModel
        paint.shader = BackgroundProvider.createLinearGradient(Point(width, height), gradModel)
        this.cornerRadius = cornerRadius
        canShow = true
        postInvalidate()
    }

    fun setColor(@ColorInt color: Int, gradModel: GradModel? = null) {
        if (isSent != false) return
        if (gradModel == null) {
            paint.apply {
                this.color = color
                shader = null
            }
        } else {
            this.gradModel = gradModel
            paint.shader = BackgroundProvider.createLinearGradient(Point(width, height), gradModel)
        }
        postInvalidate()
    }

    fun hideBg() {
        this.canShow = false
    }

    //endregion COMMU

}