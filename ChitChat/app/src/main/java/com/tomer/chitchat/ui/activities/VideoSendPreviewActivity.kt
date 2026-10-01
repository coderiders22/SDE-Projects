package com.tomer.chitchat.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import com.tomer.chitchat.R
import com.tomer.chitchat.databinding.ActivityVideoSendPreviewBinding
import com.tomer.chitchat.utils.Utils.Companion.px
import com.tomer.chitchat.utils.timeTextFromMs
import com.tomer.chitchat.viewmodals.VideoSendPreviewViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@UnstableApi
@AndroidEntryPoint
class VideoSendPreviewActivity : AppCompatActivity() {

    private val b by lazy { ActivityVideoSendPreviewBinding.inflate(layoutInflater) }
    private val vm: VideoSendPreviewViewModel by viewModels()


    override fun onBackPressed() {
        super.onBackPressed()
    }

    override fun onPause() {
        super.onPause()
        vm.exoPlayer.pause()
    }

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(b.root)
        ViewCompat.setOnApplyWindowInsetsListener(b.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val uri = intent.getStringExtra("uri")
        val partnerPhone = intent.getStringExtra("partnerPhone")
        val replyId = intent.getLongExtra("replyId", -1L)
        if (uri == null) {
            finish()
            return
        }
        vm.processSelectedUri(uri.toUri(), partnerPhone ?: "", replyId)
        b.exoPlayerView.player = vm.exoPlayer

        b.videoTrimView.onRangeChangeListener = { start, end -> vm.onSeekBarChange(start, end) }
        b.videoTrimView.onTouchDownListener = { isDown, s, m, e ->
            if (isDown) {
                vm.exoPlayer.pause()
                val startMs = (vm.exoPlayer.duration * s).toLong()
                val endMs = (vm.exoPlayer.duration * e).toLong()

                lifecycleScope.launch {
                    vm.videoTrimTime.emit("${startMs.timeTextFromMs()} -- ${endMs.timeTextFromMs()}")
                }

            } else {
                lifecycleScope.launch {
                    vm.videoTrimTime.emit("")
                }
                lifecycleScope.launch {
                    vm.seekBars.emit(Triple(s, m, e))
                }
                vm.exoPlayer.play()
            }
        }
        b.exoPlayerView.setOnClickListener {
            if (vm.exoPlayer.isPlaying) vm.exoPlayer.pause()
            else vm.exoPlayer.play()
        }

        b.btBack.setOnClickListener {
            onBackPressed()
        }

        b.btSend.setOnClickListener {
            b.btSend.playAnimation()
        }
        b.btMute.setOnClickListener {
            vm.toggleMuteButton()
        }
        b.btSend.setOnClickListener {
            b.btSend.playAnimation()
            vm.finalProcessOfVideo()
        }
        setupLiveData()
        setupImages()

    }

    private fun setupImages() {
        repeat(10) {
            b.llImages.addView(
                ImageView(this)
                    .apply {
                        layoutParams = LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.MATCH_PARENT, 1f
                        )
                        scaleType = ImageView.ScaleType.CENTER_CROP
                    })
        }
        lifecycleScope.launch {
            vm.flowFramesThumb.collectLatest {
                (b.llImages.getChildAt(it.first) as ImageView).setImageBitmap(it.second)
            }
        }
    }


    fun setupLiveData() {
        lifecycleScope.launch {
            vm.finishActivity.collectLatest {
                if (it == null) return@collectLatest
                if (it) {
                    val resultIntent = Intent().apply {
                        putExtra("FILE_NAME", vm.fileName)
                        putExtra("FILE_URI", vm.uri.toString())
                        putExtra("ASPECT", vm.aspect.toString())
                        putExtra("VIDEO_TIME", vm.videoTime)
                    }
                    setResult(RESULT_OK, resultIntent)
                } else setResult(RESULT_CANCELED)
                finish()
            }
        }
        lifecycleScope.launch {
            vm.encodeProg.collectLatest {
                if (it == -1f) b.progEncoding.visibility = View.GONE
                else {
                    b.progEncoding.visibility = View.VISIBLE
                    b.progEncoding.progress = (100 * it).toInt()
                }
            }
        }
        lifecycleScope.launch {
            vm.seekBars.collectLatest {
                b.videoTrimView.setRange(it.first, it.third)
                b.videoTrimView.setProgress(it.second)
            }
        }
        lifecycleScope.launch {
            vm.videoFinalTime.collectLatest {
                b.tvLength.visibility = if (it.isEmpty()) View.GONE else View.VISIBLE
                b.tvLength.text = it
            }
        }
        lifecycleScope.launch {
            vm.videoTrimTime.collectLatest {
                if (it.isEmpty()) {
                    b.tvTrimTime.visibility = View.GONE
                    b.layBottom.visibility = View.VISIBLE
                } else {
                    b.tvTrimTime.text = it
                    b.tvTrimTime.visibility = View.VISIBLE
                    b.layBottom.visibility = View.INVISIBLE
                }
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
                    b.bgPlayButton.visibility = View.VISIBLE
                } else {
                    b.imgPlay.visibility = View.GONE
                    b.bgPlayButton.visibility = View.GONE
                }
            }
        }
        lifecycleScope.launch {
            vm.metaData.collectLatest {
                b.tvVideoName.text = it?.name ?: ""
            }
        }
        lifecycleScope.launch {
            vm.partnerName.collectLatest {
                if (it == null) return@collectLatest
                b.tvPartnerName.text = it.name
                if (it.accent.grad == null) {
                    b.bgSendButton.setData(null, 40.px, it.accent.color)
                    b.bgBtMute.setData(null, 40.px, it.accent.color)
                    b.bgPlayButton.setData(null, 40.px, it.accent.color)
                    b.bgTvLength.setData(null, 40.px, it.accent.color)
                } else {
                    b.bgSendButton.setData(null, 40.px, it.accent.grad!!)
                    b.bgBtMute.setData(null, 40.px, it.accent.grad!!)
                    b.bgPlayButton.setData(null, 40.px, it.accent.grad!!)
                    b.bgTvLength.setData(null, 40.px, it.accent.grad!!)
                }
            }
        }
    }


}