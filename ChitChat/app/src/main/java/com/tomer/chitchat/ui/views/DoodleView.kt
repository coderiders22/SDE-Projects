package com.tomer.chitchat.ui.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.FloatRange
import com.tomer.chitchat.R
import com.tomer.chitchat.utils.qrProvider.AssetsProvider
import com.tomer.chitchat.utils.qrProvider.BackgroundProvider
import com.tomer.chitchat.utils.qrProvider.GradModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlin.math.roundToInt

class DoodleView : View {


    //region CONSTRUCTOR

    constructor(con: Context) : super(con)

    constructor(con: Context, attributeSet: AttributeSet) : super(con, attributeSet)

    constructor(context: Context, attr: AttributeSet, defStyle: Int) : super(context, attr, defStyle)


    //endregion CONSTRUCTOR

    //region GLOBALS-->>>

    @ColorInt
    private var color = Color.BLACK

    @DrawableRes
    private var bgAsset = R.drawable.pattern_7
    private var isDark = true
    private var gradModel: GradModel? = AssetsProvider.gradType.getOrDefault(4, null)
    private var bmpBg = Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565)
    private val paint = Paint().apply {
        isAntiAlias = true
    }

    private var parallaxFactor = 4f
    private var parallaxMaxBounds = 100f

    private var initialX = 100f
    private var initialY = 100f

    private var actualX = 100f
    private var actualY = 100f

    private var jobBitmap: Job? = null

    //endregion GLOBALS-->>>

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > bmpBg.width || h > bmpBg.height) {
            if (jobBitmap != null && jobBitmap!!.isActive)
                jobBitmap!!.cancel()

            jobBitmap = CoroutineScope(Dispatchers.Default).launch {
                yield()
                val bmp = BackgroundProvider.getBackground(bgAsset, Point(w + 200, h + 200), isDark, context, color, gradModel)
                yield()
                bmpBg = bmp
                postInvalidate()
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawBitmap(bmpBg, -actualX, -actualY, paint)
    }

    //region COMMU

    fun setTrans(@FloatRange(0.0, 1.0) alpha: Float) {
        paint.alpha = 255f.times(alpha).roundToInt()
        postInvalidate()
    }

    fun setData(
        isDark: Boolean, @FloatRange(0.0, 1.0) alpha: Float, bgAssetNo: Int,
        @ColorInt color: Int = Color.BLACK, gradModel: GradModel? = null
    ) {
        paint.alpha = 255f.times(alpha).roundToInt()
        this.isDark = isDark
        this.bgAsset = patterns.getOrDefault(bgAssetNo, R.drawable.pattern_7)
        this.color = color
        this.gradModel = gradModel

        if (width > 0) {
            if (jobBitmap != null && jobBitmap!!.isActive)
                jobBitmap!!.cancel()

            jobBitmap = CoroutineScope(Dispatchers.Default).launch {
                yield()
                val bmp = BackgroundProvider.getBackground(this@DoodleView.bgAsset, Point(width + 200, height + 200), isDark, context, color, gradModel)
                yield()
                bmpBg = bmp
                postInvalidate()
            }
        }
        postInvalidate()
    }

    fun setParallaxFactor(fac: Float) {
        parallaxFactor = fac
        parallaxMaxBounds = fac.times(25f).coerceAtMost(200f)
        initialX = parallaxMaxBounds.times(.5f).also { actualX = it }
        initialY = parallaxMaxBounds.times(.5f).also { actualY = it }
    }

    fun onSensorEvent(x: Float, y: Float) {
        val tempActualX = initialX - (x * parallaxFactor)
        val tempActualY = initialY + (y * parallaxFactor)

        val addX = (tempActualX - actualX).coerceAtMost(parallaxFactor).coerceAtLeast(-parallaxFactor)
        val addY = (tempActualY - actualY).coerceAtMost(parallaxFactor).coerceAtLeast(-parallaxFactor)

        actualX = (addX + actualX).coerceAtMost(parallaxMaxBounds).coerceAtLeast(0f)
        actualY = (addY + actualY).coerceAtMost(parallaxMaxBounds).coerceAtLeast(0f)

        postInvalidate()

    }

    companion object {
        private val patterns by lazy {
            mapOf(
                1 to R.drawable.pattern_1,
                2 to R.drawable.pattern_2,
                3 to R.drawable.pattern_3,
                4 to R.drawable.pattern_4,
                5 to R.drawable.pattern_5,
                6 to R.drawable.pattern_6,
                7 to R.drawable.pattern_7,
                8 to R.drawable.pattern_8,
                9 to R.drawable.pattern_9,
                10 to R.drawable.pattern_10,
                11 to R.drawable.pattern_11,
                12 to R.drawable.pattern_12
            )
        }
    }

    //endregion COMMU

}