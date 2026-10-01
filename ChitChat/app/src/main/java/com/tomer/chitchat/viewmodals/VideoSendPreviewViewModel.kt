package com.tomer.chitchat.viewmodals

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Looper
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.PlayerMessage
import com.otaliastudios.transcoder.TranscoderListener
import com.tomer.chitchat.repo.RepoPersons
import com.tomer.chitchat.repo.RepoStorage
import com.tomer.chitchat.room.ModelPartnerPref
import com.tomer.chitchat.room.MsgMediaType
import com.tomer.chitchat.utils.ConversionUtils
import com.tomer.chitchat.utils.VideoDetails
import com.tomer.chitchat.utils.compressAndTrimDeepMedia
import com.tomer.chitchat.utils.estimateFileSize
import com.tomer.chitchat.utils.extract10Frames
import com.tomer.chitchat.utils.getAFrameFromVideo
import com.tomer.chitchat.utils.getAllPossibleDetails
import com.tomer.chitchat.utils.getBmpUsingGlide
import com.tomer.chitchat.utils.timeTextFromMs
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import javax.inject.Inject

@UnstableApi
@HiltViewModel
class VideoSendPreviewViewModel
@Inject constructor(
    @param:ApplicationContext val appContext: Context,
    private val repo: RepoPersons,
    private val repoStorage: RepoStorage,
) : ViewModel() {

    val exoPlayer = ExoPlayer.Builder(appContext).build()

    val flowFramesThumb = MutableSharedFlow<Pair<Int, Bitmap>>()

    val partnerName = MutableStateFlow<ModelPartnerPref?>(null)
    val showPlayButton = MutableStateFlow(true)
    val metaData = MutableStateFlow<VideoDetails?>(null)

    val videoFinalTime = MutableStateFlow("")
    val videoTrimTime = MutableStateFlow("")
    val isMute = MutableStateFlow(false)
    val seekBars = MutableStateFlow(Triple(0f, 0f, 1f))

    val encodeProg = MutableStateFlow(-1f)
    val finishActivity = MutableStateFlow<Boolean?>(null)


    private var videoThumbBytes = ByteArray(0)
    var fileName = ""
    var videoTime = ""
    var aspect: Float = 0f
    var uri: Uri? = null
    private var partnerPhone: String? = null
    private var replyId: Long = -1L
    private var seekBarSyncJob = viewModelScope.launch { }

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                if (isPlaying) {
                    seekBarSyncJob.cancel()
                    seekBarSyncJob = createSeekBarSyncJob()
                } else seekBarSyncJob.cancel()
                viewModelScope.launch {
                    showPlayButton.emit(!isPlaying)
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                if (playbackState == Player.STATE_READY) {
                    viewModelScope.launch {
                        if (videoFinalTime.value.isEmpty()) {
                            val duration = exoPlayer.duration.timeTextFromMs()
                            videoFinalTime.emit(
                                "$duration • ${
                                    estimateFileSize(
                                        0L,
                                        exoPlayer.duration,
                                        isMute.value.not()
                                    )
                                }"
                            )
                        }
                    }
                }
            }
        })
    }

    fun toggleMuteButton() {
        viewModelScope.launch {
            isMute.emit(isMute.value.not())
            val startMs = (exoPlayer.duration * seekBars.value.first).toLong()
            val endMs = (exoPlayer.duration * seekBars.value.third).toLong()

            val estimatedSize = estimateFileSize(
                startMs,
                endMs,
                isMute.value.not()
            )

            videoFinalTime.emit("${(endMs - startMs).timeTextFromMs()} • $estimatedSize")
        }
    }

    // Keep a reference to the last message so we can cancel it if the user moves the handle again
    private var currentStopMessage: PlayerMessage? = null

    fun onSeekBarChange(start: Float, end: Float) {
        val totalVideoDuration = exoPlayer.duration

        if (totalVideoDuration <= 0) return

        val startMs = (totalVideoDuration * start).toLong()
        val endMs = (totalVideoDuration * end).toLong()

        // 1. Cancel the old trap (from previous slider movement)
        currentStopMessage?.cancel()

        // 2. Define a helper to schedule the loop "trap"
        fun scheduleLoop() {
            currentStopMessage = exoPlayer.createMessage { _, _ ->
                // A. Loop back to start
                exoPlayer.seekTo(startMs)

                // B. IMPORTANT: Re-arm the trap for the next run!
                scheduleLoop()
            }
                .setLooper(Looper.getMainLooper())
                .setPosition(endMs)
                .setDeleteAfterDelivery(true)
                .send()
        }

        // 3. Set the first trap
        scheduleLoop()

        // 4. Seek to start and ensure playing
        exoPlayer.seekTo(startMs)

        // 5. Update UI
        viewModelScope.launch {
            val trimDurationStr = (endMs - startMs).timeTextFromMs()
            val estimatedSize = estimateFileSize(startMs, endMs, isMute.value.not())

            videoFinalTime.emit("$trimDurationStr • $estimatedSize")
            videoTrimTime.emit("${startMs.timeTextFromMs()} -- ${endMs.timeTextFromMs()}")
        }
    }

    fun processSelectedUri(uri: Uri, partnerPhone: String, replyId: Long) {
        extract10Frames(uri, appContext) { i, bmp ->
            viewModelScope.launch {
                flowFramesThumb.emit(i to bmp)
            }
        }
        if (this.uri != null) return

        this.uri = uri
        this.partnerPhone = partnerPhone
        this.replyId = replyId

        viewModelScope.launch {
            val p = repo.getPersonPref(phoneNo = partnerPhone)
            partnerName.emit(p)
        }
        viewModelScope.launch {
            val data = getAllPossibleDetails(appContext, uri)
            metaData.emit(data)
            if (data == null) return@launch

            val duration = if ((data.durationMs ?: 100) > 500) 500 else data.durationMs
            val bmp =
                getAFrameFromVideo(appContext, uri, (duration ?: 500L).toInt()) ?: return@launch
            val scaledBmp = getBmpUsingGlide(bmp, appContext) ?: return@launch
            aspect = scaledBmp.width.toFloat().div(scaledBmp.height.toFloat())
            val baos = ByteArrayOutputStream()
            scaledBmp.compress(Bitmap.CompressFormat.WEBP, 80, baos)
            videoThumbBytes = baos.toByteArray()
            scaledBmp.recycle()
            baos.close()
        }
        viewModelScope.launch {
            exoPlayer.setMediaItem(MediaItem.fromUri(uri))
            exoPlayer.prepare()
        }
    }

    fun finalProcessOfVideo() {
        if (uri == null) return
        if (encodeProg.value >= 0f) return
        viewModelScope.launch {
            encodeProg.emit(0f)
        }
        val listener = object : TranscoderListener {
            override fun onTranscodeProgress(prog: Double) {
                viewModelScope.launch {
                    encodeProg.emit(prog.toFloat())
                }
            }

            override fun onTranscodeCompleted(p0: Int) {
                viewModelScope.launch {
                    finishActivity.emit(true)
                }
            }

            override fun onTranscodeCanceled() {
            }

            override fun onTranscodeFailed(p0: Throwable) {
                Log.e("TAG--", "onTranscodeFailed: ", p0)
                viewModelScope.launch {
                    finishActivity.emit(false)
                }
            }
        }
        val videoName = getVideoName(System.currentTimeMillis())
        this.fileName = videoName
        repoStorage.saveVideoThumb(videoName, videoThumbBytes)
        repoStorage.saveBytesToFolder(MsgMediaType.VIDEO, videoName, ByteArray(0))
        val fileVideo = repoStorage.getFileFromFolder(MsgMediaType.VIDEO, videoName)!!
        if (seekBars.value.first == 0f && seekBars.value.third == 1f)
            compressAndTrimDeepMedia(
                appContext,
                uri!!,
                fileVideo.absolutePath,
                listener,
                !isMute.value,
            ).also { videoTime = exoPlayer.duration.timeTextFromMs() }
        else {
            val startMilli = seekBars.value.first
                .times(exoPlayer.duration).toLong()
            val endMilli = seekBars.value.third
                .times(exoPlayer.duration).toLong()
            compressAndTrimDeepMedia(
                appContext,
                uri!!,
                fileVideo.absolutePath,
                listener,
                !isMute.value,
                startMilli, endMilli
            )
            videoTime = (endMilli - startMilli).timeTextFromMs()
        }
    }

    private fun createSeekBarSyncJob(): Job {
        return viewModelScope.launch {
            while (isActive) {
                val currentPosition = exoPlayer.currentPosition
                val totalDuration = exoPlayer.duration

                if (totalDuration > 0) {
                    val progress = currentPosition.toFloat() / totalDuration
                    seekBars.emit(Triple(seekBars.value.first, progress, seekBars.value.third))
                }
                delay(100)
            }
        }
    }

    private fun getVideoName(millis: Long) =
        "VID_${ConversionUtils.millisToFullDateText(millis)}.mp4"

    override fun onCleared() {
        super.onCleared()
        exoPlayer.pause()
        exoPlayer.release()
        seekBarSyncJob.cancel()
    }
}