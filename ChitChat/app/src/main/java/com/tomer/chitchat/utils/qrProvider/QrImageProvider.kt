package com.tomer.chitchat.utils.qrProvider

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.graphics.drawable.VectorDrawable
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.toRect
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.tomer.chitchat.R
import com.tomer.chitchat.utils.Utils.Companion.px

object QrImageProvider {

    private fun bmpFromSVG(dr: Drawable, dimen: Int, rot: Int): Bitmap {
        val bmp = Bitmap.createBitmap(dimen, dimen, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.rotate(rot.toFloat(), dimen / 2f, dimen / 2f)
        dr.setBounds(0, 0, dimen, dimen)
        dr.draw(c)
        return bmp
    }

    private fun centerSquareBmp(dimen: Int): Bitmap {
        val bmp = Bitmap.createBitmap(dimen, dimen, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawRect(Rect(0, 0, dimen, dimen), Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        })
        return bmp
    }

    private fun getBodyBmp(qrMatrtix: Array<BooleanArray>, unit: Int, x: Int, trail: Int, alone: Int, corner: Int, con: Context): Bitmap {
        val markUpBmp = Bitmap.createBitmap(unit * x, unit * x, Bitmap.Config.ARGB_8888)
        val c = Canvas(markUpBmp)

        val mainRect = RectF()

        val trailVec = ContextCompat.getDrawable(con, AssetsProvider.trailAssets.getOrDefault(trail, R.drawable.q_trail1)) as VectorDrawable
        val cornerVec = ContextCompat.getDrawable(con, AssetsProvider.cornerAssets.getOrDefault(corner, R.drawable.q_corner1)) as VectorDrawable
        val aloneVec = ContextCompat.getDrawable(con, AssetsProvider.aloneAssets.getOrDefault(alone, R.drawable.q_alone1)) as VectorDrawable

        val trailTop: Bitmap = bmpFromSVG(trailVec, unit, 0)
        val trailRight: Bitmap = bmpFromSVG(trailVec, unit, 90)
        val trailBottom: Bitmap = bmpFromSVG(trailVec, unit, 180)
        val trailLeft: Bitmap = bmpFromSVG(trailVec, unit, 270)


        val cornerTopLeft: Bitmap = bmpFromSVG(cornerVec, unit, 90)
        val cornerTopRight: Bitmap = bmpFromSVG(cornerVec, unit, 180)
        val cornerBottomLeft: Bitmap = bmpFromSVG(cornerVec, unit, 0)
        val cornerBottomRight: Bitmap = bmpFromSVG(cornerVec, unit, 270)

        val centre: Bitmap = centerSquareBmp(unit)


        //draw simple plane complex
        for (i in 0 until x) for (j in 0 until x) if (qrMatrtix[i][j]) {
            val left = if (j == 0) false
            else qrMatrtix[i][j - 1]

            val top = if (i == 0) false
            else qrMatrtix[i - 1][j]

            val right = if (j == x - 1) false
            else qrMatrtix[i][j + 1]

            val bottom = if (i == x - 1) false
            else qrMatrtix[i + 1][j]


            mainRect[(j * unit).toFloat(), (i * unit).toFloat(), (j * unit).toFloat() + unit] = (i * unit + unit).toFloat()


            if (!top && left && !right) {
                if (bottom) {
                    c.drawBitmap(cornerTopRight, mainRect.left, mainRect.top, null)
                } else {
                    c.drawBitmap(trailRight, mainRect.left, mainRect.top, null)
                }
                continue
            }

            if (!top && !left) {
                if (bottom) {
                    if (right) {
                        c.drawBitmap(cornerTopLeft, mainRect.left, mainRect.top, null)
                    } else {
                        c.drawBitmap(trailTop, mainRect.left, mainRect.top, null)
                    }
                } else {
                    if (right) {
                        c.drawBitmap(trailLeft, mainRect.left, mainRect.top, null)
                    } else {
                        aloneVec.bounds = mainRect.toRect()
                        aloneVec.draw(c)
                    }
                }
                continue
            }

            if (top && !left && right && !bottom) {
                c.drawBitmap(cornerBottomLeft, mainRect.left, mainRect.top, null)
                continue
            }
            if (top && left && !right && !bottom) {
                c.drawBitmap(cornerBottomRight, mainRect.left, mainRect.top, null)
                continue
            }
            if (top && !left && !right && !bottom) {
                c.drawBitmap(trailBottom, mainRect.left, mainRect.top, null)
                continue
            }
            c.drawBitmap(centre, mainRect.left, mainRect.top, null)
        }
        return markUpBmp
    }

    private fun drawEyes(bmp: Bitmap, eyeType: Int, unit: Int, x: Int, con: Context) {

        val c = Canvas(bmp)
        val wu = (7 * unit)

        val b = Bitmap.createBitmap(wu, wu, Bitmap.Config.ARGB_8888)
        val ce = Canvas(b)
        val eyeInner = ContextCompat.getDrawable(con, AssetsProvider.eyesAssets.getOrDefault(eyeType, R.drawable.q_eye_1)) as VectorDrawable
        eyeInner.setBounds(0, 0, wu, wu)
        eyeInner.draw(ce)

        var eye: Rect

        // eye TOPLeft
        c.drawBitmap(b, 0f, 0f, null)
        val halfWith = (b.width shr 1).toFloat()

        val m = Matrix()
        m.postScale(-1f, 1f, halfWith, halfWith)
        val unitDiff = x - 14


        // eye TOPRight
        eye = Rect(unit * (7 + unitDiff), 0, unit * (14 + unitDiff), unit * 7)
        c.drawBitmap(Bitmap.createBitmap(b, 0, 0, b.width, b.width, m, true), null, eye, null)


        // eye BottomLeft
        m.postScale(-1f, -1f, halfWith, halfWith)
        eye = Rect(0, unit * (7 + unitDiff), unit * 7, unit * (14 + unitDiff))
        c.drawBitmap(Bitmap.createBitmap(b, 0, 0, b.width, b.width, m, true), null, eye, null)
    }

    private fun shadeQR(grad: GradModel, bmp: Bitmap, dimen: Float) {
        val shader: Shader = BackgroundProvider.createLinearGradient(Point(bmp.width, bmp.height), grad)
        val paint = Paint()
        paint.isDither = true
        paint.isAntiAlias = true
        paint.blendMode = BlendMode.SRC_ATOP
        paint.setShader(shader)
        val c = Canvas(bmp)
        c.drawRect(RectF(0f, 0f, dimen, dimen), paint)


    }

    private fun drawLogo(bmp: Bitmap, unit: Int, con: Context) {
        try {
            val sizeLogo: Int = unit * 5
            val offSet = (unit * 10f) + unit.times(.66f)
            val bmpLogo = bmpFromSVG(ContextCompat.getDrawable(con, R.drawable.logo_noti2)!!, sizeLogo, 0)
            val c = Canvas(bmp)
            val rect = RectF(offSet, offSet, offSet + sizeLogo - unit, offSet + sizeLogo - unit)
            c.drawBitmap(bmpLogo, null, rect, Paint(Paint.ANTI_ALIAS_FLAG))
        } catch (_: Exception) {
        }
    }

    fun getQRBMP(
        data: String, bodyType: Int, gradType: Int, con: Context,
        width: Int, height: Int, name: String, isDark: Boolean, bgAsset: Int
    ): Bitmap {
        val grad = AssetsProvider.gradType.getOrDefault(gradType, GradModel(45, Color.parseColor("#cb356b"), Color.parseColor("#bd3f32")))
        val mainBmp = BackgroundProvider.getBackground(bgAsset, Point(width, height), isDark, con, Color.BLACK, grad)

        val c = Canvas(mainBmp)
        val mat = getMATRIX(data) ?: return mainBmp

        val h80 = height.times(.8f)
        val h10 = height.times(.1f)
        val h5 = height.times(.05f)

        val textPaint = Paint().apply {
            isAntiAlias = true
            textSize = 26.px
            typeface = ResourcesCompat.getFont(con, R.font.nunito_bold)
        }
        val lineHeight = textPaint.fontMetrics.descent - textPaint.fontMetrics.ascent
        val unit = (h80 - lineHeight - h10).div(mat.size).toInt()

        val size = mat.size * unit
        val p = Paint()
        p.isAntiAlias = true
        p.color = Color.WHITE
        p.style = Paint.Style.FILL

        val cen = width shr 1
        c.drawRoundRect(cen - size.div(2f) - h5, h10, cen + size.div(2f) + h5, h80 + h10, h5, h5, p)

        val bodyData = AssetsProvider.bodyType.getOrDefault(bodyType, listOf(1, 1, 5, 4))
        val body = getBodyBmp(mat, unit, mat.size, bodyData[2], bodyData[0], bodyData[1], con)
        drawEyes(body, bodyData[3], unit, mat.size, con)
        drawLogo(body, unit, con)
        shadeQR(grad, body, size.toFloat())

        c.drawBitmap(body, cen - size.div(2f), h10 + h5, null)
        val shader: Shader = BackgroundProvider.createLinearGradient(Point(width, height), grad)
        textPaint.setShader(shader)

        val textWidth = textPaint.measureText(name)
        val xt = (width - textWidth).div(2)

        c.drawText(name, xt, h80 + h5, textPaint)
        return mainBmp
    }

    private fun genQr(data: String): BitMatrix? {
        val r = QRCodeWriter()
        val b: BitMatrix
        val err: MutableMap<EncodeHintType, ErrorCorrectionLevel?> = HashMap(1)
        err[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.M
        b = try {
            r.encode(data, BarcodeFormat.QR_CODE, 120, 120, err)
        } catch (e: Exception) {
            return null
        }
        return b
    }

    private fun getMATRIX(data: String): Array<BooleanArray>? {
        val bmp = genQr(data) ?: return null
        val bmpDimen = bmp.width
        val unit: Int
        val topPoint: Int


        // sideways finding the first eye pixel
        var i = 0
        while (i < bmpDimen) {
            if (bmp[i, i]) break
            i++
        }
        topPoint = i

        while (i < bmpDimen) {
            if (!bmp[i, i]) break
            i++
        }
        unit = i - topPoint

        var x = bmpDimen - (topPoint shl 1)
        x /= unit

        val ret = Array(x) { BooleanArray(x) }

        var r = 0
        var c = 0

        val ru = bmpDimen - topPoint - unit + 1

        i = topPoint
        while (i < ru) {
            var j = topPoint
            while (j < ru) {
                if (bmp[i, j]) ret[c][r] = true
                c++
                j += unit
            }
            r++
            c = 0
            i += unit
        }

        //remove eyeTopRight
        for (k in 0..6) for (l in x - 7 until x) ret[l][k] = false

        //remove  eyeTopLeft
        for (k in 0..6) for (l in 0..6) ret[l][k] = false

        //remove eyeBottomLeft
        for (k in x - 7 until x) for (l in 0..6) ret[l][k] = false

        //remove center
        for (k in 9..14) for (j in 9..14) ret[j][k] = false

        return ret
    }

}