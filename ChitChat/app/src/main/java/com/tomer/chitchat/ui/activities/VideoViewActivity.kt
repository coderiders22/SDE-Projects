package com.tomer.chitchat.ui.activities

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.ContentValues
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.transition.Transition.TransitionListener
import android.util.Log
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import com.tomer.chitchat.R
import com.tomer.chitchat.databinding.ActivityVideoViewBinding
import com.tomer.chitchat.utils.AlertDialogBuilder
import com.tomer.chitchat.utils.ConversionUtils
import com.tomer.chitchat.utils.Utils
import com.tomer.chitchat.utils.Utils.Companion.isDarkModeEnabled
import com.tomer.chitchat.utils.Utils.Companion.px
import com.tomer.chitchat.viewmodals.VideoViewVIewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.FileInputStream

@UnstableApi
@AndroidEntryPoint
class VideoViewActivity : AppCompatActivity() {

    private val b by lazy { ActivityVideoViewBinding.inflate(layoutInflater) }
    private val vm: VideoViewVIewModel by viewModels()

    private var progressTracking = false
    private val colBg = Array(3) { 0 }
    private var currAlfa = 255
    private var initialTouchDownY = 0f
    private val limit = 480f

    @SuppressLint("ClickableViewAccessibility")
    private val swipeDownTouchLis = View.OnTouchListener { v, event ->
        if (event.action == MotionEvent.ACTION_DOWN) {
            initialTouchDownY = event.rawY
            return@OnTouchListener true
        }

        if (event.action == MotionEvent.ACTION_UP) {
            if (currAlfa < 10)
                onBackPressed()
            else {
                b.root.animate().apply {
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

            b.root.y = diff.times(1.2f)

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

                return true
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
        val videoTime = intent.getStringExtra("videoTime").toString()
        val videoName = intent.getStringExtra("videoName") ?: ""
        Log.d("TAG--", "onCreate: $videoName")
        if (videoName.startsWith("VID").not()) {
            finish()
            return
        }
        b.imgThumb.visibility = View.VISIBLE
        b.imgThumb.transitionName = videoName

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

        if (canSaveToGal) setupSaveVideo(videoName)
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
        b.btDelete.visibility = if (canDelete) View.VISIBLE else View.GONE
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        val h = if (resourceId > 0) {
            resources.getDimensionPixelSize(resourceId)
        } else {
            0
        }
        b.layTopBar.setPadding(0, h, 0, 0)
        b.exoPlayerView.setOnTouchListener(swipeDownTouchLis)
        b.imgThumb.setOnTouchListener(swipeDownTouchLis)
        b.exoPlayerView.player = vm.exoPlayer

        vm.processSelectedUri(videoName, Utils.currentPartner?.partnerId ?: "", videoTime)

        b.imgPlay.setOnClickListener {
            if (vm.exoPlayer.isPlaying) vm.exoPlayer.pause()
            else vm.exoPlayer.play()
        }

        window.sharedElementEnterTransition.addListener(
            object : TransitionListener {
                override fun onTransitionStart(transition: android.transition.Transition?) {

                }

                override fun onTransitionEnd(transition: android.transition.Transition?) {
                    vm.onEnd()
                }


                override fun onTransitionCancel(transition: android.transition.Transition?) {
                }

                override fun onTransitionPause(transition: android.transition.Transition?) {
                }

                override fun onTransitionResume(transition: android.transition.Transition?) {
                }

            }
        )
        setupLiveData()
        setupSeekBar()
    }

    override fun onPause() {
        super.onPause()
        vm.exoPlayer.pause()
    }

    fun setupLiveData() {
        lifecycleScope.launch {
            vm.times.collectLatest {
                b.tvCurrentTime.text = it.first
                b.tvLength.text = it.second
            }
        }
        lifecycleScope.launch {
            vm.isImgVisible.collectLatest {
                if (it) {
                    b.imgThumb.animate()
                        .apply {
                            alpha(1f)
                            duration = 100
                            start()
                        }
                } else {
                    b.imgThumb.animate()
                        .apply {
                            duration = 100
                            alpha(0f)
                            start()
                        }
                }
            }
        }
        lifecycleScope.launch {
            vm.bmp.collectLatest {
                if (it==null) return@collectLatest
                b.imgThumb.setImageBitmap(it)
            }
        }
        lifecycleScope.launch {
            vm.isMute.collectLatest {
                if (it) b.btMute.setImageResource(R.drawable.ic_speaker_cross)
                else b.btMute.setImageResource(R.drawable.ic_speaker_on)
                vm.exoPlayer.volume = if (it) 0f else 1f
            }
        }
        lifecycleScope.launch {
            vm.showPlayButton.collectLatest {
                if (it) {
                    b.imgPlay.visibility = View.VISIBLE
                } else {
                    b.imgPlay.visibility = View.GONE
                }
            }
        }
        lifecycleScope.launch {
            vm.partnerName.collectLatest {
                if (it == null) return@collectLatest
                b.tvPartnerName.text = it.name
                if (it.accent.grad == null) {
                    b.bgTvCurrentTime.setData(null, 40.px, it.accent.color)
                    b.bgBtMute.setData(null, 40.px, it.accent.color)
                    b.bgTvLength.setData(null, 40.px, it.accent.color)
                } else {
                    b.bgTvCurrentTime.setData(null, 40.px, it.accent.grad!!)
                    b.bgBtMute.setData(null, 40.px, it.accent.grad!!)
                    b.bgTvLength.setData(null, 40.px, it.accent.grad!!)
                }
            }
        }
    }

    fun setupSeekBar() {
        b.seekPlayer.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    vm.exoPlayer.seekTo(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                progressTracking = true
                vm.exoPlayer.pause()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                progressTracking = false
                vm.exoPlayer.play()
            }
        })
        lifecycleScope.launch {
            vm.seekBar.collectLatest {
                b.seekPlayer.progress = (it * 100).toInt()
            }
        }
    }

    fun setupSaveVideo(videoName: String) {
        b.btSaveToGallery.setOnClickListener {
            if (vm.videoFile == null) return@setOnClickListener
            val collection =
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

            val cnVals = ContentValues()

            cnVals.put(
                MediaStore.Video.Media.RELATIVE_PATH,
                Environment.DIRECTORY_MOVIES + "/Chit Chat"
            )
            cnVals.put(MediaStore.Video.Media.IS_PENDING, 1)

            cnVals.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            cnVals.put(MediaStore.Video.Media.DISPLAY_NAME, videoName)

            try {
                val nuri =
                    this.contentResolver.insert(collection, cnVals) ?: return@setOnClickListener

                this.contentResolver.openOutputStream(nuri).use { outputStream ->
                    FileInputStream(vm.videoFile).use { inputStream ->
                        inputStream.copyTo(outputStream!!)
                    }
                    outputStream?.flush()
                }

                cnVals.clear()
                cnVals.put(MediaStore.Video.Media.IS_PENDING, 0)
                this.contentResolver.update(nuri, cnVals, null, null)

                b.root.post {
                    Toast.makeText(
                        this,
                        "Video saved to Gallery...",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Can't save video...", Toast.LENGTH_SHORT).show()
            }
        }
        b.btSaveToGallery.visibility = View.VISIBLE
    }
}