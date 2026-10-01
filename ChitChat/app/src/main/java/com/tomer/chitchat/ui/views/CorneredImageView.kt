package com.tomer.chitchat.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

class CorneredImageView : AppCompatImageView {


    //region CONSTRUCTOR

    constructor(con: Context) : super(con)

    constructor(con: Context, attributeSet: AttributeSet) : super(con, attributeSet)

    constructor(context: Context, attr: AttributeSet, defStyle: Int) : super(context, attr, defStyle)


    //endregion CONSTRUCTOR

    //region GLOBALS-->>>

    private var cornerRadius = -1f
    private val bounds = RectF()
    private val path = Path()

    //endregion GLOBALS-->>>

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        bounds.set(0f, 0f, w.toFloat(), h.toFloat())
        if (cornerRadius == -1f) cornerRadius = w.toFloat()
    }

    override fun onDraw(canvas: Canvas) {
        path.rewind()
        path.addRoundRect(bounds, cornerRadius, cornerRadius, Path.Direction.CW)
        canvas.clipPath(path)
        super.onDraw(canvas)
    }

    //region COMMU

    fun setCorners(cornerRadius: Float) {
        this.cornerRadius = cornerRadius
        postInvalidate()
    }
    //endregion COMMU

}