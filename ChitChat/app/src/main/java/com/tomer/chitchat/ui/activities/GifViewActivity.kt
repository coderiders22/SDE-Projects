package com.tomer.chitchat.ui.activities

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.transition.Transition.TransitionListener
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.animation.AccelerateInterpolator
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.tomer.chitchat.databinding.ActivityGifViewBinding
import com.tomer.chitchat.utils.AlertDialogBuilder
import com.tomer.chitchat.utils.ConversionUtils
import com.tomer.chitchat.utils.Utils.Companion.isDarkModeEnabled
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class GifViewActivity : AppCompatActivity() {

    companion object {
        var bytesImage: ByteArray? = null
    }

    private val b by lazy { ActivityGifViewBinding.inflate(layoutInflater) }

    private var initialTouchDownY = 0f
    private val limit = 480f

    private val colBg = Array(3) { 0 }
    private var currAlfa = 255

    private val dimen = Point(0, 0)

    override fun onBackPressed() {
        b.gifImgView2.visibility = View.GONE
        super.onBackPressedDispatcher.onBackPressed()
    }

    @SuppressLint("ClickableViewAccessibility")
    private val swipeDownTouchLis = View.OnTouchListener { v, event ->
        if (v.id == b.gifImgView.id) mGestureDetector.onTouchEvent(event)
        if (event.action == MotionEvent.ACTION_DOWN) {
            initialTouchDownY = event.rawY
            return@OnTouchListener true
        }

        if (event.action == MotionEvent.ACTION_UP) {
            if (currAlfa < 10)
                onBackPressed()
            else {
                b.cont.animate().apply {
                    y(0f)
                    duration = 200
                    interpolator = AccelerateInterpolator(1.2f)
                    start()
                }
                ValueAnimator.ofInt(currAlfa, 255).apply {
                    duration = 200
                    addUpdateListener {
                        val alfa = it.animatedValue as Int
                        currAlfa = alfa
                        b.root.setBackgroundColor(Color.argb(alfa, colBg[0], colBg[1], colBg[2]))
                        b.layTopBar.alpha = currAlfa.div(255f)
                    }
                    start()
                }
            }
            initialTouchDownY = event.rawY
            return@OnTouchListener true
        }

        if (event.action == MotionEvent.ACTION_MOVE) {

            val diff = event.rawY - initialTouchDownY
            if (diff < 1) return@OnTouchListener true

            b.cont.y = diff.times(1.2f)

            val calAlfa = 255 * diff.div(limit)
            currAlfa = 255 - calAlfa.toInt().coerceAtMost(255)
            b.root.setBackgroundColor(Color.argb(currAlfa, colBg[0], colBg[1], colBg[2]))
            b.layTopBar.alpha = currAlfa.div(255f)

            return@OnTouchListener true
        }
        return@OnTouchListener true
    }

    private val mGestureDetector by lazy {
        GestureDetector(this, object : SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                return b.gifImgView.performClick()
            }
        })
    }

    private val topBarAnimClickLis = View.OnClickListener {
        if (b.layTopBar.y < 0) {
            statusBarVisi(true)
            b.layTopBar.animate().apply {
                duration = 220
                interpolator = AccelerateInterpolator(1.2f)
                y(0f)
                start()
            }
        } else {
            statusBarVisi(false)
            b.layTopBar.animate().apply {
                duration = 160
                interpolator = AccelerateInterpolator(1.2f)
                y(-b.layTopBar.height.toFloat())
                start()
            }
        }
    }

    private fun statusBarVisi(visible: Boolean) {
        if (visible) {
            // Show the status bar
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                window.insetsController?.show(WindowInsets.Type.statusBars())
                return
            }
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            actionBar?.show()
            return
        }
        // Hide the status bar
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
            return
        }
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        actionBar?.hide()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(b.root)
        enableEdgeToEdge(
            SystemBarStyle.auto(
                Color.TRANSPARENT,
                Color.TRANSPARENT
            ) { this.isDarkModeEnabled() })

        if (!this.isDarkModeEnabled()) {
            colBg[0] = 244
            colBg[1] = 248
            colBg[2] = 251
        } else {
            colBg[0] = 4
            colBg[1] = 16
            colBg[2] = 22
        }
        b.root.setBackgroundColor(Color.argb(255, colBg[0], colBg[1], colBg[2]))
        val canSaveToGal = intent.getBooleanExtra("canSaveToGal", false)
        val canDelete = intent.getBooleanExtra("canDelete", false)
        val heading = intent.getStringExtra("heading").toString()
        val file = File(intent.getStringExtra("file") ?: "")

        if (intent.hasExtra("timeText"))
            b.tvDetails.text = (intent.getStringExtra("timeText")
                ?: "").also { if (it.isEmpty()) b.tvDetails.visibility = View.GONE }
        else {
            val time = intent.getLongExtra("timeMillis", System.currentTimeMillis())
            val dayDate =
                ConversionUtils.getRelativeTime(time).takeIf { !it.contains(':') } ?: "Today"
            val timeToday = ConversionUtils.millisToTimeText(time)
            "$dayDate • $timeToday".also { b.tvDetails.text = it }
        }

        if (canDelete)
            b.btDelete.setOnClickListener {
                AlertDialogBuilder(this)
                    .setTitle("Delete file?")
                    .setDescription("Do you really want to delete this file?")
                    .setPositiveButton("Delete for me") {
                        onBackPressed()
                        setResult(RESULT_OK)
                    }
                    .show()
            }

        b.btBack.setOnClickListener {
            onBackPressed()
        }

        b.tvPartnerName.text = heading
        if (canSaveToGal) {
            b.btSaveToGallery.setOnClickListener {
                val uri = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

                val cnVals = ContentValues()
                cnVals.put(MediaStore.Images.Media.WIDTH, dimen.x)
                cnVals.put(MediaStore.Images.Media.HEIGHT, dimen.y)

                cnVals.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Chit Chat")
                cnVals.put(MediaStore.Images.Media.MIME_TYPE, "image/gif")
                cnVals.put(MediaStore.Images.Media.DISPLAY_NAME, file.name)

                try {
                    val nuri = this.contentResolver.insert(uri, cnVals) ?: return@setOnClickListener
                    this.contentResolver.openOutputStream(nuri).use {
                        it?.write(bytesImage)
                        it?.flush()
                    }
                    b.root.post {
                        Toast.makeText(
                            this,
                            "Image saved to Gallery...",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (_: Exception) {
                    Toast.makeText(this, "Can't save...", Toast.LENGTH_SHORT).show()
                }
            }
            b.btSaveToGallery.visibility = View.VISIBLE
        }

        b.btDelete.visibility = if (canDelete) View.VISIBLE else View.GONE
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        val h = if (resourceId > 0) {
            resources.getDimensionPixelSize(resourceId)
        } else {
            0
        }
        b.layTopBar.setPadding(0, h, 0, 0)
        b.gifImgView.transitionName = file.name
        b.gifImgView.visibility = View.VISIBLE
        b.gifImgView.setOnTouchListener(swipeDownTouchLis)
        b.gifImgView.setOnClickListener(topBarAnimClickLis)
        Glide.with(this.baseContext)
            .asBitmap()
            .load(bytesImage)
            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .into(
                object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        b.gifImgView.setImageBitmap(resource)
                        dimen.set(resource.width, resource.height)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                    }

                })

        window.sharedElementEnterTransition.addListener(
            object : TransitionListener {
                override fun onTransitionStart(transition: android.transition.Transition?) {

                }

                override fun onTransitionEnd(transition: android.transition.Transition?) {
//                    b.viewIMg.postInvalidate()
                    b.gifImgView.postInvalidate()
                    b.gifImgView2.visibility = View.VISIBLE
                    Glide.with(this@GifViewActivity)
                        .load(bytesImage)
                        .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .into(b.gifImgView2)
                }

                override fun onTransitionCancel(transition: android.transition.Transition?) {
                }

                override fun onTransitionPause(transition: android.transition.Transition?) {
                }

                override fun onTransitionResume(transition: android.transition.Transition?) {
                }

            }
        )
    }
}