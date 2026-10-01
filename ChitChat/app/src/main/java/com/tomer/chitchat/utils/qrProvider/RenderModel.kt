package com.tomer.chitchat.utils.qrProvider

import androidx.annotation.ColorInt
import androidx.annotation.FloatRange

data class RenderModel(
    @param:FloatRange(0.0, 1.0)
    var alpha: Float,
    @param:ColorInt
    val color: Int,
    val grad: GradModel? = null
)
