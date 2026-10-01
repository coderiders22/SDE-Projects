package com.tomer.chitchat.ui.views

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.tomer.chitchat.R
import com.tomer.chitchat.utils.Utils.Companion.px
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.suspendCoroutine

@SuppressLint("ClickableViewAccessibility")
class ExtendedDpView : View {
    //region CONSTRUCTOR

    constructor(con: Context) : super(con)

    constructor(con: Context, attributeSet: AttributeSet) : super(con, attributeSet)

    constructor(context: Context, attr: AttributeSet, defStyle: Int) : super(context, attr, defStyle)


    //endregion CONSTRUCTOR

    //region GLOBALS-->>>
    private var cornerRadius = 0f
    private var animDir = 0 //0 not animating -1 going down 1 going up
    private var canShow = false
    private var bitmapDp: Bitmap? = null
    private val bounds = RectF()
    private val initialBounds = RectF()
    private val finalBounds = RectF()
    private val loc = IntArray(2)

    private var animObj: ValueAnimator? = null
    private var animFac = 0f
    private val px48 = 48.px

    private var defAvatar: Bitmap? = null

    //endregion GLOBALS-->>>

    init {
        setOnTouchListener { _, event ->
            if (!canShow) return@setOnTouchListener false

            if (event.action == MotionEvent.ACTION_UP) {
                if (!bounds.contains(event.x, event.y))
                    setBitmap("", PointF(0f, 0f))
            }
            true
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val finalW = w.times(.68f)
        val x = (w - finalW).times(.5f)
        val y = h.times(.2f)
        finalBounds.set(x, y, x + finalW, y + finalW)
        getLocationOnScreen(loc)

        defAvatar = getDefAvatar(finalBounds)
    }

    private fun getDefAvatar(finalBounds: RectF): Bitmap {
        val bmp = Bitmap.createBitmap(finalBounds.width().toInt(), finalBounds.height().toInt(), Bitmap.Config.ARGB_8888)

        val dr = ContextCompat.getDrawable(this.context, R.drawable.def_avatar) ?: return bmp
        dr.setBounds(0, 0, finalBounds.width().toInt(), finalBounds.height().toInt())
        dr.draw(Canvas(bmp))

        return bmp
    }

    private val path = Path()

    override fun onDraw(canvas: Canvas) {
        if (!canShow || bitmapDp == null) return

        path.rewind()
        path.addRoundRect(bounds, cornerRadius, cornerRadius, Path.Direction.CW)

        canvas.clipPath(path)
        canvas.drawBitmap(bitmapDp!!, null, bounds, null)
    }

    //region COMMA

    fun isVisible() = canShow

    fun setBitmap(path: String, pos: PointF = PointF()) {
        val file = File(path).takeIf { it.exists() }
        if (file == null) {
            if (!canShow) return
            if (animDir == -1)
                return
            if (animDir == 1)
                animObj?.cancel()
            startDownAnim()
            return
        }
        CoroutineScope(Dispatchers.Default).launch {
            bitmapDp = getBitmap(file) ?: defAvatar
            canShow = true
            withContext(Dispatchers.Main) {
                startUpAnim()
            }
        }
        initialBounds.set(pos.x, pos.y - loc[1], pos.x + px48, pos.y + px48 - loc[1])
    }

    //endregion COMMA

    private fun startUpAnim() {
        animObj = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 340
            addUpdateListener {
                animFac = it.animatedValue as Float
                bounds.set(
                    (finalBounds.left - initialBounds.left).times(animFac) + initialBounds.left,
                    (finalBounds.top - initialBounds.top).times(animFac) + initialBounds.top,
                    (finalBounds.right - initialBounds.right).times(animFac) + initialBounds.right,
                    (finalBounds.bottom - initialBounds.bottom).times(animFac) + initialBounds.bottom
                )
                cornerRadius = px48.times(1 - animFac)
                postInvalidate()
            }
            doOnStart { animDir = 1 }
            doOnEnd { animDir = 0 }
            start()
        }
    }

    private fun startDownAnim() {
        animObj = ValueAnimator.ofFloat(animFac, 0f).apply {
            duration = 340
            addUpdateListener {
                animFac = it.animatedValue as Float
                interpolator = AccelerateDecelerateInterpolator()
                bounds.set(
                    (finalBounds.left - initialBounds.left).times(animFac) + initialBounds.left,
                    (finalBounds.top - initialBounds.top).times(animFac) + initialBounds.top,
                    (finalBounds.right - initialBounds.right).times(animFac) + initialBounds.right,
                    (finalBounds.bottom - initialBounds.bottom).times(animFac) + initialBounds.bottom
                )
                cornerRadius = px48.times(1 - animFac)
                postInvalidate()
            }
            doOnStart { animDir = -1 }
            doOnEnd {
                canShow = false
                animDir = 0
            }
            start()
        }
    }

    private suspend fun getBitmap(file: File): Bitmap? {
        return suspendCoroutine { continuation ->
            Glide.with(this)
                .asBitmap()
                .load(file)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        continuation.resumeWith(Result.success(resource))
                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        continuation.resumeWith(Result.success(null))
                        super.onLoadFailed(errorDrawable)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                    }
                })
        }
    }
}