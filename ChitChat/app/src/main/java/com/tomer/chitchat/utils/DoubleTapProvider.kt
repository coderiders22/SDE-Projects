package com.tomer.chitchat.utils

import android.graphics.PointF
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View

fun dispatchDoubleTap(
    targetView: View,
    point: PointF,
    intervalMs: Long = 100L // gap between taps
) {
    val downTime = SystemClock.uptimeMillis()
    // First tap: DOWN + UP
    MotionEvent.obtain(
        downTime,
        downTime,
        MotionEvent.ACTION_DOWN,
        point.x,
        point.y,
        0
    ).also {
        targetView.dispatchTouchEvent(it)
        it.recycle()
    }

    MotionEvent.obtain(
        downTime,
        downTime + 50,
        MotionEvent.ACTION_UP,
        point.x,
        point.y,
        0
    ).also {
        targetView.dispatchTouchEvent(it)
        it.recycle()
    }

    // Second tap: DOWN + UP (after interval)
    val secondDownTime = downTime + intervalMs

    MotionEvent.obtain(
        secondDownTime,
        secondDownTime,
        MotionEvent.ACTION_DOWN,
        point.x,
        point.y,
        0
    ).also {
        targetView.dispatchTouchEvent(it)
        it.recycle()
    }

    MotionEvent.obtain(
        secondDownTime,
        secondDownTime + 50,
        MotionEvent.ACTION_UP,
        point.x,
        point.y,
        0
    ).also {
        targetView.dispatchTouchEvent(it)
        it.recycle()
    }
}
