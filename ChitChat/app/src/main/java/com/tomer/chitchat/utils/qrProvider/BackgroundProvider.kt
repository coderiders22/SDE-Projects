package com.tomer.chitchat.utils.qrProvider

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Point
import android.graphics.RectF
import android.graphics.Shader
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.tomer.chitchat.utils.Utils.Companion.px
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

object BackgroundProvider {

    private fun bmpFromBgAsset(bgAsset: Int, con: Context): Bitmap {
        val bmp = Bitmap.createBitmap(1125, 2436, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val dr = ContextCompat.getDrawable(con, bgAsset)!!
        dr.setBounds(0, 0, 1125, 2436)
        dr.draw(c)
        return bmp
    }

    fun createLinearGradient(dimen: Point, grad: GradModel): LinearGradient {

        // Convert angle from degrees to radians
        val angleRadians = Math.toRadians((grad.angle.mod(360)).toDouble())

        // Calculate the start and end points based on the angle
        val x0 = (dimen.x / 2 + dimen.x / 2 * cos(angleRadians)).toFloat()
        val y0 = (dimen.y / 2 - dimen.y / 2 * sin(angleRadians)).toFloat()
        val x1 = (dimen.x / 2 - dimen.x / 2 * cos(angleRadians)).toFloat()
        val y1 = (dimen.y / 2 + dimen.y / 2 * sin(angleRadians)).toFloat()

        // Create and return the LinearGradient
        return LinearGradient(
            x0, y0, x1, y1,
            grad.startColor, grad.endColor,
            Shader.TileMode.CLAMP
        )
    }

    private fun genTintedPattern(bgAsset: Int, shader: Shader, dimen: Point, con: Context): Bitmap {
        val tBmp = Bitmap.createBitmap(dimen.x, dimen.y, Bitmap.Config.ARGB_8888)
        val bgmp = bmpFromBgAsset(bgAsset, con)

        val paint = Paint().apply {
            isAntiAlias = true
            setShader(shader)
            blendMode = BlendMode.SRC_ATOP
        }

        val adjustedWidth = 280.px
        val adjustedHeight = 2.1653333f.times(adjustedWidth)

        val cM = Canvas(tBmp)
        val w = dimen.x / adjustedWidth.roundToInt()
        val h = dimen.y / adjustedHeight.roundToInt()

        val widthExt = adjustedWidth * (w + 1)
        val heightExt = adjustedHeight * (h + 1)

        val xM = (widthExt - dimen.x).roundToInt() shr 1
        val yM = (heightExt - dimen.y).roundToInt() shr 1


        val pT = Paint(Paint.ANTI_ALIAS_FLAG)
        val destR = RectF(0f, 0f, adjustedWidth, adjustedHeight)
        for (i in 0..w)
            for (j in 0..h) {
                destR.set((adjustedWidth * i) - xM, (adjustedHeight * j) - yM, (adjustedWidth * (i + 1)) - xM, (adjustedHeight * (j + 1) - yM))
                cM.drawBitmap(bgmp, null, destR, pT)
            }
        cM.drawRect(0f, 0f, dimen.x.toFloat(), dimen.y.toFloat(), paint)
        return tBmp
    }

    private fun drawBgDark(bgAsset: Int, dimen: Point, grad: GradModel, con: Context): Bitmap {
        val bmpMain = Bitmap.createBitmap(dimen.x, dimen.y, Bitmap.Config.ARGB_8888)
        val shader: Shader = createLinearGradient(dimen, grad)

        val c = Canvas(bmpMain)
        c.drawColor(Color.BLACK)
        c.drawBitmap(genTintedPattern(bgAsset, shader, dimen, con), 0f, 0f, null)
        return bmpMain
    }

    private fun drawBgLight(bgAsset: Int, dimen: Point, grad: GradModel, con: Context): Bitmap {
        val bmpMain = Bitmap.createBitmap(dimen.x, dimen.y, Bitmap.Config.ARGB_8888)
        val rectF = RectF(0f, 0f, dimen.x.toFloat(), dimen.y.toFloat())
        val shader: Shader = createLinearGradient(dimen, grad)

        val c = Canvas(bmpMain)
        c.drawColor(Color.WHITE)
        c.drawRect(rectF, Paint().apply {
            isAntiAlias = true
            setShader(shader)
            alpha = 110
        })
        c.drawBitmap(genTintedPattern(bgAsset, shader, dimen, con), 0f, 0f, null)
        return bmpMain
    }

    fun getBackground(@DrawableRes bgAsset: Int, dimen: Point, isDark: Boolean, con: Context, @ColorInt color: Int = Color.BLACK, gradModel: GradModel? = null): Bitmap {
        val grad = gradModel ?: GradModel(0, color, color)
        return if (isDark) drawBgDark(bgAsset, dimen, grad, con)
        else drawBgLight(bgAsset, dimen, grad, con)
    }
}